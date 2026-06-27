离线库下载脚本

以下功能默认使用 CDN 加载，如需离线使用请运行对应脚本：

  download-pdfjs.ps1     → 下载 PDF.js 5.7.284（约 1.7 MB）
  download-mathjax.ps1   → 下载 MathJax 3.2.2（约 24 MB）

不运行脚本也可正常使用——CDN 会自动兜底。

PDF.js 加载顺序：本地 libs/pdf.min.mjs → CDN jsDelivr → CDN unpkg → 内置轻量提取器
MathJax 加载顺序：本地 libs/mathjax/ → CDN jsDelivr → 降级文本显示

注意：当前 PDF 导入只支持文字型 PDF，不支持扫描版/图片版 PDF OCR。
