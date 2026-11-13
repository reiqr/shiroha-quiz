const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const appSource = fs.readFileSync(path.join(__dirname, '..', 'app.js'), 'utf8');
const testSource = appSource.replace(
  /\ninit\(\);\s*\n\}\)\(\);/,
  '\nglobalThis.xlsxTestApi = { extractXlsxSheetsV49, tableSheetsToQuestionsV49 };\n})();'
);
assert.notEqual(testSource, appSource, 'Could not expose the current Web import functions');
const context = {
  localStorage: { getItem: () => null },
  console,
  document: {},
  window: {},
  crypto: require('node:crypto').webcrypto,
  TextDecoder,
  Blob,
  URL,
  btoa,
};
vm.createContext(context);
vm.runInContext(testSource, context, { filename: 'app.js' });

function makeStoredZip(parts) {
  const local = [];
  const central = [];
  let offset = 0;
  for (const [name, value] of Object.entries(parts)) {
    const fileName = Buffer.from(name);
    const data = Buffer.isBuffer(value) ? value : Buffer.from(value);
    const header = Buffer.alloc(30);
    header.writeUInt32LE(0x04034b50, 0);
    header.writeUInt16LE(20, 4);
    header.writeUInt32LE(data.length, 18);
    header.writeUInt32LE(data.length, 22);
    header.writeUInt16LE(fileName.length, 26);
    local.push(header, fileName, data);
    const directory = Buffer.alloc(46);
    directory.writeUInt32LE(0x02014b50, 0);
    directory.writeUInt16LE(20, 4);
    directory.writeUInt16LE(20, 6);
    directory.writeUInt32LE(data.length, 20);
    directory.writeUInt32LE(data.length, 24);
    directory.writeUInt16LE(fileName.length, 28);
    directory.writeUInt32LE(offset, 42);
    central.push(directory, fileName);
    offset += header.length + fileName.length + data.length;
  }
  const directory = Buffer.concat(central);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(Object.keys(parts).length, 8);
  end.writeUInt16LE(Object.keys(parts).length, 10);
  end.writeUInt32LE(directory.length, 12);
  end.writeUInt32LE(offset, 16);
  return Buffer.concat([...local, directory, end]);
}

async function main() {
  const sheet = `<worksheet xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheetData>
    <row r="1"><c r="A1" t="inlineStr"><is><t>题型</t></is></c><c r="B1" t="inlineStr"><is><t>题干</t></is></c><c r="C1" t="inlineStr"><is><t>答案</t></is></c></row>
    <row r="3"><c r="A3" t="inlineStr"><is><t>判断题</t></is></c><c r="B3" t="inlineStr"><is><t>有图题</t></is></c><c r="C3" t="inlineStr"><is><t>正确</t></is></c></row>
    <row r="4"><c r="A4" t="inlineStr"><is><t>判断题</t></is></c><c r="B4" t="inlineStr"><is><t>无图题</t></is></c><c r="C4" t="inlineStr"><is><t>错误</t></is></c></row>
    </sheetData><drawing r:id="rId1"/></worksheet>`;
  const workbook = '<workbook><sheets><sheet name="题库" r:id="rId1"/></sheets></workbook>';
  const workbookRels = '<Relationships><Relationship Id="rId1" Target="worksheets/sheet1.xml"/></Relationships>';
  const sheetRels = '<Relationships><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/></Relationships>';
  const drawing = `<xdr:wsDr><xdr:oneCellAnchor><xdr:from><xdr:row>0</xdr:row></xdr:from><a:blip r:embed="rId1"/></xdr:oneCellAnchor>
    <xdr:oneCellAnchor><xdr:from><xdr:row>2</xdr:row></xdr:from><a:blip r:embed="rId2"/></xdr:oneCellAnchor></xdr:wsDr>`;
  const drawingRels = '<Relationships><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/header.png"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/question.png"/></Relationships>';
  const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL/nwAAAABJRU5ErkJggg==', 'base64');
  const parts = {
    'xl/workbook.xml': workbook,
    'xl/_rels/workbook.xml.rels': workbookRels,
    'xl/worksheets/sheet1.xml': sheet,
    'xl/worksheets/_rels/sheet1.xml.rels': sheetRels,
    'xl/drawings/drawing1.xml': drawing,
    'xl/drawings/_rels/drawing1.xml.rels': drawingRels,
    'xl/media/header.png': png,
    'xl/media/question.png': png,
  };
  const zip = makeStoredZip(parts);
  const file = { arrayBuffer: async () => zip.buffer.slice(zip.byteOffset, zip.byteOffset + zip.byteLength) };
  const sheets = await context.xlsxTestApi.extractXlsxSheetsV49(file);
  assert.equal(sheets.length, 1);
  assert.equal(sheets[0].rows[1].sourceRow, 3);
  const result = context.xlsxTestApi.tableSheetsToQuestionsV49(sheets, 'sample.xlsx');
  assert.equal(result.questions.length, 2);
  assert.equal(result.questions[0].images.length, 1);
  assert.equal(result.questions[1].images.length, 0);
  assert.ok(result.warnings.some(warning => warning.includes('第 1 行')));
  const broken = makeStoredZip({ ...parts, 'xl/media/question.png': Buffer.from('broken image') });
  const brokenFile = { arrayBuffer: async () => broken.buffer.slice(broken.byteOffset, broken.byteOffset + broken.byteLength) };
  const brokenSheets = await context.xlsxTestApi.extractXlsxSheetsV49(brokenFile);
  const brokenResult = context.xlsxTestApi.tableSheetsToQuestionsV49(brokenSheets, 'broken.xlsx');
  assert.equal(brokenResult.questions.length, 2);
  assert.equal(brokenResult.questions[0].images.length, 0);
  assert.ok(brokenResult.warnings.some(warning => warning.includes('图片格式或内容损坏')));
  console.log('XLSX image row binding passed');
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
