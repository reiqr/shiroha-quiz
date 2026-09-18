<div align="center">

<img src="assets/promo4.jpg" width="800" alt="Shiroha Quiz">
</div>

<br>

# Shiroha Quiz

<img src="assets/shiroha_quiz_ui_assets_v2_cutout/illus_me_settings.webp" width="160" align="right" alt="看板娘" />

Shiroha Quiz 是一个轻量、开源的刷题工具，支持自导入题库、练习、考试、与多端使用。

Shiroha Quiz 解决一个很实际的问题：

> 你手里有题库——Word、Excel、TXT、JSON，或者题目和答案分开的文件。但格式不统一，整理成本高，即使整理完了也只能翻看，没法真正做题。Shiroha Quiz 把它们自动识别导入，变成可练习、可考试、可错题复盘的个人题库。

当前项目主要包含两条公开使用线：

- **Web 版**：下载解压后本地运行，数据保存在自己的浏览器里；支持题库导入、刷题考试、错题复习、分组练习、数据备份与跨端互通，含 AI 辅助导入、扫描 PDF OCR 测试与单题 AI 解析追问，适合桌面端整理题库。
- **Android 原生版**：当前主推安装包，使用 Kotlin + Compose 原生实现，**AI 全功能**、**多空填空**、**背题/斩题**、**图片题**等原生体验。

> [!IMPORTANT]
> **更新节奏**：最近工作稍微闲了一些，应该会开始恢复更新。问题反馈仍欢迎提交。

<br style="clear:both">

---

## 快速开始

