const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const React = require('react');
const { renderToStaticMarkup } = require('react-dom/server');
const source = fs.readFileSync(path.join(__dirname, '../src/app/admin/orders/page.tsx'), 'utf8');
const compiled = ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX,
} }).outputText;
function renderOrder(method, status) {
  const fragment = ({ children }) => React.createElement(React.Fragment, null, children);
  const context = { exports: {}, URLSearchParams, require(name) {
    if (name === '@/components/admin/useAdminResource') return { useAdminResource: () => ({
      data: { items: [{ MaDH: 42, MaKH: 1, HoTen: 'QA', NgayTao: '2026-11-14',
        TongTien: 2000, TrangThai: status, NguonDon: 'SALE', PhuongThucThanhToan: method }], totalPages: 1 },
      refresh() {},
    }) };
    if (name === '@/components/admin/AdminUI') return { AdminTitle: fragment, Confirm: fragment, Empty: fragment, Notice: fragment, Pagination: fragment };
    if (name === '@/services/admin-api') return { money: String, datetime: String };
    return require(name);
  } };
  vm.runInNewContext(compiled, context);
  return renderToStaticMarkup(React.createElement(context.exports.default));
}
test('unpaid external payment orders expose neither delivery nor unsupported cancellation', () => {
  for (const method of ['SEPAY', 'MOMO_SANDBOX']) {
    const html = renderOrder(method, 'PENDING');
    assert.doesNotMatch(html, />Đã giao</);
    assert.doesNotMatch(html, />Hủy đơn</);
    assert.match(html, /đối soát/);
  }
});
test('COD pending orders retain delivery and cancellation actions', () => {
  const html = renderOrder('COD', 'PENDING');
  assert.match(html, />Đã giao</);
  assert.match(html, />Hủy đơn</);
});
test('paid external payment orders retain delivery without wallet refund actions', () => {
  const html = renderOrder('SEPAY', 'PAID');
  assert.match(html, />Đã giao</);
  assert.doesNotMatch(html, />Hủy đơn</);
});
