# Shiroha Quiz 后续功能开发计划 33-34

当前版本：原生 `v0.8.0-native`，Web `v0.6.0-alpha`。本阶段原生端完成 21 个版本迭代（v0.6.7~v0.8.0），Web 端完成 9 个版本迭代（v0.4.7~v0.6.0），统一发布推进至 v2.4.0-beta。

---

## 一、33 阶段实际完成（已落地，不得回退）

### 1. 导入解析

**原生端：**

- 解析器重构：选项检测逻辑从 `StandardQuestionParser` 移入 `CompactQuestionRepair` 集中管理
- JSON 导入全面兼容：多题库数组、题目数组、单题库对象（含 name/questions）、Web 备份包、对象选项（`{"A":"text"}`）
- 答案解析多别名兼容：`answer`/`answers`/`correctAnswer`/`correctAnswers`
- 图片标记保护：`protectImageMarkersForOptionSplit`，选项拆分不误入图片标记
- 英文单词内字母误判修复：`looksLikeOptionMarkerInsideAsciiWord`（Vitamin A）、`looksLikeDottedEnglishAbbreviation`（U.S.A.）
- 主观题防误判：`looksLikeSubjectivePrompt`、`looksLikeMisparsedObjectiveQuestion`，防止简答/填空被误拆为选择题
- 双文件合并顺序修复：各候选先合并答案再比较，避免提前淘汰
- 空结果兜底：无可用解析时生成 ERROR 级别警告
- 回归测试 12 用例积累（12/12，1 例待修复）

**Web 端（V33→V34）：**

- 原文区间局部修复（v58.9.10）：`locateQuestionBlocksV58910` 题目↔原文字节定位，`repairMissingAndStuckQuestionsBySourceV58910` 漏题补回/粘题拆分
- 过度拆题局部合并（v58.9.11）：`repairOverSplitQuestionsBySourceV58911`，真实题号锚点间合并多余题目
- 答案解析区分区感知匹配（v58.9.11）：`answerEntryMatchRankV58911` 卷/章/分组三级匹配权重
- 局部修复守卫与审计（v58.9.12）：`localRichContentPreservedV58912` 富内容保留、`scoreQuestionNumberContinuity` 只看原文真实题号、修复上限动态化
- 英文题干勘误（v58.9.8.2）：`isBareEnglishStemStartV5982`，"A vowel..." 不误判为选项
- 首页版本标签动态同步：`syncHomeVersionPromptV586`

### 2. 练习与考试

- 选项打乱（练习+考试独立控制）：固定 A/B/C/D 标签仅随机内容映射，同一场练习/考试内顺序稳定
- AI 单题分析：练习提交后或背题模式触发，AI 独立判题 + 答案对照 + 置信度标记 + 人工确认提示
- 编辑器 AI 补解析：`AiAnalysisFillPanel` 统一组件，题库编辑/审阅/导入预览/快速编辑四个入口集成
- 错题本作用域切换：当前题库/全部题库两种范围，影响列表、清空、首页统计、智能复习
- 记录筛选：记录列表 + 记录详情"全部/只看错题"
- 首页错题数按作用域显示

### 3. Web 功能增强

- 收藏夹升级为独立导航页面
- 练习/错题自定义题量（下拉+输入框联动）
- 导入预览工具栏响应式排版
- 导出流程分离：题库管理页只含题库、设置页处理完整备份

### 4. 后台化（content-admin 分支，本地提交）

| 里程碑 | 内容 | 状态 |
|--------|------|------|
| M1 | Question 模型 11 云端字段，JSON 兼容 | ✅ |
| M2 | 服务端用户认证（bcrypt + JWT） | ✅ |
| M3 | 题库/任务/作答/统计基础表（SQLite + WAL） | ✅ |
| M4 | 文件上传（multipart 解析） | ✅ |
| M5 | 管理面板题库导入、任务下发、作答模拟 | ✅ |
| M6 | 后台查看用户错题和作答统计 | ✅ |
| M7 | 使用端 API、用户组发布、回传幂等 | ✅ |
| — | 服务端 JSON 选项对象格式兼容 | ✅ |
| M8 | Android 使用端登录并拉取云端任务 | ⬜ |
| M9 | Android 使用端答题记录回传 | ⬜ |
| M9.5 | 管理员端 Word/PDF AI 切题与人工核对 | ⬜ |
| M10 | 统计看板增强 | ⬜ |
| M11 | 标签组题筛选 | ⬜ |

