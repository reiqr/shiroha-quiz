package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType

object QuestionParser {
    fun parseStandard(text: String): List<Question> {
        return StandardQuestionParser.parse(text)
    }

    /**
     * 标准解析作为主路径；只有单个题块出现明确紧凑格式信号，且修复结果结构更完整时，
     * 才替换该题块。正常标准题不会再与整份紧凑解析结果竞争。
     */
    fun parseStandardFirst(
        text: String,
        forcedType: QuestionType? = null,
        category: String = "",
        allowUnnumbered: Boolean = true
    ): List<Question> {
        val blocks = QuestionBlockSplitter.split(
            text = text,
            forcedType = forcedType,
            category = category,
            allowUnnumbered = allowUnnumbered
        )

        return blocks.mapNotNull(::parseBlockStandardFirst)
    }

    fun parseCompact(text: String): List<Question> {
        val preprocessed = CompactQuestionRepair.repair(text)
        return StandardQuestionParser.parse(preprocessed)
    }

    fun looksCompact(text: String): Boolean {
        return CompactQuestionRepair.hasCompactPattern(text)
    }

    fun parseSectioned(text: String): List<Question> {
        val sections = splitBySections(text)
        if (sections.isEmpty()) return parseStandardFirst(text)

        return sections.flatMap { section ->
            parseStandardFirst(
                text = section.body,
                forcedType = section.forcedType,
                category = section.title
            )
        }
    }

    fun looksSectioned(text: String): Boolean {
        return text.lineSequence().any { line ->
            val section = SectionTitleParser.parse(line.trim())
            section != null && !section.isAnswerSection
        }
    }

    private fun parseBlockStandardFirst(block: QuestionBlock): Question? {
        val standard = StandardQuestionParser.parseBlock(block)
        val rawBlock = block.lines.joinToString("\n")
        val expectedOptionKeys = CompactQuestionRepair.compactOptionKeys(rawBlock)
        if (!CompactQuestionRepair.hasCompactPattern(rawBlock)) return standard

        // 明确的主观题中，A./B./C. 很可能只是分项说明，不能按紧凑选择题拆分。
        if (
            expectedOptionKeys.isNotEmpty() &&
            (block.forcedType == QuestionType.SHORT || block.forcedType == QuestionType.BLANK)
        ) {
            return standard
        }
        if (expectedOptionKeys.isNotEmpty() && looksLikeSubjectiveEnumeration(standard)) return standard
        if (expectedOptionKeys.size == 1) return standard
        if (expectedOptionKeys.size >= 2 && isCompleteStandardQuestion(standard, expectedOptionKeys)) return standard

        val repaired = CompactQuestionRepair.repair(rawBlock)
        if (repaired == rawBlock) return standard

        val repairedLines = repaired.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (repairedLines.isEmpty()) return standard

        val fallback = StandardQuestionParser.parseBlock(block.copy(lines = repairedLines))

        if (expectedOptionKeys.isEmpty()) {
            return if (inlineMetadataFallbackCanReplace(standard, fallback)) fallback else standard
        }

        return if (
            localFallbackCanReplace(
                block = block,
                standard = standard,
                fallback = fallback,
                expectedOptionKeys = expectedOptionKeys
            )
        ) {
            fallback
        } else {
            standard
        }
    }

    private fun inlineMetadataFallbackCanReplace(
        standard: Question?,
        fallback: Question?
    ): Boolean {
        fallback ?: return false
        if (fallback.question.isBlank()) return false
        if (standard == null) return fallback.answer.isNotEmpty() || fallback.analysis.isNotBlank()

        if (standard.answer.isNotEmpty() && standard.answer != fallback.answer) return false
        if (standard.analysis.isNotBlank() && standard.analysis != fallback.analysis) return false

        val standardOptions = standard.options.map { it.key to it.text }
        val fallbackOptions = fallback.options.map { it.key to it.text }
        val judgeUpgrade = standard.type == QuestionType.SHORT &&
            fallback.type == QuestionType.JUDGE &&
            standard.options.isEmpty() &&
            fallback.answer.isNotEmpty()
        if (!judgeUpgrade) {
            if (standard.type != fallback.type) return false
            if (!inlineMetadataOptionsPreserved(standardOptions, fallbackOptions)) return false
        }

        val questionPreserved = fallback.question == standard.question ||
            standard.question.startsWith(fallback.question) ||
            standard.question.contains(fallback.question)
        if (!questionPreserved) return false

        val answerImproved = standard.answer.isEmpty() && fallback.answer.isNotEmpty()
        val analysisImproved = standard.analysis.isBlank() && fallback.analysis.isNotBlank()
        return answerImproved || analysisImproved
    }

