(function(){
const KEY='uquiz_state_v8_c1';
const TYPE_LABEL={single:'单选题',multiple:'多选题',multi:'多选题',judge:'判断题',blank:'填空题',short:'简答题',short_answer:'简答题'};
const state=loadState();
let importCache=[];let importWarnings=[];let importReport='';let importDiagnostics=null;let importPreviewFilter='priority';let importSelected=new Set();let exportBankSelectedV23=new Set();let backupImportModeV23='merge';let practice={items:[],idx:0,answered:0,correct:0,wrong:0,start:0};let exam={items:[],answers:{},start:0,timer:null,deadline:0,submitted:false};
const $=s=>document.querySelector(s);const $$=s=>[...document.querySelectorAll(s)];
init();
function init(){upgradeState();ensureDefaultBank();bindNav();bindEvents();renderBankSelect();renderAll();setupEnhancedDataToolsV23();}
function defaultBank(){const qb=window.questionBank||{meta:{title:'空题库'},questions:[]};return {id:'default-c1',name:qb.meta?.title||'默认题库',createdAt:now(),questions:(qb.questions||[]).map(normalizeQuestion)}}
function ensureDefaultBank(){if(!state.banks.length) state.banks.push(defaultBank()); if(!state.activeBankId) state.activeBankId=state.banks[0]?.id;}
function loadState(){try{return JSON.parse(localStorage.getItem(KEY))||{banks:[],activeBankId:'',wrongBook:{},records:[],settings:{}}}catch(e){return{banks:[],activeBankId:'',wrongBook:{},records:[],settings:{}}}}
function upgradeState(){
  state.banks=Array.isArray(state.banks)?state.banks:[];
  state.records=Array.isArray(state.records)?state.records:[];
  state.wrongBook=state.wrongBook&&typeof state.wrongBook==='object'?state.wrongBook:{};
  state.settings=state.settings&&typeof state.settings==='object'?state.settings:{};
  for(const b of state.banks){
    b.questions=(b.questions||[]).map((q,i)=>({...normalizeQuestion(q,i),id:q.id||('q_'+Date.now()+'_'+i),number:q.number||i+1}));
    b.updatedAt=b.updatedAt||b.createdAt||now();
  }
  for(const bid of Object.keys(state.wrongBook)){
    const val=state.wrongBook[bid];
    if(Array.isArray(val)){
      state.wrongBook[bid]=val.map(x=>typeof x==='string'?{id:x,wrongCount:1,rightCount:0,lastWrongAt:'',lastCorrectAt:'',status:'未掌握'}:{id:x.id,wrongCount:Number(x.wrongCount||1),rightCount:Number(x.rightCount||0),lastWrongAt:x.lastWrongAt||'',lastCorrectAt:x.lastCorrectAt||'',status:x.status||'未掌握'});
    }
  }
}
function saveState(){localStorage.setItem(KEY,JSON.stringify(state));toast('已保存到浏览器本地。','ok')}
function now(){return new Date().toISOString()}
function activeBank(){return state.banks.find(b=>b.id===state.activeBankId)||state.banks[0]||{questions:[]}}
function bindNav(){ $$('.nav').forEach(btn=>btn.onclick=()=>{ $$('.nav').forEach(b=>b.classList.remove('active')); btn.classList.add('active'); $$('.view').forEach(v=>v.classList.remove('active')); $('#'+btn.dataset.view).classList.add('active'); $('#page-title').textContent=btn.textContent; renderAll(); });}
function bindEvents(){
$('#active-bank-select').onchange=e=>{state.activeBankId=e.target.value;saveSilent();renderAll()};const importNameInput=$('#import-bank-name');if(importNameInput)importNameInput.addEventListener('input',()=>{importNameInput.dataset.autoName='0'});$('#save-all-btn').onclick=saveState;
$('#load-sample-btn').onclick=loadSample;$('#import-file').onchange=readImportFile;$('#parse-import-btn').onclick=parseImport;$('#confirm-import-btn').onclick=confirmImport;const dualConfirmBtn=$('#dual-confirm-import-btn');if(dualConfirmBtn)dualConfirmBtn.onclick=confirmImport;$('#clear-import-btn').onclick=()=>{$('#import-text').value='';importCache=[];importSelected.clear();importDiagnostics=null;renderImportPreview([])};
$('#dual-question-file').onchange=e=>readDualFile(e,'question');$('#dual-answer-file').onchange=e=>readDualFile(e,'answer');$('#parse-dual-import-btn').onclick=parseDualImport;$('#clear-dual-import-btn').onclick=()=>{$('#dual-question-text').value='';$('#dual-answer-text').value='';importCache=[];importSelected.clear();importDiagnostics=null;renderImportPreview([])};$('#dual-load-sample-btn').onclick=loadDualSample;$('#revalidate-import-btn').onclick=()=>renderImportPreview(importCache);
$('#edit-close-btn').onclick=closeEditModal;$('#edit-save-btn').onclick=saveEditQuestion;$('#edit-delete-btn').onclick=deleteEditQuestion;const pf=$('#import-preview-filter');if(pf)pf.onchange=e=>{importPreviewFilter=e.target.value;renderImportPreview(importCache)};const bid=$('#batch-delete-import-btn');if(bid)bid.onclick=batchDeleteImportSelected;const cis=$('#clear-import-selection-btn');if(cis)cis.onclick=()=>{importSelected.clear();renderImportPreview(importCache)};
$('#dedupe-btn').onclick=dedupeActiveBank;$('#rename-bank-btn').onclick=renameActiveBank;$('#duplicate-bank-btn').onclick=duplicateActiveBank;$('#new-empty-bank-btn').onclick=newEmptyBank;$('#merge-bank-btn').onclick=mergeBankIntoActive;$('#bank-sort-mode').onchange=renderBankList;$('#start-practice-btn').onclick=startPractice;$('#reset-practice-btn').onclick=()=>{exitPracticeFocus();$('#practice-card').innerHTML='<div class="empty">选择条件后点击“开始练习”。</div>';practice={items:[],idx:0,answered:0,correct:0,wrong:0,start:0};$('#practice-progress').textContent='0 / 0'};
$('#start-exam-btn').onclick=startExam;$('#submit-exam-btn').onclick=()=>submitExam(false);$('#clear-wrong-btn').onclick=()=>{if(confirm('确定清空当前题库错题本？')){state.wrongBook[activeBank().id]=[];saveSilent();renderAll()}};
$('#clear-records-btn').onclick=()=>{if(confirm('确定清空全部刷题记录？')){state.records=[];saveSilent();renderAll()}};$('#export-records-btn').onclick=exportRecords;$('#record-mode-filter').onchange=renderRecords;$('#record-limit').onchange=renderRecords;$('#record-refresh-btn').onclick=renderRecords;$('#wrong-status-filter').onchange=renderWrongBook;$('#wrong-sort-mode').onchange=renderWrongBook;$('#practice-wrong-btn').onclick=startWrongPractice;$('#export-json-btn').onclick=exportCurrentBank;$('#export-all-btn').onclick=exportAll;$('#reset-data-btn').onclick=resetData;
}

function cleanImportBankNameFromFile(fileName){
  return String(fileName||'').replace(/\.[^.]+$/,'').trim()||'导入题库';
}
function setImportBankNameFromFile(fileName){
  const inp=$('#import-bank-name');
  if(!inp)return;
  const next=cleanImportBankNameFromFile(fileName);
  const current=inp.value.trim();
  const lastAuto=inp.dataset.autoNameValue||'';
  const canOverwrite=!current || inp.dataset.autoName==='1' || current===lastAuto;
  if(canOverwrite){
    inp.value=next;
    inp.dataset.autoName='1';
    inp.dataset.autoNameValue=next;
  }
}

function saveSilent(){localStorage.setItem(KEY,JSON.stringify(state))}
function renderAll(){renderStats();renderBankSelect();renderMergeSelect();renderBankList();renderBankPreview();renderWrongBook();renderRecords();renderBankInputs();if(typeof renderExportBankSelectorV23==='function')renderExportBankSelectorV23();}
function renderStats(){const b=activeBank();$('#stat-total').textContent=b.questions.length;$('#stat-wrong').textContent=(state.wrongBook[b.id]||[]).length;$('#stat-records').textContent=state.records.length;}
function renderBankSelect(){const sel=$('#active-bank-select');const old=state.activeBankId;sel.innerHTML=state.banks.map(b=>`<option value="${esc(b.id)}">${esc(b.name)}（${b.questions.length}题）</option>`).join('');sel.value=old||state.activeBankId;}
function renderMergeSelect(){const sel=$('#merge-bank-select');if(!sel)return;const current=state.activeBankId;sel.innerHTML=state.banks.filter(b=>b.id!==current).map(b=>`<option value="${esc(b.id)}">${esc(b.name)}（${b.questions.length}题）</option>`).join('')||'<option value="">暂无可合并题库</option>'}
function renderBankInputs(){const inp=$('#bank-rename-input');if(inp&&!inp.value)inp.placeholder='当前：'+(activeBank().name||'未命名题库')}
function renderBankList(){
  const box=$('#bank-list');
  let banks=[...state.banks];
  const sort=$('#bank-sort-mode')?.value||'created';
  if(sort==='name')banks.sort((a,b)=>a.name.localeCompare(b.name,'zh-CN'));
  else if(sort==='count')banks.sort((a,b)=>b.questions.length-a.questions.length);
  else banks.sort((a,b)=>String(b.createdAt||'').localeCompare(String(a.createdAt||'')));
  const validIds=new Set(state.banks.map(b=>b.id));
  exportBankSelectedV23=new Set([...exportBankSelectedV23].filter(id=>validIds.has(id)));
  box.innerHTML=banks.map(b=>{
    const stats=countTypes(b.questions);const active=b.id===state.activeBankId;
    return `<div class="bank-item ${active?'active-bank':''}">
      <label class="bank-bulk-check-v23" title="选择后可批量导出"><input type="checkbox" data-bank-bulk-v23="${esc(b.id)}" ${exportBankSelectedV23.has(b.id)?'checked':''}></label>
      <div><b>${esc(b.name)}${active?'<span class="source-badge">当前</span>':''}</b><p class="muted">${b.questions.length}题｜单选${stats.single}｜多选${stats.multiple+stats.multi}｜判断${stats.judge}｜填空${stats.blank}｜简答${stats.short}｜创建 ${fmt(b.createdAt||now())}</p></div>
      <div class="row-actions"><button class="ghost" data-openbank="${esc(b.id)}">设为当前</button><button class="ghost" data-copybank="${esc(b.id)}">复制</button><button class="ghost" data-exportbank="${esc(b.id)}">导出</button>${b.id==='default-c1'?'':`<button class="ghost danger" data-delbank="${esc(b.id)}">删除</button>`}</div>
    </div>`
  }).join('')||'<p class="muted">暂无题库。</p>';
  $$('[data-bank-bulk-v23]').forEach(x=>x.onchange=()=>{if(x.checked)exportBankSelectedV23.add(x.dataset.bankBulkV23);else exportBankSelectedV23.delete(x.dataset.bankBulkV23);renderExportBankSummaryV23()});
  $$('[data-openbank]').forEach(x=>x.onclick=()=>{state.activeBankId=x.dataset.openbank;saveSilent();renderAll()});
  $$('[data-copybank]').forEach(x=>x.onclick=()=>duplicateBankById(x.dataset.copybank));
  $$('[data-exportbank]').forEach(x=>x.onclick=()=>exportBankById(x.dataset.exportbank));
  $$('[data-delbank]').forEach(x=>x.onclick=()=>{if(confirm('删除该题库？')){state.banks=state.banks.filter(b=>b.id!==x.dataset.delbank);delete state.wrongBook[x.dataset.delbank];exportBankSelectedV23.delete(x.dataset.delbank);state.activeBankId=state.banks[0]?.id||'';saveSilent();renderAll()}});
  renderExportBankSummaryV23();
}
function renderBankPreview(){const qs=activeBank().questions.slice(0,300);$('#bank-preview tbody').innerHTML=qs.map((q,i)=>`<tr><td>${i+1}</td><td>${label(q.type)}</td><td>${esc(short(q.question,80))}</td><td>${esc((q.answer||q.answerKeys||[]).join(''))}</td><td>${esc(q.category||q.topic||'')}</td><td>${esc(q.score||'默认')}</td></tr>`).join('')}
function countTypes(qs){return qs.reduce((a,q)=>{a[q.type]=(a[q.type]||0)+1;return a},{single:0,multiple:0,multi:0,judge:0,blank:0,short:0})}
function label(t){return TYPE_LABEL[t]||t||'未知'}
function normalizeQuestion(q,i=0){
  let type=normalizeType(q.type||q.questionType||q.kind||'');
  let rawAnswer=q.answer??q.answerKeys??q.correctAnswer??q.correct??q.rightAnswer??q.referenceAnswer??q.standardAnswer??[];
  let answer=Array.isArray(rawAnswer)?rawAnswer:(isTextType(type)?splitTextAnswer(rawAnswer):splitAnswer(rawAnswer));
  let options=[];
  if(Array.isArray(q.options)){
    options=q.options.map((o,j)=>typeof o==='string'?{key:String.fromCharCode(65+j),text:o}:{key:normalizeOptionKey(o.key||o.label||o.value||String.fromCharCode(65+j)),text:String(o.text||o.content||o.label||'').trim()}).filter(o=>o.text);
  }else if(Array.isArray(q.choices)){
    options=q.choices.map((o,j)=>typeof o==='string'?{key:String.fromCharCode(65+j),text:o}:{key:normalizeOptionKey(o.key||o.label||o.value||String.fromCharCode(65+j)),text:String(o.text||o.content||o.label||'').trim()}).filter(o=>o.text);
  }else{
    const keys='ABCDEFG';
    for(const k of keys){ if(q[k]!=null||q[k.toLowerCase()]!=null) options.push({key:k,text:String(q[k]??q[k.toLowerCase()]??'').trim()}); }
  }
  options=options.filter(o=>o.text);
  let questionText=String(q.question||q.title||q.stem||'').trim();
  const pureJudge=extractPureJudgeStemAnswer(questionText);
  if(pureJudge && !options.length && (!type||type==='single'||type==='judge')){
    type='judge';
    questionText=pureJudge.question;
    answer=answer.concat([pureJudge.answer]);
  }
  if(!type)type=guessType(questionText,options,answer,q.group||q.category||'');
  const fixedStem=cleanQuestionStemAndAnswer(questionText,answer,type,options);
  questionText=fixedStem.question;
  answer=fixedStem.answer;
  // 兼容纯判断题：题干末尾直接写（√）（×）（对）（错），且没有 A/B 选项和题型标题。
  // 这类题如果先按普通选择题猜型，会出现“单选题缺少选项”。
  if(!options.length && answer.length && answer.some(a=>isJudgeSymbolAnswer(a))){
    type='judge';
  }
  if(type==='judge'){
    options=normalizeJudgeOptions(options);
  }
  if(isTextType(type)){
    answer=splitTextAnswer((Array.isArray(rawAnswer)&&rawAnswer.length)?rawAnswer.join('；'):(rawAnswer||answer.join('；')));
  }else{
    answer=normalizeAnswer(answer,options,type);
  }
  return {id:q.id||'q_'+Date.now()+'_'+i,type,number:q.number||i+1,volume:q.volume||'',group:q.group||'',question:questionText,options,answer,analysis:q.analysis||q.explanation||q.explain||'',category:q.category||q.topic||q.group||'',score:Number(q.score||0)||undefined,normalized:normalizeText(questionText)}
}
function normalizeType(t){
  t=String(t||'').trim();
  if(!t)return'';
  if(t==='multi')return'multiple';
  if(t==='short_answer'||t==='essay'||t==='qa'||t==='subjective')return'short';
  if(t==='fill'||t==='fill_blank')return'blank';
  if(['single','multiple','judge','blank','short'].includes(t))return t;
  return mapType(t)||'';
}
function isTextType(t){return t==='blank'||t==='short'||t==='short_answer'}
function splitAnswerByType(s,type){
  if(isTextType(type))return splitTextAnswer(s);
  const a=splitAnswer(s);
  if(a.length)return a;
  if(!type && looksLikeTextualAnswer(s))return splitTextAnswer(s);
  return [];
}
function stripAnswerPrefix(s){
  return String(s??'').trim().replace(/^(?:\u3010|\[)?\s*(?:\u6b63\u786e\u7b54\u6848|\u53c2\u8003\u7b54\u6848|\u6807\u51c6\u7b54\u6848|\u7b54\u6848|\u53c2\u8003\u8981\u70b9|\u7b54\u9898\u8981\u70b9|Answer|Correct\s*answer)\s*(?:\u3011|\])?\s*(?:\:|\uFF1A)?\s*/i,'').trim();
}
function isObjectiveAnswerText(s){
  s=stripAnswerPrefix(s).replace(/[?.!??;?,?\s]+$/,'').trim();
  if(!s)return false;
  const compact=s.replace(/[\s,??;?/\\??()]+/g,'').toUpperCase();
  if(/^[A-G]{1,7}$/.test(compact))return true;
  if(/^[1-9]{1,9}$/.test(compact))return true;
  if(/^(?:\u5bf9|\u9519|\u6b63\u786e|\u9519\u8bef|\u662f|\u5426|\u221A|\u2713|\u2714|\u00D7|X|V|T|F|TRUE|FALSE)$/i.test(s))return true;
  if(/^([A-Ga-g])\s*[、.．:：]\s*.+$/.test(s))return true;
  return false;
}
function looksLikeTextualAnswer(s){
  s=stripAnswerPrefix(s).trim();
  if(!s||isObjectiveAnswerText(s))return false;
  const compact=normalizeTextAnswerForCompare(s);
  if(compact.length>20)return true;
  if(/[??;\n]/.test(s))return true;
  if(/[??]/.test(s)&&!isObjectiveAnswerText(s))return true;
  if(/(?:\u5305\u62ec|\u5e94\u5f53|\u9700\u8981|\u6b65\u9aa4|\u63aa\u65bd|\u6d41\u7a0b|\u539f\u56e0|\u8981\u6c42|\u5185\u5bb9|\u68c0\u67e5|\u6838\u5bf9|\u8fdb\u884c|\u4fdd\u8bc1|\u786e\u4fdd|\u4e25\u7981|\u5fc5\u987b|\u4e0d\u5f97|\u9996\u5148|\u5176\u6b21|\u7136\u540e|\u6700\u540e|\u4e00\u662f|\u4e8c\u662f|\u4e09\u662f|\u65bd\u5de5|\u8bbe\u5907|\u5b89\u5168|\u53c2\u6570)/.test(s))return true;
  return false;
}
function shouldGuessBlankFromNoOption(question,answer){
  const q=String(question||'');
  const ans=(answer||[]).map(x=>String(x||'').trim()).filter(Boolean);
  const ansText=ans.join('?');
  const ansLen=normalizeTextAnswerForCompare(ansText).length;
  const hasBlankStem=/_{2,}|____|[\uFF08(]\s*[\uFF09)]|\[\s*\]|\u586b\u7a7a|\u586b\u5165|\u8865\u5168|\u7a7a\u683c|\u6a2a\u7ebf/.test(q);
  const hasShortStem=/\u7b80\u8ff0|\u8bf4\u660e|\u9610\u8ff0|\u5206\u6790|\u8bba\u8ff0|\u5217\u4e3e|\u5199\u51fa|\u53d9\u8ff0|\u4e3a\u4ec0\u4e48|\u539f\u56e0|\u63aa\u65bd|\u6d41\u7a0b|\u6b65\u9aa4|\u8981\u6c42|\u5185\u5bb9|\u6ce8\u610f\u4e8b\u9879|\u5982\u4f55|\u54ea\u4e9b|\u4ec0\u4e48|\u7b80\u7b54|\u95ee\u7b54/.test(q);
  const hasLongText=looksLikeTextualAnswer(ansText)||ansLen>20;
  if(hasShortStem||hasLongText)return false;
  if(hasBlankStem)return true;
  return ans.length<=2 && ansLen>0 && ansLen<=20 && !/[??;\n]/.test(ansText);
}
function splitTextAnswer(s){
  if(Array.isArray(s))return s.map(x=>String(x||'').trim()).filter(Boolean);
  s=String(s??'').trim().replace(/^(?:【|\[)?\s*(?:正确答案|参考答案|标准答案|答案|参考要点|答题要点|Answer|Correct\s*answer)\s*(?:】|\])?\s*[:：]?\s*/i,'').trim();
  if(!s)return[];
  // 支持多个等价参考答案：用 || 或 ｜ 分隔；不按逗号/分号拆分，避免把简答要点拆坏。
  return s.split(/\s*(?:\|\||｜)\s*/).map(x=>x.trim()).filter(Boolean);
}

function extractPureJudgeStemAnswer(text){
  const s=String(text||'').trim();
  const m=s.match(/[（(]\s*(对|错|正确|错误|是|否|√|✓|✔|×|X|x|v|V|T|F|True|False)\s*[）)]\s*[。.!！?？]*\s*$/i);
  if(!m)return null;
  const ans=m[1].trim();
  const question=s.slice(0,m.index).trim().replace(/[。.!！?？]*\s*$/,'')+'（ ）';
  return {question,answer:ans};
}

function normalizeTextAnswerForCompare(s){return String(s||'').trim().replace(/[\s\u3000]+/g,'').replace(/[，,。.;；:：、!！?？]/g,'').toLowerCase()}
function normalizeText(t){return String(t).replace(/[\s\u3000]/g,'').replace(/[（）()【】\[\]{}。？?！!，,、：:；;\.\*]/g,'').toLowerCase()}
function short(s,n){s=String(s||'');return s.length>n?s.slice(0,n)+'…':s}
function esc(s){return String(s??'').replace(/[&<>"]/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[m]))}
function toast(msg,type='warn',title='提示'){
  const n=$('#import-summary');
  if(n){n.textContent=msg;n.className='notice '+type}
  showNotice(title,msg,type);
}
function showNotice(title,msg,type='ok'){
  let box=$('#app-toast');
  if(!box){box=document.createElement('div');box.id='app-toast';box.className='app-toast';document.body.appendChild(box)}
  const item=document.createElement('div');
  item.className='toast-item '+(type==='ok'?'ok':type==='danger'?'danger':'warn');
  item.innerHTML=`<div><b>${esc(title||'提示')}</b><p>${esc(msg||'')}</p></div><button type="button" aria-label="关闭">×</button>`;
  item.querySelector('button').onclick=()=>item.remove();
  box.appendChild(item);
  setTimeout(()=>{item.classList.add('hide');setTimeout(()=>item.remove(),260)},type==='danger'?6500:4200);
}
function summarizeImportResult(arr,warnings=[]){
  const stats=countTypes(arr||[]);
  return `识别到 ${(arr||[]).length} 道题：单选${stats.single||0}、多选${(stats.multiple||0)+(stats.multi||0)}、判断${stats.judge||0}、填空${stats.blank||0}、简答${stats.short||0}。${warnings&&warnings.length?`发现 ${warnings.length} 条需要确认的问题。`:'未发现明显异常。'}`;
}
function loadSample(){$('#import-bank-name').value='C1导入示例题库';$('#import-text').value=`1. 机动车驾驶人初次申领驾驶证后的实习期是多长时间？
A. 6个月
B. 12个月
C. 16个月
D. 18个月
答案：B
解析：初次取得汽车类准驾车型后的实习期为12个月。

2. 初次申领的机动车驾驶证有效期为6年。[判断题]
A、正确(正确答案)
B、错误

3. 雨天安全驾驶应注意哪些事项？ [多选题]
A、降低车速(正确答案)
B、加大跟车距离(正确答案)
C、避免急刹急打方向(正确答案)
D、高速通过积水路段`}
async function readImportFile(e){
  const file=e.target.files[0];
  if(!file)return;
  setImportBankNameFromFile(file.name);
  try{
    toast('正在读取文件，请稍候……','warn');
    const text=await readFileToText(file);
    $('#import-text').value=text;
    toast(`文件读取完成：提取到 ${text.split(/\n+/).filter(Boolean).length} 行文本。请点击“开始识别”。`,'ok');
  }catch(err){
    toast('文件读取失败：'+err.message+'。可尝试复制文件内容粘贴到文本框。');
  }
}
async function readDualFile(e,kind){
  const file=e.target.files[0];
  if(!file)return;
  try{
    toast(`正在读取${kind==='question'?'题目':'答案'}文件，请稍候……`,'warn');
    const text=await readFileToText(file);
    if(kind==='question'){
      $('#dual-question-text').value=text;
      setImportBankNameFromFile(file.name);
    }else{
      $('#dual-answer-text').value=text;
    }
    toast(`${kind==='question'?'题目':'答案'}文件读取完成。`,'ok');
  }catch(err){
    toast('文件读取失败：'+err.message+'。');
  }
}
async function readFileToText(file){
  const lower=file.name.toLowerCase();
  if(lower.endsWith('.docx'))return await extractDocxText(file);
  if(lower.endsWith('.doc'))throw new Error('暂不支持旧版 .doc，请先在 Word/WPS 中另存为 .docx 后导入');
  if(lower.endsWith('.pdf'))return await extractPdfText(file);
  return await new Promise((resolve,reject)=>{
    const reader=new FileReader();
    reader.onload=()=>resolve(String(reader.result||''));
    reader.onerror=()=>reject(new Error('文本读取失败'));
    reader.readAsText(file,'UTF-8');
  });
}
async function extractDocxText(file){
  const buf=await file.arrayBuffer();
  const entries=parseZipEntries(buf);
  const doc=entries.find(e=>e.name==='word/document.xml');
  if(!doc)throw new Error('未找到 word/document.xml，可能不是有效 .docx 文件');
  const xml=await unzipEntry(buf,doc);
  return docxXmlToText(xml);
}
function parseZipEntries(buf){
  const view=new DataView(buf);const bytes=new Uint8Array(buf);
  let eocd=-1;
  for(let i=bytes.length-22;i>=Math.max(0,bytes.length-66000);i--){if(view.getUint32(i,true)===0x06054b50){eocd=i;break}}
  if(eocd<0)throw new Error('无法识别 docx 压缩结构');
  const total=view.getUint16(eocd+10,true);const cdOffset=view.getUint32(eocd+16,true);let off=cdOffset;const out=[];
  for(let i=0;i<total;i++){
    if(view.getUint32(off,true)!==0x02014b50)break;
    const method=view.getUint16(off+10,true),compSize=view.getUint32(off+20,true),nameLen=view.getUint16(off+28,true),extraLen=view.getUint16(off+30,true),commentLen=view.getUint16(off+32,true),localOffset=view.getUint32(off+42,true);
    const name=utf8(bytes.slice(off+46,off+46+nameLen));
    out.push({name,method,compSize,localOffset});
    off+=46+nameLen+extraLen+commentLen;
  }
  return out;
}
async function unzipEntry(buf,e){
  const view=new DataView(buf);const bytes=new Uint8Array(buf);let off=e.localOffset;
  if(view.getUint32(off,true)!==0x04034b50)throw new Error('docx 条目结构异常');
  const nameLen=view.getUint16(off+26,true),extraLen=view.getUint16(off+28,true);const dataStart=off+30+nameLen+extraLen;const data=bytes.slice(dataStart,dataStart+e.compSize);
  if(e.method===0)return utf8(data);
  if(e.method!==8)throw new Error('不支持的 docx 压缩方式：'+e.method);
  if(!('DecompressionStream' in window))throw new Error('当前浏览器不支持本地解压 docx，请换新版 Chrome/Edge，或复制 Word 内容粘贴导入');
  let stream;
  try{stream=new Blob([data]).stream().pipeThrough(new DecompressionStream('deflate-raw'))}catch(_){stream=new Blob([data]).stream().pipeThrough(new DecompressionStream('deflate'))}
  const ab=await new Response(stream).arrayBuffer();
  return new TextDecoder('utf-8').decode(ab);
}
function utf8(u8){return new TextDecoder('utf-8').decode(u8)}
function docxXmlToText(xml){
  xml=xml.replace(/<w:tab\/>/g,'\t').replace(/<w:br\/>/g,'\n');
  const paras=xml.match(/<w:p\b[\s\S]*?<\/w:p>/g)||[];
  const lines=[];
  for(const p of paras){
    const parts=[];let m;const re=/<w:t(?:\s[^>]*)?>([\s\S]*?)<\/w:t>/g;
    while((m=re.exec(p)))parts.push(decodeXml(m[1]));
    const text=parts.join('').replace(/[\t ]+/g,' ').trim();
    if(text)lines.push(text);
  }
  return lines.join('\n');
}
function decodeXml(s){return String(s).replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&').replace(/&quot;/g,'"').replace(/&apos;/g,"'")}

async function extractPdfText(file){
  const data=new Uint8Array(await file.arrayBuffer());
  // v6: PDF 文本层本地提取。优先尝试本地 PDF.js；如果没有打包 PDF.js，则使用内置轻量提取器。
  try{
    const loaded=await loadLocalPdfJs();
    const pdfjsLib=loaded&&loaded.lib;
    if(pdfjsLib&&pdfjsLib.getDocument){
      if(pdfjsLib.GlobalWorkerOptions)pdfjsLib.GlobalWorkerOptions.workerSrc=loaded.workerSrc;
      const loadingTask=pdfjsLib.getDocument({data});
      const pdf=await loadingTask.promise;
      const pages=[];let extractedChars=0;
      for(let pageNo=1;pageNo<=pdf.numPages;pageNo++){
        const page=await pdf.getPage(pageNo);
        const content=await page.getTextContent();
        const pageText=pdfItemsToLines(content.items||[]);
        extractedChars+=pageText.replace(/\s/g,'').length;
        pages.push(`【第${pageNo}页】\n${pageText}`.trim());
      }
      const text=pages.join('\n\n').replace(/\n{3,}/g,'\n\n').trim();
      if(extractedChars>=20){
        toast(`PDF 文字提取完成：使用 ${loaded.mode}。`,'ok');
        return text;
      }
    }
  }catch(_){/* PDF.js 不可用时，转入内置轻量提取器。 */}
  const text=await extractPdfTextLite(data);
  const chars=text.replace(/\s/g,'').length;
  if(chars<20){
    throw new Error('该 PDF 未提取到足够文字，可能是扫描版/图片版 PDF。当前版本只支持文字型 PDF；可先复制 PDF 文本粘贴导入，或等待后续 OCR 版本。');
  }
  return text.replace(/\n{3,}/g,'\n\n').trim();
}
async function loadLocalPdfJs(){
  // v7: 混合 PDF.js 加载策略：本地最小版优先；本地缺失时 CDN；最后才降级轻量提取器。
  if(window.__pdfjsMixed)return window.__pdfjsMixed;
  const candidates=[
    {mode:'本地 PDF.js',module:'./libs/pdf.min.mjs',worker:'./libs/pdf.worker.min.mjs'},
    {mode:'CDN PDF.js/jsDelivr',module:'https://cdn.jsdelivr.net/npm/pdfjs-dist@5.7.284/build/pdf.min.mjs',worker:'https://cdn.jsdelivr.net/npm/pdfjs-dist@5.7.284/build/pdf.worker.min.mjs'},
    {mode:'CDN PDF.js/unpkg',module:'https://unpkg.com/pdfjs-dist@5.7.284/build/pdf.min.mjs',worker:'https://unpkg.com/pdfjs-dist@5.7.284/build/pdf.worker.min.mjs'}
  ];
  for(const c of candidates){
    try{
      const mod=await import(c.module);
      window.__pdfjsMixed={lib:mod,workerSrc:c.worker,mode:c.mode};
      return window.__pdfjsMixed;
    }catch(e){/* 尝试下一种来源 */}
  }
  return null;
}
async function extractPdfTextLite(bytes){
  const raw=latin1(bytes);
  const streams=[];
  const re=/stream\r?\n([\s\S]*?)\r?\nendstream/g;
  let m;
  while((m=re.exec(raw))){
    const before=raw.slice(Math.max(0,m.index-900),m.index);
    const dict=(before.match(/<<[\s\S]*?>>\s*$/)||[''])[0];
    let bin=m[1];
    if(bin.startsWith('\r\n'))bin=bin.slice(2);else if(bin.startsWith('\n'))bin=bin.slice(1);
    let u8=latin1ToBytes(bin);
    if(/FlateDecode/.test(dict)){
      try{u8=await inflateBytes(u8)}catch(_){continue}
    }
    const txt=decodePdfStreamText(latin1(u8));
    if(txt.trim())streams.push(txt.trim());
  }
  let text=streams.join('\n');
  // 有些极简 PDF 没有压缩流，直接在全文中包含 Tj/TJ。
  if(text.replace(/\s/g,'').length<20){
    text=decodePdfStreamText(raw);
  }
  return cleanupPdfLiteText(text);
}
function latin1(u8){let out='';const chunk=0x8000;for(let i=0;i<u8.length;i+=chunk)out+=String.fromCharCode.apply(null,u8.slice(i,i+chunk));return out}
function latin1ToBytes(s){const out=new Uint8Array(s.length);for(let i=0;i<s.length;i++)out[i]=s.charCodeAt(i)&255;return out}
async function inflateBytes(u8){
  if(!('DecompressionStream' in window))throw new Error('当前浏览器缺少 DecompressionStream，无法解压 PDF 压缩文本流');
  const tryOne=async fmt=>new Uint8Array(await new Response(new Blob([u8]).stream().pipeThrough(new DecompressionStream(fmt))).arrayBuffer());
  try{return await tryOne('deflate')}catch(e){return await tryOne('deflate-raw')}
}
function decodePdfStreamText(s){
  const out=[];
  const tj=/((?:\((?:\\.|[^\\()])*\)|<[^>]+>|\[(?:[^\]]|\((?:\\.|[^\\()])*\))*\]))\s*(?:Tj|TJ|\'|\")/g;
  let m;
  while((m=tj.exec(s))){
    const token=m[1];
    if(token.startsWith('[')){
      out.push(...extractPdfArrayStrings(token));
    }else{
      out.push(decodePdfToken(token));
    }
  }
  return out.join('\n');
}
function extractPdfArrayStrings(arr){
  const items=[];const re=/\((?:\\.|[^\\()])*\)|<[^>]+>/g;let m;
  while((m=re.exec(arr)))items.push(decodePdfToken(m[0]));
  return items;
}
function decodePdfToken(t){
  t=String(t||'').trim();
  if(t.startsWith('(')&&t.endsWith(')'))return decodePdfLiteral(t.slice(1,-1));
  if(t.startsWith('<')&&t.endsWith('>'))return decodePdfHex(t.slice(1,-1));
  return '';
}
function decodePdfLiteral(s){
  let out='';
  for(let i=0;i<s.length;i++){
    const c=s[i];
    if(c==='\\'){
      const n=s[++i];
      if(n==='n')out+='\n';else if(n==='r')out+='\r';else if(n==='t')out+='\t';else if(n==='b')out+='\b';else if(n==='f')out+='\f';
      else if(/[0-7]/.test(n||'')){
        let oct=n;for(let k=0;k<2&&/[0-7]/.test(s[i+1]||'');k++)oct+=s[++i];out+=String.fromCharCode(parseInt(oct,8));
      }else if(n==='\n'||n==='\r'){ if(n==='\r'&&s[i+1]==='\n')i++; }
      else out+=n||'';
    }else out+=c;
  }
  return decodeMaybeUtf16OrGbk(out);
}
function decodePdfHex(hex){
  hex=hex.replace(/\s+/g,'');if(hex.length%2)hex+='0';
  const bytes=[];for(let i=0;i<hex.length;i+=2)bytes.push(parseInt(hex.slice(i,i+2),16));
  if(bytes[0]===0xfe&&bytes[1]===0xff){let str='';for(let i=2;i<bytes.length;i+=2)str+=String.fromCharCode((bytes[i]<<8)|(bytes[i+1]||0));return str}
  if(bytes[0]===0xff&&bytes[1]===0xfe){let str='';for(let i=2;i<bytes.length;i+=2)str+=String.fromCharCode(bytes[i]|((bytes[i+1]||0)<<8));return str}
  return decodeMaybeUtf16OrGbk(String.fromCharCode(...bytes));
}
function decodeMaybeUtf16OrGbk(s){
  if(!s)return '';
  const bytes=latin1ToBytes(s);
  try{
    if(bytes[0]===0xfe&&bytes[1]===0xff){let out='';for(let i=2;i<bytes.length;i+=2)out+=String.fromCharCode((bytes[i]<<8)|(bytes[i+1]||0));return out}
    if(bytes[0]===0xff&&bytes[1]===0xfe){let out='';for(let i=2;i<bytes.length;i+=2)out+=String.fromCharCode(bytes[i]|((bytes[i+1]||0)<<8));return out}
    if(typeof TextDecoder!=='undefined'){
      try{return new TextDecoder('utf-8',{fatal:true}).decode(bytes)}catch(_){ }
      try{return new TextDecoder('gb18030').decode(bytes)}catch(_){ }
    }
  }catch(_){ }
  return s;
}
function cleanupPdfLiteText(text){
  return String(text||'')
    .replace(/\r/g,'\n')
    .split('\n')
    .map(x=>x.replace(/[\t ]+/g,' ').trim())
    .filter(Boolean)
    .join('\n');
}

function pdfItemsToLines(items){
  const parts=items.map(it=>({
    text:String(it.str||'').trim(),
    x:Number(it.transform&&it.transform[4]||0),
    y:Number(it.transform&&it.transform[5]||0)
  })).filter(it=>it.text);
  if(!parts.length)return '';
  parts.sort((a,b)=>Math.abs(b.y-a.y)>3?b.y-a.y:a.x-b.x);
  const lines=[];
  for(const part of parts){
    let line=lines.find(l=>Math.abs(l.y-part.y)<=3);
    if(!line){line={y:part.y,items:[]};lines.push(line)}
    line.items.push(part);
  }
  lines.sort((a,b)=>b.y-a.y);
  return lines.map(line=>line.items.sort((a,b)=>a.x-b.x).map(x=>x.text).reduce(joinPdfText,'').trim()).filter(Boolean).join('\n');
}
function joinPdfText(acc,cur){
  if(!acc)return cur;
  const last=acc.slice(-1);
  const first=cur.charAt(0);
  if(/[A-Za-z0-9)]/.test(last)&&/[A-Za-z0-9(]/.test(first))return acc+' '+cur;
  if(/[，。；：、？！,.!?;:]/.test(first))return acc+cur;
  return acc+cur;
}

function parseImport(){
  const text=$('#import-text').value.trim();
  const strategy=$('#import-strategy')?.value||'auto';
  if(!text){toast('请先粘贴或上传题库文本。','warn','导入未开始');return}
  try{
    importWarnings=[];importReport='';importDiagnostics=null;
    if($('#import-mode').value==='json'||text.startsWith('[')||text.startsWith('{')){
      const data=JSON.parse(text);const arr=Array.isArray(data)?data:(data.questions||[]);
      importCache=arr.map(normalizeQuestion).filter(q=>q.question);importReport='解析策略：JSON结构化导入。';importDiagnostics={mode:'JSON结构化导入',strategy:'JSON结构化导入',profile:{},candidates:[{name:'JSON结构化导入',questions:importCache.length,score:importCache.length*10,warnings:collectImportWarnings(importCache)}],expected:{total:0,types:{}},stats:countTypes(importCache)};
    }else importCache=parseTextQuestions(text,strategy);
    importSelected.clear();
    renderImportPreview(importCache);
    $('#confirm-import-btn').disabled=!importCache.length;
    const warnings=collectSoftRiskWarnings(importCache, importDiagnostics?.profile||{});
    if(importCache.length)showNotice('识别完成',summarizeImportResult(importCache,warnings),warnings.length?'warn':'ok');
    else showNotice('识别失败','没有识别到有效题目。请检查题号、选项或答案格式，也可以先粘贴纯文本后再试。','danger');
  }catch(e){toast('识别失败：'+e.message,'danger','识别失败')}
}
function loadDualSample(){
  $('#import-bank-name').value='C1双文件导入示例题库';
  $('#dual-question-text').value=`一、单选题
1. 机动车驾驶人初次申领驾驶证后的实习期是多长时间？
A. 6个月
B. 12个月
C. 16个月
D. 18个月

二、判断题
1. 初次申领的机动车驾驶证有效期为6年。
A. 正确
B. 错误

三、多选题
1. 雨天安全驾驶应注意哪些事项？
A. 降低车速
B. 加大跟车距离
C. 避免急刹急打方向
D. 高速通过积水路段`;
  $('#dual-answer-text').value=`一、单选题
1. B

二、判断题
1. A

三、多选题
1. ABC`;
  $('#dual-match-mode').value='auto';
  toast('已填入C1双文件示例，请点击“识别并合并双文件”。','ok');
}
function parseDualImport(){
  const qText=$('#dual-question-text').value.trim();
  const aText=$('#dual-answer-text').value.trim();
  if(!qText||!aText){toast('请先提供题目文本和答案文本。','warn','双文件导入未开始');return}
  try{
    importWarnings=[];importReport='';
    const qStrategy=$('#import-strategy')?.value||'auto';
    const questions=parseTextQuestions(qText,qStrategy).map((q,i)=>({...q,answer:[],number:q.number||i+1}));
    const mode=$('#dual-match-mode').value;
    const resolved=resolveDualAnswerCandidates(questions,aText,mode);
    const answerEntries=resolved.answerEntries||[];
    const result=resolved.result||mergeQuestionAnswers(questions,answerEntries,mode);
    importCache=result.questions;
    importWarnings=result.warnings;
    importReport='解析策略：双文件导入；题目文件和答案文件分别识别后按自动/所选规则合并。'
      +(result.strategyName?' 实际采用：'+result.strategyName+'。':'')
      +(resolved.answerSourceName?' 答案文件识别：'+resolved.answerSourceName+'。':'')
      +(resolved.answerSourceSummary?' 答案候选：'+resolved.answerSourceSummary+'。':'')
      +(result.autoSummary?' 合并候选：'+result.autoSummary+'。':'');
    importDiagnostics={...(importDiagnostics||{}),mode:'双文件导入',matchMode:mode,chosenMatchMode:result.chosenMode||mode,answerCount:answerEntries.length,questionCount:questions.length,stats:countTypes(result.questions||[]),mergeWarnings:result.warnings||[],autoSummary:result.autoSummary||'',answerSource:resolved.answerSourceName||'',answerSourceSummary:resolved.answerSourceSummary||''};
    importSelected.clear();
    renderImportPreview(importCache);
    $('#confirm-import-btn').disabled=!importCache.length;
    const warnings=[...new Set(collectSoftRiskWarnings(importCache, importDiagnostics?.profile||{}).concat(importWarnings||[]))];
    if(importCache.length)showNotice('双文件合并完成',`题目文件识别 ${questions.length} 道，答案文件识别 ${answerEntries.length} 条；答案策略：${resolved.answerSourceName||'答案表提取'}；合并后 ${importCache.length} 道。${warnings.length?`存在 ${warnings.length} 条提示，请在预览中确认。`:'未发现明显异常。'}`,warnings.length?'warn':'ok');
    else showNotice('双文件合并失败','没有得到可导入题目，请检查题目文件和答案文件的对应方式。','danger');
  }catch(e){toast('双文件识别失败：'+e.message,'danger','双文件识别失败')}
}
function parseAnswerEntries(text){
  text=normalizeImportText(text);
  const lines=text.split('\n').map(x=>x.trim()).filter(Boolean).filter(l=>!isImportNoiseLine(l));
  const entries=[];let group='';let pendingNumber='';
  const push=(number,ans,g=group)=>{const a=splitAnswer(ans);const finalAns=a.length?a:splitTextAnswer(ans);if(finalAns.length)entries.push({number:String(number||''),group:g||'',answer:finalAns,raw:ans})};
  for(const raw of lines){
    const heading=getHeadingType(raw);
    if(heading){group=heading;pendingNumber='';continue}
    let line=raw.replace(/^[-•●]\s*/,'').trim();
    // 1-10：D A A B C D A C B D
    let range=line.match(/^\s*(\d+)\s*[-~—至到]\s*(\d+)\s*[:：]\s*(.+)$/);
    if(range){
      const start=Number(range[1]),end=Number(range[2]);
      const toks=range[3].trim().split(/\s+/).filter(Boolean);
      if(toks.length===end-start+1){toks.forEach((t,i)=>push(start+i,t));continue}
    }
    // 一行多个：1.D 2.A 3.A 4.B 5.C
    const pairRe=/(?:第\s*)?(\d+)\s*(?:题)?\s*[\.、．:：]?\s*(?:答案|正确答案|参考答案)?\s*[:：]?\s*([A-Ga-g]{1,7}|[1-9]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|v|V|T|F|True|False)(?=\s|$|\d+[\.、．:：])/g;
    let hits=[];let m;
    while((m=pairRe.exec(line)))hits.push({number:m[1],ans:m[2]});
    if(hits.length>=2){hits.forEach(h=>push(h.number,h.ans));continue}
    // 第1题：D / 1. D / 1 答案：A、C、D
    let one=line.match(/^\s*(?:第\s*)?(\d+)\s*(?:题)?\s*[\.、．:：]?\s*(?:答案|正确答案|参考答案|标准答案|参考要点|答题要点)?\s*[:：]?\s*(.+?)\s*$/);
    if(one){
      const ans=one[2].trim();
      if(splitAnswer(ans).length){push(one[1],ans);pendingNumber='';continue}
    }
    // 只有题号，下一行是答案
    let numOnly=line.match(/^\s*(?:第\s*)?(\d+)\s*(?:题)?[\.、．:：]?\s*$/);
    if(numOnly){pendingNumber=numOnly[1];continue}
    // 答案：D，没有题号，按顺序使用
    let ansOnly=line.match(/^(?:答案|正确答案|参考答案|标准答案)\s*[:：]?\s*(.+)$/);
    if(ansOnly){push(pendingNumber,ansOnly[1]);pendingNumber='';continue}
    // 只有答案，按顺序使用
    if(splitAnswer(line).length && /^([A-Ga-g]{1,7}|[1-9]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|v|V|T|F|True|False)$/.test(line)){
      push(pendingNumber,line);pendingNumber='';continue;
    }
  }
  return entries;
}

function parseAnswerEntriesByQuestionParse(text){
  let parsed=[];
  try{parsed=parseTextQuestions(text,'auto')||[]}catch(e){parsed=[]}
  const entries=[];
  parsed.forEach((q,i)=>{
    const ans=(q.answer||[]).map(x=>String(x||'').trim()).filter(Boolean);
    if(!ans.length)return;
    entries.push({
      number:String(q.number||i+1),
      group:q.category||q.group||q.type||'',
      answer:ans,
      raw:ans.join('')
    });
  });
  return {entries,questionCount:parsed.length};
}
function scoreDualMergeResult(result){
  const qs=result?.questions||[];
  const importWarns=collectImportWarnings(qs);
  const unanswered=qs.filter(q=>!(q.answer||[]).length).length;
  const invalid=importWarns.filter(w=>!/缺少答案|缺少参考答案/.test(w)).length;
  const typeMismatch=qs.filter(q=>q.type==='judge'&&(q.answer||[]).some(a=>!['A','B'].includes(String(a)))).length;
  const answered=qs.length-unanswered;
  return answered*120-invalid*100-unanswered*90-typeMismatch*150-((result?.warnings||[]).length*10);
}
function resolveDualAnswerCandidates(questions,text,mode='auto'){
  const standardEntries=parseAnswerEntries(text);
  const analysisEntries=parseAnswerAnalysisEntries(text);
  const fullParsed=parseAnswerEntriesByQuestionParse(text);
  const candidates=[
    {key:'simple',name:'答案表提取',entries:standardEntries,detail:`答案表提取${standardEntries.length}条`}
  ];
  if(analysisEntries.length){
    candidates.push({key:'analysis',name:'答案解析区提取',entries:analysisEntries,detail:`答案解析区提取${analysisEntries.length}条`});
  }
  if(fullParsed.entries.length){
    candidates.push({key:'full',name:'完整题库解析兜底',entries:fullParsed.entries,detail:`完整题库解析${fullParsed.entries.length}条（共识别${fullParsed.questionCount||0}题）`});
  }
  if((standardEntries.length||analysisEntries.length)&&fullParsed.entries.length){
    const merged=[];const seen=new Set();
    const add=e=>{const key=`${String(e.number||'')}|${String(e.group||'')}|${(e.answer||[]).join('')}`;if(seen.has(key))return;seen.add(key);merged.push(e)};
    standardEntries.forEach(add);analysisEntries.forEach(add);fullParsed.entries.forEach(add);
    candidates.push({key:'hybrid',name:'答案表提取 + 完整题库解析兜底',entries:merged,detail:`混合提取${merged.length}条（答案表${standardEntries.length} + 答案解析${analysisEntries.length} + 完整题库${fullParsed.entries.length}）`});
  }
  const evaluated=candidates.map(c=>{
    const result=mergeQuestionAnswers(questions,c.entries,mode);
    const score=scoreDualMergeResult(result);
    return {...c,result,score};
  }).sort((a,b)=>b.score-a.score);
  const best=evaluated[0]||{entries:[],name:'答案表提取',detail:'答案表提取0条',result:mergeQuestionAnswers(questions,[],mode),score:-999999};
  return {
    answerEntries:best.entries||[],
    answerSourceName:best.name,
    answerSourceSummary:evaluated.map(c=>`${c.name}${(c.entries||[]).length}条/质量${c.score}`).join('；'),
    result:best.result,
    candidates:evaluated
  };
}
function mergeQuestionAnswers(questions,answers,mode){
  if(mode==='auto'){
    const labelMap={group:'按题型分组 + 组内题号对应',number:'智能按题号对应',order:'按顺序对应'};
    const candidates=['group','number','order'].map(m=>{
      const r=mergeQuestionAnswers(questions,answers,m);
      const qs=r.questions||[];
      const importWarns=collectImportWarnings(qs);
      const unanswered=qs.filter(q=>!(q.answer||[]).length).length;
      const invalid=importWarns.filter(w=>!/缺少答案|缺少参考答案/.test(w)).length;
      const typeMismatch=qs.filter(q=>q.type==='judge'&&(q.answer||[]).some(a=>!['A','B'].includes(String(a)))).length;
      const answered=qs.length-unanswered;
      const score=answered*100-invalid*80-unanswered*50-typeMismatch*120-(r.warnings||[]).length*8;
      return {...r,chosenMode:m,strategyName:labelMap[m],autoScore:score,autoUnanswered:unanswered,autoInvalid:invalid};
    });
    candidates.sort((a,b)=>b.autoScore-a.autoScore);
    const best=candidates[0]||{questions:[],warnings:[]};
    const summary=candidates.map(c=>`${c.strategyName}${(c.questions||[]).length}题/缺答案${c.autoUnanswered}/质量${c.autoScore}`).join('；');
    best.autoSummary=summary;
    return best;
  }
  const warnings=[];
  if(!questions.length)warnings.push('题目文件未识别到题目。');
  if(!answers.length)warnings.push('答案文件未识别到答案。');
  const qs=questions.map((q,i)=>({...q,answer:[],number:q.number||i+1}));
  const normGroup=s=>mapType(s)||mapType(String(s||'').replace(/题$/,''))||String(s||'').replace(/\s/g,'');
  if(mode==='order'){
    qs.forEach((q,i)=>{if(answers[i])q.answer=normalizeAnswer(answers[i].answer,q.options,q.type)});
  }else if(mode==='group'){
    const used=new Set();
    qs.forEach((q,i)=>{
      const qg=normGroup(q.category||q.group||'');
      const hitIndex=answers.findIndex((a,ai)=>!used.has(ai)&&String(a.number)===String(q.number)&&normGroup(a.group)===qg);
      if(hitIndex>=0){q.answer=normalizeAnswer(answers[hitIndex].answer,q.options,q.type);used.add(hitIndex)}
    });
    if(qs.some(q=>!q.answer.length))warnings.push('部分题目没有按“题型分组 + 题号”匹配到答案，可尝试改用“按顺序对应”或检查分组标题。');
  }else{
    // 按题号对应时也要兼容“每个题型都从 1 重新编号”的答案表。
    // 如果同一题号在单选/多选/判断区重复出现，优先按题目自身题型匹配答案分组，避免判断题误取单选题的 C/D 答案。
    const byNumber=new Map();const dup=new Set();
    answers.forEach((a,idx)=>{if(!a.number){return}const key=String(a.number);if(!byNumber.has(key))byNumber.set(key,[]);else dup.add(key);byNumber.get(key).push({...a,idx})});
    if(dup.size)warnings.push('答案文件存在重复题号：'+[...dup].slice(0,10).join('、')+'。已优先按题型分组匹配；如仍有异常，请使用“按题型分组 + 组内题号对应”。');
    qs.forEach((q,i)=>{
      const candidates=byNumber.get(String(q.number))||[];
      let hit=null;
      if(candidates.length){
        const qg=normGroup(q.category||q.group||q.type||'');
        hit=candidates.find(a=>normGroup(a.group)===qg)||null;
        if(!hit&&q.type==='judge')hit=candidates.find(a=>normGroup(a.group)==='judge'||isRawJudgeSymbolAnswer(a.raw)||(a.answer||[]).some(x=>isJudgeSymbolAnswer(x)))||null;
        if(!hit&&q.type==='multiple')hit=candidates.find(a=>normGroup(a.group)==='multiple'||((a.answer||[]).join('').length>1&&!isRawJudgeSymbolAnswer(a.raw)))||null;
        if(!hit&&q.type==='single')hit=candidates.find(a=>normGroup(a.group)==='single'||((a.answer||[]).length===1&&/^[A-G1-9]$/.test(String(a.answer[0]||''))&&!isRawJudgeSymbolAnswer(a.raw)))||null;
        if(!hit){
          // 重复题号场景下不要把单选/多选答案强行套给判断题；宁可留给预览区标异常，也不要静默错配。
          hit=(candidates.length===1||mode==='number-strict')?candidates[0]:null;
        }
      }
      if(hit)q.answer=normalizeAnswer(hit.answer,q.options,q.type);
      else if(answers[i]&&!answers[i].number)q.answer=normalizeAnswer(answers[i].answer,q.options,q.type);
    });
  }
  const unanswered=qs.filter(q=>!q.answer.length).length;
  if(questions.length!==answers.length)warnings.push(`数量提示：识别到题目 ${questions.length} 道，答案 ${answers.length} 个。`);
  if(unanswered)warnings.push(`合并后仍有 ${unanswered} 道题缺少答案。`);
  return {questions:qs.map((q,i)=>normalizeQuestion(q,i)),warnings};
}
function parseTextQuestionsBaseDetailed(text){
  text=normalizeImportText(text);
  text=preSplitVolumeAndCompactQuestions(text);
  if(!text.trim())return {questions:[],blocks:[],pairs:[]};
  const blocks=splitQuestionBlocks(text);
  const questions=[];const pairs=[];
  blocks.forEach((block,idx)=>{
    const q=parseBlock(block,idx);
    if(q&&q.question&&(q.options.length||q.answer.length||q.type==='judge'||isTextType(q.type))){
      const nq=normalizeQuestion({...q,volume:block.volume||q.volume||'',group:block.group||q.group||''},questions.length);
      questions.push(nq);pairs.push({question:nq,block,blockIndex:idx});
    }
  });
  return {questions,blocks,pairs};
}
function parseTextQuestionsBase(text){
  return parseTextQuestionsBaseDetailed(text).questions;
}


function importWarningsForStrategy(qs,profile){
  const warnings=collectImportWarnings(qs||[]);
  const sourceLikelyHasAnswers=!!(profile&&(profile.inlineAnswerLikely||profile.hasAnswerAnalysisSection)) || (qs||[]).some(q=>(q.answer||[]).length);
  return sourceLikelyHasAnswers ? warnings : warnings.filter(w=>!/缺少答案|缺少参考答案/.test(w));
}
function localRepairRiskStatus(q,profile){
  const status=validateQuestion(q);
  if(status!=='正常' && !(!profile?.inlineAnswerLikely && /缺少答案|缺少参考答案/.test(status)))return status;
  const question=String(q.question||'');
  const options=q.options||[];
  if((options||[]).some(o=>String(o.text||'').length>220||/【\s*(?:答案|正确答案)|\b\d{1,4}\s*[、.．:：].+【\s*答案/.test(o.text||'')))return'选项疑似粘连';
  if(/【\s*(?:答案|正确答案|参考答案)|(?:答案|正确答案|参考答案)\s*[:：]/.test(question))return'题干残留答案标记';
  if(question.length>260)return'题干过长';
  if(['single','multiple'].includes(q.type)){
    if(/[（(]\s*[）)]\s*A\s*(?:[、.．:：]|\s+|(?=[\u4e00-\u9fa5]))/.test(question)||/[。？！?]\s*A\s*(?:[、.．:：]|\s+)(?!级|PI\b|P\b)/i.test(question))return'题干疑似混入A选项';
    if(q.type==='single'&&options.length===2&&!isJudgeBlock(options,q.answer||[]))return'单选题选项数量偏少';
    if(q.type==='multiple'&&options.length<3)return'多选题选项数量偏少';
  }
  if(q.type==='judge'){
    const map=judgeOptionMap(options);
    if(options.length>=2&&!map.confidence&&!(options.some(o=>o.key==='A')&&options.some(o=>o.key==='B')))return'判断题选项含义疑似不明确';
  }
  return'正常';
}

function isConfirmableImportIssue(status){
  return /请确认|偏少|过短|疑似判断题被识别为单选题/.test(String(status||''));
}
function importIssueStatus(q,profile){
  const hard=validateQuestion(q);
  if(hard!=='正常')return isConfirmableImportIssue(hard)?'异常：'+hard:hard;
  const soft=localRepairRiskStatus(q,profile||{});
  return soft==='正常'?'正常':'异常：'+soft;
}

function collectSoftRiskWarnings(arr,profile){
  const warnings=[];
  (arr||[]).forEach((q,i)=>{const status=importIssueStatus(q,profile||{});if(status!=='正常')warnings.push(`第${i+1}题：${status}`)});
  return warnings;
}

function countLocalRepairWarnings(qs,profile){
  return (qs||[]).reduce((n,q)=>n+(localRepairRiskStatus(q,profile)==='正常'?0:1),0);
}
function scoreLocalSegment(qs,profile){
  let score=(qs||[]).length*30 - countLocalRepairWarnings(qs,profile)*120;
  (qs||[]).forEach(q=>{
    if(q.question&&q.question.length>260)score-=30;
    if((q.options||[]).some(o=>String(o.text||'').length>220))score-=40;
    if(['single','multiple'].includes(q.type)&&q.options.length>=2)score+=20;
    if(q.type==='judge'&&q.options.length>=2)score+=20;
  });
  return score;
}
function parseLocalRepairCandidates(text){
  const arr=[];
  const push=(name,fn)=>{try{const qs=fn().map((q,i)=>normalizeQuestion(q,i)).filter(q=>q.question);arr.push({name,questions:qs});}catch(_){}}; 
  push('标准试卷段落解析',()=>parseStructuredExamText(text));
  push('局部标准解析',()=>parseTextQuestionsBase(text));
  push('局部紧凑解析',()=>parseTextQuestionsBase(forceSplitCompactText(text)));
  push('局部分卷分区解析',()=>parseByVolumeAndSections(text));
  return arr.filter(c=>c.questions.length);
}
function repairParsedQuestionsLocally(original,standardQuestions,profile){
  const detailed=parseTextQuestionsBaseDetailed(original);
  const base=(standardQuestions&&standardQuestions.length?standardQuestions:detailed.questions).map((q,i)=>normalizeQuestion(q,i));
  const pairs=detailed.pairs||[];
  if(!base.length||!pairs.length||Math.abs(base.length-pairs.length)>3)return {questions:base,repaired:0,segments:[]};
  const risky=[];
  base.forEach((q,i)=>{if(localRepairRiskStatus(q,profile)!=='正常')risky.push(i);});
  if(!risky.length)return {questions:base,repaired:0,segments:[]};
  // 只有少量风险题时才做局部修复；大量异常仍交给全量候选策略比较，避免局部替换放大错误。
  if(risky.length>Math.max(8,Math.ceil(base.length*0.04)))return {questions:base,repaired:0,segments:[]};
  const replaced=new Set();
  const result=[];let repaired=0;const segments=[];
  let r=0;
  while(r<risky.length){
    let start=risky[r],end=start;
    while(r+1<risky.length&&risky[r+1]<=end+1){r++;end=risky[r];}
    const segStart=Math.max(0,start-1),segEnd=Math.min(pairs.length-1,end+1);
    const localBlocks=pairs.slice(segStart,segEnd+1).map(p=>p.block).filter(Boolean);
    const segmentText=localBlocks.map(b=>{
      const head=[b.volume,b.group].filter(Boolean).join('\n');
      const body=(b.lines||[]).join('\n');
      return [head,body].filter(Boolean).join('\n');
    }).join('\n');
    const originalQs=base.slice(segStart,segEnd+1);
    const originalScore=scoreLocalSegment(originalQs,profile);
    let best={name:'原标准片段',questions:originalQs,score:originalScore};
    parseLocalRepairCandidates(segmentText).forEach(c=>{
      if(c.questions.length<Math.max(1,originalQs.length-1)||c.questions.length>originalQs.length+2)return;
      const sc=scoreLocalSegment(c.questions,profile);
      if(sc>best.score+40)best={...c,score:sc};
    });
    if(best.name!=='原标准片段'){
      for(let i=segStart;i<=segEnd;i++)replaced.add(i);
      result.push({insertAt:segStart,questions:best.questions,from:originalQs.length,to:best.questions.length,name:best.name});
      repaired+=Math.max(1,countLocalRepairWarnings(originalQs,profile)-countLocalRepairWarnings(best.questions,profile));
      segments.push(`第${segStart+1}-${segEnd+1}题：${best.name}，${originalQs.length}题→${best.questions.length}题`);
    }
    r++;
  }
  if(!result.length)return {questions:base,repaired:0,segments:[]};
  const out=[];let ins=0;
  for(let i=0;i<base.length;i++){
    const rep=result.find(x=>x.insertAt===i);
    if(rep){rep.questions.forEach(q=>out.push(q));ins++;}
    if(replaced.has(i))continue;
    out.push(base[i]);
  }
  return {questions:out.map((q,i)=>normalizeQuestion(q,i)),repaired,segments};
}


function isAnswerAnalysisEntryLine(line){
  const s=String(line||'').trim();
  if(!s)return false;
  if(/^\s*\d{1,4}\s*(?:[.、．:：]\s*)?(?:【\s*(?:答案|正确答案|参考答案|标准答案)\s*】|\[\s*(?:答案|正确答案|参考答案|标准答案)\s*\]|(?:答案|正确答案|参考答案|标准答案)\s*[:：])\s*(?:[A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)/i.test(s))return true;
  if(/^\s*\d{1,4}\s*[.、．:：]\s*(?:【\s*(?:解析|答案解析|试题解析)\s*】|\[\s*(?:解析|答案解析|试题解析)\s*\]|(?:解析|答案解析|试题解析|分析)\s*[:：])\s*(?:选\s*)?(?:[A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)/i.test(s))return true;
  if(/^\s*\d{1,4}\s*[.、．:：]\s*[A-Ga-g]{1,7}(?=$|[\s。．.、，,；;：:]|【\s*解析\s*】|\[\s*解析\s*\]|解析|分析)/.test(s))return true;
  if(/^\s*\d{1,4}\s*[.、．:：]\s*[A-Ga-g][\d\/\.]+/.test(s))return true; // 16.B352 / 20.A122/199
  return false;
}
function isAnswerSectionHeading(line,nextLines=[]){
  const raw=String(line||'').trim();
  const s=raw.replace(/[\s　]+/g,'');
  if(!s)return false;
  const heading=/^(?:[一二三四五六七八九十]+[、.．:：]?)?(?:答案|参考答案|标准答案|正确答案|答案解析|答案及解析|答案与解析|试题解析|真题答案|解析)$/.test(s);
  if(!heading)return false;
  const look=(nextLines||[]).slice(0,6).filter(Boolean).map(x=>String(x).trim());
  const hits=look.filter(isAnswerAnalysisEntryLine).length;
  const hasAnswerWords=look.some(l=>/【\s*(?:答案|解析)\s*】|(?:答案|正确答案|参考答案|标准答案|解析|分析)\s*[:：]|故(?:答案)?选|本题(?:答案)?(?:为|选)|因此.*(?:答案|选)|所以.*选/.test(l));
  return hits>=1 || hasAnswerWords;
}
function isGenericQuestionSectionHeading(line,nextLines=[]){
  const raw=String(line||'').trim();
  if(!raw||raw.length>50)return false;
  if(isOptionLine(raw)||isAnswerAnalysisEntryLine(raw)||isAnswerLine(raw)||isAnalysisLine(raw))return false;
  const compact=raw.replace(/[\s　]+/g,'');
  // 真实分区通常是“第一部分/一、言语理解/三、图形推理/四、逻辑判断”；
  // 普通题目也可能以“1．心理契约……”开头，不能当分区。
  const chineseSection=/^(?:第[一二三四五六七八九十0-9]+部分|[一二三四五六七八九十]+[、.．:：])/.test(compact)
    && /(?:言语|语言|数学|数量|图形|逻辑|判断推理|定义判断|类比推理|资料分析|材料分析|综合|能力|常识|单选|多选|判断题|填空|简答)/.test(compact);
  const numericTypeSection=/^\d+[、.．:：]/.test(compact)
    && compact.length<=20
    && /(?:言语|语言|数学|数量|图形|逻辑|判断推理|资料分析|材料分析|单选|多选|判断题|填空|简答)/.test(compact);
  if(!chineseSection&&!numericTypeSection)return false;
  const look=(nextLines||[]).slice(0,10).filter(Boolean).map(x=>String(x).trim());
  const qHits=look.filter(l=>hasStrongQuestionNo(l)||/^[【\[]\s*\d{1,4}\s*[】\]]/.test(l)||isQuestionStart(l)||/^\d{1,4}\s*[.、．:：]\s*$/.test(l)).length;
  const aHits=look.filter(isAnswerAnalysisEntryLine).length;
  return qHits>=1 || aHits>=1 || chineseSection;
}
function sectionKeyFromText(s){
  const x=String(s||'').replace(/\s+/g,'');
  if(!x)return'';
  if(/言语|语言/.test(x))return'verbal';
  if(/数学|数量/.test(x))return'math';
  if(/图形/.test(x))return'graph';
  if(/逻辑|定义判断|类比推理|判断推理/.test(x))return'logic';
  if(/资料分析|材料分析/.test(x))return'data';
  if(/判断题|正误|是非/.test(x))return'judge';
  if(/多选|多项/.test(x))return'multiple';
  if(/单选|单项/.test(x))return'single';
  return'';
}
function hasAnswerAnalysisSignal(text){
  const lines=normalizeImportText(text).split('\n').map(x=>x.trim()).filter(Boolean);
  for(let i=0;i<lines.length;i++){
    if(isAnswerSectionHeading(lines[i],lines.slice(i+1,i+7)))return true;
  }
  for(let i=0;i<lines.length-2;i++){
    const win=lines.slice(i,i+4);
    if(win.filter(isAnswerAnalysisEntryLine).length>=2)return true;
  }
  return false;
}
function extractObjectiveAnswerFromText(raw,allowLeading=true){
  let s=String(raw||'').trim();
  if(!s)return [];
  s=s.replace(/^[【\[]\s*(?:答案|正确答案|参考答案|标准答案)\s*[】\]]\s*/,'').trim();
  s=s.replace(/^(?:答案|正确答案|参考答案|标准答案)\s*[:：]\s*/,'').trim();
  let m=s.match(/^[【\[]\s*(?:解析|答案解析|试题解析)\s*[】\]]\s*(?:选\s*)?([A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)/i);
  if(m)return splitAnswer(m[1]);
  m=s.match(/^(?:解析|答案解析|试题解析|分析|详解|说明|思路|解题思路)\s*[:：]\s*(?:选\s*)?([A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)/i);
  if(m)return splitAnswer(m[1]);
  m=s.match(/^(?:答|答案)\s*[:：]\s*(?:选\s*)?([A-Ga-g]{1,7}|[1-9]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)/i);
  if(m)return splitAnswer(m[1]);
  m=s.match(/(?:故|因此|所以|故而|因而)?\s*(?:本题)?\s*(?:正确)?答案\s*(?:为|是|选|选择)?\s*([A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)(?:项|选项)?/i);
  if(m)return splitAnswer(m[1]);
  m=s.match(/(?:故|因此|所以|故而|因而)\s*(?:本题)?\s*(?:应|可)?\s*(?:选|选择)\s*([A-Ga-g]{1,7})(?:项|选项)?/i);
  if(m)return splitAnswer(m[1]);
  m=s.match(/(?:本题|此题)\s*(?:应|可)?\s*(?:选|选择)\s*([A-Ga-g]{1,7})(?:项|选项)?/i);
  if(m)return splitAnswer(m[1]);
  if(allowLeading){
    m=s.match(/^([A-Ga-g]{1,7}|[1-9]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)(?=$|[\s。．.、，,；;：:]|【|\[|解析|分析)/i);
    if(m)return splitAnswer(m[1]);
  }
  return [];
}
function parseAnswerAnalysisEntries(text){
  const lines=normalizeImportText(text).split('\n').map(x=>x.trim()).filter(Boolean);
  const entries=[];let currentGroup='';let inAnswerSection=false;let currentNumber='';let currentEntry=null;
  const ansToken='[A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False';
  const push=(number,ans,analysis='',raw='',group=currentGroup)=>{
    const a=Array.isArray(ans)?ans:splitAnswer(ans);
    if(!number||!a.length)return;
    // 答案解析区只接收客观题/判断题答案；纯数字“2、3、5”这类是解析文字，不是答案。
    const valid=a.filter(x=>/^[A-G]$/.test(String(x||'').toUpperCase())||isJudgeSymbolAnswer(x));
    if(!valid.length)return;
    const entry={number:String(number),group:group||'',answer:valid.map(x=>String(x).toUpperCase()),analysis:String(analysis||'').trim(),raw:String(raw||'')};
    entries.push(entry);currentEntry=entry;currentNumber=String(number);
  };
  const appendAnalysis=(txt)=>{if(currentEntry&&txt){const t=String(txt||'').trim();if(t)currentEntry.analysis=(currentEntry.analysis?currentEntry.analysis+'\n':'')+t;}};
  const parseNumberedLine=(line,onlyStrong=false)=>{
    let m;
    // 1.【答案】A【解析】... / 1.答案：A ...
    m=line.match(/^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?[.、．:：]?\s*(?:【\s*(?:答案|正确答案|参考答案|标准答案)\s*】|\[\s*(?:答案|正确答案|参考答案|标准答案)\s*\]|(?:答案|正确答案|参考答案|标准答案)\s*[:：])\s*(.+)$/i);
    if(m){
      const a=extractObjectiveAnswerFromText(m[2],true);
      if(a.length)return {number:m[1],answer:a,analysis:m[2].replace(/^([A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)\s*[。．.、，,；;：:]?/i,'').trim(),raw:line};
    }
    // 1.【解析】A。... / 1.解析：A。... / 1.分析：选D...
    m=line.match(/^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?[.、．:：]?\s*(?:【\s*(?:解析|答案解析|试题解析)\s*】|\[\s*(?:解析|答案解析|试题解析)\s*\]|(?:解析|答案解析|试题解析|分析)\s*[:：])\s*(.+)$/i);
    if(m){
      const a=extractObjectiveAnswerFromText(m[2],true);
      if(a.length)return {number:m[1],answer:a,analysis:m[2],raw:line};
    }
    // 1.A.【解析】... / 1.A 解析... / 16.B352
    m=line.match(/^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?[.、．:：]\s*([A-Ga-g]{1,7})(?=$|[\s。．.、，,；;：:]|【|\[|解析|分析|\d)/i);
    if(m){
      const rest=line.slice(m[0].length).trim();
      return {number:m[1],answer:splitAnswer(m[2]),analysis:rest,raw:line};
    }
    if(onlyStrong)return null;
    return null;
  };
  for(let i=0;i<lines.length;i++){
    let line=lines[i];const next=lines.slice(i+1,i+8);
    if(isAnswerSectionHeading(line,next)){inAnswerSection=true;currentEntry=null;currentNumber='';if(/解析|答案及解析|答案与解析|试题解析|真题答案/.test(line))currentGroup='';continue;}
    if(inAnswerSection&&isGenericQuestionSectionHeading(line,next)){inAnswerSection=false;currentEntry=null;currentNumber='';currentGroup=line;continue;}
    if(isGenericQuestionSectionHeading(line,next)){currentGroup=line;currentNumber='';currentEntry=null;continue;}
    const heading=getHeadingType(line);
    if(heading){currentGroup=heading;continue;}

    // 优先处理紧凑答案表：1【答案】D 2【答案】D 3【答案】B ...
    if((inAnswerSection || /\d{1,4}\s*(?:题)?\s*【\s*(?:答案|正确答案|参考答案|标准答案)\s*】/.test(line)) && (line.match(/【\s*(?:答案|正确答案|参考答案|标准答案)\s*】/g)||[]).length>=2){
      const compactAnswerRe=/(?:第\s*)?(\d{1,4})\s*(?:题)?\s*(?:[.、．:：])?\s*【\s*(?:答案|正确答案|参考答案|标准答案)\s*】\s*([A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)/gi;
      let compactHits=[];let cm;
      while((cm=compactAnswerRe.exec(line)))compactHits.push({number:cm[1],ans:cm[2],index:cm.index});
      if(compactHits.length>=2){
        compactHits.forEach((h,idx)=>{
          const end=idx+1<compactHits.length?compactHits[idx+1].index:line.length;
          push(h.number,h.ans,line.slice(h.index,end),line.slice(h.index,end));
        });
        continue;
      }
    }

    const numbered=parseNumberedLine(line,!inAnswerSection);
    if(numbered){
      push(numbered.number,numbered.answer,numbered.analysis,numbered.raw);
      continue;
    }

    // 紧凑答案表：1【答案】D 2【答案】D 3【答案】B ...
    if(inAnswerSection || /\d{1,4}\s*(?:题)?\s*【\s*(?:答案|正确答案|参考答案|标准答案)\s*】/.test(line)){
      const compactAnswerRe=/(?:第\s*)?(\d{1,4})\s*(?:题)?\s*(?:[.、．:：])?\s*【\s*(?:答案|正确答案|参考答案|标准答案)\s*】\s*([A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)/gi;
      let compactHits=[];let cm;
      while((cm=compactAnswerRe.exec(line)))compactHits.push({number:cm[1],ans:cm[2],index:cm.index});
      if(compactHits.length>=1){
        compactHits.forEach((h,idx)=>{
          const end=idx+1<compactHits.length?compactHits[idx+1].index:line.length;
          push(h.number,h.ans,line.slice(h.index,end),line.slice(h.index,end));
        });
        continue;
      }
    }

    // 答案区里的一行多个短答案：1.A 2.B 3.C（不接收数字答案，避免“2、3、5”解析文字误拆）
    if(inAnswerSection){
      const pairRe=/(?:第\s*)?(\d{1,4})\s*(?:题)?[.、．:：]\s*(?:【\s*(?:答案|正确答案|参考答案|标准答案|解析|答案解析|试题解析)\s*】\s*)?(?:答案|正确答案|参考答案|标准答案|解析|分析)?\s*[:：]?\s*(?:选\s*)?([A-Ga-g]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F|True|False)(?=\s|$|[。．.、，,；;：:]|【|\[|\d{1,4}\s*[.、．:：])/gi;
      let hits=[];let m;
      while((m=pairRe.exec(line)))hits.push({number:m[1],ans:m[2],index:m.index});
      if(hits.length>=2){
        hits.forEach((h,idx)=>{
          const end=idx+1<hits.length?hits[idx+1].index:line.length;
          push(h.number,h.ans,line.slice(h.index,end),line.slice(h.index,end));
        });
        continue;
      }
    }

    // 记录普通题号，供紧随其后的“分析：选D / 答：选C”回填。
    const qNo=line.match(/^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?[.、．:：]\s*\S+/)||line.match(/^\s*[【\[]\s*(\d{1,4})\s*[】\]]\s*\S+/);
    const lineIsAnswerish=isAnswerAnalysisEntryLine(line)||/【\s*(?:答案|解析)\s*】|(?:答案|正确答案|参考答案|标准答案|解析|分析|答)\s*[:：]|故(?:答案)?选|本题(?:答案)?(?:为|选)|因此.*(?:答案|选)|所以.*选/.test(line);
    if(qNo&&!lineIsAnswerish){currentNumber=qNo[1];currentEntry=null;}

    // 无题号但当前题块下的“分析：选D / 答：选C / 故答案选B”。只在答案区或刚读到题号后生效。
    if((inAnswerSection||currentNumber) && /^(?:分析|解析|答|答案|详解|说明|思路|解题思路)\s*[:：]|故(?:答案)?选|故选择|因此|所以|本题/.test(line)){
      const a=extractObjectiveAnswerFromText(line,false);
      if(a.length){push(currentNumber,a,line,line);continue;}
      appendAnalysis(line);continue;
    }
    if(inAnswerSection&&currentEntry&&!isAnswerAnalysisEntryLine(line)&&!hasStrongQuestionNo(line))appendAnalysis(line);
  }
  // 去重：相同题号按出现顺序保留；如果答案相同则合并解析。重复题号可能属于不同分区，不能全局合并掉。
  const out=[];
  entries.forEach(e=>{
    const last=out[out.length-1];
    if(last&&last.number===e.number&&(last.answer||[]).join('')===(e.answer||[]).join('')){
      if(e.analysis&&!last.analysis.includes(e.analysis))last.analysis+=(last.analysis?'\n':'')+e.analysis;
    }else out.push(e);
  });
  return out;
}
function stripAnswerAnalysisTextForQuestions(text){
  const lines=normalizeImportText(text).split('\n').map(x=>x.trim()).filter(Boolean);
  const out=[];let inAnswerSection=false;
  for(let i=0;i<lines.length;i++){
    const line=lines[i];const next=lines.slice(i+1,i+7);
    if(isAnswerSectionHeading(line,next)){inAnswerSection=true;continue;}
    if(inAnswerSection&&isGenericQuestionSectionHeading(line,next)){inAnswerSection=false;out.push(line);continue;}
    if(inAnswerSection)continue;
    if(isAnswerAnalysisEntryLine(line))continue;
    if(/^(?:分析|解析|答|答案|详解|说明|思路|解题思路)\s*[:：]\s*(?:选\s*)?[A-Ga-g]/.test(line))continue;
    if(/^(?:故|因此|所以|故而|因而)\s*(?:本题)?\s*(?:正确)?答案/.test(line))continue;
    out.push(line);
  }
  return out.join('\n');
}
function mergeAnswerAnalysisEntries(questions,entries){
  const qs=(questions||[]).map((q,i)=>({...q,answer:[...(q.answer||[])],analysis:q.analysis||'',number:q.number||i+1}));
  const warnings=[];let matched=0;let cursor=0;
  const qSec=(q)=>sectionKeyFromText(q.category||q.group||'');
  const eSec=(e)=>sectionKeyFromText(e.group||'');
  const compatible=(q,e)=>{
    const ans=e.answer||[];
    if(q.type==='judge')return ans.some(a=>isJudgeSymbolAnswer(a)||['A','B'].includes(String(a).toUpperCase()));
    if(['single','multiple'].includes(q.type))return ans.every(a=>/^[A-G]$/.test(String(a).toUpperCase()));
    return true;
  };
  const canUse=(q,e,strictSection=false)=>{
    if(String(q.number)!==String(e.number))return false;
    if(!compatible(q,e))return false;
    const es=eSec(e), qsx=qSec(q);
    if(es&&qsx&&es!==qsx)return false;
    if(strictSection&&es&&!qsx)return false;
    return true;
  };
  for(const e of (entries||[])){
    const es=eSec(e);
    let idx=-1;
    // 1) 优先在当前位置之后找“题号 + 分区”都匹配的题。
    for(let i=cursor;i<qs.length;i++){
      if(canUse(qs[i],e,true)){idx=i;break;}
    }
    // 2) 再允许全局同分区未答题匹配。
    if(idx<0){
      for(let i=0;i<qs.length;i++){
        if(!qs[i].answer.length&&canUse(qs[i],e,true)){idx=i;break;}
      }
    }
    // 3) 没有分区信息时，才按旧的顺序题号匹配。
    if(idx<0&&!es){
      for(let i=cursor;i<qs.length;i++){if(canUse(qs[i],e,false)){idx=i;break;}}
      if(idx<0){for(let i=0;i<qs.length;i++){if(!qs[i].answer.length&&canUse(qs[i],e,false)){idx=i;break;}}}
    }
    // 4) 有明确分区但项目没有解析出对应分区题目，例如图形题只有图片，跳过，不要污染后续逻辑题。
    if(idx<0)continue;
    qs[idx].answer=normalizeAnswer(e.answer,qs[idx].options||[],qs[idx].type);
    if(e.analysis)qs[idx].analysis=e.analysis;
    cursor=idx+1;matched++;
  }
  if(entries.length&&!matched)warnings.push('检测到答案解析区，但未能按题号合并答案。');
  return {questions:qs.map((q,i)=>normalizeQuestion(q,i)),warnings,matched};
}
function parseDocumentWithAnswerSections(text){
  if(!hasAnswerAnalysisSignal(text))return [];
  const qText=stripAnswerAnalysisTextForQuestions(text);
  const entries=parseAnswerAnalysisEntries(text);
  if(!entries.length)return [];
  const qCandidates=[];
  const push=(name,fn)=>{
    try{
      const qs=fn().map((q,i)=>normalizeQuestion(q,i)).filter(q=>q.question);
      if(qs.length)qCandidates.push({name,questions:qs,score:scoreParsedQuestions(qs,{})});
    }catch(_){}
  };
  push('题目区标准试卷段落解析',()=>parseStructuredExamText(qText));
  push('题目区标准逐行解析',()=>parseTextQuestionsBase(qText));
  push('题目区紧凑解析',()=>parseTextQuestionsBase(forceSplitCompactText(qText)));

  const mergedCandidates=qCandidates.map(c=>{
    const merged=mergeAnswerAnalysisEntries(c.questions,entries);
    const qs=merged.questions||[];
    const warnings=collectImportWarnings(qs);
    const answered=qs.filter(q=>(q.answer||[]).length).length;
    const missing=warnings.filter(w=>/缺少答案|缺少参考答案/.test(w)).length;
    const hard=warnings.length-missing;
    const polluted=qs.filter(q=>/【\s*(?:答案|解析)\s*】|答案解析|正确答案|参考答案/.test(q.question||'')).length;
    // 这里不能只看题量。标准段落解析在 2014 这类“每区后置答案”文本中
    // 可能少识别一两个图片题/材料引导块，但题号、分区、答案匹配最稳。
    const score=answered*120 - missing*90 - hard*140 - polluted*250 + Math.min(qs.length,answered)*5;
    return {...c,questions:qs,warnings,answered,score};
  }).filter(c=>c.questions&&c.questions.length);

  mergedCandidates.sort((a,b)=>b.score-a.score || b.answered-a.answered || a.warnings.length-b.warnings.length || b.questions.length-a.questions.length);
  const best=mergedCandidates[0];
  if(!best)return [];
  return best.questions;
}

function scoreAnswerSectionCandidate(qs,profile){
  const arr=qs||[];
  let score=scoreParsedQuestions(arr,profile||{});
  const answered=arr.filter(q=>(q.answer||[]).length).length;
  const answerRate=arr.length?answered/arr.length:0;
  const polluted=arr.filter(q=>/【\s*(?:答案|解析)\s*】|答案解析|正确答案|参考答案/.test(q.question||'')).length;
  const warnings=collectImportWarnings(arr);
  const hard=warnings.filter(w=>!/缺少答案|缺少参考答案|多选题只有一个答案/.test(w)).length;
  score+=answered*80;
  if(answerRate>=0.5)score+=600;
  if(answerRate>=0.75)score+=800;
  score-=polluted*250;
  score-=hard*90;
  if(profile&&profile.expectedByHeadings){score-=Math.min(400,Math.abs(arr.length-profile.expectedByHeadings)*12)}
  return score;
}

function parseTextQuestions(text,strategy='auto'){
  const original=String(text||'');
  const profile=analyzeQuestionTextProfile(original);
  const candidates=[];
  const addCandidate=(name,fn)=>{
    try{
      const qs=fn().map((q,i)=>normalizeQuestion(q,i)).filter(q=>q.question);
      candidates.push({name,questions:qs,score:scoreParsedQuestions(qs,profile),warnings:collectImportWarnings(qs)});
    }catch(e){candidates.push({name,questions:[],score:-9999,warnings:['解析失败：'+e.message]});}
  };
  const evaluateCandidate=(candidate)=>{
    if(!candidate||!candidate.questions||!candidate.questions.length){
      return {qtyOk:false, warnOk:false, typeOk:false, hardCount:9999, warnings:[]};
    }
    const warnings=importWarningsForStrategy(candidate.questions,profile);
    const hard=warnings.filter(w=>!/缺少答案|缺少参考答案|多选题只有一个答案/.test(w));
    const expected=profile.expectedByHeadings||0;
    const qtyOk=!expected||Math.abs(candidate.questions.length-expected)<=Math.max(2,Math.ceil(expected*0.05));
    const typeExpected=profile.expectedByType||{};
    const st=countTypes(candidate.questions||[]);
    const typeOk=(!typeExpected.judge||st.judge>=Math.max(0,typeExpected.judge-2)) && (!typeExpected.single||st.single+st.multiple+st.multi+st.judge+st.blank+st.short>=candidate.questions.length*0.9);
    const warnOk=hard.length<=Math.max(2,Math.ceil(candidate.questions.length*0.02));
    return {qtyOk,warnOk,typeOk,hardCount:hard.length,warnings};
  };
  const strategyLabel={auto:'自动推荐',standard:'标准逐行解析',volume:'分卷分区三层解析',compact:'紧凑格式解析'}[strategy]||'自动推荐';
  if(strategy==='standard'){
    addCandidate('标准试卷段落解析',()=>parseStructuredExamText(original));
    addCandidate('标准逐行解析',()=>parseTextQuestionsBase(original));
  }
  else if(strategy==='volume')addCandidate('分卷分区三层解析',()=>parseByVolumeAndSections(original));
  else if(strategy==='compact')addCandidate('紧凑题干选项解析',()=>parseTextQuestionsBase(forceSplitCompactText(original)));
  else{
    addCandidate('标准试卷段落解析',()=>parseStructuredExamText(original));
    addCandidate('标准逐行解析',()=>parseTextQuestionsBase(original));
    if(profile.hasAnswerAnalysisSection||hasAnswerAnalysisSignal(original)){
      try{
        const qs=parseDocumentWithAnswerSections(original).map((q,i)=>normalizeQuestion(q,i)).filter(q=>q.question);
        if(qs.length)candidates.push({name:'题目区 + 答案解析区合并解析',questions:qs,score:scoreAnswerSectionCandidate(qs,profile),warnings:collectImportWarnings(qs)});
      }catch(e){candidates.push({name:'题目区 + 答案解析区合并解析',questions:[],score:-9999,warnings:['解析失败：'+e.message]});}
    }

    const baselineCandidates=candidates
      .filter(c=>['标准试卷段落解析','标准逐行解析'].includes(c.name))
      .filter(c=>c.questions&&c.questions.length)
      .map(c=>({...c,eval:evaluateCandidate(c)}));

    baselineCandidates.sort((a,b)=>{
      const aStable=(a.eval.qtyOk&&a.eval.typeOk?1:0);
      const bStable=(b.eval.qtyOk&&b.eval.typeOk?1:0);
      return bStable-aStable || a.eval.hardCount-b.eval.hardCount || b.questions.length-a.questions.length || b.score-a.score;
    });

    const primary=baselineCandidates[0]||candidates[0]||{questions:[],warnings:[],score:-9999};
    const primaryEval=primary.eval||evaluateCandidate(primary);
    primary.warnings=primaryEval.warnings;
    primary.score=scoreParsedQuestions(primary.questions||[],profile);

    const localRisk=countLocalRepairWarnings(primary.questions||[],profile);
    const localRepairWorthTrying=(primary.questions||[]).length>0 && primaryEval.qtyOk && primaryEval.typeOk && localRisk>0 && localRisk<=Math.max(8,Math.ceil((primary.questions||[]).length*0.04));
    if(localRepairWorthTrying){
      const repaired=repairParsedQuestionsLocally(original,primary.questions,profile);
      if(repaired.repaired>0){
        const qs=repaired.questions.map((q,i)=>normalizeQuestion(q,i)).filter(q=>q.question);
        const warnings=importWarningsForStrategy(qs,profile);
        const score=scoreParsedQuestions(qs,profile)+Math.min(120,repaired.repaired*20);
        candidates.push({name:'标准解析 + 异常局部修复',questions:qs,score,warnings,segments:repaired.segments});
      }
    }

    const stableMainline=candidates
      .filter(c=>['标准试卷段落解析','标准逐行解析','标准解析 + 异常局部修复','题目区 + 答案解析区合并解析'].includes(c.name))
      .filter(c=>c.questions&&c.questions.length)
      .map(c=>({...c,eval:evaluateCandidate(c)}))
      .sort((a,b)=>{
        const aGood=(a.eval.qtyOk&&a.eval.typeOk&&a.eval.warnOk?1:0);
        const bGood=(b.eval.qtyOk&&b.eval.typeOk&&b.eval.warnOk?1:0);
        return bGood-aGood || b.score-a.score || a.eval.hardCount-b.eval.hardCount || b.questions.length-a.questions.length;
      });

    const mainlineBest=stableMainline[0]||primary;
    const mainlineEval=mainlineBest.eval||evaluateCandidate(mainlineBest);
    const highRisk=profile.hasVolumeHeading||profile.repeatedQuestionNumbers||profile.inlineOptionLikely||profile.inlineAnswerLikely;
    const needFullComplex=!(mainlineBest.questions||[]).length || !mainlineEval.qtyOk || !mainlineEval.typeOk || (!mainlineEval.warnOk && mainlineEval.hardCount>Math.max(8,Math.ceil((mainlineBest.questions||[]).length*0.04))) || (highRisk && !localRepairWorthTrying && !primaryEval.warnOk);

    if(needFullComplex){
      if(profile.hasVolumeHeading||profile.repeatedQuestionNumbers||profile.hasTypeSections)addCandidate('分卷分区三层解析',()=>parseByVolumeAndSections(original));
      if(profile.inlineOptionLikely||profile.inlineAnswerLikely||!mainlineEval.warnOk||!mainlineEval.qtyOk)addCandidate('紧凑题干选项解析',()=>parseTextQuestionsBase(forceSplitCompactText(original)));
    }
  }

  let best=candidates.slice().sort((a,b)=>b.score-a.score)[0]||{name:'标准逐行解析',questions:[],score:0,warnings:[]};
  if(strategy==='auto'){
    const stableCandidates=candidates
      .filter(c=>['标准试卷段落解析','标准逐行解析','标准解析 + 异常局部修复','题目区 + 答案解析区合并解析'].includes(c.name))
      .filter(c=>c.questions&&c.questions.length)
      .map(c=>({...c,eval:evaluateCandidate(c)}))
      .sort((a,b)=>{
        const aGood=(a.eval.qtyOk&&a.eval.typeOk&&a.eval.warnOk?1:0);
        const bGood=(b.eval.qtyOk&&b.eval.typeOk&&b.eval.warnOk?1:0);
        return bGood-aGood || b.score-a.score || a.eval.hardCount-b.eval.hardCount || b.questions.length-a.questions.length;
      });
    if(stableCandidates.length && stableCandidates[0].eval.qtyOk && stableCandidates[0].eval.typeOk && stableCandidates[0].eval.warnOk){
      best=stableCandidates[0];
    }
    // 有明显答案解析区时，“题目区 + 答案解析区合并解析”不是普通复杂兜底，
    // 而是结构化解析候选；只要答案命中率明显更高且总评分领先，就允许覆盖标准主线。
    const answerSectionCandidate=candidates.find(c=>c.name==='题目区 + 答案解析区合并解析'&&c.questions&&c.questions.length);
    if(answerSectionCandidate){
      const answered=(answerSectionCandidate.questions||[]).filter(q=>(q.answer||[]).length).length;
      const answerRate=answerSectionCandidate.questions.length?answered/answerSectionCandidate.questions.length:0;
      const bestAnswered=(best.questions||[]).filter(q=>(q.answer||[]).length).length;
      const bestRate=(best.questions||[]).length?bestAnswered/(best.questions||[]).length:0;
      if(answerRate>=0.35 && answerRate>bestRate+0.25 && answerSectionCandidate.score>best.score+300){
        best=answerSectionCandidate;
      }
    }
  }

  const stats=countTypes(best.questions||[]);
  const profileBits=[];
  if(profile.hasVolumeHeading)profileBits.push('检测到分卷');
  if(profile.hasTypeSections)profileBits.push('检测到题型分区');
  if(profile.repeatedQuestionNumbers)profileBits.push('题号存在重复');
  if(profile.inlineOptionLikely)profileBits.push('存在同一行选项');
  if(profile.inlineAnswerLikely)profileBits.push('存在题尾答案标记');
  if(profile.hasAnswerAnalysisSection)profileBits.push('检测到答案解析区');
  const candidateLine=candidates.map(c=>`${c.name}${c.questions.length}题/质量${c.score}${c.segments?.length?'（局部修复'+c.segments.length+'处）':''}`).join('；');
  const expected=profile.expectedByHeadings?`；标题预期约${profile.expectedByHeadings}题，实际${best.questions.length}题，差值${best.questions.length-profile.expectedByHeadings}`:'';
  importReport=`解析模式：${strategyLabel}；采用策略：${best.name}。${profileBits.length?'格式画像：'+profileBits.join('、')+'。':''}候选结果：${candidateLine}。最终识别：${best.questions.length}题（单选${stats.single||0}、多选${(stats.multiple||0)+(stats.multi||0)}、判断${stats.judge||0}、填空${stats.blank||0}、简答${stats.short||0}）${expected}。`;
  importDiagnostics={mode:strategyLabel,strategy:best.name,profile,candidates:candidates.map(c=>({name:c.name,questions:c.questions.length,score:c.score,warnings:c.warnings||[],segments:c.segments||[]})),expected:{total:profile.expectedByHeadings||0,types:profile.expectedByType||{}},stats,warnings:best.warnings||[]};
  return best.questions;
}

function analyzeQuestionTextProfile(text){
  const t=normalizeImportText(text);
  const lines=t.split('\n').map(x=>x.trim()).filter(Boolean);
  const qnums=[];
  lines.forEach(l=>{const m=l.match(/^\s*(?:第\s*)?(\d{1,3})\s*(?:题)?[、.．:：]/);if(m)qnums.push(m[1]);});
  const seen=new Set();let repeated=false;for(const n of qnums){if(seen.has(n)){repeated=true;break}seen.add(n)}
  return {
    lineCount:lines.length,
    hasVolumeHeading:lines.some(isVolumeHeading)||/202\d年.*(?:试卷|考试).*?[·._-]?[一二三四五六七八九十0-9]+卷/.test(t),
    hasTypeSections:lines.some(l=>!!getHeadingType(l)),
    repeatedQuestionNumbers:repeated,
    inlineOptionLikely:/[A-Ga-g]\s*[、.．:：]?[^\n]{1,50}[;；]\s*[B-Gb-g]\s*[、.．:：]?/.test(t)||/[A-Ga-g][、.．:：][^\n]{1,50}[B-Gb-g][、.．:：]/.test(t),
    inlineAnswerLikely:/[【\[（(]\s*(?:答案|正确答案|参考答案|标准答案)\s*[:：]/.test(t),
    hasAnswerAnalysisSection:hasAnswerAnalysisSignal(t),
    expectedByHeadings:estimateExpectedQuestionCount(t),
    expectedByType:estimateExpectedQuestionTypeCounts(t)
  };
}
function estimateExpectedQuestionCount(text){
  let total=0;let m;
  const direct=/(?:单选题|单项选择题|多选题|多项选择题|判断题|填空题|简答题)[^\n]{0,25}?共\s*(\d{1,4})\s*题/g;
  while((m=direct.exec(text))){const n=Number(m[1]);if(n>0)total+=n}
  if(total)return total;
  const re=/(单选题|多选题|判断题|填空题|简答题)[^\n]{0,30}?每题\s*([\d.]+)\s*分[^\n]{0,20}?共\s*([\d.]+)\s*分/g;
  while((m=re.exec(text))){const per=Number(m[2]),sum=Number(m[3]);if(per>0&&sum>0)total+=Math.round(sum/per)}
  if(total)return total;
  const loose=/(单项选择题|单选题|单选|多项选择题|多选题|多选|判断题|判断|填空题|填空|简答题|简答)[^\n]{0,20}?(\d{1,4})\s*(?:题|道)/g;
  while((m=loose.exec(text))){if(!/每题\s*$/.test(m[0])){const n=Number(m[2]);if(n>0)total+=n}}
  if(total)return total;
  const range=/(单项选择题|单选题|单选|多项选择题|多选题|多选|判断题|判断|填空题|填空|简答题|简答)[^\n]{0,15}?(\d{1,4})\s*(?:-|~|—|至|到)\s*(\d{1,4})/g;
  while((m=range.exec(text))){const a=Number(m[2]),b=Number(m[3]);if(b>=a&&b-a<2000)total+=b-a+1}
  return total||0;
}
function estimateExpectedQuestionTypeCounts(text){
  const out={single:0,multiple:0,judge:0,blank:0,short:0};
  const map={单选题:'single',单选:'single',单项选择题:'single',多选题:'multiple',多选:'multiple',多项选择题:'multiple',判断题:'judge',判断:'judge',填空题:'blank',填空:'blank',简答题:'short',简答:'short'};
  let m;
  const direct=/(单项选择题|单选题|单选|多项选择题|多选题|多选|判断题|判断|填空题|填空|简答题|简答)[^\n]{0,25}?共\s*(\d{1,4})\s*题/g;
  while((m=direct.exec(text))){const t=map[m[1]];if(t)out[t]+=Number(m[2]||0)}
  const score=/(单项选择题|单选题|单选|多项选择题|多选题|多选|判断题|判断|填空题|填空|简答题|简答)[^\n]{0,30}?每题\s*([\d.]+)\s*分[^\n]{0,20}?共\s*([\d.]+)\s*分/g;
  while((m=score.exec(text))){const t=map[m[1]],per=Number(m[2]),sum=Number(m[3]);if(t&&per>0&&sum>0)out[t]+=Math.round(sum/per)}
  const loose=/(单项选择题|单选题|单选|多项选择题|多选题|多选|判断题|判断|填空题|填空|简答题|简答)[^\n]{0,20}?(\d{1,4})\s*(?:题|道)/g;
  while((m=loose.exec(text))){const t=map[m[1]],n=Number(m[2]);if(t&&n>0&&!/每题\s*$/.test(m[0]))out[t]=Math.max(out[t]||0,n)}
  const range=/(单项选择题|单选题|单选|多项选择题|多选题|多选|判断题|判断|填空题|填空|简答题|简答)[^\n]{0,15}?(\d{1,4})\s*(?:-|~|—|至|到)\s*(\d{1,4})/g;
  while((m=range.exec(text))){const t=map[m[1]],a=Number(m[2]),b=Number(m[3]);if(t&&b>=a&&b-a<2000)out[t]=Math.max(out[t]||0,b-a+1)}
  return out;
}
function scoreParsedQuestions(qs,profile){
  const arr=qs||[];let score=arr.length*10;
  const warnings=collectImportWarnings(arr);
  // 题目文件和答案文件分离时，题目本身没有答案是正常现象，不能让“缺少答案”主导策略选择。
  const sourceLikelyHasAnswers=!!(profile.inlineAnswerLikely||profile.hasAnswerAnalysisSection) || arr.some(q=>(q.answer||[]).length);
  warnings.forEach(w=>{score-=w.includes('缺少答案')&&!sourceLikelyHasAnswers?3:25});
  let suspicious=0;
  arr.forEach(q=>{
    if((q.options||[]).some(o=>String(o.text||'').length>220||/【\s*答案|\b\d{1,3}\s*[、.．:：].+【\s*答案/.test(o.text)))suspicious++;
    if(/【\s*答案|正确答案|参考答案/.test(q.question||''))suspicious++;
    if((q.question||'').length>260)suspicious++;
    if(localRepairRiskStatus(q,profile)!=='正常')suspicious++;
  });
  score-=suspicious*30;
  if(profile.expectedByHeadings){const diff=Math.abs(arr.length-profile.expectedByHeadings);score-=Math.min(300,diff*12);}
  return score;
}
function forceSplitCompactText(text){
  let s=String(text||'');
  s=s.replace(/([。？！?])\s*(?=A\s*[^A-G\n]{1,80}\s*[\n\r]+\s*B\s*[、.．:：\s])/g,'$1 ');
  s=s.replace(/([^\n])\s+(?=A\s*[^A-G\n]{1,80}\s*B\s*[、.．:：])/g,'$1\n');
  return preSplitVolumeAndCompactQuestions(s);
}
function parseStructuredExamText(text){
  const s=normalizeImportText(text);
  const lines=s.split('\n').map(x=>x.trim()).filter(Boolean);
  const questions=[]; let currentType=''; let current=null; let collectingAnalysis=false;
  const flush=()=>{
    if(!current)return;
    const stem=(current.questionLines||[]).join(' ').replace(/\s+/g,' ').trim();
    const mergedOptions=mergeDuplicateOptions(repairEmbeddedOptions(current.options||[])).filter(o=>o.text);
    let answer=[...(current.answer||[])];
    let type=current.type||guessType(stem,mergedOptions,answer,current.group||'');
    const fixed=cleanQuestionStemAndAnswer(stem,answer,type,mergedOptions);
    answer=isTextType(type)?splitTextAnswer(fixed.answer.join('；')):normalizeAnswer(fixed.answer,mergedOptions,type);
    const finalOptions=(type==='judge'&&!mergedOptions.length)?[{key:'A',text:'正确'},{key:'B',text:'错误'}]:mergedOptions;
    questions.push({
      id:'structured_'+Date.now()+'_'+questions.length,
      type,
      number:current.number||questions.length+1,
      question:fixed.question,
      options:finalOptions,
      answer,
      analysis:(current.analysisLines||[]).join('\n').trim(),
      group:current.group||'',
      volume:current.volume||''
    });
    current=null;
  };
  const beginQuestion=(number,lineAfterNo)=>{
    flush();
    current={number,questionLines:[],options:[],answer:[],analysisLines:[],group:currentType,volume:'',type:mapType(currentType)||''};
    if(lineAfterNo)current.questionLines.push(lineAfterNo.trim());
  };
  for(let i=0;i<lines.length;i++){
    let line=lines[i];
    const heading=getHeadingType(line);
    if(heading){flush();currentType=heading;continue;}
    if(isImportNoiseLine(line))continue;
    // Handle 答案解析 prefix: strip and re-process as answer content
    if(/^答案解析\s*\d/.test(line)){
      const after=line.replace(/^答案解析\s*/,'');
      if(current&&collectingAnalysis){current.analysisLines.push(after);collectingAnalysis=false;}
      const combinedM=after.match(/^(\d{1,4})\s*[.、．]\s*(?:【答案】\s*)?([A-Ga-g]{1,7})\s*(?:【解析】\s*(.*))?$/);
      if(combinedM){
        flush();current={number:Number(combinedM[1]),questionLines:[],options:[],answer:splitAnswerByType(combinedM[2],currentType||''),analysisLines:combinedM[3]?[combinedM[3].trim()]:[],group:currentType,volume:'',type:mapType(currentType)||''};collectingAnalysis=!!combinedM[3];
      } else {
        const compactReg=/(\d{1,4})\s*[.、．]\s*([A-Ga-g]{1,7})\s*(?=【|$)/g;let cm;const pairs=[];
        while((cm=compactReg.exec(after))!==null)pairs.push({no:Number(cm[1]),ans:cm[2]});
        if(pairs.length){
          flush();
          pairs.forEach((p,idx)=>{
            if(idx>0)flush();
            current={number:p.no,questionLines:[],options:[],answer:splitAnswerByType(p.ans,currentType||''),analysisLines:[],group:currentType,volume:'',type:mapType(currentType)||''};
          });
        } else {
          current.answer=splitAnswerByType(after,current.type||'');
        }
      }
      continue;
    }
    // Handle 解析 continuation lines (e.g., 2．【解析】C。explanation)
    const analysisNum=line.match(/^\s*(\d{1,4})\s*[.、．]\s*【解析】\s*([A-Ga-g]{1,7})\s*[。.]?\s*(.*)$/);
    if(analysisNum){
      if(current&&collectingAnalysis){current.analysisLines.push(line);collectingAnalysis=false;}
      else{flush();current={number:Number(analysisNum[1]),questionLines:[],options:[],answer:splitAnswerByType(analysisNum[2],currentType||''),analysisLines:analysisNum[3]?[analysisNum[3].trim()]:[],group:currentType,volume:'',type:mapType(currentType)||''};collectingAnalysis=!!analysisNum[3];}
      continue;
    }
    // Handle combined 【答案】...【解析】... format (e.g., 21.【答案】C【解析】explanation)
    const combined=line.match(/^\s*(\d{1,4})\s*[.、．]\s*【答案】\s*([A-Ga-g]{1,7})\s*【解析】\s*(.*)$/);
    if(combined){
      if(current&&collectingAnalysis){collectingAnalysis=false;}
      flush();
      current={number:Number(combined[1]),questionLines:[],options:[],answer:splitAnswerByType(combined[2],currentType||''),analysisLines:combined[3]?[combined[3].trim()]:[],group:currentType,volume:'',type:mapType(currentType)||''};
      collectingAnalysis=!!combined[3];
      continue;
    }
    // Handle compact multi-answer lines (e.g., 1.A    2.B    3.C)
    const compactPairs=line.match(/^(\d{1,4})\s*[.、．]\s*(?:【答案】\s*)?([A-Ga-g]{1,7})\s*(?:【解析】\s*[^\d]*?)?(?=\s+\d{1,4}\s*[.、．]|\s*$)/);
    if(compactPairs&&/\d{1,4}\s*[.、．]\s*[A-Ga-g]{1,7}.*\d{1,4}\s*[.、．]\s*[A-Ga-g]{1,7}/.test(line)){
      const compactReg=/(\d{1,4})\s*[.、．]\s*(?:【答案】\s*)?([A-Ga-g]{1,7})\s*(?:【解析】\s*([^\d]*?))?(?=\s+\d{1,4}\s*[.、．]|\s*$)/g;let cm;const pairs=[];
      while((cm=compactReg.exec(line))!==null)pairs.push({no:Number(cm[1]),ans:cm[2],analysis:(cm[3]||'').trim()});
      if(current&&collectingAnalysis){collectingAnalysis=false;}
      pairs.forEach((p,idx)=>{
        if(idx===0&&current&&current.number===p.no&&!current.answer.length){current.answer=splitAnswerByType(p.ans,current.type||'');if(p.analysis)current.analysisLines.push(p.analysis);}
        else{flush();current={number:p.no,questionLines:[],options:[],answer:splitAnswerByType(p.ans,currentType||''),analysisLines:p.analysis?[p.analysis]:[],group:currentType,volume:'',type:mapType(currentType)||''};}
      });
      if(pairs.length)continue;
    }
    const qm=line.match(/^\s*(?:[【\[]\s*(\d{1,4})\s*[】\]]|(?:第\s*)?(\d{1,4})\s*(?:题)?[.、．:：]?)\s*(.*)$/);
    const optionLike=isOptionLine(line)||!!extractInlineOptionsRich(line)||splitInlineOptions(line).length>=2;
    if(qm && !optionLike){
      beginQuestion(Number(qm[1]||qm[2]), qm[3]||'');
      continue;
    }
    if(!current)continue;
    const inlineAnswerTag=extractInlineAnswerTag(line,current.type||'');
    if(inlineAnswerTag.answer.length){current.answer.push(...inlineAnswerTag.answer);line=inlineAnswerTag.text;}
    const trailingAnswer=extractTrailingAnswerFromText(line,current.type||'');
    if(trailingAnswer.answer.length && !isAnswerLine(line)){current.answer.push(...trailingAnswer.answer);line=trailingAnswer.text;}
    if(isAnswerLine(line)){
      const stripped=line.replace(/^(?:【|\[)?\s*(?:正确答案|参考答案|标准答案|答案解析|答案|参考要点|答题要点|Answer|Correct\s*answer)\s*(?:】|\])?\s*[:：]?\s*/i,'').trim();
      const ca=stripped.match(/^(\d{1,4})\s*[.、．]\s*(?:【答案】\s*)?([A-Ga-g]{1,7})\s*【解析】\s*(.*)$/);
      if(ca){current.answer=splitAnswerByType(ca[2],current.type||'');current.analysisLines=[ca[3].trim()];collectingAnalysis=true;}
      else{current.answer=splitAnswerByType(stripped,current.type||'');collectingAnalysis=false;}
      continue;
    }
    if(isAnalysisLine(line)){current.analysisLines.push(line.replace(/^(?:【|\[)?\s*(?:解析|答案解析|试题解析|说明|考点)\s*(?:】|\])?\s*[:：]?\s*/i,'').trim());collectingAnalysis=true;continue;}
    const rich=extractInlineOptionsRich(line);
    if(rich && rich.options.length>=2){
      if(rich.prefix)current.questionLines.push(rich.prefix);
      rich.options.forEach(it=>{
        let key=normalizeOptionKey(it.key), txt=(it.text||'').trim();
        if(it.correct||hasCorrectMark(txt)){current.answer.push(key);txt=removeCorrectMark(txt);}
        if(it.extraAnswer&&it.extraAnswer.length)current.answer.push(...it.extraAnswer);
        if(txt)current.options.push({key,text:txt});
      });
      continue;
    }
    const inline=splitInlineOptions(line);
    if(inline.length>=2){
      inline.forEach(it=>{
        let key=normalizeOptionKey(it.key), txt=(it.text||'').trim();
        if(it.correct||hasCorrectMark(txt)){current.answer.push(key);txt=removeCorrectMark(txt);}
        if(txt)current.options.push({key,text:txt});
      });
      continue;
    }
    const om=line.match(/^\s*([oOxXuUyYvV√✔✓])?\s*(?:[（(]\s*([A-Ga-g1-90])\s*[）)]|([A-Ga-g0])\s*(?:[、.．:：，,]|\s+))\s*(.*)$/);
    if(om){
      let key=normalizeOptionKey(om[2]||om[3]); let txt=(om[4]||'').trim();
      if(om[1]||hasCorrectMark(txt)){current.answer.push(key);txt=removeCorrectMark(txt);}
      if(txt)current.options.push({key,text:txt});
      continue;
    }
    if(collectingAnalysis){current.analysisLines.push(line);continue;}
    if(current.options.length){
      current.options[current.options.length-1].text=(current.options[current.options.length-1].text+' '+line).trim();
    }else{
      current.questionLines.push(line);
    }
  }
  flush();
  return questions.map((q,i)=>normalizeQuestion(q,i)).filter(q=>q.question&&(q.options.length||q.answer.length||q.type==='judge'||isTextType(q.type)));
}
function parseByVolumeAndSections(text){
  const s=preSplitVolumeAndCompactQuestions(normalizeImportText(text));
  const lines=s.split('\n').map(x=>x.trim()).filter(Boolean);
  const blocks=[];let volume='';let group='';let section=[];
  const flush=()=>{
    if(section.length){
      if(!group && !section.some(l=>looksLikeNewQuestionLine(l,group)||hasStrongQuestionNo(l)||hasInlineAnswerTag(l))){section=[];return;}
      const sub=splitQuestionBlocks(section.join('\n'));
      sub.forEach(b=>blocks.push({...b,volume:volume||b.volume||'',group:group||b.group||''}));
      section=[];
    }
  };
  for(const line of lines){
    if(isVolumeHeading(line)){flush();volume=getVolumeLabel(line);group='';continue;}
    const h=getHeadingType(line);if(h){flush();group=h;continue;}
    if(isImportNoiseLine(line))continue;
    section.push(line);
  }
  flush();
  return blocks.map((b,i)=>parseBlock({...b,volume:b.volume||volume,group:b.group||group},i)).filter(q=>q&&q.question&&(q.options.length||q.answer.length||q.type==='judge'||isTextType(q.type))).map((q,i)=>normalizeQuestion(q,i));
}

function normalizeImportText(text){
  let raw=String(text||'');
  if(/<w:t(?:\s[^>]*)?>[\s\S]*?<\/w:t>/.test(raw)){
    try{
      const extracted=docxXmlToText(raw);
      if(extracted&&extracted.trim())raw=extracted;
      else{
        const parts=[];let m;const re=/<w:t(?:\s[^>]*)?>([\s\S]*?)<\/w:t>/g;
        while((m=re.exec(raw)))parts.push(decodeXml(m[1]));
        if(parts.length)raw=parts.join('\n');
      }
    }catch(_){
      const parts=[];let m;const re=/<w:t(?:\s[^>]*)?>([\s\S]*?)<\/w:t>/g;
      while((m=re.exec(raw)))parts.push(decodeXml(m[1]));
      if(parts.length)raw=parts.join('\n');
    }
  }
  if(/<w:/.test(raw)){
    raw=raw.replace(/<w:t(?:\s[^>]*)?>/g,'\n').replace(/<[^>]+>/g,'').split('\n').map(x=>decodeXml(x).trim()).filter(Boolean).join('\n');
  }
  return raw
    .replace(/\r/g,'')
    .replace(/\u00a0/g,' ')
    .replace(/[\u200b\ufeff]/g,'')
    .replace(/[Ａ-Ｇａ-ｇ]/g,ch=>String.fromCharCode(ch.charCodeAt(0)-0xFEE0))
    .replace(/[０-９]/g,ch=>String.fromCharCode(ch.charCodeAt(0)-0xFEE0));
}

function preSplitVolumeAndCompactQuestions(text){
  let s=String(text||'');
  // 让“第一卷/第二卷/第1套/试卷一”等标题独占一行，避免卷内重复 1-25 题被拼接。
  s=s.replace(/\s*((?:第\s*[一二三四五六七八九十百千万0-9]+\s*(?:卷|套|部分|单元)|试卷\s*[一二三四五六七八九十百千万0-9]+|卷\s*[一二三四五六七八九十百千万0-9]+))\s*/g,'\n$1\n');
  // 若多个题目在同一段里，用题号强制切题。
  // 增强：避免把选项值“A. 1.O m / B.1.2 m”里的“1.O”误认为新题号。
  s=s.replace(/([^\n])\s+((?:第\s*)?\d{1,4}\s*(?:题)?[、.．:：]\s*(?!\s*\d)(?!mm|cm|m|MPa|bar|kg|℃|%))/gi,(m,prev,next,offset,full)=>{
    const before=full.slice(Math.max(0,offset-8),offset+1);
    if(/[A-Ga-g0]\s*[、.．:：]\s*$/.test(before))return prev+' '+next;
    if(/^\d+\s*[.．]\s*[OoIl]\b/.test(next))return prev+' '+next;
    return prev+'\n'+next;
  });
  // 答案标记后面紧跟下一题题号时切开。
  s=s.replace(/([】\]\)）])\s+(?=(?:第\s*)?\d{1,3}\s*(?:题)?[、.．:：])/g,'$1\n');
  // 兼容“题干【答案：B.20mm】 A...;B...;C...”这种紧凑格式：先把答案标记前后留出边界。
  s=s.replace(/[ \t]*(【\s*(?:正确答案|参考答案|标准答案|答案)\s*[:：][^】]{1,80}】)[ \t]*/g,' $1 ');
  return s;
}
function isVolumeHeading(line){
  const s=String(line||'').replace(/\s/g,'');
  return /^(?:第[一二三四五六七八九十百千万0-9]+(?:卷|套|部分|单元)|试卷[一二三四五六七八九十百千万0-9]+|卷[一二三四五六七八九十百千万0-9]+)$/.test(s)
    || /^202\d年.*(?:试卷|考试).*?[·._-]?[一二三四五六七八九十百千万0-9]+卷$/.test(s);
}
function getVolumeLabel(line){
  const s=String(line||'').replace(/\s/g,'');
  const m=s.match(/([一二三四五六七八九十百千万0-9]+)卷$/);
  if(m)return '第'+m[1]+'卷';
  return s;
}
function hasInlineAnswerTag(line){
  const s=String(line||'');
  return /[【\[（(]\s*(?:正确答案|参考答案|标准答案|答案|参考要点|答题要点)\s*[:：]/i.test(s)
    || /(?:^|\s)(?:正确答案|参考答案|标准答案|答案|参考要点|答题要点)\s*[:：]\s*\S+/i.test(s);
}
function hasEmbeddedAnswerStem(line,group=''){
  const raw=String(line||'').trim();
  if(!raw||isOptionLine(raw)||isAnswerLine(raw)||isAnalysisLine(raw)||getHeadingType(raw)||isImportNoiseLine(raw))return false;
  if(hasInlineAnswerTag(raw))return true;
  const gt=mapType(group);
  const m=raw.match(/[（(]\s*([^()（）]{1,100})\s*[）)]/);
  if(!m)return false;
  const inner=m[1].trim();
  if(!inner)return false;
  const pureJudgeInner=/^(?:对|错|正确|错误|是|否|√|✓|✔|×|X|x|v|V|T|F|True|False)$/i.test(inner);
  if((gt==='judge'||!gt) && pureJudgeInner)return true;
  if(gt==='multiple' && /^[A-Ga-g]{2,7}$/.test(inner.replace(/[\s,，、;；/\\]+/g,'')))return true;
  if((gt==='single'||gt==='multiple'||gt==='') && raw.length>8 && !pureJudgeInner && !/^[A-Ga-g][、.．:：]/.test(raw))return true;
  return false;
}
function looksLikeNewQuestionLine(line,group=''){
  if(isOptionLine(line)||isAnswerLine(line)||isAnalysisLine(line)||getHeadingType(line)||isImportNoiseLine(line))return false;
  return hasStrongQuestionNo(line)||!!detectType(line)||hasEmbeddedAnswerStem(line,group)||isQuestionStart(line);
}
function extractInlineAnswerTag(line,type){
  let s=String(line||'');const answers=[];
  s=s.replace(/[【\[]\s*(?:正确答案|参考答案|标准答案|答案)\s*[:：]?\s*([^】\]]{1,100})\s*[】\]]/gi,(m,a)=>{answers.push(...splitAnswerByType(a,type));return ' '});
  s=s.replace(/[（(]\s*(?:正确答案|参考答案|标准答案|答案)\s*[:：]?\s*([^）)]{1,100})\s*[）)]/gi,(m,a)=>{answers.push(...splitAnswerByType(a,type));return ' '});
  return {text:s.replace(/\s+/g,' ').trim(),answer:answers};
}
function splitSemicolonOptionsFromLine(line,answer=[]){
  const raw=String(line||'').trim();
  if(!raw || !/[;；]/.test(raw))return null;
  let parts=raw.split(/[;；]/).map(x=>x.trim()).filter(Boolean);
  if(parts.length<2||parts.length>8)return null;
  const opts=[];
  for(let i=0;i<parts.length;i++){
    let part=parts[i];
    let m=part.match(/^([A-Ga-g])\s*[、.．:：]?\s*(.+)$/);
    if(m)opts.push({key:m[1].toUpperCase(),text:m[2].trim()});
    else if(i===0)opts.push({key:'A',text:part});
    else opts.push({key:String.fromCharCode(65+i),text:part});
  }
  // 如果答案写成 B.20mm，而选项中有 “20mm”，优先映射到 B。
  const ansText=(answer||[]).map(x=>String(x||'')).join(' ');
  return opts.some(o=>o.text)?opts:null;
}

function splitQuestionBlocks(text){
  let lines=text.split('\n').map(x=>x.trim()).filter(Boolean);
  const blocks=[];let cur=[];let group='';let curGroup='';let volume='';let curVolume='';
  const flush=()=>{if(cur.length){blocks.push({group:curGroup||group,volume:curVolume||volume,lines:[...cur]});cur=[];curGroup=group;curVolume=volume}};
  const curHasContent=()=>cur.length>0;
  const curHasRealQuestion=()=>cur.some(l=>!isOptionLine(l)&&!isAnswerLine(l)&&!isAnalysisLine(l)&&!getHeadingType(l));
  const curHasAnsweredStem=()=>cur.some(l=>hasInlineAnswerTag(l)||hasEmbeddedAnswerStem(l,curGroup||group));
  for(const raw of lines){
    if(isVolumeHeading(raw)){flush();volume=getVolumeLabel(raw);curVolume=volume;continue}
    if(isImportNoiseLine(raw))continue
    const headingType=getHeadingType(raw);
    if(headingType){flush();group=headingType;curGroup=group;continue}
    const line=raw;
    const newQuestion=looksLikeNewQuestionLine(line,group);
    const option=isOptionLine(line), answerLine=isAnswerLine(line), analysisLine=isAnalysisLine(line);
    if(curHasContent() && !option && !answerLine && !analysisLine){
      const hasPrevOptions=cur.some(isOptionLine)||cur.some(l=>extractInlineOptionsRich(l)?.options?.length>=2);
      const hasPrevAnswer=cur.some(isAnswerLine)||curHasAnsweredStem();
      const shouldStartNew=newQuestion && (hasPrevOptions||hasPrevAnswer||curHasRealQuestion()&&hasStrongQuestionNo(line)||hasInlineAnswerTag(line)&&curHasRealQuestion());
      if(shouldStartNew)flush();
    }
    if(!cur.length){curGroup=group;curVolume=volume}
    cur.push(line);
  }
  flush();
  if(blocks.length<2){
    return text.split(/\n\s*\n+/).map(x=>({group:'',volume:'',lines:x.split('\n').map(y=>y.trim()).filter(Boolean)})).filter(b=>b.lines.length);
  }
  return blocks;
}
function isImportNoiseLine(line){
  const s=String(line||'').trim();
  if(!s)return true;
  return /^(基本信息|题目|答案表|参考答案|答案解析)$/.test(s) || /^基本信息[:：]?/.test(s)
    || /^202\d年.*(?:考试|试卷).*(?:卷|套)?$/.test(s)
    || /^(?:.*(?:考试试卷|知识考试试卷|综合知识考试试卷).*|[（(]?考试时间[:：]?.*满分.*|满分\d+分.*考试时间.*)$/.test(s)
    || /^\[?矩阵文本题\]?/.test(s)
    || /^\*+$/.test(s);
}
function getHeadingType(line){
  const raw=String(line||'').trim();
  const s=raw.replace(/\s/g,'');
  if(!s)return'';
  // 只把“短标题/带章节序号/带题量分值说明”的行识别为题型分区。
  // 避免把题干里的“判断某段程序……”、选项里的“单选/多选”等误当成题型标题。
  const hasSectionPrefix=/^(?:第[一二三四五六七八九十0-9]+部分|[一二三四五六七八九十]+[、.．:：])/.test(s)||/^\d+[、.．:：](?:单选题|单项选择题|多选题|多项选择题|判断题|填空题|简答题)/.test(s);
  const hasCountInfo=/^(?:[一二三四五六七八九十]+[、.．:：])?(?:单选题|单项选择题|多选题|多项选择题|判断题|填空题|简答题).{0,40}(?:共\d+题|每题|满分|分，共|题，每题)/.test(s);
  const bracketOnly=/^[\[【(（]?(?:单选题|单选|单项选择题|單選題|單選|多选题|多选|多项选择题|多選題|多選|复选题|複選題|复选|複選|判断题|判断|判斷題|是非题|是非題|填空题|填空|填充题|简答题|簡答題|简答|問答題|问答题|名词解释|名詞解釋|论述题|論述題)[\]】)）]?$/.test(s);
  const likelyHeading=bracketOnly || hasSectionPrefix || hasCountInfo;
  if(!likelyHeading)return'';
  if(/[。？?！!；;]$/.test(raw)&&!hasSectionPrefix&&!hasCountInfo&&!bracketOnly)return'';
  if(/[A-G][:：]/.test(s))return'';
  if(/(?:逻辑判断|判断推理|图形推理|定义判断|类比推理|资料分析|材料分析|言语理解|语言理解|数学运算|数量关系|综合能力|常识判断)/.test(s))return raw;
  if(/(?:填空题|填空|填充题)/.test(s))return'填空题';
  if(/(?:简答题|簡答題|简答|問答題|问答题|名词解释|名詞解釋|论述题|論述題)/.test(s))return'简答题';
  if(/(?:单选题|单项选择题|单选|單選題|單選)/.test(s))return'单选题';
  if(/(?:多选题|多项选择题|多选|复选题|複選題|复选|複選|多選題|多選)/.test(s))return'多选题';
  if(/(?:判断题|判断|判斷題|是非题|是非題)/.test(s))return'判断题';
  return'';
}
function hasStrongQuestionNo(line){return /^\s*(?:第\s*\d+\s*题|\d+\s*[、.．:：]|[（(]\s*\d+\s*[）)]|[【\[]\s*\d+\s*[】\]])/.test(line)}
function isQuestionStart(line){
  if(isOptionLine(line)||isAnswerLine(line)||isAnalysisLine(line))return false;
  if(hasInlineAnswerTag(line))return true;
  return hasStrongQuestionNo(line)||!!detectType(line)||/[（(]\s*[）)]/.test(line)&&/[。？?]?\s*\*?$/.test(line)||/[。？?]\s*\*?$/.test(line)&&line.length>8||/\*\s*$/.test(line)&&line.length>8;
}
function isOptionLine(line){return /^\s*(?:[oOxXuUyYvV√✔✓]\s*)?(?:[（(]\s*[A-Ga-g1-9]\s*[）)]|[A-Ga-g]\s*(?:[、.．:：，,]|\s+|(?=[\u4e00-\u9fa5]))|0\s*[.．、:：]\s+(?=\S))(?![+＋])/.test(line)}
function isAnswerLine(line){return /^(?:【|\[)?\s*(?:正确答案|参考答案|标准答案|答案解析|答案|参考要点|答题要点|Answer|Correct\s*answer)\s*(?:】|\])?\s*[:：]?\s*\S+/i.test(line)}
function isAnalysisLine(line){return /^(?:【|\[)?\s*(?:解析|答案解析|试题解析|说明|考点)\s*(?:】|\])?\s*[:：]?/i.test(line)}
function detectType(text){
  const s=String(text||'').trim();
  const compact=s.replace(/\s/g,'');
  // 单题题型只识别显式标记或非常短的独立题型词；不能把“判断某段程序……”误识别为判断题。
  if(/[\[【(（]\s*(简答题|简答|问答题|问答|名词解释|论述题)\s*[\]】)）]/.test(s)||/^(?:简答题|简答|问答题|问答|名词解释|论述题)$/.test(compact))return'short';
  if(/[\[【(（]\s*(填空题|填空|填充题)\s*[\]】)）]/.test(s)||/^(?:填空题|填空|填充题)$/.test(compact))return'blank';
  if(/[\[【(（]\s*(多选题|多选|多项选择题)\s*[\]】)）]/.test(s)||/^(?:多选题|多选|多项选择题)$/.test(compact))return'multiple';
  if(/[\[【(（]\s*(判断题|判断)\s*[\]】)）]/.test(s)||/^(?:判断题|判断|是非题)$/.test(compact))return'judge';
  if(/[\[【(（]\s*(单选题|单选|单项选择题)\s*[\]】)）]/.test(s)||/^(?:单选题|单选|单项选择题)$/.test(compact))return'single';
  return'';
}

function splitTrailingFirstOptionFromQuestion(line,nextLine){
  const raw=String(line||'').trim();const next=String(nextLine||'').trim();
  if(!/^B\s*(?:[、.．:：，,]|\s+|(?=[\u4e00-\u9fa5]))/i.test(next))return null;
  if(isOptionLine(raw)||isAnswerLine(raw)||isAnalysisLine(raw)||getHeadingType(raw))return null;
  const patterns=[
    /^(.*?)(?:\s+|　+)(A)\s*[、.．:：]?\s*([^A-G\n]{1,120})$/i,
    /^(.*?[。？！?）)])\s*(A)\s*[、.．:：]?\s*([^A-G\n]{1,120})$/i
  ];
  for(const re of patterns){
    const m=raw.match(re);if(!m)continue;
    const q=(m[1]||'').trim();let txt=(m[3]||'').trim();
    if(q.length<8||!txt||/(?:答案|解析)\s*[:：]/.test(txt))continue;
    if(txt.length>80)continue;
    if(!/[（(]\s*[）)]|[。？！?]$/.test(q)&&q.length<16)continue;
    return {question:q,key:'A',text:txt};
  }
  return null;
}

function parseBlock(block,idx){
  const lines=(Array.isArray(block)?block:block.lines||String(block).split('\n')).map(x=>String(x).trim()).filter(Boolean);
  const group=block.group||'';let type=mapType(group)||'';let answer=[];let analysis='';let options=[];let qlines=[];let collectingAnalysis=false;let unkeyedMode=false;let seenQuestion=false;let number=idx+1;
  const full=lines.join('\n');const inlineType=detectType(full);if(inlineType)type=inlineType;
  for(let li=0;li<lines.length;li++){
    let line=lines[li].trim();
    const t=detectType(line);if(t)type=t;
    const inlineAnswerTag=extractInlineAnswerTag(line,type);
    if(inlineAnswerTag.answer.length){answer.push(...inlineAnswerTag.answer);line=inlineAnswerTag.text;}
    const lineAnswerExtract=extractTrailingAnswerFromText(line,type);
    if(lineAnswerExtract.answer.length && !isAnswerLine(line)){
      answer.push(...lineAnswerExtract.answer);
      line=lineAnswerExtract.text;
    }
    const bareJudgeExtract=extractBareJudgeAnswerFromLine(line,type,group);
    if(bareJudgeExtract.answer.length){
      type='judge';
      answer.push(...bareJudgeExtract.answer);
      line=bareJudgeExtract.text;
    }

    // Handle combined 【答案】...【解析】... format
    const pbCombined=line.match(/^\s*(\d{1,4})\s*[.、．]\s*【答案】\s*([A-Ga-g]{1,7})\s*【解析】\s*(.*)$/);
    if(pbCombined){
      number=pbCombined[1];answer=splitAnswerByType(pbCombined[2],type);
      analysis=(pbCombined[3]||'').trim();collectingAnalysis=true;seenQuestion=true;
      continue;
    }
    // Handle 答案解析 prefix
    if(/^答案解析\s*\d/.test(line)){
      const after=line.replace(/^答案解析\s*/,'');
      const pbCA=after.match(/^(\d{1,4})\s*[.、．]\s*(?:【答案】\s*)?([A-Ga-g]{1,7})\s*(?:【解析】\s*(.*))?$/);
      if(pbCA){number=pbCA[1];answer=splitAnswerByType(pbCA[2],type);if(pbCA[3]){analysis=pbCA[3].trim();collectingAnalysis=true;}seenQuestion=true;}
      continue;
    }
    // Handle 解析 continuation lines (e.g., 2．【解析】C。explanation)
    const pbAnalysis=line.match(/^\s*(\d{1,4})\s*[.、．]\s*【解析】\s*([A-Ga-g]{1,7})\s*[。.]?\s*(.*)$/);
    if(pbAnalysis){
      number=pbAnalysis[1];answer=splitAnswerByType(pbAnalysis[2],type);
      analysis=(pbAnalysis[3]||'').trim();collectingAnalysis=true;seenQuestion=true;
      continue;
    }
    // 支持”题号 答案 题目”格式，如：15. B 以下何者…… / 1. AD 下列何者……
    const pre=line.match(/^\s*(?:第\s*)?(\d+)\s*(?:题)?[\.、．:：]?\s+([A-Ga-g]{1,7}|[对错正确错误√×XxTtFf])(?:\s+(.+))?$/);
    if(pre && (!seenQuestion || !options.length) && !isOptionLine(line)){
      const maybeCode=/^[A-Ga-g]{2,7}$/.test(pre[2]||'') && /^\s*\d/.test(pre[3]||'');
      if(!maybeCode && !/【解析】/.test(pre[3]||'')){
        number=pre[1]; answer.push(...splitAnswerByType(pre[2],type));
        if(pre[3]){qlines.push(pre[3].trim());seenQuestion=true;}
        continue;
      }
    }
    // 支持答案在题号下一行分裂显示：2. / ABC / DE / 题干
    const onlyNo=line.match(/^\s*(?:第\s*)?(\d+)\s*(?:题)?[\.、．:：]?\s*$/);
    if(onlyNo && !seenQuestion && !options.length){number=onlyNo[1];continue;}
    if(!seenQuestion && !options.length && /^[A-Ga-g]{1,7}$/.test(line)){
      answer.push(...splitAnswer(line));
      continue;
    }

    const am=line.match(/^(?:【|\[)?\s*(?:正确答案|参考答案|标准答案|答案解析|答案|参考要点|答题要点|Answer|Correct\s*answer)\s*(?:】|\])?\s*[:：]?\s*(.+)$/i);
    if(am){
      const amText=am[1].trim();
      const amCombined=amText.match(/^(\d{1,4})\s*[.、．]\s*(?:【答案】\s*)?([A-Ga-g]{1,7})\s*【解析】\s*(.*)$/);
      if(amCombined){number=amCombined[1];answer=splitAnswerByType(amCombined[2],type);analysis=(amCombined[3]||'').trim();collectingAnalysis=true;}
      else{answer=splitAnswerByType(amText,type);collectingAnalysis=false;}
      continue;
    }
    const xm=line.match(/^(?:【|\[)?\s*(?:解析|答案解析|试题解析|说明|考点)\s*(?:】|\])?\s*[:：]?\s*(.*)$/i);
    if(xm){analysis=xm[1]||'';collectingAnalysis=true;continue}

    // 兼容题干被 Word 断成两行：上一行“……使用”，下一行“(D）来选取文本。”，再下一行才是 A/B/C/D 选项。
    const leadingAnswerContinuation=line.match(/^\s*[（(]\s*([A-Ga-g]{1,7}|[1-9]{1,7}|对|错|正确|错误|√|✓|✔|×|X|x|T|F)\s*[）)〕]\s*(.+)$/);
    if(leadingAnswerContinuation && qlines.length && !options.length && /^\s*A\s*[、.．:：，,\s]/.test(lines[li+1]||'')){
      answer.push(...splitAnswerByType(leadingAnswerContinuation[1],type));
      qlines.push(leadingAnswerContinuation[2].trim());seenQuestion=true;
      continue;
    }

    const firstOpt=splitTrailingFirstOptionFromQuestion(line,lines[li+1]||'');
    if(firstOpt && !options.length){
      qlines.push(firstOpt.question);seenQuestion=true;collectingAnalysis=false;unkeyedMode=false;
      options.push({key:firstOpt.key,text:firstOpt.text});
      continue;
    }

    const richInline=extractInlineOptionsRich(line);
    if(richInline && richInline.options.length>=2){
      collectingAnalysis=false;unkeyedMode=false;
      if(richInline.prefix && !options.length){qlines.push(richInline.prefix);seenQuestion=true;}
      for(const it of richInline.options){
        let key=normalizeOptionKey(it.key);let txt=(it.text||'').trim();
        if(it.correct||hasCorrectMark(txt)){answer.push(key);txt=removeCorrectMark(txt)}
        if(it.extraAnswer&&it.extraAnswer.length)answer.push(...it.extraAnswer);
        if(txt)options.push({key,text:txt});
      }
      continue;
    }

    const semiOptions=splitSemicolonOptionsFromLine(line,answer);
    if(semiOptions && semiOptions.length>=2 && (seenQuestion||qlines.length)){
      collectingAnalysis=false;unkeyedMode=false;
      semiOptions.forEach(o=>{if(o.text)options.push(o)});
      continue;
    }

    const inlineOpts=splitInlineOptions(line);
    if(inlineOpts.length>=2 || (inlineOpts.length===1 && (options.length||seenQuestion))){
      collectingAnalysis=false;unkeyedMode=false;
      for(const it of inlineOpts){
        let key=normalizeOptionKey(it.key);let txt=(it.text||'').trim();
        if(it.correct||hasCorrectMark(txt)){answer.push(key);txt=removeCorrectMark(txt)}
        if(txt)options.push({key,text:txt});
      }
      continue;
    }

    const om=line.match(/^\s*([oOxXuUyYvV√✔✓])?\s*(?:[（(]\s*([A-Ga-g1-90])\s*[）)]|([A-Ga-g0])\s*(?:[、.．:：，,]|\s+|(?=[\u4e00-\u9fa5])))\s*(.*)$/);
    if(om){
      collectingAnalysis=false;unkeyedMode=false;
      let key=normalizeOptionKey(om[2]||om[3]);let txt=(om[4]||'').trim();
      if(om[1]||hasCorrectMark(txt)){answer.push(key);txt=removeCorrectMark(txt)}
      const inlineAns=txt.match(/(?:^|[（(【\[])(?:答案|正确答案)\s*[:：]?\s*([A-Ga-g1-9对错正确错误√×XxTtFf,，、;；/\s]+)[）)】\]]?$/);
      if(inlineAns){answer.push(...splitAnswer(inlineAns[1]));txt=txt.replace(inlineAns[0],'').trim()}
      if(txt)options.push({key,text:txt});
      continue;
    }
    if(collectingAnalysis){analysis+=(analysis?'\n':'')+line;continue}
    const questionSeed=qlines.join(' ');
    const nextLine=lines[li+1]||'';
    const nextLooksLikeLaterOption=/^\s*[B-Gb-g]\s*[、.．:：]/.test(nextLine);
    const shouldBeUnkeyedOption=qlines.length && !looksLikeNewQuestionLine(line,group) && !isAnswerLine(line) && !isAnalysisLine(line) && (unkeyedMode || hasCorrectMark(line) || detectType(full) || /[（(]\s*[）)]/.test(questionSeed) || nextLooksLikeLaterOption || /^(对|错|正确|错误)$/.test(line));
    if(shouldBeUnkeyedOption){
      let key=String.fromCharCode(65+options.length);let txt=line.trim();unkeyedMode=true;
      if(hasCorrectMark(txt)){answer.push(key);txt=removeCorrectMark(txt)}
      if(txt)options.push({key,text:txt});
      continue;
    }
    if(options.length && !unkeyedMode && !looksLikeNewQuestionLine(line,group) && !isAnswerLine(line) && !isAnalysisLine(line)){
      options[options.length-1].text=(options[options.length-1].text+' '+line).trim();
    }else if(!getHeadingType(line)){
      qlines.push(line);seenQuestion=true;
    }
  }
  const qNo=(qlines[0]||'').match(/^\s*(?:[【\[]\s*(\d+)\s*[】\]]|(?:第\s*)?(\d+)\s*(?:题)?[\.、．:：])/);
  if(qNo)number=qNo[1]||qNo[2];
  let question=qlines.join(' ')
    .replace(/^\s*(?:[【\[]\s*\d+\s*[】\]]|第\s*\d+\s*题|\d+\s*[\.、．:：])\s*/,'')
    .replace(/^\s*[（(]\s*\d+\s*[）)]\s*/,'')
    .replace(/[\[【(（]\s*(单选题|单选|单项选择题|單選題|單選|多选题|多选|多项选择题|多選題|多選|复选题|複選題|判断题|判断|判斷題|是非题|是非題|填空题|填空|填充题|简答题|简答|問答題|问答题|名词解释|名詞解釋|论述题|論述題)\s*[\]】)）]/g,'')
    .replace(/\s*\*+\s*$/,'')
    .trim();
  options=mergeDuplicateOptions(repairEmbeddedOptions(options)).filter(o=>o.text&&!/^\s*$/.test(o.text));
  if(!type)type=guessType(question,options,answer,group);
  const fixedQuestion=cleanQuestionStemAndAnswer(question,answer,type,options);
  question=fixedQuestion.question;
  answer=fixedQuestion.answer;
  if(type==='judge'&&!options.length)options=[{key:'A',text:'正确'},{key:'B',text:'错误'}];
  answer=isTextType(type)?splitTextAnswer(answer.join('；')):normalizeAnswer(answer,options,type);
  return {id:'imp_'+Date.now()+'_'+idx,type,number,question,options,answer,analysis:analysis.trim(),group};
}

function normalizeOptionKey(k){
  k=String(k||'').trim();
  if(k==='0')return 'D';
  if(/^[a-g]$/.test(k))return k.toUpperCase();
  if(/^[A-G]$/.test(k))return k;
  return k;
}
function splitInlineOptions(line){
  const s=String(line||'');
  const re=/([oOxXuUyYvV√✔✓])?\s*[（(]\s*([A-Ga-g1-9])\s*[）)]/g;
  const hits=[];let m;
  while((m=re.exec(s)))hits.push({idx:m.index,len:m[0].length,correct:!!m[1],key:m[2]});
  if(hits.length<1)return[];
  if(hits.length===1 && hits[0].idx>3)return[];
  return hits.map((h,i)=>{
    const start=h.idx+h.len;const end=i+1<hits.length?hits[i+1].idx:s.length;
    return {key:h.key,correct:h.correct,text:s.slice(start,end).trim().replace(/[;；，,]+$/,'').trim()};
  }).filter(o=>o.text||o.correct);
}
function repairEmbeddedOptions(options){
  const out=[];
  const keyCode=k=>String(k||'A').toUpperCase().charCodeAt(0);
  for(const opt of (options||[])){
    const txt=String(opt.text||'');
    const hits=[];
    const base=keyCode(opt.key);
    for(let i=1;i<txt.length;i++){
      const ch=txt[i];
      if(!/^[A-Ga-g]$/.test(ch))continue;
      const key=normalizeOptionKey(ch);
      if(keyCode(key)<=base)continue;
      const prev=txt[i-1]||'';
      const next=txt[i+1]||'';
      const nextOk=/[、.．:：，,；;\s]/.test(next)||/[\u4e00-\u9fa5]/.test(next);
      if(!nextOk)continue;
      // 避免拆 HSE会议、API标准 这类英文缩写；但允许 0.2MPaB.0.3 这种单位后粘连选项。
      if(/[A-Za-z]/.test(prev) && /[\u4e00-\u9fa5]/.test(next))continue;
      let len=1;
      while(i+len<txt.length && /[、.．:：，,；;\s]/.test(txt[i+len]))len++;
      hits.push({idx:i,len,key});
    }
    if(!hits.length){out.push(opt);continue;}
    out.push({...opt,text:txt.slice(0,hits[0].idx).trim()});
    for(let i=0;i<hits.length;i++){
      const start=hits[i].idx+hits[i].len;
      const end=i+1<hits.length?hits[i+1].idx:txt.length;
      const part=txt.slice(start,end).trim();
      if(part)out.push({key:hits[i].key,text:part});
    }
  }
  return out.filter(o=>o.text);
}
function mergeDuplicateOptions(options){
  const map=new Map();
  for(const o of options){if(!map.has(o.key))map.set(o.key,{key:o.key,text:o.text});else map.get(o.key).text=(map.get(o.key).text+' '+o.text).trim()}
  return [...map.values()].sort((a,b)=>a.key.localeCompare(b.key));
}
function mapType(s){
  s=String(s||'');
  // “逻辑判断/判断推理”是行测题型分区，仍然通常是单选题，不能按 true/false 判断题处理。
  if(/逻辑判断|判断推理|图形推理|定义判断|类比推理|资料分析|材料分析|言语理解|语言理解|数学运算|数量关系/i.test(s))return'single';
  if(/简答|簡答|问答|問答|主观|主觀|名词解释|名詞解釋|论述|論述|short|essay/i.test(s))return'short';
  if(/填空|填充|blank|fill/i.test(s))return'blank';
  if(/多选|多選|多项|多項|复选|複選|multiple|multi/i.test(s))return'multiple';
  if(/判断题|判斷題|判断正误|判斷正誤|正误判断|是非题|judge|truefalse/i.test(s))return'judge';
  if(/单选|單選|单项|單項|single/i.test(s))return'single';
  return'';
}
function guessType(question,options,answer,group=''){
  const gt=mapType(group);if(gt)return gt;
  const optionCount=(options||[]).length;
  const hasChoiceOptions=optionCount>=3 || (options||[]).some(o=>/^[C-G]$/.test(String(o.key||'').toUpperCase()));
  const ans=(answer||[]).map(a=>String(a||'').trim()).filter(Boolean);
  // 只要已经识别到选项，优先作为客观选择题处理。
  // “填入横线/补全语句”在行测、公考题库里通常仍是单选题，不能因为题干有空格就改成填空题。
  if(optionCount){
    if(!hasChoiceOptions && isJudgeBlock(options,ans))return'judge';
    if(ans.length>1&&ans.every(a=>/^[A-G1-9]$/.test(String(a))))return'multiple';
    if(/多选|多项选择/.test(question))return'multiple';
    return'single';
  }
  if(/简答|问答|名词解释|论述/.test(question))return'short';
  if(/填空|填入|补全|_{2,}|____|（\s*）|\(\s*\)/.test(question)){
    if(ans.length && !shouldGuessBlankFromNoOption(question,ans))return'short';
    return'blank';
  }
  if(!optionCount && ans.length){
    if(ans.some(a=>isJudgeSymbolAnswer(a)))return'judge';
    if(ans.every(a=>/^[A-Ga-g]$/.test(a)))return ans.length>1?'multiple':'single';
    if(ans.every(a=>/^[1-9]$/.test(a)))return'blank';
    return shouldGuessBlankFromNoOption(question,ans)?'blank':'short';
  }
  return'single';
}
function hasCorrectMark(s){return /(?:正确答案|答案正确|参考答案|标准答案|√|✔|✓)/.test(String(s||''))}
function removeCorrectMark(s){return String(s||'').replace(/[（(【\[]\s*(?:正确答案|答案正确|参考答案|标准答案)\s*[）)】\]]/g,'').replace(/(?:正确答案|答案正确|参考答案|标准答案)/g,'').replace(/[√✔✓]/g,'').replace(/\s+/g,' ').trim()}
function splitAnswer(s){
  s=String(s??'').trim();
  s=s.replace(/^(?:\u3010|\[)?\s*(?:\u6b63\u786e\u7b54\u6848|\u53c2\u8003\u7b54\u6848|\u6807\u51c6\u7b54\u6848|\u7b54\u6848|\u53c2\u8003\u8981\u70b9|\u7b54\u9898\u8981\u70b9|Answer|Correct\s*answer)\s*(?:\u3011|\])?\s*(?:\:|\uFF1A)?\s*/i,'').trim();
  s=s.replace(/[?.!??;?,?\s]+$/,'').trim();
  if(!s)return[];
  const numeric=s.match(/^[\uFF08(]?\s*([1-9])\s*[\uFF09)]?$/);if(numeric)return[numeric[1]];
  const numericMulti=s.match(/^(?:[\uFF08(]?\s*[1-9]\s*[\uFF09)]?\s*[,??;?/\\\s]*){2,9}$/);if(numericMulti)return s.match(/[1-9]/g)||[];
  const letterCompact=s.replace(/[\s,??;?/\\??()]+/g,'').toUpperCase();
  if(/^[A-G]{2,7}$/.test(letterCompact))return letterCompact.split('');
  if(/^[A-Ga-g](?:\s*[,??;?/\\\s]+\s*[A-Ga-g])+(?:\s*)$/.test(s))return s.toUpperCase().split(/[,??;?/\\\s]+/).filter(Boolean);
  if(/^[A-Ga-g]{2,7}$/.test(s))return s.toUpperCase().split('');
  const leadOpt=s.match(/^([A-Ga-g])\s*[、.．:：]\s*.+$/);if(leadOpt)return [leadOpt[1].toUpperCase()];
  if(/^[A-Ga-g]$/.test(s))return [s.toUpperCase()];
  if(/^(?:\u5bf9|\u6b63\u786e|\u662f|\u221A|\u2713|\u2714|v|V|T|True)$/i.test(s))return ['正确'];
  if(/^(?:\u9519|\u9519\u8bef|\u5426|\u00D7|X|F|False)$/i.test(s))return ['错误'];
  if(looksLikeTextualAnswer(s))return [];
  const parts=s.split(/[,??;?/\\]+/).map(x=>x.trim()).filter(Boolean);
  if(parts.length>1){
    const parsed=parts.flatMap(x=>x===s?[]:splitAnswer(x));
    return parsed.length&&parsed.length===parts.length?parsed:[];
  }
  return [];
}
function normalizeAnswer(answer,options,type){
  const out=[];
  for(const raw of (answer||[])){
    const token=String(raw??'').trim();if(!token)continue;
    const key=mapAnswerToken(token,options,type);
    if(Array.isArray(key))out.push(...key);else if(key)out.push(key);
  }
  return [...new Set(out.map(x=>String(x).trim().toUpperCase()).filter(Boolean))];
}
function mapAnswerToken(token,options,type){
  let t=String(token).trim();
  if(type==='judge')return judgeToKey(t,options);
  if(/^[A-Ga-g]$/.test(t))return t.toUpperCase();
  if(/^[A-Ga-g]{2,7}$/.test(t))return t.toUpperCase().split('');
  if(/^[1-9]$/.test(t))return t;
  if(/^[1-9]{2,9}$/.test(t))return t.split('');
  // 非判断题不把 √/×/v/x 当成 A/B，避免题型误判时静默错配。
  if(isJudgeSymbolAnswer(t))return '';
  const hit=options.find(o=>normalizeText(o.text)===normalizeText(t)||normalizeText(o.text).includes(normalizeText(t))&&normalizeText(t).length>=2);
  return hit?hit.key:t;
}
function isJudgeAnswer(a){return /^(?:A|B|对|错|正确|错误|是|否|√|✓|✔|×|X|x|v|V|T|F|True|False)$/i.test(String(a??'').trim())}
function isJudgeSymbolAnswer(a){return /^(?:对|错|正确|错误|是|否|√|✓|✔|×|X|x|v|V|T|F|True|False)$/i.test(String(a??'').trim())}
function isRawJudgeSymbolAnswer(a){return isJudgeSymbolAnswer(String(a??'').replace(/^(?:答案|正确答案|参考答案|标准答案)\s*[:：]?\s*/,'').trim())}
function isJudgeCorrectToken(t){return /^(?:对|正确|是|√|✓|✔|v|V|T|True)$/i.test(String(t??'').trim())}
function isJudgeWrongToken(t){return /^(?:\u9519|\u9519\u8bef|\u5426|\u00D7|X|x|F|False)$/i.test(String(t??'').trim())}
function isExactJudgeOptionText(s){return /^(?:\u5bf9|\u9519|\u6b63\u786e|\u9519\u8bef|\u662f|\u5426|\u221A|\u2713|\u2714|\u00D7|x|true|false)$/i.test(String(s||'').trim())}
function isJudgeOptionPair(options){
  const opts=(options||[]).filter(o=>o&&String(o.text||'').trim());
  if(opts.length!==2)return false;
  const a=String(opts[0].text||'').trim();
  const b=String(opts[1].text||'').trim();
  const aRight=isJudgeCorrectToken(a), aWrong=isJudgeWrongToken(a);
  const bRight=isJudgeCorrectToken(b), bWrong=isJudgeWrongToken(b);
  return isExactJudgeOptionText(a)&&isExactJudgeOptionText(b)&&((aRight&&bWrong)||(aWrong&&bRight));
}
function isJudgeBlock(options,answer){
  const opts=options||[];
  const explicitJudge=(answer||[]).some(a=>isJudgeSymbolAnswer(a));
  if(!opts.length)return explicitJudge;
  if(opts.length>=3 || opts.some(o=>/^[C-G]$/.test(String(o.key||'').toUpperCase())))return false;
  return explicitJudge && isJudgeOptionPair(opts);
}
function normalizeJudgeOptions(options){
  options=(options||[]).filter(o=>o&&o.text).map(o=>({key:normalizeOptionKey(o.key),text:String(o.text||'').trim()}));
  if(!options.length)return [{key:'A',text:'正确'},{key:'B',text:'错误'}];
  if(options.length===1){
    const t=normalizeText(options[0].text);
    if(/^(对|正确|是|√|v|true)$/.test(t))return [options[0],{key:'B',text:'错误'}];
    if(/^(错|错误|否|×|x|false)$/.test(t))return [{key:'A',text:'正确'},options[0]];
  }
  return options;
}
function judgeOptionMap(options){
  options=normalizeJudgeOptions(options);
  const exactCorrect=options.find(o=>/^(对|正确|是|√|✓|✔|true)$/i.test(String(o.text||'').trim()));
  const exactWrong=options.find(o=>/^(错|错误|否|×|x|false)$/i.test(String(o.text||'').trim()));
  const looseCorrect=options.find(o=>/(^|[^不非])(?:对|正确|是|√|✓|✔|true)/i.test(String(o.text||'')));
  const looseWrong=options.find(o=>/(错|错误|否|×|x|false)/i.test(String(o.text||'')));
  return {
    correctKey:(exactCorrect||looseCorrect||options[0]||{key:'A'}).key,
    wrongKey:(exactWrong||looseWrong||options[1]||{key:'B'}).key,
    confidence:!!((exactCorrect||looseCorrect)&&(exactWrong||looseWrong))
  };
}
function judgeToKey(a,options){
  const t=String(a??'').trim();
  const map=judgeOptionMap(options);
  if(/^[A-Ba-b]$/.test(t))return t.toUpperCase();
  // 判断题只接受 A/B 或明确的对错符号；C/D/E 等选择题答案不再静默接受。
  if(/^[C-Gc-g]$/.test(t))return '';
  if(isJudgeCorrectToken(t))return map.correctKey;
  if(isJudgeWrongToken(t))return map.wrongKey;
  return '';
}

function collectImportWarnings(arr){
  const warnings=[];
  (arr||[]).forEach((q,i)=>{const status=validateQuestion(q);if(status!=='正常')warnings.push(`第${i+1}题：${status}`)});
  return warnings;
}
function cleanImportedQuestion(q){
  const fixed=cleanQuestionStemAndAnswer(q.question,q.answer,q.type,q.options||[]);
  return {...q,question:fixed.question,answer:isTextType(q.type)?splitTextAnswer(fixed.answer.join('；')):normalizeAnswer(fixed.answer,q.options||[],q.type),normalized:normalizeText(fixed.question)};
}

function extractBareJudgeAnswerFromLine(line,type='',group=''){
  let s=String(line||'').trim();
  if(!s || isAnswerLine(s) || isOptionLine(s))return {text:s,answer:[]};
  const contextIsJudge=mapType(group)==='judge'||type==='judge';
  const m=s.match(/^(.*?)(?:\s+|[。\uFF1B;，,\u3001])(?:\u7b54\u6848\s*(?:\:|\uFF1A)?\s*)?(\u6b63\u786e|\u9519\u8bef|\u5bf9|\u9519|\u662f|\u5426|\u221A|\u2713|\u2714|\u00D7|X|x|T|F|True|False)\s*$/i);
  if(!m)return {text:s,answer:[]};
  const stem=(m[1]||'').trim();
  const token=(m[2]||'').trim();
  const hasQuestionNo=/^\s*(?:第\s*)?\d{1,4}\s*(?:题)?[、.．:：]/.test(stem);
  const tokenIsSymbol=/^(?:\u221A|\u2713|\u2714|\u00D7|X|x|T|F)$/i.test(token);
  if(!contextIsJudge && !(hasQuestionNo && tokenIsSymbol))return {text:s,answer:[]};
  if(stem.length<6)return {text:s,answer:[]};
  return {text:stem.replace(/[??;?,?]\s*$/,'').trim(),answer:splitAnswer(token)};
}
function extractTrailingAnswerFromText(text,type){
  let s=String(text||'').trim();let found=[];
  const bareJudge=s.match(/^(.*?)[。.!！?？]?\s*[\uFF08(]\s*(\u5bf9|\u9519|\u6b63\u786e|\u9519\u8bef|\u662f|\u5426|\u221A|\u2713|\u2714|\u00D7|X|x|v|V|T|F|True|False)\s*[\uFF09)]\s*$/i);
  if(bareJudge){
    const stem=(bareJudge[1]||'').trim();
    const hasQuestionNo=/^\s*(?:第\s*)?\d{1,4}\s*(?:题)?[、.．:：]/.test(stem);
    const hasInlineChoice=/(?:^|[\s;，、])A\s*[、.．:：，,]|(?:^|[\s;，、])B\s*[、.．:：，,]|(?:^|[\s;，、])C\s*[、.．:：，,]|(?:^|[\s;，、])D\s*[、.．:：，,]/i.test(stem);
    if((type==='judge'||(hasQuestionNo&&stem.length>=8&&!hasInlineChoice))){
      return {text:stem.replace(/[?.!???]\s*$/,'').trim(),answer:splitAnswer(bareJudge[2])};
    }
  }
  const pats=[
    /(?:[\uFF08(\u3010\[]\s*(?:\u6b63\u786e\u7b54\u6848|\u53c2\u8003\u7b54\u6848|\u6807\u51c6\u7b54\u6848|\u7b54\u6848)\s*(?:\:|\uFF1A)?\s*([^\uFF09)\u3011\]]+)\s*[\uFF09)\u3011\]]\s*)$/i,
    /(?:\s|^)(?:\u6b63\u786e\u7b54\u6848|\u53c2\u8003\u7b54\u6848|\u6807\u51c6\u7b54\u6848|\u7b54\u6848|\u53c2\u8003\u8981\u70b9|\u7b54\u9898\u8981\u70b9)\s*(?:\:|\uFF1A)?\s*([^\n]+?)\s*$/i
  ];
  for(const re of pats){
    const m=s.match(re);
    if(m){
      const candidate=m[1].trim();
      const a=splitAnswerByType(candidate,type);
      if(a.length){found=a;s=s.slice(0,m.index).trim();break}
    }
  }
  return {text:s,answer:found};
}
function cleanQuestionStemAndAnswer(question,answer=[],type='',options=[]){
  let ans=[...(answer||[])].filter(Boolean);
  let q=String(question||'').trim();
  const ex=extractTrailingAnswerFromText(q,type);
  if(ex.answer.length){ans=ans.concat(ex.answer);q=ex.text}
  const optionTexts=(options||[]).map(o=>String(o.text||'')).filter(Boolean);
  const answerKeys=ans.map(a=>String(a||'').trim().toUpperCase()).filter(a=>/^[A-G]$/.test(a));
  const correctOptionTexts=(options||[]).filter(o=>answerKeys.includes(String(o.key||'').toUpperCase())).map(o=>String(o.text||''));
  q=q.replace(/[\uFF08(]\s*([^()\uFF08\uFF09]{1,120})\s*[\uFF09)\u3015]/g,(m,inner)=>{
    const raw=String(inner||'').trim();
    if(!raw)return '（ ）';
    const compact=raw.replace(/[\s,??;?/\\]+/g,'').toUpperCase();
    const looksLikeChoiceAnswer=/^[A-G]{1,7}$/.test(compact)||/^[1-9]{1,9}$/.test(compact);
    const looksLikeJudgeAnswer=/^(?:\u5bf9|\u9519|\u6b63\u786e|\u9519\u8bef|\u662f|\u5426|\u221A|\u00D7|X|x|v|V|T|F|TRUE|FALSE)$/i.test(raw);
    if(looksLikeChoiceAnswer || (looksLikeJudgeAnswer && (type==='judge'||!(options||[]).length))){
      const hasKeyAnswer=ans.some(a=>/^[A-Ga-g]$/.test(String(a||'').trim()));
      const shouldAdd=!hasKeyAnswer || /^[A-G]{1,7}$/.test(compact) || (looksLikeJudgeAnswer && type==='judge');
      if(shouldAdd)ans=ans.concat(splitAnswer(raw));
      return '（ ）';
    }
    const n=normalizeTextAnswerForCompare(raw);
    const normAns=ans.map(a=>normalizeTextAnswerForCompare(a)).filter(Boolean);
    if(normAns.includes(n))return '（ ）';
    const matchesOption=optionTexts.some(t=>{
      const nt=normalizeTextAnswerForCompare(t);
      return nt===n || (n.length>=2 && nt.includes(n)) || (nt.length>=2 && n.includes(nt));
    });
    const matchesCorrectOption=correctOptionTexts.some(t=>{
      const nt=normalizeTextAnswerForCompare(t);
      return nt===n || (n.length>=2 && nt.includes(n)) || (nt.length>=2 && n.includes(nt));
    });
    if((matchesCorrectOption||matchesOption) && !/预案|方案|队站|站|县级以上|市级以上/.test(raw))return '（ ）';
    return m;
  });
  q=q.replace(/[\uFF08(]\s*([A-Ga-g][A-Ga-g\s,，、;；/\\]{0,12}|[1-9][1-9\s,，、;；/\\]{0,12}|对|错|正确|错误|是|否|\u221A|\u00D7|X|v|V|T|F|True|False)\s*$/g,(m,inner)=>{
    ans=ans.concat(splitAnswer(inner));
    return '（ ）';
  });
  return {question:q.replace(/\s+/g,' ').trim(),answer:[...new Set(ans)]};
}
function selectOrderedOptionHits(hits){
  hits=[...hits].map(h=>String(h.key)==='0'?{...h,key:'D'}:h).sort((a,b)=>a.idx-b.idx);
  const de=[];
  for(const h of hits){if(!de.some(x=>Math.abs(x.idx-h.idx)<2&&String(x.key).toUpperCase()===String(h.key).toUpperCase()))de.push(h)}
  const letters=de.filter(h=>/^[A-Ga-g]$/.test(String(h.key||'')));
  if(letters.length>=2){
    const start=letters.find(h=>String(h.key).toUpperCase()==='A')||letters[0];
    const seq=[];let need=String(start.key).toUpperCase().charCodeAt(0);let pos=start.idx-1;
    for(let code=need;code<=71;code++){
      const hit=letters.find(h=>h.idx>pos&&String(h.key).toUpperCase().charCodeAt(0)===code);
      if(!hit)break;
      seq.push(hit);pos=hit.idx;
    }
    if(seq.length>=2)return seq;
  }
  const nums=de.filter(h=>/^[1-9]$/.test(String(h.key||'')));
  if(nums.length>=2){
    const start=nums.find(h=>String(h.key)==='1')||nums[0];
    const seq=[];let need=Number(start.key);let pos=start.idx-1;
    for(let n=need;n<=9;n++){
      const hit=nums.find(h=>h.idx>pos&&String(h.key)===String(n));
      if(!hit)break;
      seq.push(hit);pos=hit.idx;
    }
    if(seq.length>=2)return seq;
  }
  return de;
}
function extractInlineOptionsRich(line){
  const s=String(line||'').trim();
  if(!s)return null;
  // 兼容答案选项本身也是字母的紧凑行：A.D B.C C.B D.A
  if(!/[\u4e00-\u9fa5]/.test(s)){
    const vals=[];let vm;const vre=/([A-Ga-g])\s*[.．、:：]\s*([A-Ga-g])(?=\s|$|[A-Ga-g]\s*[.．、:：])/g;
    while((vm=vre.exec(s)))vals.push({key:normalizeOptionKey(vm[1]),text:vm[2].toUpperCase()});
    if(vals.length>=2){
      const order=vals.map(v=>v.key).join('');
      if(/^ABCD?|ABCDEF?G?$/.test(order))return {prefix:'',options:vals};
    }
  }
  // 兼容强粘连选项：A.0.2MPaB.0.3MPaC.0.5MPaD.0.8MPa
  {
    const gh=[];let gm;const gre=/([A-Da-d0])\s*[、.．:：]\s*/g;
    while((gm=gre.exec(s))){
      const idx=gm.index, key=gm[1];
      const prev=idx>0?s[idx-1]:'';
      const after=s.slice(gre.lastIndex).trimStart()[0]||'';
      if(key==='0' && (/^[\d]/.test(after)||/[\d.．]/.test(prev)))continue;
      if(/[（(]/.test(prev) && /[）)〕]/.test(after))continue;
      gh.push({idx,len:gm[0].length,key,correct:false});
    }
    const seq=selectOrderedOptionHits(gh);
    if(seq.length>=3 && String(seq[0].key).toUpperCase()==='A'){
      const prefix=s.slice(0,seq[0].idx).trim().replace(/[;；，,、]+$/,'').trim();
      const options=seq.map((h,i)=>{
        const start=h.idx+h.len;const end=i+1<seq.length?seq[i+1].idx:s.length;
        let txt=s.slice(start,end).trim().replace(/^[;；，,、]+/,'').replace(/[;；，,、]+$/,'').trim();
        const ex=extractTrailingAnswerFromText(txt,'');
        return {key:normalizeOptionKey(h.key),correct:false,text:ex.text,extraAnswer:ex.answer};
      }).filter(o=>o.text||o.extraAnswer.length);
      if(options.length>=2)return {prefix,options};
    }
  }
  const hits=[];
  const re=/([A-Ga-g0])\s*(?:[、.．:：，,]|\s+)\s*/g;
  let m;
  while((m=re.exec(s))){
    const idx=m.index;
    const prev=idx>0?s[idx-1]:'';
    const next=s[re.lastIndex]||'';
    const after=s.slice(re.lastIndex).trimStart()[0]||'';
    // 避免把 API、100Bc、A级油井水泥、题干括号里的答案“（D ）”误作选项。
    if(prev && /[A-Za-z0-9]/.test(prev))continue;
    if((/[（(]/.test(prev)||/[（(]\s*$/.test(s.slice(Math.max(0,idx-4),idx))) && /[）)〕]/.test(after))continue;
    if(m[1]==='0' && (/^[\d]/.test(after)||/[\d.．]/.test(prev)))continue;
    if(!next)continue;
    hits.push({idx,len:m[0].length,key:m[1],correct:false});
  }
  // 兼容 A从左向右、B粉煤灰、C控制系统、D套管 这种没有标点的选项。
  const noSepRe=/([A-Da-d])(?=[\u4e00-\u9fa5])/g;
  while((m=noSepRe.exec(s))){
    const idx=m.index;
    const prev=idx>0?s[idx-1]:'';
    const next=s[idx+1]||'';
    // 避免 HSE计划、API标准、A级/G级、WB型、C100-30 等被当作无分隔选项。
    if(prev && /[A-Za-z0-9]/.test(prev))continue;
    if(/[级型类款层]/.test(next))continue;
    hits.push({idx,len:1,key:m[1],correct:false});
  }
  // 兼容 (A) / （A） / √(A)
  const parRe=/([oOxXuUyYvV√✔✓])?\s*[（(]\s*([A-Ga-g1-9])\s*[）)]/g;
  while((m=parRe.exec(s))){
    const idx=m.index;
    const prev=idx>0?s[idx-1]:'';
    if(prev && /[A-Za-z0-9\u4e00-\u9fa5）)】\]]/.test(prev))continue;
    hits.push({idx,len:m[0].length,key:m[2],correct:!!m[1]&&/[oOyYvV√✔✓]/.test(m[1])});
  }
  let uniq=selectOrderedOptionHits(hits);
  if(uniq.length<2)return null;
  const prefix=s.slice(0,uniq[0].idx).trim().replace(/[;；，,、]+$/,'').trim();
  // 如果第一组选项标记前只有题号，例如“86. A级油井水泥……”，这里的 A 是题干首字母，不是 A 选项。
  if(/^\s*(?:第\s*)?\d{1,4}\s*(?:题)?[、.．:：]\s*$/.test(prefix))return null;
  const options=uniq.map((h,i)=>{
    const start=h.idx+h.len;const end=i+1<uniq.length?uniq[i+1].idx:s.length;
    let txt=s.slice(start,end).trim().replace(/^[;；，,、]+/,'').replace(/[;；，,、]+$/,'').trim();
    const ex=extractTrailingAnswerFromText(txt,'');
    return {key:normalizeOptionKey(h.key),correct:h.correct,text:ex.text,extraAnswer:ex.answer};
  }).filter(o=>o.text||o.correct||o.extraAnswer.length);
  if(options.length<2)return null;
  return {prefix,options};
}

function renderImportReportPanel(arr, rows=[], warnings=[]){
  const el=$('#import-report-panel');if(!el)return;
  if(!arr||!arr.length){el.classList.add('hidden');el.innerHTML='';return}
  const d=importDiagnostics||{};
  const stats=countTypes(arr);
  const expected=d.expected||{};
  const expTotal=Number(expected.total||0);
  const diff=expTotal?arr.length-expTotal:0;
  const diffClass=!expTotal?'':' '+(Math.abs(diff)<=Math.max(2,Math.ceil(expTotal*0.05))?'ok':'warn');
  const statusList=(arr||[]).map(q=>importIssueStatus(q,d.profile||{}));
  const hardCount=statusList.filter(x=>importStatusSeverity(x)==='error').length;
  const riskCount=statusList.filter(x=>importStatusSeverity(x)==='warn').length;
  const repairedSegments=(d.candidates||[]).flatMap(c=>c.segments||[]);
  const candidates=(d.candidates||[]).map(c=>`<tr><td>${esc(c.name)}</td><td>${esc(c.questions)}</td><td>${esc(c.score)}</td><td>${esc((c.warnings||[]).length)}</td><td>${esc((c.segments||[]).length)}</td></tr>`).join('');
  const profile=d.profile||{};
  const profileBits=[];
  if(profile.hasVolumeHeading)profileBits.push('分卷');
  if(profile.hasTypeSections)profileBits.push('题型分区');
  if(profile.repeatedQuestionNumbers)profileBits.push('重复题号');
  if(profile.inlineOptionLikely)profileBits.push('同一行选项');
  if(profile.inlineAnswerLikely)profileBits.push('题尾答案');
  const typeExpected=expected.types||{};
  const expLine=Object.entries({single:'单选',multiple:'多选',judge:'判断',blank:'填空',short:'简答'}).map(([k,n])=>typeExpected[k]?`${n}${typeExpected[k]}题`:null).filter(Boolean).join('、');
  el.classList.remove('hidden');
  el.innerHTML=`<div class="report-grid">
    <div><span>采用策略</span><b>${esc(d.strategy||'未记录')}</b><small>模式：${esc(d.mode||'自动推荐')}</small></div>
    <div><span>识别题量</span><b>${arr.length}</b><small>单选${stats.single||0}｜多选${(stats.multiple||0)+(stats.multi||0)}｜判断${stats.judge||0}｜填空${stats.blank||0}｜简答${stats.short||0}</small></div>
    <div class="${diffClass}"><span>题量核对</span><b>${expTotal?`${arr.length}/${expTotal}`:'未发现标题题量'}</b><small>${expTotal?`差值 ${diff>0?'+':''}${diff}`:'可在预览区人工核对'}</small></div>
    <div><span>错误/异常</span><b>${hardCount}/${riskCount}</b><small>错误 ${hardCount}｜需确认 ${riskCount}</small></div>
  </div>
  <div class="report-detail"><b>格式画像：</b>${profileBits.length?profileBits.join('、'):'标准或低风险格式'}${expLine?`；<b>标题分布：</b>${esc(expLine)}`:''}${repairedSegments.length?`；<b>已局部修复：</b>${esc(repairedSegments.slice(0,3).join('；'))}${repairedSegments.length>3?'……':''}`:''}</div>
  ${candidates?`<details class="candidate-details"><summary>查看候选策略质量对比</summary><table><thead><tr><th>策略</th><th>题数</th><th>质量分</th><th>异常数</th><th>局部修复</th></tr></thead><tbody>${candidates}</tbody></table></details>`:''}`;
}
function importStatusSeverity(status){
  if(status==='正常')return 'ok';
  if(/^异常[:：]/.test(String(status||'')))return 'warn';
  return 'error';
}
function renderImportPreview(arr){
  importSelected=new Set([...importSelected].filter(i=>i>=0&&i<(arr||[]).length));
  const warnings=[...importWarnings];
  const rows=(arr||[]).map((q,i)=>{const status=importIssueStatus(q,importDiagnostics?.profile||{});if(status!=='正常')warnings.push(`第${i+1}题：${status}`);return{q,i,status}});
  let shown=[...rows];
  if(importPreviewFilter==='problem')shown=shown.filter(r=>r.status!=='正常');
  else if(importPreviewFilter==='normal')shown=shown.filter(r=>r.status==='正常');
  else if(importPreviewFilter==='priority')shown.sort((a,b)=>{const aw=a.status==='正常'?1:0,bw=b.status==='正常'?1:0;return aw-bw||a.i-b.i});
  const shownIdx=shown.map(r=>r.i);
  $('#import-preview tbody').innerHTML=shown.map(({q,i,status})=>{
    const severity=importStatusSeverity(status);
    const cls=severity==='ok'?'status-ok':severity==='error'?'status-error':'status-warn';
    const rowCls=severity==='ok'?'':severity==='error'?'error-row':'issue-row';
    const seqInfo=[q.volume,q.group,q.number?`原${q.number}`:''].filter(Boolean).join(' · ');
    const checked=importSelected.has(i)?'checked':'';
    return `<tr class="${rowCls}"><td class="select-cell"><input type="checkbox" class="import-row-check" data-select-import="${i}" ${checked}></td><td class="seq-cell"><b>${i+1}</b>${seqInfo?`<small>${esc(seqInfo)}</small>`:''}</td><td>${label(q.type)}</td><td>${esc(short(q.question,72))}</td><td>${q.options.length}</td><td>${esc(q.answer.join(''))}</td><td class="${cls}">${esc(status)}</td><td><div class="row-actions"><button class="ghost mini-btn" data-edit-import="${i}">编辑</button><button class="ghost danger mini-btn" data-delete-import="${i}">删除</button></div></td></tr>`
  }).join('');
  const filterLabel={priority:'异常优先',problem:'仅异常',normal:'仅正常',all:'全部'}[importPreviewFilter]||'异常优先';
  const report=importReport?`<div class="import-report">${esc(importReport)}</div>`:'';
  renderImportReportPanel(arr, rows, warnings);
  $('#import-summary').innerHTML=arr.length?`${report}<b>识别到 ${arr.length} 道题，当前显示 ${shown.length} 道（${filterLabel}），已选择 ${importSelected.size} 道。</b>${warnings.length?'<br>警告 '+warnings.length+' 条：<br>'+warnings.slice(0,12).map(esc).join('<br>')+(warnings.length>12?'<br>……':''):'<br>未发现明显异常。'}`:'尚未识别到题目。';
  $('#import-summary').className='notice '+(warnings.length?'warn':'ok');
  const pf=$('#import-preview-filter');if(pf&&pf.value!==importPreviewFilter)pf.value=importPreviewFilter;
  $('#confirm-import-btn').disabled=!arr.length;const dualConfirm=$('#dual-confirm-import-btn');if(dualConfirm)dualConfirm.disabled=!arr.length;
  const batchBtn=$('#batch-delete-import-btn');if(batchBtn)batchBtn.disabled=importSelected.size===0;
  const clearBtn=$('#clear-import-selection-btn');if(clearBtn)clearBtn.disabled=importSelected.size===0;
  const all=$('#import-select-all-visible');
  if(all){const selectedVisible=shownIdx.filter(i=>importSelected.has(i)).length;all.checked=shownIdx.length>0&&selectedVisible===shownIdx.length;all.indeterminate=selectedVisible>0&&selectedVisible<shownIdx.length;all.onchange=()=>{if(all.checked)shownIdx.forEach(i=>importSelected.add(i));else shownIdx.forEach(i=>importSelected.delete(i));renderImportPreview(importCache)}}
  $$('[data-select-import]').forEach(cb=>cb.onchange=()=>{const i=Number(cb.dataset.selectImport);if(cb.checked)importSelected.add(i);else importSelected.delete(i);renderImportPreview(importCache)});
  $$('[data-edit-import]').forEach(btn=>btn.onclick=()=>openEditQuestion(Number(btn.dataset.editImport)));
  $$('[data-delete-import]').forEach(btn=>btn.onclick=()=>{const i=Number(btn.dataset.deleteImport);if(confirm('删除这道识别结果？')){importCache.splice(i,1);importSelected=new Set([...importSelected].map(x=>x>i?x-1:x).filter(x=>x!==i));renderImportPreview(importCache)}});
}
function batchDeleteImportSelected(){
  const ids=[...importSelected].filter(i=>i>=0&&i<importCache.length).sort((a,b)=>b-a);
  if(!ids.length){toast('请先勾选要删除的题目。','warn');return}
  if(!confirm(`确定批量删除选中的 ${ids.length} 道识别结果？`))return;
  ids.forEach(i=>importCache.splice(i,1));
  importSelected.clear();
  renderImportPreview(importCache);
  toast(`已批量删除 ${ids.length} 道题。`,'ok');
}
function validateQuestion(q){
  if(!q.question)return'缺少题干';
  if(['single','multiple'].includes(q.type)&&!q.options.length)return'选择题缺少选项';
  if(q.type==='judge'){
    const map=judgeOptionMap(q.options||[]);
    const keys=(q.options||[]).map(o=>o.key);
    if(!keys.length)return'判断题缺少正确/错误选项';
    if(!q.answer.length)return'缺少答案';
    if(q.answer.some(a=>!keys.includes(a)))return'判断题答案无法映射到正确/错误';
    if(q.answer.length>1)return'判断题出现多个答案';
    if(!map.confidence&&!(keys.includes('A')&&keys.includes('B')))return'判断题选项含义疑似不明确';
  }
  if(!q.answer.length)return isTextType(q.type)?'缺少参考答案':'缺少答案';
  const keys=q.options.map(o=>o.key);
  if(['single','multiple'].includes(q.type)&&q.answer.some(a=>!keys.includes(a)))return'答案超出选项范围';
  if(q.type==='single'&&q.answer.length>1)return'单选题出现多个答案';
  if(q.type==='multiple'&&q.answer.length===1)return'多选题只有一个答案';
  if(['single','multiple','judge'].includes(q.type)&&/(?:^|[\s。？！?])A\s*[^\s]{1,40}$/.test(q.question||'')&&q.options.some(o=>o.key==='B')&&!q.options.some(o=>o.key==='A'))return'题干疑似混入A选项';
  if(/【?\s*(?:答案|正确答案|参考答案)|(?:答案|正确答案|参考答案)\s*(?:\:|：)/.test(q.question||''))return'题干残留答案标记';
  if((q.options||[]).some(o=>String(o.text||'').length>240||/【?\s*(?:答案|正确答案)|\b\d{1,3}\s*[、.．:：].+【?\s*答案/.test(o.text||'')))return'选项疑似粘连';
  return'正常';
}
function openEditQuestion(i){
  const q=importCache[i];if(!q)return;
  $('#edit-index').value=i;$('#edit-type').value=q.type||'single';$('#edit-question').value=q.question||'';$('#edit-answer').value=(q.answer||[]).join('');$('#edit-analysis').value=q.analysis||'';$('#edit-category').value=q.category||q.group||'';$('#edit-score').value=q.score||'';
  $('#edit-options').value=(q.options||[]).map(o=>`${o.key}. ${o.text}`).join('\n');
  $('#edit-status').textContent='可修改后保存。';$('#edit-status').className='notice';
  $('#edit-modal').classList.remove('hidden');$('#edit-modal').setAttribute('aria-hidden','false');
}
function closeEditModal(){
  $('#edit-modal').classList.add('hidden');$('#edit-modal').setAttribute('aria-hidden','true');
}
function parseOptionsText(text){
  const lines=String(text||'').split('\n').map(x=>x.trim()).filter(Boolean);
  const out=[];
  for(const line of lines){
    const m=line.match(/^\s*(?:[（(]\s*([A-Ga-g1-90])\s*[）)]|([A-Ga-g0])\s*(?:[、.．:：]|\s+)|([1-9])\s*(?:[、.．:：]|\s+))\s*(.*)$/);
    if(m){out.push({key:normalizeOptionKey(m[1]||m[2]||m[3]),text:(m[4]||'').trim()});}
    else{out.push({key:String.fromCharCode(65+out.length),text:line});}
  }
  return out.filter(o=>o.text);
}
function saveEditQuestion(){
  const i=Number($('#edit-index').value);if(!importCache[i])return;
  const options=parseOptionsText($('#edit-options').value);
  const raw={...importCache[i],type:$('#edit-type').value,question:$('#edit-question').value.trim(),options,answer:splitAnswer($('#edit-answer').value),analysis:$('#edit-analysis').value.trim(),category:$('#edit-category').value.trim(),score:$('#edit-score').value};
  const q=normalizeQuestion(raw,i);
  importCache[i]=q;
  const status=validateQuestion(q);
  $('#edit-status').textContent='已保存。当前状态：'+status;
  $('#edit-status').className='notice '+(status==='正常'?'ok':'warn');
  renderImportPreview(importCache);
}
function deleteEditQuestion(){
  const i=Number($('#edit-index').value);if(!importCache[i])return;
  if(confirm('删除这道题？')){importCache.splice(i,1);importSelected=new Set([...importSelected].map(x=>x>i?x-1:x).filter(x=>x!==i));closeEditModal();renderImportPreview(importCache)}
}
function confirmImport(){
  if(!importCache.length){showNotice('导入失败','当前没有可导入的题目。','danger');return}
  const warnings=collectImportWarnings(importCache);
  const name=$('#import-bank-name').value.trim()||'导入题库';
  const bank={id:'bank_'+Date.now(),name,createdAt:now(),questions:importCache.map((q,i)=>cleanImportedQuestion({...q,id:'q_'+Date.now()+'_'+i,number:i+1}))};
  state.banks.push(bank);state.activeBankId=bank.id;saveSilent();renderAll();
  showNotice('导入成功',`已创建题库“${name}”，共 ${bank.questions.length} 道题。${warnings.length?`导入前有 ${warnings.length} 条提示，建议在题库管理中抽查。`:''}`,'ok');
  toast(`已导入题库：${name}，共 ${bank.questions.length} 题。`,'ok','导入成功');
}

function renameActiveBank(){const b=activeBank();const val=$('#bank-rename-input').value.trim();if(!val){alert('请输入新的题库名称。');return}b.name=val;b.updatedAt=now();$('#bank-rename-input').value='';saveSilent();renderAll()}
function duplicateActiveBank(){duplicateBankById(activeBank().id)}
function duplicateBankById(id){const b=state.banks.find(x=>x.id===id);if(!b)return;const copy=JSON.parse(JSON.stringify(b));copy.id='bank_'+Date.now();copy.name=b.name+' - 副本';copy.createdAt=now();copy.updatedAt=now();copy.questions=(copy.questions||[]).map((q,i)=>({...q,id:'q_'+Date.now()+'_'+i,number:i+1}));state.banks.push(copy);state.activeBankId=copy.id;saveSilent();renderAll()}
function newEmptyBank(){const name=prompt('请输入新题库名称：','新建空题库');if(!name)return;const bank={id:'bank_'+Date.now(),name:name.trim()||'新建空题库',createdAt:now(),updatedAt:now(),questions:[]};state.banks.push(bank);state.activeBankId=bank.id;saveSilent();renderAll()}
function mergeBankIntoActive(){const sourceId=$('#merge-bank-select').value;const target=activeBank();const src=state.banks.find(b=>b.id===sourceId);if(!src){alert('没有可合并的来源题库。');return}if(!confirm(`将“${src.name}”的 ${src.questions.length} 道题合并到当前题库“${target.name}”？`))return;const before=target.questions.length;const existing=new Set(target.questions.map(q=>normalizeText(q.question)));let added=0,skipped=0;src.questions.forEach((q)=>{const k=normalizeText(q.question);if(existing.has(k)){skipped++;return}existing.add(k);target.questions.push({...JSON.parse(JSON.stringify(q)),id:'q_'+Date.now()+'_'+Math.random().toString(16).slice(2),number:target.questions.length+1});added++});target.updatedAt=now();saveSilent();renderAll();alert(`合并完成：新增 ${added} 题，跳过重复 ${skipped} 题。合并前 ${before} 题，当前 ${target.questions.length} 题。`)}
function exportBankById(id){const b=state.banks.find(x=>x.id===id);if(!b)return;const text=JSON.stringify(b,null,2);$('#export-output')&&($('#export-output').value=text);download((b.name||'题库')+'.json',text)}

function dedupeActiveBank(){const b=activeBank();const map=new Map(),dups=[];b.questions.forEach(q=>{const k=normalizeText(q.question);if(map.has(k))dups.push(q);else map.set(k,q)});b.questions=[...map.values()].map((q,i)=>({...q,number:i+1}));saveSilent();renderAll();alert(`去重完成：删除 ${dups.length} 道重复题，剩余 ${b.questions.length} 道。`)}
function filteredQuestions(source,type,order,limit){let qs=[...activeBank().questions];if(source==='wrong'){const ids=new Set(getWrongEntries(activeBank().id).filter(e=>e.status!=='已掌握').map(e=>e.id));qs=qs.filter(q=>ids.has(q.id))}if(type&&type!=='all'){const t=type==='multi'?'multiple':type;qs=qs.filter(q=>q.type===t)}if(order==='random')qs=shuffle(qs);if(limit==='half'){qs=qs.slice(0,Math.max(1,Math.ceil(qs.length/2)))}else if(limit&&limit!=='all'){qs=qs.slice(0,Number(limit))}return qs}
function shuffle(a){a=[...a];for(let i=a.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[a[i],a[j]]=[a[j],a[i]]}return a}
function startPractice(){
  const limit=$('#practice-limit').value;
  practice={items:filteredQuestions($('#practice-source').value,$('#practice-type').value,$('#practice-order').value,limit),idx:0,answered:0,correct:0,wrong:0,start:Date.now(),details:[]};
  if(!practice.items.length){$('#practice-card').innerHTML='<div class="empty">当前条件下没有题目。</div>';showNotice('无法开始练习','当前筛选条件下没有题目。','warn');return}
  if((limit==='all'||limit==='half')&&practice.items.length>500&&!confirm(`本轮将练习 ${practice.items.length} 道题，题量较大，可能导致页面加载和记录保存变慢，是否继续？`)){practice={items:[],idx:0,answered:0,correct:0,wrong:0,start:0,details:[]};return}
  enterPracticeFocus();
  showNotice('练习开始',`本轮共 ${practice.items.length} 道题。`,'ok');
  renderPracticeQuestion();
}
function startWrongPractice(){
  $$('.nav').forEach(b=>b.classList.remove('active'));document.querySelector('[data-view="practice"]').classList.add('active');$$('.view').forEach(v=>v.classList.remove('active'));$('#practice').classList.add('active');$('#page-title').textContent='刷题练习';
  $('#practice-source').value='wrong';$('#practice-order').value='random';$('#practice-limit').value='100';startPractice();
}
function enterPracticeFocus(){document.body.classList.add('practice-focus')}
function exitPracticeFocus(){document.body.classList.remove('practice-focus')}

function renderPracticeQuestion(done=false){$('#practice-progress').textContent=`${Math.min(practice.idx+1,practice.items.length)} / ${practice.items.length}`;if(done||practice.idx>=practice.items.length){finishPractice();return}const q=practice.items[practice.idx];$('#practice-card').innerHTML=`<div class="practice-focus-head"><b>刷题练习</b><span>${practice.idx+1} / ${practice.items.length}</span><button class="ghost mini-btn" id="p-exit">退出练习</button></div>`+questionHtml(q,false)+`<div class="actions"><button class="primary" id="p-submit">提交答案</button><button class="ghost" id="p-reveal">看答案</button><button class="ghost" id="p-next" disabled>下一题</button></div><div id="p-feedback"></div>`;bindOptionSelect('#practice-card',q);$('#p-exit').onclick=()=>{if(confirm('退出本轮练习？已作答部分会保存为一条记录。'))finishPractice(true)};$('#p-submit').onclick=()=>submitPractice(q,false);$('#p-reveal').onclick=()=>submitPractice(q,true);$('#p-next').onclick=()=>{practice.idx++;renderPracticeQuestion()}}
function questionHtml(q,examMode,idx=0){
  const meta=`<div class="qmeta"><span class="pill">${label(q.type)}</span><span class="pill">${esc(q.category||'未分类')}</span>${examMode?`<span class="pill">${scoreOf(q)}分</span>`:''}</div><div class="question-title">${examMode?idx+'. ':''}${esc(q.question)}</div>`;
  if(isTextType(q.type)){
    const placeholder=q.type==='short'?'请输入你的简答内容；提交后可对照参考答案。':'请输入答案；多个空可用分号分隔。';
    const input=q.type==='short'?`<textarea class="text-answer" data-qid="${esc(q.id)}" placeholder="${placeholder}"></textarea>`:`<input class="text-answer" data-qid="${esc(q.id)}" placeholder="${placeholder}" />`;
    return meta+`<div class="answer-input-wrap">${input}</div>`;
  }
  return meta+`<div class="options">${q.options.map(o=>`<label class="option" data-key="${esc(o.key)}"><input type="${q.type==='multiple'?'checkbox':'radio'}" name="q_${esc(q.id)}" value="${esc(o.key)}"><span class="option-key">${esc(o.key)}.</span><span>${esc(o.text)}</span></label>`).join('')}</div>`;
}
function bindOptionSelect(root,q){$$(root+' .option').forEach(opt=>{opt.onclick=()=>setTimeout(()=>{$$(root+' .option').forEach(o=>o.classList.toggle('selected',o.querySelector('input').checked))},0)})}
function selectedKeys(root){return $$(root+' input:checked').map(x=>x.value).sort()}
function textAnswer(root){const el=$(root+' .text-answer');return el&&el.value.trim()?[el.value.trim()]:[]}
function collectAnswer(root,q){return isTextType(q.type)?textAnswer(root):selectedKeys(root)}
function answerDisplay(ans){return (ans||[]).join(isTextTypeValueList(ans)?'；':'')}
function isTextTypeValueList(ans){return (ans||[]).some(x=>String(x).length>1&&!/^[A-G1-9]$/.test(String(x)))}
function same(a,b){return JSON.stringify([...a].sort())===JSON.stringify([...b].sort())}
function sameAnswerForQuestion(q,chosen,answer){
  if(isTextType(q.type)){
    const user=normalizeTextAnswerForCompare((chosen||[]).join('；'));
    return !!user&&(answer||[]).some(a=>normalizeTextAnswerForCompare(a)===user);
  }
  return same(chosen,answer);
}
function submitPractice(q,reveal){
  const chosen=collectAnswer('#practice-card',q);
  if(!chosen.length&&!reveal){$('#p-feedback').innerHTML='<div class="feedback warn">请先作答，再提交。</div>';return}
  if(q.type==='short'){
    showSubjectiveFeedback(q,chosen,reveal);
    return;
  }
  const ok=!reveal&&sameAnswerForQuestion(q,chosen,q.answer);
  if(!reveal){recordPracticeAnswer(q,chosen,ok)}
  markOptions('#practice-card',q,chosen);
  $('#p-feedback').innerHTML=`<div class="feedback ${reveal?'warn':ok?'ok':'bad'}">${reveal?'已显示参考答案':ok?'✓ 回答正确':'✕ 这题要再看一遍'}｜参考答案：${esc(q.answer.join('；'))}${q.analysis?'<br>解析：'+esc(q.analysis):''}</div>`;
  $('#p-submit').disabled=true;$('#p-reveal').disabled=true;$('#p-next').disabled=false;saveSilent();renderStats();
}
function showSubjectiveFeedback(q,chosen,reveal){
  const user=chosen.join('；')||'未填写';
  $('#p-feedback').innerHTML=`<div class="feedback warn">你的作答：${esc(user)}<br>参考答案：${esc(q.answer.join('；')||'未提供')}${q.analysis?'<br>解析：'+esc(q.analysis):''}<br><div class="actions"><button class="primary" id="p-self-right">判为正确</button><button class="danger" id="p-self-wrong">判为错误</button></div></div>`;
  $('#p-submit').disabled=true;$('#p-reveal').disabled=true;$('#p-next').disabled=true;
  $('#p-self-right').onclick=()=>{recordPracticeAnswer(q,chosen,true);$('#p-next').disabled=false;$('#p-self-right').disabled=true;$('#p-self-wrong').disabled=true;saveSilent();renderStats()};
  $('#p-self-wrong').onclick=()=>{recordPracticeAnswer(q,chosen,false);$('#p-next').disabled=false;$('#p-self-right').disabled=true;$('#p-self-wrong').disabled=true;saveSilent();renderStats()};
}
function recordPracticeAnswer(q,chosen,ok){practice.answered++;if(ok){practice.correct++;markRight(q.id)}else{practice.wrong++;addWrong(q.id)}practice.details.push(makeAnswerDetail(q,chosen,ok,scoreOf(q),scoreOf(q)))}
function markOptions(root,q,chosen){if(isTextType(q.type))return;$$(root+' .option').forEach(o=>{const k=o.dataset.key;if(q.answer.includes(k))o.classList.add('correct');else if(chosen.includes(k))o.classList.add('wrong')})}
function getWrongEntries(bid=activeBank().id){const v=state.wrongBook[bid]||[];if(!Array.isArray(v))return[];return v.map(x=>typeof x==='string'?{id:x,wrongCount:1,rightCount:0,lastWrongAt:'',lastCorrectAt:'',status:'未掌握'}:{id:x.id,wrongCount:Number(x.wrongCount||0),rightCount:Number(x.rightCount||0),lastWrongAt:x.lastWrongAt||'',lastCorrectAt:x.lastCorrectAt||'',status:x.status||'未掌握'}).filter(x=>x.id)}
function setWrongEntries(entries,bid=activeBank().id){state.wrongBook[bid]=entries}
function addWrong(id){const bid=activeBank().id;const arr=getWrongEntries(bid);let e=arr.find(x=>x.id===id);if(!e){e={id,wrongCount:0,rightCount:0,lastWrongAt:'',lastCorrectAt:'',status:'未掌握'};arr.push(e)}e.wrongCount++;e.lastWrongAt=now();e.status=e.wrongCount>=2?'未掌握':'复习中';setWrongEntries(arr,bid)}
function markRight(id){const bid=activeBank().id;const arr=getWrongEntries(bid);let e=arr.find(x=>x.id===id);if(!e)return;e.rightCount++;e.lastCorrectAt=now();e.status=e.rightCount>=2?'已掌握':'复习中';setWrongEntries(arr,bid)}
function removeWrong(id){const bid=activeBank().id;setWrongEntries(getWrongEntries(bid).filter(x=>x.id!==id),bid)}
function makeAnswerDetail(q,chosen,ok,score,totalScore){return {questionId:q.id,question:short(q.question,120),type:q.type,category:q.category||'',chosen:[...chosen],answer:[...q.answer],correct:!!ok,score:ok?score:0,fullScore:score,time:now()}}
function finishPractice(exited=false){
  const total=practice.answered;
  const rec={id:'rec_'+Date.now(),mode:'练习',bankId:activeBank().id,bankName:activeBank().name,total:practice.items.length,answered:total,correct:practice.correct,wrong:practice.wrong,accuracy:total?Math.round(practice.correct/total*100):0,score:null,date:now(),duration:Math.round((Date.now()-practice.start)/1000),details:practice.details||[]};
  if(total||exited)state.records.unshift(rec);
  saveSilent();
  $('#practice-card').innerHTML=`<div class="score-card"><div class="metric"><span>已答</span><b>${total}</b></div><div class="metric"><span>正确</span><b>${practice.correct}</b></div><div class="metric"><span>错误</span><b>${practice.wrong}</b></div><div class="metric"><span>正确率</span><b>${rec.accuracy}%</b></div></div><div class="notice ok">${exited?'本轮练习已退出，已保存已作答记录。':'本轮练习已完成，并已写入刷题记录。记录包含每题作答明细。'}</div><div class="actions"><button class="primary" id="back-practice-setup">返回练习设置</button><button class="ghost" id="again-practice">按当前条件再练一次</button></div>`;
  $('#back-practice-setup').onclick=()=>exitPracticeFocus();
  $('#again-practice').onclick=()=>startPractice();
  showNotice(exited?'练习已退出':'练习完成',`已答 ${total} 道，正确率 ${rec.accuracy}%。`,'ok');
  renderAll();
}

function startExam(){let count=Number($('#exam-count').value)||50;exam={name:($('#exam-name').value||'模拟考试').trim(),passScore:Number($('#exam-pass-score').value)||0,items:filteredQuestions('all',$('#exam-type').value,$('#exam-order').value,count),answers:{},start:Date.now(),deadline:0,timer:null,submitted:false};if(!exam.items.length){$('#exam-card').innerHTML='<div class="empty">当前条件下没有题目。</div>';return}const min=Number($('#exam-minutes').value)||0;if(min>0){exam.deadline=Date.now()+min*60000;clearInterval(exam.timer);exam.timer=setInterval(updateTimer,1000);updateTimer()}else $('#exam-timer').textContent='不限时';$('#submit-exam-btn').disabled=false;renderExamPaper()}
function updateTimer(){if(!exam.deadline)return;const left=Math.max(0,exam.deadline-Date.now());const m=Math.floor(left/60000),s=Math.floor((left%60000)/1000);$('#exam-timer').textContent=`剩余 ${m}:${String(s).padStart(2,'0')}`;if(left<=0)submitExam(true)}
function scoreOf(q){if(q.score)return q.score;return Number(q.type==='multiple'?$('#score-multiple').value:q.type==='judge'?$('#score-judge').value:q.type==='blank'?$('#score-blank').value:q.type==='short'?$('#score-short').value:$('#score-single').value)||0}
function renderExamPaper(){let html='<div class="notice warn">考试中：作答后点击底部“交卷评分”。多选题需完全一致才得分；填空/简答按参考答案规范化匹配评分，简答题建议交卷后人工核对。</div>';exam.items.forEach((q,i)=>{html+=`<div class="exam-q" data-qid="${esc(q.id)}">${questionHtml(q,true,i+1)}</div>`});$('#exam-card').innerHTML=html;$$('#exam-card .option').forEach(opt=>{opt.onclick=()=>setTimeout(()=>{const box=opt.closest('.exam-q');const id=box.dataset.qid;exam.answers[id]=selectedKeys(`[data-qid="${CSS.escape(id)}"]`);box.querySelectorAll('.option').forEach(o=>o.classList.toggle('selected',o.querySelector('input').checked))},0)});$$('#exam-card .text-answer').forEach(el=>{el.oninput=()=>{const box=el.closest('.exam-q');if(box)exam.answers[box.dataset.qid]=el.value.trim()?[el.value.trim()]:[]}})}
function collectExamTextAnswers(){if(!exam.items)return;exam.items.forEach(q=>{if(!isTextType(q.type))return;const box=$(`#exam-card [data-qid="${CSS.escape(q.id)}"]`);const el=box&&box.querySelector('.text-answer');if(el)exam.answers[q.id]=el.value.trim()?[el.value.trim()]:[]})}
function submitExam(auto){if(exam.submitted)return;collectExamTextAnswers();exam.submitted=true;clearInterval(exam.timer);let got=0,total=0,correct=0;const details=[];const byType={};exam.items.forEach(q=>{const sc=scoreOf(q);total+=sc;const ans=exam.answers[q.id]||[];const ok=sameAnswerForQuestion(q,ans,q.answer);if(ok){got+=sc;correct++} addWrongOnExam(q.id,!ok);details.push(makeAnswerDetail(q,ans,ok,sc,sc));const k=q.type||'single';byType[k]=byType[k]||{total:0,correct:0,score:0,fullScore:0};byType[k].total++;if(ok)byType[k].correct++;byType[k].score+=ok?sc:0;byType[k].fullScore+=sc;});const acc=Math.round(correct/exam.items.length*100);const rec={id:'rec_'+Date.now(),name:exam.name||'模拟考试',mode:'考试',bankId:activeBank().id,bankName:activeBank().name,total:exam.items.length,answered:Object.keys(exam.answers).length,correct,wrong:exam.items.length-correct,accuracy:acc,score:got,totalScore:total,passScore:exam.passScore,passed:got>=Number(exam.passScore||0),autoSubmitted:!!auto,date:now(),duration:Math.round((Date.now()-exam.start)/1000),details,byType};state.records.unshift(rec);saveSilent();$('#submit-exam-btn').disabled=true;$('#exam-timer').textContent=auto?'已自动交卷':'已交卷';renderExamResult(rec);renderAll()}
function addWrongOnExam(id,isWrong){if(isWrong)addWrong(id);else markRight(id)}
function renderExamResult(rec){const typeRows=Object.entries(rec.byType||{}).map(([t,v])=>`<tr><td>${label(t)}</td><td>${v.correct}/${v.total}</td><td>${Number(v.score.toFixed? v.score.toFixed(1):v.score)}/${v.fullScore}</td></tr>`).join('');let html=`<div class="score-card"><div class="metric"><span>得分</span><b>${rec.score}/${rec.totalScore}</b></div><div class="metric"><span>结果</span><b>${rec.passed?'合格':'未合格'}</b></div><div class="metric"><span>正确率</span><b>${rec.accuracy}%</b></div><div class="metric"><span>用时</span><b>${rec.duration}秒</b></div></div><div class="notice ${rec.passed?'ok':'warn'}">${esc(rec.name||'模拟考试')}：及格线 ${rec.passScore} 分，${rec.autoSubmitted?'系统已自动交卷。':'已交卷。'}</div>${typeRows?`<div class="table-wrap"><table><thead><tr><th>题型</th><th>正确</th><th>得分</th></tr></thead><tbody>${typeRows}</tbody></table></div>`:''}`;exam.items.forEach((q,i)=>{const ans=exam.answers[q.id]||[];html+=`<div class="exam-q" data-result-qid="${esc(q.id)}">${questionHtml(q,true,i+1)}<div class="feedback ${sameAnswerForQuestion(q,ans,q.answer)?'ok':'bad'}">你的答案：${esc(ans.join('；')||'未作答')}｜参考答案：${esc(q.answer.join('；'))}${q.analysis?'<br>解析：'+esc(q.analysis):''}</div></div>`});$('#exam-card').innerHTML=html;exam.items.forEach(q=>markOptions(`#exam-card [data-result-qid="${CSS.escape(q.id)}"]`,q,exam.answers[q.id]||[]))}
function renderWrongBook(){const bid=activeBank().id;let entries=getWrongEntries(bid);const filter=$('#wrong-status-filter')?.value||'active';if(filter==='active')entries=entries.filter(e=>e.status!=='已掌握');else if(filter!=='all')entries=entries.filter(e=>e.status===filter);const sort=$('#wrong-sort-mode')?.value||'lastWrong';if(sort==='wrongCount')entries.sort((a,b)=>b.wrongCount-a.wrongCount);else if(sort==='status')entries.sort((a,b)=>String(a.status).localeCompare(String(b.status),'zh-CN'));else entries.sort((a,b)=>String(b.lastWrongAt||'').localeCompare(String(a.lastWrongAt||'')));const map=new Map(activeBank().questions.map(q=>[q.id,q]));const rows=entries.map(e=>({e,q:map.get(e.id)})).filter(x=>x.q);$('#wrongbook-list').innerHTML=rows.length?rows.map(({e,q})=>`<div class="wrong-item"><div class="section-head"><div><b>${label(q.type)}｜${esc(short(q.question,80))}</b><p class="muted">答案：${esc(q.answer.join(''))}｜状态：${esc(e.status)}｜错误 ${e.wrongCount} 次｜做对 ${e.rightCount} 次${e.lastWrongAt?'｜最近错：'+fmt(e.lastWrongAt):''}${q.analysis?'｜解析：'+esc(short(q.analysis,80)):''}</p></div><div class="row-actions"><button class="ghost mini-btn" data-master-wrong="${esc(q.id)}">标记已掌握</button><button class="ghost danger mini-btn" data-remove-wrong="${esc(q.id)}">移出</button></div></div></div>`).join(''):'<p class="muted">当前条件下暂无错题。</p>';$$('[data-remove-wrong]').forEach(b=>b.onclick=()=>{removeWrong(b.dataset.removeWrong);saveSilent();renderAll()});$$('[data-master-wrong]').forEach(b=>b.onclick=()=>{const arr=getWrongEntries();const e=arr.find(x=>x.id===b.dataset.masterWrong);if(e){e.status='已掌握';e.lastCorrectAt=now();e.rightCount=Math.max(e.rightCount||0,2);setWrongEntries(arr);saveSilent();renderAll()}})}
function renderRecords(){const list=$('#records-list');let rows=[...state.records];const mode=$('#record-mode-filter')?.value||'all';if(mode!=='all')rows=rows.filter(r=>r.mode===mode);const lim=$('#record-limit')?.value||'30';if(lim!=='all')rows=rows.slice(0,Number(lim));list.innerHTML=rows.length?rows.map((r,idx)=>{const detail=(r.details||[]).slice(0,8).map((d,i)=>`<tr><td>${i+1}</td><td>${esc(short(d.question,42))}</td><td>${esc((d.chosen||[]).join('')||'未答')}</td><td>${esc((d.answer||[]).join(''))}</td><td>${d.correct?'正确':'错误'}</td></tr>`).join('');const summary=`题数${r.total}｜已答${r.answered}｜正确${r.correct}｜错误${r.wrong}｜正确率${r.accuracy}%${r.score!=null?`｜得分${r.score}/${r.totalScore}${r.passScore!=null?`｜及格线${r.passScore}｜${r.passed?'合格':'未合格'}`:''}`:''}｜用时${r.duration}秒`;return `<div class="record-item"><b>${esc(r.name||r.mode)}｜${esc(r.bankName)}｜${fmt(r.date)}</b><p class="muted">${summary}</p>${detail?`<details><summary>查看作答明细（前8题 / 共${(r.details||[]).length}题）</summary><div class="table-wrap"><table><thead><tr><th>#</th><th>题干</th><th>你的答案</th><th>正确答案</th><th>结果</th></tr></thead><tbody>${detail}</tbody></table></div></details>`:''}</div>`}).join(''):'<p class="muted">暂无刷题记录。</p>'}
function exportRecords(){const text=JSON.stringify(state.records||[],null,2);$('#export-output')&&($('#export-output').value=text);download('刷题记录.json',text)}
function fmt(s){return new Date(s).toLocaleString('zh-CN',{hour12:false})}
function exportCurrentBank(){const text=JSON.stringify(activeBank(),null,2);$('#export-output').value=text;download(activeBank().name+'.json',text)}
function exportAll(){const text=JSON.stringify(state,null,2);$('#export-output').value=text;download('shiroha_quiz_all_data.json',text)}
function download(name,text){const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([text],{type:'application/json;charset=utf-8'}));a.download=name;a.click();URL.revokeObjectURL(a.href)}


/* SHIROHA_V23_DATA_TOOLS_PATCH_START
   v23: 题库管理页批量选择导出 + 导入页/设置页备份 JSON 导入恢复
*/
function setupEnhancedDataToolsV23(){
  injectDataToolsStyleV23();
  ensureBackupFileInputV23();
  ensureBankManageExportPanelV23();
  ensureImportBackupPanelV23();
  ensureSettingsBackupPanelV23();
  if(!exportBankSelectedV23.size)exportBankSelectedV23=new Set(state.banks.map(b=>b.id));
  renderExportBankSummaryV23();
}
function ensureBackupFileInputV23(){
  if($('#backup-json-file-v23'))return;
  const input=document.createElement('input');
  input.type='file';input.id='backup-json-file-v23';input.accept='.json,application/json';input.style.display='none';
  input.onchange=importBackupJsonFileV23;
  document.body.appendChild(input);
}
function ensureBankManageExportPanelV23(){
  if($('#bank-bulk-export-panel-v23'))return;
  const bankList=$('#bank-list');if(!bankList)return;
  bankList.insertAdjacentHTML('beforebegin',`<div id="bank-bulk-export-panel-v23" class="data-tool-card-v23 bank-bulk-panel-v23">
    <div class="section-head compact-head-v23"><div><h3>批量选择导出题库</h3><p class="muted">在下方题库列表左侧勾选一个或多个题库，然后导出合成 JSON；该 JSON 可在“导入题库”页或“设置/导出”页导入。</p></div></div>
    <div class="actions wrap-v23">
      <label class="check-line-v23"><input id="export-bank-all-v23" type="checkbox">全选</label>
      <button class="ghost" id="export-bank-invert-v23" type="button">反选</button>
      <button class="ghost" id="export-bank-current-v23" type="button">仅选当前题库</button>
      <button class="primary" id="export-selected-banks-v23" type="button">导出选中题库</button>
      <button class="ghost" id="copy-selected-banks-v23" type="button">复制选中题库 JSON</button>
    </div>
    <div id="export-bank-summary-v23" class="notice warn">请选择需要导出的题库。</div>
  </div>`);
  $('#export-bank-all-v23').onchange=e=>{exportBankSelectedV23=e.target.checked?new Set(state.banks.map(b=>b.id)):new Set();renderBankList()};
  $('#export-bank-invert-v23').onclick=()=>{const next=new Set();state.banks.forEach(b=>{if(!exportBankSelectedV23.has(b.id))next.add(b.id)});exportBankSelectedV23=next;renderBankList()};
  $('#export-bank-current-v23').onclick=()=>{exportBankSelectedV23=new Set([activeBank().id]);renderBankList()};
  $('#export-selected-banks-v23').onclick=exportSelectedBanksV23;
  $('#copy-selected-banks-v23').onclick=copySelectedBanksJsonV23;
}
function ensureImportBackupPanelV23(){
  if($('#import-backup-panel-v23'))return;
  const importSection=$('#import');if(!importSection)return;
  const firstCard=importSection.querySelector('.card');if(!firstCard)return;
  firstCard.insertAdjacentHTML('afterend',`<div id="import-backup-panel-v23" class="card data-tools-v23">
    <div class="section-head"><div><p class="kicker">Backup Import</p><h2>导入配置 / 备份 JSON</h2></div></div>
    <p class="muted">这里专门用于导入 Shiroha Quiz 导出的 JSON 文件，包括“导出全部数据 JSON”和“题库管理页导出选中题库”生成的合成 JSON。普通 TXT / Word / PDF 题库仍走上方“单文件智能导入”。</p>
    <div class="form-grid">
      <label>导入方式<select id="import-backup-mode-v23"><option value="merge" selected>合并导入：保留当前数据，新增 JSON 内题库</option><option value="overwrite">覆盖恢复：用 JSON 替换当前本地数据</option></select></label>
      <label>说明<input disabled value="JSON 备份不会走普通题库识别解析" /></label>
    </div>
    <div class="actions"><button class="primary" id="import-backup-json-from-import-v23" type="button">导入配置/备份 JSON</button></div>
    <p class="muted">如果导入的是“选中题库”合成 JSON，建议选择“合并导入”；如果导入的是“全部数据备份”，可按需要选择合并或覆盖。</p>
  </div>`);
  $('#import-backup-json-from-import-v23').onclick=()=>{backupImportModeV23=$('#import-backup-mode-v23')?.value||'merge';$('#backup-json-file-v23').click()};
}
function ensureSettingsBackupPanelV23(){
  if($('#settings-backup-panel-v23'))return;
  const settingsCard=$('#settings .card');if(!settingsCard)return;
  settingsCard.insertAdjacentHTML('beforeend',`<div id="settings-backup-panel-v23" class="data-tools-v23">
    <h2>备份与恢复</h2>
    <p class="muted">用于完整备份/恢复本机 localStorage 中的 Shiroha Quiz 数据。手机 WebView 若无法下载文件，可使用“复制全部数据备份文本”。</p>
    <div class="form-grid">
      <label>恢复方式<select id="settings-backup-mode-v23"><option value="merge">合并导入：保留当前数据，新增备份中的题库</option><option value="overwrite" selected>覆盖恢复：用备份替换当前本地数据</option></select></label>
      <label>备份范围<input disabled value="全部题库、错题、记录、设置" /></label>
    </div>
    <div class="actions wrap-v23">
      <button class="primary" id="settings-export-all-backup-v23" type="button">导出全部数据备份</button>
      <button class="ghost" id="settings-copy-all-backup-v23" type="button">复制全部数据备份文本</button>
      <button class="ghost" id="settings-import-backup-v23" type="button">导入全部数据/备份 JSON</button>
    </div>
    <p class="muted">提示：覆盖恢复会替换本机数据；合并导入遇到同名题库会自动追加“_导入”。下方文本框会显示最近一次导出或导入的 JSON。</p>
  </div>`);
  $('#settings-export-all-backup-v23').onclick=exportAllBackupV23;
  $('#settings-copy-all-backup-v23').onclick=copyAllBackupJsonV23;
  $('#settings-import-backup-v23').onclick=()=>{backupImportModeV23=$('#settings-backup-mode-v23')?.value||'overwrite';$('#backup-json-file-v23').click()};
  const oldAll=$('#export-all-btn');if(oldAll)oldAll.onclick=exportAllBackupV23;
}
function injectDataToolsStyleV23(){
  if($('#data-tools-style-v23'))return;
  const style=document.createElement('style');
  style.id='data-tools-style-v23';
  style.textContent=`
    .data-tools-v23{margin:16px 0;padding:16px;border:1px solid rgba(120,144,180,.28);border-radius:16px;background:rgba(248,251,255,.72)}
    .data-tools-v23 h2{margin:0 0 8px}.data-tools-v23 h3{margin:0 0 10px}
    .data-tool-card-v23{margin:14px 0;padding:14px;border:1px solid rgba(120,144,180,.22);border-radius:14px;background:rgba(255,255,255,.82)}
    .bank-bulk-panel-v23{background:rgba(248,251,255,.9)}.compact-head-v23{margin-bottom:8px}
    .wrap-v23{display:flex;flex-wrap:wrap;gap:8px;align-items:center}.check-line-v23{display:inline-flex;align-items:center;gap:6px;cursor:pointer}
    .bank-bulk-check-v23{display:flex;align-items:center;padding-right:8px}.bank-bulk-check-v23 input{width:18px;height:18px;cursor:pointer}
  `;
  document.head.appendChild(style);
}
function renderExportBankSummaryV23(){
  const summary=$('#export-bank-summary-v23');
  const selected=selectedBanksV23();const qCount=selected.reduce((n,b)=>n+(b.questions||[]).length,0);
  const all=$('#export-bank-all-v23');if(all){all.checked=state.banks.length>0&&selected.length===state.banks.length;all.indeterminate=selected.length>0&&selected.length<state.banks.length}
  if(summary){summary.textContent=selected.length?`已选择 ${selected.length} 个题库，共 ${qCount} 道题。`:'请至少选择一个题库。';summary.className='notice '+(selected.length?'ok':'warn')}
  const btn=$('#export-selected-banks-v23');if(btn)btn.disabled=!selected.length;
  const copy=$('#copy-selected-banks-v23');if(copy)copy.disabled=!selected.length;
}
function selectedBanksV23(){return state.banks.filter(b=>exportBankSelectedV23.has(b.id))}
function cleanFileNameV23(s){return String(s||'').replace(/[\\/:*?"<>|]/g,'_').replace(/\s+/g,'_').slice(0,80)||'bank'}
function todayV23(){return new Date().toISOString().slice(0,10)}
function buildBackupPayloadV23(banks,exportType='selected_banks',includeAll=false){
  const bankIds=new Set((banks||[]).map(b=>b.id));
  const wrongBook={};Object.keys(state.wrongBook||{}).forEach(id=>{if(includeAll||bankIds.has(id))wrongBook[id]=state.wrongBook[id]});
  return {app:'Shiroha Quiz',schemaVersion:1,exportType,exportedAt:now(),banks:banks||[],wrongBook,records:includeAll?(state.records||[]):[],settings:includeAll?(state.settings||{}):{},activeBankId:includeAll?state.activeBankId:((banks&&banks[0]&&banks[0].id)||'')};
}
function exportSelectedBanksV23(){
  const banks=selectedBanksV23();if(!banks.length){toast('请至少选择一个题库。','warn');return}
  const payload=buildBackupPayloadV23(banks,banks.length===1?'single_bank':'selected_banks',false);
  const text=JSON.stringify(payload,null,2);setBackupPreviewV23(text);
  const name=banks.length===1?`shiroha-quiz-bank-${cleanFileNameV23(banks[0].name)}-${todayV23()}.json`:`shiroha-quiz-selected-banks-${todayV23()}.json`;
  download(name,text);toast(`已导出 ${banks.length} 个题库。该 JSON 可在“导入题库”页的“导入配置/备份 JSON”中导入。`,'ok');
}
function exportAllBackupV23(){
  const payload=buildBackupPayloadV23(state.banks||[],'all_data',true);
  const text=JSON.stringify(payload,null,2);setBackupPreviewV23(text);
  download(`shiroha-quiz-all-data-${todayV23()}.json`,text);toast('已生成全部数据备份。手机端若未弹出下载，可复制文本框内容。','ok');
}
async function copySelectedBanksJsonV23(){
  const banks=selectedBanksV23();if(!banks.length){toast('请至少选择一个题库。','warn');return}
  const text=JSON.stringify(buildBackupPayloadV23(banks,banks.length===1?'single_bank':'selected_banks',false),null,2);setBackupPreviewV23(text);
  await copyTextV23(text,'已复制选中题库备份文本。');
}
async function copyAllBackupJsonV23(){
  const text=JSON.stringify(buildBackupPayloadV23(state.banks||[],'all_data',true),null,2);setBackupPreviewV23(text);
  await copyTextV23(text,'已复制全部数据备份文本。');
}
async function copyTextV23(text,okMsg){try{await navigator.clipboard.writeText(text);toast(okMsg,'ok')}catch(e){toast('浏览器不允许自动复制，请手动复制下方文本。','warn')}}
function setBackupPreviewV23(text){const out=$('#export-output');if(out)out.value=text}
async function importBackupJsonFileV23(e){
  const file=e.target.files&&e.target.files[0];if(!file)return;
  try{
    const text=await new Promise((resolve,reject)=>{const r=new FileReader();r.onload=()=>resolve(String(r.result||''));r.onerror=()=>reject(new Error('文件读取失败'));r.readAsText(file,'UTF-8')});
    setBackupPreviewV23(text);
    const data=JSON.parse(text);const normalized=normalizeBackupPayloadV23(data,file.name);
    if(!normalized.banks.length){toast('没有在 JSON 中找到可导入的题库。','warn');return}
    const mode=backupImportModeV23||'merge';
    if(mode==='overwrite'){
      if(!confirm(`覆盖恢复会清空当前本地数据，并导入 ${normalized.banks.length} 个题库。确定继续？`))return;
      state.banks=normalized.banks;state.activeBankId=normalized.activeBankId||state.banks[0]?.id||'';state.wrongBook=normalized.wrongBook||{};state.records=Array.isArray(normalized.records)?normalized.records:[];state.settings=normalized.settings&&typeof normalized.settings==='object'?normalized.settings:{};
    }else{
      mergeBackupBanksV23(normalized);
    }
    upgradeState();ensureDefaultBank();saveSilent();renderAll();setupEnhancedDataToolsV23();
    const total=normalized.banks.reduce((n,b)=>n+(b.questions||[]).length,0);
    toast(`导入完成：${normalized.banks.length} 个题库，${total} 道题。`,'ok');
  }catch(err){toast('导入备份 JSON 失败：'+err.message,'danger')}
  finally{e.target.value=''}
}
function normalizeBackupPayloadV23(data,fileName){
  if(!data||typeof data!=='object')throw new Error('JSON 根节点不是对象');
  let banks=[];let wrongBook={};let records=[];let settings={};let activeBankId='';
  if(Array.isArray(data.banks)){banks=data.banks;wrongBook=data.wrongBook||{};records=Array.isArray(data.records)?data.records:[];settings=data.settings||{};activeBankId=data.activeBankId||''}
  else if(Array.isArray(data.questions)){banks=[{id:data.id||('bank_'+Date.now()),name:data.name||cleanFileNameV23(fileName).replace(/\.json$/i,'')||'导入题库',createdAt:data.createdAt||now(),updatedAt:now(),questions:data.questions}]}
  else if(Array.isArray(data)){banks=[{id:'bank_'+Date.now(),name:cleanFileNameV23(fileName).replace(/\.json$/i,'')||'导入题库',createdAt:now(),updatedAt:now(),questions:data}]}
  else throw new Error('不是 Shiroha Quiz 备份，也不是单题库 JSON');
  banks=banks.map((b,i)=>({id:String(b.id||('bank_import_'+Date.now()+'_'+i)),name:String(b.name||b.title||('导入题库_'+(i+1))),createdAt:b.createdAt||now(),updatedAt:now(),questions:(b.questions||[]).map((q,j)=>normalizeQuestion(q,j)).filter(q=>q.question)})).filter(b=>b.questions.length||b.name);
  return {banks,wrongBook,records,settings,activeBankId};
}
function mergeBackupBanksV23(normalized){
  const existingNames=new Set(state.banks.map(b=>b.name));
  const existingIds=new Set(state.banks.map(b=>b.id));
  normalized.banks.forEach((bank,idx)=>{
    const oldId=bank.id;
    let name=bank.name||('导入题库_'+(idx+1));
    if(existingNames.has(name)){let n=1;const base=name+'_导入';while(existingNames.has(base+n))n++;name=base+n}
    let id=bank.id;if(existingIds.has(id))id='bank_import_'+Date.now()+'_'+idx;
    existingIds.add(id);existingNames.add(name);
    const next={...bank,id,name,createdAt:bank.createdAt||now(),updatedAt:now(),questions:(bank.questions||[]).map((q,i)=>({...normalizeQuestion(q,i),id:q.id||('q_'+Date.now()+'_'+idx+'_'+i),number:q.number||i+1}))};
    state.banks.push(next);
    if(normalized.wrongBook&&normalized.wrongBook[oldId])state.wrongBook[id]=normalized.wrongBook[oldId];
  });
  state.activeBankId=state.banks[state.banks.length-normalized.banks.length]?.id||state.activeBankId;
}
/* SHIROHA_V23_DATA_TOOLS_PATCH_END */

function resetData(){if(confirm('确定清除全部本地数据？默认题库也会重新初始化。')){localStorage.removeItem(KEY);location.reload()}}
})();
