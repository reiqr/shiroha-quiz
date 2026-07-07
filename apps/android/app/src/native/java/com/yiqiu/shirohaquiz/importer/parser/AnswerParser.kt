package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.QuestionType

data class ParsedAnswerEntry(
    val number: String,
    val answer: List<String>,
    val analysis: String = "",
    val type: QuestionType? = null,
    val category: String = "",
    val sequence: Int = 0
)

object AnswerTokenParser {
    private const val ANSWER_LABEL_PATTERN = "答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答"
    private const val ANSWER_SEPARATOR_PATTERN = """(?:\s*(?:[:：,，、.．;；]|为)\s*|\s+|(?=\s*[\(（]))"""
    private val judgeTrueRegex = Regex("""^(正确|对|是|√|true|t)$""", RegexOption.IGNORE_CASE)
    private val judgeFalseRegex = Regex("""^(错误|错|否|×|x|false|f)$""", RegexOption.IGNORE_CASE)
    private val leadingChoiceRegex = Regex("""^\s*([A-Ga-g]{1,7})(?=\s*(?:[.、．:：)）;；\]\}]|[\u4e00-\u9fa5]|$))""")
    private val choiceRangeRegex = Regex("""^\s*([A-Ga-g])\s*(?:-|－|–|—|~|～|至|到)\s*([A-Ga-g])\s*$""")
    private val allChoiceRegex = Regex("""^(?:全选|全部|全部选|以上全选|所有选项|全都选|都选)$""")

    fun isObjectiveAnswerText(raw: String): Boolean {
        val value = cleanup(raw)
        if (value.isBlank()) return false
        if (isJudgeAnswerText(value)) return true
        if (parseChoiceRange(value).isNotEmpty()) return true
        if (allChoiceRegex.matches(value)) return true
        val separated = parseSeparatedChoiceLetters(value)
        if (separated.isNotEmpty()) return true
        if (leadingChoiceRegex.find(value) != null) return true
        val compact = value.replace(Regex("""[,，、;；/\\s]+"""), "").uppercase()
        return Regex("""^[A-G]{1,7}$""").matches(compact)
    }

    fun isJudgeAnswerText(raw: String): Boolean {
        val value = cleanup(raw)
        if (value.isBlank()) return false
        if (judgeTrueRegex.matches(value) || judgeFalseRegex.matches(value)) return true
        return Regex(
            """^[ABab]\s*[.、．:：)）]?\s*(?:正确|错误|对|错|是|否|√|×|True|False)$""",
            RegexOption.IGNORE_CASE
        ).matches(value)
    }

    fun parseJudgeAnswer(raw: String): List<String> {
        val value = cleanup(raw)
        if (value.isBlank()) return emptyList()
        if (judgeTrueRegex.matches(value)) return listOf("正确")
        if (judgeFalseRegex.matches(value)) return listOf("错误")
        when (value.uppercase()) {
            "A" -> return listOf("正确")
            "B" -> return listOf("错误")
        }
        val labeled = Regex(
            """^([ABab])\s*[.、．:：)）]?\s*(?:正确|错误|对|错|是|否|√|×|True|False)$""",
            RegexOption.IGNORE_CASE
        ).find(value)?.groupValues?.getOrNull(1)?.uppercase()
        return when (labeled) {
            "A" -> listOf("正确")
            "B" -> listOf("错误")
            else -> emptyList()
        }
    }

