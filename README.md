<div align="center">
  <img src="assets/promo4.jpg" width="800" alt="Shiroha Quiz">
</div>

<br>

# Shiroha Quiz

<img src="assets/shiroha_quiz_ui_assets_v2_cutout/illus_me_settings.webp" width="160" align="right" alt="看板娘" />

Shiroha Quiz 一个轻量、开源的刷题工具，支持自导入题库、练习、考试、与多端使用。

Shiroha Quiz 解决一个很实际的问题：

> 你手里有题库——Word、PDF、Excel、TXT，或者题目和答案分开的文件。但格式不统一，整理成本高，即使整理完了也只能翻看，没法真正做题。Shiroha Quiz 把它们自动识别导入，变成可练习、可考试、可错题复盘的个人题库。

当前项目包含三条线：

- **Web 版**：功能完整稳定，题库导入、刷题考试、错题复习均已就绪。
- **Android WebView 壳版**：Web 资源打包进 APK，离线加载，UI较一般，逻辑同web一致。
- **Android 原生 Compose 版**：Kotlin + Compose 原生实现，暗夜模式、AI 解析等均已落地，画面精致、使用流畅。

<br style="clear:both">

---

### 快速导览

| 想了解什么 | 跳转 |
|-----------|------|
| 有哪些功能 | [当前能力](#当前能力) |
| AI 功能 | [AI 智能功能](#ai-智能功能原生版) |
| 两个 APK 选哪个 | [Android 双版本说明](#android-双版本说明) |
| 怎么用 | [使用说明](#使用说明) |
| 支持什么格式 | [导入格式与策略](#导入格式与策略) |
| 怎么下载 | [下载与使用](#下载与使用) |
| 怎么参与 | [参与贡献](#参与贡献) |

---

## 当前能力

- Web 端功能完整，在线即可使用。
- Android WebView 壳版离线加载，适合日常刷题。
- Android 原生 Compose 版暗夜模式、AI 解析、表格导入等均已落地。
- 内置 C1 科目一题库，方便首次体验。

### 刷题与考试

**练习模式**
- 支持随机抽题或题库顺序两种组题方式，偏好自动记忆
- 单选题/多选题选项选择，判断题对错切换，填空/简答文本输入
- 支持分组答题，一组提交后查看结果
- 提交后选项着色区分正误，顶部卡片可收起
- 答错的题自动进入错题本
- 完成全部题目后展示总结：正确率、错题数、重新练习入口

**考试模式**
- 按题型自定义题目数量与分值，设置考试时长，偏好自动记忆
- 实时倒计时，到时自动交卷
- 答题卡快速跳题，未答题目交卷前提醒，支持滑动切题
- 交卷后展示各题型得分、正确率和明细报告

**错题本**
- 练习与考试中答错的题自动收录
- 支持按题库、题型筛选错题
- 错题可重新练习，掌握后可标记

**刷题记录**
- 每轮练习或考试生成一条独立记录
- 记录详情支持逐题复盘，查看每道题的作答与正误
- 按时间倒序排列，方便回顾学习轨迹

**多题型支持**
- 单选题、多选题、判断题、填空题、简答题

### 题库导入

**多格式支持**
- 上传 `docx` 文件（推荐），也支持 `xlsx`/`xls` 表格、`txt`、`json`、文字层 `pdf` 或粘贴纯文本
- 原生版支持 docx 内嵌图片提取，Web 版支持 PDF.js 解析
- **扫描件/图片型 PDF 暂不支持**

**双文件导入**
- 题目文件和答案文件分别上传，自动匹配题号
- 支持 “1-10：D A A B C…” 范围格式和 “1.D 2.A” 配对格式
- 答案文件缺失题号时按顺序自动对应

**识别与预览**
- 自动识别题号、题干、选项、答案、解析和题型
- 支持分区标题继承题型（如 “一、单选题” 下所有题自动归为单选）
- 填空题关键词覆盖更广（空白、空格、横线、括号内等），减少简答题误判
- 识别结果预览：逐题查看题型、答案和异常标记，核对筛选器按需显示
- 识别失败时可手动切换解析策略或调整文本后重试

**手动修正**
- 预览中可逐题修改题型、答案和题干
- 文本编辑器支持查找/替换，可正则匹配批量修改导入原文
- 支持批量编辑和删除异常题目

**备份恢复**
- 全部数据一键导出为 JSON 备份文件，Web 端与原生端导出格式互通，可相互导入
- 原生端导出 ZIP 含图片素材，Web 端同样可直接导入并自动转换
- 支持批量导出单个题库 JSON
- 恢复时可选合并或覆盖现有数据

### AI 智能功能（原生版）

- **AI 核对**：导入结果可发送至 AI 自动校验题型、答案和解析
- **AI 解析**：AI 自动生成题目解析
- 支持自定义 API Key，兼容 OpenAI / DeepSeek 等主流格式

### 视觉与体验（原生版）

- 暗夜模式 / 浅色模式切换
- Shiroha 模式：统一管控开屏图、页面插画和应用图标，可以在更通用的场景下使用（二次元ssw了）
- 统一的 Design Token 间距与颜色系统

---

## Android 双版本说明

<!-- ![WebView 壳版截图](assets/screenshot-web.png) -->
<!-- ![原生 Compose 版截图](assets/screenshot-native.png) -->

Releases 页面提供两个 APK，根据需求选择：

### WebView 壳版（`*-web-release.apk`）

- 内置完整 Web 页面，离线可用
- 功能最全面：导入、练习、考试、错题、备份均已稳定
- 下载约 6 MB

### 原生 Compose 版（`*-native-release.apk`）

- Kotlin + Compose 纯原生实现
- 启动更快，界面更流畅，体积更小（约 5 MB）
- 暗夜模式、AI 功能、表格导入均已落地

| | WebView 壳版 | 原生 Compose 版 |
|------|-------------|----------------|
| 稳定性 | 高 | 中 |
| 流畅度 | 一般 | 高 |
| 功能完整度 | 完整 | AI/暗夜模式/表格导入等已落地 |
| APK 大小 | ~6 MB | ~5 MB |
| 适合人群 | 日常刷题用户 | 想体验原生流畅度的用户 |

> **两个版本可同时安装，互不冲突（包名不同）。**

### 工程结构

Android 工程通过 `productFlavors` 维护两个版本，共享同一 Gradle 项目：

| Flavor | 包名 | 技术路线 |
|---|---|---|
| `web` | `com.yiqiu.shirohaquiz` | WebView 加载本地 Web 资源 |
| `native` | `com.reqir.shirohaquiz` | Kotlin + Jetpack Compose + Material3 |

```text
apps/android/
├── app/src/main/assets/web/        # WebView 壳内置 Web 资源
├── app/src/main/java/              # 通用 Android 入口
└── app/src/native/java/            # 原生 Compose 版本代码
```

---

## 原生 Compose 当前状态

原生版已覆盖从导入到复盘的核心流程，包含 13 个页面，暗夜模式、AI 功能、表格导入等已落地。后续重点是导入核校、页面细节和发布前稳定性。

| 页面 | 功能 |
|------|------|
| `HomeScreen` | 首页、题库概览、今日状态 |
| `ImportScreen` | 文件解析、双文件导入、后台处理 |
| `PracticeScreen` | 随机/顺序组题、选项锁定、完成总结 |
| `ExamScreen` | 计时考试、答题卡、未答提醒 |
| `BankListScreen` | 题库管理与重命名 |
| `BankDetailScreen` | 题库详情与二次核对 |
| `BankReviewScreen` | 逐题检查与修正 |
| `WrongBookScreen` | 错题汇总复习 |
| `RecordsScreen` | 练习考试记录列表 |
| `RecordDetailScreen` | 单轮逐题复盘 |
| `MeScreen` | 数据导入/导出/备份 |
| `AboutScreen` | 版本信息 |
| `StandardImportFormatScreen` | 标准格式说明与 AI 清洗指引 |

核心模块：

- `QuizRepository`：题库、练习、考试、错题、记录的集中状态管理
- `ai/`：AI 核对、AI 解析生成、OpenAI 兼容 API
- `importer/`：解析器链、双文件合并、策略评分、验证
- `ui/components/`：GlassCard、ActionPillButton 等设计系统组件
- `ui/theme/`：Material3 主题、间距令牌、字体系统

---

## 使用说明

### Web 端快速上手

1. 打开 `apps/web/index.html`，或访问 [在线版](https://reiqr.github.io/shiroha-quiz)。
2. 进入 **导入题库**，粘贴文本或上传文件。
3. 系统自动识别题型、选项、答案和解析。
4. 在识别预览中确认题目无误。
5. 进入 **刷题练习** 或 **考试模式** 开始使用。
6. 答错的题会进入 **错题本**。
7. 定期在 **设置/导出** 中导出备份。

### 数据备份建议

Shiroha Quiz 的题库和记录保存在本地存储中（Web 端使用浏览器 LocalStorage，原生版使用 SharedPreferences）。

建议：

- **重要题库导入后，及时导出全部数据备份。**
- **换设备、清理缓存、卸载 App 前，务必先导出备份 JSON 或 ZIP。**
- 从 Shiroha Quiz 导出的备份 JSON/ZIP，应在 **设置/导出 → 导入配置 / 备份 JSON/ZIP** 中导入。
- Web 端导出的 JSON 可直接导入原生端；原生端导出的 ZIP 也可导入 Web 端，含图片题库完全互通。
- **备份 JSON、批量题库 JSON 不要放进普通题库导入区解析。**

---

## 导入格式与策略

支持 `docx`（推荐）、`xlsx`/`xls` 表格、`txt`、`json`、文字层 `pdf`、粘贴纯文本、题目+答案双文件导入。系统自动识别题号、题干、选项、答案、解析、题型、分区/分卷，导入后进入识别预览供逐题确认。

详细说明：

- [所有支持的题库导入格式](docs/Shiroha_Quiz_题库导入格式支持说明.md)
- [题库导入策略与使用指南](docs/Shiroha%20Quiz%20题库导入策略与使用指南.md)
- [题目导入解析方法说明](docs/Shiroha%20Quiz%20题目导入解析方法说明.md)
- 标准题库格式示例：[Markdown](docs/标准题库格式示例.md) / [Word](docs/标准题库格式示例.docx) / [PDF](docs/标准题库格式示例.pdf)

如果原题库格式非常混乱，且题目没有保密需求，可以先使用 LLM 智能体进行数据清洗，例如豆包、深度求索、通义千问等。清洗目标不是改题，而是统一题号、选项、答案和解析格式。

---

## 仓库结构

```text
shiroha-quiz/
├── .github/                         # Issue 模板与 GitHub Actions
├── apps/
│   ├── web/                         # Web 版
│   │   ├── index.html
│   │   ├── app.js
│   │   ├── styles.css
│   │   ├── question-bank.js
│   │   ├── media/                   # Web 插画素材
│   │   ├── data/                    # 内置题库
│   │   └── libs/                    # PDF.js 等本地库
│   └── android/                     # Android 工程
│       └── app/
│           ├── src/main/assets/web/ # Android WebView 壳内置 Web 资源
│           ├── src/main/java/       # WebView 壳入口与 Android 原生入口
│           └── src/native/java/     # 原生 Compose 代码
├── docs/
│   ├── web/                         # Web 端相关文档
│   ├── native/                      # 原生 Android 相关文档
│   └── universal/                   # 通用架构建议
├── assets/                          # 宣传图与素材源文件
├── packages/                        # 跨端接口契约与设计文档
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

构建 WebView 壳版本：

```bash
./gradlew assembleWebRelease
```

构建原生 Compose 版本：

```bash
./gradlew assembleNativeRelease
```

Windows PowerShell 可使用：

```powershell
.\gradlew.bat assembleWebRelease
.\gradlew.bat assembleNativeRelease
```

构建输出通常位于：

```text
apps/android/app/build/outputs/
```

---

## 下载与使用

下载入口：

- [GitHub Releases](https://github.com/reiqr/shiroha-quiz/releases)
- [在线体验](https://reiqr.github.io/shiroha-quiz)

最新版本请以 [GitHub Releases](https://github.com/reiqr/shiroha-quiz/releases) 为准。当前仓库文档记录的主要版本线为：

- Web 版：`v0.4.6-alpha`
- 原生 Compose 版：`v0.4.6-native`

每次发布包含 Android APK 及相关说明文档。

> **当前为 beta 测试阶段，功能尚在完善中，不建议用于高风险正式考试场景。**

---

## 开发计划

详见 [30.1-30.4](docs/native/Shiroha_Quiz_后续功能开发计划30.1-30.4.md) / [30.5-31](docs/native/Shiroha_Quiz_后续功能开发计划30.5-31.md)。

---

## 参与贡献

欢迎通过 Issue 提交：

- Bug 反馈
- 题库格式兼容问题
- 导入失败样例
- UI / 交互优化建议
- Android 适配问题
- 文档补充建议

详见：

- [CONTRIBUTING.md](./CONTRIBUTING.md)
- [CHANGELOG.md](./CHANGELOG.md)

---

## 许可证

本项目采用 `GPL-3.0` 开源。
