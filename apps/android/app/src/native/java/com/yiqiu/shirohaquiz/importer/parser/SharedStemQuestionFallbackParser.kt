package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.Question

/**
 * Low-priority fallback for shared-stem / material questions.
 *
 * The app does not store parent-question + sub-question structures yet, so this parser rewrites
 * an explicit shared stem group into normal numbered questions by copying the shared stem into
 * every child question, then delegates to the standard parser.
 */
object SharedStemQuestionFallbackParser {
    private data class SharedIntro(
        val startNumber: Int? = null,
        val endNumber: Int? = null,
        val explicitLabel: Boolean = false
    )

    private data class ParsedQuestionStart(
        val number: String,
        val numberValue: Int?,
        val remainder: String
    )

    private data class ChildBlock(
        val start: ParsedQuestionStart,
        val lines: List<String>
    )

    private val sharedLabelRegex = Regex(
        """^\s*[【\[（(〔〖《]?\s*(?:共用题干题|共用题干|共用材料题|共用材料|共用资料题|共用资料|共用案例题|共用案例|共用病例题|共用病例)\s*[】\]）)〕〗》]?\s*$"""
    )

    private val materialIntroRegex = Regex(
        """^\s*(?:[一二三四五六七八九十0-9]+[、.．:：]\s*)?根据(?:以下|下列|上述|给定)?(?:资料|材料|图表|统计资料|病例|案例|病历).*回答\s*(\d{1,4})\s*[~～\-—至到]\s*(\d{1,4})\s*题\s*[。.:：]?\s*$"""
    )

    private val strictQuestionStartRegex = Regex(
        """^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：)）]\s*(.*)$"""
    )

    private val bracketQuestionStartRegex = Regex(
        """^\s*[【\[]\s*(\d{1,4})\s*[】\]]\s*(.*)$"""
    )

    private val stemPrefixRegex = Regex(
        """^\s*(?:题干|材料|资料|案例|病例|病历摘要|共用题干|共用材料|共用资料|共用病例)\s*[:：]\s*(.*)$"""
    )

    private val optionStartRegex = Regex("""^\s*(?:[A-Ha-h]\s*[.、．:：)）]|[\(（\[【〔〖《]\s*[A-Ha-h]\s*[\)）\]】〕〗》])""")
    private val answerLineRegex = Regex("""^\s*(?:[\[【]\s*)?(?:本题)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)(?:\s*[\]】])?\s*(?:[:：]|为)?""")
    private val analysisLineRegex = Regex("""^\s*(?:(?:[\[【]\s*(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*[\]】]\s*)|(?:(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*[:：]\s*))""")

    fun parse(text: String): List<Question> {
        val rewritten = rewrite(text) ?: return emptyList()
        return StandardQuestionParser.parse(rewritten)
    }

