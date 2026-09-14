'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const fsp = require('node:fs/promises');
const http = require('node:http');
const path = require('node:path');
const { fixture, MOCK_ENDPOINT, FAKE_KEY, MODEL, BANK_ID } = require('./fixtures.cjs');

const TEST_ROOT = __dirname;
const WEB_ROOT = path.resolve(TEST_ROOT, '../../apps/web');
const DEPENDENCIES = 'C:\\Users\\REiQEr\\.cache\\codex-runtimes\\codex-primary-runtime\\dependencies\\node\\node_modules';
const SEL = {
  panel: '#p-ai-v613', open: '[data-ai-single-open-v613]', regenerate: '[data-ai-single-analysis-v613]',
  follow: '[data-ai-single-follow-v613]', input: '#practice-ai-follow-input-v613', send: '[data-ai-single-send-v613]',
  save: '[data-ai-single-save-v613]', stop: '[data-ai-single-stop-v613]', close: '[data-ai-single-close-v613]',
  settings: '[data-ai-single-settings-v613]',
  text: '.practice-ai-text-v613', chat: '.practice-ai-chat-v613', confidence: '.practice-ai-confidence-v613',
};

function within(root, target) {
  const relative = path.relative(root, target);
  return relative !== '' && !relative.startsWith(`..${path.sep}`) && relative !== '..' && !path.isAbsolute(relative);
}

function reportPath(value) {
  const target = path.resolve(TEST_ROOT, value);
  assert(within(TEST_ROOT, target), 'Report must be a file inside test/web-ai-practice/.');
  // Check existing ancestors too, so a junction cannot redirect output outside the test directory.
  let ancestor = path.dirname(target);
  while (!fs.existsSync(ancestor)) ancestor = path.dirname(ancestor);
  const actualRoot = fs.realpathSync(TEST_ROOT);
  assert(ancestor === TEST_ROOT || fs.realpathSync(ancestor) === actualRoot || within(actualRoot, fs.realpathSync(ancestor)), 'Unsafe report ancestor.');
  if (fs.existsSync(target)) assert(within(actualRoot, fs.realpathSync(target)), 'Unsafe report file.');
  return target;
}

function storageKeys() {
  const source = fs.readFileSync(path.join(WEB_ROOT, 'app.js'), 'utf8');
  function read(name) {
    const match = source.match(new RegExp(`\\bconst\\s+${name}\\s*=\\s*(['"])([^'"\\r\\n]+)\\1\\s*;`));
    assert(match, `Cannot read ${name} from apps/web/app.js; update the test adapter, not application source.`);
    return match[2];
  }
  return { state: read('KEY'), session: read('AI_KEY_SESSION_V99'), local: read('AI_KEY_LOCAL_V99') };
}

function loadPlaywright() {
  try { return require('playwright'); }
  catch (error) {
    if (error.code !== 'MODULE_NOT_FOUND' || !error.message.startsWith("Cannot find module 'playwright'")) throw error;
  }
  try { return require(require.resolve('playwright', { paths: [DEPENDENCIES] })); }
  catch (cause) { throw new Error(`Playwright is not already available through require('playwright') or the machine-specific bundled fallback ${DEPENDENCIES}. Provide existing Node + Playwright + Chromium; no install, network access or framework fallback was attempted.`, { cause }); }
}

const MIME = { '.html': 'text/html; charset=utf-8', '.js': 'application/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.png': 'image/png', '.webp': 'image/webp', '.jpg': 'image/jpeg', '.svg': 'image/svg+xml', '.woff': 'font/woff', '.woff2': 'font/woff2', '.ico': 'image/x-icon' };

