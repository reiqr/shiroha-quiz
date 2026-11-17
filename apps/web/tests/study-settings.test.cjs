const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '..', 'app.js'), 'utf8');
const testSource = source.replace(/\ninit\(\);\s*\n\}\)\(\);/, `
globalThis.api={studyPreferences,saveStudyPreferences,rememberStudyConfig,restoreStudyConfigs,
  resetStudyPracticeForm,serializeState,normalizeBackupPayloadV23,
  state, setPractice(value){practice=value}, getPractice(){return practice},
  prepare(){toast=(text)=>globalThis.messages.push(text);syncLimitControlV60=()=>{};syncPracticeStartUiV58916=()=>{}}
};
})();`);
assert.notEqual(source, testSource);
const elements = new Map();
let storageFailure = false;
let saved = '';
const timers = new Map();
let timerId = 0;
const context = {
  console, window:{}, messages:[],
  document:{querySelector:selector=>elements.get(selector)||null,querySelectorAll:()=>[]},
  localStorage:{getItem:()=>null,setItem:(_,value)=>{if(storageFailure)throw new Error('disk full');saved=value}},
  crypto:require('node:crypto').webcrypto,TextDecoder,Blob,URL,btoa,
  setTimeout:fn=>{timers.set(++timerId,fn);return timerId},clearTimeout:id=>timers.delete(id),
};
vm.createContext(context);
vm.runInContext(testSource,context);
const api=context.api;
api.prepare();
const plain=value=>JSON.parse(JSON.stringify(value));
assert.equal(api.studyPreferences().autoSubmit,false);
assert.equal(api.studyPreferences().rememberExam,false);
assert.equal(api.studyPreferences().shufflePractice,false);
function input(id,value,type='text'){
  const el={value,type,tagName:'INPUT',checked:false};elements.set('#'+id,el);return el;
}
input('practice-type','multiple');input('practice-source','favorite');input('practice-order','random');
input('practice-limit','custom');input('practice-custom-count','12');
const priority=input('practice-unanswered-priority-v391','', 'checkbox');priority.checked=true;
const start=input('practice-start-mode-v58916','custom');input('practice-number-range-v58916','3-5');
input('exam-name','test');input('exam-count','30');input('exam-minutes','45');
api.state.settings.practiceProgressV58916={existing:{index:8}};
api.saveStudyPreferences({...api.studyPreferences(),rememberPractice:true,rememberExam:true});
api.rememberStudyConfig('practice');api.rememberStudyConfig('exam');
assert.equal(api.studyPreferences().lastPractice['practice-custom-count'],'12');
assert.equal(api.studyPreferences().lastPractice['practice-start-mode-v58916'],undefined);
assert.equal(api.studyPreferences().lastPractice['practice-number-range-v58916'],undefined);
elements.get('#practice-custom-count').value='99';elements.get('#exam-count').value='99';
api.restoreStudyConfigs();
assert.equal(elements.get('#practice-custom-count').value,'12');
assert.equal(elements.get('#exam-count').value,'30');
assert.equal(start.value,'custom');
api.saveStudyPreferences({...api.studyPreferences(),rememberPractice:false});
elements.get('#practice-custom-count').value='17';api.restoreStudyConfigs();
assert.equal(elements.get('#practice-custom-count').value,'17');
api.resetStudyPracticeForm();
assert.equal(elements.get('#practice-custom-count').value,'20');
assert.equal(start.value,'from_start');
assert.equal(api.state.settings.practiceProgressV58916.existing.index,8);
assert.equal(JSON.parse(api.serializeState()).settings.webStudy.rememberExam,true);
const before=JSON.stringify(api.state.settings.webStudy);
storageFailure=true;
assert.equal(api.saveStudyPreferences({...api.studyPreferences(),autoNext:true}),false);
assert.equal(JSON.stringify(api.state.settings.webStudy),before);
assert.match(context.messages.at(-1),/设置未保存/);
storageFailure=false;
assert.ok(saved);
console.log('Web study settings: defaults, remembered forms, backup serialization and save rollback passed');
