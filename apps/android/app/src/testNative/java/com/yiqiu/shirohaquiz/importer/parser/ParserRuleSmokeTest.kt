package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserRuleSmokeTest {
    @Test
    fun answerSectionShortTitlesAreRecognizedBeforeInlineAnswerLines() {
        listOf("答案解析", "答案与解析", "参考答案", "标准答案", "正确答案", "解析区").forEach { title ->
            assertTrue("$title should be an answer section heading", SectionTitleParser.isAnswerSectionHeading(title))
        }

        assertFalse("答案：A is answer content, not a section heading", SectionTitleParser.isAnswerSectionHeading("答案：A"))
        assertFalse("参考答案：检查设备 is answer content, not a section heading", SectionTitleParser.isAnswerSectionHeading("参考答案：检查设备"))
    }

    @Test
    fun chineseQuestionIndexNormalizationIsSharedAndStable() {
        val cases = mapOf(
            "一" to "1",
            "十" to "10",
            "十一" to "11",
            "二十一" to "21",
            "一百" to "100",
            "一百三" to "103",
            "103" to "103"
        )

        cases.forEach { (raw, expected) ->
            assertEquals("normalizeQuestionIndex($raw)", expected, normalizeQuestionIndex(raw))
        }
    }

    @Test
    fun answerParserRuleOrderIsExplicitAndStable() {
        assertEquals(
            listOf(
                "table_answer",
                "compact_answer_line",
                "labeled_answer",
                "expression_answer",
                "subjective_answer",
                "section_heading",
                "range_answer",
                "multiple_bracket_answer",
                "bracket_answer",
                "inline_multi_answer",
                "answer_analysis",
                "inline_single_answer",
                "simple_tail_answer"
            ),
            AnswerParser.ruleNamesForTest()
        )
    }

    @Test
    fun dualFileMergeNormalizesSingleQuestionWithMultipleAnswers() {
        val singleQuestion = Question(
            number = "1",
            type = QuestionType.SINGLE,
            question = "下列说法正确的是",
            options = listOf(
                Option("A", "选项一"),
                Option("B", "选项二")
            )
        )
        val multipleAnswer = ParsedAnswerEntry(number = "1", answer = listOf("A", "B"))

        val result = DualFileMerger.mergeByNumber(listOf(singleQuestion), listOf(multipleAnswer))
        assertEquals(QuestionType.MULTIPLE, result.questions.first().type)
        assertEquals(listOf("A", "B"), result.questions.first().answer)
        assertTrue(result.warnings.any { it.message.contains("题型根据多答案由单选修正为多选") })

        val singleAnswerResult = DualFileMerger.mergeByNumber(
            listOf(singleQuestion),
            listOf(ParsedAnswerEntry(number = "1", answer = listOf("A")))
        )
        assertEquals(QuestionType.SINGLE, singleAnswerResult.questions.first().type)
    }

    @Test
    fun binaryCorrectWrongOptionsDoNotOverrideChoicePrompt() {
        val choicePrompt = StandardQuestionParser.parse(
            """
            1. 下列说法正确的是
            A. 正确
            B. 错误
            答案：A
            """.trimIndent()
        ).single()
        assertEquals(QuestionType.SINGLE, choicePrompt.type)

        val judgePrompt = StandardQuestionParser.parse(
            """
            1. 该说法是否正确
            A. 正确
            B. 错误
            答案：A
            """.trimIndent()
        ).single()
        assertEquals(QuestionType.JUDGE, judgePrompt.type)
    }
}
