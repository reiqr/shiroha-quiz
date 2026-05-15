package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.QuestionType

data class QuestionBlock(
    val number: String,
    val lines: List<String>,
    val category: String = "",
    val forcedType: QuestionType? = null,
    val sequence: Int = 0
)

object QuestionBlockSplitter {
    private val strictQuestionStartRegex = Regex(
        """^\s*(?:第\s*)?(\d{1,4})\s*(?:题)?\s*[.、．:：)）]\s*(.*)$"""
    )
    private val bracketQuestionStartRegex = Regex(
        """^\s*[【\[]\s*(\d{1,4})\s*[】\]]\s*(.*)$"""
    )
    private val interviewQuestionStartRegex = Regex(
        """^\s*(?:(?:问题|题目)\s*([一二三四五六七八九十百0-9]{1,4})|第\s*([一二三四五六七八九十百0-9]{1,4})\s*(?:题|问|道题|个问题)|([一二三四五六七八九十百]+)\s*(?:、|．|\.))\s*[.、．:：)）]?\s*(.*)$"""
    )
    private val spacedQuestionStartRegex = Regex(
        """^\s*(\d{1,3})\s+(.+)$"""
    )
    private val gluedQuestionStartRegex = Regex(
        """^\s*(\d{1,2})(?=[\u4e00-\u9fa5A-Za-z])(.*)$"""
    )
    private val optionStartRegex = Regex("""^\s*[A-Ga-g]\s*[.、．:：)）]""")
    private val answerLineRegex = Regex("""^\s*(?:[\[【]\s*)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)(?:\s*[\]】])?\s*[:：]?""")
    private val analysisLineRegex = Regex("""^\s*(?:[\[【]\s*)?(?:解析|答案解析|说明|解题思路)(?:\s*[\]】])?\s*[:：]?""")
    private val embeddedAnswerRegex = Regex("""[\[【]\s*(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答)\s*(?:[:：]|[\]】])""")
    private val subjectiveQuestionTypeRegex = Regex("""(?:【\s*)?(?:简答题|问答题|面试题|结构化面试题|公考面试题|公务员面试题|材料分析题|案例分析题|论述题|综合题)(?:\s*】)?""")
    private val subjectiveContinuationMarkerRegex = Regex("""^\s*(?:参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答)\s*[:：]""")
    private val globalAnswerSectionRegex = Regex("""(?:集中答案|集中解析|参考答案|标准答案|正确答案|答案解析|答案区|解析区|答案部分|答案(?:与|及)解析)""")

    fun split(
        text: String,
        forcedType: QuestionType? = null,
        category: String = "",
        allowUnnumbered: Boolean = true
    ): List<QuestionBlock> {
        val blocks = mutableListOf<QuestionBlock>()
        var currentNumber: String? = null
        var currentLines = mutableListOf<String>()
        var syntheticNumber = 0
        var sequence = 0
        var skippingGlobalAnswerSection = false
        var skippingMaterialIntro = false

        fun flush() {
            val number = currentNumber ?: return
            val cleanLines = currentLines.map { it.trim() }.filter { it.isNotBlank() }
            if (cleanLines.isNotEmpty()) {
                blocks += QuestionBlock(
                    number = number,
                    lines = cleanLines,
                    category = category,
                    forcedType = forcedType,
                    sequence = sequence++
                )
            }
            currentNumber = null
            currentLines = mutableListOf()
        }

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach
            if (isMaterialIntroLine(line)) {
                flush()
                skippingMaterialIntro = true
                return@forEach
            }
            SectionTitleParser.parse(line)?.let { section ->
                if (!section.isAnswerSection) {
                    flush()
                    skippingGlobalAnswerSection = false
                    skippingMaterialIntro = false
                    return@forEach
                }
                if (isGlobalAnswerSectionHeading(line)) {
                    flush()
                    skippingGlobalAnswerSection = true
                    return@forEach
                }
            }

            if (skippingGlobalAnswerSection) return@forEach

            if (currentNumber != null && shouldKeepAsSubjectiveAnswerContinuation(currentLines, line)) {
                currentLines += line
                return@forEach
            }

            val explicitStart = parseQuestionStart(line)
            if (explicitStart != null) {
                flush()
                skippingMaterialIntro = false
                currentNumber = explicitStart.first
                currentLines = mutableListOf<String>().apply {
                    val remainder = explicitStart.second.trim()
                    if (remainder.isNotBlank()) add(remainder)
                }
                return@forEach
            }

            if (currentNumber == null) {
                if (skippingMaterialIntro) return@forEach
                if (allowUnnumbered && (isLikelyUnnumberedQuestionLine(line) || isLikelyTypedQuestionLine(line, forcedType))) {
                    syntheticNumber += 1
                    currentNumber = syntheticNumber.toString()
                    currentLines += line
                }
                return@forEach
            }

            if (allowUnnumbered && shouldStartNextSyntheticBlock(currentLines, line)) {
                flush()
                syntheticNumber += 1
                currentNumber = syntheticNumber.toString()
                currentLines += line
            } else {
                currentLines += line
            }
        }

