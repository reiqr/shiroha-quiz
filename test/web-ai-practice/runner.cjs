'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const http = require('node:http');
const path = require('node:path');
const { analysis, MOCK_ENDPOINT, FAKE_KEY, MODEL } = require('./fixtures.cjs');
const { cases } = require('./cases.cjs');
const { TEST_ROOT, WEB_ROOT, within, reportPath, storageKeys, loadPlaywright, startServer, baseUrl, setup, bounded, assertKeys } = require('./harness.cjs');

function argumentsFrom(argv) {
  const options = { timeout: 10000, headed: false, selfTest: false, list: false, filter: '' };
  for (let index = 0; index < argv.length; index++) {
    const token = argv[index];
    if (token === '--headed') options.headed = true;
    else if (token === '--self-test') options.selfTest = true;
    else if (token === '--list') options.list = true;
    else if (['--base-url', '--timeout', '--filter', '--report', '--executable-path'].includes(token)) {
      const value = argv[++index];
      assert(value && !value.startsWith('--'), `Missing value for ${token}.`);
      if (token === '--base-url') options.url = baseUrl(value);
      if (token === '--timeout') options.timeout = Number(value);
      if (token === '--filter') options.filter = value;
      if (token === '--report') options.report = reportPath(value);
      if (token === '--executable-path') { assert(path.isAbsolute(value), 'Browser executable path must be absolute.'); options.executablePath = value; }
    } else throw new Error(`Unknown argument ${token}. Use --list, --self-test, --base-url, --timeout, --filter, --report, --executable-path or --headed.`);
  }
  assert(Number.isInteger(options.timeout) && options.timeout >= 1000 && options.timeout <= 60000, '--timeout must be 1000..60000 milliseconds.');
  return options;
}

function rawGet(url, requestPath) {
  return new Promise((resolve, reject) => {
    const request = http.request(new URL(url), { path: requestPath, method: 'GET' }, response => {
      let body = '';
      response.setEncoding('utf8');
      response.on('data', chunk => { body += chunk; });
      response.on('end', () => resolve({ status: response.statusCode, body }));
    });
    request.on('error', reject);
    request.end();
  });
}