async function startServer() {
  const root = await fsp.realpath(WEB_ROOT);
  const server = http.createServer(async (request, response) => {
    try {
      if (!['GET', 'HEAD'].includes(request.method)) { response.writeHead(405); response.end(); return; }
      const raw = request.url.split('?')[0];
      const decoded = decodeURIComponent(raw);
      // Reject before URL normalization and verify both lexical and real paths (including junctions).
      if (!decoded.startsWith('/') || /[\\\0:]/.test(decoded) || decoded.split('/').some(part => part === '..')) {
        response.writeHead(403); response.end(); return;
      }
      const target = path.resolve(root, `.${decoded === '/' ? '/index.html' : decoded}`);
      if (!within(root, target)) { response.writeHead(403); response.end(); return; }
      const actual = await fsp.realpath(target);
      if (!within(root, actual)) { response.writeHead(403); response.end(); return; }
      const stat = await fsp.stat(actual);
      if (!stat.isFile()) { response.writeHead(404); response.end(); return; }
      // Never serve bundled question-bank data, even during framework self-tests.
      const data = path.basename(actual).toLowerCase() === 'question-bank.js'
        ? Buffer.from('/* Built-in banks suppressed by Web AI E2E. */') : await fsp.readFile(actual);
      response.writeHead(200, { 'Content-Type': MIME[path.extname(actual)] || 'application/octet-stream', 'Content-Length': data.length, 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
      response.end(request.method === 'HEAD' ? undefined : data);
    } catch (error) {
      response.writeHead(error instanceof URIError ? 400 : error.code === 'ENOENT' || error.code === 'ENOTDIR' ? 404 : 500);
      response.end();
    }
  });
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(0, '127.0.0.1', resolve); });
  return { url: `http://127.0.0.1:${server.address().port}/`, root, close: () => new Promise((resolve, reject) => { server.close(error => error ? reject(error) : resolve()); server.closeIdleConnections(); }) };
}

function baseUrl(value) {
  const url = new URL(value);
  assert(url.protocol === 'http:' && ['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname), '--base-url must be an HTTP loopback static service.');
  assert(!url.username && !url.password && !url.search && !url.hash, 'Base URL cannot contain credentials, query or hash.');
  if (!url.pathname.endsWith('/') && !url.pathname.endsWith('/index.html')) url.pathname += '/';
  return url.href;
}

function deferred() {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return { promise, resolve };
}

class MockAi {
  constructor() { this.queue = []; this.calls = []; this.violations = []; this.tasks = new Set(); this.entries = []; }

  enqueue(kind, content, { status = 200, hold = false, raw = false } = {}) {
    const arrived = deferred();
    const released = deferred();
    const entry = { kind, content, status, raw, arrived, released, hold };
    this.entries.push(entry);
    this.queue.push(entry);
    if (!hold) released.resolve();
    return { received: arrived.promise, release: () => released.resolve(), entry };
  }

  async handle(route) {
    const task = this.respond(route);
    this.tasks.add(task);
    try { await task; } finally { this.tasks.delete(task); }
  }

  async respond(route) {
    const request = route.request();
    if (request.method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers: { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'authorization,content-type', 'Access-Control-Allow-Methods': 'POST,OPTIONS' } });
      return;
    }
    let body;
    try { body = request.postDataJSON(); } catch { body = null; }
    const call = { url: request.url(), method: request.method(), headers: request.headers(), body };
    this.calls.push(call);
    const entry = this.queue.shift();
    if (entry) entry.request = request;
    const errors = [];
    if (call.url !== MOCK_ENDPOINT || call.method !== 'POST') errors.push('Unexpected mock endpoint or method.');
    if (body?.model !== MODEL || body?.stream !== false || !Array.isArray(body?.messages)) errors.push('Unexpected AI request shape/model.');
    if (call.headers.authorization !== `Bearer ${FAKE_KEY}`) errors.push('Only the synthetic session key may be sent.');
    if (!entry) errors.push('Unplanned AI request.');
    if (entry?.kind === 'follow' && body?.response_format) errors.push('Follow-up must use plainText, without response_format.');
    if (entry?.kind === 'analysis' && body?.response_format?.type !== 'json_object') errors.push('Analysis must request a JSON object.');
    if (errors.length) {
      this.violations.push(...errors);
      entry?.arrived.resolve(call);
      await route.fulfill({ status: 599, contentType: 'application/json', body: JSON.stringify({ error: { message: errors.join(' ') } }) });
      return;
    }
    entry.arrived.resolve(call);
    await entry.released.promise;
    const content = typeof entry.content === 'string' ? entry.content : JSON.stringify(entry.content);
    const response = entry.raw ? content : JSON.stringify(entry.status >= 400 ? { error: { message: 'Synthetic mock failure.' } } : { choices: [{ message: { role: 'assistant', content } }] });
    try { await route.fulfill({ status: entry.status, contentType: 'application/json', headers: { 'Access-Control-Allow-Origin': '*' }, body: response }); }
    catch (error) {
      // A stopped request or closed context may no longer accept a response.
      if (!/closed|disposed|Invalid InterceptionId|not found|already handled|canceled|cancelled/i.test(error.message)) throw error;
    }
  }

  async shutdown() {
    this.entries.forEach(entry => entry.released.resolve());
    await Promise.allSettled([...this.tasks]);
  }
}

