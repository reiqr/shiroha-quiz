const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const http = require('node:http');

const source = fs.readFileSync(path.join(__dirname, '..', 'app.js'), 'utf8');
const testSource = source.replace(/\ninit\(\);\s*\n\}\)\(\);/,
  '\nglobalThis.testApi={normalizeAiEndpointV99,aiAddressSpaceV140,aiFetchNetworkOptionsV140,requestAiChatV99,buildAiDiagnosticV375};\n})();');
assert.notEqual(testSource, source);
let captured;
const context = {
  localStorage: { getItem: () => null }, sessionStorage: { getItem: () => null },
  document: { getElementById: () => ({}) }, window: {}, console, URL, Blob, TextDecoder, btoa,
  crypto: require('node:crypto').webcrypto, setTimeout, clearTimeout,
  location: { origin: 'https://reiqr.github.io', href: 'https://reiqr.github.io/shiroha-quiz' },
  fetch: (url, options) => { captured = options; return fetch(url, options); },
};
vm.createContext(context);
vm.runInContext(testSource, context, { filename: 'app.js' });
const api = context.testApi;
for (const host of ['localhost', '127.0.0.2', '[::1]']) assert.equal(api.aiAddressSpaceV140(`http://${host}`), 'loopback');
for (const host of ['10.0.0.2', '172.16.0.1', '172.31.255.254', '192.168.1.2', '[fd00::1]', '[fe80::1]', '[::ffff:c0a8:102]', 'model.local']) {
  assert.equal(api.aiAddressSpaceV140(`http://${host}`), 'local', host);
}
for (const host of ['0.0.0.0', '8.8.8.8', '172.32.0.1', '[::]', '[ff02::1]', '[2001:4860::1]']) {
  assert.equal(api.aiAddressSpaceV140(`http://${host}`, 'local'), null, host);
  assert.throws(() => api.normalizeAiEndpointV99(`http://${host}`, 'local'));
}
assert.equal(api.aiAddressSpaceV140('http://model.internal', 'local'), 'local');
assert.equal(api.aiAddressSpaceV140('http://model.internal', 'custom'), null);
assert.equal(api.normalizeAiEndpointV99('http://192.168.1.2:11434', 'ollama'), 'http://192.168.1.2:11434/v1/chat/completions');
assert.equal(api.normalizeAiEndpointV99('https://api.example.com/v1', 'custom'), 'https://api.example.com/v1/chat/completions');
assert.equal(api.aiFetchNetworkOptionsV140({ endpoint: 'https://api.example.com/v1' }).targetAddressSpace, undefined);
const diagnostic = api.buildAiDiagnosticV375({ network: true }, { endpoint: 'http://model.internal', provider: 'local' });
assert.equal(diagnostic.local, true);
assert.ok(diagnostic.steps.some(step => step.includes('本地网络访问')));

(async () => {
  let authorization;
  const server = http.createServer((req, res) => {
    authorization = req.headers.authorization;
    req.resume();
    req.on('end', () => { res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify({ choices: [{ message: { content: 'local-ok' } }] })); });
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  try {
    const config = { endpoint: `http://127.0.0.1:${server.address().port}/v1/chat/completions`, model: 'fake-local', provider: 'ollama', apiKey: '' };
    const result = await api.requestAiChatV99(config, [{ role: 'user', content: 'test' }], { plainText: true });
    assert.equal(result.content, 'local-ok');
    assert.equal(captured.targetAddressSpace, 'loopback');
    assert.equal(captured.redirect, 'error');
    assert.equal(authorization, undefined);
    await api.requestAiChatV99({ ...config, apiKey: 'test-key' }, [], { plainText: true });
    assert.equal(authorization, 'Bearer test-key');
  } finally { await new Promise(resolve => server.close(resolve)); }
  console.log('AI local-network policy and local HTTP round-trip passed');
})().catch(error => { console.error(error); process.exitCode = 1; });
