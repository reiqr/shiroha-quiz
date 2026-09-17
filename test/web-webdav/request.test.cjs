'use strict';

const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

// Execute the real transport and diagnostic functions with a fake fetch, without app startup.
const source = readFileSync(path.resolve(__dirname, '../../apps/web/app.js'), 'utf8');
const start = source.indexOf('function webdavBasicAuthV378(');
const end = source.indexOf('function setWebdavDiagnosticV378(', start);
assert(start >= 0 && end > start, 'WebDAV source boundaries changed; update the test loader.');

function harness(protocol, fetch) {
  const timers = new Set();
  const context = vm.createContext({
    URL, TextEncoder, AbortController, btoa,
    location: { protocol }, fetch,
    setTimeout(callback) { const timer = { callback }; timers.add(timer); return timer; },
    clearTimeout(timer) { timers.delete(timer); },
  });
  vm.runInContext(source.slice(start, end), context);
  return { context, timers };
}

for (const method of ['PROPFIND', 'MKCOL', 'GET', 'PUT']) {
  test(`HTTPS page reaches HTTP WebDAV when fetch permits ${method}`, async () => {
    const response = { status: method === 'PROPFIND' ? 207 : 200 };
    const calls = [];
    const { context, timers } = harness('https:', async (url, options) => {
      calls.push({ url, options });
      return response;
    });
    const actual = await context.webdavRequestV378(
      { endpoint: 'http://dav.example.invalid/root', username: 'test-user' },
      'fake-password', 'backup.json', { method, headers: { Depth: '0' } }
    );
    assert.equal(actual, response);
    assert.equal(calls.length, 1);
    assert.equal(calls[0].url, 'http://dav.example.invalid/root/backup.json');
    assert.equal(calls[0].options.method, method);
    assert.equal(calls[0].options.headers.Authorization, `Basic ${btoa('test-user:fake-password')}`);
    assert.equal(calls[0].options.headers.Depth, '0');
    assert.equal(timers.size, 0);
  });
}

test('failed HTTPS to HTTP request reports mixed content as a possibility', async () => {
  let calls = 0;
  const failure = new TypeError('Failed to fetch');
  const { context, timers } = harness('https:', async () => { calls++; throw failure; });
  await assert.rejects(context.webdavRequestV378({ endpoint: 'http://dav.example.invalid' }, '', ''), error => {
    assert.equal(error.code, 'mixed_content');
    assert.equal(error.cause, failure);
    const diagnostic = context.classifyWebdavErrorV378(error);
    assert(diagnostic.summary.includes('可能'));
    assert(diagnostic.summary.includes('跨域'));
    assert(diagnostic.suggestion.includes('明文'));
    return true;
  });
  assert.equal(calls, 1);
  assert.equal(timers.size, 0);
});

for (const [pageProtocol, endpoint] of [
  ['https:', 'https://dav.example.invalid'],
  ['http:', 'http://dav.example.invalid'],
]) {
  test(`${pageProtocol} page failure to ${endpoint} stays a network diagnosis`, async () => {
    const { context, timers } = harness(pageProtocol, async () => { throw new TypeError('Failed to fetch'); });
    await assert.rejects(context.webdavRequestV378({ endpoint }, '', ''), error => {
      assert.equal(error.code, 'network');
      assert.equal(context.classifyWebdavErrorV378(error).type, '网络 / CORS / TLS');
      return true;
    });
    assert.equal(timers.size, 0);
  });
}

test('HTTP response errors keep their status diagnosis even on HTTPS to HTTP', async () => {
  const { context, timers } = harness('https:', async () => ({ status: 401 }));
  await assert.rejects(context.webdavRequestV378({ endpoint: 'http://dav.example.invalid' }, '', ''), error => {
    assert.equal(error.code, 'http');
    assert.equal(error.status, 401);
    assert.equal(context.classifyWebdavErrorV378(error).type, '鉴权失败');
    return true;
  });
  assert.equal(timers.size, 0);
});

test('timeout abort keeps its diagnosis and releases the timer', async () => {
  const { context, timers } = harness('https:', async (_, options) => new Promise((resolve, reject) => {
    options.signal.addEventListener('abort', () => reject(Object.assign(new Error('Aborted'), { name: 'AbortError' })));
  }));
  const pending = context.webdavRequestV378({ endpoint: 'http://dav.example.invalid' }, '', '');
  assert.equal(timers.size, 1);
  [...timers][0].callback();
  await assert.rejects(pending, error => {
    assert.equal(error.code, 'timeout');
    assert.equal(context.classifyWebdavErrorV378(error).type, '请求超时');
    return true;
  });
  assert.equal(timers.size, 0);
});
