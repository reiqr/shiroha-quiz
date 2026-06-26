package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.QuestionType

data class QuestionBlock(
    val number: String,
    val lines: List<String>,
    val category: String = "",
    val forcedType: QuestionType? = null,
    val sequence: Int = 0,
    val numberGenerated: Boolean = false
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
    private val subjectiveAnswerMarkerWithTailRegex = Regex(
        """^\s*(?:(?:[\[【]\s*(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)\s*[\]】]\s*)|(?:(?:本题)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)\s*(?:[:：]|为)\s*))(.*)$"""
    )
    private val bareSubjectiveAnswerMarkerRegex = Regex(
        """^\s*(?:本题)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)\s*$"""
    )
    private val analysisLineRegex = Regex("""^\s*(?:(?:[\[【]\s*(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*[\]】]\s*)|(?:(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*[:：]\s*))""")
    private val embeddedAnswerRegex = Regex("""[\[【]\s*(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答)\s*(?:[:：]|[\]】])|(?:本题)?(?:答案|正确答案|参考答案|标准答案)\s*为""")
    private val subjectiveQuestionTypeRegex = Regex("""(?:[【\[（(〔〖《]\s*)?(?:简答题|问答题|面试题|结构化面试题|公考面试题|公务员面试题|材料分析题|案例分析题|名词解释|论述题|综合题)(?:\s*[】\]）)〕〗》])?""")
    private val subjectiveContinuationMarkerRegex = Regex("""^\s*(?:参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答)\s*[:：]?\s*$""")
    private val subjectiveQuestionCueRegex = Regex(
        """(?:请)?(?:简述|简答|论述|阐述|概括|分析|说明|解释|谈谈|指出|列举|试述|为什么|如何|有何|哪些|什么是|提出.{0,12}(?:措施|建议|对策)|说明.{0,12}(?:原因|意义|作用|原则|内容))"""
    )
    private val subjectiveAnswerIntroRegex = Regex(
        """(?:如下|以下|主要包括|包括以下|可概括为|分为以下|有以下|要点|措施|原则|原因|意义|作用)\s*[:：]?$"""
    )
    private val numberedSubjectiveItemRegex = Regex(
        """^\s*(?:(\d{1,2})\s*[.、．:：)）]|[（(]\s*(\d{1,2})\s*[)）])\s*(.+)$"""
    )
    private val chineseSubjectiveItemRegex = Regex(
        """^\s*(?:[一二三四五六七八九十]+\s*[、.．:：)）]|(?:首先|其次|再次|最后|第一|第二|第三|第四|第五|一是|二是|三是|四是|五是))"""
    )
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
        var currentNumberGenerated = false
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
                    sequence = sequence++,
                    numberGenerated = currentNumberGenerated
                )
            }
            currentNumber = null
            currentLines = mutableListOf()
            currentForcedType = currentSectionForcedType
            currentNumberGenerated = false
        }

        val sourceLines = text.lineSequence().toList()
        sourceLines.forEachIndexed { lineIndex, rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEachIndexed
            if (currentNumber == null && isPureFrontMatterLine(line)) return@forEachIndexed
            if (isMaterialIntroLine(line)) {
                flush()
                skippingMaterialIntro = true
                return@forEachIndexed
            }
            if (
                currentNumber != null &&
                shouldAttachSubjectiveAnswerMarkerToCurrentBlock(
                    currentLines = currentLines,
                    line = line,
                    forcedType = currentForcedType
                )
            ) {
                currentLines += line
                skippingMaterialIntro = false
                return@forEachIndexed
            }
            SectionTitleParser.parse(line)?.let { section ->
                if (section.isAnswerSection) {
                    flush()
                    skippingGlobalAnswerSection = true
                    skippingMaterialIntro = false
                    return@forEachIndexed
                }
                flush()
                currentCategory = section.title.ifBlank { category }
                currentSectionForcedType = section.forcedType ?: forcedType
                currentForcedType = currentSectionForcedType
                skippingGlobalAnswerSection = false
                skippingMaterialIntro = false
                return@forEachIndexed
            }

            if (skippingGlobalAnswerSection) return@forEachIndexed

            val activeNumber = currentNumber
            if (
                activeNumber != null &&
                shouldKeepAsSubjectiveAnswerContinuation(
                    currentLines = currentLines,
                    line = line,
                    currentNumber = activeNumber,
                    forcedType = currentForcedType,
                    sourceLines = sourceLines,
                    lineIndex = lineIndex
                )
            ) {
                currentLines += line
                return@forEachIndexed
            }

            val explicitStart = parseQuestionStart(line)
            if (explicitStart != null) {
                flush()
                skippingMaterialIntro = false
                currentNumber = explicitStart.number
                currentNumberGenerated = false
                currentForcedType = explicitStart.forcedType ?: currentSectionForcedType
                currentLines = mutableListOf<String>().apply {
                    val remainder = explicitStart.remainder.trim()
                    if (remainder.isNotBlank()) add(remainder)
                }
                return@forEachIndexed
            }

            if (currentNumber == null) {
                if (skippingMaterialIntro) return@forEachIndexed
                if (allowUnnumbered && (isLikelyUnnumberedQuestionLine(line) || isLikelyTypedQuestionLine(line, forcedType))) {
                    syntheticNumber += 1
                    currentNumber = syntheticNumber.toString()
                    currentNumberGenerated = true
                    val typed = QuestionTypeLabelParser.extractLeading(line)
                    currentForcedType = typed?.type ?: currentSectionForcedType
                    currentLines += typed?.remainder?.takeIf { it.isNotBlank() } ?: line
                }
                return@forEachIndexed
            }

            if (allowUnnumbered && shouldStartNextSyntheticBlock(currentLines, line)) {
                val parentNumber = currentNumber
                flush()
                syntheticNumber += 1
                currentNumber = syntheticQuestionNumber(parentNumber, syntheticNumber)
                currentNumberGenerated = true
                val typed = QuestionTypeLabelParser.extractLeading(line)
                currentForcedType = typed?.type ?: currentSectionForcedType
                currentLines += typed?.remainder?.takeIf { it.isNotBlank() } ?: line
            } else {
                currentLines += line
            }
        }

        flush()
        return normalizeGeneratedQuestionNumbers(blocks)
    }

    private fun normalizeGeneratedQuestionNumbers(blocks: List<QuestionBlock>): List<QuestionBlock> {
        if (blocks.none { it.numberGenerated }) return blocks

        // Explicit numbers remain untouched. Auto-generated numbers are assigned from the
        // final question order so an unnumbered paper is displayed as 1, 2, 3, ... instead
        // of exposing temporary parent-child numbers such as 1-2 or 1-3.
        val explicitNumericNumbers = blocks
            .asSequence()
            .filterNot { it.numberGenerated }
            .mapNotNull { it.number.trim().toIntOrNull() }
            .filter { it > 0 }
            .toMutableSet()
        val generatedNumbers = mutableSetOf<Int>()

        return blocks.mapIndexed { index, block ->
            if (!block.numberGenerated) return@mapIndexed block

            var candidate = index + 1
            while (candidate in explicitNumericNumbers || candidate in generatedNumbers) {
                candidate += 1
            }
            generatedNumbers += candidate
            block.copy(number = candidate.toString())
        }
    }

    private fun syntheticQuestionNumber(parentNumber: String?, syntheticIndex: Int): String {
        // A previous synthetic split may already have appended a suffix (for example, 1-2).
        // Always reuse the original root number so later splits become 1-3, 1-4, ...
        // instead of recursively growing into 1-2-3-4-....
        val base = parentNumber
            ?.trim()
            ?.substringBefore('-')
            .orEmpty()
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

    private data class NumberedSubjectiveItem(
        val number: Int,
        val content: String
    )

    private fun shouldKeepAsSubjectiveAnswerContinuation(
        currentLines: List<String>,
        line: String,
        currentNumber: String,
        forcedType: QuestionType?,
        sourceLines: List<String>,
        lineIndex: Int
    ): Boolean {
        val answerMarkerIndex = findSubjectiveAnswerMarkerIndex(currentLines, forcedType)
        if (answerMarkerIndex < 0) return false
        if (looksLikeTypedQuestionStart(line)) return false
        if (Regex("""^\s*\d{2,4}\s*[.、．:：)）]""").containsMatchIn(line)) return false
        if (Regex("""^\s*(?:问题|题目|第\s*[一二三四五六七八九十百0-9]+\s*(?:题|问|道题|个问题))""").containsMatchIn(line)) return false

        val numberedItem = parseNumberedSubjectiveItem(line)
        if (numberedItem == null) {
            return chineseSubjectiveItemRegex.containsMatchIn(line)
        }

        val previousItemNumbers = currentLines
            .drop(answerMarkerIndex + 1)
            .mapNotNull(::parseNumberedSubjectiveItem)
            .map { it.number }
        val lastItemNumber = previousItemNumbers.lastOrNull()
        val currentQuestionIndex = currentNumber.substringBefore('-').toIntOrNull()
        val isExpectedOfficialNext = currentQuestionIndex != null && numberedItem.number == currentQuestionIndex + 1
        val rollsBackFromAnswerItems = lastItemNumber != null && numberedItem.number <= lastItemNumber
        val breaksAnswerItemSequence = lastItemNumber != null && numberedItem.number != lastItemNumber + 1
        val hasBlankSeparator = sourceLines
            .subList((lineIndex - 2).coerceAtLeast(0), lineIndex)
            .any { it.isBlank() }
        val looksLikeQuestion = looksLikeSubjectiveQuestionRemainder(numberedItem.content)
        val hasOwnAnswerMarkerAhead = hasAnswerMarkerAheadBeforeNextQuestion(sourceLines, lineIndex)

        val isHighConfidenceNextQuestion = isExpectedOfficialNext && (
            hasOwnAnswerMarkerAhead ||
                (rollsBackFromAnswerItems && looksLikeQuestion) ||
                (breaksAnswerItemSequence && looksLikeQuestion) ||
                (lastItemNumber == null && hasBlankSeparator && looksLikeQuestion)
            )
        if (isHighConfidenceNextQuestion) return false

        if (lastItemNumber == null) {
            return numberedItem.number == 1 || !isExpectedOfficialNext
        }
        if (numberedItem.number == lastItemNumber + 1) return true
        if (numberedItem.number > lastItemNumber) return true
        return !isExpectedOfficialNext
    }

    private fun shouldAttachSubjectiveAnswerMarkerToCurrentBlock(
        currentLines: List<String>,
        line: String,
        forcedType: QuestionType?
    ): Boolean {
        if (currentLines.isEmpty()) return false
        if (extractSubjectiveAnswerMarkerTail(line) == null) return false
        if (currentLines.any { CompactQuestionRepair.isStandardOptionLine(it) }) return false
        if (forcedType == QuestionType.SHORT) return true

        val questionPart = currentLines.joinToString("\n")
        if (subjectiveQuestionTypeRegex.containsMatchIn(questionPart)) return true
        return subjectiveQuestionCueRegex.containsMatchIn(questionPart)
    }

    private fun findSubjectiveAnswerMarkerIndex(
        currentLines: List<String>,
        forcedType: QuestionType?
    ): Int {
        val markerIndex = currentLines.indexOfLast { extractSubjectiveAnswerMarkerTail(it) != null }
        if (markerIndex < 0) return -1
        if (forcedType == QuestionType.SHORT) return markerIndex

        val questionPart = currentLines.take(markerIndex).joinToString("\n")
        if (subjectiveQuestionTypeRegex.containsMatchIn(questionPart)) return markerIndex
        if (subjectiveQuestionCueRegex.containsMatchIn(questionPart)) return markerIndex

        val markerLine = currentLines[markerIndex]
        if (subjectiveContinuationMarkerRegex.containsMatchIn(markerLine)) return markerIndex
        val markerTail = extractSubjectiveAnswerMarkerTail(markerLine).orEmpty()
        if (markerTail.isBlank() || subjectiveAnswerIntroRegex.containsMatchIn(markerTail)) return markerIndex
        return -1
    }

    private fun extractSubjectiveAnswerMarkerTail(line: String): String? {
        subjectiveAnswerMarkerWithTailRegex.find(line)?.let { match ->
            return match.groupValues[1].trim()
        }
        if (bareSubjectiveAnswerMarkerRegex.matches(line)) return ""
        return null
    }

    private fun parseNumberedSubjectiveItem(line: String): NumberedSubjectiveItem? {
        val match = numberedSubjectiveItemRegex.find(line) ?: return null
        val number = match.groupValues[1].ifBlank { match.groupValues[2] }.toIntOrNull() ?: return null
        val content = match.groupValues[3].trim()
        if (content.isBlank()) return null
        return NumberedSubjectiveItem(number = number, content = content)
    }

    private fun looksLikeSubjectiveQuestionRemainder(content: String): Boolean {
        val normalized = content.trim()
        if (normalized.isBlank()) return false
        if (normalized.endsWith("？") || normalized.endsWith("?")) return true
        return Regex(
            """^(?:请|简述|简答|论述|阐述|概括|分析|说明|解释|谈谈|指出|列举|试述|为什么|如何|结合|根据|什么是|有哪些|有何|提出)"""
        ).containsMatchIn(normalized)
    }

    private fun hasAnswerMarkerAheadBeforeNextQuestion(
        sourceLines: List<String>,
        currentLineIndex: Int
    ): Boolean {
        var nonBlankCount = 0
        for (index in (currentLineIndex + 1) until sourceLines.size) {
            val nextLine = sourceLines[index].trim()
            if (nextLine.isBlank()) continue
            nonBlankCount += 1
            if (extractSubjectiveAnswerMarkerTail(nextLine) != null) return true
            if (parseQuestionStart(nextLine) != null) return false
            if (SectionTitleParser.isSectionHeading(nextLine)) return false
            if (nonBlankCount >= 12) return false
        }
        return false
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
