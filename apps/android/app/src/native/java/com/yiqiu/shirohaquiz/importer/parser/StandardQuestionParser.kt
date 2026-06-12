package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.assets.QuestionImageMarker
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType

object StandardQuestionParser {
    private const val answerLabelPattern = "答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答"
    private const val analysisLabelPattern = "答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明"
    private const val objectiveAnswerValuePattern = "[A-Ga-g]{1,7}|对|错|正确|错误|是|否|√|×|True|False"
    private const val answerSeparatorPattern = """(?:\s*(?:[:：,，、.．;；]|为)\s*|\s+|(?=\s*[\(（]))"""
    private val answerLineRegex = Regex("""^\s*(?:(?:[\[【]\s*(?:$answerLabelPattern)\s*[\]】]\s*)|(?:(?:本题)?(?:$answerLabelPattern)$answerSeparatorPattern))(.+?)\s*$""")
    private val analysisLineRegex = Regex("""^\s*(?:(?:[\[【]\s*(?:$analysisLabelPattern)\s*[\]】]\s*)|(?:(?:$analysisLabelPattern)\s*[:：]\s*))(.*)$""")
    private val bracketAnswerRegex = Regex("""[\[【\(（]\s*(?:$answerLabelPattern)$answerSeparatorPattern([^\]】\)）]+)\s*[\]】\)）]""")
    private val embeddedChoiceAnswerRegex = Regex("""[\(（]\s*([A-Ga-g]{1,7}|对|错|正确|错误|是|否|√|×|True|False)\s*[\)）]""", RegexOption.IGNORE_CASE)
    private val blankKeywords = Regex("""(填空|填入|补全|补充完整|空白处|空白|空格|横线|横线上|括号内|括号里|_{2,}|[\(（]\s*[\)）])""")
    private val solutionChoiceRegex = Regex(
        """^\s*(?:(?:本题)?(?:答案|正确答案|参考答案|标准答案|正确选项)\s*(?:为|是)|(?:本题)?(?:应选|故选))\s*($objectiveAnswerValuePattern)\b[.。,:：，、;；]?\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val subjectiveAnswerLineRegex = Regex(
        """^\s*(?:(?:[\[【]\s*(?:$answerLabelPattern)\s*[\]】]\s*)|(?:(?:本题)?(?:$answerLabelPattern)$answerSeparatorPattern))(.*)$"""
    )

    private data class OptionMarker(val key: String, val markerStart: Int, val contentStart: Int)
    private data class ProtectedImageMarkerText(val text: String, val replacements: Map<String, String>)
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

    private fun parseBlock(block: QuestionBlock): Question? {
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
                appendOptionsOrStem(line, options, stemLines) -> Unit
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
            inferPlainOptionLines(stemLines, forcedType, answerText)?.let { inferred ->
                stemLines.clear()
                stemLines += inferred.stem
                options += inferred.options
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

        val type = inferType(
            stem = stem,
            options = options,
            answerText = answerText,
            forcedType = forcedType
        )
        val normalizedOptions = normalizeOptionsForType(options, type)
        val answer = normalizeAnswer(answerText, type)

        return Question(
            number = block.number,
            type = type,
            question = stem,
            options = normalizedOptions,
            answer = answer,
            analysis = analysisLines.joinToString("\n").trim(),
            category = block.category
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
        val matches = embeddedChoiceAnswerRegex.findAll(stem).toList()
        if (matches.isEmpty()) return null

        val answerTokens = AnswerTokenParser.parseObjectiveAnswers(existingAnswer)
        val selected = matches.asReversed().firstOrNull { match ->
            val candidate = match.groupValues[1].trim()
            val candidateTokens = AnswerTokenParser.parseObjectiveAnswers(candidate)
            when {
                existingAnswer.isBlank() && candidateTokens.isNotEmpty() -> true
                candidateTokens.isNotEmpty() && answerTokens.isNotEmpty() && candidateTokens == answerTokens -> true
                options.isNotEmpty() && AnswerTokenParser.isObjectiveAnswerText(candidate) -> true
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

        val answerWithAnalysis = Regex(
            """^\s*(?:(?:[\[【]\s*(?:$answerLabelPattern)\s*[\]】]\s*)|(?:(?:本题)?(?:$answerLabelPattern)$answerSeparatorPattern))(.+?)(?:\s*(?:$analysisLabelPattern)\s*[:：]\s*(.*))?\s*$"""
        ).find(clean)
        if (answerWithAnalysis != null) {
            answer = answer ?: answerWithAnalysis.groupValues[1].trim()
            analysis = answerWithAnalysis.groupValues.getOrElse(2) { "" }.trim().ifBlank { null }
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
        options: MutableList<Option>,
        stemLines: MutableList<String>
    ): Boolean {
        val optionLine = stripLeadingOptionLabel(line)
        val protectedLine = protectImageMarkersForOptionSplit(optionLine)
        val optionSplitLine = protectedLine.text
        val markers = findOptionMarkers(optionSplitLine)
        if (markers.isEmpty()) return false
        if (markers.size == 1 && markers.first().markerStart > 0 && options.isNotEmpty()) return false

        val firstMarker = markers.first()
        if (firstMarker.markerStart > 0) {
            val prefix = restoreProtectedImageMarkers(
                optionSplitLine.substring(0, firstMarker.markerStart),
                protectedLine
            ).trim()
            if (prefix.isNotBlank()) {
                val inferredKey = missingPreviousOptionKey(firstMarker.key, options)
                if (inferredKey != null && shouldUsePrefixAsMissingOption(prefix, inferredKey, firstMarker.key)) {
                    options += Option(inferredKey, prefix)
                } else {
                    stemLines += prefix
                }
            }
        }

        markers.forEachIndexed { index, marker ->
            val end = markers.getOrNull(index + 1)?.markerStart ?: optionSplitLine.length
            val text = restoreProtectedImageMarkers(
                optionSplitLine.substring(marker.contentStart, end),
                protectedLine
            )
                .trim()
                .trim(';', '；')
                .trim()
            if (text.isNotBlank()) {
                val existingIndex = options.indexOfLast { it.key == marker.key }
                if (existingIndex >= 0) {
                    val relabelKey = missingPreviousOptionKey(marker.key, options)
                    if (relabelKey != null) {
                        val old = options[existingIndex]
                        options[existingIndex] = old.copy(key = relabelKey)
                        options += Option(marker.key, text)
                    } else {
                        val old = options[existingIndex]
                        options[existingIndex] = old.copy(text = "${old.text} $text".replace(Regex("""\s+"""), " ").trim())
                    }
                } else {
                    options += Option(marker.key, text)
                }
            }
        }
        return true
    }

    private fun shouldUsePrefixAsMissingOption(prefix: String, inferredKey: String, firstMarkerKey: String): Boolean {
        if (inferredKey != "A" || firstMarkerKey != "B") return false
        val clean = prefix.trim()
        if (clean.length > 80) return false
        if (QuestionImageMarker.contains(clean)) return true
        if (Regex("""^[+-]?\d+(?:\s*[.．]\s*\d+)?(?:%|[A-Za-z]*)?(?:\s*(?:和|与|、|,|，)\s*[+-]?\d+(?:\s*[.．]\s*\d+)?(?:%|[A-Za-z]*)?)*$""", RegexOption.IGNORE_CASE).matches(clean)) return true
        if (Regex("""^[A-Za-z0-9\-+*/=^_()（）\[\]{}{}.,，。%％\s]{1,40}$""").matches(clean)) return true
        return false
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
        if (Regex("""[（(]\s*[)）]""").containsMatchIn(stem)) return true
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

    private fun protectImageMarkersForOptionSplit(line: String): ProtectedImageMarkerText {
        val ranges = QuestionImageMarker.rangesIn(line)
        if (ranges.isEmpty()) return ProtectedImageMarkerText(line, emptyMap())
        val replacements = linkedMapOf<String, String>()
        val builder = StringBuilder()
        var cursor = 0
        ranges.forEachIndexed { index, range ->
            if (range.first < cursor) return@forEachIndexed
            builder.append(line.substring(cursor, range.first))
            val token = "@@SHIROHA_IMG_MARKER_$index@@"
            replacements[token] = line.substring(range)
            builder.append(token)
            cursor = range.last + 1
        }
        if (cursor < line.length) builder.append(line.substring(cursor))
        return ProtectedImageMarkerText(builder.toString(), replacements)
    }

    private fun restoreProtectedImageMarkers(text: String, protectedText: ProtectedImageMarkerText): String {
        if (protectedText.replacements.isEmpty()) return text
        var restored = text
        protectedText.replacements.forEach { (token, marker) ->
            restored = restored.replace(token, marker)
        }
        return restored
    }

    private fun findOptionMarkers(line: String): List<OptionMarker> {
        val markers = mutableListOf<OptionMarker>()

        Regex("""([A-Ga-g])\s*[.、．:：]""").findAll(line).forEach { match ->
            markers += OptionMarker(
                key = match.groupValues[1].uppercase(),
                markerStart = match.range.first,
                contentStart = match.range.last + 1
            )
        }

        Regex("""^\s*([A-Ga-g])\s*[)）]""").findAll(line).forEach { match ->
            markers += OptionMarker(
                key = match.groupValues[1].uppercase(),
                markerStart = match.range.first,
                contentStart = match.range.last + 1
            )
        }

        val bracketOptionMatches = Regex("""[\(（\[【〔〖《]\s*([A-Ga-g])\s*[\)）\]】〕〗》]""").findAll(line).toList()
        val bracketOptionsAreLikelyInlineOptions = bracketOptionMatches.size >= 2 &&
            bracketOptionMatches.map { it.groupValues[1].uppercase() }.distinct().size >= 2
        bracketOptionMatches.forEach { match ->
            val isLeadingMarker = line.take(match.range.first).isBlank()
            if (isLeadingMarker || bracketOptionsAreLikelyInlineOptions) {
                markers += OptionMarker(
                    key = match.groupValues[1].uppercase(),
                    markerStart = match.range.first,
                    contentStart = match.range.last + 1
                )
            }
        }

        Regex("""[;；]\s*([A-Ga-g])(?=\S)""").findAll(line).forEach { match ->
            val keyGroup = match.groups[1] ?: return@forEach
            markers += OptionMarker(
                key = keyGroup.value.uppercase(),
                markerStart = keyGroup.range.first,
                contentStart = keyGroup.range.last + 1
            )
        }

        val imageRanges = QuestionImageMarker.rangesIn(line)
        return markers
            .filterNot { marker -> imageRanges.any { range -> marker.markerStart in range } }
            .filterNot { marker -> looksLikeOptionMarkerInsideAsciiWord(line, marker) }
            .filterNot { marker -> looksLikeInlineEnumerationMarker(line, marker) }
            .distinctBy { it.markerStart to it.key }
            .sortedBy { it.markerStart }
    }

    private fun looksLikeOptionMarkerInsideAsciiWord(line: String, marker: OptionMarker): Boolean {
        val previous = line.getOrNull(marker.markerStart - 1) ?: return false
        return previous in 'A'..'Z' || previous in 'a'..'z' || previous in '0'..'9' || previous == '_'
    }

    private fun looksLikeInlineEnumerationMarker(line: String, marker: OptionMarker): Boolean {
        val markerText = line.substring(marker.markerStart, marker.contentStart)
        if ('、' !in markerText) return false
        val previous = line.getOrNull(marker.markerStart - 1)
        val next = line.getOrNull(marker.contentStart)
        if (previous != null && (previous in 'A'..'G' || previous in 'a'..'g')) return true
        if (next != null && (next in 'A'..'G' || next in 'a'..'g')) {
            val tail = line.substring(marker.contentStart).trimStart()
            if (Regex("""^[A-Ga-g]\s*[.、．:：)）]""").containsMatchIn(tail)) return true
        }
        val prefix = line.take(marker.markerStart)
        if (Regex("""[A-Ga-g]\s*、\s*$""").containsMatchIn(prefix)) return true
        if (marker.markerStart > 0 && previous != null && previous.toString().matches(Regex("""[\u4e00-\u9fa5A-Za-z0-9]"""))) {
            return true
        }
        return false
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
        forcedType: QuestionType?
    ): QuestionType {
        if (forcedType != null) return forcedType

        if (options.isEmpty()) {
            if (AnswerTokenParser.isJudgeAnswerText(answerText)) {
                return QuestionType.JUDGE
            }
            if (AnswerTokenParser.isObjectiveAnswerText(answerText)) {
                val tokens = AnswerTokenParser.parseObjectiveAnswers(answerText)
                if (tokens.all { it == "A" || it == "B" } && shouldInferJudgeFromBinaryOptions(stem, answerText)) {
                    return QuestionType.JUDGE
                }
                return if (blankKeywords.containsMatchIn(stem)) QuestionType.BLANK else QuestionType.SHORT
            }
            return when {
                blankKeywords.containsMatchIn(stem) -> QuestionType.BLANK
                else -> QuestionType.SHORT
            }
        }

        val optionKeys = options.map { it.key.uppercase() }
        val looksLikeJudgePair = isJudgeOptionPair(
            optionKeys = optionKeys,
            optionTexts = options.map { it.text }
        )
        if (looksLikeJudgePair && shouldInferJudgeFromBinaryOptions(stem, answerText)) return QuestionType.JUDGE

        val tokens = AnswerTokenParser.parseObjectiveAnswers(answerText)
        return if (tokens.size > 1) QuestionType.MULTIPLE else QuestionType.SINGLE
    }

    private fun normalizeOptionsForType(options: List<Option>, type: QuestionType): List<Option> {
        return if (type == QuestionType.JUDGE && options.isEmpty()) {
            listOf(Option("A", "正确"), Option("B", "错误"))
        } else {
            options.distinctBy { it.key }
        }
    }

    private fun normalizeAnswer(answerText: String, type: QuestionType): List<String> {
        if (answerText.isBlank()) return emptyList()
        return when (type) {
            QuestionType.SINGLE, QuestionType.MULTIPLE -> AnswerTokenParser.parseObjectiveAnswers(answerText)
            QuestionType.JUDGE -> {
                val normalized = AnswerTokenParser.parseObjectiveAnswers(answerText)
                when {
                    normalized.isNotEmpty() -> normalized
                    Regex("""^(对|正确|是|√|true|t)$""", RegexOption.IGNORE_CASE).matches(answerText.trim()) -> listOf("A")
                    Regex("""^(错|错误|否|×|x|false|f)$""", RegexOption.IGNORE_CASE).matches(answerText.trim()) -> listOf("B")
                    else -> emptyList()
                }
            }
            QuestionType.BLANK, QuestionType.SHORT -> AnswerTokenParser.parseTextAnswer(answerText)
        }
    }
}
