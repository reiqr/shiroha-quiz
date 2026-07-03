# Shiroha Quiz 原生解析器外部回归核对包

这个目录用于核对 Android 原生解析器稳定性，不改 App 源码、不改现有单元测试。它直接调用当前仓库里的原生解析器源码，把 `samples/` 的输入解析成 `actual/`，再和 `expected/` 对比。

## 使用原则

- 这里的样例只作为外部回归基准。
- 先用真实中文样例固定预期结果，再决定是否修改解析器。
- 如果后续解析器输出和 `expected/` 不一致，先判断是解析器误伤，还是预期需要更新。
- 当前目录可以整体删除，不影响 App 编译和现有测试。

## 核对重点

- 标准规范题应走简单路径，不被复杂策略干扰。
- 两选项单选题不能因为选项是“正确 / 错误”就自动变成判断题。
- 题干末尾答案标记应清洗掉，但不能清洗正文里的普通括号内容。
- 答案解析区只能作为答案与解析来源，不能被解析成新题。
- 简答题、填空题文本答案不能被逗号、顿号或字母开头误拆成选项答案。
- 双文件导入优先按题号匹配；题号不足时才考虑顺序兜底。
- 表格、分区、紧凑格式都应稳定输出题数、题型和答案覆盖率。
- 集中答案区后的真实题目应能恢复，解析正文中的编号分步不能被误切成独立题目。

## 目录结构

- `samples/`：导入原文样例。
- `expected/`：每个样例的预期解析结果摘要。
- `actual/`：每次运行生成的实际输出和报告，不建议提交到 Git。
- `manifest.json`：样例清单、运行模式、输入文件和主要风险说明；Runner 以它作为唯一用例来源。
- `runner/`：Kotlin JVM 小运行器，直接引用 `apps/android/app/src/native` 下的 parser/model/score/validate 源码。
- `tools/compare_regression.py`：对比 `actual/` 和 `expected/` 并生成报告。
- `run-external-regression.ps1`：一键运行脚本。


## manifest 用例清单

Runner 不再在 `RegressionRunner.kt` 里硬编码用例列表。新增、删除或调整样例时，优先只改 `manifest.json`。

单文件样例：

```json
{
  "id": "01_standard_single",
  "mode": "single",
  "sample": "samples/01_standard_single.txt",
  "expected": "expected/01_standard_single.json",
  "risk": "标准题不应被复杂策略误伤"
}
```

双文件样例：

```json
{
  "id": "05_dual_file",
  "mode": "dual",
  "questionSample": "samples/05_dual_questions.txt",
  "answerSample": "samples/05_dual_answers.txt",
  "expected": "expected/05_dual_file.json",
  "risk": "双文件导入应优先按题号合并"
}
```

约定：

- `id` 必须和 `expected/<id>.json`、`actual/<id>.json` 对应。
- `mode=single` 使用 `sample`。
- `mode=dual` 使用 `questionSample` 和 `answerSample`。
- 样例路径必须在 `test/native-parser-regression/` 目录内，Runner 会阻止路径越界。

## 一键运行

在当前目录执行：

```powershell
.\run-external-regression.ps1
```

脚本会先清理旧的 `actual/*.json` 和 `actual/REGRESSION_REPORT.md`，再重新生成结果。

### 环境要求

- **JDK 17+**（runner 编译依赖 JDK 17 工具链）
- **Windows 10+**（脚本使用 PowerShell + `gradlew.bat`）
- 首次运行需要联网下载 Gradle 和 Kotlin 依赖，之后走本地缓存
- **macOS / Linux**：无需 PowerShell 脚本，直接在 `runner/` 目录执行 `../../apps/android/gradlew run`

Gradle 路径解析顺序：

1. 优先使用环境变量 `SHIROHA_GRADLE` 指向的 `gradle.bat`。
2. 其次使用本机常用路径 `E:\codex\exercise\output\gradle-8.7\bin\gradle.bat`。
3. 最后回退到仓库内 `apps/android/gradlew.bat`。

如果你的 Gradle 不在上述路径，可以临时指定：

```powershell
$env:SHIROHA_GRADLE = 'E:\your\gradle\bin\gradle.bat'
.\run-external-regression.ps1
```

## 失败判定

Runner 内嵌了最小对比逻辑（题数、答案数、题型分布、警告数）。全部通过时正常结束；任一用例失败时打印 `RUNNER_FAIL` 并返回非 0 退出码，PowerShell / CI 会识别为失败。

