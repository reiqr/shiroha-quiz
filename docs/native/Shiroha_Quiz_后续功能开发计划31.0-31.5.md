# Shiroha Quiz 原生 Android / Compose 版 31.0～31.5 开发记录

> 说明：本文是 v0.4.0 至 v0.4.3 期间的功能开发回顾记录。  
> 当前项目状态请优先参考根目录 `README.md`、`CHANGELOG.md`。

## 一、阶段概述

31.0～31.5 阶段时间跨度约 2 天（2026-05-16～17），围绕 **导入核对、素材整理、跨端互通、交互优化** 四个方向进行开发。

本阶段延续 30.5～31 刷题体验交付后的节奏，将重心转到导入流程质量、视觉一致性和跨端数据互通。

---

## 二、开发范围

本阶段仅处理原生 Android / Compose 版：

```text
apps/android/app/src/native/
```

涉及但不影响：

```text
apps/web/
apps/android/app/src/main/assets/web/
packages/
```

---

# 三、31.0：导入编辑器与题型识别优化（v0.4.0）

## 1. 查找 / 替换功能

在导入文本编辑器中新增全文查找/替换：

```text
普通文本匹配
正则表达式匹配
替换内容留空则删除匹配文本
替换前预览模式提示
```

入口：编辑页顶部工具栏「查找」按钮。

## 2. 填空题识别优化

新增填空关键词覆盖：

```text
空白 | 空格 | 横线 | 横线上 | 括号内 | 括号里
```

移除简答题误判逻辑：
- 删除 `shortKeywords` 正则（简答/问答/面试等关键词不再触发题型推断）
- 删除按答案长度 ≤20 字判断为填空题的规则
- 无选项、无填空特征的题目默认按简答处理

## 3. 核对页筛选器改进

```text
筛选分类只在有结果时显示（计数为 0 的不渲染）
选中空分类时自动回退到"全部"
筛选器分组按每 5 题一页展示
```

## 4. 31.0 产出

| 版本 | 内容 |
|------|------|
| `v0.4.0-native` | 查找替换、填空识别优化、筛选器改进 |

---

# 四、31.1：单题编辑与分区识别（v0.4.1）

## 1. 分区来源识别与重复题号检测

```text
增强 SectionTitleParser 的分区来源记录
按分区 + 题型维度判断重复题号
不同分区内相同题号不视为重复
```

## 2. 单题编辑页

在核对流程中新增独立单题编辑页：

```text
编辑入口：筛选列表项和题目详情均可进入
编辑页前置显示本题提示（数量、当前状态）
编辑页前置显示 AI 核对/解析建议
编辑返回后自动聚焦筛选列表中当前位置
滚动联动：BringIntoViewRequester 定位当前题所在分组
```

## 3. 31.1 产出

| 版本 | 内容 |
|------|------|
| `v0.4.1-native` | 分区识别、重复题号检测、单题编辑页、聚焦联动 |

---

# 五、31.2：交互组件与批量编辑（v0.4.2）

## 1. ShirohaClick 组件

新增统一按压反馈组件 `ShirohaClick`：

```text
替代原有的 shirohaNoRippleClickable
使用标准 Compose clickable，兼容 Material3 水波纹
全局应用于按钮、筛选芯片、列表项
```

## 2. 批量编辑页返回优化

批量编辑页面（全文编辑器）交互改进：

```text
编辑回退后视觉中心点自动定位
保存按钮独立为胶囊组件 EditorSaveButton
底部工具栏重新排列（查找 + 保存）
```

## 3. 筛选列表缩放

```text
收紧当前筛选列表展示数量，提升移动端核对效率
优化分页定位，按每 5 题一组展示
EditText 从筛选列表进入编辑后，返回时自动聚焦该位置
```

## 4. 31.2 产出

| 版本 | 内容 |
|------|------|
| `v0.4.2-native` | ShirohaClick、批量编辑视觉优化、筛选列表缩放 |

---

# 六、31.3：素材与图标系统整理（v0.4.3）

## 1. 双图标统一至 native 源集

原 Shiroha 图标在 `src/main/res/`（共享源集），默认图标在 `src/native/res/`，管理分散。

整理后：

```text
Shiroha 图标：native/res/mipmap-anydpi-v26/ic_launcher_shiroha.xml
             → drawable-nodpi/ic_launcher_shiroha_image.webp
默认图标：native/res/mipmap-anydpi-v26/ic_launcher_default.xml
          → drawable-nodpi/ic_launcher_default_image.webp
AndroidManifest 中 ShirohaLauncher 引用改为 @mipmap/ic_launcher_shiroha
```

两套图标均在 `native/` 源集内独立管理，main/ 源集不再参与。