        flush()
        return blocks
    }

    private fun parseQuestionStart(line: String): Pair<String, String>? {
        bracketQuestionStartRegex.find(line)?.let { match ->
            return match.groupValues[1] to match.groupValues[2]
        }

        interviewQuestionStartRegex.find(line)?.let { match ->
            val rawNumber = listOf(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            val number = normalizeQuestionIndex(rawNumber)
            val rest = match.groupValues[4].trim()
            val isChineseOrdinalOnly = match.groupValues[3].isNotBlank()
            if (number.isNotBlank() && (!isChineseOrdinalOnly || looksLikeInterviewQuestionRemainder(rest))) {
                return number to rest
            }
        }

        strictQuestionStartRegex.find(line)?.let { match ->
            return match.groupValues[1] to match.groupValues[2]
        }

        spacedQuestionStartRegex.find(line)?.let { match ->
            val number = match.groupValues[1]
            val rest = match.groupValues[2]
            if (number.length <= 3 && !looksLikeYearPrefix(number, rest)) {
                return number to rest
            }
        }

        gluedQuestionStartRegex.find(line)?.let { match ->
            val number = match.groupValues[1]
            val rest = match.groupValues[2]
            if (number.length <= 2 && rest.isNotBlank()) {
                return number to rest
            }
        }

        return null
    }

    private fun looksLikeYearPrefix(number: String, rest: String): Boolean {
        return number.length == 4 && rest.startsWith("年")
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

    private fun isMaterialIntroLine(line: String): Boolean {
        return Regex("""^\s*材料[一二三四五六七八九十0-9]+\s*[:：]""").containsMatchIn(line)
    }

    private fun isGlobalAnswerSectionHeading(line: String): Boolean {
        return globalAnswerSectionRegex.containsMatchIn(line) && !Regex("""^\s*(?:[\[【]\s*)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)(?:\s*[\]】])?\s*[:：]?\s*\S+""").containsMatchIn(line)
    }

    private fun shouldKeepAsSubjectiveAnswerContinuation(currentLines: List<String>, line: String): Boolean {
        if (!hasSubjectiveContinuationContext(currentLines)) return false
        if (looksLikeTypedQuestionStart(line)) return false
        if (Regex("""^\s*\d{2,4}\s*[.、．:：)）]""").containsMatchIn(line)) return false
        if (Regex("""^\s*(?:问题|题目|第\s*[一二三四五六七八九十百0-9]+\s*(?:题|问|道题|个问题))""").containsMatchIn(line)) return false
        return Regex("""^\s*(?:\d{1,2}|[一二三四五六七八九十])\s*[.、．)）]""").containsMatchIn(line) ||
            Regex("""^\s*(?:首先|其次|再次|最后|第一|第二|第三|第四|一是|二是|三是|四是)""").containsMatchIn(line)
    }

    private fun hasSubjectiveContinuationContext(currentLines: List<String>): Boolean {
        if (currentLines.any { subjectiveContinuationMarkerRegex.containsMatchIn(it) }) return true
        val combined = currentLines.joinToString("\n")
        if (!subjectiveQuestionTypeRegex.containsMatchIn(combined)) return false
        return currentLines.any { answerLineRegex.containsMatchIn(it) }
    }

    private fun looksLikeTypedQuestionStart(line: String): Boolean {
        return Regex("""^\s*\d{1,4}\s*[.、．:：)）]\s*(?:[\[【]\s*)?(?:单选题|单项选择题|多选题|多项选择题|判断题|填空题|简答题|问答题|面试题|材料分析题)(?:\s*[\]】])?""").containsMatchIn(line)
    }

    private fun looksLikeInterviewQuestionRemainder(rest: String): Boolean {
        if (rest.isBlank()) return false
        if (SectionTitleParser.isSectionHeading(rest)) return false
        if (Regex("""(?:测试区|样本|题库|格式|边界|极端|客观题|主观题|材料题|集中答案|功能测试)""", RegexOption.IGNORE_CASE).containsMatchIn(rest)) return false
        return Regex("""[?？]""").containsMatchIn(rest) ||
            Regex("""^(?:请|谈谈|你|如何|为什么|是否|如果|根据|结合|概括|指出|分析|提出|围绕|下列|单位|群众|有人认为|某)""").containsMatchIn(rest)
    }

    private fun shouldStartNextSyntheticBlock(currentLines: List<String>, nextLine: String): Boolean {
        if (hasSubjectiveContinuationContext(currentLines)) return false
        if (!isLikelyUnnumberedQuestionLine(nextLine)) return false
        if (optionStartRegex.containsMatchIn(nextLine)) return false
        val hasOption = currentLines.any { optionStartRegex.containsMatchIn(it) || hasMultipleInlineOptions(it) }
        val hasAnswer = currentLines.any { embeddedAnswerRegex.containsMatchIn(it) || answerLineRegex.containsMatchIn(it) }
        return hasAnswer || hasOption
    }

    private fun isLikelyTypedQuestionLine(line: String, forcedType: QuestionType?): Boolean {
        if (forcedType == null) return false
        if (line.length < 2) return false
        if (optionStartRegex.containsMatchIn(line)) return false
        if (answerLineRegex.containsMatchIn(line) || analysisLineRegex.containsMatchIn(line)) return false
        if (SectionTitleParser.isSectionHeading(line)) return false
        return true
    }

    private fun isLikelyUnnumberedQuestionLine(line: String): Boolean {
        if (line.length < 4) return false
        if (Regex("""^(?:用途|说明|备注|注意|提示)\s*[:：]""").containsMatchIn(line)) return false
        if (optionStartRegex.containsMatchIn(line)) return false
        if (answerLineRegex.containsMatchIn(line) || analysisLineRegex.containsMatchIn(line)) return false
        if (SectionTitleParser.isSectionHeading(line)) return false
        if (embeddedAnswerRegex.containsMatchIn(line)) return true
        if (Regex("""[（(]\s*(?:[A-Ga-g]{1,7}|对|错|正确|错误|√|×|True|False)\s*[)）]""", RegexOption.IGNORE_CASE).containsMatchIn(line)) return true
        if (Regex("""[（(]\s*[)）]""").containsMatchIn(line)) return true
        if (Regex("""^(?:问题|题目|请回答|请谈谈|谈谈|你怎么看|你怎么处理)""").containsMatchIn(line)) return true
        if (Regex("""[?？。]$""").containsMatchIn(line)) return true
        return false
    }

    private fun hasMultipleInlineOptions(line: String): Boolean {
        return Regex("""[A-Ga-g]\s*[.、．:：)）]""").findAll(line).take(2).count() >= 2
    }
}
