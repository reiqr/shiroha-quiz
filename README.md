<div align="center">

<img src="assets/promo4.jpg" width="800" alt="Shiroha Quiz">
</div>

<br>

# Shiroha Quiz

<img src="assets/shiroha_quiz_ui_assets_v2_cutout/illus_me_settings.webp" width="160" align="right" alt="看板娘" />

Shiroha Quiz 是一个轻量、开源的刷题工具，支持自导入题库、练习、考试与多端使用。

> 你手里有 Word、Excel、TXT、JSON，或者题目和答案分开的文件。Shiroha Quiz 尝试把这些内容自动识别成可练习、可考试、可复盘的个人题库。

当前主要有两条公开使用线：

- **Web 版**：适合电脑端整理题库，支持导入、练习、考试、错题、备份、AI 辅助与 OCR 测试。
- **Android 原生版**：当前主推安装包，使用 Kotlin + Compose 原生实现，支持 AI 辅助、多空填空、背题/斩题、图片题、智能复习等功能。

> [!IMPORTANT]
> **更新节奏**：最近工作稍微闲了一些，应该会开始恢复更新。问题反馈仍欢迎提交。

<br style="clear:both">

---

## 快速开始

| 你的情况 | 下载什么 | 入口 |
| --- | --- | --- |
| Android 手机 | 原生版 APK，文件名以 `-native-release.apk` 结尾 | [下载](https://github.com/reiqr/shiroha-quiz/releases/latest) |
| Windows 电脑 | Web ZIP，解压后双击 `打开Shiroha Quiz.cmd` | [下载](https://github.com/reiqr/shiroha-quiz/releases/latest) |
| 先体验一下 | 在线版，免安装 | [打开在线版](https://reiqr.github.io/shiroha-quiz) |

> 原生版与 Web 版使用独立版本号，不是新旧关系。**下载时认文件名即可，当前版本以 [Releases/latest](https://github.com/reiqr/shiroha-quiz/releases/latest) 为准。**

> 当前仍处于测试阶段，不建议用于高风险正式考试场景。重要题库请定期备份。

---

## 原生版与 Web 版区别

| 维度 | Android 原生版 | Web 版 |
| --- | --- | --- |
| 使用方式 | 安装 APK，Android 8.0+ | 解压后双击启动器，免安装 |
| 数据存储 | 本地 SharedPreferences | 浏览器 LocalStorage |
| 离线能力 | 除 AI、在线文档识别外可离线 | 基础功能可离线，扩展库可按需下载 |
| 文档识别 | MinerU 在线 OCR、DOCX 图片提取 | PDF.js 文字层解析、OCR 测试区 |
| 特色 | 背题、斩题、智能复习、暗夜模式、平板适配 | 桌面题库清洗、OCR、AI 导入与单题解析 |
| 适合 | 手机日常刷题 | 电脑整理题库与快速使用 |

<details>
<summary><strong>展开查看两端详细差异</strong></summary>

<br>

| 功能 | Android 原生版 | Web 版 |
| --- | --- | --- |
| 技术路线 | Kotlin + Jetpack Compose + Material3 | HTML + CSS + JavaScript |
| 练习模式 | 普通、随机、顺序、即时反馈、批量练习 | 普通、随机、顺序练习 |
| 背题 / 斩题 | 支持 | 暂无 |
| 智能复习 | 支持 | 暂无 |
| 选项打乱 | 练习、考试独立控制 | 基础刷题为主 |
| 顺序进度 | 退出时可保存当前位置，背题进度独立 | 自动记录顺序练习进度 |
| 错题本 | 当前题库 / 全部题库作用域、掌握状态、智能复习 | 错题记录、重新练习、掌握状态 |
| 学习记录 | 逐题复盘、只看错题筛选 | 练习 / 考试记录与复盘 |
| 图片题 | 原生图片题、DOCX 内嵌图片、ZIP 备份 | 支持图片题显示、JSON / ZIP 跨端导入 |
| 文档识别 | MinerU 在线 OCR，支持图片提取与人工绑定 | PDF.js 文字层解析；扫描 PDF 可用 OCR 测试区 |
| AI 导入 | 核对、补解析、批量范围处理、结构化优先 | 整理、核对、补解析、结构化优先 |
| 单题 AI | 解析、追问、保存解析；可选无答案题临时参考 | 按题型分层解析、连续追问、保存完整解析草稿 |
| AI 接口 | DeepSeek、OpenAI 兼容、自定义接口 | DeepSeek、OpenAI 兼容、自定义接口，也可连接 Ollama / LM Studio |
| 云备份 | WebDAV 手动 / 自动备份与恢复 | WebDAV 上传、列表、校验、合并 / 覆盖恢复 |
| 外观 | 浅色 / 暗夜、Shiroha 模式、平板侧边导航 | 蓝白桌面界面 |
| 主要定位 | 手机日常刷题、复习 | 电脑端题库导入、清洗和刷题 |

> 两端题库与备份格式持续保持互通；具体功能变化以 [CHANGELOG](./CHANGELOG.md) 和 [Releases](https://github.com/reiqr/shiroha-quiz/releases) 为准。

</details>

---

## 当前能力

### 刷题与考试

- 单选、多选、判断、填空（含多空）、简答题。
- 随机练习、顺序练习、考试、错题复习、收藏与学习记录。
- 支持题库分组、跨题库练习范围和顺序练习进度。
- 原生版支持即时/批量练习、背题、斩题、选项打乱、字号调整和智能复习。
- 考试支持倒计时、答题卡、交卷提醒和结果统计。
- 练习中可快速编辑题目，修改后写回来源题库。

### 题库导入

- 支持 `docx`、`xlsx` / `csv`、`txt`、`json`、文字层 `pdf` 和粘贴文本。
- 支持“题目文件 + 答案文件”双文件导入。
- 自动识别题号、题干、选项、答案、解析、题型和分区。
- 支持导入预览、异常提示、手动修正、全文查找/替换。
- 原生版支持 DOCX 内嵌图片提取、材料题/共用题干和集中答案区等复杂格式。
- Web 版支持 PDF.js 文字层解析，并提供扫描 PDF OCR 测试兜底。
- 原生解析器持续维护真实题库回归测试，复杂边界说明放在专项文档中。

### AI 辅助

- Web 与原生版均支持 AI 辅助导入、核对和补解析。
- AI 导入优先使用结构化题目结果，校验失败时再退回文本解析。
- 练习页支持按需单题 AI 解析与追问，确认后可保存解析。
- 原生版可选启用“无答案题 AI 临时参考答案”，默认关闭，不写回题库、不进入错题本。
- 支持 DeepSeek、OpenAI 兼容接口和自定义接口；Web 版也可连接 Ollama / LM Studio。

> AI 结果仅作辅助参考；使用外部 AI 服务时，题目文本会发送给你配置的服务商。API Key 不写入题库备份。

### 备份与跨端

- 支持完整数据备份和恢复。
- Web 与原生版题库格式互通，图片题可通过 ZIP 保留素材。
- 支持 WebDAV 云备份、远程恢复和原生版可选自动备份。
- 原生版 API Key 使用 Android Keystore 加密存储。

---

## 使用说明

### Web 版

1. 从 [Releases](https://github.com/reiqr/shiroha-quiz/releases/latest) 下载 Web ZIP 并解压。
2. 双击 `打开Shiroha Quiz.cmd`，脚本会启动本地服务并打开浏览器，无需预装 Python。
3. 在 **导入题库** 中粘贴文本或上传文件。
4. 在识别预览中核对题型、答案和解析后导入。
5. 进入练习或考试开始使用。

> 不建议直接双击 `index.html`。PDF/OCR 依赖 Web Worker 和 ES module，`file://` 下部分能力会失效，而且数据来源与启动器模式不同。

### Android 原生版

1. 安装 `*-native-release.apk`。
2. 在 **导入** 页面选择文件或粘贴文本。
3. 在核对页检查识别结果，必要时手动修正或使用 AI 辅助。
4. 选择练习、考试、背题等模式开始使用。
5. 错题、收藏、学习记录和智能复习会保存在本机。

### 数据备份建议

- 重要题库导入后及时导出备份。
- 换设备、清缓存、卸载 App 前先备份。
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

每次正式发布通常提供 Android 原生 APK 与 Web ZIP。近期功能变化以 Release 说明和 CHANGELOG 为准，README 只保留当前稳定能力概览。

Web ZIP 不包含约 88 MB 的 PDF.js / MathJax / Tesseract 离线扩展资源，可在 `apps/web/libs/` 下按需下载；联网时也可使用 CDN 兜底。

---

> 以下为开发者内容。

## 仓库结构

```text
shiroha-quiz/
├── apps/
│   ├── web/                        # Web 版
│   └── android/                    # Android 工程
│       └── app/src/native/         # 原生 Compose 源码
│           ├── ai/                 # AI
│           ├── importer/           # 导入解析
│           ├── document/           # 文档识别
│           ├── security/           # 密钥存储
│           ├── state/              # 状态管理
│           ├── sync/               # WebDAV
│           └── ui/                 # Compose UI
├── docs/                           # 文档索引见 docs/README.md
├── test/                           # 回归与 Web 测试
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
apps/web/打开Shiroha Quiz.cmd
```

也可以使用任意静态服务器：

```bash
npx serve apps/web
```

在线版：[https://reiqr.github.io/shiroha-quiz](https://reiqr.github.io/shiroha-quiz)

### Android

```bash
cd apps/android
./gradlew assembleNativeRelease
```

Windows PowerShell：

```powershell
.\gradlew.bat assembleNativeRelease
```

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

---

## 开发状态与文档

历史开发计划已归档到 `docs/archive/`。当前功能状态以 README、CHANGELOG、Releases 和原生开发进度为准。

- [文档索引](docs/README.md)
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

---

## 许可证

本项目采用 `GPL-3.0` 开源。