async function bounded(promise, timeout, message) {
  let timer;
  try { return await Promise.race([promise, new Promise((_, reject) => { timer = setTimeout(() => reject(new Error(message)), timeout); })]); }
  finally { clearTimeout(timer); }
}

async function setup(browser, url, keys, { configured = true, rich = false, ignoreAbort = false, timeout = 10000 } = {}) {
  const context = await browser.newContext({ serviceWorkers: 'block', viewport: { width: 1440, height: 1000 }, acceptDownloads: false });
  const mock = new MockAi();
  const blocked = [];
  const pageErrors = [];
  let bridgeUsed = false;
  const page = await context.newPage();
  page.setDefaultTimeout(timeout);
  page.setDefaultNavigationTimeout(timeout * 3);
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('dialog', dialog => dialog.accept());
  const location = new URL(url);
  const staticPrefix = location.pathname.endsWith('index.html') ? location.pathname.slice(0, -'index.html'.length) : location.pathname;
  await context.route('**/*', async route => {
    const request = route.request();
    const destination = new URL(request.url());
    // All fetch/XHR traffic is blocked unless handled by the mock route registered below.
    const isStatic = destination.origin === location.origin && destination.pathname.startsWith(staticPrefix)
      && ['GET', 'HEAD'].includes(request.method()) && !['fetch', 'xhr'].includes(request.resourceType());
    if (isStatic) {
      if (destination.pathname.endsWith('/question-bank.js')) {
        await route.fulfill({ contentType: 'application/javascript', body: '/* Built-in bank suppressed. */' });
      } else if (destination.pathname.endsWith('/app.js')) {
        const response = await route.fetch({ maxRedirects: 0 });
        assert.equal(response.status(), 200, 'Local static service did not return app.js.');
        let source = await response.text();
        if (/^\s*\(function\(\)\{/.test(source)) {
          const endings = [...source.matchAll(/^\}\)\(\);/gm)];
          assert.equal(endings.length, 1, 'Cannot safely locate the application IIFE end for a read-only bridge.');
          const offset = endings[0].index;
          const fixtureHook = rich ? `\nwindow.__webAiPracticeSeedRich = ${seedRichFixture.toString()};\n` : '';
          source = `${source.slice(0, offset)}\nwindow.__webAiPracticeSnapshot = ${readSnapshot.toString()};\n${fixtureHook}${source.slice(offset)}`;
          bridgeUsed = true;
        }
        await route.fulfill({ status: 200, contentType: 'application/javascript; charset=utf-8', body: source });
      } else await route.continue();
      return;
    }
    blocked.push({ url: request.url(), method: request.method(), type: request.resourceType() });
    await route.abort('blockedbyclient');
  });
  // Last registered route wins; no fall-through to a real network request is permitted.
  await page.route('https://mock-ai.invalid/**', route => mock.handle(route));
  if (typeof context.routeWebSocket === 'function') await context.routeWebSocket('**/*', socket => { blocked.push({ url: socket.url(), type: 'websocket' }); socket.close(); });
  await context.addInitScript(({ keys: storage, initial, key, ignoreAbort: ignore }) => {
    localStorage.clear();
    sessionStorage.clear();
    localStorage.setItem(storage.state, JSON.stringify(initial));
    sessionStorage.setItem(storage.session, key);
    if (ignore) {
      // Fault injection: emulate a response that races with abort, exercising session identity guards.
      const original = window.fetch.bind(window);
      window.fetch = (input, options) => {
        const target = typeof input === 'string' ? input : input?.url;
        return original(input, target?.startsWith('https://mock-ai.invalid/') ? { ...options, signal: undefined } : options);
      };
    }
  }, { keys, initial: fixture({ configured, rich }), key: FAKE_KEY, ignoreAbort });

  const harness = {
    page, context, mock, blocked, pageErrors, timeout, keys,
    get bridgeUsed() { return bridgeUsed; },
    async boot() {
      await page.goto(url, { waitUntil: 'load' });
      await page.locator('#start-practice-btn').waitFor({ state: 'attached' });
      if (rich) {
        // Existing upgradeState normalizes away richContent; seed only our safe fixture after upgrade.
        const prepared = fixture({ configured, rich }).banks[0].questions[0];
        const hook = await page.evaluate(() => typeof window.__webAiPracticeSeedRich === 'function');
        if (hook) await page.evaluate(prepared => window.__webAiPracticeSeedRich(prepared), prepared);
        else await page.evaluate(seedRichFixture, prepared);
      }
    },
    async start() {
      await page.locator('.nav[data-view="practice"]').click();
      await page.locator('#practice-type').selectOption('all');
      await page.locator('#practice-source').selectOption('all');
      await page.locator('#practice-order').selectOption('sequence');
      await page.locator('#practice-start-mode-v58916').selectOption('from_start');
      await page.locator('#practice-limit').selectOption('all');
      await page.locator('#start-practice-btn').click();
      await page.locator('#p-reveal').waitFor();
    },
    async jump(index) { await page.locator(`[data-practice-jump="${index}"]`).click(); },
    async reveal() { await page.locator('#p-reveal').click(); },
    async submitChoice(key = 'A') { await page.locator(`#practice-card .option[data-key="${key}"]`).click(); await page.locator('#p-submit').click(); },
    async waitCall(entry) { return bounded(entry.received, timeout, 'Expected mocked AI request did not arrive.'); },
    async deliver(entry) {
      const response = page.waitForResponse(response => response.request() === entry.entry.request, { timeout });
      entry.release();
      const received = await response;
      const error = await received.finished();
      assert.equal(error, null, 'Delayed response was not actually delivered.');
      await page.evaluate(() => new Promise(resolve => setTimeout(resolve, 0)));
    },
    async idle() {
      await page.waitForFunction(selector => {
        const panel = document.querySelector(selector);
        return panel && !panel.querySelector('.is-busy') && !panel.querySelector('[data-ai-single-stop-v613]');
      }, SEL.panel, { timeout });
    },
    async text(expected) {
      await page.waitForFunction(({ selector, expected: text }) => document.querySelector(selector)?.textContent.includes(text), { selector: `${SEL.panel} ${SEL.text}`, expected }, { timeout });
      await harness.idle();
    },
    async generate(result, { regenerate = false, status = 200, raw = false, visibleText } = {}) {
      const entry = mock.enqueue('analysis', result, { status, raw });
      await page.locator(regenerate ? SEL.regenerate : SEL.open).click();
      const call = await harness.waitCall(entry);
      if (status === 200 && !raw) await harness.text(visibleText || result.analysis);
      else await harness.idle();
      return call;
    },
    async followInput() {
      if (!await page.locator(SEL.input).isVisible()) await page.locator(SEL.follow).click();
      await page.locator(SEL.input).waitFor();
    },
    async follow(text, reply) {
      await harness.followInput();
      await page.locator(SEL.input).fill(text);
      const entry = mock.enqueue('follow', reply);
      await page.locator(SEL.send).click();
      const call = await harness.waitCall(entry);
      await page.waitForFunction(({ selector, reply: value }) => document.querySelector(selector)?.textContent.includes(value), { selector: SEL.chat, reply }, { timeout });
      await harness.idle();
      return call;
    },
    async expandExisting() {
      const count = mock.calls.length;
      if (await page.locator(SEL.open).isVisible()) await page.locator(SEL.open).click();
      await harness.idle();
      assert.equal(mock.calls.length, count, 'Reopening an existing draft must not generate again.');
    },
    async quiet(milliseconds = 180) {
      const count = mock.calls.length;
      await page.waitForTimeout(milliseconds);
      assert.equal(mock.calls.length, count, 'AI request occurred without a generate/send action.');
    },
    async cleanup() { await context.close(); await mock.shutdown(); },
  };
  return harness;
}

