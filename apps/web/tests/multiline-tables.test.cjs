const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '..', 'app.js'), 'utf8');
const testSource = source.replace(/\ninit\(\);\s*\n\}\)\(\);/, `
globalThis.api = {normalizeQuestion, cleanImportedQuestion, cleanQuestionStemAndAnswer,
  normalizeBackupPayloadV23, serializeQuestionForCrossExportV53, parseStructuredExamText,
  parseTextQuestions, parseBlock, parseDocxMarkdownTableBlockV55, renderQuestionContent};
})();`);
assert.notEqual(testSource, source, 'Could not expose current Web functions');
const context = {
  console, window: {},
  document: {getElementById: () => ({}), head: {appendChild() {}}},
  localStorage: {getItem: () => null}, crypto: require('node:crypto').webcrypto,
  TextDecoder, Blob, URL, btoa, setTimeout: () => 0,
};
vm.createContext(context);
vm.runInContext(testSource, context, {filename: 'app.js'});
const api = context.api;
const plain = value => JSON.parse(JSON.stringify(value));
const table = [
  '【DOCX表格开始】',
  '| Item | Value |',
  '| --- | --- |',
  '| Temperature | 25 |',
  '| Pressure | 10 |',
  '【DOCX表格结束】',
].join('\n');
const stem = 'Use the following table:\n' + table + '\nWhich temperature is shown?';
const raw = {
  id: 'q1', type: 'single', question: stem,
  options: [{key: 'A', text: '25'}, {key: 'B', text: '30'}],
  answer: ['A'], analysis: table,
};
function checkTable(question) {
  assert.equal(question.question, stem);
  assert.deepEqual(plain(question.answer), ['A']);
  const rows = plain(api.parseDocxMarkdownTableBlockV55(question.question));
  assert.deepEqual(rows, [['Item', 'Value'], ['Temperature', '25'], ['Pressure', '10']]);
  assert.match(api.renderQuestionContent(question.question), /<table class="q-table">/);
  assert.match(api.renderQuestionContent(question.question), /<td>Temperature<\/td>/);
}

const normalized = api.normalizeQuestion(raw);
checkTable(normalized);
checkTable(api.normalizeQuestion(normalized));
checkTable(api.cleanImportedQuestion(normalized));
assert.equal(api.parseDocxMarkdownTableBlockV55(normalized.analysis).length, 3);
for (const payload of [
  {questions: [raw]},
  {banks: [{id: 'bank1', name: 'Fixture', questions: [raw]}]},
  {kind: 'shiroha_quiz_backup', banks: [{id: 'bank1', name: 'Fixture', questions: [api.serializeQuestionForCrossExportV53(normalized)]}]},
]) {
  const restored = api.normalizeBackupPayloadV23(JSON.parse(JSON.stringify(payload)), 'fixture.json');
  checkTable(restored.banks[0].questions[0]);
}

const text = '1. ' + stem + '\nA. 25\nB. 30\n答案：A\n解析：Read the temperature row.';
for (const questions of [api.parseStructuredExamText(text), api.parseTextQuestions(text), [api.parseBlock({lines: text.split('\n')}, 0)]]) {
  assert.equal(questions.length, 1);
  checkTable(questions[0]);
}

const multiline = 'First paragraph\n\n```bash\n\tprintf "hello"\n  echo done\n```';
assert.equal(api.normalizeQuestion({...raw, type: 'short', options: [], answer: ['text'], question: multiline}).question, multiline);
assert.equal(api.normalizeQuestion({...raw, question: stem.replace(/\n/g, '\r\n')}).question, stem);
assert.equal(api.cleanQuestionStemAndAnswer('Choose (A)', [], 'single', raw.options).answer.join(''), 'A');
assert.equal(api.cleanQuestionStemAndAnswer('Choose\n答案：B', [], 'single', raw.options).question, 'Choose');
assert.equal(api.cleanQuestionStemAndAnswer('Choose\n答案：B', [], 'single', raw.options).answer.join(''), 'B');
assert.equal(api.normalizeQuestion(raw).normalized, api.normalizeQuestion({...raw, question: stem.replace(/\n/g, ' ')}).normalized);
console.log('Web multiline tables: normalization, import, backup roundtrip, rendering and answer cleanup passed');
