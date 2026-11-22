# 第三方组件与许可声明

Shiroha Quiz 以 **GPL-3.0** 发布。本文件列出项目直接使用的第三方开源组件及其许可；第三方组件仍按各自许可分发，不受本项目 GPL 覆盖。

- 许可原文：[LICENSES/](./LICENSES/)
- 项目源码：[github.com/reiqr/shiroha-quiz](https://github.com/reiqr/shiroha-quiz)

## Android 原生运行时

<details>
<summary>展开：AndroidX / Compose / 网络与后台组件（Apache-2.0）</summary>

| 组件 | 版本 | 许可 | 版权 / 来源 |
| --- | --- | --- | --- |
| `androidx.core:core-ktx` | 1.13.1 | Apache-2.0 | Android Open Source Project |
| `androidx.activity:activity-compose` | 1.9.0 | Apache-2.0 | Android Open Source Project |
| `androidx.core:core-splashscreen` | 1.0.1 | Apache-2.0 | Android Open Source Project |
| Jetpack Compose BOM（`ui` / `ui-tooling-preview` / `foundation` / `material-icons-extended` / `material3`） | 2024.06.00 | Apache-2.0 | Android Open Source Project |
| `com.google.android.material:material` | 1.12.0 | Apache-2.0 | Google LLC |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | Apache-2.0 | Square, Inc. |
| `androidx.work:work-runtime-ktx` | 2.9.1 | Apache-2.0 | Android Open Source Project |
| Kotlin stdlib | 1.9.24 | Apache-2.0 | JetBrains s.r.o. |
| kotlinx.coroutines | 随依赖传递 | Apache-2.0 | JetBrains s.r.o. |

Apache-2.0 完整文本见 [LICENSES/Apache-2.0.txt](./LICENSES/Apache-2.0.txt)。

</details>

<details>
<summary>展开：公式渲染（MIT）</summary>

| 组件 | 版本 | 许可 | 版权 / 来源 |
| --- | --- | --- | --- |
| RaTeX（`io.github.erweixin:ratex-android`） | 0.1.15 | MIT | RaTeX Contributors，https://github.com/erweixin/RaTeX |
| KaTeX 字体（随 RaTeX 内置） | - | MIT | Khan Academy and other contributors，https://katex.org |
| KaTeX（仅 Android KaTeX 对照包） | 0.19.1 | MIT | Khan Academy and other contributors，[KaTeX v0.19.1](https://github.com/KaTeX/KaTeX/releases/tag/v0.19.1) |

MIT 完整文本见 [LICENSES/MIT.txt](./LICENSES/MIT.txt)。KaTeX 字体随 KaTeX 项目分发；如上游对字体另有许可声明，以该声明为准。

</details>

## Web 运行时

<details>
<summary>展开：MathJax / PDF.js / Tesseract.js（Apache-2.0）</summary>

| 组件 | 版本 | 许可 | 版权 / 来源 |
| --- | --- | --- | --- |
| MathJax | 3.2.2 | Apache-2.0 | The MathJax Consortium，https://www.mathjax.org |
| PDF.js（`pdfjs-dist`） | 5.7.284 | Apache-2.0 | Mozilla Foundation，https://mozilla.github.io/pdf.js |
| Tesseract.js / tesseract.js-core | 5.1.1 | Apache-2.0 | Tesseract.js contributors，https://tesseract.projectnaptha.com |

Apache-2.0 完整文本见 [LICENSES/Apache-2.0.txt](./LICENSES/Apache-2.0.txt)。

</details>

<details>
<summary>展开：PDF.js 增强资源许可</summary>

这些资源由 `libs/download-pdfjs.ps1` 按需下载，默认不包含在 Web ZIP 中；下载包内附有对应许可。

| 资源 | 许可 | 许可文件 |
| --- | --- | --- |
| PDF.js CMaps | Adobe BSD-style | `apps/web/libs/cmaps/LICENSE` |
| JBIG2 解码器 | 见资源包 | `apps/web/libs/wasm/LICENSE_JBIG2`、`LICENSE_PDFJS_JBIG2` |
| OpenJPEG 解码器 | 见资源包 | `apps/web/libs/wasm/LICENSE_OPENJPEG`、`LICENSE_PDFJS_OPENJPEG` |
| QCMS 色彩管理 | 见资源包 | `apps/web/libs/wasm/LICENSE_QCMS`、`LICENSE_PDFJS_QCMS` |

</details>

## 测试与构建依赖

<details>
<summary>展开：测试与构建工具</summary>

以下依赖不进入正式 APK / Web ZIP，列在此处便于源码审计。

| 组件 | 版本 | 许可 | 用途 |
| --- | --- | --- | --- |
| JUnit | 4.13.2 | EPL-1.0 | JVM 单元测试 |
| Robolectric | 4.12.2 | MIT | Android 单元测试 |
| kxml2 | 2.3.0 | BSD-style | XML 测试支持 |
| kotlinx-coroutines-test | 1.7.1 | Apache-2.0 | 协程测试 |
| Android Gradle Plugin | 8.5.2 | Apache-2.0 | 构建 |
| Gradle | 8.7 | Apache-2.0 | 构建 |
| Node.js（Node 内置测试运行器） | 运行环境 | MIT | Web 单元测试 |

EPL-1.0、BSD-style 的完整文本请以各上游发布包为准；Apache-2.0 文本见 [LICENSES/Apache-2.0.txt](./LICENSES/Apache-2.0.txt)。

</details>

## 特别说明

### OkHttp / Public Suffix List

OkHttp 包含由 Public Suffix List 编译的数据，其 NOTICE 原文如下：

```text
Note that publicsuffixes.gz is compiled from The Public Suffix List:
https://publicsuffix.org/list/public_suffix_list.dat

It is subject to the terms of the Mozilla Public License, v. 2.0:
https://mozilla.org/MPL/2.0/
```

MPL-2.0 完整文本见 [LICENSES/MPL-2.0.txt](./LICENSES/MPL-2.0.txt)。

### 说明

- 本文件只列直接依赖；传递依赖的许可随对应组件发布包提供。
- 若本文件与组件自带许可文本存在冲突，以组件自带的许可文本为准。
- 本项目不修改上述第三方组件的许可条款；对组件的使用、复制和分发仍受各自许可约束。

## 致谢

感谢 AndroidX、Jetpack Compose、Material Components、OkHttp、WorkManager、Kotlin、RaTeX、KaTeX、MathJax、PDF.js、Tesseract.js 及其贡献者，也感谢所有提交 issue、PR 和反馈的社区成员。
