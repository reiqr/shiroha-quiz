const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '..', 'app.js'), 'utf8');
const testSource = source.replace(
  /\ninit\(\);\s*\n\}\)\(\);/,
  '\nglobalThis.testApi = { normalizeQuestion, normalizeAnswer, parseTextQuestions, sameAnswerForQuestion, toNativeQuestionType, serializeQuestionForCrossExportV53, feedbackAnswerHtmlV58914, renderRecordAnswerCellV58914, questionHtml, splitAnswer };\n})();'
);
assert.notEqual(testSource, source, 'Could not expose current Web parser functions');
const context = {
  localStorage: { getItem: () => null },
  console,
  document: { getElementById: () => ({}) },
  window: {},
  crypto: require('node:crypto').webcrypto,
  TextDecoder,
  Blob,
  URL,
  btoa,
  setTimeout: () => 0,
};
vm.createContext(context);
vm.runInContext(testSource, context, { filename: 'app.js' });
const api = context.testApi;
const plain = value => JSON.parse(JSON.stringify(value));

const options = 'ABCDEFGH'.split('').map(key => ({ key, text: `选项${key}` }));
const eight = api.normalizeQuestion({ type: 'single', question: '八选一？', options, answer: 'H' });
assert.equal(eight.options.length, 8);
assert.deepEqual(plain(eight.answer), ['H']);
assert.deepEqual(plain(api.normalizeAnswer(api.splitAnswer('FH'), options, 'multiple')), ['F', 'H']);
assert.deepEqual(plain(api.normalizeAnswer(api.splitAnswer('F,H'), options, 'multiple')), ['F', 'H']);
assert.deepEqual(plain(api.normalizeAnswer(api.splitAnswer('f'), options, 'single')), ['F']);
assert.deepEqual(plain(api.normalizeAnswer(api.splitAnswer('f'), [], 'judge')), ['B']);
assert.deepEqual(plain(api.normalizeAnswer(api.splitAnswer('False'), [], 'judge')), ['B']);

const original = api.normalizeQuestion({
  id: 'native-cloze', type: 'CLOZE', question: 'The sky is (1) ____ and grass is (2) ____.',
  clozeItems: [
    { number: 1, options: [{ key: 'A', text: 'blue' }, { key: 'B', text: 'red' }], answer: 'A', analysis: 'Sky.' },
    { number: 2, options: [{ key: 'A', text: 'blue' }, { key: 'B', text: 'green' }], answer: 'B', analysis: 'Grass.' },
  ],
  inkStrokes: [{ anchor: 'stem', points: [{ x: 1, y: 2 }] }],
});
assert.equal(original.type, 'cloze');
assert.equal(api.toNativeQuestionType(original.type), 'CLOZE');
assert.deepEqual(plain(original.answer), ['A', 'B']);
assert.equal(original.inkStrokes.length, 1);
const exported = api.serializeQuestionForCrossExportV53(original);
assert.equal(exported.type, 'CLOZE');
assert.deepEqual(plain(exported.clozeItems), plain(original.clozeItems));
assert.deepEqual(plain(exported.inkStrokes), plain(original.inkStrokes));
assert.equal(api.sameAnswerForQuestion(original, ['A', ''], original.answer), false);
assert.equal(api.sameAnswerForQuestion(original, ['A', 'B'], original.answer), true);
assert.match(api.feedbackAnswerHtmlV58914(original, ['A', '']), /第2空/);
const record = { type: 'cloze', chosen: ['A', ''], answer: original.answer, nativeQuestion: original };
assert.match(api.renderRecordAnswerCellV58914(record, 'user'), /第2空：<\/b>未答/);
assert.match(api.renderRecordAnswerCellV58914(record, 'answer'), /第2空：<\/b>B/);
assert.match(api.questionHtml(original), /data-cloze-index="1"/);
assert.deepEqual(plain(api.normalizeQuestion(original).clozeItems), plain(original.clozeItems));

const text = [
  '二、完形填空',
  'In the story (1) _____ something happened. (2) _____ changed later.',
  '1. A. red B. blue C. green D. white',
  '2. A. red B. blue C. green D. white',
  '答案：1.A 2.B',
].join('\n');
const parsed = api.parseTextQuestions(text);
assert.equal(parsed.length, 1);
assert.equal(parsed[0].type, 'cloze');
assert.deepEqual(plain(parsed[0].clozeItems.map(item => item.answer)), ['A', 'B']);
assert.equal(parsed[0].clozeItems[1].options.length, 4);
assert.throws(() => api.parseTextQuestions('二、完形填空\nOnly text'), /未完整识别/);

const choiceText = [
  '一、单选题', '1. 请选择最后一个选项。',
  ...options.map(option => `${option.key}. ${option.text}`),
  '答案：H',
].join('\n');
const choices = api.parseTextQuestions(choiceText);
assert.equal(choices.length, 1);
assert.equal(choices[0].options.length, 8);
assert.deepEqual(plain(choices[0].answer), ['H']);
const compactChoice = api.parseTextQuestions(`一、单选题\n1. 请选择最后一个选项。\n${options.map(option => `${option.key}. ${option.text}`).join(' ')}\n答案：H`);
assert.equal(compactChoice.length, 1);
assert.equal(compactChoice[0].options.length, 8);
assert.deepEqual(plain(compactChoice[0].answer), ['H']);
const mixed = api.parseTextQuestions(`${choiceText}\n${text}`);
assert.equal(mixed.length, 2);
assert.deepEqual(plain(mixed.map(question => question.type)), ['single', 'cloze']);

console.log('Web A-H and cloze checks passed');
