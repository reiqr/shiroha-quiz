const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const source=fs.readFileSync(path.join(__dirname,'..','app.js'),'utf8');
const testSource=source.replace(/\ninit\(\);\s*\n\}\)\(\);/, `
globalThis.api={state,submitPractice,bindPracticeAutoSubmit,navigatePractice,canNavigatePractice,
 schedulePracticeAutoNext,cancelPracticeAutoNext,practiceItemKeyV8916,
 getPractice(){return practice},setPractice(value){practice=value},
 prepare(){
  collectAnswer=()=>globalThis.chosen;
  markOptions=()=>{};showAnsweredStateV26=()=>{};refreshPracticeStatsV30=()=>{};
  renderStats=()=>{};scoreOf=()=>1;
  renderPracticeQuestion=()=>globalThis.renders++;
  finishPractice=()=>{cancelPracticeAutoNext();globalThis.finishes++};
 }
};
})();`);
assert.notEqual(source,testSource);
const timers=new Map();let timerId=0;const inputs=[];
const context={console,window:{},chosen:['A'],renders:0,finishes:0,
 document:{body:{classList:{contains:()=>true}},querySelector:()=>null,
   querySelectorAll:selector=>selector==='#practice-card .option input'?inputs:[]},
 localStorage:{getItem:()=>null,setItem:()=>{}},
 crypto:require('node:crypto').webcrypto,TextDecoder,Blob,URL,btoa,
 setTimeout:(fn,delay)=>{assert.equal(delay,800);timers.set(++timerId,fn);return timerId},
 clearTimeout:id=>timers.delete(id)};
vm.createContext(context);vm.runInContext(testSource,context);
const api=context.api;api.prepare();
const question=(type='single',id='q')=>({bankId:'bank',question:{id,type,question:'test',options:[{key:'A',text:'yes'},{key:'B',text:'no'}],answer:['A']}});
function setup(type='single',prefs={}){
 api.cancelPracticeAutoNext();context.chosen=['A'];inputs.length=0;
 api.state.settings.webStudy=prefs;api.state.wrongBook={};
 const items=[question(type),question('single','q2')];
 api.setPractice({items,idx:0,answered:0,correct:0,wrong:0,answerState:{},details:[],start:Date.now()});
 return items[0];
}
function tick(){const callbacks=[...timers.values()];timers.clear();callbacks.forEach(fn=>fn())}
let item=setup();api.submitPractice(item,false);
assert.equal(api.getPractice().answered,1);assert.equal(timers.size,0);
api.submitPractice(item,false);assert.equal(api.getPractice().answered,1);
item=setup('single',{autoNext:true});api.submitPractice(item,false);
assert.equal(timers.size,1);tick();assert.equal(api.getPractice().idx,1);
item=setup('single',{autoNext:true});context.chosen=['B'];api.submitPractice(item,false);
assert.equal(timers.size,0);assert.equal(api.getPractice().wrong,1);
item=setup('single',{autoNext:true});api.submitPractice(item,true);assert.equal(timers.size,0);
item=setup('single',{autoNext:true});api.getPractice().items=[item];api.submitPractice(item,false);
assert.equal(timers.size,0);assert.equal(context.finishes,0);
item=setup('single',{autoNext:true});api.submitPractice(item,false);api.cancelPracticeAutoNext();tick();
assert.equal(api.getPractice().idx,0);
item=setup('single',{autoNext:true});api.submitPractice(item,false);
api.setPractice({...api.getPractice()});tick();assert.equal(api.getPractice().idx,0);
item=setup('single',{autoNext:true});api.submitPractice(item,false);
api.state.settings.webStudy.autoNext=false;tick();assert.equal(api.getPractice().idx,0);
item=setup('single',{nextRequiresResult:true});
assert.equal(api.canNavigatePractice(1),false);api.navigatePractice(1);assert.equal(api.getPractice().idx,0);
api.submitPractice(item,true);assert.equal(api.canNavigatePractice(1),true);api.navigatePractice(1);
assert.equal(api.getPractice().idx,1);assert.equal(api.canNavigatePractice(0),true);
assert.equal(api.canNavigatePractice(2),false);api.navigatePractice(2);assert.equal(context.finishes,0);
item=setup('multiple',{autoSubmit:true});
let listener=null;inputs.push({addEventListener:(_,fn)=>listener=fn});
api.bindPracticeAutoSubmit(item.question,item);assert.equal(listener,null);
for(const type of ['single','judge']){
 item=setup(type,{autoSubmit:true});inputs.push({addEventListener:(_,fn)=>listener=fn});
 api.bindPracticeAutoSubmit(item.question,item);listener();listener();
 assert.equal(api.getPractice().answered,1);
}
item=setup('single');api.navigatePractice(1);
assert.equal(api.getPractice().answerState[api.practiceItemKeyV8916(item)].chosen[0],'A');
console.log('Web study interaction: submission, auto-next races, guards and draft navigation passed');
