import com.yiqiu.shirohaquiz.importer.model.ImportResult
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.WarningLevel
import com.yiqiu.shirohaquiz.importer.parser.QuizImportParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val root: Path = Path.of("..").toAbsolutePath().normalize()
private val manifest: Path = root.resolve("manifest.json")
private val actual: Path = root.resolve("actual")

private enum class CaseMode {
    SINGLE,
    DUAL
}

private data class RegressionCase(
    val id: String,
    val mode: CaseMode,
    val sample: String? = null,
    val questionSample: String? = null,
    val answerSample: String? = null
)

fun main() {
    actual.createDirectories()
    val cases = readManifestCases()
    val completed = cases.map { case ->
        case.id to runCase(case)
    }

    val indexJson = buildString {
        append("{\n")
        append("  \"generatedAt\": \"").append(System.currentTimeMillis()).append("\",\n")
        append("  \"manifest\": \"manifest.json\",\n")
        append("  \"cases\": [\n")
        completed.forEachIndexed { index, item ->
            append("    {\"id\":\"").append(escape(item.first)).append("\",\"actual\":\"actual/")
                .append(escape(item.first)).append(".json\"}")
            if (index != completed.lastIndex) append(',')
            append('\n')
        }
        append("  ]\n")
        append("}\n")
    }
    actual.resolve("index.json").writeText(indexJson, Charsets.UTF_8)
}

private fun runCase(case: RegressionCase): ImportResult {
    return when (case.mode) {
        CaseMode.SINGLE -> runSingle(case)
        CaseMode.DUAL -> runDual(case)
    }
}

private fun runSingle(case: RegressionCase): ImportResult {
    val samplePath = case.sample ?: error("${case.id}: single 用例缺少 sample")
    val text = resolveUnderRoot(samplePath).readText(Charsets.UTF_8)
    return QuizImportParser.parseStandardText(text).also { result ->
        writeActual(case.id, "single-file", result)
    }
}

private fun runDual(case: RegressionCase): ImportResult {
    val questionSample = case.questionSample ?: error("${case.id}: dual 用例缺少 questionSample")
    val answerSample = case.answerSample ?: error("${case.id}: dual 用例缺少 answerSample")
    val questionText = resolveUnderRoot(questionSample).readText(Charsets.UTF_8)
    val answerText = resolveUnderRoot(answerSample).readText(Charsets.UTF_8)
    return QuizImportParser.parseDualText(questionText, answerText).also { result ->
        writeActual(case.id, "dual-file", result)
    }
}

private fun writeActual(id: String, mode: String, result: ImportResult) {
    val path = actual.resolve("$id.json")
    Files.createDirectories(path.parent)
    path.writeText(toActualJson(id, mode, result), Charsets.UTF_8)
}

private fun readManifestCases(): List<RegressionCase> {
    if (!manifest.exists()) {
        error("缺少 manifest.json：$manifest")
    }
    val text = manifest.readText(Charsets.UTF_8)
    val blocks = extractCaseObjects(text)
    if (blocks.isEmpty()) {
        error("manifest.json 中没有 cases 用例")
    }
    return blocks.map { block ->
        val id = requireStringField(block, "id")
        val modeText = readStringField(block, "mode")
        val sample = readStringField(block, "sample")
        val questionSample = readStringField(block, "questionSample")
        val answerSample = readStringField(block, "answerSample")
        val mode = parseCaseMode(id, modeText, sample, questionSample, answerSample)
        RegressionCase(
            id = id,
            mode = mode,
            sample = sample,
            questionSample = questionSample,
            answerSample = answerSample
        )
    }
}

private fun parseCaseMode(
    id: String,
    modeText: String?,
    sample: String?,
    questionSample: String?,
    answerSample: String?
): CaseMode {
    return when (modeText?.trim()?.lowercase()) {
        "single", "single-file", "standard" -> CaseMode.SINGLE
        "dual", "dual-file" -> CaseMode.DUAL
        null, "" -> {
            if (questionSample != null || answerSample != null || sample.orEmpty().contains(" + ")) {
                CaseMode.DUAL
            } else {
                CaseMode.SINGLE
            }
        }
        else -> error("$id: 不支持的 mode：$modeText")
    }
}

private fun extractCaseObjects(json: String): List<String> {
    val caseFieldIndex = json.indexOf("\"cases\"")
    if (caseFieldIndex < 0) return emptyList()
    val arrayStart = json.indexOf('[', startIndex = caseFieldIndex)
    if (arrayStart < 0) return emptyList()

    val objects = mutableListOf<String>()
    var depth = 0
    var objectStart = -1
    var inString = false
    var escaping = false
    var index = arrayStart + 1

    while (index < json.length) {
        val ch = json[index]
        if (inString) {
            when {
                escaping -> escaping = false
                ch == '\\' -> escaping = true
                ch == '"' -> inString = false
            }
        } else {
            when (ch) {
                '"' -> inString = true
                '{' -> {
                    if (depth == 0) objectStart = index
                    depth += 1
                }
                '}' -> {
                    depth -= 1
                    if (depth == 0 && objectStart >= 0) {
                        objects += json.substring(objectStart, index + 1)
                        objectStart = -1
                    }
                    if (depth < 0) error("manifest.json cases 结构异常")
                }
                ']' -> if (depth == 0) return objects
            }
        }
        index += 1
    }
    error("manifest.json cases 数组未闭合")
}