    private fun inlineMetadataOptionsPreserved(
        standardOptions: List<Pair<String, String>>,
        fallbackOptions: List<Pair<String, String>>
    ): Boolean {
        if (standardOptions.size != fallbackOptions.size) return false
        return standardOptions.zip(fallbackOptions).all { (standardOption, fallbackOption) ->
            if (standardOption.first != fallbackOption.first) return@all false
            if (standardOption.second == fallbackOption.second) return@all true

            val standardText = standardOption.second.trim()
            val fallbackText = fallbackOption.second.trim()
            standardText.startsWith(fallbackText) &&
                Regex("""(?:答案|正确答案|参考答案|标准答案|解析|答案解析|说明)\s*[:：]""")
                    .containsMatchIn(standardText.removePrefix(fallbackText))
        }
    }

    private fun isCompleteStandardQuestion(
        question: Question?,
        expectedOptionKeys: Set<String>
    ): Boolean {
        question ?: return false
        if (question.question.isBlank()) return false

        val isObjective = question.type == QuestionType.SINGLE ||
            question.type == QuestionType.MULTIPLE ||
            question.type == QuestionType.JUDGE
        if (!isObjective || question.options.size < 2) return false

        val optionKeys = question.options.map { it.key.uppercase() }.toSet()
        if (!optionKeys.containsAll(expectedOptionKeys)) return false
        return question.answer.isEmpty() || question.answer.all { it.uppercase() in optionKeys }
    }

    private fun localFallbackCanReplace(
        block: QuestionBlock,
        standard: Question?,
        fallback: Question?,
        expectedOptionKeys: Set<String>
    ): Boolean {
        fallback ?: return false
        if (fallback.question.isBlank()) return false
        if (block.forcedType == QuestionType.SHORT || block.forcedType == QuestionType.BLANK) return false

        val fallbackIsObjective = fallback.type == QuestionType.SINGLE ||
            fallback.type == QuestionType.MULTIPLE ||
            fallback.type == QuestionType.JUDGE
        if (!fallbackIsObjective || fallback.options.size < 2) return false

        val fallbackOptionKeys = fallback.options.map { it.key.uppercase() }.toSet()
        if (!fallbackOptionKeys.containsAll(expectedOptionKeys)) return false
        if (fallback.answer.any { it.uppercase() !in fallbackOptionKeys }) return false

        // “AB / A,B / A B”属于合法的紧凑客观答案，不能因为标准结果暂时是 SHORT
        // 就被当成文本答案拦截；只有无法严格归一化为现有选项键的内容才视为主观答案。
        val standardAnswer = normalizeLocalObjectiveAnswer(standard?.answer.orEmpty(), expectedOptionKeys)
        val fallbackAnswer = normalizeLocalObjectiveAnswer(fallback.answer, expectedOptionKeys)
        val standardHasTextAnswer = standard?.answer.orEmpty().isNotEmpty() && standardAnswer == null
        if (standardHasTextAnswer) {
            if (fallback.answer != standard?.answer) return false
            if (fallback.type != standard.type) return false
        } else if (standardAnswer != null && standardAnswer.isNotEmpty()) {
            if (fallbackAnswer == null || fallbackAnswer != standardAnswer) return false
        }

        return questionStructureScore(fallback, expectedOptionKeys) >
            questionStructureScore(standard, expectedOptionKeys)
    }

