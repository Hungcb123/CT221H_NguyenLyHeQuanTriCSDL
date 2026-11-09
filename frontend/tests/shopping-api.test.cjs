const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const ts = require('typescript');
const source = ts.transpileModule(fs.readFileSync(`${__dirname}/../src/services/api.ts`, 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
}).outputText;
function service(fetch) {
  const context = { exports: {}, process: { env: {} }, fetch, URLSearchParams };
  vm.runInNewContext(source, context);
  return context.exports.apiService;
}
const ok = (data) => ({ ok: true, json: async () => ({ success: true, statusCode: 1, data }) });

test('cart maps backend items and stock, including an empty cart', async () => {
  const api = service(async () => ok({ maKH: 1, tongTien: 60000, tongSoLuong: 2, items: [{ maSP: 7, soLuong: 2, soLuongTon: 3 }] }));
  const cart = await api.getCart();
  assert.equal(cart.danhSachMon[0].maSP, 7);
  assert.equal(cart.danhSachMon[0].soLuongTonHienTai, 3);
  assert.equal((await service(async () => ok({ items: [] })).getCart()).danhSachMon.length, 0);
});
test('adding multiple units sends one request with the exact backend DTO', async () => {
  let calls = 0;
  const api = service(async (url, init) => {
    calls++;
    assert.equal(url, '/api/v1/cart/items');
    assert.deepEqual(JSON.parse(init.body), { maKH: 1, maSP: 7, soLuong: 3 });
    return ok(null);
  });
  await api.addToCart({ customerId: 1, productId: 7, quantity: 3 });
  assert.equal(calls, 1);
});
test('removing a product sends the backend DELETE DTO', async () => {
  await service(async (url, init) => {
    assert.equal(init.method, 'DELETE');
    assert.deepEqual(JSON.parse(init.body), { maKH: 1, maSP: 7 });
    return ok(null);
  }).removeFromCart(1, 7);
});
test('checkout sends delivery fields and preserves a real zero wallet balance', async () => {
  const result = { maDH: 42, statusCode: 1, tongTien: 60000, soDuConLai: 0 };
  const api = service(async (url, init) => {
    assert.equal(url, '/api/v1/orders/checkout');
    assert.deepEqual(JSON.parse(init.body), { maKH: 1, diaChiGiaoHang: 'Cần Thơ', ghiChu: 'Gọi trước' });
    return ok(result);
  });
  assert.deepEqual(await api.checkout(' Cần Thơ ', ' Gọi trước '), result);
});
test('HTTP 200 with a rejected transaction never becomes a successful order', async () => {
  const api = service(async () => ok({ statusCode: -2, message: 'Số dư ví không đủ' }));
  await assert.rejects(api.checkout('Cần Thơ', ''), /Số dư ví không đủ/);
});
test('API errors never become mock carts, orders, categories, products or bot replies', async () => {
  const api = service(async () => ({ ok: false, json: async () => ({ success: false, message: 'Hệ thống đang bận' }) }));
  for (const run of [() => api.getCart(), () => api.getCategories(), () => api.addToCart({ customerId: 1, productId: 7, quantity: 1 }), () => api.checkout('Cần Thơ', ''), () => api.chatWithBot({ message: 'mua canh chua' }), () => api.createProduct({})]) {
    await assert.rejects(run(), /Hệ thống đang bận/);
  }
});
test('network failures and malformed cart responses surface an error', async () => {
  await assert.rejects(service(async () => { throw new Error('offline'); }).getCart(), /Không kết nối/);
  await assert.rejects(service(async () => ok({})).getCart(), /không hợp lệ/);
});
