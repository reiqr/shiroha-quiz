package com.yiqiu.shirohaquiz.importer.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class FollowingAnswerAnalysisTest {
    private val questions = """
        1. 第一题？
        A. 甲
        B. 乙

        2. 第二题？
        A. 丙
        B. 丁
    """.trimIndent()

    @Test
    fun dualImportKeepsAnalysisOnSeparateLines() {
        val answers = """
            1. 【答案】A

            【解析】第一题第一段。
            这是第一题的续行。

            2【答案】B
            解析：
            第二题的解析。
        """.trimIndent()
        val imported = QuizImportParser.parseDualText(questions, answers).questions
        assertEquals(listOf(listOf("A"), listOf("B")), imported.map { it.answer })
        assertEquals(listOf("第一题第一段。\n这是第一题的续行。", "第二题的解析。"), imported.map { it.analysis })
    }

    @Test
    fun keepsNumberedAnalysisSectionSeparateAndPreservesInlineAnalysis() {
        val entries = AnswerParser.parse("""
            1. A
            2. B
            解析
            1. 第一题解析。
            2. 第二题解析。
        """.trimIndent())
        assertEquals(listOf("第一题解析。", "第二题解析。"), entries.map { it.analysis })
        assertEquals("原有解析。", AnswerParser.parse("1. 答案：A 解析：原有解析。").single().analysis)
    }

    @Test
    fun standardQuestionStillAcceptsSeparateAnalysisLine() {
        val result = QuizImportParser.parseStandardText("""
            1. 第一题？
            A. 甲
            B. 乙
            答案：A

            解析：第一题解析。
        """.trimIndent()).questions.single()
        assertEquals(listOf("A"), result.answer)
        assertEquals("第一题解析。", result.analysis)
    }
}