private fun requireStringField(jsonObject: String, name: String): String {
    return readStringField(jsonObject, name) ?: error("manifest 用例缺少字段：$name")
}

private fun readStringField(jsonObject: String, name: String): String? {
    val pattern = Regex("\\\"${Regex.escape(name)}\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
    val match = pattern.find(jsonObject) ?: return null
    return unescapeJsonString(match.groupValues[1])
}

private fun unescapeJsonString(value: String): String {
    return buildString {
        var index = 0
        while (index < value.length) {
            val ch = value[index]
            if (ch != '\\' || index == value.lastIndex) {
                append(ch)
                index += 1
                continue
            }
            val next = value[index + 1]
            when (next) {
                '\\' -> append('\\')
                '"' -> append('"')
                '/' -> append('/')
                'b' -> append('\b')
                'f' -> append('\u000C')
                'n' -> append('\n')
                'r' -> append('\r')
                't' -> append('\t')
                'u' -> {
                    val hex = value.substring(index + 2, index + 6)
                    append(hex.toInt(16).toChar())
                    index += 4
                }
                else -> append(next)
            }
            index += 2
        }
    }
}

private fun resolveUnderRoot(relativePath: String): Path {
    val path = root.resolve(relativePath).normalize()
    if (!path.startsWith(root)) {
        error("样例路径不能越过回归目录：$relativePath")
    }
    if (!path.exists()) {
        error("样例文件不存在：$relativePath")
    }
    return path
}

private fun toActualJson(id: String, mode: String, result: ImportResult): String {
    val typeCounts = result.questions.groupingBy { it.type.name }.eachCount().toSortedMap()
    return buildString {
        append("{\n")
        field("id", id, comma = true)
        field("mode", mode, comma = true)
        field("strategyName", result.strategyName, comma = true)
        append("  \"questionCount\": ").append(result.questions.size).append(",\n")
        append("  \"answeredCount\": ").append(result.questions.count { it.answer.isNotEmpty() }).append(",\n")
        append("  \"typeCounts\": {")
        typeCounts.entries.forEachIndexed { index, entry ->
            if (index > 0) append(", ")
            append("\"").append(escape(entry.key)).append("\": ").append(entry.value)
        }
        append("},\n")
        append("  \"warnings\": [\n")
        result.warnings.forEachIndexed { index, warning ->
            append("    {\"level\":\"").append(warning.level.name)
                .append("\",\"number\":\"").append(escape(warning.questionNumber.orEmpty()))
                .append("\",\"message\":\"").append(escape(warning.message)).append("\"}")
            if (index != result.warnings.lastIndex) append(',')
            append('\n')
        }
        append("  ],\n")
        append("  \"diagnostics\": {\n")
        append("    \"normalizedLength\": ").append(result.diagnostics.normalizedLength).append(",\n")
        append("    \"blockCount\": ").append(result.diagnostics.blockCount).append(",\n")
        append("    \"candidateCount\": ").append(result.diagnostics.candidateCount).append(",\n")
        append("    \"errorCount\": ").append(result.warnings.count { it.level == WarningLevel.ERROR }).append(",\n")
        append("    \"warningCount\": ").append(result.warnings.count { it.level == WarningLevel.WARNING }).append("\n")
        append("  },\n")
        append("  \"questions\": [\n")
        result.questions.forEachIndexed { index, question ->
            appendQuestion(question, index == result.questions.lastIndex)
        }
        append("  ]\n")
        append("}\n")
    }
}

private fun StringBuilder.appendQuestion(question: Question, last: Boolean) {
    append("    {\n")
    field("number", question.number, comma = true, indent = "      ")
    field("type", question.type.name, comma = true, indent = "      ")
    field("question", question.question, comma = true, indent = "      ")
    append("      \"optionCount\": ").append(question.options.size).append(",\n")
    append("      \"options\": {")
    question.options.forEachIndexed { index, option ->
        if (index > 0) append(", ")
        append("\"").append(escape(option.key)).append("\": \"").append(escape(option.text)).append("\"")
    }
    append("},\n")
    append("      \"answer\": [")
    question.answer.forEachIndexed { index, answer ->
        if (index > 0) append(", ")
        append("\"").append(escape(answer)).append("\"")
    }
    append("],\n")
    append("      \"blankAnswers\": [")
    question.blankAnswers.forEachIndexed { i, group ->
        if (i > 0) append(", ")
        append("[")
        group.forEachIndexed { j, ans ->
            if (j > 0) append(", ")
            append("\"").append(escape(ans)).append("\"")
        }
        append("]")
    }
    append("],\n")
    field("analysis", question.analysis, comma = true, indent = "      ")
    field("category", question.category, comma = false, indent = "      ")
    append("    }")
    if (!last) append(',')
    append('\n')
}

private fun StringBuilder.field(name: String, value: String, comma: Boolean, indent: String = "  ") {
    append(indent).append('"').append(name).append("\": \"").append(escape(value)).append('"')
    if (comma) append(',')
    append('\n')
}

private fun escape(value: String): String {
    return buildString {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}
