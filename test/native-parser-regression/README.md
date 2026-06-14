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

Gradle 路径解析顺序：

1. 优先使用环境变量 `SHIROHA_GRADLE` 指向的 `gradle.bat`。
2. 其次使用仓库内 `apps/android/gradlew.bat`。
3. 最后回退到本机常用路径 `E:\codex\exercise\output\gradle-8.7\bin\gradle.bat`。

如果你的 Gradle 不在上述路径，可以临时指定：

```powershell
$env:SHIROHA_GRADLE = 'E:\your\gradle\bin\gradle.bat'
.\run-external-regression.ps1
```

## 失败判定

`tools/compare_regression.py` 现在会在存在失败用例时返回非 0 退出码。也就是说：

- 全部通过：脚本正常结束。
- 任一用例失败：PowerShell / CI 会识别为失败。

报告输出位置：

- `actual/REGRESSION_REPORT.md`
- `actual/comparison-summary.json`
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
