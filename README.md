<div align="center">
  <img src="assets/promo.png" width="800" alt="Shiroha Quiz">
</div>

<br>

<img src="assets/mascot.png" width="160" align="right" alt="看板娘" />

# Shiroha Quiz

Shiroha Quiz 是一个轻量、开源的通用刷题工具，支持自导入题库、练习、考试、错题复习与多端使用。

Shiroha Quiz 解决一个很实际的问题：

> 你手里有题库——Word、PDF、TXT，或者题目和答案分开的文件。但格式不统一，整理成本高，即使整理完了也只能翻看，没法真正做题。Shiroha Quiz 把它们自动识别导入，变成可练习、可考试、可错题复盘的个人题库。

当前项目包含三条线：

- **Web 版**：功能完整稳定，题库导入、刷题考试、错题复习均已就绪。
- **Android WebView 壳版**：Web 资源打包进 APK，离线加载，适合日常手机刷题。
- **Android 原生 Compose 版**：Kotlin + Compose 原生实现，启动快、体积小，核心流程已可用。

---

### 快速导览

| 想了解什么 | 跳转 |
|-----------|------|
| 有哪些功能 | [当前能力](#当前能力) |
| 两个 APK 选哪个 | [Android 双版本说明](#android-双版本说明) |
| 怎么用 | [使用说明](#使用说明) |
| 支持什么导入格式 | [导入格式与策略](#导入格式与策略) |
| 怎么下载 | [下载与使用](#下载与使用) |
| 开发进度 | [原生 Compose 当前状态](#原生-compose-当前状态) |
| 怎么参与 | [参与贡献](#参与贡献) |

---

## 当前能力

### 刷题与考试

**练习模式**
- 支持随机抽题或题库顺序两种组题方式
- 单选题/多选题选项选择，判断题对错切换，填空/简答文本输入
- 提交后锁定选项，即时显示正确/错误反馈与解析
- 答错的题自动进入错题本
- 完成全部题目后展示总结：正确率、错题数、重新练习入口

**考试模式**
- 按题型自定义题目数量与分值，设置考试时长
- 实时倒计时，到时自动交卷
- 答题卡快速跳题，未答题目交卷前提醒
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
- 上传 `docx` 文件（推荐），也支持 `txt`、`json`、文字层 `pdf` 或粘贴纯文本
- 原生版支持 docx 内嵌图片提取，Web 版支持 PDF.js 解析
- **扫描件/图片型 PDF 暂不支持**

**双文件导入**
- 题目文件和答案文件分别上传，自动匹配题号
- 支持 “1-10：D A A B C…” 范围格式和 “1.D 2.A” 配对格式
- 答案文件缺失题号时按顺序自动对应

**识别与预览**
- 自动识别题号、题干、选项、答案、解析和题型
- 支持分区标题继承题型（如 “一、单选题” 下所有题自动归为单选）
- 识别结果预览：逐题查看题型、答案和异常标记
- 识别失败时可手动切换解析策略或调整文本后重试

**手动修正**
- 预览中可逐题修改题型、答案和题干
- 支持批量编辑和删除异常题目

**备份恢复**
- 全部数据一键导出为 JSON 备份文件
- 支持批量导出单个题库 JSON
- 恢复时可选合并或覆盖现有数据

### 多端支持

- Web 端功能完整，在线即可使用。
- Android WebView 壳版离线加载，适合日常刷题。
- Android 原生 Compose 版核心流程可用，体验更流畅。
- 内置 C1 科目一题库，方便首次体验。

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
- 功能快速迭代中，核心流程已可用

| | WebView 壳版 | 原生 Compose 版 |
|------|-------------|----------------|
| 稳定性 | 高 | 中 |
| 流畅度 | 一般 | 高 |
| 功能完整度 | 完整 | 核心流程已就绪 |
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

原生版已覆盖从导入到复盘的全流程，包含 13 个页面：

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
- `importer/`：解析器链、双文件合并、策略评分、验证
- `ui/components/`：GlassCard、ActionPillButton 等设计系统组件
- `ui/theme/`：Material3 主题、间距令牌、字体系统

---

## 使用说明

### Web 端快速上手

1. 打开 `apps/web/index.html`，或访问在线版。
2. 进入 **导入题库**，粘贴文本或上传文件。
3. 系统自动识别题型、选项、答案和解析。
4. 在识别预览中确认题目无误。
5. 进入 **刷题练习** 或 **考试模式** 开始使用。
6. 答错的题会进入 **错题本**。
7. 定期在 **设置/导出** 中导出备份。

### 数据备份建议

Shiroha Quiz 的题库和记录主要保存在本地浏览器或 App WebView 的本地存储中。

建议：

- **重要题库导入后，及时导出全部数据备份。**
- **换设备、清理缓存、卸载 App 前，务必先导出备份 JSON。**
- 从 Shiroha Quiz 导出的备份 JSON，应在 **设置/导出 → 导入配置 / 备份 JSON** 中导入。
- **备份 JSON、批量题库 JSON 不要放进普通题库导入区解析。**

---

## 导入格式与策略

支持：

```text
docx（推荐）
txt / json / 文字层 pdf
题目文件 + 答案文件双文件导入
```

系统会尝试识别：

```text
题号
题干
选项
答案
解析
题型
分区 / 分卷
```

导入后会进入识别结果预览。建议在确认导入前检查题型、答案、选项和异常提示。

详细说明：

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

最新发布版本：`v1.0.0-beta`（含 Web `v0.4.4-alpha` / 原生 `v0.2.7-native`）。详见 [Releases](https://github.com/reiqr/shiroha-quiz/releases)。

每次发布包含 Android APK 及相关说明文档。

> **当前为 beta 测试阶段，功能尚在完善中，不建议用于高风险正式考试场景。**

---

## 开发计划

详见 [下阶段开发计划](docs/下阶段开发计划.md)。

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