    fun rewrite(text: String): String? {
        val lines = text.lines()
        val output = mutableListOf<String>()
        var index = 0
        var rewrittenGroupCount = 0

        while (index < lines.size) {
            val rawLine = lines[index]
            val line = rawLine.trim()
            val intro = parseSharedIntro(line)
            if (intro == null) {
                output += rawLine
                index += 1
                continue
            }

            val groupStart = index
            index += 1

            val stemLines = mutableListOf<String>()
            while (index < lines.size) {
                val current = lines[index].trim()
                if (current.isBlank()) {
                    if (stemLines.isNotEmpty()) stemLines += ""
                    index += 1
                    continue
                }
                if (parseQuestionStart(current) != null) break
                if (parseSharedIntro(current) != null || SectionTitleParser.parse(current) != null) break
                stemLines += normalizeStemLine(current)
                index += 1
            }

            val children = mutableListOf<ChildBlock>()
            var currentStart: ParsedQuestionStart? = null
            var currentLines = mutableListOf<String>()

            fun flushChild() {
                val start = currentStart ?: return
                val cleanLines = currentLines.map { it.trim() }.filter { it.isNotBlank() }
                if (cleanLines.isNotEmpty()) {
                    children += ChildBlock(start = start, lines = cleanLines)
                }
                currentStart = null
                currentLines = mutableListOf()
            }

            while (index < lines.size) {
                val currentRaw = lines[index]
                val current = currentRaw.trim()
                if (current.isBlank()) {
                    currentLines += currentRaw
                    index += 1
                    continue
                }
                if (parseSharedIntro(current) != null) break
                val section = SectionTitleParser.parse(current)
                if (section != null && (section.isAnswerSection || children.isNotEmpty())) break

                val start = parseQuestionStart(current)
                if (start != null) {
                    if (intro.endNumber != null && start.numberValue != null && start.numberValue > intro.endNumber && children.isNotEmpty()) {
                        break
                    }
                    if (intro.startNumber != null && start.numberValue != null && start.numberValue < intro.startNumber && children.isEmpty()) {
                        output += lines.subList(groupStart, index + 1)
                        index += 1
                        currentStart = null
                        currentLines = mutableListOf()
                        break
                    }
                    flushChild()
                    currentStart = start
                    currentLines = mutableListOf(currentRaw)
                    index += 1
                    continue
                }

                if (currentStart == null) break
                currentLines += currentRaw
                index += 1
            }
            flushChild()

            val cleanedStem = stemLines
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .trim()

            val validChildren = children.filter { child ->
                intro.startNumber == null || intro.endNumber == null || child.start.numberValue == null ||
                    child.start.numberValue in intro.startNumber..intro.endNumber
            }

            val hasEnoughChildren = validChildren.size >= 2
            val stemLooksUseful = cleanedStem.length >= 4
            if (!hasEnoughChildren || !stemLooksUseful) {
                output += lines.subList(groupStart, index)
                continue
            }

            validChildren.forEach { child ->
                output += buildRewrittenChildLines(cleanedStem, child)
                output += ""
            }
            rewrittenGroupCount += 1
        }

        return if (rewrittenGroupCount > 0) output.joinToString("\n") else null
    }

    private fun parseSharedIntro(line: String): SharedIntro? {
        if (sharedLabelRegex.matches(line)) {
            return SharedIntro(explicitLabel = true)
        }
        materialIntroRegex.find(line)?.let { match ->
            val start = match.groupValues[1].toIntOrNull()
            val end = match.groupValues[2].toIntOrNull()
            if (start != null && end != null && end > start) {
                return SharedIntro(startNumber = start, endNumber = end)
            }
        }
        return null
    }

    private fun parseQuestionStart(line: String): ParsedQuestionStart? {
        bracketQuestionStartRegex.find(line)?.let { match ->
            val number = match.groupValues[1]
            return ParsedQuestionStart(
                number = number,
                numberValue = number.toIntOrNull(),
                remainder = match.groupValues[2].trim()
            )
        }
        strictQuestionStartRegex.find(line)?.let { match ->
            val number = match.groupValues[1]
            val remainder = match.groupValues[2].trim()
            if (remainder.isBlank()) return null
            if (optionStartRegex.containsMatchIn(line) || answerLineRegex.containsMatchIn(line) || analysisLineRegex.containsMatchIn(line)) return null
            return ParsedQuestionStart(
                number = number,
                numberValue = number.toIntOrNull(),
                remainder = remainder
            )
        }
        return null
    }

    private fun normalizeStemLine(line: String): String {
        stemPrefixRegex.find(line)?.let { match ->
            return match.groupValues[1].trim()
        }
        return line.trim()
    }

    private fun buildRewrittenChildLines(stem: String, child: ChildBlock): List<String> {
        val originalLines = child.lines
        val firstLineRemainder = child.start.remainder.trim()
        val result = mutableListOf<String>()
        val stemParts = stem.lines().map { it.trim() }.filter { it.isNotBlank() }
        val firstStemLine = stemParts.firstOrNull().orEmpty()
        result += "${child.start.number}．$firstStemLine"
        stemParts.drop(1).forEach { result += it }
        if (firstLineRemainder.isNotBlank()) result += firstLineRemainder
        originalLines.drop(1).forEach { result += it.trim() }
        return result
    }
}
