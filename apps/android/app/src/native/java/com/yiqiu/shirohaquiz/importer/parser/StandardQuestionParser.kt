package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.assets.QuestionImageMarker
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType

object StandardQuestionParser {
    private const val answerLabelPattern = "答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答"
    private const val analysisLabelPattern = "答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明"
    private const val objectiveAnswerValuePattern = "[A-Ga-g]\\s*(?:-|－|–|—|~|～|至|到)\\s*[A-Ga-g]|全选|[A-Ga-g]{1,7}|全部|全部选|以上全选|所有选项|全都选|都选|对|错|正确|错误|是|否|√|×|True|False"
    private const val embeddedChoiceLetterPattern =
        "(?:[A-Ga-g](?:\\s*[,，、;；/\\\\]\\s*[A-Ga-g]){1,6}|[A-Ga-g](?:\\s+[A-Ga-g]){1,6}|[A-Ga-g]{1,7})"
    private const val answerSeparatorPattern = """(?:\s*(?:[:：,，、.．;；]|为)\s*|\s+|(?=\s*[\(（]))"""
    private val answerLineRegex = Regex("""^\s*(?:(?:[\[【]\s*(?:$answerLabelPattern)\s*[\]】]\s*)|(?:(?:本题)?(?:$answerLabelPattern)$answerSeparatorPattern))(.+?)\s*$""")
    private val analysisLineRegex = Regex("""^\s*(?:(?:[\[【]\s*(?:$analysisLabelPattern)\s*[\]】]\s*)|(?:(?:$analysisLabelPattern)\s*[:：]\s*))(.*)$""")
    private val bracketAnswerRegex = Regex("""[\[【\(（]\s*(?:$answerLabelPattern)$answerSeparatorPattern([^\]】\)）]+)\s*[\]】\)）]""")
    private val embeddedChoiceAnswerRegex = Regex(
        """[\(（]\s*($embeddedChoiceLetterPattern|对|错|正确|错误|是|否|√|×|True|False)\s*[\)）]""",
        RegexOption.IGNORE_CASE
    )
    private val blankTextKeywords = Regex("""(填空|填入|补全|补充完整|空白处|空白|空格|横线|横线上|括号内|括号里|_{2,})""")
    private val solutionChoiceRegex = Regex(
        """^\s*(?:(?:本题)?(?:答案|正确答案|参考答案|标准答案|正确选项)\s*(?:为|是)|(?:本题)?(?:应选|故选)|(?:分析|解析)\s*[:：]\s*(?:应选|故选|选))\s*($objectiveAnswerValuePattern)\b[.。,:：，、;；]?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val subjectiveAnswerLineRegex = Regex(
        """^\s*(?:(?:[\[【]\s*(?:$answerLabelPattern)\s*[\]】]\s*)|(?:(?:本题)?(?:$answerLabelPattern)$answerSeparatorPattern))(.*)$"""
    )
    private val inlineAnswerAnalysisRegex = Regex(
        """^\s*(?:(?:[\[【]\s*(?:$answerLabelPattern)\s*[\]】]\s*)|(?:(?:本题)?(?:$answerLabelPattern)$answerSeparatorPattern))(.+?)\s*(?:(?:[\[【]\s*(?:$analysisLabelPattern)\s*[\]】]\s*[:：]?\s*)|(?:(?:$analysisLabelPattern)\s*[:：]\s*))(.*)\s*$"""
    )

    private data class OptionMarker(val key: String, val markerStart: Int, val contentStart: Int)
    private data class LineAnswerExtraction(val cleanLine: String, val answerText: String? = null, val analysisText: String? = null)
    private data class EmbeddedStemAnswer(val cleanStem: String, val answerText: String)
    private data class InferredPlainOptions(val stem: List<String>, val options: List<Option>)
    private data class PreparedQuestionLine(val line: String, val forcedType: QuestionType? = null)

    fun parse(
        text: String,
        forcedType: QuestionType? = null,
        category: String = "",
        allowUnnumbered: Boolean = true
    ): List<Question> {
        return QuestionBlockSplitter.split(
            text = text,
            forcedType = forcedType,
            category = category,
            allowUnnumbered = allowUnnumbered
        ).mapNotNull(::parseBlock)
    }

    internal fun parseBlock(block: QuestionBlock): Question? {
        if (block.lines.isEmpty()) return null

        val options = mutableListOf<Option>()
        val stemLines = mutableListOf<String>()
        val analysisLines = mutableListOf<String>()
        val subjectiveAnswerLines = mutableListOf<String>()
        var answerText = ""
        var forcedType = block.forcedType
        var inAnalysis = false
        var inSubjectiveAnswer = false

        block.lines.forEach { rawLine ->
            val preparedLine = prepareQuestionLine(
                rawLine = rawLine,
                canStripTypeLabel = stemLines.isEmpty() && options.isEmpty() && answerText.isBlank() && subjectiveAnswerLines.isEmpty()
            )
            if (preparedLine.forcedType != null) {
                forcedType = preparedLine.forcedType
            }
            val extracted = extractInlineAnswer(preparedLine.line.trim())
            val line = extracted.cleanLine.trim()
            if (extracted.answerText?.isNotBlank() == true && answerText.isBlank()) {
                answerText = extracted.answerText.trim()
            }
            if (extracted.analysisText?.isNotBlank() == true) {
                analysisLines += extracted.analysisText.trim()
                inAnalysis = true
                inSubjectiveAnswer = false
            }

            when {
                line.isBlank() -> Unit
                inAnalysis -> analysisLines += line
                analysisLineRegex.matches(line) -> {
                    inAnalysis = true
                    inSubjectiveAnswer = false
                    analysisLines += analysisLineRegex.find(line)?.groupValues?.get(1).orEmpty().trim()
                }
                subjectiveAnswerLineRegex.matches(line) -> {
                    val content = subjectiveAnswerLineRegex.find(line)?.groupValues?.get(1).orEmpty().trim()
                    if (content.isNotBlank()) subjectiveAnswerLines += content
                    inSubjectiveAnswer = true
                }
                answerLineRegex.matches(line) -> {
                    if (answerText.isBlank()) {
                        answerText = answerLineRegex.find(line)?.groupValues?.get(1)?.trim().orEmpty()
                    }
                }
                inSubjectiveAnswer -> subjectiveAnswerLines += line
                appendOptionsOrStem(line, options) -> Unit
                options.isNotEmpty() -> {
                    val last = options.removeLast()
                    options += last.copy(text = "${last.text} $line".replace(Regex("""\s+"""), " ").trim())
                }
                else -> stemLines += line
            }
        }

        if (answerText.isBlank() && subjectiveAnswerLines.isNotEmpty()) {
            answerText = subjectiveAnswerLines.joinToString("\n").trim()
        }

        if (options.isEmpty()) {
            val inferred = inferImageOptionLabelLine(stemLines, forcedType)
                ?: inferPlainOptionLines(stemLines, forcedType, answerText)
            inferred?.let {
                stemLines.clear()
                stemLines += it.stem
                options += it.options
            }
        }

        var stem = stemLines.joinToString(" ").replace(Regex("""\s+"""), " ").trim()
        if (stem.isBlank()) return null

        extractEmbeddedAnswerFromStem(stem, answerText, options)?.let { extracted ->
            if (answerText.isBlank()) {
                answerText = extracted.answerText
            }
            stem = extracted.cleanStem
        }

        val inlineBlankAnalysis = MultiBlankQuestionParser.analyzeInlineFilledBlanks(
            stem = stem,
            options = options,
            answerText = answerText,
            forcedBlankContext = forcedType == QuestionType.BLANK
        )
        val type = inferType(
            stem = stem,
            options = options,
            answerText = answerText,
            forcedType = forcedType,
            inlineBlankAnalysis = inlineBlankAnalysis
        )
        val normalizedOptions = normalizeOptionsForType(options, type)
        val answer = normalizeAnswer(answerText, type, normalizedOptions)
        val multiBlank = MultiBlankQuestionParser.extract(
            stem = stem,
            type = type,
            options = normalizedOptions,
            answerText = answerText,
            normalizedAnswer = answer,
            inlineAnalysis = inlineBlankAnalysis
        )

        return Question(
            number = block.number,
            type = type,
            question = multiBlank.questionText,
            options = normalizedOptions,
            answer = multiBlank.answer,
            blankAnswers = multiBlank.blankAnswers,
            analysis = analysisLines.joinToString("\n").trim(),
            category = block.category,
            warnings = multiBlank.warnings
        )
    }


    private fun prepareQuestionLine(rawLine: String, canStripTypeLabel: Boolean): PreparedQuestionLine {
        val line = rawLine.trim()
        if (!canStripTypeLabel) return PreparedQuestionLine(line)
        val typed = QuestionTypeLabelParser.extractLeading(line) ?: return PreparedQuestionLine(line)
        return PreparedQuestionLine(
            line = typed.remainder,
            forcedType = typed.type
        )
    }

    private fun extractEmbeddedAnswerFromStem(
        stem: String,
        existingAnswer: String,
        options: List<Option>
    ): EmbeddedStemAnswer? {
        val matches = embeddedChoiceAnswerRegex.findAll(stem)
            .filterNot { match ->
                CodeLikeTextGuard.isProtectedParenthesizedToken(
                    text = stem,
                    range = match.range,
                    token = match.groupValues[1]
                )
            }
            .toList()
        if (matches.isEmpty()) return null

        val answerTokens = AnswerTokenParser.parseObjectiveAnswers(existingAnswer)
        val hasCompactOptionContext = CompactQuestionRepair.hasCompactOptionSequence(stem)
        val hasChoiceIntent = Regex(
            """(?:下列|以下).{0,24}(?:哪|正确|错误|不正确|符合)|选择|选出|最(?:合适|符合|恰当)|应(?:选择|选)"""
        ).containsMatchIn(stem)
        val selected = matches.asReversed().firstOrNull { match ->
            val candidate = match.groupValues[1].trim()
            val candidateTokens = AnswerTokenParser.parseObjectiveAnswers(candidate)
            val hasObjectiveContext = options.isNotEmpty() || hasCompactOptionContext || hasChoiceIntent
            when {
                candidateTokens.isEmpty() -> false
                candidateTokens.isNotEmpty() && answerTokens.isNotEmpty() && candidateTokens == answerTokens -> true
                AnswerTokenParser.isJudgeAnswerText(candidate) -> true
                existingAnswer.isBlank() && hasObjectiveContext -> true
                else -> false
            }
        } ?: return null

        val candidate = selected.groupValues[1].trim()
        val cleanStem = stem.removeRange(selected.range)
            .replace(Regex("""\s+([?？。；;，,])"""), "$1")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd('，', ',', '；', ';', ':', '：')
            .trim()

        return EmbeddedStemAnswer(cleanStem = cleanStem, answerText = candidate)
    }

    private fun extractInlineAnswer(line: String): LineAnswerExtraction {
        var answer: String? = null
        var analysis: String? = null
        var clean = line

        solutionChoiceRegex.find(clean)?.let { match ->
            answer = answer ?: match.groupValues[1].trim()
            val tail = match.groupValues.getOrElse(2) { "" }.trim()
            analysis = cleanAnalysisTail(tail)
            clean = ""
        }

        bracketAnswerRegex.find(clean)?.let { match ->
            answer = match.groupValues[1].trim()
            clean = clean.removeRange(match.range).trim()
        }

        inlineAnswerAnalysisRegex.find(clean)?.let { match ->
            answer = answer ?: match.groupValues[1].trim()
            analysis = match.groupValues.getOrElse(2) { "" }.trim().ifBlank { null }
            clean = ""
        }

        if (clean.isNotBlank()) {
            val answerWithAnalysis = Regex(
                """^\s*(?:(?:[\[【]\s*(?:$answerLabelPattern)\s*[\]】]\s*)|(?:(?:本题)?(?:$answerLabelPattern)$answerSeparatorPattern))(.+?)(?:\s*(?:$analysisLabelPattern)\s*[:：]\s*(.*))?\s*$"""
            ).find(clean)
            if (answerWithAnalysis != null) {
                answer = answer ?: answerWithAnalysis.groupValues[1].trim()
                analysis = answerWithAnalysis.groupValues.getOrElse(2) { "" }.trim().ifBlank { null }
            }
        }

        return LineAnswerExtraction(cleanLine = clean, answerText = answer, analysisText = analysis)
    }

    private fun cleanAnalysisTail(raw: String): String? {
        val tail = raw.trim().trimStart('。', '.', '，', ',', '；', ';', ':', '：').trim()
        if (tail.isBlank()) return null
        analysisLineRegex.find(tail)?.let { match ->
            return match.groupValues.getOrElse(1) { "" }.trim().ifBlank { null }
        }
        return tail
    }

    private fun appendOptionsOrStem(
        line: String,
        options: MutableList<Option>
    ): Boolean {
        val optionLine = stripLeadingOptionLabel(line)
        val marker = findLeadingOptionMarker(optionLine) ?: return false
        val text = optionLine.substring(marker.contentStart)
            .trim()
            .trim(';', '；')
            .trim()
        if (text.isBlank()) return false

        val existingIndex = options.indexOfLast { it.key == marker.key }
        if (existingIndex >= 0) {
            val relabelKey = missingPreviousOptionKey(marker.key, options)
            if (relabelKey != null) {
                val old = options[existingIndex]
                options[existingIndex] = old.copy(key = relabelKey)
                options += Option(marker.key, text)
            } else {
                val old = options[existingIndex]
                options[existingIndex] = old.copy(
                    text = "${old.text} $text".replace(Regex("""\s+"""), " ").trim()
                )
            }
        } else {
            options += Option(marker.key, text)
        }
        return true
    }

    private fun inferImageOptionLabelLine(
        stemLines: List<String>,
        forcedType: QuestionType?
    ): InferredPlainOptions? {
        if (forcedType == QuestionType.BLANK || forcedType == QuestionType.SHORT || forcedType == QuestionType.JUDGE) {
            return null
        }
        if (stemLines.none(QuestionImageMarker::contains)) return null

        val labelIndex = stemLines.indexOfLast { line ->
            parseSequentialImageOptionKeys(line) != null
        }
        if (labelIndex < 0) return null

        val keys = parseSequentialImageOptionKeys(stemLines[labelIndex]) ?: return null
        val remainingStem = stemLines.filterIndexed { index, _ -> index != labelIndex }
        val plainStem = remainingStem
            .filterNot(QuestionImageMarker::contains)
            .joinToString(" ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (!looksLikeChoiceStemNeedingOptions(plainStem)) return null

        return InferredPlainOptions(
            stem = remainingStem,
            options = keys.map { key -> Option(key, "图中选项$key") }
        )
    }

    private fun parseSequentialImageOptionKeys(line: String): List<String>? {
        val tokens = line.trim()
            .split(Regex("""\s+"""))
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
        if (tokens.size !in 2..7) return null
        if (tokens.any { !Regex("""^[A-G]$""").matches(it) }) return null
        val expected = ('A'..'G').take(tokens.size).map { it.toString() }
        return tokens.takeIf { it == expected }
    }

    private fun inferPlainOptionLines(
        stemLines: MutableList<String>,
        forcedType: QuestionType?,
        answerText: String
    ): InferredPlainOptions? {
        if (stemLines.size !in 5..8) return null
        if (!shouldInferPlainOptions(forcedType, answerText)) return null
        val stem = stemLines.first().trim()
        if (!looksLikeChoiceStemNeedingOptions(stem)) return null
        val optionLines = stemLines.drop(1).map { it.trim() }.filter { it.isNotBlank() }
        if (optionLines.size !in 4..7) return null
        if (optionLines.any { !looksLikePlainOptionText(it) }) return null
        if (!looksLikePlainOptionList(optionLines)) return null
        val keys = ('A'..'G').take(optionLines.size).map { it.toString() }
        return InferredPlainOptions(
            stem = listOf(stem),
            options = keys.zip(optionLines).map { (key, value) -> Option(key, value) }
        )
    }

    private fun shouldInferPlainOptions(forcedType: QuestionType?, answerText: String): Boolean {
        if (forcedType == QuestionType.SINGLE || forcedType == QuestionType.MULTIPLE) return true
        if (forcedType == QuestionType.BLANK || forcedType == QuestionType.SHORT || forcedType == QuestionType.JUDGE) return false
        return AnswerTokenParser.parseObjectiveAnswers(answerText).isNotEmpty()
    }

    private fun looksLikeChoiceStemNeedingOptions(stem: String): Boolean {
        if (CodeLikeTextGuard.hasUnprotectedEmptyParentheses(stem)) return true
        return Regex("""(?:下列|以下|哪个|哪项|哪一项|选择|选出|不正确|符合|不符合|可以避免|统计量是)""").containsMatchIn(stem)
    }

    private fun looksLikePlainOptionText(line: String): Boolean {
        if (line.length !in 1..90) return false
        if (answerLineRegex.matches(line) || analysisLineRegex.matches(line)) return false
        if (QuestionTypeLabelParser.extractLeading(line) != null) return false
        if (Regex("""^\s*(?:第\s*)?\d{1,4}\s*(?:题)?\s*[.、．:：)）]""").containsMatchIn(line)) return false
        if (Regex("""^(?:df|SS|MS|F|Significance\s+F|回归分析|残差|总计|方差)$""", RegexOption.IGNORE_CASE).matches(line)) return false
        return true
    }

    private fun looksLikePlainOptionList(lines: List<String>): Boolean {
        if (lines.any { Regex("""[。；;]\s*$""").containsMatchIn(it) }) return false
        if (lines.count { it.length <= 32 } < lines.size) return false
        return lines.map { it.length }.average() <= 24.0
    }

    private fun stripLeadingOptionLabel(line: String): String {
        return line.replace(Regex("""^\s*(?:选项|备选项|选项内容|候选项)\s*[:：]\s*"""), "")
    }

    private fun findLeadingOptionMarker(line: String): OptionMarker? {
        Regex("""^\s*([A-Ga-g])\s*[.、．:：)）]""").find(line)?.let { match ->
            val keyGroup = match.groups[1] ?: return@let
            val marker = OptionMarker(
                key = keyGroup.value.uppercase(),
                markerStart = keyGroup.range.first,
                contentStart = match.range.last + 1
            )
            if (
                !looksLikeDottedEnglishAbbreviation(line, marker) &&
                !CodeLikeTextGuard.looksLikeLeadingCodeOption(line, marker.markerStart, marker.contentStart)
            ) return marker
        }

        Regex("""^\s*[\(（\[【〔〖《]\s*([A-Ga-g])\s*[\)）\]】〕〗》]""").find(line)?.let { match ->
            val keyGroup = match.groups[1] ?: return@let
            val marker = OptionMarker(
                key = keyGroup.value.uppercase(),
                markerStart = match.range.first,
                contentStart = match.range.last + 1
            )
            if (!CodeLikeTextGuard.looksLikeLeadingCodeOption(line, marker.markerStart, marker.contentStart)) {
                return marker
            }
        }
        return null
    }

    private fun looksLikeDottedEnglishAbbreviation(line: String, marker: OptionMarker): Boolean {
        val markerText = line.substring(marker.markerStart, marker.contentStart)
        if ('.' !in markerText && '．' !in markerText) return false
        if (looksLikeTechnicalOptionExpression(line, marker)) return false

        val next = line.getOrNull(marker.contentStart) ?: return false
        if (next !in 'A'..'Z' && next !in 'a'..'z') return false
        val nextNext = line.getOrNull(marker.contentStart + 1)

        if (nextNext == '.' || nextNext == '．') return true
        if (next.isUpperCase() && (nextNext == null || nextNext.isWhitespace() || nextNext in setOf(
                ',', '，', ';', '；', ':', '：', ')', '）', ']', '】', '}', '》', '/', '\\'
            ))) {
            return true
        }
        return false
    }

    /** A.Q/P、A.Y,yn0 这类以选项标记开头的技术表达式，不属于 E.U、D.H. 式英文缩写。 */
    private fun looksLikeTechnicalOptionExpression(line: String, marker: OptionMarker): Boolean {
        val tail = line.substring(marker.contentStart).trim()
        return Regex("""^[A-Z](?:/|[,，])\s*\S+""").containsMatchIn(tail)
    }

    private fun missingPreviousOptionKey(key: String, options: List<Option>): String? {
        val expected = listOf("A", "B", "C", "D", "E", "F", "G")
        val index = expected.indexOf(key.uppercase())
        if (index <= 0) return null
        val previous = expected[index - 1]
        return previous.takeIf { candidate -> options.none { it.key == candidate } }
    }

    private fun inferType(
        stem: String,
        options: List<Option>,
        answerText: String,
        forcedType: QuestionType?,
        inlineBlankAnalysis: InlineBlankAnalysis
    ): QuestionType {
        if (forcedType != null) return forcedType

        if (options.isEmpty()) {
            if (inlineBlankAnalysis.confidence == InlineBlankConfidence.HIGH) {
                return QuestionType.BLANK
            }
            if (AnswerTokenParser.isJudgeAnswerText(answerText)) {
                return QuestionType.JUDGE
            }
            if (AnswerTokenParser.isObjectiveAnswerText(answerText)) {
                val tokens = AnswerTokenParser.parseObjectiveAnswers(answerText)
                if (tokens.all { it == "A" || it == "B" } && shouldInferJudgeFromBinaryOptions(stem, answerText)) {
                    return QuestionType.JUDGE
                }
                return if (containsBlankCue(stem)) QuestionType.BLANK else QuestionType.SHORT
            }
            return when {
                containsBlankCue(stem) -> QuestionType.BLANK
                else -> QuestionType.SHORT
            }
        }

        val optionKeys = options.map { it.key.uppercase() }
        val looksLikeJudgePair = isJudgeOptionPair(
            optionKeys = optionKeys,
            optionTexts = options.map { it.text }
        )
        if (looksLikeJudgePair && shouldInferJudgeFromBinaryOptions(stem, answerText)) return QuestionType.JUDGE

        val tokens = AnswerTokenParser.parseObjectiveAnswers(answerText, optionKeys)
        return if (tokens.size > 1) QuestionType.MULTIPLE else QuestionType.SINGLE
    }

    private fun containsBlankCue(stem: String): Boolean {
        return blankTextKeywords.containsMatchIn(stem) ||
            CodeLikeTextGuard.hasUnprotectedEmptyParentheses(stem)
    }

    private fun normalizeOptionsForType(options: List<Option>, type: QuestionType): List<Option> {
        return if (type == QuestionType.JUDGE && options.isEmpty()) {
            listOf(Option("A", "正确"), Option("B", "错误"))
        } else {
            options.distinctBy { it.key }
        }
    }

    private fun normalizeAnswer(answerText: String, type: QuestionType, options: List<Option>): List<String> {
        if (answerText.isBlank()) return emptyList()
        val optionKeys = options.map { it.key.uppercase() }
        return when (type) {
            QuestionType.SINGLE, QuestionType.MULTIPLE -> AnswerTokenParser.parseObjectiveAnswers(answerText, optionKeys)
            QuestionType.JUDGE -> {
                AnswerTokenParser.parseJudgeAnswer(answerText)
            }
            QuestionType.BLANK, QuestionType.SHORT -> AnswerTokenParser.parseTextAnswer(answerText)
        }
    }
}
