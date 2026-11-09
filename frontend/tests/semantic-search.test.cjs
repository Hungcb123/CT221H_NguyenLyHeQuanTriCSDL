const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

// Exercise the actual API service without adding a browser/test-runner dependency.
const source = fs.readFileSync(path.join(__dirname, '../src/services/api.ts'), 'utf8');
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
}).outputText;

function serviceWith(fetch) {
  const context = { exports: {}, process: { env: {} }, fetch, URLSearchParams };
  vm.runInNewContext(compiled, context);
  return context.exports.apiService;
}

const product = {
  maSP: 7, tenSP: 'Cần tây', gia: 35000, giaGoc: 35000, giaHienTai: 28000,
  dangKhuyenMai: true, phanTramGiam: 20, donViTinh: 'kg', soLuongTon: 3,
  hanSuDung: '2026-10-28T00:00:00', maDanhMuc: 1,
  similarityScore: 0.812, matchType: 'SEMANTIC_VECTOR',
};

const hybrid = (products, mode = 'HYBRID') => ({
  products, relatedProducts: [], mode, rankingMethod: 'EXACT_NAME_THEN_RRF', rrfK: 60, semanticWeight: mode === 'HYBRID' ? 0.5 : 0,
  keywordWeight: mode === 'HYBRID' ? 0.5 : 1,
});

test('semantic search preserves real backend fields, price, stock, and score', async () => {
  const service = serviceWith(async (url) => {
    const parsed = new URL(url, 'http://localhost');
    assert.equal(parsed.searchParams.get('query'), 'rau & trái cây');
    assert.equal(parsed.searchParams.get('limit'), '12');
    return { ok: true, json: async () => ({ success: true, data: hybrid([product]) }) };
  });
  assert.deepEqual(await service.searchSemantic('rau & trái cây', 12), hybrid([product]));
});

test('empty results stay empty instead of becoming mock matches', async () => {
  const service = serviceWith(async () => ({ ok: true, json: async () => ({ success: true, data: hybrid([]) }) }));
  assert.deepEqual(await service.searchSemantic('không khớp'), hybrid([]));
});

test('keyword fallback mode is preserved even when there are no matches', async () => {
  const payload = hybrid([], 'KEYWORD_FALLBACK');
  const service = serviceWith(async () => ({ ok: true, json: async () => ({ success: true, data: payload }) }));
  assert.deepEqual(await service.searchSemantic('không khớp'), payload);
});

test('substitutes stay separate when primary results are empty', async () => {
  const payload = { ...hybrid([]), relatedProducts: [product] };
  const service = serviceWith(async () => ({ ok: true, json: async () => ({ success: true, data: payload }) }));
  const result = await service.searchSemantic('mực ống');
  assert.deepEqual(result.products, []);
  assert.deepEqual(result.relatedProducts, [product]);
});

test('network failures propagate instead of returning mock products', async () => {
  const service = serviceWith(async () => { throw new Error('Network offline'); });
  await assert.rejects(service.searchSemantic('rau'), /Network offline/);
});

for (const [name, response] of [
  ['HTTP failure', { ok: false, json: async () => ({ success: false, message: 'Qdrant offline' }) }],
  ['application failure', { ok: true, json: async () => ({ success: false, data: [] }) }],
  ['invalid data', { ok: true, json: async () => ({ success: true, data: {} }) }],
  ['non-JSON response', { ok: false, json: async () => { throw new SyntaxError('Invalid JSON'); } }],
]) {
  test(`${name} rejects instead of fabricating semantic results`, async () => {
    const service = serviceWith(async () => response);
    await assert.rejects(service.searchSemantic('rau'));
  });
}

test('catalog accepts the real backend items pagination contract', async () => {
  const service = serviceWith(async () => ({ ok: true, json: async () => ({
    success: true, data: { items: [product], currentPage: 2, pageSize: 12, totalRecords: 15, totalPages: 2 },
  }) }));
  const result = await service.getProducts({ page: 2, size: 12 });
  assert.deepEqual(result.products, [product]);
  assert.equal(result.page, 2);
  assert.equal(result.totalPages, 2);
});

test('catalog API failure never fabricates local keyword matches', async () => {
  const service = serviceWith(async () => { throw new Error('SQL unavailable'); });
  await assert.rejects(service.getProducts({ keyword: 'cá' }), /SQL unavailable/);
});
