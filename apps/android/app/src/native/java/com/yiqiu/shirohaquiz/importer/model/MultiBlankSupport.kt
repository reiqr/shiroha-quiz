package com.yiqiu.shirohaquiz.importer.model

object MultiBlankSupport {
    private val roundBlankRegex = Regex("""(?<![A-Za-z0-9_])(?:\(\s*\)|（\s*）)""")
    private val underlineBlankRegex = Regex("""_{3,}""")

    fun hasStructuredAnswers(question: Question): Boolean {
        return question.type == QuestionType.BLANK && question.blankAnswers.isNotEmpty()
    }

    fun countExplicitBlanks(text: String): Int {
        if (text.isBlank()) return 0
        return roundBlankRegex.findAll(text).count() + underlineBlankRegex.findAll(text).count()
    }

    fun normalizeGroups(groups: List<List<String>>, preserveEmptyGroups: Boolean = true): List<List<String>> {
        return groups.mapNotNull { group ->
            val cleaned = group
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(3)
            if (cleaned.isNotEmpty() || preserveEmptyGroups) cleaned else null
        }
    }

    fun compatibilityAnswer(groups: List<List<String>>): List<String> {
        if (groups.isEmpty()) return emptyList()
        val primaryAnswers = groups.map { group -> group.firstOrNull { it.isNotBlank() }?.trim().orEmpty() }
        if (primaryAnswers.all { it.isBlank() }) return emptyList()
        return listOf(primaryAnswers.joinToString("；"))
    }

    fun withBlankAnswers(question: Question, groups: List<List<String>>): Question {
        val preservedGroups = groups.map { group -> group.take(3) }
        val cleanWarnings = clearMultiBlankWarnings(question.warnings)
        return question.copy(
            blankAnswers = preservedGroups,
            answer = compatibilityAnswer(preservedGroups),
            warnings = warningsForGroups(cleanWarnings, preservedGroups)
        )
    }

    fun clearMultiBlankWarnings(warnings: List<String>): List<String> {
        return warnings.filterNot { message ->
            (message.contains("个题空") &&
                (message.contains("未识别逐空答案") || message.contains("答案数量无法对应") || message.contains("当前配置了"))) ||
                message.contains("多空填空题存在未配置答案")
        }
    }

    fun sanitizeQuestion(question: Question): Question {
        if (question.type != QuestionType.BLANK) {
            val cleanWarnings = clearMultiBlankWarnings(question.warnings)
            return if (question.blankAnswers.isEmpty() && cleanWarnings == question.warnings) {
                question
            } else {
                question.copy(blankAnswers = emptyList(), warnings = cleanWarnings)
            }
        }
        if (question.blankAnswers.isEmpty()) return question
        val cleaned = normalizeGroups(question.blankAnswers, preserveEmptyGroups = true)
        return question.copy(
            blankAnswers = cleaned,
            answer = compatibilityAnswer(cleaned),
            warnings = warningsForGroups(clearMultiBlankWarnings(question.warnings), cleaned)
        )
    }

    private fun warningsForGroups(baseWarnings: List<String>, groups: List<List<String>>): List<String> {
        if (groups.isEmpty() || groups.all { group -> group.any { it.isNotBlank() } }) return baseWarnings
        return (baseWarnings + "多空填空题存在未配置答案的题空").distinct()
    }

    fun expectedAnswerText(groups: List<List<String>>): String {
        if (groups.isEmpty()) return "未识别答案"
        return groups.mapIndexed { index, answers ->
            val text = answers.filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "未配置" }
            "第${index + 1}空：$text"
        }.joinToString("\n")
    }

    fun userAnswerText(answers: List<String>): String {
        if (answers.isEmpty()) return "未作答"
        return answers.mapIndexed { index, answer ->
            "第${index + 1}空：${answer.ifBlank { "未作答" }}"
        }.joinToString("\n")
    }

    fun padUserAnswers(answers: List<String>, count: Int): List<String> {
        if (count <= 0) return emptyList()
        return List(count) { index -> answers.getOrNull(index).orEmpty().trim() }
    }

    fun isUserAnswerComplete(question: Question, answers: List<String>): Boolean {
        return if (hasStructuredAnswers(question)) {
            padUserAnswers(answers, question.blankAnswers.size).all { it.isNotBlank() }
        } else {
            answers.any { it.isNotBlank() }
        }
    }

    fun initialGroups(questionText: String, legacyAnswer: List<String>): List<List<String>> {
        val count = countExplicitBlanks(questionText)
        if (count <= 1) return emptyList()
        val raw = legacyAnswer.joinToString("；").trim()
        val parts = splitReliableParts(raw, count)
        return if (parts != null) parts.map { listOf(it) } else List(count) { emptyList() }
    }

    fun splitReliableParts(raw: String, expectedCount: Int): List<String>? {
        if (expectedCount <= 1 || raw.isBlank()) return null
        val trimmed = raw.trim()
        val lineParts = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lineParts.size == expectedCount) return lineParts

        val separatorCandidates = listOf(
            Regex("""[；;]"""),
            Regex("""[|｜]""")
        )
        separatorCandidates.forEach { regex ->
            val parts = trimmed.split(regex).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size == expectedCount) return parts
        }

        if (isSafeSlashSeparated(trimmed)) {
            val parts = trimmed.split('/', '／').map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size == expectedCount) return parts
        }
        return null
    }

    private fun isSafeSlashSeparated(value: String): Boolean {
        if ('/' !in value && '／' !in value) return false
        if (Regex("""https?://|www\.""", RegexOption.IGNORE_CASE).containsMatchIn(value)) return false
        if (Regex("""\b\d{1,4}/\d{1,2}(?:/\d{1,4})?\b""").containsMatchIn(value)) return false
        if (Regex("""\b[A-Za-z]{1,8}\s*/\s*[A-Za-z]{1,8}\b""").containsMatchIn(value)) return false
        return true
    }
}
