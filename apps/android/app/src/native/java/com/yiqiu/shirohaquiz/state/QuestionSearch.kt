package com.yiqiu.shirohaquiz.state

import com.yiqiu.shirohaquiz.importer.model.Question
import java.util.Locale

sealed class QuestionSearchScope {
    object ActiveBank : QuestionSearchScope()
    object AllBanks : QuestionSearchScope()
    data class Bank(val bankId: String) : QuestionSearchScope()
}

enum class QuestionSearchMatchedField(val label: String) {
    Question("题干"),
    Options("选项"),
    Answer("答案"),
    Analysis("解析")
}

data class QuestionSearchResult(
    val bankId: String,
    val bankName: String,
    val groupName: String,
    val questionIndex: Int,
    val question: Question,
    val matchedFields: List<QuestionSearchMatchedField>
) {
    val key: String = "$bankId:${question.id}:$questionIndex"
}

object QuestionSearchEngine {
    fun search(
        banks: List<QuizBank>,
        activeBankId: String?,
        query: String,
        scope: QuestionSearchScope
    ): List<QuestionSearchResult> {
        val tokens = query
            .split(Regex("\\s+"))
            .map { normalizeForSearch(it) }
            .filter { it.isNotBlank() }
            .distinct()

        if (tokens.isEmpty()) return emptyList()

        val targetBanks = when (scope) {
            QuestionSearchScope.ActiveBank -> banks.filter { it.id == activeBankId }
            QuestionSearchScope.AllBanks -> banks
            is QuestionSearchScope.Bank -> banks.filter { it.id == scope.bankId }
        }

        return targetBanks.flatMap { bank ->
            bank.questions.mapIndexedNotNull { index, question ->
                val fieldTexts = mapOf(
                    QuestionSearchMatchedField.Question to question.question,
                    QuestionSearchMatchedField.Options to question.options.joinToString(" ") { "${it.key} ${it.text}" },
                    QuestionSearchMatchedField.Answer to question.answer.joinToString(" "),
                    QuestionSearchMatchedField.Analysis to question.analysis
                )
                val normalizedFields = fieldTexts.mapValues { (_, value) -> normalizeForSearch(value) }
                val combinedText = normalizedFields.values.joinToString(" ")
                if (!tokens.all { token -> combinedText.contains(token) }) return@mapIndexedNotNull null

                val matchedFields = normalizedFields
                    .filterValues { fieldText -> tokens.any { token -> fieldText.contains(token) } }
                    .keys
                    .toList()

                QuestionSearchResult(
                    bankId = bank.id,
                    bankName = bank.name,
                    groupName = bank.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME },
                    questionIndex = index + 1,
                    question = question,
                    matchedFields = matchedFields
                )
            }
        }
    }

    private fun normalizeForSearch(value: String): String {
        if (value.isBlank()) return ""
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace('，', ',')
            .replace('。', '.')
            .replace('；', ';')
            .replace('：', ':')
            .replace('（', '(')
            .replace('）', ')')
            .replace('！', '!')
            .replace('？', '?')
            .replace(Regex("\\s+"), " ")
    }
}