| 你的情况 | 下载什么 | 入口 |
| --- | --- | --- |
| 用手机（Android） | 原生版 APK（**主推**），文件名以 `-native-release.apk` 结尾 | [下载](https://github.com/reiqr/shiroha-quiz/releases/latest) |
| 电脑上（推荐） | Web ZIP，解压后本地运行，数据存在自己的浏览器里 | [下载](https://github.com/reiqr/shiroha-quiz/releases/latest) |
| 只是想先看看 | 在线版，免安装、免解压 | [打开在线版](https://reiqr.github.io/shiroha-quiz) |

> **版本号怎么看**：原生版与 Web 版各自独立编号，不是新旧关系。统一发布版 `v2.8.10-beta` 一次产出两个文件，文件名自带各自版本号：`Shiroha-Quiz-v0.9.11-native-release.apk`（原生）、`shiroha-quiz-web-v0.8.7-alpha.zip`（Web）。**下载认文件名即可。**

> **当前为 beta 测试阶段，功能尚在完善中，不建议用于高风险正式考试场景。** 使用前请阅读[数据备份建议](#数据备份建议)。

更多入口：

| 想做什么 | 入口 |
| --- | --- |
| 查看主要功能 | [当前能力](#当前能力) |
| 导入自己的题库 | [导入格式与策略](#导入格式与策略) |
| 原生版和 Web 版区别 | [原生版与 Web 版区别](#原生版与-web-版区别) |
| 使用说明 | [Web 版快速上手](#web-版快速上手) / [原生版快速上手](#原生版快速上手) |
| 提交问题或建议 | [参与贡献 / 提交反馈](#参与贡献--提交反馈) |

> 开发者入口（本地运行、回归测试、文档索引）见文末 [本地运行](#本地运行)、[测试与回归](#测试与回归)、[开发计划](#开发计划)。

---

## 原生版与 Web 版区别

<table>
  <tr>
    <th width="80">维度</th>
    <th>Android 原生版</th>
    <th>Web 版</th>
  </tr>
  <tr><td>使用形态</td><td>安装 APK（Android 8.0+，即 API 26+）</td><td>浏览器直接打开，免安装</td></tr>
  <tr><td>数据存储</td><td>本地 SharedPreferences</td><td>浏览器 LocalStorage</td></tr>
  <tr><td>离线能力</td><td>除 AI 与在线文档识别外可完全离线</td><td>离线扩展库需单独下载（约 88 MB）</td></tr>
  <tr><td>文档识别</td><td>MinerU 在线 OCR、docx 内嵌图片提取</td><td>PDF.js 文字层 PDF 解析、OCR 测试区</td></tr>
  <tr><td>平台特色</td><td>暗夜模式、平板侧边导航、系统返回键、Shiroha 模式、背题模式、斩题、智能复习</td><td>单题 AI 解析分层、OCR 结果导出 DOCX</td></tr>
  <tr><td>适用场景</td><td>手机日常刷题</td><td>电脑端题库清洗与快速体验</td></tr>
</table>

Releases 页面推荐下载原生版 APK（支持 **Android 8.0+**，即 API 26+）：

- Kotlin + Compose 纯原生实现，当前主要开发线
- 更美观、更易用：Material3 原生界面、流畅动画与手势、Design Token 视觉规范

---

## 当前能力

> 各小节内分「双端通用」与「原生版专属」两段；完整差异见[原生版与 Web 版区别](#原生版与-web-版区别)。内置 C1 科目一题库，方便首次体验。

### 刷题与考试

#### 练习模式

**双端通用**

- 支持随机抽题或题库顺序两种组题方式（组题偏好记忆为原生版）
- 单选题/多选题选项选择，判断题对错切换，填空/简答文本输入，**多空填空题**逐空输入
- 支持题目快速编辑：修改自动写回来源题库并同步刷新当前题
- 支持题目收藏：练习中一键收藏题目，在收藏页集中查看
- 支持顺序练习进度记忆：下次可从上次顺序练习进度继续（Web 版自动记录，原生版退出时可选保存当前位置）
- 提交后选项着色区分正误，顶部卡片可收起
- 答错的题自动进入错题本
- 完成全部题目后展示总结：正确率、错题数、重新练习入口

**原生版专属**

- 支持即时练习与批量练习：即时练习可选择”选择后立即判题”和”答对后自动下一题”，单选/判断题支持”选后自动下一题”，批量练习保留整组提交
- 支持**背题模式**：练习页直接显示答案与解析，不计入正确率、不加入错题本、不生成普通练习记录
- 支持选项打乱（练习+考试独立控制）：固定 A/B/C/D 标签仅随机内容映射，本次练习/考试内顺序不变
- 支持**斩题功能**：可把一眼会的题移出普通练习池，并在题库详情中集中管理和恢复
- 支持字号缩放：题干与选项字号可独立调整，紧凑选项模式减少卡片间距适合长题快速阅读

#### 考试模式

**双端通用**

- 实时倒计时，到时自动交卷
- 答题卡快速跳题，未答题目交卷前提醒
- 交卷后展示正确率和明细报告（按题型汇总得分为 Web 版）

**原生版专属**

- 按题型自定义题目数量与分值，设置考试时长，偏好自动记忆
- 支持滑动切题
- 考试不受背题模式和斩题功能影响：不会提前显示答案，也不会过滤已斩题

#### 错题本

**双端通用**

- 练习与考试中答错的题自动收录，记录最近出错时间和累计错误次数
- 错题本展示”错 X 次 / 对 Y 次”，用于保留进入错题本后的历史累计表现
- 错题可重新练习，连续答对 2 次后自动标记为已掌握
- 再次答错会清空连续答对次数，并回到未掌握状态
- 支持手动标记掌握 / 取消掌握

**原生版专属**

- 支持**错题作用域切换**：可按当前题库或全部题库筛选错题列表、首页统计和复习范围
- 支持按题库、题型和掌握状态筛选错题
- 支持**智能复习模式**：根据错题表现自动安排到期复习，首页同步待复习数量
- 标记掌握 / 取消掌握时不会篡改历史答对次数（Web 版取消掌握会截断答对数）
- 斩题与错题掌握互相独立：斩题用于移出普通练习池，错题掌握用于错题复习状态

#### 刷题记录

**双端通用**

- 每轮练习或考试生成一条独立记录
- 记录详情支持逐题复盘，查看每道题的作答与正误（Web 版每轮最多展示前 8 题）
- 按时间倒序排列，方便回顾学习轨迹

**原生版专属**

- 记录列表和详情均支持**只看错题筛选**

#### 多题型支持
- 单选题、多选题、判断题、填空题（含多空）、简答题

### 题库导入

#### 多格式支持
- 上传 `docx` 文件（推荐；旧版 `.doc` 需先在 Word 或 WPS 中另存为 `.docx`），也支持 `xlsx`/`csv` 表格（旧版 `.xls`/`.xlsm` 需先另存为 `.xlsx` 或 `.csv`）、`txt`、`json`、文字层 `pdf` 或粘贴纯文本
- 原生版支持 **docx 内嵌图片提取**，Web 版支持 PDF.js 解析文字层 PDF，并提供扫描 PDF OCR 测试兜底
- 原生版兼容 Web 导出的图片题 JSON：支持旧 Markdown base64 图片和新的 `images` 数组结构
- 扫描件/图片型 PDF 建议先在 Web 版 OCR 测试区转成可编辑文本或 DOCX，人工核对后再导入题库；OCR 依赖较大的离线语言包，[本地运行](#本地运行)后访问加载更快

#### 双文件导入
- 题目文件和答案文件分别上传，自动匹配题号
- 支持 “1-10：D A A B C…” 范围格式和 “1.D 2.A” 配对格式
- 答案文件缺失题号时按顺序自动对应

#### 识别与预览
- 自动识别题号、题干、选项、答案、解析和题型
- 兼容题型大小写和常见别名，如 `single`/`SINGLE`、`multiple`/`MULTIPLE`、`judge`/`JUDGE` 等
- 支持分区标题继承题型（如 “一、单选题” 下所有题自动归为单选）
- 原生版支持**共用题干 / 材料题兜底识别**，并可将集中答案解析区合并回对应题目；答案区后续正文恢复、解析内编号分步保护和医学缩写选项等边界已纳入外部回归
- 填空题关键词覆盖更广（空格、横线、括号内等），减少简答题误判
- 识别结果预览：逐题查看题型、答案和异常标记，核对筛选器按需显示
- 识别失败时可手动切换解析策略或调整文本后重试

#### 手动修正
- 预览中可逐题修改题型、答案和题干
- 文本编辑器支持查找/替换，可正则匹配批量修改导入原文
- 支持批量编辑和删除异常题目

#### 备份恢复
- 全部数据一键导出为 JSON 备份文件，Web 版与原生版导出格式互通，可相互导入
- 完整备份含错题本、收藏夹、学习记录，跨端互导时可自动恢复（原生 v0.8.1-native+）
- 原生版导出 ZIP 含图片素材，Web 版同样可直接导入并自动转换
- 支持批量导出单个题库 JSON
- 恢复时可选合并或覆盖现有数据

### AI 智能功能

- **接入 AI 后导入默认走 JSON 结构化路径**：AI 直接产出题干、选项、答案、解析等结构化字段，不再让清洗后的文本重新走一遍本地切块解析，因此不会被编号、代码片段、多行答案等特殊文本触发切分误判，**准确率明显高于纯文本切块解析，一般不会出问题**；只有结构化校验不通过时才退回标准文本重解析，并在预览中提示题量变化供人工核对。
- **Web 版 AI 辅助导入**：支持整理原文、核对导入结果、补全解析三类辅助；可按选中文本、题号范围或条件筛选处理，适合在桌面端做题库清洗。核对维度已与原生版同步，覆盖题型误判、选项缺失或粘连、答案越出选项范围、判断题与选择题互误、题干截断与答案标记残留、跨章节同号串题、页眉页脚与页码噪声、图片占位符破损、集中答案区错配等异常，并区分低风险 / 需确认 / 无法可靠修复三档建议。
- **Web 版 AI 单题解析与追问**：练习作答后或查看答案后按需开启。解析按题型分层生成——客观题说明正确项为什么成立、关键干扰项为什么不成立；填空题给出参考答案与概念或计算依据；简答题、案例分析、结构化面试题给出「参考作答 / 答题思路 / 答题要点」，且不虚构机构、姓名或真实事件。追问不再返回聊天式回复，而是产出**可独立保存到题库的完整解析草稿**，确认后写回，不改变题干、选项、答案和作答进度，存储失败保留草稿并回滚；追问记录按题目和本轮练习隔离。当前只发送文字，图片题须人工核对。
- **原生版 AI 核对 / 补解析（导入页）**：导入预览中可按异常题、缺解析题、题号范围或当前筛选结果批量处理，并显示批次进度。
- **原生版 AI 补解析（编辑器）**：题库编辑、审阅、导入预览、快速编辑等入口统一集成，一键生成解析建议；导入页 AI 重构默认走结构化优先路径（见本节首条）。
- **原生版 AI 单题追问（练习页）**：练习中可围绕当前题继续追问，生成的解析可保存回题库，并同步当前练习、错题本和收藏夹中的题目副本。
- **原生版 AI 临时参考答案**（默认关闭）：练习题缺少题库答案时，提交后临时向 AI 取一次参考答案用于本次判分，不写回题库、不计入错题本；关闭时保持原有判分行为不变。
- 支持 DeepSeek、OpenAI 兼容接口和自定义接口，可配置 API 地址、API Key 与模型名称；Web 版也可配置 Ollama / LM Studio 等本机 OpenAI 兼容服务。**AI 功能需自备 API Key，费用由所选服务商按用量收取；不使用 AI 时，刷题、考试、错题本等功能完全不受影响。**
- AI 结果仅作辅助参考。涉及答案、题型和解析的写入都应经过用户确认，不建议把不确定答案交给 AI 编造。
- **隐私提示**：使用 AI 功能时，当前题目文本会发送到你配置的 AI 服务提供商（DeepSeek / OpenAI / Ollama 等）。API Key 不会写入源码、备份或打包文件，但请勿将敏感内容用于 AI 处理；本地 Ollama / LM Studio 可完全离线运行。

---

## 使用说明

### Web 版快速上手

1. 从 [Releases](https://github.com/reiqr/shiroha-quiz/releases/latest) 下载 Web ZIP 解压，打开 `index.html` 即可使用（OCR 等依赖 Worker 的功能需执行 `npx serve .` 后访问）；也可以先用[在线版](https://reiqr.github.io/shiroha-quiz)试，但两者数据不互通。
2. 进入 **导入题库**，粘贴文本或上传文件。
3. 系统自动识别题型、选项、答案和解析；扫描 PDF 可先在 **OCR 测试区** 转成文本或 DOCX。
4. 在**识别预览**中确认题目无误。
5. 进入 **刷题练习** 或 **考试模式** 开始使用。
6. 答错的题会进入 **错题本**。
7. 定期在 **设置/导出** 中导出备份。

### 原生版快速上手

1. 安装 `*-native-release.apk`，进入**首页**查看当前题库和学习状态。
2. 在 **导入** 页面上传 `docx`、表格、`txt`、`json` 或粘贴文本。
3. 在**核对页**检查题型、答案、解析和异常标记，必要时用全文编辑或 AI 核对辅助清洗。
4. 在 **练习** 中选择普通练习、即时反馈、自动下一题或背题模式。
5. 遇到一眼会的题，可以开启**斩题功能**，把它移出普通练习池。
6. 答错的题会进入 **错题本**，连续答对 2 次后自动标记为已掌握。
7. 在 **记录** 中复盘每轮练习或考试的逐题结果。

### 数据备份建议

Shiroha Quiz 的题库和记录保存在本地存储中（Web 版使用浏览器 LocalStorage，原生版使用 SharedPreferences）。

Web 版的存储按来源隔离：**在线版、本地解压版（`file://`）、本地服务版（`localhost`）是三套互不相通的存储**，在它们之间切换会看不到原来的题库；清缓存或换浏览器也会清空。建议固定一种打开方式，并定期导出备份。

建议：

- **重要题库导入后**，及时导出全部数据备份。
- **换设备、清理缓存、卸载 App 前**，务必先导出备份 JSON 或 ZIP。
- **备份导入位置**：设置/导出 → 导入配置 / 备份 JSON/ZIP。
- Web 版导出的 **JSON** 可直接导入原生版；原生版导出的 **ZIP** 也可导入 Web 版，含图片题库完全互通。
- 原生版会兼容题型大小写差异和图片字段差异，减少跨端导入时的题型丢失与 base64 文本外露。
- **备份 JSON、批量题库 JSON** 不要放进普通题库导入区解析。
- **云备份**：可把题库备份到自己的 WebDAV 云盘，支持自动备份与云端恢复，详见 [WebDAV 云备份使用指南](docs/WebDAV云备份使用指南/Shiroha%20Quiz%20WebDAV%20云备份使用指南.md)。

---

## 导入格式与策略

支持格式：`docx`（推荐）、`xlsx`/`csv` 表格（旧版 `.xls`/`.xlsm` 需先另存为 `.xlsx` 或 `.csv`）、`txt`、`json`、粘贴纯文本、题目+答案双文件导入，也可辅助解析文字层 `pdf`；Web 版提供扫描 PDF OCR 测试，原生版额外加强共用题干、材料题、集中答案解析区等边界识别。

导入流程：上传/粘贴 → 自动识别题号、题干、选项、答案、解析、题型、分区/分卷 → 进入识别预览逐题确认 → 导入题库。也可先用 AI 辅助整理、核对或补全解析。

详细说明：

- [所有支持的题库导入格式](docs/题库导入格式支持说明/Shiroha_Quiz_题库导入格式支持说明.md)
- [题库导入策略与使用指南](docs/题库导入策略与使用指南/Shiroha%20Quiz%20题库导入策略与使用指南.md)
- [题目导入解析方法说明](docs/题目导入解析方法说明/Shiroha%20Quiz%20题目导入解析方法说明.md)
- 标准题库格式示例：[Markdown](docs/标准题库格式示例/标准题库格式示例.md) / [Word](docs/标准题库格式示例/标准题库格式示例.docx) / [PDF](docs/标准题库格式示例/标准题库格式示例.pdf) / [Excel 通用模板](docs/标准题库格式示例/Shiroha_Quiz_Excel题库导入通用模板.xlsx)

如果原题库格式比较混乱，可以先做一次格式清洗再导入：

- **清洗工具**：国内用户可优先用 DeepSeek、GLM / 智谱清言；能稳定使用海外模型的话，ChatGPT、Claude 也可以。
- **隐私边界**：风险来自「把题库上传到外部 AI 工具清洗」，不是 Shiroha Quiz 本地导入本身。请勿上传敏感或内部题库。
- **清洗原则**：只统一题号、选项、答案和解析格式；不改题意、不编造答案，无法确认的答案标为「待确认」。
- **仍不准确时**：可按 JSON 题库格式组织题目——结构化键值不会因为部分文字触发切分失误，详见 [JSON 题库导入说明](docs/题库导入格式支持说明/Shiroha_Quiz_题库导入格式支持说明.md#format-21)。

---

## 下载与使用

下载入口：

- [GitHub Releases](https://github.com/reiqr/shiroha-quiz/releases)
- [在线体验](https://reiqr.github.io/shiroha-quiz)

最新版本请以 [GitHub Releases](https://github.com/reiqr/shiroha-quiz/releases) 为准。本次统一发布版为 `v2.8.10-beta`，版本号关系见文首[快速开始](#快速开始)，三条版本线的编号规则见 [Git Release 操作速查](docs/universal/Git-Release-操作速查.md)。

`v0.9.x-native` 系列近期重点：

- **多空题备选答案不限数量**：解除 3 个上限，支持任意数量并逐项删除
- **AI 密钥加密存储**：API Key 改为 Keystore 加密存储，升级时自动迁移旧明文
- **文档识别**：MinerU 在线 OCR 解析扫描版 PDF，免费/精准双模式，图片提取与人工绑定
- **WebDAV 云备份**：备份到自己的云盘，支持自动备份与覆盖恢复
- **AI 导入重构改为结构化优先**：AI 直接产出结构化题目字段，不再依赖文本切块，避免特殊文本导致的解析错题；校验不通过自动退回标准文本重解析并提示题量变化
- **AI 导入核对与补解析**：批次任务、范围选择、进度面板
- **AI 单题追问**：会话式交互、解析保存到题库
- **无答案题 AI 临时参考**：题库缺答案时临时取一次参考答案判分，不写回题库、不进错题本
- **答案区恢复与防截断修复**：答案区后续正文恢复、解析分步编号不误切
- **图片题支持**：纯图片题干与图片选项识别
- **练习答题卡升级** 与 **错题本状态记忆**

此外已完成多空填空题全链路、CodeLikeTextGuard 代码表达式守卫、分组练习范围、平板侧边导航、判分归一化、跨端互通和解析器外部回归增强。

`v0.8.x-alpha` Web 版重点：

- **WebDAV 云备份**：备份到自己的云盘，含 SHA-256 校验与合并/覆盖恢复
- **WebDAV 混合内容处理修复**：不再提前阻止 HTTPS 页面请求 HTTP 服务，改为失败后提示可能的混合内容拦截
- **AI 辅助导入**：整理/核对/补解析三维度，核对维度与原生版同步
- **AI 导入重构改为结构化优先**：AI 直接产出结构化题目字段，不再依赖文本切块，避免特殊文本导致的解析错题
- **单题 AI 解析按题型分层**：客观题讲清正确项与干扰项依据、填空给关键词与依据、简答与面试题给答题要点
- **单题 AI 追问产出完整解析草稿**：追问返回可独立保存的解析而非聊天记录，确认后写回题库，练习轮次之间不串会话
- **AI 连接诊断面板**：错误分类/跨域排查
- **共用材料题干回填** 与 **选中文本与题号范围处理**
- **导入大文件预警** 与 **原生兼容 ZIP 导出**

`v0.8.0-alpha` 起提供 OCR 扫描件兜底、MathJax 3.2.2 本地化、PDF.js 完整版和 LaTeX 公式防选项误判。

每次发布包含 Android APK 与 Web ZIP。

Web ZIP **不含离线扩展库**（PDF.js 完整包、MathJax、Tesseract OCR，合计约 **88 MB**）。解压后运行 `libs/` 下对应的下载脚本按需获取，也可直接联网使用 CDN 兜底。详见 [额外附加功能说明.txt](apps/web/额外附加功能说明.txt)。

---


> 以下为开发者内容：目录结构、本地运行与测试。

## 仓库结构

```text
shiroha-quiz/
├── apps/
│   ├── web/                        # Web 版（纯静态页面，无需构建）
│   └── android/                    # Android 工程（web / native 两个 flavor）
│       └── app/src/native/         #   原生版源码
│           ├── ai/                 #     AI 客户端与提示词
│           ├── importer/           #     题库导入引擎（解析 / 评分 / 校验）
│           ├── document/           #     文档识别（MinerU 在线 OCR）
│           ├── security/           #     密钥加密存储（Android Keystore）
│           ├── state/              #     全局状态管理
│           ├── sync/               #     WebDAV 云备份
│           ├── ui/                 #     Compose UI
│           └── util/
├── docs/                           # 文档（完整索引见 docs/README.md）
│   ├── 标准题库格式示例/
│   ├── 题库导入格式支持说明/
│   ├── 题库导入策略与使用指南/
│   ├── 题目导入解析方法说明/
│   ├── WebDAV云备份使用指南/
│   ├── native/  universal/         #   原生开发文档 / 架构与发布文档
│   └── archive/                    #   历史计划归档
├── test/
│   ├── native-parser-regression/   # 解析器外部回归
│   ├── web-webdav/                 # Web 版 WebDAV 请求测试
│   └── web-ai-practice/            # Web 版 AI 练习测试
├── assets/                         # 宣传图与素材
└── CHANGELOG.md / CONTRIBUTING.md / LICENSE / README.md
```

### Android 工程结构

Android 工程通过 `productFlavors` 保留历史 WebView 壳与当前原生版本，共享同一 Gradle 项目：

| Flavor | 包名 | 技术路线 |
|---|---|---|
| `web` | `com.yiqiu.shirohaquiz` | WebView 加载本地 Web 资源 |
| `native` | `com.reqir.shirohaquiz` | Kotlin + Jetpack Compose + Material3 |

---

## 本地运行

### Web 版

`apps/web/` 是纯静态页面，无需构建。

```bash
# 方式一：直接打开
apps/web/index.html

# 方式二：本地静态服务
npx serve apps/web
```

在线版：[https://reiqr.github.io/shiroha-quiz](https://reiqr.github.io/shiroha-quiz)

### Android 端

进入 Android 工程目录：

```bash
cd apps/android
```

构建原生版：

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

项目有三层测试，分别覆盖解析逻辑、原生业务逻辑与 Web 版。

### 解析器外部回归（真实题库样例）

用真实题库验证导入解析逻辑是否被新改动误伤：

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

### 原生 JVM 单元测试

覆盖 DOCX 解码、多空题备选答案、AI 密钥存储、WebDAV 备份与云端恢复安全等：

```powershell
cd apps/android
.\gradlew.bat :app:testNativeDebugUnitTest
```

### Web 版测试

**WebDAV 请求测试**：`test/web-webdav/` 使用 Node 18+ 内置测试运行器，无需安装依赖。

```powershell
node --test test/web-webdav/request.test.cjs
```

**AI 练习回归**：`test/web-ai-practice/` 需要预先具备 Node、可被 `require('playwright')` 解析的 Playwright 包，以及已安装的 Chromium（或用 `--executable-path` 指定现有 Chromium / Edge）。脚本**不会自动安装依赖或下载浏览器**。

```powershell
node ./test/web-ai-practice/runner.cjs
```

说明文档：

- [测试体系整理说明](docs/native/测试体系整理说明.md)
- [解析器回归测试说明](docs/native/解析器回归测试说明.md)
- [外部回归测试包说明](test/native-parser-regression/README.md)

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

> 提交 Issue 时请选择正确的平台（原生版 / Web 版），方便快速定位问题。

详见：

- [CONTRIBUTING.md](./CONTRIBUTING.md)
- [CHANGELOG.md](./CHANGELOG.md)

---

## 贡献者

感谢以下贡献者对项目的帮助：

- [@jiesou](https://github.com/jiesou) —— 独立实现了 Web 练习页单题 AI 解析与追问（已采纳并加固）与 WebDAV 同步，保留作者署名
- [@anupamme](https://github.com/anupamme) —— 指出 AI API Key 明文存储问题，其最小修复改动已采纳，保留作者署名

---

## 许可证

本项目采用 `GPL-3.0` 开源。