如需更细粒度的逐题对比（stemContains、blankAnswers、选项内容），可额外运行：

```bash
python tools/compare_regression.py
```

报告输出位置：

- `actual/runner-summary.json`（Kotlin 内嵌对比）
- `actual/comparison-summary.json`（Python 深度对比）
- `actual/<case_id>.json`

失败报告会附带实际题目摘录，方便直接定位题型、答案、选项、题干和解析差异。

## 建议核对流程

1. 覆盖最新解析器补丁。
2. 执行 `run-external-regression.ps1`。
3. 查看控制台输出和 `actual/REGRESSION_REPORT.md`。
4. 如果失败，先看失败报告里的实际题目摘录，再打开对应 `actual/<case_id>.json`。
5. 只有确认预期合理后，才修改解析器逻辑或更新 `expected/`。

## Git 提交建议

建议提交：

- `README.md`
- `manifest.json`
- `run-external-regression.ps1`
- `samples/`
- `expected/`
- `tools/`
- `runner/src/`
- `runner/build.gradle`
- `runner/settings.gradle.kts`

不建议提交：

- `actual/`
- `runner/build/`
- `runner/.gradle/`

## v3 新增边界回归

- 紧凑多选答案 `AB`、`A,B`、`A B` 的等价归一化。
- 主观题中的 `A./B./C.` 分项、单字母文本答案及 `AB法`、`AI` 文本答案保护。
- 完整试卷前言、答题卡提示、考试时间和结束语过滤。
- 整卷主观题不得因答案为字母而转换为客观题。
- 无题目文本必须产生“未识别到任何题目”错误。
- 双文件密集整卷应在答案合并后再决定是否由整卷候选接管。
- “说明文字中……”等合法题干不得被前言规则误删。
## v4 新增代码表达式边界回归

- `ls.append(Da)`、`func(AB)`、`if (A)`、`run()` 等调用结构不能被识别为括号答案或填空。
- 字符串和注释中的 `(AD)`、`(B)`、`(C)` 不参与答案识别。
- `A.append(value)`、`A.id, B.name, C.value` 等代码成员访问不能被拆成标准或紧凑选项。
- `1.0 + 2.0`、点分数字数据不能被当成新的题号。
- 正常的 `(AD)` 括号答案、`f(x)=（ ）` 填空、标准选项和紧凑选项必须继续正常识别。

## v6 新增答案文件侧格式回归

- 双文件导入中，`题号: 1 2 3 / 答案: A B C` 双行答案表应按题号合并。
- 双文件导入中，`1-5: A B A C D` 范围答案应展开后合并到对应题目。
- 双文件导入中，`1A2B3C4D5A` 紧凑答案行应作为答案文件解析，不能污染题干。
- 双文件导入中，`1.A 2.B 3.C` 行内多条目答案应逐题合并。
- 这些用例覆盖的是答案文件侧能力，不代表单文件题库正文开头直接放答案表也一定可用。

## v7 新增标准解析边界回归

- 解析/分析文本里的 `选B`、`故选C` 等表达应能提取为客观题答案。
- 无 `A/B/C/D` 标记但结构明显、且有客观答案支撑的短行选项，可推断为选项。
- `问题一`、`问题二` 等中文题号应分块为主观题，并按实际题号输出。
- `1题干`、`2题干` 这类题号与题干粘连格式应能识别题号边界。
- `材料一`、`材料二` 等导语行不能被误判成独立题目。
- 题干开头直接内嵌答案如 `1. （C）题干` 当前暂不纳入支持范围。
- 主观题答案中的编号分项场景等待重新整理样例后再纳入已通过回归。

## v8 新增答案区恢复与防截断回归

- 答案解析区后继续出现真实题目时，应在小题号间隙内恢复正文解析，避免整卷被截断。
- 解析正文中的“正确答案为 B”“参考答案为 C”等说明应保留为当前题解析，不触发集中答案区。
- 真正的 `参考答案` / `答案解析` 区仍应作为答案区识别，不能因为防误判而失效。
- 纯答案表 `1.A 2.B 3.C` 不能被恢复成新题。
- 解析中的编号分步 `1. 先... / 2. 再...` 不能被误切成独立简答题。
- 大跨度非连续题号默认不轻易恢复，避免年份、页码或章节编号误触发。
- 主观题在答案区后再次出现，并且自带 `答案` / `参考答案` 标记时，应能恢复为真实题目。
- 医学缩写选项如 `IV:5°`、`ID:40°`、`IM:20°` 应保持为选项文本，不被拆坏。

