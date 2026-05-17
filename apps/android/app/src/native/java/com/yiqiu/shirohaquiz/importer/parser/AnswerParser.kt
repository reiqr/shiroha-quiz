package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.QuestionType

data class ParsedAnswerEntry(
    val number: String,
    val answer: List<String>,
    val analysis: String = "",
    val type: QuestionType? = null,
    val sequence: Int = 0
)

object AnswerTokenParser {
    private const val ANSWER_LABEL_PATTERN = "答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答"
    private const val ANSWER_SEPARATOR_PATTERN = """(?:\s*(?:[:：,，、.．;；]|为)\s*|\s+|(?=\s*[\(（]))"""
    private val judgeTrueRegex = Regex("""^(正确|对|是|√|true|t)$""", RegexOption.IGNORE_CASE)
    private val judgeFalseRegex = Regex("""^(错误|错|否|×|x|false|f)$""", RegexOption.IGNORE_CASE)
    private val leadingChoiceRegex = Regex("""^\s*([A-Ga-g]{1,7})(?=\s*(?:[.、．:：)）;；\]\}]|[\u4e00-\u9fa5]|$))""")

    fun isObjectiveAnswerText(raw: String): Boolean {
        val value = cleanup(raw)
        if (value.isBlank()) return false
        if (isJudgeAnswerText(value)) return true
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

    fun parseObjectiveAnswers(raw: String): List<String> {
        val value = cleanup(raw)
        if (value.isBlank()) return emptyList()
        if (judgeTrueRegex.matches(value)) return listOf("A")
        if (judgeFalseRegex.matches(value)) return listOf("B")

        leadingChoiceRegex.find(value)?.let { match ->
            val letters = match.groupValues[1].uppercase()
            if (Regex("""^[A-G]{1,7}$""").matches(letters)) {
                return letters.map { it.toString() }.distinct()
            }
        }

        val compact = value.replace(Regex("""[,，、;；/\\\s]+"""), "").uppercase()
        if (Regex("""^[A-G]{1,7}$""").matches(compact)) {
            return compact.map { it.toString() }.distinct()
        }

        return value.split(Regex("""[,，、;；/\\]+"""))
            .map { it.trim().uppercase() }
            .filter { Regex("""^[A-G]$""").matches(it) }
            .distinct()
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
            .trim()
            .trim('[', ']', '【', '】', '(', ')', '（', '）')
            .trim()
    }
}

object AnswerParser {
    private const val ANSWER_LABEL_PATTERN = "答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答"
    private const val ANALYSIS_LABEL_PATTERN = "答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明"
    private const val ANSWER_SEPARATOR_PATTERN = """(?:\s*(?:[:：,，、.．;；]|为)\s*|\s+|(?=\s*[\(（]))"""
    private const val OBJECTIVE_ANSWER_VALUE_PATTERN = """[\(（]?\s*(?:[A-Ga-g]{1,7}|对|错|正确|错误|√|×|True|False)\s*[\)）]?"""
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


    fun parse(text: String): List<ParsedAnswerEntry> {
        val entries = mutableListOf<ParsedAnswerEntry>()
        var currentType: QuestionType? = null
        var sequence = 0

        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]

            val tableEntries = parseAnswerTableAt(lines, index, currentType, sequence)
            if (tableEntries != null) {
                entries += tableEntries
                sequence += tableEntries.size
                index += 2
                continue
            }

            val labeledAnswerMatch = labeledAnswerLineRegex.find(line)
            if (labeledAnswerMatch != null) {
                val answer = AnswerTokenParser.parseObjectiveAnswers(labeledAnswerMatch.groupValues[2])
                if (answer.isNotEmpty()) {
                    entries += ParsedAnswerEntry(
                        number = labeledAnswerMatch.groupValues[1],
                        answer = answer,
                        analysis = labeledAnswerMatch.groupValues[3].trim(),
                        type = currentType,
                        sequence = sequence++
                    )
                    index += 1
                    continue
                }
            }

            val expressionMatch = expressionAnswerLineRegex.find(line)
            if (expressionMatch != null) {
                val answer = AnswerTokenParser.parseObjectiveAnswers(expressionMatch.groupValues[2])
                if (answer.isNotEmpty()) {
                    entries += ParsedAnswerEntry(
                        number = expressionMatch.groupValues[1],
                        answer = answer,
                        analysis = expressionMatch.groupValues[3].trim(),
                        type = currentType,
                        sequence = sequence++
                    )
                    index += 1
                    continue
                }
            }

            val subjectiveMatch = subjectiveAnswerLineRegex.find(line)
            if (subjectiveMatch != null) {
                val number = normalizeQuestionIndex(subjectiveMatch.groupValues[1].ifBlank { subjectiveMatch.groupValues[2] })
                val answerText = subjectiveMatch.groupValues[3].trim()
                if (number.isNotBlank() && answerText.isNotBlank()) {
                    val objectiveAnswer = AnswerTokenParser.parseObjectiveAnswers(answerText)
                    entries += if (objectiveAnswer.isNotEmpty()) {
                        ParsedAnswerEntry(
                            number = number,
                            answer = objectiveAnswer,
                            type = currentType,
                            sequence = sequence++
                        )
                    } else {
                        ParsedAnswerEntry(
                            number = number,
                            answer = listOf(answerText),
                            type = currentType ?: QuestionType.SHORT,
                            sequence = sequence++
                        )
                    }
                    index += 1
                    continue
                }
            }

