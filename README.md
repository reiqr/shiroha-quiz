<div align="center">

> [!NOTE]
> 最近空闲时间沉迷「黑夜君临」，更新不会很勤快（）祝大家刷题顺利~

<img src="assets/promo4.jpg" width="800" alt="Shiroha Quiz">
</div>

<br>

# Shiroha Quiz

<img src="assets/shiroha_quiz_ui_assets_v2_cutout/illus_me_settings.webp" width="160" align="right" alt="看板娘" />

Shiroha Quiz 一个轻量、开源的刷题工具，支持自导入题库、练习、考试、与多端使用。

Shiroha Quiz 解决一个很实际的问题：

> 你手里有题库——Word、Excel、TXT、JSON，或者题目和答案分开的文件。但格式不统一，整理成本高，即使整理完了也只能翻看，没法真正做题。Shiroha Quiz 把它们自动识别导入，变成可练习、可考试、可错题复盘的个人题库。文字层 PDF 可直接辅助解析，Web 版也提供扫描 PDF 的 OCR 测试入口，可先转成可编辑文本或 DOCX 后再人工核对导入。

当前项目主要包含两条公开使用线：

- **Web 版**：在线即可使用，支持题库导入、刷题考试、错题复习、分组练习、数据备份与跨端互通；当前 V36 增加 OCR 测试能力，可将扫描 PDF 识别为文本或 DOCX，适合桌面端整理题库和快速体验。
- **Android 原生 Compose 版**：当前主推安装包，使用 Kotlin + Compose 原生实现，支持多空填空、平板侧边导航、暗夜模式、AI 核对/解析/单题分析、图片题、背题模式、斩题、智能复习、收藏、快速编辑和错题复盘。

<br style="clear:both">

---

### 快速入口

