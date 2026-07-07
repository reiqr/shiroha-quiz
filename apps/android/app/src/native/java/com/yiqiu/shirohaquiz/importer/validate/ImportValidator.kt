package com.yiqiu.shirohaquiz.importer.validate

import com.yiqiu.shirohaquiz.importer.assets.QuestionImageMarker
import com.yiqiu.shirohaquiz.importer.model.ImportWarning
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.importer.model.WarningLevel

object ImportValidator {
    fun validate(questions: List<Question>): List<ImportWarning> {
        val warnings = mutableListOf<ImportWarning>()

        questions.forEach { question ->
            question.warnings.forEach { message ->
                warnings += ImportWarning(WarningLevel.WARNING, question.number, message)
            }
            if (question.question.isBlank()) {
                warnings += ImportWarning(WarningLevel.ERROR, question.number, "题干为空")
            }

            when (question.type) {
                QuestionType.SINGLE -> {
                    if (question.options.size < 2 && !isImageChoiceQuestion(question)) warnings += ImportWarning(WarningLevel.ERROR, question.number, "单选题缺少足够选项")
                    if (question.answer.isEmpty()) warnings += ImportWarning(WarningLevel.WARNING, question.number, "单选题未识别到答案")
                    if (question.answer.size > 1) warnings += ImportWarning(WarningLevel.ERROR, question.number, "单选题出现多个答案")
                    validateChoiceAnswer(question, warnings)
                }

                QuestionType.MULTIPLE -> {
                    if (question.options.size < 2 && !isImageChoiceQuestion(question)) warnings += ImportWarning(WarningLevel.ERROR, question.number, "多选题缺少足够选项")
                    if (question.answer.isEmpty()) warnings += ImportWarning(WarningLevel.WARNING, question.number, "多选题未识别到答案")
                    validateChoiceAnswer(question, warnings)
                }

                QuestionType.JUDGE -> {
                    if (question.options.size < 2) warnings += ImportWarning(WarningLevel.WARNING, question.number, "判断题缺少对/错选项，已尝试自动补全")
                    if (question.answer.isEmpty()) warnings += ImportWarning(WarningLevel.WARNING, question.number, "判断题未识别到答案")
                    if (question.answer.any { !isValidJudgeAnswer(it) }) warnings += ImportWarning(WarningLevel.WARNING, question.number, "判断题答案不是标准对/错标记")
                }

                QuestionType.BLANK -> {
                    if (MultiBlankSupport.hasStructuredAnswers(question)) {
                        val detectedCount = MultiBlankSupport.countExplicitBlanks(question.question)
                        if (detectedCount > 0 && detectedCount != question.blankAnswers.size) {
                            warnings += ImportWarning(
                                WarningLevel.WARNING,
                                question.number,
                                "题干检测到${detectedCount}个题空，当前配置了${question.blankAnswers.size}组答案，请人工核对"
                            )
                        }
                        if (question.blankAnswers.any { group -> group.none { it.isNotBlank() } }) {
                            warnings += ImportWarning(WarningLevel.WARNING, question.number, "多空填空题存在未配置答案的题空")
                        }
                    } else if (question.answer.isEmpty()) {
                        warnings += ImportWarning(WarningLevel.WARNING, question.number, "主观题未识别到参考答案")
                    }
                }

                QuestionType.SHORT -> {
                    if (question.answer.isEmpty()) warnings += ImportWarning(WarningLevel.WARNING, question.number, "主观题未识别到参考答案")
                }
            }
        }

        return warnings
    }

    private fun isImageChoiceQuestion(question: Question): Boolean {
        if (question.images.isNotEmpty()) return true
        if (QuestionImageMarker.contains(question.question)) return true
        if (QuestionImageMarker.contains(question.analysis)) return true
        return question.options.any { QuestionImageMarker.contains(it.text) }
    }

    private fun validateChoiceAnswer(question: Question, warnings: MutableList<ImportWarning>) {
        if (question.answer.isEmpty() || question.options.isEmpty()) return
        val keys = question.options.map { it.key }.toSet()
        val invalid = question.answer.filterNot { it in keys }
        if (invalid.isNotEmpty()) {
            warnings += ImportWarning(WarningLevel.WARNING, question.number, "答案选项不在当前题目选项范围内")
        }
    }

    private fun isValidJudgeAnswer(answer: String): Boolean {
        return when (answer.trim().lowercase()) {
            "a", "b", "正确", "错误", "对", "错", "是", "否",
            "√", "✓", "✔", "☑", "×", "✗", "✖", "true", "false", "t", "f" -> true
            else -> false
        }
    }
}
