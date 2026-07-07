package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.QuestionType

object AnswerSectionParser {
    private const val implicitObjectiveAnswerPattern =
        """[\(（]?\s*(?:[A-Ga-g]\s*(?:-|－|–|—|~|～|至|到)\s*[A-Ga-g]|全选|全部|全部选|以上全选|所有选项|全都选|都选|[A-Ga-g]{1,7}|对|错|正确|错误|√|×|True|False)\s*[\)）]?"""
    private const val implicitAnswerLabelPattern =
        """(?:[【\[]\s*(?:答案|正确答案|参考答案|标准答案)\s*[】\]]\s*[:：]?|(?:本题)?(?:答案|正确答案|参考答案|标准答案)\s*(?:[:：]|为|是))"""
    private const val implicitAnalysisLabelPattern =
        """(?:[【\[]\s*(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*[】\]]\s*[:：]?|(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*(?:[:：]\s*|\s+))"""

    /**
     * 无明确“答案区”标题时，只接受完整的独立答案行。
     * 不能仅凭题号后第一个字符是 A-G、对或错，就把正常题干截成集中答案区。
     */
    private val answerEntryWithAnalysisRegex = Regex(
        """^\s*(?:第\s*)?\d{1,4}\s*(?:题)?\s*[.、．:：)）]?\s*(?:$implicitObjectiveAnswerPattern\s*[.。]?|$implicitAnswerLabelPattern\s*$implicitObjectiveAnswerPattern\s*[.。]?(?:\s*$implicitAnalysisLabelPattern\s*.*)?|$implicitObjectiveAnswerPattern\s*[.。]?\s*$implicitAnalysisLabelPattern\s*.+)\s*$""",
        RegexOption.IGNORE_CASE
    )

    fun hasAnswerSection(text: String): Boolean {
        val lines = text.lineSequence().toList()
        if (findExplicitAnswerStart(lines) >= 0) return true
        return findImplicitAnswerStart(lines) >= 0
    }

    fun splitSections(text: String): Pair<String, String?> {
        val lines = text.lineSequence().toList()
        val explicitIndex = findExplicitAnswerStart(lines)
        val startIndex = if (explicitIndex >= 0) explicitIndex else findImplicitAnswerStart(lines)
        if (startIndex < 0) return text to null
        val questionArea = lines.take(startIndex).joinToString("\n").trim()
        val answerArea = lines.drop(if (explicitIndex >= 0) startIndex + 1 else startIndex).joinToString("\n").trim()
        return questionArea to answerArea
    }

    fun parse(text: String): List<ParsedAnswerEntry> {
        val (_, answerArea) = splitSections(text)
        val sectionText = answerArea?.takeIf { it.isNotBlank() } ?: text
        val parsed = AnswerParser.parse(sectionText)
        if (parsed.isNotEmpty()) return parsed

        val blocks = QuestionBlockSplitter.split(sectionText, allowUnnumbered = false)
        return blocks.mapNotNull { block ->
            val combined = block.lines.joinToString("\n")
            val entries = AnswerParser.parse("${block.number}. $combined")
            entries.firstOrNull()
        }
    }


    private val localSubjectiveAnswerHeadingRegex = Regex(
        """^\s*(?:本题)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)\s*[:：]?\s*$"""
    )
    private val numberedQuestionLineRegex = Regex(
        """^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：)）]\s*(.*)$"""
    )
    private val subjectiveQuestionCueRegex = Regex(
        """^(?:请|简述|简答|论述|阐述|概括|分析|说明|解释|谈谈|指出|列举|试述|为什么|如何|结合|根据|什么是|有哪些|有何|提出)"""
    )
    private val answerMarkerWithTailRegex = Regex(
        """^\s*(?:(?:[\[【]\s*(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)\s*[\]】]\s*.*)|(?:(?:本题)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)\s*(?:[:：]|为)\s*.*))$"""
    )

    private fun findExplicitAnswerStart(lines: List<String>): Int {
        for (index in lines.indices) {
            val line = lines[index].trim()
            if (!SectionTitleParser.isAnswerSectionHeading(line)) continue
            if (isLocalSubjectiveAnswerHeading(lines, index)) continue
            return index
        }
        return -1
    }