| 想做什么 | 入口 |
| --- | --- |
| 直接体验 Web 版 | [下载与使用](#下载与使用) |
| 下载 Android 安装包 | [下载与使用](#下载与使用) |
| 了解原生版和 Web 版区别 | [Android 版本说明](#android-版本说明) |
| 查看主要功能 | [当前能力](#当前能力) |
| 导入自己的题库 | [导入格式与策略](#导入格式与策略) |
| 本地运行项目 | [本地运行](#本地运行) |
| 跑解析器回归测试 | [测试与回归](#测试与回归) |
| 提交问题或建议 | [参与贡献 / 提交反馈](#参与贡献--提交反馈) |

---

## 当前能力

- Web 端功能完整，在线即可使用；V36 增加扫描 PDF OCR 测试、MathJax 本地 LaTeX 公式渲染，可输出可编辑文本和 DOCX，核对后继续进入题库导入流程。
- Android 原生 Compose 版：多空填空题、平板侧边导航、暗夜模式、AI 解析/AI 核对/AI 单题分析、表格导入、JSON 多格式导入、背题模式、斩题功能、选项打乱、智能复习、题目收藏、快速编辑、错题作用域切换、顺序练习进度记忆、记录只看错题筛选、错题复盘均已落地。
- 内置 C1 科目一题库，方便首次体验。

### 刷题与考试

**练习模式**
- 支持随机抽题或题库顺序两种组题方式，偏好自动记忆
- 单选题/多选题选项选择，判断题对错切换，填空/简答文本输入，**多空填空题**逐空输入
- 支持即时练习与批量练习：即时练习可选择”选择后立即判题”和”答对后自动下一题”，单选/判断题支持”选后自动下一题”，批量练习保留整组提交
- 支持题目快速编辑：练习中可从题目右上角直接修改当前题目
- 支持题目收藏：练习中一键收藏题目，在收藏页集中查看
- 支持背题模式：练习页直接显示答案与解析，不计入正确率、不加入错题本、不生成普通练习记录
- 支持选项打乱（练习+考试独立控制）：固定 A/B/C/D 标签仅随机内容映射，本次练习/考试内顺序不变
- 支持斩题功能：可把一眼会的题移出普通练习池，并在题库详情中集中管理和恢复
- 支持顺序练习进度记忆：下次可从上次顺序练习进度继续，退出练习时可选择保存当前位置
- 支持字号缩放：题干与选项字号可独立调整，紧凑选项模式减少卡片间距适合长题快速阅读
- 提交后选项着色区分正误，顶部卡片可收起
- 答错的题自动进入错题本
- 完成全部题目后展示总结：正确率、错题数、重新练习入口

**考试模式**
- 按题型自定义题目数量与分值，设置考试时长，偏好自动记忆
- 实时倒计时，到时自动交卷
- 答题卡快速跳题，未答题目交卷前提醒，支持滑动切题
- 考试不受背题模式和斩题功能影响：不会提前显示答案，也不会过滤已斩题
- 交卷后展示各题型得分、正确率和明细报告

**错题本**
- 练习与考试中答错的题自动收录，记录首次出错时间和累计错误次数
- 支持**错题作用域切换**：可按当前题库或全部题库筛选错题列表、首页统计和复习范围
- 支持按题库、题型和掌握状态筛选错题
- 错题本展示”错 X 次 / 对 Y 次”，用于保留进入错题本后的历史累计表现
- 错题可重新练习，连续答对 2 次后自动标记为已掌握
- 再次答错会清空连续答对次数，并回到未掌握状态
- 支持手动标记掌握 / 取消掌握，状态调整不会篡改历史答对次数
- 支持智能复习模式：根据错题表现自动安排到期复习，首页同步待复习数量
- 斩题与错题掌握互相独立：斩题用于移出普通练习池，错题掌握用于错题复习状态

**刷题记录**
- 每轮练习或考试生成一条独立记录
- 记录列表和详情均支持**只看错题筛选**
- 记录详情支持逐题复盘，查看每道题的作答与正误
- 按时间倒序排列，方便回顾学习轨迹

**多题型支持**
- 单选题、多选题、判断题、填空题（含多空）、简答题

### 题库导入

**多格式支持**
- 上传 `docx` 文件（推荐），也支持 `xlsx`/`xls` 表格、`txt`、`json`、文字层 `pdf` 或粘贴纯文本
- 原生版支持 **docx 内嵌图片提取**，Web 版支持 PDF.js 解析文字层 PDF，并提供扫描 PDF OCR 测试兜底
- 原生版兼容 Web 导出的图片题 JSON：支持旧 Markdown base64 图片和新的 `images` 数组结构
- 扫描件/图片型 PDF 建议先在 Web 版 OCR 测试区转成可编辑文本或 DOCX，人工核对后再导入题库

**双文件导入**
- 题目文件和答案文件分别上传，自动匹配题号
- 支持 “1-10：D A A B C…” 范围格式和 “1.D 2.A” 配对格式
- 答案文件缺失题号时按顺序自动对应

**识别与预览**
- 自动识别题号、题干、选项、答案、解析和题型
- 兼容题型大小写和常见别名，如 `single`/`SINGLE`、`multiple`/`MULTIPLE`、`judge`/`JUDGE` 等
- 支持分区标题继承题型（如 “一、单选题” 下所有题自动归为单选）
- 原生版支持**共用题干 / 材料题兜底识别**，并可将集中答案解析区合并回对应题目
- 填空题关键词覆盖更广（空白、空格、横线、括号内等），减少简答题误判
- 识别结果预览：逐题查看题型、答案和异常标记，核对筛选器按需显示
- 识别失败时可手动切换解析策略或调整文本后重试

**手动修正**
- 预览中可逐题修改题型、答案和题干
- 文本编辑器支持查找/替换，可正则匹配批量修改导入原文
- 支持批量编辑和删除异常题目

**备份恢复**
- 全部数据一键导出为 JSON 备份文件，Web 端与原生端导出格式互通，可相互导入
- 完整备份含错题本、收藏夹、学习记录，跨端互导时可自动恢复（v0.8.1+）
- 原生端导出 ZIP 含图片素材，Web 端同样可直接导入并自动转换
- 支持批量导出单个题库 JSON
- 恢复时可选合并或覆盖现有数据

### AI 智能功能（原生版）

- **AI 核对（导入页）**：导入结果可发送至 AI 自动校验题型、答案和解析
- **AI 解析（导入页）**：AI 自动生成题目解析，适合补全缺解析或解析过短的题目
- **AI 补解析（编辑器）**：题库编辑/审阅/导入预览/快速编辑四个入口统一集成，一键 AI 生成解析建议
- **AI 单题分析（练习页）**：练习提交后或背题模式下触发，AI 独立判断参考答案并与题库对照，标注置信度和需要人工确认的标记
- 支持 DeepSeek、OpenAI 兼容接口和自定义接口，可配置 API 地址、API Key 与模型名称
- 支持仅处理异常题、控制单批处理数量、超时时间和保存阈值
- **AI 结果仅作参考，不会自动修改题库答案或解析。**

### 视觉与体验（原生版）

- 平板侧边导航：宽屏时底部导航自动切换为左侧导航，外观设置中可开关
- 暗夜模式 / 浅色模式切换，偏好会持久化保存
- Shiroha 模式：统一管控开屏图、页面插画和应用图标，可在更通用的场景下关闭角色元素
- 二级页面支持系统侧边返回映射，减少误退到桌面的情况
- 阅读显示偏好可调：题干/选项字号独立控制，支持紧凑选项模式
- 统一的 Design Token 间距与颜色系统，卡片、按钮、底部导航保持一致质感

---

## Android 版本说明

<!-- ![原生 Compose 版截图](assets/screenshot-native.png) -->

Releases 页面推荐下载原生 Compose 版 APK：

- Kotlin + Compose 纯原生实现，当前主要开发线
- 更美观、更易用、刷题体验更好：Material3 原生界面、流畅动画与手势、平板自适应侧边导航、暗夜模式、系统返回键、Design Token 视觉规范
- 原生版独有：**AI 全功能**（核对/解析/单题分析/补解析）、**多空填空题**全链路、**docx 内嵌图片提取**、选项打乱、背题模式、斩题功能

### Android 工程结构

Android 工程通过 `productFlavors` 保留历史 WebView 壳与当前原生版本，共享同一 Gradle 项目：

| Flavor | 包名 | 技术路线 |
|---|---|---|
| `web` | `com.yiqiu.shirohaquiz` | WebView 加载本地 Web 资源 |
| `native` | `com.reqir.shirohaquiz` | Kotlin + Jetpack Compose + Material3 |

---

## 使用说明

### Web 端快速上手

1. 打开 `apps/web/index.html`，或访问 [在线版](https://reiqr.github.io/shiroha-quiz)。如果要使用 OCR，建议双击 `apps/web/打开Shiroha Quiz.cmd` 启动本地服务后访问网页。
2. 进入 **导入题库**，粘贴文本或上传文件。
3. 系统自动识别题型、选项、答案和解析；扫描 PDF 可先在 OCR 测试区转成文本或 DOCX。
4. 在识别预览中确认题目无误。
5. 进入 **刷题练习** 或 **考试模式** 开始使用。
6. 答错的题会进入 **错题本**。
7. 定期在 **设置/导出** 中导出备份。

### 原生 Compose 版快速上手

1. 安装 `*-native-release.apk`，进入首页查看当前题库和学习状态。
2. 在 **导入** 页面上传 `docx`、表格、`txt`、`json` 或粘贴文本。
3. 在核对页检查题型、答案、解析和异常标记，必要时用全文编辑或 AI 核对辅助清洗。
4. 在 **练习** 中选择普通练习、即时反馈、自动下一题或背题模式。
5. 遇到一眼会的题，可以开启斩题功能，把它移出普通练习池。
6. 答错的题会进入 **错题本**，连续答对 2 次后自动标记为已掌握。
7. 在 **记录** 中复盘每轮练习或考试的逐题结果。

### 数据备份建议

Shiroha Quiz 的题库和记录保存在本地存储中（Web 端使用浏览器 LocalStorage，原生版使用 SharedPreferences）。

建议：

- **重要题库导入后，及时导出全部数据备份。**
- **换设备、清理缓存、卸载 App 前，务必先导出备份 JSON 或 ZIP。**
- **从 Shiroha Quiz 导出的备份 JSON/ZIP，应在设置/导出 → 导入配置 / 备份 JSON/ZIP 中导入。**
- **Web 端导出的 JSON 可直接导入原生端；原生端导出的 ZIP 也可导入 Web 端，含图片题库完全互通。**
- 原生端会兼容题型大小写差异和图片字段差异，减少跨端导入时的题型丢失与 base64 文本外露。
- **备份 JSON、批量题库 JSON 不要放进普通题库导入区解析。**

---

## 导入格式与策略

支持 `docx`（推荐）、`xlsx`/`xls` 表格、`txt`、`json`、粘贴纯文本、题目+答案双文件导入，也可辅助解析文字层 `pdf`。Web 版 V36 提供扫描 PDF OCR 测试，可生成可编辑文本或 DOCX 后再人工核对导入。系统自动识别题号、题干、选项、答案、解析、题型、分区/分卷；原生版额外加强了共用题干、材料题和集中答案解析区的兜底合并能力。导入后会进入识别预览，供逐题确认。

详细说明：

- [所有支持的题库导入格式](docs/题库导入格式支持说明/Shiroha_Quiz_题库导入格式支持说明.md)
- [题库导入策略与使用指南](docs/题库导入策略与使用指南/Shiroha%20Quiz%20题库导入策略与使用指南.md)
- [题目导入解析方法说明](docs/题目导入解析方法说明/Shiroha%20Quiz%20题目导入解析方法说明.md)
- 标准题库格式示例：[Markdown](docs/标准题库格式示例/标准题库格式示例.md) / [Word](docs/标准题库格式示例/标准题库格式示例.docx) / [PDF](docs/标准题库格式示例/标准题库格式示例.pdf)

如果原题库格式非常混乱，可以先做一次格式清洗再导入。国内用户可优先使用 DeepSeek、GLM / 智谱清言等工具辅助整理；如果可以稳定使用海外模型，也推荐 ChatGPT、Claude。**请注意：隐私风险主要来自”把题库上传到外部 AI 工具清洗”，不是 Shiroha Quiz 本地导入本身。** 清洗目标只是统一题号、选项、答案和解析格式，不能改题意、不能编造答案，无法确认的答案应标为”待确认”。

---

## 仓库结构

```text
shiroha-quiz/
├── .github/                         # Issue 模板与 GitHub Actions
├── apps/
│   ├── web/                         # Web 版
│   │   ├── index.html               # Web 入口
│   │   ├── app.js                   # Web 主逻辑
│   │   ├── styles.css               # Web 样式
│   │   ├── question-bank.js         # 内置题库数据
│   │   ├── media/                   # Web 插画素材
│   │   ├── data/                    # 内置题库
│   │   └── libs/                    # PDF.js 等本地库
│   └── android/                     # Android 工程
│       ├── app/
│       │   ├── build.gradle.kts
│       │   ├── src/main/            # 通用入口、Manifest、图标与内置资源
│       │   ├── src/web/             # 历史 WebView 壳入口
│       │   ├── src/native/          # 原生 Compose 版源码
│       │   │   ├── ai/              # AI 客户端与提示词
│       │   │   ├── importer/        # 题库导入引擎
│       │   │   │   ├── assets/      #   素材提取与绑定
│       │   │   │   ├── model/       #   导入数据模型
│       │   │   │   ├── parser/      #   文本 / 表格 / 双文件解析
│       │   │   │   ├── score/       #   解析策略评分
│       │   │   │   └── validate/    #   导入结果校验
│       │   │   ├── state/           # 全局状态管理
│       │   │   ├── ui/              # Compose UI
│       │   │   │   ├── app/         #   App Shell
│       │   │   │   ├── components/  #   可复用组件
│       │   │   │   ├── screens/     #   各页面
│       │   │   │   └── theme/       #   主题与设计 Token
│       │   │   └── util/            # 工具类
│       │   ├── src/test/            # 通用单元测试
│       │   └── src/testNative/      # 原生版解析器测试
│       ├── build.gradle.kts
│       ├── settings.gradle.kts
│       ├── gradle.properties
│       └── gradlew / gradlew.bat
├── docs/                            # 使用说明、导入格式、架构与开发文档
│   ├── 标准题库格式示例/
│   ├── 题库导入格式支持说明/
│   ├── 题库导入策略与使用指南/
│   ├── 题目导入解析方法说明/
│   ├── native/
│   ├── universal/
│   └── archive/
├── test/                            # 解析器回归测试
│   └── native-parser-regression/
├── assets/                          # 宣传图与素材源文件
├── CHANGELOG.md
├── CONTRIBUTING.md
├── LICENSE
└── README.md
```

---

## 本地运行

### Web 端

`apps/web/` 是纯静态页面，无需构建。

```bash
# 方式一：直接打开
apps/web/index.html

# 方式二：本地静态服务
npx serve apps/web
```

在线版：

```text
https://reiqr.github.io/shiroha-quiz
```

### Android 端

进入 Android 工程目录：

```bash
cd apps/android
```

构建原生 Compose 版本：

```bash
./gradlew assembleNativeRelease
```

Windows PowerShell 可使用：

```powershell
.\gradlew.bat assembleNativeRelease
```

构建输出通常位于：

```text
apps/android/app/build/outputs/
```

---

## 测试与回归

原生解析器有一套外部回归测试，用真实题库样例验证导入解析逻辑是否被新改动误伤。

推荐入口：

```powershell
.\run-regression.ps1
```

也可以只运行外部回归包：

```powershell
cd test\native-parser-regression
.\run-external-regression.ps1
```

回归测试会读取 `test/native-parser-regression/manifest.json`，将 `samples/` 里的样例解析为 `actual/`，再与 `expected/` 对比。失败时先查看：

- `test/native-parser-regression/actual/REGRESSION_REPORT.md`
- `test/native-parser-regression/actual/runner-summary.json`
- `test/native-parser-regression/actual/comparison-summary.json`

说明文档：

- [解析器回归测试说明](docs/native/解析器回归测试说明.md)
- [外部回归测试包说明](test/native-parser-regression/README.md)

---

## 下载与使用

下载入口：

- [GitHub Releases](https://github.com/reiqr/shiroha-quiz/releases)
- [在线体验](https://reiqr.github.io/shiroha-quiz)

最新版本请以 [GitHub Releases](https://github.com/reiqr/shiroha-quiz/releases) 为准。当前仓库文档记录的主要版本线为：

- 统一发布版：**v2.7.1-beta**
- Web 版：**v0.8.1-alpha**
- 原生 Compose 版：**v0.9.5-native**

`v0.8.x-native` 近期原生版重点：**多空填空题全链路**（解析/练习/考试/搜索/编辑，含内联多空结构与分区多空继承）；**CodeLikeTextGuard 代码表达式守卫**；**分组练习范围**（跨题库出题）；路由系统升级（子页面回退到上级）；系统返回键支持；图片选项识别；招聘答案清理；DOCX紧凑选项修复；判分归一化双端对齐；考试状态标签单行布局。早期阶段已完成导入解析器重构、JSON多格式导入、选项打乱、AI分析与补解析、错题作用域、填空/简答判分、题目搜索、分组管理、LaTeX识别、智能复习、跨端互通等能力。

`v0.8.0-alpha` Web 版 V36 重点：**OCR 扫描件兜底**（Tesseract.js，可识别为文本或下载 DOCX）；**MathJax 3.2.2 本地化**（离线 LaTeX 公式渲染）；**PDF.js 完整版**（CMaps + 标准字体 + ICC + WASM 图片解码）；**LaTeX 公式防选项误判**。

每次发布包含 Android APK 与 Web ZIP。

Web ZIP 不含离线扩展库（PDF.js 完整包、MathJax、Tesseract OCR，合计约 88 MB）。解压后运行 `libs/download-*.ps1` 按需下载，也可直接联网使用 CDN 兜底。详见 `额外附加功能说明.txt`。

> **当前为 beta 测试阶段，功能尚在完善中，不建议用于高风险正式考试场景。**

---

## 开发计划

历史开发计划已归档至 `docs/archive/`，当前功能状态以本 README、[CHANGELOG](./CHANGELOG.md)、[GitHub Releases](https://github.com/reiqr/shiroha-quiz/releases) 和 [原生 Android 开发进度](docs/native/原生开发进度.md) 为准。

开发与维护文档入口：

- [文档索引](docs/README.md)
- [原生 Android 开发进度](docs/native/原生开发进度.md)
- [架构说明](docs/universal/架构说明.md)
- [解析器回归测试说明](docs/native/解析器回归测试说明.md)

---

## 参与贡献 / 提交反馈

欢迎通过 [Issue](https://github.com/reiqr/shiroha-quiz/issues/new/choose) 提交，仓库提供了 **Bug 报告** 和 **功能建议** 两种模板：

- Bug 反馈
- 题库格式兼容问题
- 导入失败样例
- UI / 交互优化建议
- Android 适配问题
- 文档补充建议

> 提交 Issue 时请选择正确的平台（原生 Android App / Web 版），方便快速定位问题。

详见：

- [CONTRIBUTING.md](./CONTRIBUTING.md)
- [CHANGELOG.md](./CHANGELOG.md)

---

## 许可证

本项目采用 `GPL-3.0` 开源。