function seedRichFixture(prepared) {
  const bank = state.banks.find(bank => bank.id === 'web-ai-practice-safe-bank');
  if (state.banks.length !== 1 || !bank || prepared.id !== 'safe-single-1') throw new Error('Refusing to seed rich fields outside the synthetic fixture.');
  const question = bank.questions.find(question => question.id === prepared.id);
  question.richContent = JSON.parse(JSON.stringify(prepared.richContent));
}

function readSnapshot() {
    if (typeof state === 'undefined' || typeof practice === 'undefined') throw new Error('V613 adapter requires global lexical state/practice. Current app may still be inside an IIFE; ask the parent task for state hooks.');
    // Track reference identity in test globals without adding properties to application objects.
    const identities = globalThis.__webAiPracticeIdentities ||= { ids: new WeakMap(), next: 1 };
    const identity = object => {
      if (!object || typeof object !== 'object') return null;
      if (!identities.ids.has(object)) identities.ids.set(object, identities.next++);
      return identities.ids.get(object);
    };
    const item = practice.items[practice.idx];
    const question = item?.question && typeof item.question === 'object' ? item.question : item;
    const bankId = item?.bankId || state.activeBankId;
    const key = item?.sessionKey || `${bankId}#${question?.id || ''}`;
    return JSON.parse(JSON.stringify({
      question, bank: state.banks.find(bank => bank.id === bankId), key,
      identities: { practice: identity(practice), answerState: identity(practice.answerState) },
      practice: {
        idx: practice.idx, answered: practice.answered, correct: practice.correct, wrong: practice.wrong, start: practice.start,
        answerState: practice.answerState, details: practice.details, progressKey: practice.progressKey,
        startMode: practice.startMode, startIndex: practice.startIndex, baseItemKeys: practice.baseItemKeys,
        items: practice.items.map(entry => ({ bankId: entry.bankId, sessionKey: entry.sessionKey, questionId: (entry.question || entry).id })),
      },
      progress: state.settings.practiceProgressV58916, wrongBook: state.wrongBook, favorites: state.favorites,
      records: state.records, settings: state.settings, activeBankId: state.activeBankId,
      ui: {
        progress: document.querySelector('#practice-progress')?.textContent,
        submitDisabled: document.querySelector('#p-submit')?.disabled, revealDisabled: document.querySelector('#p-reveal')?.disabled,
        answers: [...document.querySelectorAll('#practice-card .option input, #practice-card .text-answer')].map(input => ({ value: input.value, checked: input.checked, disabled: input.disabled })),
        nav: document.querySelector('#practice-nav-v26')?.innerHTML,
      },
    }));
}

