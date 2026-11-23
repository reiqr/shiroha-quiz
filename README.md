<div align="center">

<img src="assets/promo4.jpg" width="800" alt="Shiroha Quiz">
</div>

<br>

# Shiroha Quiz

Shiroha Quiz 是一个轻量、开源的刷题工具，支持自导入题库、练习、考试与多端使用。

> 你手里有 Word、Excel、TXT、JSON，或者题目和答案分开的文件。Shiroha Quiz 尝试把这些内容自动识别成可练习、可考试、可复盘的个人题库。

当前主要有两条公开使用线：

- **Web 版**：适合电脑整理题库，支持导入、练习、考试、错题、备份、AI 与 OCR。
- **Android 原生版**：当前主推，Kotlin + Compose 原生实现，支持 AI、多空填空、背题/斩题、图片题和智能复习。

<img src="assets/shiroha_quiz_ui_assets_v2_cutout/illus_me_settings.webp" width="150" align="right" alt="看板娘" />

> [!IMPORTANT]
> **发布状态（2026-11-23 核对）**：目前公开下载的是 [`v2.8.13-beta`](https://github.com/reiqr/shiroha-quiz/releases/tag/v2.8.13-beta)，内含 Android 原生 `v0.9.17`（RaTeX 标准版和纯文本 Lite）与 Web `v0.8.10-alpha`。主分支已通过 [PR #147](https://github.com/reiqr/shiroha-quiz/pull/147) 将原生默认引擎切换为离线 KaTeX，**还没有发布对应 APK**。

<br clear="right">

---

## 快速开始

| 你的情况 | 下载什么 | 入口 |
| --- | --- | --- |
| Android 手机 | 当前下载包提供标准版（ARM64，含公式渲染）和 Lite（纯文本，不依赖 RaTeX）；Lite 文件名带 `-lite-native-release.apk` | [下载](https://github.com/reiqr/shiroha-quiz/releases/latest) |
| Windows 电脑 | Web ZIP，解压后双击 `打开Shiroha Quiz.cmd` | [下载](https://github.com/reiqr/shiroha-quiz/releases/latest) |
| 先体验一下 | 在线版，免安装 | [打开在线版](https://reiqr.github.io/shiroha-quiz) |

> 原生版和 Web 版使用独立版本号。在线 Web 版由 `main` 的 Web 源码部署，下载版以 [Release 附件](https://github.com/reiqr/shiroha-quiz/releases/latest)为准。

> **当前仍处于测试阶段**，不建议用于高风险正式考试场景。**重要题库请定期备份。**

---

## 原生版与 Web 版区别

| 维度 | Android 原生版 | Web 版 |
| --- | --- | --- |
| 使用方式 | 安装 APK，Android 8.0+；当前 RaTeX 标准下载包要求 ARM64 | 在线使用，或解压后双击启动器 |
| 数据存储 | 本地 SharedPreferences | 浏览器 LocalStorage |
| 离线能力 | 导入、刷题和本地备份可离线；在线 AI、MinerU 和 WebDAV 需要连接服务 | 基础功能可离线；PDF、公式和 OCR 扩展库可按需下载 |
| 文档识别 | MinerU 在线 OCR、DOCX 图片提取 | PDF.js 文字层解析、OCR 测试区 |
| 特色 | 背题、斩题、智能复习、暗夜模式、平板适配 | 桌面题库清洗、OCR、AI 导入与单题解析 |
| 适合 | 手机日常刷题 | 电脑整理题库与快速使用 |

<details>
<summary><strong>展开查看两端详细差异</strong></summary>

<br>

### 刷题与考试

**双端都有**

- 支持随机练习、顺序练习、考试、错题复习、收藏和学习记录。
- 支持单选、多选、判断、填空（含多空）、简答和逐空作答的完形填空。
- 支持题库分组、顺序练习进度、随机练习优先未做题，以及独立设置练习和考试的选项打乱。
- 支持题目快速编辑、考试倒计时、答题卡和结果统计；学习记录可逐题复盘。

**Android 原生版额外支持（Web 当前没有）**

- **批量练习**：除即时判题外，也可整组提交。
- **背题模式**：直接查看答案与解析，不计入普通练习记录。
- **斩题**：把已经掌握的题移出普通练习池，并可随时恢复。
- **智能复习**：根据错题状态安排到期复习。
- **字号与紧凑模式**：题干、选项字号可调，适合长题阅读。
- **错题作用域**：可在当前题库 / 全部题库之间切换，并按掌握状态筛选。
- **考试细化设置**：可按题型设置题量、分值和时长，并支持滑动切题。

**Web 版额外 / 实现不同**

- 可在设置中开启单选、判断选择后立即判题，以及答对后自动下一题；也可分别记住练习和考试设置。
- 考试结果可按题型汇总得分。
- 更偏桌面端快速刷题与题库整理，不提供背题、斩题和智能复习。

### 题库导入与文档处理

**双端都有**

- 支持 `docx`、`xlsx` / `csv`、`txt`、`json` 和粘贴文本导入。
- 支持“题目文件 + 答案文件”双文件导入。
- 自动识别题号、题干、选项、答案、解析、题型和分区。
- 支持导入预览、异常提示、手动修正和全文查找 / 替换。
- 支持普通 JSON、跨端备份和图片题数据互通。
- 支持提取 DOCX 内嵌图片，以及按标准 XLSX 表格的题目行尝试关联图片；归属不明的图片需人工核对。
- 对材料题、共用题干和集中答案区提供兼容解析；复杂排版请先预览和核对。

**Android 原生版额外支持**

- **MinerU 在线 OCR**：支持扫描 PDF 识别和精准模式图片绑定。题目与答案分成两份 PDF 时，可分别识别，再送入双文件导入。

**Web 版额外支持**

- **PDF.js** 直接解析文字层 PDF。
- **OCR 测试区**：扫描 PDF 可转为文本或 DOCX 后再人工核对导入。
- 更适合在电脑上先做全文清洗，再进入识别预览。

### AI 功能

**双端都有**

- AI 导入核对、补解析和结构化结果优先。
- 练习页支持单题 AI 解析与连续追问。
- 确认后的 AI 解析可以保存回题库。
- 支持 DeepSeek、OpenAI 兼容接口和自定义接口。
- 可连接 **Ollama / LM Studio** 等本机或内网 OpenAI 兼容服务。

> AI 只作辅助。使用外部 AI 服务时，题目文本会发送给你配置的服务商；API Key 不写入题库备份。

**Android 原生版额外支持（Web 当前没有）**

- **无答案题 AI 临时参考答案**：默认关闭，只用于本次判题，不写回题库，也不进入错题本。
- API Key 使用 Android Keystore 加密保存。

**Web 版额外支持**

- AI 可用于**整理原始题库文本**后再进入导入流程。
- 单题追问可产出可独立保存的完整解析草稿。

### 本地 / 内网 AI 接入

- 原生版支持回环和内网 HTTP 模型，HTTP 模型未启用鉴权时可留空 API Key；公网接口仍使用 HTTPS 并填写 Key。Web 可选择 Ollama、LM Studio 或“内网本地模型”（用于内网域名）。
- 例如填写 `http://192.168.1.10:11434/v1`（Web 也可填写完整 `/v1/chat/completions` 地址）。手机上的 `127.0.0.1` 指向手机本身；连接电脑模型请填写电脑内网 IP，允许服务监听内网，并检查防火墙。
- HTTP 明文传输题目和密钥，仅用于可信网络。原生端会校验实际解析出的地址并拒绝公网 HTTP、代理绕过和自动跳转；MinerU / WebDAV 仍要求 HTTPS。
- Web 仍受浏览器本地网络权限、CORS 和混合内容规则限制。允许当前网页 Origin 的跨域请求及浏览器本地网络访问；浏览器不支持 HTTPS 网页访问 HTTP 内网时，请用项目的本地启动脚本或为模型配置可信 HTTPS。内网域名选项是访问意图声明，浏览器不支持地址空间约束时不能替代 DNS 安全校验。

### 备份与跨端

**双端都有**

- 支持本地完整备份与恢复。
- Web 与原生版题库格式互通。
- 图片题可通过 ZIP 保留素材并跨端导入。
- 支持 WebDAV 云备份与远程恢复。

**Android 原生版额外支持**

- 可选每天 / 每周自动 WebDAV 备份，并设置网络、充电和旧备份保留条件。

**Web 版实现特点**

- 支持 WebDAV 远程列表、SHA-256 校验，以及合并 / 覆盖恢复。

### 界面与平台能力

**Android 原生版**

- Kotlin + Jetpack Compose + Material3 原生界面。
- 支持浅色 / 暗夜模式、Shiroha 模式、平板侧边导航和系统返回键。
- 主分支用 KaTeX 离线排版 LaTeX，失败时保留可读文本；RaTeX 源码保留用于开发对照。当前公开 APK 仍用 RaTeX，详见上方发布状态。
- 主要面向手机和平板日常刷题。

**Web 版**

- HTML + CSS + JavaScript，浏览器直接运行。
- 支持本地启动器和在线版，主要面向电脑端题库整理与快速使用。

> 两端题库与备份格式持续保持互通；具体功能变化以 [CHANGELOG](./CHANGELOG.md) 和 [Releases](https://github.com/reiqr/shiroha-quiz/releases) 为准。

</details>

---

## 使用说明

### Web 版

1. 从 [Releases](https://github.com/reiqr/shiroha-quiz/releases/latest) 下载 Web ZIP 并解压。
2. 双击 `打开Shiroha Quiz.cmd`，脚本会启动本地服务并打开浏览器，无需预装 Python。
3. 在 **导入题库** 中粘贴文本或上传文件。
4. 在 **识别预览** 中核对题型、答案和解析后导入。
5. 进入 **练习** 或 **考试** 开始使用。

> 不建议直接双击 `index.html`。PDF/OCR 依赖 Web Worker 和 ES module，`file://` 下部分能力会失效，而且数据来源与启动器模式不同。

### Android 原生版

1. 安装 `*-native-release.apk`。
2. 在 **导入** 页面选择文件或粘贴文本。
3. 在 **核对页** 检查识别结果，必要时手动修正或使用 AI 辅助。
4. 选择 **练习、考试、背题** 等模式开始使用。
5. 错题、收藏、学习记录和智能复习会保存在本机。

### 数据备份建议

- **重要题库导入后**及时导出备份。
- **换设备、清缓存、卸载 App 前**先备份。
- Web 版建议固定使用同一种访问方式，在线版、本地服务和 `file://` 的数据互不相通。
- Web JSON 与原生备份可跨端导入；含图片题建议使用 ZIP。
- WebDAV 使用方式见 [WebDAV 云备份使用指南](docs/WebDAV云备份使用指南/Shiroha%20Quiz%20WebDAV%20云备份使用指南.md)。

---

## 导入格式与策略

推荐优先使用结构清晰的 `docx`、`xlsx` / `csv`、`txt` 或 JSON。旧版 `.doc`、`.xls`、`.xlsm` 建议先另存为新版格式。

基本流程：

```text
上传 / 粘贴
→ 自动解析
→ 识别预览
→ 人工核对
→ 导入题库
```

详细文档：

- [题库导入格式支持说明](docs/题库导入格式支持说明/Shiroha_Quiz_题库导入格式支持说明.md)
- [题库导入策略与使用指南](docs/题库导入策略与使用指南/Shiroha%20Quiz%20题库导入策略与使用指南.md)
- [题目导入解析方法说明](docs/题目导入解析方法说明/Shiroha%20Quiz%20题目导入解析方法说明.md)
- [标准题库格式示例](docs/标准题库格式示例/标准题库格式示例.md)
- [Excel 通用模板](docs/标准题库格式示例/Shiroha_Quiz_Excel题库导入通用模板.xlsx)

如果原始题库很乱，也可以先用 AI 做格式清洗，但建议只统一题号、选项、答案和解析格式，不修改题意、不编造答案。

---

## 下载与更新

- [最新 Release](https://github.com/reiqr/shiroha-quiz/releases/latest)
- [全部 Releases](https://github.com/reiqr/shiroha-quiz/releases)
- [在线体验](https://reiqr.github.io/shiroha-quiz)
- [CHANGELOG](./CHANGELOG.md)

新发布的 Android 原生包计划只发 KaTeX 版。现有 Release 中的 RaTeX 和 Lite 仍可下载，是否已有新 APK 请以 Release 附件为准；未发布的变化单独列在 [CHANGELOG](./CHANGELOG.md)。

Web ZIP 不捆绑完整 PDF.js、MathJax 和 Tesseract 离线扩展，可按 [离线资源下载速查](./apps/web/离线资源下载速查.txt) 补齐；联网时可使用 CDN 兜底。

---

> 以下为开发者内容。

## 仓库结构

```text
shiroha-quiz/
├── apps/
│   ├── web/                        # Web 版
│   └── android/app/src/            # Android 工程的主要源集
│       ├── native/                 # 原生 Compose、导入解析、AI、WebDAV
│       ├── katex/                  # 正式版公式排版与本地资源
│       ├── ratex/、lite/           # 开发对照与纯文本构建
│       ├── latexComparison/        # 仅用于对照 APK 的内置题库
│       └── web/                    # 历史 WebView 壳
├── docs/                           # 文档索引见 docs/README.md
├── test/                           # 原生解析、公式对照与 Web 专项测试
├── assets/                         # 宣传素材
├── CHANGELOG.md
├── CONTRIBUTING.md
├── LICENSE
└── README.md
```

Android 工程仍保留两个 flavor：

| Flavor | 包名 | 技术路线 |
| --- | --- | --- |
| `web` | `com.yiqiu.shirohaquiz` | 历史 WebView 壳 |
| `native` | `com.reqir.shirohaquiz` | Kotlin + Jetpack Compose + Material3 |

---

## 本地运行

### Web

```powershell
& '.\apps\web\打开Shiroha Quiz.cmd'
```

也可以使用任意静态服务器：

```bash
npx serve apps/web
```

在线版：[https://reiqr.github.io/shiroha-quiz](https://reiqr.github.io/shiroha-quiz)

### Android

```bash
cd apps/android
./gradlew assembleNativeRelease              # 正式版（离线 KaTeX）
```

Windows PowerShell：

```powershell
cd apps/android
.\gradlew.bat assembleNativeRelease              # 正式版（离线 KaTeX）
```

RaTeX 对照包和纯文本 Lite 保留为开发构建，见 [公式引擎测试说明](./test/android-katex/README.md)。

---

## 测试与回归

解析器外部回归：

```powershell
.\run-regression.ps1
```

原生 JVM 单元测试：

```powershell
cd apps/android
.\gradlew.bat :app:testNativeDebugUnitTest
```

WebDAV 请求测试：

```powershell
node --test test/web-webdav/request.test.cjs
```

AI 练习回归位于 `test/web-ai-practice/`，需要 Node、Playwright 和可用的 Chromium / Edge。

相关说明：

- [测试体系整理说明](docs/native/测试体系整理说明.md)
- [解析器回归测试说明](docs/native/解析器回归测试说明.md)
- [外部回归测试包说明](test/native-parser-regression/README.md)
- [LaTeX 引擎对照与内置题库](test/android-katex/README.md)

---

## 开发状态与文档

历史开发计划已归档到 `docs/archive/`。当前功能状态以 README、CHANGELOG、Releases 和原生开发进度为准。

- 文档索引：[Markdown](docs/README.md) / [Word](docs/文档索引/README.docx)
- [原生 Android 开发进度](docs/native/原生开发进度.md)
- [架构说明](docs/universal/架构说明.md)
- [解析器回归测试说明](docs/native/解析器回归测试说明.md)

---

## 参与贡献 / 提交反馈

欢迎通过 [Issue](https://github.com/reiqr/shiroha-quiz/issues/new/choose) 提交 Bug、题库兼容问题、导入失败样例、功能建议或文档修正。

提交时请注明平台（原生版 / Web 版），方便定位问题。

- [CONTRIBUTING.md](./CONTRIBUTING.md)
- [CHANGELOG.md](./CHANGELOG.md)

---

## 贡献者

感谢以下贡献者：

- [@jiesou](https://github.com/jiesou) —— Web 练习页单题 AI 解析/追问与 WebDAV 同步相关贡献
- [@anupamme](https://github.com/anupamme) —— 指出 AI API Key 明文存储问题并提供最小修复思路
- [@lyoualker-sudo](https://github.com/lyoualker-sudo) —— 为原生 Android 版实现离线 KaTeX 公式排版，并补充渲染回退、无障碍与测试题库（[PR #147](https://github.com/reiqr/shiroha-quiz/pull/147)）

---

## 许可证

本项目采用 `GPL-3.0` 开源。

### 第三方组件

<details>
<summary>点击展开第三方组件与许可声明</summary>

本项目包含以下第三方开源组件；完整清单、版权信息与许可原文见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。

| 平台 | 组件 | 许可 |
|------|------|------|
| Android | AndroidX / Jetpack Compose / Material Components / OkHttp / WorkManager / Kotlin | Apache-2.0 |
| Android | KaTeX；RaTeX（开发对照） | MIT |
| Web | MathJax / PDF.js / Tesseract.js | Apache-2.0 |
| 测试 | JUnit / Robolectric / kxml2 | EPL-1.0 / MIT / BSD-style |

</details>

## Star History

<a href="https://star-history.com/#reiqr/shiroha-quiz&Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=reiqr/shiroha-quiz&type=Date&theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=reiqr/shiroha-quiz&type=Date" />
    <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=reiqr/shiroha-quiz&type=Date" />
  </picture>
</a>
