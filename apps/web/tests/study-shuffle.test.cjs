const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const source=fs.readFileSync(path.join(__dirname,'..','app.js'),'utf8');
const testSource=source.replace(/\ninit\(\);\s*\n\}\)\(\);/, `
globalThis.api={sessionOptionOrder,displayAnswerKeys,questionHtml,feedbackAnswerHtmlV58914,
 sameAnswerForQuestion,makeAnswerDetail,
 prepare(){shuffle=values=>[...values].reverse();renderQuestionBodyV83=q=>q.question;
 renderQuestionContent=value=>value;displayOptionTextV589=(q,o)=>o.text;scoreOf=()=>1}
};
})();`);
assert.notEqual(testSource,source);
const context={console,window:{},document:{querySelector:()=>null,querySelectorAll:()=>[]},
 localStorage:{getItem:()=>null,setItem:()=>{}},crypto:require('node:crypto').webcrypto,
 TextDecoder,Blob,URL,btoa,setTimeout,clearTimeout};
vm.createContext(context);vm.runInContext(testSource,context);
const api=context.api;api.prepare();
const q={id:'q1',type:'single',question:'Eight options',analysis:'Original H',answer:['H'],
 options:[...'ABCDEFGH'].map(key=>({key,text:`Original ${key}`}))};
const item={bankId:'bank1',bankName:'Fixture',question:q};
const original=JSON.stringify(q),session={shuffleOptions:true};
const order=api.sessionOptionOrder(session,item);
assert.equal(Array.from(order).join(''),'HGFEDCBA');
assert.equal(api.sessionOptionOrder(session,item),order,'Re-render must retain the same mapping');
assert.notEqual(api.sessionOptionOrder(session,{...item,bankId:'bank2'}),order,'Same ID in another bank needs a separate mapping');
assert.equal(api.sessionOptionOrder({shuffleOptions:false},item),null);
for(const type of ['judge','cloze','blank','short'])assert.equal(api.sessionOptionOrder(session,{...item,question:{...q,type}}),null);
const html=api.questionHtml(q,false,0,order);
assert.match(html,/data-key="H"><input[^>]*value="H"><span class="option-key">A\.<\/span><span class="option-text">Original H/);
assert.match(html,/显示选项 → 原题选项：A → H/);
assert.equal(api.displayAnswerKeys(q,['H'],order).join(''),'A');
assert.match(api.feedbackAnswerHtmlV58914(q,['H'],order),/你的答案：A｜参考答案：A/);
assert.equal(api.sameAnswerForQuestion(q,['H'],q.answer),true);
assert.equal(api.sameAnswerForQuestion(q,['A'],q.answer),false);
const multi={...q,type:'multiple',answer:['F','H']};
assert.equal(api.displayAnswerKeys(multi,['F','H'],order).join(''),'AC');
assert.match(api.questionHtml(multi,true,1,order),/type="checkbox"/);
assert.equal(api.sameAnswerForQuestion(multi,['H','F'],multi.answer),true);
const detail=api.makeAnswerDetail(item,['H'],true,1,1);
assert.equal(detail.chosen.join(''),'H');assert.equal(detail.answer.join(''),'H');
assert.equal(detail.nativeQuestion.options[0].key,'A');
assert.equal(JSON.stringify(q),original,'Rendering and scoring must never mutate source data');
assert.match(api.questionHtml(q,false),/data-key="A"><input[^>]*value="A"><span class="option-key">A\./);
assert.doesNotMatch(api.questionHtml(q,false),/显示选项 →/);
console.log('Web study shuffle: stable A-H mapping, grading, feedback and original records passed');