    private fun normalizeLocalObjectiveAnswer(
        answers: List<String>,
        expectedOptionKeys: Set<String>
    ): List<String>? {
        if (answers.isEmpty()) return emptyList()
        if (expectedOptionKeys.isEmpty()) return null

        val raw = answers.joinToString(",").trim()
            .trim('[', ']', '【', '】', '(', ')', '（', '）')
            .trim()
        if (raw.isBlank()) return emptyList()

        // 严格限制为选项字母及分隔符，避免把“AB法、A方案、AI”等文本答案拆成字母。
        if (!Regex("""^[A-Ga-g\s,，、;；/\\]+$""").matches(raw)) return null
        val compact = raw.replace(Regex("""[\s,，、;；/\\]+"""), "").uppercase()
        if (compact.isBlank() || !Regex("""^[A-G]+$""").matches(compact)) return null

        val letters = compact.map { it.toString() }
        val normalized = letters.distinct()
        // 重复字母（如 AA / A,A）不是合法多选答案，不能作为题型转换依据。
        if (normalized.size != letters.size) return null
        if (normalized.any { it !in expectedOptionKeys }) return null
        return normalized.sorted()
    }

    private fun looksLikeSubjectiveEnumeration(question: Question?): Boolean {
        question ?: return false
        if (question.type != QuestionType.SHORT && question.type != QuestionType.BLANK) return false

        val stem = question.question.trim()
        val choiceIntent = Regex(
            """(?:下列|以下).{0,24}(?:哪|正确|错误|不正确|符合)|选择|选出|最(?:合适|符合|恰当)|应(?:选择|选)"""
        ).containsMatchIn(stem)
        if (choiceIntent) return false

        val subjectiveLead = Regex(
            """^(?:请|试)?(?:分别)?(?:说明|简述|阐述|论述|比较|解释|概述|谈谈|回答|分析)"""
        ).containsMatchIn(stem)
        val subjectivePurpose = Regex(
            """(?:差异|区别|联系|原因|影响|措施|方法|原则|条件|优缺点|意义|作用|过程|依据|适用|如何|为什么|各自|分别)"""
        ).containsMatchIn(stem)
        val comparisonQuestion = Regex("""(?:有何|有什么).*(?:区别|差异|联系)""").containsMatchIn(stem)
        return (subjectiveLead && subjectivePurpose) || comparisonQuestion
    }

    private fun questionStructureScore(question: Question?, expectedOptionKeys: Set<String>): Int {
        question ?: return Int.MIN_VALUE / 4
        if (question.question.isBlank()) return Int.MIN_VALUE / 4

        var score = 100
        val optionKeys = question.options.map { it.key.uppercase() }.toSet()
        val isObjective = question.type == QuestionType.SINGLE ||
            question.type == QuestionType.MULTIPLE ||
            question.type == QuestionType.JUDGE

        if (expectedOptionKeys.isNotEmpty()) {
            val covered = expectedOptionKeys.count { it in optionKeys }
            score += covered * 35
            score -= (expectedOptionKeys.size - covered) * 55
            score += if (isObjective) 80 else -180
        } else if (isObjective) {
            score += if (question.options.size >= 2) 60 else -100
        }

        if (isObjective) {
            score += when {
                question.options.size >= 2 -> 40
                question.options.isEmpty() -> -100
                else -> -60
            }
            if (question.answer.isNotEmpty()) {
                score += if (question.answer.all { it.uppercase() in optionKeys }) 35 else -90
            }
        } else if (expectedOptionKeys.isNotEmpty()) {
            score -= 120
        }

        if (question.analysis.isNotBlank()) score += 5
        return score
    }

    private data class SectionChunk(
        val title: String,
        val forcedType: QuestionType?,
        val body: String
    )

    private fun splitBySections(text: String): List<SectionChunk> {
        val chunks = mutableListOf<SectionChunk>()
        var currentTitle = ""
        var currentType: QuestionType? = null
        val currentLines = mutableListOf<String>()
        var sawSection = false

        fun flush() {
            val body = currentLines.joinToString("\n").trim()
            if (body.isNotBlank()) {
                chunks += SectionChunk(currentTitle, currentType, body)
            }
            currentLines.clear()
        }

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) {
                currentLines += rawLine
                return@forEach
            }

            val section = SectionTitleParser.parse(line)
            if (section != null && !section.isAnswerSection) {
                flush()
                sawSection = true
                currentTitle = section.title
                currentType = section.forcedType
            } else {
                currentLines += rawLine
            }
        }
        flush()

        return if (sawSection) chunks else emptyList()
    }
}