    fun parseObjectiveAnswers(raw: String, availableOptionKeys: List<String> = emptyList()): List<String> {
        val value = cleanup(raw)
        if (value.isBlank()) return emptyList()
        if (judgeTrueRegex.matches(value)) return listOf("A")
        if (judgeFalseRegex.matches(value)) return listOf("B")

        val normalizedOptionKeys = availableOptionKeys
            .map { it.trim().uppercase() }
            .filter { Regex("""^[A-G]$""").matches(it) }
            .distinct()

        if (allChoiceRegex.matches(value)) {
            return if (normalizedOptionKeys.isNotEmpty()) normalizedOptionKeys else listOf("全选")
        }

        parseChoiceRange(value).takeIf { it.isNotEmpty() }?.let { rangeAnswers ->
            return filterAnswersByAvailableOptions(rangeAnswers, normalizedOptionKeys)
        }

        parseSeparatedChoiceLetters(value).takeIf { it.isNotEmpty() }?.let { separatedAnswers ->
            return filterAnswersByAvailableOptions(separatedAnswers, normalizedOptionKeys)
        }

        leadingChoiceRegex.find(value)?.let { match ->
            val letters = match.groupValues[1].uppercase()
            if (Regex("""^[A-G]{1,7}$""").matches(letters)) {
                return filterAnswersByAvailableOptions(letters.map { it.toString() }.distinct(), normalizedOptionKeys)
            }
        }

        val compact = value.replace(Regex("""[,，、;；/\\\s]+"""), "").uppercase()
        if (Regex("""^[A-G]{1,7}$""").matches(compact)) {
            return filterAnswersByAvailableOptions(compact.map { it.toString() }.distinct(), normalizedOptionKeys)
        }

        return filterAnswersByAvailableOptions(
            value.split(Regex("""[,，、;；/\\]+"""))
                .map { it.trim().uppercase() }
                .filter { Regex("""^[A-G]$""").matches(it) }
                .distinct(),
            normalizedOptionKeys
        )
    }

    fun isAllChoiceAnswerText(raw: String): Boolean {
        return allChoiceRegex.matches(cleanup(raw))
    }

    fun parseTextAnswer(raw: String): List<String> {
        val value = cleanup(raw)
        return if (value.isBlank()) emptyList() else listOf(value)
    }

    private fun cleanup(raw: String): String {
        return raw.trim()
            .trim('[', ']', '【', '】', '(', ')', '（', '）')
            .replace(Regex("""^\s*(?:本题)?(?:$ANSWER_LABEL_PATTERN)(?:$ANSWER_SEPARATOR_PATTERN)?"""), "")
            .replace(Regex("""^\s*(?:应选|故选)\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^\s*选\s*(?=[A-Ga-g](?:\b|[.、．:：)）;；,，]))"""), "")
            .trim()
            .trim('[', ']', '【', '】', '(', ')', '（', '）')
            .trim()
    }

    private fun parseChoiceRange(value: String): List<String> {
        val match = choiceRangeRegex.find(value) ?: return emptyList()
        val start = match.groupValues[1].uppercase().first()
        val end = match.groupValues[2].uppercase().first()
        if (end < start) return emptyList()
        return (start..end).map { it.toString() }
    }

    private fun parseSeparatedChoiceLetters(value: String): List<String> {
        if (!Regex("""^[A-Ga-g](?:\s*[,，、;；/\\]\s*[A-Ga-g]|\s+[A-Ga-g]){1,6}$""").matches(value)) {
            return emptyList()
        }
        return value
            .split(Regex("""[,，、;；/\\\s]+"""))
            .map { it.trim().uppercase() }
            .filter { Regex("""^[A-G]$""").matches(it) }
            .distinct()
    }

    private fun filterAnswersByAvailableOptions(answers: List<String>, availableOptionKeys: List<String>): List<String> {
        if (answers.isEmpty()) return emptyList()
        if (availableOptionKeys.isEmpty()) return answers
        return answers.filter { it in availableOptionKeys }.distinct()
    }
}

