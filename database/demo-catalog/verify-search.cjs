const assert = require('node:assert/strict');
const cases = require('./search-cases.json');
const products = require('./products.json');
const fs = require('node:fs');
const base = process.env.DEMO_API_URL || 'http://localhost:8085';
async function get(route) {
  const response = await fetch(base + route, { signal: AbortSignal.timeout(30000) });
  const json = await response.json();
  assert.ok(response.ok && json.success, json.message);
  return json.data;
}
(async () => {
  let passed = 0;
  const results = [];
  for (const test of cases) {
    for (const mode of ['keyword', 'hybrid']) {
      const data = await get(mode === 'keyword'
        ? '/api/v1/products?' + new URLSearchParams({ keyword: test.query, size: '100' })
        : '/api/v1/search/semantic?' + new URLSearchParams({ query: test.query, limit: '100' }));
      const names = (mode === 'keyword' ? data.items : data.products).map(p => p.tenSP);
      const ok = names.includes(test.expected) && test.excluded.every(name => !names.includes(name));
      results.push({ query: test.query, mode, passed: ok, expected: test.expected, names });
      if (ok) passed++;
      else console.error('FAIL', mode, test.query, JSON.stringify(names));
    }
  }
  for (const p of products) {
    const data = await get('/api/v1/products?' + new URLSearchParams({ keyword: p.name, size: '100' }));
    const parent = data.items.find(x => x.tenSP === p.name);
    assert.ok(parent, 'Missing family ' + p.name);
    const types = await get(`/api/v1/products/${parent.maSP}/types`);
    for (const pack of p.packs) {
      const type = types.find(x => x.tenLoai === pack.label);
      assert.ok(type, `Missing ${p.name}: ${pack.label}`);
      assert.equal(type.giaGoc, pack.price, `${p.name}: ${pack.label} price`);
      assert.equal(type.donViTinh, pack.unit, `${p.name}: ${pack.label} unit`);
      assert.ok(type.soLuongTon > 0, `${p.name}: ${pack.label} stock`);
    }
  }
  const summary = { searchPassed: passed, searchTotal: cases.length * 2, families: products.length, variants: products.reduce((n,p) => n+p.packs.length, 0), results };
  if (process.env.DEMO_RESULTS_PATH) fs.writeFileSync(process.env.DEMO_RESULTS_PATH, JSON.stringify(summary, null, 2));
  console.log(JSON.stringify({ searchPassed: passed, searchTotal: cases.length * 2, families: summary.families, variants: summary.variants }));
  assert.equal(passed, cases.length * 2, 'Some primary search results failed expectations; inspect results.');
})().catch(error => { console.error(error.message); process.exitCode = 1; });