### 5. 工程

- 清理未启用的 monorepo 骨架（pnpm-workspace.yaml / package.json / packages/）
- 回归测试 21 用例积累，全部通过
- 紧凑答案归一化修复（13）：`embeddedChoiceLetterPattern` 扩展支持 (A,B)/(A B)
- 前言过滤（17/18）：`pureFrontMatterLineRegex` 阻止卷首语入题块，`isPureFrontMatterPseudoQuestion` 最终守卫
- 英文缩写防误判回归用例（21）：Vitamin A / U.S.A. / e.g.
- 空结果报告：无可用解析时产出 ERROR 级别警告

**跨端数据互通：**

- Web 导出适配 Native 格式：`buildNativeInteropStateV24`，完整备份含 `kind:shiroha_quiz` 互通字段
- Web 导入兼容 Native 格式：识别 `kind`，数组→对象映射（3 个转换函数）
- Native 导入兼容 Web 格式：`crossPlatform` 包装解包、ISO 时间字符串解析、状态自动映射
- 备份格式升级 v3：`crossPlatformSchemaVersion` / `exportedBy` 等元数据

---

## 二、当前阶段定位

33 阶段结束时，原生 Compose 版已覆盖完整的导入→练习→考试→错题→复习→AI 辅助闭环，Web 端解析架构从 V33 升级至 V34。

34 阶段的重点是：

1. **解析稳定性收口**：修复已知回归，收紧极端格式兼容
2. **跨端数据互通**：错题本/收藏夹/记录实现标准化跨端互导
3. **后台化推进**：Android 端接入后台
4. **工程基础设施**：测试与 CI

---

## 三、34 阶段目标

### 目标 1：解析稳定性收口

| 编号 | 内容 | 状态 |
|------|------|------|
| 1.1 | 修复紧凑多选答案提取 + 前言/注意事项过滤 | ✅ 21/21 |
| 1.2 | 补充英文缩写防误判回归用例（21号） | ✅ |
| 1.3 | 大题库（500+ 题）导入性能摸底与优化 | ⬜ |

### 目标 2：跨端数据互通（以 Native 为标准）

| 编号 | 内容 | 状态 |
|------|------|------|
| 2.1 | Web 导出适配 Native 格式 | ✅ |
| 2.2 | Web 导入兼容 Native 格式 | ✅ |
| 2.3 | Native 导入兼容 Web 格式 | ✅ |

### 目标 3：后台化推进

| 编号 | 内容 |
|------|------|
| 3.1 | M8：Android 使用端登录界面 + 拉取云端任务 |
| 3.2 | M9：Android 使用端答题记录回传 |

### 目标 4：工程基础设施

| 编号 | 内容 |
|------|------|
| 4.1 | 解析器单元测试框架搭建 |
| 4.2 | CI 集成回归测试自动化 |

---

## 四、建议开发顺序

1. **1.1** — 修复回归测试，确保解析重构零回归
2. **2.1-2.3** — 跨端数据互通（改动量小，用户价值高）
3. **3.1-3.2** — 后台化 Android 接入
4. **4.1-4.2** — 测试与 CI

---

## 五、开发约束

1. **默认开发范围** — 原生 `apps/android/app/src/native/`，Web `apps/web/`
2. **最小修改原则** — 不顺手重构，不引入无关变更
3. **不回退既有功能** — 已完成能力清单不得破坏
4. **标准解析优先** — 兜底策略不能污染标准格式
5. **后台化本地提交** — admin/server 只在 `content-admin` 分支本地提交
6. **标签保护** — 永远不删除 Git 标签，只删除 Draft 草稿 Release
