OCR 离线扩展说明

当前 Web 版 OCR 使用 Tesseract.js 5.1.1。

用途：
  - 图片型 PDF / 扫描 PDF 的兜底识别
  - OCR 文本预览
  - OCR 结果下载为 DOCX
  - OCR 文本填回导入框后继续走 Shiroha Quiz 原有题库解析

推荐语言包：
  - chi_sim：简体中文
  - eng：英文、字母选项和数字

首次完整离线使用前，请运行：

  .\download-ocr.ps1

脚本会下载：
  - tesseract.min.js
  - worker.min.js
  - tesseract.js-core WASM 运行文件
  - chi_sim.traineddata.gz
  - eng.traineddata.gz

注意：
  - OCR 是高级兜底，不会自动替代文字层 PDF 解析。
  - OCR 结果可能有错字、漏字和段落错位，导入题库前必须人工核对。
  - OCR 生成 DOCX 是可编辑文本版，不承诺还原扫描 PDF 的原始排版。
  - 部分浏览器会限制 file:// 页面加载本地 Web Worker；如离线 OCR 加载失败，建议通过本地 HTTP 服务打开 Web 版。