object AnswerParser {
    private const val ANSWER_LABEL_PATTERN = "答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答"
    private const val ANALYSIS_LABEL_PATTERN = "答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明"
    private const val ANSWER_SEPARATOR_PATTERN = """(?:\s*(?:[:：,，、.．;；]|为)\s*|\s+|(?=\s*[\(（]))"""
    private const val OBJECTIVE_ANSWER_VALUE_PATTERN = """[\(（]?\s*(?:[A-Ga-g]\s*(?:-|－|–|—|~|～|至|到)\s*[A-Ga-g]|全选|[A-Ga-g]{1,7}|全部|全部选|以上全选|所有选项|全都选|都选|对|错|正确|错误|√|×|True|False)\s*[\)）]?"""
    private val inlineEntryRegex = Regex(
        """(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：]?\s*(?:(?:答案)$ANSWER_SEPARATOR_PATTERN)?($OBJECTIVE_ANSWER_VALUE_PATTERN)(?=\s*(?:\d{1,4}\s*[.、．:：]|$|\[|【|解析|[;；]))""",
        RegexOption.IGNORE_CASE
    )
    private val answerAnalysisLineRegex = Regex(
        """^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：]?\s*(?:(?:本题)?(?:答案|正确答案|参考答案|标准答案)$ANSWER_SEPARATOR_PATTERN)?($OBJECTIVE_ANSWER_VALUE_PATTERN)\s*[.。]?\s*(?:\[?\s*(?:$ANALYSIS_LABEL_PATTERN)\s*\]?\s*[:：]?|【\s*(?:$ANALYSIS_LABEL_PATTERN)\s*】\s*[:：]?)?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val rangeEntryRegex = Regex(
        """^\s*(\d{1,4})\s*[-~～至]\s*(\d{1,4})\s*[:：]\s*(.+)$"""
    )
    private val bracketAnswerLineRegex = Regex(
        """^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：]?\s*(?:[【\[]\s*(?:答案|正确答案|参考答案|标准答案|解析)\s*[】\]]|(?:本题)?(?:答案|正确答案|参考答案|标准答案|解析)$ANSWER_SEPARATOR_PATTERN)\s*($OBJECTIVE_ANSWER_VALUE_PATTERN)\s*[.。]?\s*(?:[【\[]?\s*(?:$ANALYSIS_LABEL_PATTERN)\s*[】\]]?\s*[:：]?)?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val simpleAnswerTailRegex = Regex(
        """^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：]?\s*([\(（]?\s*[A-Ga-g]{1,7}\s*[\)）]?)(?=\s|$|[\u4e00-\u9fa5])\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val multipleBracketEntryRegex = Regex(
        """(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[【\[]\s*(?:答案|正确答案|参考答案|标准答案)\s*[】\]]\s*($OBJECTIVE_ANSWER_VALUE_PATTERN)""",
        RegexOption.IGNORE_CASE
    )
    private val expressionAnswerLineRegex = Regex(
        """^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：)）]?\s*(?:(?:本题)?(?:答案|正确答案|参考答案|标准答案|正确选项)\s*(?:为|是)|(?:本题)?(?:应选|故选))\s*($OBJECTIVE_ANSWER_VALUE_PATTERN)\s*[.。,:：，、;；]?\s*(?:[【\[]?\s*(?:$ANALYSIS_LABEL_PATTERN)\s*[】\]]?\s*[:：]?)?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val labeledAnswerLineRegex = Regex(
        """^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：)）]?\s*(?:(?:本题)?(?:答案|正确答案|参考答案|标准答案|正确选项)$ANSWER_SEPARATOR_PATTERN)\s*($OBJECTIVE_ANSWER_VALUE_PATTERN)\s*[.。,:：，、;；]?\s*(?:[【\[]?\s*(?:$ANALYSIS_LABEL_PATTERN)\s*[】\]]?\s*[:：]?)?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val subjectiveAnswerLineRegex = Regex(
        """^\s*(?:(?:第\s*)?([一二三四五六七八九十百0-9]{1,4})\s*(?:题|问)?|(?:问题|题目)\s*([一二三四五六七八九十百0-9]{1,4}))\s*[.、．:：]?\s*(?:本题)?(?:$ANSWER_LABEL_PATTERN)$ANSWER_SEPARATOR_PATTERN(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val tableQuestionNumberRowRegex = Regex(
        """^\s*(?:题号|题目|序号)\s*[:：]?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val tableAnswerRowRegex = Regex(
        """^\s*(?:答案|正确答案|参考答案|标准答案)\s*[:：]?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val tableNumberTokenRegex = Regex("""(?:第\s*)?([0-9]{1,4}|[一二三四五六七八九十百]{1,4})(?:\s*题)?""")
    private val compactAnswerPairRegex = Regex("""(\d{1,4})\s*([A-Ga-g])(?=\s*\d{1,4}|\s*$)""")
    private val compactAnswerNoiseRegex = Regex("""[\s,，、;；:：.．\-—_()（）\[\]【】第章节单项选择题答案参考标准正确]+""")
    private val analysisOnlySectionHeadingRegex = Regex(
        """^\s*(?:[一二三四五六七八九十百]+|\d{1,3})?[、.．]?\s*(?:答案解析|集中解析|参考解析|解析区|解题思路|解析思路|解题分析|详解|分析|理由|解答|解析|说明)\s*[:：]?\s*$"""
    )
    private val numberedAnalysisOnlyLineRegex = Regex(
        """^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：)）]\s*(.+)$"""
    )


    private data class AnswerParseContext(
        val lines: List<String>,
        val index: Int,
        val currentType: QuestionType?,
        val currentCategory: String,
        val sequence: Int
    ) {
        val line: String get() = lines[index]
    }

    private sealed class AnswerRuleResult(open val consumedLines: Int) {
        data class Entries(
            val values: List<ParsedAnswerEntry>,
            override val consumedLines: Int = 1
        ) : AnswerRuleResult(consumedLines)

        data class ConsumeLine(
            val nextType: QuestionType?,
            val nextCategory: String? = null,
            override val consumedLines: Int = 1
        ) : AnswerRuleResult(consumedLines)
    }

    private data class AnswerParseRule(
        val name: String,
        val parse: (AnswerParseContext) -> AnswerRuleResult?
    )

    private val answerParseRules = listOf(
        AnswerParseRule("table_answer", ::parseTableAnswerRule),
        AnswerParseRule("compact_answer_line", ::parseCompactAnswerLineRule),
        AnswerParseRule("labeled_answer", ::parseLabeledAnswerRule),
        AnswerParseRule("expression_answer", ::parseExpressionAnswerRule),
        AnswerParseRule("subjective_answer", ::parseSubjectiveAnswerRule),
        AnswerParseRule("section_heading", ::parseSectionHeadingRule),
        AnswerParseRule("range_answer", ::parseRangeAnswerRule),
        AnswerParseRule("multiple_bracket_answer", ::parseMultipleBracketAnswerRule),
        AnswerParseRule("bracket_answer", ::parseBracketAnswerRule),
        AnswerParseRule("inline_multi_answer", ::parseInlineMultiAnswerRule),
        AnswerParseRule("answer_analysis", ::parseAnswerAnalysisRule),
        AnswerParseRule("inline_single_answer", ::parseInlineSingleAnswerRule),
        AnswerParseRule("simple_tail_answer", ::parseSimpleTailAnswerRule)
    )

    internal fun ruleNamesForTest(): List<String> = answerParseRules.map { it.name }

    fun parse(text: String): List<ParsedAnswerEntry> {
        val entries = mutableListOf<ParsedAnswerEntry>()
        var currentType: QuestionType? = null
        var currentCategory = ""
        var sequence = 0
        var inNumberedAnalysisSection = false

        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        var index = 0
        while (index < lines.size) {
            if (analysisOnlySectionHeadingRegex.matches(lines[index])) {
                inNumberedAnalysisSection = true
                index += 1
                continue
            }
            if (inNumberedAnalysisSection) {
                parseNumberedAnalysisOnlyLine(lines[index])?.let { entry ->
                    upsertEntry(entries, entry.copy(sequence = sequence))
                    sequence += 1
                    index += 1
                    continue
                }
            }

            val context = AnswerParseContext(lines, index, currentType, currentCategory, sequence)
            val result = answerParseRules.firstNotNullOfOrNull { rule -> rule.parse(context) }

            when (result) {
                is AnswerRuleResult.Entries -> {
                    result.values.forEach { upsertEntry(entries, it) }
                    result.values.lastOrNull { it.category.isNotBlank() }?.let { entry ->
                        currentCategory = entry.category
                        currentType = entry.type ?: currentType
                    }
                    sequence += result.values.size
                    index += result.consumedLines
                    inNumberedAnalysisSection = false
                }
                is AnswerRuleResult.ConsumeLine -> {
                    currentType = result.nextType
                    result.nextCategory?.let { currentCategory = it }
                    index += result.consumedLines
                    inNumberedAnalysisSection = false
                }
                null -> index += 1
            }
        }

        return entries
    }

    private fun parseNumberedAnalysisOnlyLine(line: String): ParsedAnswerEntry? {
        val match = numberedAnalysisOnlyLineRegex.find(line) ?: return null
        val number = normalizeQuestionIndex(match.groupValues[1])
        val analysis = match.groupValues[2].trim()
        if (number.isBlank() || analysis.isBlank()) return null
        if (AnswerTokenParser.parseObjectiveAnswers(analysis).isNotEmpty()) return null
        return ParsedAnswerEntry(number = number, answer = emptyList(), analysis = analysis)
    }

    private fun upsertEntry(entries: MutableList<ParsedAnswerEntry>, incoming: ParsedAnswerEntry) {
        val index = entries.indexOfLast { existing ->
            existing.number == incoming.number &&
                (incoming.category.isBlank() || existing.category.isBlank() || existing.category == incoming.category) &&
                (incoming.type == null || existing.type == null || existing.type == incoming.type)
        }
        if (index < 0) {
            entries += incoming
            return
        }

        val existing = entries[index]
        entries[index] = existing.copy(
            answer = existing.answer.ifEmpty { incoming.answer },
            analysis = existing.analysis.ifBlank { incoming.analysis },
            type = existing.type ?: incoming.type,
            category = existing.category.ifBlank { incoming.category }
        )
    }


    private data class CompactAnswerLineContext(
        val body: String,
        val type: QuestionType?,
        val category: String,
        val explicitChapter: Boolean
    )

    private fun parseCompactAnswerLineRule(context: AnswerParseContext): AnswerRuleResult? {
        val entries = parseCompactAnswerLine(
            line = context.line,
            currentType = context.currentType,
            currentCategory = context.currentCategory,
            startSequence = context.sequence
        ) ?: return null
        return AnswerRuleResult.Entries(entries)
    }

    private fun parseCompactAnswerLine(
        line: String,
        currentType: QuestionType?,
        currentCategory: String,
        startSequence: Int
    ): List<ParsedAnswerEntry>? {
        val lineContext = compactLineContext(line, currentType, currentCategory) ?: return null
        val matches = compactAnswerPairRegex.findAll(lineContext.body).toList()
        val minPairs = if (lineContext.explicitChapter || lineContext.category.isNotBlank()) 1 else 5
        if (matches.size < minPairs) return null
        val noise = compactAnswerPairRegex.replace(lineContext.body, "")
        val remainingNoise = compactAnswerNoiseRegex.replace(noise, "")
        if (remainingNoise.isNotBlank()) return null

        return matches.mapIndexedNotNull { offset, match ->
            val number = normalizeQuestionIndex(match.groupValues[1])
            val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
            if (number.isBlank() || answer.isEmpty()) null else ParsedAnswerEntry(
                number = number,
                answer = answer,
                type = lineContext.type,
                category = lineContext.category,
                sequence = startSequence + offset
            )
        }.takeIf { it.size >= minPairs }
    }

    private fun compactLineContext(
        line: String,
        currentType: QuestionType?,
        currentCategory: String
    ): CompactAnswerLineContext? {
        val firstPair = compactAnswerPairRegex.find(line) ?: return null
        val prefix = line.substring(0, firstPair.range.first).trim()
        val body = line.substring(firstPair.range.first).trim()
        val explicitChapter = ChapterScopeParser.hasChapter(prefix)
        val category = ChapterScopeParser.normalizeChapter(prefix).ifBlank { currentCategory }
        val type = QuestionTypeLabelParser.extractLeading(prefix)?.type
            ?: QuestionTypeLabelParser.parseLabel(prefix.replace(Regex("""^.*章"""), "").trim().trimEnd(':', '：'))
            ?: currentType
        return CompactAnswerLineContext(body = body, type = type, category = category, explicitChapter = explicitChapter)
    }

    private fun parseTableAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val tableEntries = parseAnswerTableAt(
            context.lines,
            context.index,
            context.currentType,
            context.currentCategory,
            context.sequence
        ) ?: return null
        return AnswerRuleResult.Entries(tableEntries, consumedLines = 2)
    }

    private fun parseLabeledAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val match = labeledAnswerLineRegex.find(context.line) ?: return null
        val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
        if (answer.isEmpty()) return null
        return AnswerRuleResult.Entries(
            listOf(
                ParsedAnswerEntry(
                    number = match.groupValues[1],
                    answer = answer,
                    analysis = match.groupValues[3].trim(),
                    type = context.currentType,
                    category = context.currentCategory,
                    sequence = context.sequence
                )
            )
        )
    }

    private fun parseExpressionAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val match = expressionAnswerLineRegex.find(context.line) ?: return null
        val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
        if (answer.isEmpty()) return null
        return AnswerRuleResult.Entries(
            listOf(
                ParsedAnswerEntry(
                    number = match.groupValues[1],
                    answer = answer,
                    analysis = match.groupValues[3].trim(),
                    type = context.currentType,
                    category = context.currentCategory,
                    sequence = context.sequence
                )
            )
        )
    }

