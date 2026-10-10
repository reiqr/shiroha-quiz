package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerOptionFHTest {
    @Test
    fun objectiveFVariantsRemainChoiceF() {
        val optionKeys = listOf("A", "B", "C", "D", "E", "F")
        listOf("F", "F.", "f").forEach { raw ->
            assertEquals(raw, listOf("F"), AnswerTokenParser.parseObjectiveAnswers(raw, optionKeys))
        }
    }

    @Test
    fun judgeFAndFalseVariantsRemainFalse() {
        listOf("f", "false", "False").forEach { raw ->
            assertEquals(raw, listOf("错误"), AnswerTokenParser.parseJudgeAnswer(raw))
        }
    }

    @Test
    fun fullImportKeepsChoiceFSeparateFromJudgeF() {
        val result = QuizImportParser.parseStandardText(
            """
            1. 哪一项是第六项？
            A. 第一项
            B. 第二项
            C. 第三项
            D. 第四项
            E. 第五项
            F. 第六项
            答案：f

            2. 【判断题】六大于八。
            答案：f
            """.trimIndent()
        )

        assertEquals(2, result.questions.size)
        assertEquals(QuestionType.SINGLE, result.questions[0].type)
        assertEquals(listOf("A", "B", "C", "D", "E", "F"), result.questions[0].options.map { it.key })
        assertEquals(listOf("F"), result.questions[0].answer)
        assertEquals(QuestionType.JUDGE, result.questions[1].type)
        assertEquals(listOf("错误"), result.questions[1].answer)
    }
}
