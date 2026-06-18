package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import kotlin.math.max

internal enum class InlineBlankConfidence {
    NONE,
    MEDIUM,
    HIGH
}

internal data class InlineBlankAnalysis(
    val confidence: InlineBlankConfidence,
    val questionText: String,
    val blankAnswers: List<List<String>> = emptyList(),
    val answer: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val evidence: List<String> = emptyList(),
    val rejectionReason: String? = null
)

internal data class MultiBlankParseResult(
    val questionText: String,
    val blankAnswers: List<List<String>> = emptyList(),
    val answer: List<String>,
    val warnings: List<String> = emptyList()
)

internal object MultiBlankQuestionParser {
    private data class FilledBlankCandidate(
        val match: MatchResult,
        val content: String
    )

    private val filledBracketRegex = Regex("""([（(])\s*([^()（）\r\n]{1,80}?)\s*([）)])""")
    private val fillIntentRegex = Regex("""(填空|填入|填写|依次填写|补全|补充完整|括号内|括号里|空白处|横线上)""")
    private val semanticStructureRegex = Regex(
        """(?:包括|包含|主要有|由.{0,24}组成|由.{0,24}构成|分为|可分为|分成|可分成|分别(?:是|为)|依次(?:是|为)|组成(?:是|为)|构成(?:是|为)|概括(?:是|为)|表现(?:是|为)|要求做到|对应(?:是|为)|表示)|(?:方针|原则|内容|要点|措施|目标|要求|职责|任务|分类|类型|阶段|部分|模块|环节|步骤|条件|因素|特点|特征).{0,12}(?:是|为|有|包括|包含|分为|组成|构成)"""
    )
    private val pairedStructureRegex = Regex(
        """(?:一是.{0,80}二是|其一.{0,80}其二|首先.{0,80}其次|第一.{0,80}第二|一个.{0,80}另一个|前者.{0,80}后者|横坐标.{0,80}纵坐标|横向.{0,80}纵向)"""
    )
    private val objectiveAnswerTokenRegex = Regex("""^[A-Ga-g](?:\s*[,，、;；/\\]?\s*[A-Ga-g]){0,6}$""")
    private val likelyUnitOnlyRegex = Regex("""^(?:MPa|kPa|Pa|℃|°C|K|m|cm|mm|km|m/s|km/h|kg|g|mol|s|min|h|%|V|A)$""", RegexOption.IGNORE_CASE)
    private val metadataContextRegex = Regex(
        """(?:以下简称|简称|又称|英文名称?|注册地址|统一社会信用代码|主席令|令第\s*\d+\s*号|征求意见稿|修订版|修订稿|出版社|文件编号|标准编号|附录\s*[A-Z0-9一二三四五六七八九十]+|图表\s*\d+)""",
        RegexOption.IGNORE_CASE
    )
    private val metadataContentRegex = Regex(
        """^(?:\d{4}\s*年?(?:修订|版)?|第?\s*\d+\s*号|.*(?:修订版|修订稿|征求意见稿|主席令|出版社)|(?:以下简称|简称|又称|英文名称?|注册地址|型号|规格|编号|标准号|文件号)\s*[:：]?.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val parameterContentRegex = Regex(
        """^(?:(?:型号|规格|功率|电压|电流|直径|长度|宽度|高度|重量|质量|容量|温度|压力|频率|转速)\s*[:：]?)?.*\d+(?:\.\d+)?\s*(?:MPa|kPa|Pa|℃|°C|K|mm|cm|km|m|m/s|km/h|kg|g|mol|s|min|h|Hz|kW|W|V|A|%|型|号)$""",
        RegexOption.IGNORE_CASE
    )
    private val ordinalTokenRegex = Regex("""^(?:\d{1,2}|[一二三四五六七八九十]{1,3})$""")

    fun analyzeInlineFilledBlanks(
        stem: String,
        options: List<Option>,
        answerText: String,
        forcedBlankContext: Boolean
    ): InlineBlankAnalysis {
        if (stem.isBlank() || options.isNotEmpty()) return none(stem)

        val rawMatches = filledBracketRegex.findAll(stem).toList()
        if (rawMatches.size < 2) return none(stem)

        val unprotectedMatches = buildList {
            rawMatches.forEachIndexed { index, match ->
                val protected = CodeLikeTextGuard.isProtectedParenthesizedToken(
                    text = stem,
                    range = match.range,
                    token = match.groupValues[2]
                )
                val previous = rawMatches.getOrNull(index - 1)
                val followsSafeBracket = protected && previous != null && previous in this &&
                    stem.substring(previous.range.last + 1, match.range.first).isBlank()
                if (!protected || followsSafeBracket) add(match)
            }
        }
        if (unprotectedMatches.size < 2) return none(stem)

        if (unprotectedMatches.any { objectiveAnswerTokenRegex.matches(it.groupValues[2].trim()) }) {
            return rejected(
                stem = stem,
                forcedBlankContext = forcedBlankContext,
                reason = "括号内容疑似选择题答案"
            )
        }

        val candidates = unprotectedMatches.mapNotNull { match ->
            val content = match.groupValues[2].trim()
            when {
                isLikelyOrdinalLabel(stem, match, content) -> null
                !isPlausibleFilledBlank(content) -> null
                else -> FilledBlankCandidate(match, content)
            }
        }
        if (candidates.size < 2) return none(stem)

        val hardRejection = hardRejectionReason(stem, candidates)
        if (hardRejection != null) {
            return rejected(
                stem = stem,
                forcedBlankContext = forcedBlankContext,
                reason = hardRejection
            )
        }

        val answerParts = MultiBlankSupport.splitReliableParts(answerText, candidates.size)
        val answerMatches = answerParts != null && answerParts.zip(candidates).all { (answer, candidate) ->
            normalizeComparable(answer) == normalizeComparable(candidate.content)
        }
        if (answerText.isNotBlank() && answerParts == null) {
            return medium(
                stem = stem,
                reason = "检测到多个内嵌答案，但独立答案数量无法对应"
            )
        }
        if (answerParts != null && !answerMatches) {
            return medium(
                stem = stem,
                reason = "内嵌答案与独立答案不一致"
            )
        }

        val evidence = mutableListOf<String>()
        var score = 0

        if (fillIntentRegex.containsMatchIn(stem)) {
            score += 4
            evidence += "明确填空提示"
        }
        if (answerMatches) {
            score += 3
            evidence += "独立答案逐空对应"
        }
        if (semanticStructureRegex.containsMatchIn(stem) || pairedStructureRegex.containsMatchIn(stem)) {
            score += 3
            evidence += "组成、分类、列举或对应结构"
        }
        if (candidates.all { it.content.length <= 32 }) {
            score += 2
            evidence += "多个短语型括号"
        }
        if (hasStableParallelGaps(stem, candidates)) {
            score += 2
            evidence += "括号之间存在稳定并列关系"
        }
        val cleanStem = replaceCandidatesWithBlanks(stem, candidates)
        if (formsPlausibleBlankSkeleton(cleanStem, candidates.size)) {
            score += 1
            evidence += "替换后形成多空骨架"
        }
        if (hasSimilarContentLengths(candidates)) {
            score += 1
            evidence += "括号内容形式接近"
        }
        if (candidates.size >= 3) {
            score += 1
            evidence += "三个以上连续题空"
        }

        val groups = candidates.mapIndexed { index, candidate ->
            buildList {
                add(candidate.content)
                answerParts?.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
            }.distinct()
        }
        val compatibilityAnswer = MultiBlankSupport.compatibilityAnswer(groups)

        val confidence = when {
            forcedBlankContext -> InlineBlankConfidence.HIGH
            score >= 7 -> InlineBlankConfidence.HIGH
            score >= 4 -> InlineBlankConfidence.MEDIUM
            else -> InlineBlankConfidence.NONE
        }
        return when (confidence) {
            InlineBlankConfidence.HIGH -> InlineBlankAnalysis(
                confidence = confidence,
                questionText = cleanStem,
                blankAnswers = groups,
                answer = compatibilityAnswer,
                evidence = if (forcedBlankContext) evidence + "明确填空题分区或题型" else evidence
            )
            InlineBlankConfidence.MEDIUM -> InlineBlankAnalysis(
                confidence = confidence,
                questionText = stem,
                warnings = listOf("疑似内嵌多空题，请人工核对"),
                evidence = evidence
            )
            InlineBlankConfidence.NONE -> none(stem)
        }
    }

    fun extract(
        stem: String,
        type: QuestionType,
        options: List<Option>,
        answerText: String,
        normalizedAnswer: List<String>,
        inlineAnalysis: InlineBlankAnalysis? = null
    ): MultiBlankParseResult {
        if (type != QuestionType.BLANK || options.isNotEmpty()) {
            val warnings = inlineAnalysis
                ?.takeIf { it.confidence == InlineBlankConfidence.MEDIUM }
                ?.warnings
                .orEmpty()
            return MultiBlankParseResult(stem, answer = normalizedAnswer, warnings = warnings)
        }

        val explicitBlankCount = MultiBlankSupport.countExplicitBlanks(stem)
        if (explicitBlankCount > 1) {
            if (answerText.isBlank()) {
                return MultiBlankParseResult(
                    questionText = stem,
                    blankAnswers = List(explicitBlankCount) { emptyList() },
                    answer = emptyList(),
                    warnings = listOf("检测到${explicitBlankCount}个题空，未识别逐空答案，请人工核对")
                )
            }
            val parts = MultiBlankSupport.splitReliableParts(answerText, explicitBlankCount)
            return if (parts != null) {
                val groups = parts.map { listOf(it) }
                MultiBlankParseResult(
                    questionText = stem,
                    blankAnswers = groups,
                    answer = MultiBlankSupport.compatibilityAnswer(groups)
                )
            } else {
                MultiBlankParseResult(
                    questionText = stem,
                    answer = normalizedAnswer,
                    warnings = listOf("检测到${explicitBlankCount}个题空，答案数量无法对应，请人工核对")
                )
            }
        }

        if (explicitBlankCount > 0) {
            return MultiBlankParseResult(stem, answer = normalizedAnswer)
        }

        val analysis = inlineAnalysis ?: analyzeInlineFilledBlanks(
            stem = stem,
            options = options,
            answerText = answerText,
            forcedBlankContext = true
        )
        return when (analysis.confidence) {
            InlineBlankConfidence.HIGH -> MultiBlankParseResult(
                questionText = analysis.questionText,
                blankAnswers = analysis.blankAnswers,
                answer = analysis.answer,
                warnings = analysis.warnings
            )
            InlineBlankConfidence.MEDIUM -> MultiBlankParseResult(
                questionText = stem,
                answer = normalizedAnswer,
                warnings = analysis.warnings
            )
            InlineBlankConfidence.NONE -> MultiBlankParseResult(stem, answer = normalizedAnswer)
        }
    }

    private fun hardRejectionReason(stem: String, candidates: List<FilledBlankCandidate>): String? {
        if (metadataContextRegex.containsMatchIn(stem)) return "括号内容疑似版本、引用或补充说明"
        if (candidates.any { metadataContentRegex.matches(it.content) }) return "括号内容疑似版本、编号或补充说明"
        if (candidates.any { parameterContentRegex.matches(it.content) }) return "括号内容疑似型号、参数或单位说明"
        if (candidates.any { it.content.length > 60 }) return "括号内容过长"
        if (candidates.any { content -> content.content.any { it in setOf('。', '！', '？', '!', '?', '；', ';') } }) {
            return "括号内容疑似完整说明句"
        }
        return null
    }

    private fun isPlausibleFilledBlank(content: String): Boolean {
        if (content.isBlank() || content.length > 60) return false
        if (objectiveAnswerTokenRegex.matches(content)) return false
        if (likelyUnitOnlyRegex.matches(content)) return false
        if (content.any { it in setOf('。', '！', '？', '!', '?', '；', ';') }) return false
        return content.any { it.isLetterOrDigit() || it.code > 127 }
    }

    private fun isLikelyOrdinalLabel(stem: String, match: MatchResult, content: String): Boolean {
        if (!ordinalTokenRegex.matches(content)) return false
        var next = match.range.last + 1
        while (next < stem.length && stem[next].isWhitespace()) next += 1
        val immediatelyFollowedByBracket = stem.getOrNull(next) == '(' || stem.getOrNull(next) == '（'
        if (!immediatelyFollowedByBracket) return false

        var previous = match.range.first - 1
        while (previous >= 0 && stem[previous].isWhitespace()) previous -= 1
        return previous < 0 || stem[previous] in setOf('。', '；', ';', '：', ':', '，', ',', '）', ')')
    }

    private fun hasStableParallelGaps(stem: String, candidates: List<FilledBlankCandidate>): Boolean {
        if (candidates.size < 2) return false
        return candidates.zipWithNext().all { (left, right) ->
            val start = left.match.range.last + 1
            val end = right.match.range.first
            if (start > end) return@all false
            val gap = stem.substring(start, end).trim()
            gap.length <= 24 && gap.none { it in setOf('。', '！', '？', '!', '?') }
        }
    }

    private fun formsPlausibleBlankSkeleton(stem: String, expectedCount: Int): Boolean {
        if (MultiBlankSupport.countExplicitBlanks(stem) != expectedCount) return false
        val outside = stem
            .replace(Regex("""(?:\(\s*\)|（\s*）|_{3,})"""), "")
            .replace(Regex("""[\s、，,；;和及与或以及并且]+"""), "")
        return outside.count { it.isLetterOrDigit() || it.code > 127 } >= 2
    }

    private fun hasSimilarContentLengths(candidates: List<FilledBlankCandidate>): Boolean {
        if (candidates.size < 2) return false
        val lengths = candidates.map { it.content.length }
        val average = lengths.average()
        return (lengths.maxOrNull() ?: 0) - (lengths.minOrNull() ?: 0) <= max(8, average.toInt())
    }

    private fun replaceCandidatesWithBlanks(text: String, candidates: List<FilledBlankCandidate>): String {
        var result = text
        candidates.asReversed().forEach { candidate ->
            val replacement = if (candidate.match.groupValues[1] == "（") "（ ）" else "( )"
            result = result.replaceRange(candidate.match.range, replacement)
        }
        return result
    }

    private fun normalizeComparable(value: String): String {
        return value.trim().lowercase().replace(Regex("""[\s　]+"""), "")
    }

    private fun none(stem: String): InlineBlankAnalysis {
        return InlineBlankAnalysis(
            confidence = InlineBlankConfidence.NONE,
            questionText = stem
        )
    }

    private fun medium(stem: String, reason: String): InlineBlankAnalysis {
        return InlineBlankAnalysis(
            confidence = InlineBlankConfidence.MEDIUM,
            questionText = stem,
            warnings = listOf("疑似内嵌多空题，请人工核对：$reason"),
            rejectionReason = reason
        )
    }

    private fun rejected(
        stem: String,
        forcedBlankContext: Boolean,
        reason: String
    ): InlineBlankAnalysis {
        return if (forcedBlankContext) {
            medium(stem, reason)
        } else {
            InlineBlankAnalysis(
                confidence = InlineBlankConfidence.NONE,
                questionText = stem,
                rejectionReason = reason
            )
        }
    }
}