            val section = SectionTitleParser.parse(line)
            if (section != null) {
                if (!section.isAnswerSection) currentType = section.forcedType
                index += 1
                continue
            }

            val rangeMatch = rangeEntryRegex.find(line)
            if (rangeMatch != null) {
                val start = rangeMatch.groupValues[1].toInt()
                val end = rangeMatch.groupValues[2].toInt()
                val tokens = rangeMatch.groupValues[3]
                    .split(Regex("""\s+"""))
                    .mapNotNull { token ->
                        val parsed = AnswerTokenParser.parseObjectiveAnswers(token)
                        parsed.takeIf { it.isNotEmpty() }
                    }
                if (tokens.isNotEmpty() && end >= start) {
                    (start..end).forEachIndexed { offset, number ->
                        if (offset < tokens.size) {
                            entries += ParsedAnswerEntry(
                                number = number.toString(),
                                answer = tokens[offset],
                                type = currentType,
                                sequence = sequence++
                            )
                        }
                    }
                    index += 1
                    continue
                }
            }

            val bracketEntries = multipleBracketEntryRegex.findAll(line).mapNotNull { match ->
                val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
                if (answer.isEmpty()) null else ParsedAnswerEntry(
                    number = match.groupValues[1],
                    answer = answer,
                    type = currentType,
                    sequence = sequence++
                )
            }.toList()
            if (bracketEntries.size >= 2) {
                entries += bracketEntries
                index += 1
                continue
            }

            val bracketMatch = bracketAnswerLineRegex.find(line)
            if (bracketMatch != null) {
                val answer = AnswerTokenParser.parseObjectiveAnswers(bracketMatch.groupValues[2])
                if (answer.isNotEmpty()) {
                    entries += ParsedAnswerEntry(
                        number = bracketMatch.groupValues[1],
                        answer = answer,
                        analysis = bracketMatch.groupValues[3].trim(),
                        type = currentType,
                        sequence = sequence++
                    )
                    index += 1
                    continue
                }
            }

            val lineEntries = inlineEntryRegex.findAll(line).mapNotNull { match ->
                val number = match.groupValues[1]
                val answer = AnswerTokenParser.parseObjectiveAnswers(match.groupValues[2])
                if (answer.isEmpty()) null else ParsedAnswerEntry(
                    number = number,
                    answer = answer,
                    type = currentType,
                    sequence = sequence++
                )
            }.toList()

            if (lineEntries.size >= 2) {
                entries += lineEntries
                index += 1
                continue
            }

            val answerAnalysisMatch = answerAnalysisLineRegex.find(line)
            if (answerAnalysisMatch != null) {
                val answer = AnswerTokenParser.parseObjectiveAnswers(answerAnalysisMatch.groupValues[2])
                if (answer.isNotEmpty()) {
                    entries += ParsedAnswerEntry(
                        number = answerAnalysisMatch.groupValues[1],
                        answer = answer,
                        analysis = answerAnalysisMatch.groupValues[3].trim(),
                        type = currentType,
                        sequence = sequence++
                    )
                    index += 1
                    continue
                }
            }

            if (lineEntries.isNotEmpty()) {
                entries += lineEntries
                index += 1
                continue
            }

            val simpleAnswerMatch = simpleAnswerTailRegex.find(line)
            if (simpleAnswerMatch != null) {
                val answer = AnswerTokenParser.parseObjectiveAnswers(simpleAnswerMatch.groupValues[2])
                val tail = simpleAnswerMatch.groupValues[3].trim()
                if (answer.isNotEmpty() && tail.length >= 2) {
                    entries += ParsedAnswerEntry(
                        number = simpleAnswerMatch.groupValues[1],
                        answer = answer,
                        analysis = tail,
                        type = currentType,
                        sequence = sequence++
                    )
                }
            }

            index += 1
        }

        return entries
    }

    private fun parseAnswerTableAt(
        lines: List<String>,
        index: Int,
        currentType: QuestionType?,
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
                sequence = startSequence + offset
            )
        }
    }

    private fun normalizeQuestionIndex(raw: String): String {
        val clean = raw.trim()
        if (clean.all { it.isDigit() }) return clean
        return chineseNumberToInt(clean)?.toString().orEmpty()
    }

    private fun chineseNumberToInt(raw: String): Int? {
        if (raw.isBlank()) return null
        val digitMap = mapOf(
            '零' to 0, '〇' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4,
            '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9
        )
        if ('百' in raw) {
            val parts = raw.split('百', limit = 2)
            val hundreds = parts.getOrNull(0)?.takeIf { it.isNotBlank() }?.let { digitMap[it.first()] } ?: 1
            val tail = parts.getOrNull(1).orEmpty()
            return hundreds * 100 + (chineseNumberToInt(tail) ?: 0)
        }
        if ('十' in raw) {
            val parts = raw.split('十', limit = 2)
            val tens = parts.getOrNull(0)?.takeIf { it.isNotBlank() }?.let { digitMap[it.first()] } ?: 1
            val ones = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { digitMap[it.first()] } ?: 0
            return tens * 10 + ones
        }
        return digitMap[raw.first()]
    }
}