async function snapshot(page) {
  const bridge = await page.evaluate(() => typeof window.__webAiPracticeSnapshot === 'function');
  return bridge ? page.evaluate(() => window.__webAiPracticeSnapshot()) : page.evaluate(readSnapshot);
}

function immutable(snapshotValue) {
  const value = JSON.parse(JSON.stringify(snapshotValue));
  const omitAnalysis = question => {
    delete question.analysis;
    const rich = question.richContent;
    if (rich?.fields) {
      delete rich.fields.analysis;
      delete rich.features;
      if (!Object.keys(rich.fields).length && Object.keys(rich).every(key => ['schema', 'fields'].includes(key))) delete question.richContent;
    }
  };
  omitAnalysis(value.question);
  delete value.bank.updatedAt;
  value.bank.questions.filter(question => question.id === value.question.id).forEach(omitAnalysis);
  return value;
}

function assertOnlyAnalysis(before, after, expected) {
  assert.equal(after.question.analysis, expected);
  assert.equal(after.bank.questions.find(question => question.id === after.question.id).analysis, expected);
  const rich = after.question.richContent;
  if (rich?.fields) {
    if (rich.fields.analysis) assert.equal(rich.fields.analysis.text, expected, 'Rich analysis still contains the old explanation.');
    const features = [...new Set(Object.values(rich.fields).flatMap(field => Array.isArray(field) ? field.filter(Boolean).flatMap(entry => entry.features || []) : field?.features || []))].sort();
    assert.deepEqual([...rich.features].sort(), features, 'Rich feature summary was not recomputed from preserved fields.');
  }
  assert.deepEqual(immutable(after), immutable(before), 'Saving AI analysis changed answers, options, stem, chosen/submission, position or progress.');
  before.bank.questions.filter(question => question.id !== before.question.id).forEach(question => {
    assert.deepEqual(after.bank.questions.find(entry => entry.id === question.id), question, 'Another question changed.');
  });
}

function assertPayload(call, marker, absent = []) {
  const payload = JSON.stringify(call.body.messages);
  assert(payload.includes(marker), `Request omitted current question ${marker}.`);
  absent.forEach(value => assert(!payload.includes(value), `Request leaked another question/history: ${value}.`));
}

async function assertKeys(page, keys) {
  const result = await page.evaluate(({ state: stateKey, session, local }) => ({
    session: sessionStorage.getItem(session), local: localStorage.getItem(local), state: localStorage.getItem(stateKey),
  }), keys);
  assert.equal(result.session, FAKE_KEY);
  assert.equal(result.local, null);
  assert(!result.state.includes(FAKE_KEY), 'API key leaked into serialized state.');
  const initial = JSON.parse(result.state);
  assert.equal(initial.banks.length, 1);
  assert.equal(initial.banks[0].id, BANK_ID);
  assert.equal(initial.settings.suppressDefaultBank, true);
}

module.exports = { TEST_ROOT, WEB_ROOT, SEL, within, reportPath, storageKeys, loadPlaywright, startServer, baseUrl, MockAi, bounded, setup, snapshot, assertOnlyAnalysis, assertPayload, assertKeys };
