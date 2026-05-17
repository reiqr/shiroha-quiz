package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import java.util.Locale

/**
 * Parses common quiz-bank spreadsheet exports after TextImportDecoder has converted
 * .xlsx/.csv content into delimited plain text. Keep this parser focused on table
 * structure; rich-text symbols/subscripts are handled by TextImportDecoder.
 */
object ExcelQuestionTableParser {
    private const val MAX_HEADER_SCAN_ROWS = 12
    private const val MAX_OPTION_COLUMNS = 7

    private data class HeaderMapping(
        val headerIndex: Int,
        val numberColumn: Int? = null,
        val typeColumn: Int? = null,
        val stemColumn: Int,
        val optionColumns: Map<String, Int> = emptyMap(),
        val combinedOptionsColumn: Int? = null,
        val answerColumn: Int? = null,
        val analysisColumn: Int? = null,
        val categoryColumn: Int? = null,
        val scoreColumn: Int? = null
    ) {
        fun copyWithHeaderOffset(offset: Int): HeaderMapping = copy(headerIndex = headerIndex + offset)
    }

    fun parse(text: String): List<Question> {
        val rows = parseDelimitedRows(text)
            .map { row -> row.map { cleanupCell(it) } }
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.size < 2) return emptyList()

        val questions = mutableListOf<Question>()
        var searchStart = 0
        var fallbackNumber = 1

        while (searchStart < rows.size) {
            val mapping = findHeaderMapping(rows.drop(searchStart))?.copyWithHeaderOffset(searchStart) ?: break
            var rowIndex = mapping.headerIndex + 1
            while (rowIndex < rows.size) {
                val nextMapping = buildHeaderMapping(rows[rowIndex], rowIndex)
                if (nextMapping != null) break
                val row = rows[rowIndex]
                if (!isIgnorableRow(row)) {
                    parseQuestionRow(row, mapping, fallbackNumber)?.let { question ->
                        questions += question
                        fallbackNumber += 1
                    }
                }
                rowIndex += 1
            }
            searchStart = rowIndex.coerceAtLeast(mapping.headerIndex + 1)
        }