async function frameworkSelfTest(browser, url, keys, timeout, ownedServer) {
  const h = await setup(browser, url, keys, { timeout });
  try {
    assert.equal(within(TEST_ROOT, path.resolve(TEST_ROOT, '..', 'escape.json')), false);
    assert.throws(() => reportPath('../escape.json'));
    assert.throws(() => baseUrl('https://mock-ai.invalid/'));
    if (ownedServer) {
      for (const input of ['/../app.js', '/%2e%2e/app.js', '/%2e%2e%5capp.js', '/C:/Windows/win.ini', '/%00', '//../app.js']) {
        assert.equal((await rawGet(url, input)).status, 403, `Traversal was not rejected: ${input}`);
      }
      assert.equal((await rawGet(url, '/%zz')).status, 400);
      const bank = await rawGet(url, '/question-bank.js');
      assert.equal(bank.status, 200);
      assert(bank.body.includes('suppressed') && bank.body.length < 100);
    }
    // Exercise real browser fetch and routing without loading any application/bank data.
    await h.context.route(url, route => route.fulfill({ contentType: 'text/html', body: '<!doctype html><html><body><button id="start-practice-btn">Harness self-test</button></body></html>' }));
    await h.boot();
    await assertKeys(h.page, keys);
    const payload = { model: MODEL, stream: false, messages: [{ role: 'user', content: 'Synthetic self-test.' }], response_format: { type: 'json_object' } };
    async function call(body = payload) {
      return h.page.evaluate(async ({ endpoint, body: request, key }) => {
        const response = await fetch(endpoint, { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}` }, body: JSON.stringify(request) });
        return { status: response.status, payload: await response.json() };
      }, { endpoint: MOCK_ENDPOINT, body, key: FAKE_KEY });
    }
    const initial = h.mock.enqueue('analysis', analysis('SELF_TEST_ANALYSIS'));
    const result = await call();
    await h.waitCall(initial);
    assert.equal(JSON.parse(result.payload.choices[0].message.content).analysis, 'SELF_TEST_ANALYSIS');
    const follow = h.mock.enqueue('follow', 'SELF_TEST_PLAIN_TEXT');
    const plainBody = { ...payload };
    delete plainBody.response_format;
    const plain = await call(plainBody);
    await h.waitCall(follow);
    assert.equal(plain.payload.choices[0].message.content, 'SELF_TEST_PLAIN_TEXT');
    const held = h.mock.enqueue('analysis', analysis('SELF_TEST_DELAY'), { hold: true });
    let settled = false;
    const delayed = call().then(value => { settled = true; return value; });
    await h.waitCall(held);
    assert.equal(settled, false);
    held.release();
    assert.equal((await delayed).status, 200);
    const failure = h.mock.enqueue('analysis', '', { status: 503 });
    assert.equal((await call()).status, 503);
    await h.waitCall(failure);
    for (const target of ['https://blocked-network.invalid/v1/chat/completions', new URL('unexpected-fetch', url).href]) {
      assert.equal(await h.page.evaluate(async target => { try { await fetch(target); return false; } catch { return true; } }, target), true);
    }
    assert.equal(h.blocked.length, 2);
    assert.equal(h.mock.violations.length, 0);
    assert.equal(h.mock.queue.length, 0);
    assert.equal(h.pageErrors.length, 0);
    return { mockRequests: h.mock.calls.length, blockedRequests: h.blocked.length, safeRoot: WEB_ROOT, isolatedContext: true, traversalChecked: Boolean(ownedServer) };
  } finally { await h.cleanup(); }
}

async function main() {
  const events = [];
  const emit = event => { events.push(event); process.stdout.write(`${JSON.stringify(event)}\n`); };
  let server;
  let browser;
  let options;
  let exitCode = 2;
  const started = Date.now();
  try {
    options = argumentsFrom(process.argv.slice(2));
    const selected = cases.filter(test => test.name.includes(options.filter));
    assert(selected.length || options.selfTest, `No tests match ${options.filter}.`);
    if (options.list) { selected.forEach(test => emit({ type: 'test', name: test.name })); exitCode = 0; return; }
    const keys = storageKeys();
    const playwright = loadPlaywright();
    server = options.url ? null : await startServer();
    const url = options.url || server.url;
    emit({ type: 'start', mode: options.selfTest ? 'framework-self-test' : 'web-ai-practice', baseUrl: url, webRoot: WEB_ROOT, tests: options.selfTest ? 1 : selected.length, storageKeys: keys, timeoutMs: options.timeout, realApiAllowed: false });
    const executablePath = options.executablePath || playwright.chromium.executablePath();
    try { await fs.access(executablePath); }
    catch (cause) { throw new Error(`Browser executable unavailable: ${executablePath}. Supply --executable-path for an installed Chromium/Edge binary. No download or framework fallback was attempted.`, { cause }); }
    emit({ type: 'browser', executablePath, isolatedContext: true });
    browser = await playwright.chromium.launch({ executablePath, headless: !options.headed, args: ['--disable-background-networking', '--disable-component-update'] });
    if (options.selfTest) {
      const result = await frameworkSelfTest(browser, url, keys, options.timeout, server);
      emit({ type: 'case', name: 'framework-self-test', status: 'passed', ...result });
      exitCode = 0;
      emit({ type: 'summary', passed: 1, failed: 0, exitCode, durationMs: Date.now() - started });
      return;
    }
    let passed = 0;
    let failed = 0;
    for (const test of selected) {
      const beginning = Date.now();
      let h;
      let error;
      let diagnostics = {};
      try {
        h = await setup(browser, url, keys, { ...test.options, timeout: options.timeout });
        await h.boot();
        await assertKeys(h.page, keys);
        await bounded(test.run(h), Math.max(options.timeout * 8, 45000), `Case timeout: ${test.name}`);
        assert.equal(h.mock.violations.length, 0, h.mock.violations.join(' '));
        assert.equal(h.mock.queue.length, 0, 'Expected mock responses were not requested.');
        assert.equal(h.pageErrors.length, 0, h.pageErrors.join(' '));
        const suspicious = h.blocked.filter(request => ['fetch', 'xhr', 'websocket'].includes(request.type) || !['GET', 'HEAD'].includes(request.method));
        assert.equal(suspicious.length, 0, `Blocked unexpected API request: ${JSON.stringify(suspicious)}`);
      } catch (cause) { error = cause; }
      finally {
        if (h) {
          diagnostics = { mockRequests: h.mock.calls.length, blockedRequests: h.blocked, pageErrors: h.pageErrors, mockViolations: h.mock.violations, readOnlyIifeBridge: h.bridgeUsed };
          try { await h.cleanup(); } catch (cause) { error ||= cause; }
        }
      }
      if (error) failed++; else passed++;
      emit({ type: 'case', name: test.name, status: error ? 'failed' : 'passed', durationMs: Date.now() - beginning, ...diagnostics, ...(error ? { error: error.message, stack: error.stack } : {}) });
    }
    exitCode = failed ? 1 : 0;
    emit({ type: 'summary', passed, failed, exitCode, durationMs: Date.now() - started });
  } catch (error) {
    emit({ type: 'fatal', status: 'error', exitCode: 2, error: error.message, stack: error.stack });
  } finally {
    try { if (browser) await browser.close(); if (server) await server.close(); }
    catch (error) { exitCode = 2; emit({ type: 'fatal', exitCode, error: error.message }); }
    if (options?.report) {
      try {
        const target = reportPath(options.report);
        await fs.mkdir(path.dirname(target), { recursive: true });
        await fs.writeFile(target, `${events.map(event => JSON.stringify(event)).join('\n')}\n`, { encoding: 'utf8', flag: 'w' });
      } catch (error) { exitCode = 2; process.stderr.write(`Report write failed: ${error.message}\n`); }
    }
    process.exitCode = exitCode;
  }
}

main().catch(error => { process.stderr.write(`${error.stack}\n`); process.exitCode = 2; });