    /**
     * “参考答案：”既可能是整卷集中答案标题，也可能是当前简答题的答案起点。
     * 当它位于主观题之后，且能确认是逐题答案结构（存在下一道明确主观题、仅有一道题，
     * 或前面题块都已经各自带有答案标记）时，应保留在当前题块中，不能提前截断整份题目文本。
     */
    private fun isLocalSubjectiveAnswerHeading(lines: List<String>, headingIndex: Int): Boolean {
        val heading = lines.getOrNull(headingIndex)?.trim().orEmpty()
        if (!localSubjectiveAnswerHeadingRegex.matches(heading)) return false

        val prefix = lines.take(headingIndex).joinToString("\n").trim()
        if (prefix.isBlank()) return false
        val prefixBlocks = QuestionBlockSplitter.split(prefix, allowUnnumbered = false)
        val currentBlock = prefixBlocks.lastOrNull() ?: return false
        val currentNumber = currentBlock.number.substringBefore('-').toIntOrNull() ?: return false
        val currentStem = currentBlock.lines.joinToString(" ").trim()
        val isSubjective = currentBlock.forcedType == QuestionType.SHORT ||
            subjectiveQuestionCueRegex.containsMatchIn(currentStem)
        if (!isSubjective) return false

        if (hasDefiniteNextSubjectiveQuestion(lines, headingIndex, currentNumber)) return true
        if (prefixBlocks.size == 1) return true

        // 多道主观题逐题给出答案时，前面题块已经各自带有答案标记；
        // 最后一题的独立“参考答案：”也应继续按局部答案处理。
        return prefixBlocks.dropLast(1).all { block ->
            block.lines.any(::isAnswerMarkerLine)
        }
    }

    private fun hasDefiniteNextSubjectiveQuestion(
        lines: List<String>,
        headingIndex: Int,
        currentNumber: Int
    ): Boolean {
        for (index in (headingIndex + 1) until lines.size) {
            val line = lines[index].trim()
            if (line.isBlank()) continue
            val match = numberedQuestionLineRegex.find(line) ?: continue
            val number = match.groupValues[1].toIntOrNull() ?: continue
            if (number != currentNumber + 1) continue

            val remainder = match.groupValues[2].trim()
            if (!looksLikeSubjectiveQuestionRemainder(remainder)) continue
            if (hasOwnAnswerMarkerAhead(lines, index)) return true
        }
        return false
    }

    private fun looksLikeSubjectiveQuestionRemainder(content: String): Boolean {
        val normalized = content.trim()
        if (normalized.isBlank()) return false
        if (normalized.endsWith("？") || normalized.endsWith("?")) return true
        return subjectiveQuestionCueRegex.containsMatchIn(normalized)
    }

    private fun isAnswerMarkerLine(line: String): Boolean {
        val normalized = line.trim()
        return localSubjectiveAnswerHeadingRegex.matches(normalized) ||
            answerMarkerWithTailRegex.matches(normalized)
    }

    private fun hasOwnAnswerMarkerAhead(lines: List<String>, questionIndex: Int): Boolean {
        var nonBlankCount = 0
        for (index in (questionIndex + 1) until lines.size) {
            val line = lines[index].trim()
            if (line.isBlank()) continue
            nonBlankCount += 1
            if (isAnswerMarkerLine(line)) return true

            val nextQuestion = numberedQuestionLineRegex.find(line)
            if (nextQuestion != null && looksLikeSubjectiveQuestionRemainder(nextQuestion.groupValues[2])) {
                return false
            }
            if (SectionTitleParser.isSectionHeading(line)) return false
            if (nonBlankCount >= 12) return false
        }
        return false
    }

    private fun findImplicitAnswerStart(lines: List<String>): Int {
        if (lines.size < 8) return -1
        val startAt = (lines.size * 0.25).toInt().coerceAtLeast(0)
        for (index in startAt until lines.size) {
            if (!answerEntryWithAnalysisRegex.containsMatchIn(lines[index].trim())) continue
            val nextWindow = lines.drop(index).take(12)
            val hitCount = nextWindow.count { answerEntryWithAnalysisRegex.containsMatchIn(it.trim()) }
            if (hitCount >= 3) return index
        }
        return -1
    }
}
