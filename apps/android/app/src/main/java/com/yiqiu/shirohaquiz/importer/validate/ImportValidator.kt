package com.yiqiu.shirohaquiz.importer.validate

import com.yiqiu.shirohaquiz.importer.model.ImportIssue
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType

object ImportValidator {
    fun validate(questions: List<Question>): List<ImportIssue> {
        val issues = mutableListOf<ImportIssue>()
        questions.forEach { q ->
            when {
                q.question.isBlank() ->
                    issues += ImportIssue(q.number, "缺少题干", true)

                q.type in listOf(QuestionType.SINGLE, QuestionType.MULTIPLE) && q.options.isEmpty() ->
                    issues += ImportIssue(q.number, "选择题缺少选项", true)

                q.answer.isEmpty() ->
                    issues += ImportIssue(q.number, "缺少答案", q.type != QuestionType.BLANK && q.type != QuestionType.SHORT)

                q.type == QuestionType.SINGLE && q.answer.size > 1 ->
                    issues += ImportIssue(q.number, "单选题出现多个答案", true)

                q.type == QuestionType.MULTIPLE && q.answer.size == 1 ->
                    issues += ImportIssue(q.number, "多选题只有一个答案，请确认", false)
            }
        }
        return issues
    }
}
