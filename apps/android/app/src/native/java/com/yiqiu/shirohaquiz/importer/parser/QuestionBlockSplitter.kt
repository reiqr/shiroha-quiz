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
    private data class ParsedQuestionStart(
        val number: String,
        val remainder: String,
        val forcedType: QuestionType? = null
    )

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
    private val answerLineRegex = Regex("""^\s*(?:[\[【]\s*)?(?:本题)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)(?:\s*[\]】])?\s*(?:[:：]|为)?""")
    private val analysisLineRegex = Regex("""^\s*(?:(?:[\[【]\s*(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*[\]】]\s*)|(?:(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*[:：]\s*))""")
    private val embeddedAnswerRegex = Regex("""[\[【]\s*(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答)\s*(?:[:：]|[\]】])|(?:本题)?(?:答案|正确答案|参考答案|标准答案)\s*为""")
    private val subjectiveQuestionTypeRegex = Regex("""(?:[【\[（(〔〖《]\s*)?(?:简答题|问答题|面试题|结构化面试题|公考面试题|公务员面试题|材料分析题|案例分析题|名词解释|论述题|综合题)(?:\s*[】\]）)〕〗》])?""")
    private val subjectiveContinuationMarkerRegex = Regex("""^\s*(?:参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答)\s*[:：]""")
    private val materialIntroLineRegex = Regex(
        """^\s*(?:[一二三四五六七八九十0-9]+[、.．:：]\s*)?根据(?:以下|下列|上述|给定)?(?:资料|材料|图表|统计资料).*回答\s*\d{1,4}\s*[~～\-—至到]\s*\d{1,4}\s*题\s*[。.:：]?\s*$"""
    )
    private val standaloneNumericTableValueRegex = Regex(
        """^\s*[+-]?\d+(?:\s*[.．]\s*\d+)?(?:[Ee][+-]?\d+)?\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val unnumberedObjectiveMarkerRegex = Regex(
        """[（(]\s*(?:([A-Ga-g]{1,7})|(对|错|正确|错误|√|×|True|False))\s*[)）]""",
        RegexOption.IGNORE_CASE
    )
    private val pureFrontMatterLineRegex = Regex(
        """^(?:[【\[]?\s*)?(?:绝密|密卷|注意事项(?:\s*[:：].*)?|说明\s*[:：]\s*(?:请|考试|答题|作答|时间|考生).*|请(?:认真|仔细)作答|请在规定时间内完成(?:答题|作答)|请将答案(?:填写|填涂|写在|写到).*|请用\s*2B.*|请勿.*|答题前.*|答题卡.*|考试时间\s*[:：].*|时间\s*[:：].*|在考试结束.*|考试结束.*|全部测验到此结束.*|祝各位考生.*|监考老师.*)(?:\s*[】\]])?\s*[。.!！]?$""",
        RegexOption.IGNORE_CASE
    )

    fun split(
        text: String,
        forcedType: QuestionType? = null,
        category: String = "",
        allowUnnumbered: Boolean = true
    ): List<QuestionBlock> {
        val blocks = mutableListOf<QuestionBlock>()
        var currentNumber: String? = null
        var currentLines = mutableListOf<String>()
        var currentCategory = category
        var currentSectionForcedType = forcedType
        var currentForcedType = forcedType
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
                    category = currentCategory,
                    forcedType = currentForcedType,
                    sequence = sequence++
                )
            }
            currentNumber = null
            currentLines = mutableListOf()
            currentForcedType = currentSectionForcedType
        }

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach
            if (currentNumber == null && isPureFrontMatterLine(line)) return@forEach
            if (isMaterialIntroLine(line)) {
                flush()
                skippingMaterialIntro = true
                return@forEach
            }
            SectionTitleParser.parse(line)?.let { section ->
                if (section.isAnswerSection) {
                    flush()
                    skippingGlobalAnswerSection = true
                    skippingMaterialIntro = false
                    return@forEach
                }
                flush()
                currentCategory = section.title.ifBlank { category }
                currentSectionForcedType = section.forcedType ?: forcedType
                currentForcedType = currentSectionForcedType
                skippingGlobalAnswerSection = false
                skippingMaterialIntro = false
                return@forEach
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
                currentNumber = explicitStart.number
                currentForcedType = explicitStart.forcedType ?: currentSectionForcedType
                currentLines = mutableListOf<String>().apply {
                    val remainder = explicitStart.remainder.trim()
                    if (remainder.isNotBlank()) add(remainder)
                }
                return@forEach
            }

            if (currentNumber == null) {
                if (skippingMaterialIntro) return@forEach
                if (allowUnnumbered && (isLikelyUnnumberedQuestionLine(line) || isLikelyTypedQuestionLine(line, forcedType))) {
                    syntheticNumber += 1
                    currentNumber = syntheticNumber.toString()
                    val typed = QuestionTypeLabelParser.extractLeading(line)
                    currentForcedType = typed?.type ?: currentSectionForcedType
                    currentLines += typed?.remainder?.takeIf { it.isNotBlank() } ?: line
                }
                return@forEach
            }

            if (allowUnnumbered && shouldStartNextSyntheticBlock(currentLines, line)) {
                val parentNumber = currentNumber
                flush()
                syntheticNumber += 1
                currentNumber = syntheticQuestionNumber(parentNumber, syntheticNumber)
                val typed = QuestionTypeLabelParser.extractLeading(line)
                currentForcedType = typed?.type ?: currentSectionForcedType
                currentLines += typed?.remainder?.takeIf { it.isNotBlank() } ?: line
            } else {
                currentLines += line
            }
        }

        flush()
        return blocks
    }

    private fun syntheticQuestionNumber(parentNumber: String?, syntheticIndex: Int): String {
        val base = parentNumber?.trim().orEmpty()
        return if (base.isNotBlank()) "$base-$syntheticIndex" else syntheticIndex.toString()
    }

    private fun parseQuestionStart(line: String): ParsedQuestionStart? {
        if (looksLikeStandaloneNumericTableValue(line)) return null
        if (CodeLikeTextGuard.looksLikeNumericCodeOrDataLine(line)) return null
        bracketQuestionStartRegex.find(line)?.let { match ->
            val typed = QuestionTypeLabelParser.extractLeading(match.groupValues[2])
            return ParsedQuestionStart(
                number = match.groupValues[1],
                remainder = typed?.remainder ?: match.groupValues[2],
                forcedType = typed?.type
            )
        }

        interviewQuestionStartRegex.find(line)?.let { match ->
            val rawNumber = listOf(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            val number = normalizeQuestionIndex(rawNumber)
            val rest = match.groupValues[4].trim()
            val isChineseOrdinalOnly = match.groupValues[3].isNotBlank()
            if (number.isNotBlank() && (!isChineseOrdinalOnly || looksLikeInterviewQuestionRemainder(rest))) {
                val typed = QuestionTypeLabelParser.extractLeading(rest)
                return ParsedQuestionStart(
                    number = number,
                    remainder = typed?.remainder ?: rest,
                    forcedType = typed?.type
                )
            }
        }

        strictQuestionStartRegex.find(line)?.let { match ->
            val number = match.groupValues[1]
            if (isInvalidQuestionNumber(number)) return null
            val typed = QuestionTypeLabelParser.extractLeading(match.groupValues[2])
            return ParsedQuestionStart(
                number = number,
                remainder = typed?.remainder ?: match.groupValues[2],
                forcedType = typed?.type
            )
        }

        spacedQuestionStartRegex.find(line)?.let { match ->
            val number = match.groupValues[1]
            val rest = match.groupValues[2]
            if (number.length <= 3 && !isInvalidQuestionNumber(number) && !looksLikeYearPrefix(number, rest)) {
                val typed = QuestionTypeLabelParser.extractLeading(rest)
                return ParsedQuestionStart(
                    number = number,
                    remainder = typed?.remainder ?: rest,
                    forcedType = typed?.type
                )
            }
        }

        gluedQuestionStartRegex.find(line)?.let { match ->
            val number = match.groupValues[1]
            val rest = match.groupValues[2]
            if (number.length <= 2 && !isInvalidQuestionNumber(number) && rest.isNotBlank()) {
                val typed = QuestionTypeLabelParser.extractLeading(rest)
                return ParsedQuestionStart(
                    number = number,
                    remainder = typed?.remainder ?: rest,
                    forcedType = typed?.type
                )
            }
        }

        return null
    }

    private fun isInvalidQuestionNumber(number: String): Boolean {
        return number.trim().toIntOrNull() == 0
    }

    private fun looksLikeStandaloneNumericTableValue(line: String): Boolean {
        val normalized = line.trim()
        if (!standaloneNumericTableValueRegex.matches(normalized)) return false
        val digits = normalized.count { it.isDigit() }
        if (digits >= 4) return true
        if (Regex("""[.．Ee]""", RegexOption.IGNORE_CASE).containsMatchIn(normalized)) return true
        return normalized == "0"
    }

    private fun looksLikeYearPrefix(number: String, rest: String): Boolean {
        return number.length == 4 && rest.startsWith("年")
    }

    private fun isMaterialIntroLine(line: String): Boolean {
        return Regex("""^\s*材料[一二三四五六七八九十0-9]+\s*[:：]""").containsMatchIn(line) ||
            materialIntroLineRegex.containsMatchIn(line)
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
        return parseQuestionStart(line)?.forcedType != null
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
        if (CompactQuestionRepair.isStandardOptionLine(nextLine)) return false
        val hasOption = currentLines.any {
            CompactQuestionRepair.isStandardOptionLine(it) || CompactQuestionRepair.hasCompactOptionSequence(it)
        }
        val hasAnswer = currentLines.any { embeddedAnswerRegex.containsMatchIn(it) || answerLineRegex.containsMatchIn(it) }
        return hasAnswer || hasOption
    }

    private fun isLikelyTypedQuestionLine(line: String, forcedType: QuestionType?): Boolean {
        if (forcedType == null) return false
        if (line.length < 2) return false
        if (CompactQuestionRepair.isStandardOptionLine(line)) return false
        if (answerLineRegex.containsMatchIn(line) || analysisLineRegex.containsMatchIn(line)) return false
        if (SectionTitleParser.isSectionHeading(line)) return false
        return true
    }

    private fun isPureFrontMatterLine(line: String): Boolean {
        val normalized = line.trim()
        if (normalized.isBlank()) return false
        return pureFrontMatterLineRegex.matches(normalized)
    }

    private fun isLikelyUnnumberedQuestionLine(line: String): Boolean {
        if (line.length < 4) return false
        if (isPureFrontMatterLine(line)) return false
        if (Regex("""^(?:用途|说明|备注|注意|提示)\s*[:：]""").containsMatchIn(line)) return false
        if (Regex("""^\s*[\[【]\s*(?:待确认|备注|提示|说明|注|注意)""").containsMatchIn(line)) return false
        if (CompactQuestionRepair.isStandardOptionLine(line)) return false
        if (answerLineRegex.containsMatchIn(line) || analysisLineRegex.containsMatchIn(line)) return false
        if (SectionTitleParser.isSectionHeading(line)) return false
        QuestionTypeLabelParser.extractLeading(line)?.let { typed ->
            if (typed.remainder.isNotBlank()) return true
        }
        if (embeddedAnswerRegex.containsMatchIn(line)) return true
        if (CodeLikeTextGuard.looksLikeStandaloneCodeExpression(line)) return false
        if (hasUnprotectedObjectiveMarker(line)) return true
        if (CodeLikeTextGuard.hasUnprotectedEmptyParentheses(line)) return true
        if (Regex("""^(?:问题|题目|请回答|请谈谈|谈谈|你怎么看|你怎么处理)""").containsMatchIn(line)) return true
        if (Regex("""[?？。]$""").containsMatchIn(line)) return true
        return false
    }

    private fun hasUnprotectedObjectiveMarker(line: String): Boolean {
        return unnumberedObjectiveMarkerRegex.findAll(line).any { match ->
            val token = match.groupValues[1].ifBlank { match.groupValues[2] }
            !CodeLikeTextGuard.isProtectedParenthesizedToken(line, match.range, token)
        }
    }

}
