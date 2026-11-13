const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const source = fs.readFileSync(path.join(__dirname, '../src/services/admin-api.ts'), 'utf8');
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText;
function load(fetch) {
  const events = [];
  const context = { exports: {}, fetch, FormData, Headers, URLSearchParams, Intl, Event, window: { dispatchEvent: event => events.push(event.type) } };
  vm.runInNewContext(compiled, context);
  return { ...context.exports, events };
}
const response = (data, status = 200) => ({ ok: status >= 200 && status < 300, status, json: async () => ({ success: status === 200, data, message: status === 200 ? 'ok' : 'server error' }) });
const session = { authenticated: true, username: 'admin', csrfToken: 'masked-token', csrfHeader: 'X-CSRF-TOKEN' };
test('admin writes use session cookies, CSRF header and real JSON payload', async () => {
  const calls = [];
  const api = load(async (path, init) => { calls.push([path, init]); return response(path.endsWith('/session') ? session : { saved: true }); });
  await api.adminRequest('/api/v1/products', { method: 'POST', body: JSON.stringify({ tenSP: 'Test' }) });
  assert.equal(calls.length, 2);
  assert.equal(calls[1][1].headers.get('X-CSRF-TOKEN'), 'masked-token');
  assert.equal(calls[1][1].credentials, 'same-origin');
  assert.equal(calls[1][1].headers.get('Content-Type'), 'application/json');
});
test('login refreshes CSRF after authentication and keeps URL encoded content type', async () => {
  const calls = []; let sessions = 0;
  const api = load(async (path, init) => { calls.push([path, init]); return response(path.endsWith('/session') ? { ...session, csrfToken: `token-${++sessions}` } : { username: 'admin' }); });
  await api.adminLogin('admin', 'password');
  await api.adminRequest('/api/v1/search/sync', { method: 'POST' });
  assert.equal(calls[1][1].body.toString(), 'username=admin&password=password');
  assert.equal(calls[1][1].headers.has('Content-Type'), false);
  assert.equal(calls[3][1].headers.get('X-CSRF-TOKEN'), 'token-2');
});
test('expired sessions notify shell and reject instead of returning sample data', async () => {
  const api = load(async path => path.endsWith('/session') ? response(session) : response(null, 401));
  await assert.rejects(api.adminRequest('/api/v1/admin/products'), /server error/);
  assert.deepEqual(api.events, ['admin:unauthorized']);
});
test('server and network failures propagate without admin mocks', async () => {
  const api = load(async path => path.endsWith('/session') ? response(session) : response(null, 500));
  await assert.rejects(api.adminRequest('/api/v1/benchmark/compare'), /server error/);
  const offline = load(async () => { throw new Error('offline'); });
  await assert.rejects(offline.adminRequest('/api/v1/admin/overview'), /offline/);
});
