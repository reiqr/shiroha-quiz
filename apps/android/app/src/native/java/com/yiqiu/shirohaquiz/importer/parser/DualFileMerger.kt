package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.ImportWarning
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.importer.model.WarningLevel
import com.yiqiu.shirohaquiz.importer.score.ImportStrategyScorer
import com.yiqiu.shirohaquiz.importer.validate.ImportValidator

data class MergeResult(
    val questions: List<Question>,
    val warnings: List<ImportWarning>,
    val name: String = "按题号合并"
)

object DualFileMerger {
    fun mergeAuto(
        questions: List<Question>,
        answers: List<ParsedAnswerEntry>
    ): MergeResult {
        val numberCandidates = listOf(
            mergeByNumber(questions, answers),
            mergeByTypeAndNumber(questions, answers),
            mergeByChapterAndNumber(questions, answers)
        )
        val sequenceCandidate = mergeBySequence(questions, answers)
        val candidates = if (hasQuestionAnswerNumberOverlap(questions, answers)) {
            // When both sides carry explicit matching numbers, keep number-based merges authoritative.
            // Sequence merge is only a fallback for truly unnumbered or non-overlapping answer sources.
            numberCandidates
        } else {
            numberCandidates + sequenceCandidate
        }
        return candidates.maxByOrNull { candidate ->
            val validation = ImportValidator.validate(candidate.questions)
            ImportStrategyScorer.score(candidate.questions, validation + candidate.warnings)
        } ?: mergeByNumber(questions, answers)
    }

    private fun hasQuestionAnswerNumberOverlap(
        questions: List<Question>,
        answers: List<ParsedAnswerEntry>
    ): Boolean {
        val questionNumbers = questions
            .mapNotNull { normalizeMergeNumber(it.number) }
            .toSet()
        if (questionNumbers.isEmpty()) return false
        return answers.any { entry ->
            normalizeMergeNumber(entry.number)?.let { it in questionNumbers } == true
        }
    }

    private fun normalizeMergeNumber(raw: String): String? {
        val value = raw.trim().substringBefore('-').trimStart('0')
        return value.ifBlank { "0" }.takeIf { normalized ->
            normalized.all { char -> char.isDigit() } && normalized != "0"
        }
    }

    fun mergeByNumber(
        questions: List<Question>,
        answers: List<ParsedAnswerEntry>
    ): MergeResult {
        val answerMap = answers.groupBy { it.number }.mapValues { it.value.first() }
        val used = mutableSetOf<ParsedAnswerEntry>()
        val warnings = mutableListOf<ImportWarning>()

        val mergedQuestions = questions.map { question ->
            val matched = answerMap[question.number]
            if (matched == null) {
                warnings += ImportWarning(WarningLevel.WARNING, question.number, "未匹配到对应答案")
                question
            } else {
                used += matched
                applyAnswer(question, matched, warnings)
            }
        }

        answers.filterNot { it in used }.forEach { entry ->
            warnings += ImportWarning(WarningLevel.WARNING, entry.number, "答案文件中存在未匹配题号")
        }

        return MergeResult(mergedQuestions, warnings, "按全局题号合并")
    }

    fun mergeByTypeAndNumber(
        questions: List<Question>,
        answers: List<ParsedAnswerEntry>
    ): MergeResult {
        val typedAnswers = answers.filter { it.type != null }
        if (typedAnswers.isEmpty()) return mergeByNumber(questions, answers).copy(name = "按题型分组题号合并")

        val answerMap = typedAnswers.groupBy { (it.type ?: QuestionType.SINGLE) to it.number }.mapValues { it.value.first() }
        val used = mutableSetOf<ParsedAnswerEntry>()
        val warnings = mutableListOf<ImportWarning>()

        val mergedQuestions = questions.map { question ->
            val matched = answerMap[question.type to question.number]
            if (matched == null) {
                warnings += ImportWarning(WarningLevel.WARNING, question.number, "未在对应题型分组中匹配到答案")
                question
            } else {
                used += matched
                applyAnswer(question, matched, warnings)
            }
        }

        typedAnswers.filterNot { it in used }.forEach { entry ->
            warnings += ImportWarning(WarningLevel.WARNING, entry.number, "分组答案中存在未匹配题号")
        }
        return MergeResult(mergedQuestions, warnings, "按题型分组题号合并")
    }


