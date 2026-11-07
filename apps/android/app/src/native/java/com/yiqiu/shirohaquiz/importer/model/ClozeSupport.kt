package com.yiqiu.shirohaquiz.importer.model

/** 按空位顺序保存答案，包括未作答位置；不能使用 filterNot(isBlank) 压缩位置。 */
object ClozeSupport {
    fun padAnswers(values: List<String>, count: Int): List<String> =
        List(count.coerceAtLeast(0)) { index -> values.getOrNull(index).orEmpty().trim().uppercase() }

    fun correctCount(question: Question, userAnswers: List<String>): Int {
        val values = padAnswers(userAnswers, question.clozeItems.size)
        return question.clozeItems.withIndex().count { (index, item) ->
            item.answer.isNotBlank() && values.getOrNull(index) == item.answer.trim().uppercase()
        }
    }

    fun allAnswered(question: Question, userAnswers: List<String>): Boolean {
        if (question.clozeItems.isEmpty()) return false
        return padAnswers(userAnswers, question.clozeItems.size).all { it.isNotBlank() }
    }

    fun hasCompleteReference(question: Question): Boolean = question.clozeItems.isNotEmpty() &&
        question.clozeItems.all { item ->
            item.answer.isNotBlank() &&
                item.options.map { it.key.uppercase() }.containsAll(listOf("A", "B", "C", "D")) &&
                item.options.all { it.text.isNotBlank() } &&
                item.options.any { it.key.equals(item.answer, ignoreCase = true) }
        }

    fun answerSummary(question: Question, userAnswers: List<String> = emptyList()): String {
        val values = padAnswers(userAnswers, question.clozeItems.size)
        return question.clozeItems.mapIndexed { index, item ->
            val expected = item.answer.ifBlank { "未识别" }
            val user = values[index]
            if (user.isBlank()) "第${item.number}空：$expected"
            else {
                val verdict = if (item.answer.isBlank()) "待核对" else if (user == expected) "正确" else "错误"
                "第${item.number}空：$expected（作答：$user，$verdict）"
            }
        }.joinToString("\n")
    }
}
