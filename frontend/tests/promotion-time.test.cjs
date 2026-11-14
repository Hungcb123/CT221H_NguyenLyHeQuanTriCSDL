const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const ts = require('typescript');
const source = ts.transpileModule(fs.readFileSync(`${__dirname}/../src/services/promotion-time.ts`, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS }}).outputText;
const context={exports:{},Date};vm.runInNewContext(source,context);
const {promotionDate,promotionInput,promotionUtc}=context.exports;
test('SQL timestamps always represent UTC, including explicit offset input',()=> {
  assert.equal(promotionDate('2026-11-14T10:00:00').toISOString(),'2026-11-14T10:00:00.000Z');
  assert.equal(promotionDate('2026-11-14T17:00:00+07:00').toISOString(),'2026-11-14T10:00:00.000Z');
  assert.equal(promotionDate('2026-11-14T10:00:00Z').toISOString(),'2026-11-14T10:00:00.000Z');
});
test('admin datetime-local roundtrip preserves the same campaign instant',()=> {
  const raw='2026-11-14T10:15:00';
  assert.equal(promotionUtc(promotionInput(raw)),raw);
});