        return questions
    }

    private fun parseQuestionRow(row: List<String>, mapping: HeaderMapping, fallbackNumber: Int): Question? {
        val stem = row.cell(mapping.stemColumn).trim()
        if (stem.isBlank() || isRepeatedHeaderRow(row, mapping)) return null

        val rawType = mapping.typeColumn?.let { row.cell(it) }.orEmpty()
        val explicitType = parseQuestionType(rawType)
        val number = mapping.numberColumn?.let { row.cell(it) }?.trim()?.takeIf { it.isNotBlank() }
            ?: fallbackNumber.toString()
        val category = mapping.categoryColumn?.let { row.cell(it) }?.trim().orEmpty()
        val analysis = mapping.analysisColumn?.let { row.cell(it) }?.trim().orEmpty()
        val rawAnswer = mapping.answerColumn?.let { row.cell(it) }?.trim().orEmpty()
        val score = mapping.scoreColumn?.let { row.cell(it) }?.trim()?.toDoubleOrNull()

        val optionMap = linkedMapOf<String, String>()
        mapping.optionColumns.toSortedMap().forEach { (key, index) ->
            val text = cleanupOptionText(row.cell(index), key)
            if (text.isNotBlank()) optionMap[key] = text
        }
        mapping.combinedOptionsColumn?.let { index ->
            parseCombinedOptions(row.cell(index)).forEach { option ->
                optionMap.putIfAbsent(option.key, option.text)
            }
        }
        val options = optionMap.map { (key, value) -> Option(key, value) }
        val type = inferQuestionType(
            explicitType = explicitType,
            stem = stem,
            options = options,
            answerText = rawAnswer
        )
        val normalizedOptions = normalizeOptionsForType(options, type)
        val answer = normalizeAnswer(rawAnswer, type)

        return Question(
            number = cleanupNumber(number),
            type = type,
            question = stem,
            options = normalizedOptions,
            answer = answer,
            analysis = analysis,
            category = category,
            score = score
        )
    }

    private fun findHeaderMapping(rows: List<List<String>>): HeaderMapping? {
        val scanCount = rows.size.coerceAtMost(MAX_HEADER_SCAN_ROWS)
        for (index in 0 until scanCount) {
            buildHeaderMapping(rows[index], index)?.let { return it }
        }
        return null
    }

    private fun buildHeaderMapping(header: List<String>, headerIndex: Int): HeaderMapping? {
        var numberColumn: Int? = null
        var typeColumn: Int? = null
        var stemColumn: Int? = null
        var combinedOptionsColumn: Int? = null
        var answerColumn: Int? = null
        var analysisColumn: Int? = null
        var categoryColumn: Int? = null
        var scoreColumn: Int? = null
        val optionColumns = linkedMapOf<String, Int>()

        header.forEachIndexed { index, raw ->
            val normalized = normalizeHeader(raw)
            if (normalized.isBlank()) return@forEachIndexed

            val optionKey = optionColumnKey(normalized)
            when {
                optionKey != null -> optionColumns[optionKey] = index
                numberColumn == null && normalized in numberHeaders -> numberColumn = index
                typeColumn == null && normalized in typeHeaders -> typeColumn = index
                stemColumn == null && normalized in stemHeaders -> stemColumn = index
                combinedOptionsColumn == null && normalized in combinedOptionHeaders -> combinedOptionsColumn = index
                answerColumn == null && normalized in answerHeaders -> answerColumn = index
                analysisColumn == null && normalized in analysisHeaders -> analysisColumn = index
                categoryColumn == null && normalized in categoryHeaders -> categoryColumn = index
                scoreColumn == null && normalized in scoreHeaders -> scoreColumn = index
            }
        }

        val stem = stemColumn ?: return null
        val hasAnswer = answerColumn != null
        val hasOptions = optionColumns.isNotEmpty() || combinedOptionsColumn != null
        val hasTypeOrAnalysis = typeColumn != null || analysisColumn != null
        if (!hasAnswer && !hasOptions && !hasTypeOrAnalysis) return null

        return HeaderMapping(
            headerIndex = headerIndex,
            numberColumn = numberColumn,
            typeColumn = typeColumn,
            stemColumn = stem,
            optionColumns = optionColumns,
            combinedOptionsColumn = combinedOptionsColumn,
            answerColumn = answerColumn,
            analysisColumn = analysisColumn,
            categoryColumn = categoryColumn,
            scoreColumn = scoreColumn
        )
    }

    private fun parseDelimitedRows(text: String): List<List<String>> {
        return text.lineSequence()
            .map { line -> line.trimEnd() }
            .filter { it.isNotBlank() }
            .map { line ->
                when {
                    '\t' in line -> splitTabLine(line)
                    looksLikeCsvLine(line) -> parseCsvLine(line)
                    else -> line.split(Regex("\\s{2,}"))
                }
            }
            .toList()
    }


    private fun splitTabLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        line.forEach { ch ->
            if (ch == '\t') {
                cells += current.toString()
                current.clear()
            } else {
                current.append(ch)
            }
        }
        cells += current.toString()
        return cells
    }

    private fun looksLikeCsvLine(line: String): Boolean {
        if (',' !in line) return false
        val commaCount = line.count { it == ',' }
        val chineseCommaCount = line.count { it == '，' }
        return commaCount >= chineseCommaCount && commaCount >= 2
    }

    private fun parseCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val ch = line[index]
            when {
                ch == '"' && inQuotes && line.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index += 1
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    cells += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
            index += 1
        }
        cells += current.toString()
        return cells
    }

    private fun cleanupCell(raw: String): String {
        return raw.trim()
            .removeSurrounding("\"")
            .replace("\\n", "\n")
            .replace("_x000D_", "\n")
            .trim()
    }

    private fun cleanupOptionText(raw: String, key: String): String {
        return raw.trim()
            .replace(Regex("""^${Regex.escape(key)}\s*[.、．:：)）]\s*""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun parseCombinedOptions(raw: String): List<Option> {
        val text = raw.trim()
        if (text.isBlank()) return emptyList()
        val markerRegex = Regex("""(?:^|[\n;；\s])([A-Ga-g])\s*[.、．:：)）]\s*""")
        val matches = markerRegex.findAll(text).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.mapIndexedNotNull { index, match ->
            val key = match.groupValues[1].uppercase(Locale.ROOT)
            val start = match.range.last + 1
            val end = matches.getOrNull(index + 1)?.range?.first ?: text.length
            val value = text.substring(start, end).trim().trim(';', '；').trim()
            if (value.isBlank()) null else Option(key, value)
        }.distinctBy { it.key }
    }

    private fun parseQuestionType(raw: String): QuestionType? {
        val text = raw.trim()
        if (text.isBlank()) return null
        QuestionTypeLabelParser.parseLabel(text)?.let { return it }
        val normalized = normalizeHeader(text)
        return when {
            normalized in setOf("single", "singlechoice", "danxuan", "单选", "单项选择") -> QuestionType.SINGLE
            normalized in setOf("multiple", "multiplechoice", "duoxuan", "多选", "多项选择", "不定项") -> QuestionType.MULTIPLE
            normalized in setOf("judge", "truefalse", "tf", "判断", "是非", "对错") -> QuestionType.JUDGE
            normalized in setOf("blank", "fillblank", "填空") -> QuestionType.BLANK
            normalized in setOf("short", "essay", "qa", "简答", "问答", "论述") -> QuestionType.SHORT
            else -> null
        }
    }

    private fun inferQuestionType(
        explicitType: QuestionType?,
        stem: String,
        options: List<Option>,
        answerText: String
    ): QuestionType {
        explicitType?.let { return it }
        val normalizedAnswer = answerText.trim()
        val optionKeys = options.map { it.key.uppercase(Locale.ROOT) }
        val looksLikeJudgePair = optionKeys == listOf("A", "B") &&
            options.map { it.text.trim() }.all { it in judgeOptionTexts }
        if (looksLikeJudgePair || AnswerTokenParser.isJudgeAnswerText(normalizedAnswer)) return QuestionType.JUDGE
        if (options.isNotEmpty()) {
            val answers = AnswerTokenParser.parseObjectiveAnswers(normalizedAnswer)
            return if (answers.size > 1) QuestionType.MULTIPLE else QuestionType.SINGLE
        }
        if (blankHints.any { it in stem }) return QuestionType.BLANK
        return QuestionType.SHORT
    }

    private fun normalizeOptionsForType(options: List<Option>, type: QuestionType): List<Option> {
        return if (type == QuestionType.JUDGE && options.isEmpty()) {
            listOf(Option("A", "正确"), Option("B", "错误"))
        } else {
            options.distinctBy { it.key }
        }
    }

    private fun normalizeAnswer(raw: String, type: QuestionType): List<String> {
        if (raw.isBlank()) return emptyList()
        return when (type) {
            QuestionType.SINGLE, QuestionType.MULTIPLE -> AnswerTokenParser.parseObjectiveAnswers(raw)
            QuestionType.JUDGE -> {
                val parsed = AnswerTokenParser.parseObjectiveAnswers(raw)
                when {
                    parsed.isNotEmpty() -> parsed
                    Regex("""^(对|正确|是|√|true|t)$""", RegexOption.IGNORE_CASE).matches(raw.trim()) -> listOf("A")
                    Regex("""^(错|错误|否|×|x|false|f)$""", RegexOption.IGNORE_CASE).matches(raw.trim()) -> listOf("B")
                    else -> emptyList()
                }
            }
            QuestionType.BLANK, QuestionType.SHORT -> AnswerTokenParser.parseTextAnswer(raw)
        }
    }


    private fun isRepeatedHeaderRow(row: List<String>, mapping: HeaderMapping): Boolean {
        val stemHeader = normalizeHeader(row.cell(mapping.stemColumn)) in stemHeaders
        if (!stemHeader) return false
        val answerHeader = mapping.answerColumn?.let { normalizeHeader(row.cell(it)) in answerHeaders } == true
        val optionHeader = mapping.optionColumns.any { (key, index) -> optionColumnKey(normalizeHeader(row.cell(index))) == key }
        return answerHeader || optionHeader || mapping.combinedOptionsColumn?.let { normalizeHeader(row.cell(it)) in combinedOptionHeaders } == true
    }

    private fun isIgnorableRow(row: List<String>): Boolean {
        val compact = row.joinToString("").trim()
        if (compact.isBlank()) return true
        return compact in setOf("合计", "总计", "备注")
    }

    private fun cleanupNumber(raw: String): String {
        return raw.trim().removeSuffix(".0").ifBlank { "" }
    }

    private fun List<String>.cell(index: Int): String = getOrNull(index).orEmpty()

    private fun normalizeHeader(raw: String): String {
        return raw.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("""[\s_\-—－:：.．()（）\[\]【】]"""), "")
            .replace("ａ", "a")
            .replace("ｂ", "b")
            .replace("ｃ", "c")
            .replace("ｄ", "d")
            .replace("ｅ", "e")
            .replace("ｆ", "f")
            .replace("ｇ", "g")
    }

    private fun optionColumnKey(normalizedHeader: String): String? {
        if (normalizedHeader.length > 8) return null
        val direct = Regex("""^(?:选项)?([a-g])(?:选项|项)?$""").find(normalizedHeader)
            ?: Regex("""^(?:option|choice)([a-g])$""").find(normalizedHeader)
        return direct?.groupValues?.get(1)?.uppercase(Locale.ROOT)
    }

    private val numberHeaders = setOf("序号", "题号", "编号", "no", "number", "id", "index")
    private val typeHeaders = setOf("题型", "类型", "题目类型", "试题类型", "questiontype", "type")
    private val stemHeaders = setOf("题干", "题目", "问题", "试题", "试题内容", "题目内容", "内容", "question", "stem", "title")
    private val combinedOptionHeaders = setOf("选项", "选项内容", "备选项", "候选项", "choices", "options", "choice", "option")
    private val answerHeaders = setOf("答案", "正确答案", "标准答案", "参考答案", "正确选项", "answer", "answers", "correctanswer")
    private val analysisHeaders = setOf("解析", "答案解析", "试题解析", "说明", "解释", "analysis", "explanation", "reason")
    private val categoryHeaders = setOf("分类", "章节", "知识点", "标签", "科目", "目录", "category", "tag", "chapter", "subject")
    private val scoreHeaders = setOf("分值", "分数", "得分", "score", "point", "points")
    private val judgeOptionTexts = setOf("正确", "错误", "对", "错", "是", "否", "√", "×", "True", "False", "true", "false")
    private val blankHints = listOf("填空", "填入", "补全", "空白处", "____", "()", "（）")
}