    private fun parseSubjectiveAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val match = subjectiveAnswerLineRegex.find(context.line) ?: return null
        val number = normalizeQuestionIndex(match.groupValues[1].ifBlank { match.groupValues[2] })
        val answerText = match.groupValues[3].trim()
        if (number.isBlank() || answerText.isBlank()) return null

        val objectiveAnswer = AnswerTokenParser.parseObjectiveAnswers(answerText)
        val entry = if (objectiveAnswer.isNotEmpty()) {
            ParsedAnswerEntry(
                number = number,
                answer = objectiveAnswer,
                type = context.currentType,
                category = context.currentCategory,
                sequence = context.sequence
            )
        } else {
            ParsedAnswerEntry(
                number = number,
                answer = listOf(answerText),
                type = context.currentType ?: QuestionType.SHORT,
                category = context.currentCategory,
                sequence = context.sequence
            )
        }
        return AnswerRuleResult.Entries(listOf(entry))
    }

    private fun parseSectionHeadingRule(context: AnswerParseContext): AnswerRuleResult? {
        val chapter = ChapterScopeParser.normalizeChapter(context.line)
        if (chapter.isNotBlank()) {
            val afterChapter = context.line.replace(Regex("""^.*?章"""), "").trim().trimEnd(':', '：')
            val type = QuestionTypeLabelParser.parseLabel(afterChapter) ?: context.currentType
            return AnswerRuleResult.ConsumeLine(nextType = type, nextCategory = chapter)
        }
        val section = SectionTitleParser.parse(context.line) ?: return null
        val nextType = if (section.isAnswerSection) context.currentType else section.forcedType
        val nextCategory = if (!section.isAnswerSection && section.forcedType == null) section.title else null
        return AnswerRuleResult.ConsumeLine(nextType = nextType, nextCategory = nextCategory)
    }

    private fun parseRangeAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val match = rangeEntryRegex.find(context.line) ?: return null
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toInt()
        val tokens = match.groupValues[3]
            .split(Regex("""\s+"""))
            .mapNotNull { token ->
                val parsed = AnswerTokenParser.parseObjectiveAnswers(token)
                parsed.takeIf { it.isNotEmpty() }
            }
        if (tokens.isEmpty() || end < start) return null

        val entries = (start..end).mapIndexedNotNull { offset, number ->
            if (offset >= tokens.size) return@mapIndexedNotNull null
            ParsedAnswerEntry(
                number = number.toString(),
                answer = tokens[offset],
                type = context.currentType,
                category = context.currentCategory,
                sequence = context.sequence + offset
            )
        }
        if (entries.isEmpty()) return null
        return AnswerRuleResult.Entries(entries)
    }

    private fun parseMultipleBracketAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val entries = multipleBracketEntryRegex.findAll(context.line).mapIndexedNotNull { offset, match ->
            val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
            if (answer.isEmpty()) null else ParsedAnswerEntry(
                number = match.groupValues[1],
                answer = answer,
                type = context.currentType,
                category = context.currentCategory,
                sequence = context.sequence + offset
            )
        }.toList()
        if (entries.size < 2) return null
        return AnswerRuleResult.Entries(entries)
    }

    private fun parseBracketAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val match = bracketAnswerLineRegex.find(context.line) ?: return null
        val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
        if (answer.isEmpty()) return null
        return AnswerRuleResult.Entries(
            listOf(
                ParsedAnswerEntry(
                    number = match.groupValues[1],
                    answer = answer,
                    analysis = match.groupValues[3].trim(),
                    type = context.currentType,
                    category = context.currentCategory,
                    sequence = context.sequence
                )
            )
        )
    }

    private fun parseInlineMultiAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val entries = parseInlineEntries(context)
        if (entries.size < 2) return null
        return AnswerRuleResult.Entries(entries)
    }

    private fun parseAnswerAnalysisRule(context: AnswerParseContext): AnswerRuleResult? {
        val match = answerAnalysisLineRegex.find(context.line) ?: return null
        val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
        if (answer.isEmpty()) return null
        return AnswerRuleResult.Entries(
            listOf(
                ParsedAnswerEntry(
                    number = match.groupValues[1],
                    answer = answer,
                    analysis = match.groupValues[3].trim(),
                    type = context.currentType,
                    category = context.currentCategory,
                    sequence = context.sequence
                )
            )
        )
    }

    private fun parseInlineSingleAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val entries = parseInlineEntries(context)
        if (entries.isEmpty()) return null
        return AnswerRuleResult.Entries(entries)
    }

    private fun parseSimpleTailAnswerRule(context: AnswerParseContext): AnswerRuleResult? {
        val match = simpleAnswerTailRegex.find(context.line) ?: return null
        val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
        val tail = match.groupValues[3].trim()
        if (answer.isEmpty() || tail.length < 2) return null
        return AnswerRuleResult.Entries(
            listOf(
                ParsedAnswerEntry(
                    number = match.groupValues[1],
                    answer = answer,
                    analysis = tail,
                    type = context.currentType,
                    category = context.currentCategory,
                    sequence = context.sequence
                )
            )
        )
    }

    private fun parseInlineEntries(context: AnswerParseContext): List<ParsedAnswerEntry> {
        return inlineEntryRegex.findAll(context.line).mapIndexedNotNull { offset, match ->
            val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
            if (answer.isEmpty()) null else ParsedAnswerEntry(
                number = match.groupValues[1],
                answer = answer,
                type = context.currentType,
                category = context.currentCategory,
                sequence = context.sequence + offset
            )
        }.toList()
    }

    private fun parseAnswerTableAt(
        lines: List<String>,
        index: Int,
        currentType: QuestionType?,
        currentCategory: String,
        startSequence: Int
    ): List<ParsedAnswerEntry>? {
        if (index + 1 >= lines.size) return null
        val numberRow = tableQuestionNumberRowRegex.find(lines[index]) ?: return null
        val answerRow = tableAnswerRowRegex.find(lines[index + 1]) ?: return null
        val numbers = tableNumberTokenRegex.findAll(numberRow.groupValues[1])
            .mapNotNull { match -> normalizeQuestionIndex(match.groupValues[1]).takeIf { it.isNotBlank() } }
            .toList()
        if (numbers.isEmpty()) return null

        val answers = answerRow.groupValues[1]
            .split(Regex("""\s+"""))
            .mapNotNull { token ->
                val parsed = AnswerTokenParser.parseObjectiveAnswers(token)
                parsed.takeIf { it.isNotEmpty() }
            }
        if (answers.size != numbers.size) return null

        return numbers.mapIndexed { offset, number ->
            ParsedAnswerEntry(
                number = number,
                answer = answers[offset],
                type = currentType,
                category = currentCategory,
                sequence = startSequence + offset
            )
        }
    }
}