## 2. 插画文件重命名

```text
illus_home_welcome_v2.webp → illus_home_welcome.webp（首页）
illus_home_welcome_webp.webp → illus_me_settings.webp（设置页）
```

消除 `_v2`、`_webp` 等历史后缀歧义。同步更新 `HomeScreen.kt` 和 `MeScreen.kt` 引用，以及 `assets/` 设计源文件。

## 3. WebP 素材净化

```text
全部插画重新编码为 WebP q70
暗夜模式下图片素材无白色边框（背景透明）
ic_launcher_default_image.png（796KB）→ .webp（480KB → 重新采样）
```

## 4. 31.3 产出

| 版本 | 内容 |
|------|------|
| `v0.4.3-native` | 双图标统一、插画重命名、WebP 净化 |

---

# 七、31.4：跨端数据互通

## 1. 题库 JSON/ZIP 互通

```text
原生端导出 ZIP（backup.json + assets/ 图片目录）
Web 端可导入 ZIP，自动将 assets 图片转为内嵌 base64 data URL
Web 端导出的 JSON（含 data:image/...;base64 内嵌图片）
原生端导入时自动提取 base64 图片写入本地文件
两端题库 JSON 结构完全兼容
```

## 2. 图片处理管道

```text
convertEmbeddedDataImages()：题干中 Markdown 图片语法识别
  ↓
Base64 解码 → 写入 assets 目录
  ↓
宽高读取 → 注册到 Question.images
  ↓
原占位符替换为【alt】
```

## 3. Web 端 ZIP 导入

```text
readNativeZipBackupPayloadV8216()：解压 ZIP，读取 backup.json
attachNativeZipAssetsToPayloadV8216()：assets 图片转 data URL
图片选项题不触发"缺少选项"警告
```

## 4. 响应式图片样式

Web 端题目图片自适应：

```text
.question-image：max-width: 100%; max-height: min(68vh, 720px)
移动端：max-height: min(58vh, 520px)
圆角 14px + 浅色背景
导入预览中尺寸更紧凑（260px 限高）
```

## 5. 31.4 产出

| 功能 | 位置 |
|------|------|
| 图片互通 | `QuizRepository.kt` convertEmbeddedDataImages() |
| ZIP 导入 | `app.js` readNativeZipBackupPayloadV8216() |
| 响应式样式 | `app.js` ensureQuestionMediaResponsiveStylesV8217() |

---

# 八、31.5：文档与工程整理

## 1. 文档更新

```text
CHANGELOG.md：补充 v0.4.0～v0.4.3 变更记录
README.md：补充查找替换、填空题识别、宣传图更新、Excel 格式
packages/：更新 shared（版本号/Schema）、parser（详细解析流程）、
          types（QuestionImage/WrongBookEntry）、ui（Shiroha 模式/双图标）
docs/native/：更新组件与状态说明（ShirohaClick/模式/图标）
docs/下阶段开发计划.md：重写当前状态与实际下一步
```

## 2. 宣传图优化

```text
promo.png 更新为 2x Retina 分辨率（2172x724）
README 引用切换到 promo4.jpg
保留历史版本不删除
```

## 3. 编译脚本修复

```text
source-upload.ps1：UTF-8 BOM + chcp 语法修复 + 中文输入切换 GBK 避免闪退
```

## 4. 四角色智能体配置

```text
探码（Explore） → 变更范围分析
质检（code-reviewer） → 代码质量
安全（security-engineer） → 漏洞扫描
文档（technical-writer） → CHANGELOG/README
```

配合并行发布工作流：编译 + 文档生成同时进行。

## 5. 31.5 产出

| 类型 | 内容 |
|------|------|
| 文档 | CHANGELOG / README / packages / 开发计划同步 |
| 宣传 | 高清宣传图切换 |
| 工具 | 编译脚本修复、GitHub MCP 配置 |
| 流程 | 四角色智能体 + 并行发布工作流 |

---

# 九、版本线对照

| 版本 | 日期 | 对应章节 | 核心功能 |
|------|------|---------|---------|
| `v0.4.0-native` | 05-16 | 31.0 | 查找替换、填空优化、筛选器 |
| `v0.4.1-native` | 05-16 | 31.1 | 分区识别、单题编辑、聚焦联动 |
| `v0.4.2-native` | 05-17 | 31.2 | ShirohaClick、批量编辑优化 |
| `v0.4.3-native` | 05-17 | 31.3 | 双图标、插画重命名、WebP 净化 |
| — | 05-16～17 | 31.4 | 跨端 JSON/ZIP 互通、图片处理 |
| — | 05-17 | 31.5 | 文档、宣传图、工具链整理 |
