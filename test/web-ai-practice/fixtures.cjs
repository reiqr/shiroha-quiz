'use strict';

const MOCK_ENDPOINT = 'https://mock-ai.invalid/v1/chat/completions';
const FAKE_KEY = 'sk-web-ai-practice-FAKE-ONLY-NOT-A-SECRET';
const MODEL = 'web-ai-practice-mock-model';
const BANK_ID = 'web-ai-practice-safe-bank';
const IMAGE = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=';
const OLD_RICH_ANALYSIS = '\u3010DOCX\u8868\u683c\u5f00\u59cb\u3011\n| OLD_RICH_ANALYSIS | Value |\n| --- | --- |\n| Original explanation | 4 |\n\u3010DOCX\u8868\u683c\u7ed3\u675f\u3011';
const FORMULA_ANALYSIS = String.raw`FORMULA_SAVE_MARKER: \(\frac{8}{2}=4\), \(\sqrt{16}=4\), and \(\alpha+\beta=\gamma\).`;

function fixture({ configured = true, rich = false } = {}) {
  const base = { category: 'E2E safe fixture', analysis: 'Original local explanation.', images: [], score: 1 };
  const questions = [
    { ...base, id: 'safe-single-1', type: 'single', question: 'SAFE_Q1: What is 2 + 2?', options: [{ key: 'A', text: '4' }, { key: 'B', text: '5' }], answer: ['A'] },
    { ...base, id: 'safe-single-2', type: 'single', question: 'SAFE_Q2: What is 3 + 3?', options: [{ key: 'A', text: '7' }, { key: 'B', text: '6' }], answer: ['B'] },
    { ...base, id: 'safe-multi-blank', type: 'blank', question: 'SAFE_BLANK: Name the first two positive integers: ___, ___.', options: [], answer: ['1;2'], blankAnswers: [['1', 'one'], ['2', 'two']] },
    { ...base, id: 'safe-short', type: 'short', question: 'SAFE_SHORT: Explain why a test mock avoids a real service.', options: [], answer: ['It returns a controlled response without a real API.'] },
    { ...base, id: 'safe-image', type: 'single', question: 'SAFE_IMAGE: Select the value visible ONLY in this picture.', options: [{ key: 'A', text: '4' }, { key: 'B', text: '5' }], answer: ['A'], images: [{ id: 'safe-pixel', dataUrl: IMAGE, sourceName: 'safe-pixel.png', order: 1 }] },
  ].map((question, index) => ({ ...question, number: index + 1 }));
  if (rich) {
    questions[0].question = String.raw`SAFE_Q1: Evaluate \(\frac{8}{2}\).`;
    questions[0].options = [{ key: 'A', text: String.raw`\(4\)` }, { key: 'B', text: String.raw`\(5\)` }];
    questions[0].analysis = OLD_RICH_ANALYSIS;
    questions[0].richContent = {
      schema: 'shiroha-web-rich-v1', features: ['latex_formula', 'docx_table'],
      fields: {
        question: { text: questions[0].question, source: 'question', features: ['latex_formula'] },
        options: questions[0].options.map(option => ({ text: option.text, source: 'option', features: ['latex_formula'] })),
        analysis: { text: OLD_RICH_ANALYSIS, source: 'analysis', features: ['docx_table'] },
      },
    };
  }
  return {
    schemaVersion: 1,
    banks: [{ id: BANK_ID, name: 'Web AI E2E synthetic bank', groupName: 'E2E', createdAt: '2026-01-01T00:00:00.000Z', updatedAt: '2026-01-01T00:00:00.000Z', questions }],
    activeBankId: BANK_ID, wrongBook: {}, favorites: {},
    records: [{
      id: 'safe-history-record', mode: '练习', bankId: BANK_ID, bankName: 'Synthetic historical practice',
      total: 1, answered: 1, correct: 1, wrong: 0, accuracy: 100, date: '2026-01-02T00:00:00.000Z', duration: 1,
      details: [{ questionId: questions[0].id, question: questions[0].question, type: 'single', chosen: ['A'], answer: ['A'], correct: true, nativeQuestion: JSON.parse(JSON.stringify(questions[0])) }],
    }],
    crossPlatformMeta: { favoriteQuestions: {} },
    settings: {
      suppressDefaultBank: true,
      practiceScope: { type: 'bank', value: BANK_ID },
      practiceProgressV58916: {},
      aiImportV99: { provider: 'custom', endpoint: configured ? MOCK_ENDPOINT : '', model: configured ? MODEL : '', timeoutSeconds: 10, rememberKey: false },
    },
  };
}

function analysis(text, extra = {}) {
  return { suggestedAnswer: 'A', matchesLocalAnswer: true, analysis: text, confidence: 'HIGH', needsReview: false, warning: '', ...extra };
}

module.exports = { fixture, analysis, MOCK_ENDPOINT, FAKE_KEY, MODEL, BANK_ID, IMAGE, OLD_RICH_ANALYSIS, FORMULA_ANALYSIS };
