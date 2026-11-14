// Uses the local admin account without printing credentials, cookies or CSRF tokens.
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.DEMO_API_URL || 'http://localhost:8085';
const cookies = new Map();
async function request(route, options = {}) {
  const response = await fetch(base + route, {
    ...options,
    headers: { Cookie: [...cookies].map(([k,v]) => `${k}=${v}`).join('; '), ...options.headers },
    signal: AbortSignal.timeout(120000),
  });
  for (const cookie of response.headers.getSetCookie()) {
    const [key, ...value] = cookie.split(';')[0].split('=');
    cookies.set(key, value.join('='));
  }
  const json = await response.json();
  if (!response.ok || !json.success) throw new Error(json.message || `HTTP ${response.status}`);
  return json.data;
}
(async () => {
  const local = process.env.ADMIN_PASSWORD ? '' : fs.readFileSync(path.resolve(__dirname, '../../backend/.local/admin-initial-password.txt'), 'utf8');
  const username = process.env.ADMIN_USERNAME || local.match(/^Username:\s*(.+)$/m)?.[1]?.trim();
  const password = process.env.ADMIN_PASSWORD || local.match(/^Password:\s*(.+)$/m)?.[1]?.trim();
  if (!username || !password) throw new Error('Set ADMIN_USERNAME and ADMIN_PASSWORD or use the local initial account.');
  let state = await request('/api/v1/admin/auth/session');
  await request('/api/v1/admin/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', [state.csrfHeader]: state.csrfToken }, body: new URLSearchParams({ username, password }) });
  state = await request('/api/v1/admin/auth/session');
  const result = await request('/api/v1/search/sync', { method: 'POST', headers: { [state.csrfHeader]: state.csrfToken } });
  console.log(JSON.stringify(result, null, 2));
})().catch(error => { console.error(error.message); process.exitCode = 1; });
