PDF.js 混合加载说明

本版本的 PDF 导入顺序：
1. 优先加载本地文件：libs/pdf.min.mjs 和 libs/pdf.worker.min.mjs
2. 如果本地文件不存在，联网加载 CDN：jsDelivr / unpkg 上的 pdfjs-dist@5.7.284
3. 如果 CDN 也不可用，降级使用内置轻量 PDF 文本提取器

如果需要真正离线的 PDF.js 最小本地版，请把以下两个文件放到本 libs 目录：
- pdf.min.mjs
- pdf.worker.min.mjs

推荐下载地址：
https://cdn.jsdelivr.net/npm/pdfjs-dist@5.7.284/build/pdf.min.mjs
https://cdn.jsdelivr.net/npm/pdfjs-dist@5.7.284/build/pdf.worker.min.mjs

注意：当前版本只支持文字型 PDF，不支持扫描版/图片版 PDF OCR。

MathJax 本地加载说明

公式渲染顺序：
1. 优先加载本地文件：libs/mathjax/tex-mml-chtml.js
2. 如果本地文件不存在，联网加载 CDN：jsDelivr 上的 mathjax@3
3. 如果本地与 CDN 都不可用，公式会降级为基础文本显示

本地 MathJax 来自官方 npm 包：
- mathjax@3.2.2

可选离线脚本：download-mathjax.ps1（从 CDN 拉取完整 MathJax 组件）
不运行脚本也可正常使用——CDN 会自动兜底。

为保证离线公式渲染和字体加载稳定，mathjax/ 目录保留完整 es5 组件，不只保留入口 js。
