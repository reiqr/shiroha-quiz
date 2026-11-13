const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '..', 'app.js'), 'utf8');
const testSource = source.replace(/\ninit\(\);\s*\n\}\)\(\);/, `
globalThis.api = { normalizeQuestion, serializeQuestionForCrossExportV53,
  stripEditedImagesV391, prioritizeUnansweredPracticeItemsV391,
  testRollback(bank, draft) {
    state.banks=[bank]; importCache=[draft];
    bankEditSessionV45={bankId:bank.id, insertedIds:new Set()};
    showNotice=()=>{}; readImportBankGroupV58=()=> 'new-group';
    saveBankEditSessionV45();
    return {bank, draft:importCache[0], session:!!bankEditSessionV45};
  }
};
})();`);
assert.notEqual(source, testSource);
const context = {
  console, window: {}, document: {querySelector: () => null},
  localStorage: {getItem: () => null, setItem: () => {throw Object.assign(new Error('full'), {name:'QuotaExceededError'});}},
  crypto: require('node:crypto').webcrypto, TextDecoder, Blob, URL, btoa, setTimeout: () => 0,
};
vm.createContext(context);
vm.runInContext(testSource, context);
const api = context.api;
const plain = value => JSON.parse(JSON.stringify(value));
const items = ['a', 'b', 'c', 'd'].map(id => ({bankId:'bank', question:{id}}));
const records = [{bankId:'bank', details:[
  {questionId:'a', chosen:['A']},
  {questionId:'b', chosen:[], userAnswer:['B']},
  {questionId:'c', chosen:[]},
  {questionId:'d', sourceBankId:'other', chosen:['A']},
]}];
const ordered = api.prioritizeUnansweredPracticeItemsV391(items, records);
assert.deepEqual(plain(ordered.slice(0,2).map(item=>item.question.id)).sort(), ['c','d']);
assert.equal(new Set(ordered.map(item=>item.question.id)).size, 4);
assert.deepEqual(items.map(item=>item.question.id), ['a','b','c','d']);
assert.equal(api.prioritizeUnansweredPracticeItemsV391([],records).length,0);

const imageA='data:image/png;base64,AAAA';
const imageB='data:image/png;base64,BBBB';
const question = api.normalizeQuestion({id:'q',type:'CLOZE',question:'Passage (1) __ (2) __',
  images:[{id:'b',dataUrl:imageB,order:1},{id:'a',dataUrl:imageA,order:2}],
  clozeItems:[1,2].map(number=>({number,options:[{key:'A',text:'yes'},{key:'B',text:'no'}],answer:'A'})),
  inkStrokes:[{anchor:'stem',points:[{x:1,y:2}]}]});
const exported=api.serializeQuestionForCrossExportV53(question);
const restored=api.normalizeQuestion(exported);
assert.deepEqual(plain(restored.images.map(image=>image.dataUrl)),[imageB,imageA]);
assert.deepEqual(plain(restored.clozeItems),plain(question.clozeItems));
assert.deepEqual(plain(restored.inkStrokes),plain(question.inkStrokes));
const stripped=api.stripEditedImagesV391(question.question,question.images);
assert.ok(!stripped.includes('data:image'));
const removed=api.normalizeQuestion({...question,question:stripped,images:[]});
assert.equal((api.serializeQuestionForCrossExportV53(removed).images||[]).length,0);
const reordered=api.normalizeQuestion({...question,question:stripped,images:[question.images[1],question.images[0]]});
assert.deepEqual(plain(api.serializeQuestionForCrossExportV53(reordered).images.map(image=>image.dataUrl)),[imageA,imageB]);

const oldQuestion={id:'old',type:'short',question:'old',answer:['answer'],options:[]};
const bank={id:'bank',name:'old-name',groupName:'old-group',updatedAt:'old-time',questions:[oldQuestion]};
const draft={...oldQuestion,question:'new'};
const result=api.testRollback(bank,draft);
assert.equal(bank.questions[0],oldQuestion);
assert.equal(bank.name,'old-name');
assert.equal(bank.groupName,'old-group');
assert.equal(bank.updatedAt,'old-time');
assert.equal(result.draft,draft);
assert.equal(result.session,true);
console.log('Web editor images, backup fidelity, storage rollback and unanswered priority checks passed');