    fun mergeByChapterAndNumber(
        questions: List<Question>,
        answers: List<ParsedAnswerEntry>
    ): MergeResult {
        val answerGroups = answers
            .filter { it.category.isNotBlank() }
            .groupByPreservingOrder { ChapterScopeParser.normalizeChapter(it.category) }
            .filterKeys { it.isNotBlank() }
        if (answerGroups.size < 2) {
            return mergeByNumber(questions, answers).copy(name = "按章节顺序题号合并")
        }

        val questionGroups = splitQuestionsByNumberRestart(questions)
        if (questionGroups.size < 2) {
            return mergeByNumber(questions, answers).copy(name = "按章节顺序题号合并")
        }

        data class Key(val type: QuestionType?, val number: String)

        val used = mutableSetOf<ParsedAnswerEntry>()
        val warnings = mutableListOf<ImportWarning>()
        var matchedCount = 0
        val groupPairs = questionGroups.zip(answerGroups.values.toList())
        val mergedQuestions = mutableListOf<Question>()

        groupPairs.forEach { (questionGroup, answerGroup) ->
            val answerMap = answerGroup.groupBy { Key(it.type, it.number) }.mapValues { it.value.first() }
            val fallbackAnswerMap = answerGroup.groupBy { Key(null, it.number) }.mapValues { it.value.first() }
            questionGroup.forEach { question ->
                val matched = answerMap[Key(question.type, question.number)]
                    ?: fallbackAnswerMap[Key(null, question.number)]
                if (matched == null) {
                    warnings += ImportWarning(WarningLevel.WARNING, question.number, "未在对应章节中匹配到答案")
                    mergedQuestions += question
                } else {
                    matchedCount += 1
                    used += matched
                    mergedQuestions += applyAnswer(question, matched, warnings)
                }
            }
        }

        if (questionGroups.size > groupPairs.size) {
            questionGroups.drop(groupPairs.size).flatten().forEach { question ->
                warnings += ImportWarning(WarningLevel.WARNING, question.number, "题目章节多于答案章节，未匹配到对应答案")
                mergedQuestions += question
            }
        }
        if (answerGroups.size > groupPairs.size) {
            warnings += ImportWarning(WarningLevel.WARNING, null, "答案章节多于题目章节，已忽略多余章节答案")
        }
        answers.filterNot { it in used }.filter { it.category.isNotBlank() }.forEach { entry ->
            warnings += ImportWarning(WarningLevel.WARNING, entry.number, "章节答案中存在未匹配题号")
        }
        if (matchedCount < 5) {
            warnings += ImportWarning(WarningLevel.WARNING, null, "章节答案匹配数量不足，建议改用标准答案表或检查章节标题")
        }
        return MergeResult(mergedQuestions, warnings, "按章节顺序题号合并")
    }

    private fun splitQuestionsByNumberRestart(questions: List<Question>): List<List<Question>> {
        if (questions.isEmpty()) return emptyList()
        val groups = mutableListOf<MutableList<Question>>()
        var current = mutableListOf<Question>()
        questions.forEach { question ->
            val number = question.number.trim().toIntOrNull()
            if (current.isNotEmpty() && number == 1) {
                groups += current
                current = mutableListOf()
            }
            current += question
        }
        if (current.isNotEmpty()) groups += current
        return groups
    }

    private fun <T, K> Iterable<T>.groupByPreservingOrder(keySelector: (T) -> K): LinkedHashMap<K, List<T>> {
        val result = linkedMapOf<K, MutableList<T>>()
        for (item in this) {
            val key = keySelector(item)
            result.getOrPut(key) { mutableListOf() }.add(item)
        }
        return LinkedHashMap<K, List<T>>().apply {
            result.forEach { (key, value) -> put(key, value) }
        }
    }

    fun mergeBySequence(
        questions: List<Question>,
        answers: List<ParsedAnswerEntry>
    ): MergeResult {
        val warnings = mutableListOf<ImportWarning>()
        val mergedQuestions = questions.mapIndexed { index, question ->
            val matched = answers.getOrNull(index)
            if (matched == null) {
                warnings += ImportWarning(WarningLevel.WARNING, question.number, "按顺序未匹配到答案")
                question
            } else {
                applyAnswer(question, matched, warnings)
            }
        }
        if (answers.size > questions.size) {
            warnings += ImportWarning(WarningLevel.WARNING, null, "答案数量多于题目数量，已忽略多余答案")
        }
        return MergeResult(mergedQuestions, warnings, "按顺序合并")
    }

    private fun applyAnswer(
        question: Question,
        entry: ParsedAnswerEntry,
        warnings: MutableList<ImportWarning>
    ): Question {
        if (question.type == QuestionType.BLANK) {
            val blankCount = question.blankAnswers.size.takeIf { it > 1 }
                ?: MultiBlankSupport.countExplicitBlanks(question.question).takeIf { it > 1 }
            if (blankCount != null && question.answer.isEmpty()) {
                val rawAnswer = entry.answer.joinToString("；")
                val parts = MultiBlankSupport.splitReliableParts(rawAnswer, blankCount)
                if (parts != null) {
                    val groups = parts.map { listOf(it) }
                    return MultiBlankSupport.withBlankAnswers(question, groups).copy(
                        analysis = if (question.analysis.isNotBlank()) question.analysis else entry.analysis
                    )
                }
                warnings += ImportWarning(
                    WarningLevel.WARNING,
                    question.number,
                    "检测到${blankCount}个题空，答案数量无法对应，请人工核对"
                )
            }
        }

        val mergedAnswer = if (question.answer.isNotEmpty()) question.answer else normalizeAnswerForQuestion(question, entry.answer)
        val mergedType = normalizeTypeAfterMerge(question, mergedAnswer)
        if (mergedType != question.type) {
            warnings += ImportWarning(WarningLevel.WARNING, question.number, "题型根据多答案由单选修正为多选")
        }
        return question.copy(
            type = mergedType,
            answer = mergedAnswer,
            analysis = if (question.analysis.isNotBlank()) question.analysis else entry.analysis
        )
    }

    private fun normalizeTypeAfterMerge(question: Question, answer: List<String>): QuestionType {
        return if (question.type == QuestionType.SINGLE && answer.size > 1) QuestionType.MULTIPLE else question.type
    }

    private fun normalizeAnswerForQuestion(question: Question, answer: List<String>): List<String> {
        if (answer.isEmpty()) return emptyList()
        if (question.type == QuestionType.JUDGE) {
            return AnswerTokenParser.parseJudgeAnswer(answer.joinToString(",")).ifEmpty { answer }
        }
        if (question.type == QuestionType.SINGLE || question.type == QuestionType.MULTIPLE) {
            val optionKeys = question.options.map { it.key.uppercase() }
            val normalized = AnswerTokenParser.parseObjectiveAnswers(answer.joinToString(","), optionKeys)
            if (normalized.isNotEmpty()) return normalized
        }
        return answer
    }
}
