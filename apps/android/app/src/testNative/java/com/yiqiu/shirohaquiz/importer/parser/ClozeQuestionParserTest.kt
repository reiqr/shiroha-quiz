package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.ClozeSupport
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClozeQuestionParserTest {
    private fun paper(count: Int, answerText: String = ""): String {
        val article = (1..count).joinToString(" ") { "In the story ($it) _____ something happened." }
        val options = (1..count).joinToString("\n") { "$it. A. red B. blue C. green D. white" }
        return "二、完形填空\n$article\n$options\n$answerText"
    }

    @Test fun recognizesMultipleIndependentAnswers() {
        val question = QuizImportParser.parseStandardText(paper(5, "答案：1.A 2.B 3.C 4.D 5.A")).questions.single()
        assertEquals(QuestionType.CLOZE, question.type)
        assertEquals(5, question.clozeItems.size)
        assertEquals(listOf("A", "B", "C", "D", "A"), question.answer)
        assertTrue(question.clozeItems.all { it.options.map { option -> option.key } == listOf("A", "B", "C", "D") })
    }

    @Test fun acceptsCompactAndSeparateAnswerKeys() {
        assertEquals(listOf("B", "C", "A"), QuizImportParser.parseStandardText(paper(3, "答案：BCA")).questions.single().answer)
        assertEquals(listOf("B", "A", "D"), QuizImportParser.parseDualText(paper(3), "1.B 2.A 3.D").questions.single().answer)
    }

    @Test fun missingAnswerDoesNotShiftLaterBlanks() {
        val question = QuizImportParser.parseStandardText(paper(4, "答案：1.A 2.B 4.D")).questions.single()
        assertEquals(listOf("A", "B", "", "D"), question.answer)
        assertFalse(ClozeSupport.hasCompleteReference(question))
        assertTrue(question.warnings.isNotEmpty())
    }

    @Test fun preservesMissingOptionGroupForManualReview() {
        val original = paper(4, "答案：1.A 2.B 3.C 4.D")
        val withoutThird = original.replace("3. A. red B. blue C. green D. white\n", "")
        val result = QuizImportParser.parseStandardText(withoutThird)
        val question = result.questions.single()
        assertEquals(4, question.clozeItems.size)
        assertEquals(3, question.clozeItems[2].number)
        assertTrue(result.warnings.isNotEmpty())
        assertFalse(ClozeSupport.hasCompleteReference(question))
    }

    @Test fun preservesPositionalAnswersAndPartialScore() {
        val question = QuizImportParser.parseStandardText(paper(5, "答案：1.A 2.B 3.C 4.D 5.A")).questions.single()
        assertEquals(listOf("A", "", "C", "", ""), ClozeSupport.padAnswers(listOf("A", "", "C"), 5))
        assertEquals(3, ClozeSupport.correctCount(question, listOf("A", "B", "A", "D", "B")))
        assertTrue(ClozeSupport.allAnswered(question, listOf("A", "B", "C", "D", "A")))
    }

    @Test fun doesNotChangeOrdinarySingleChoiceOrReading() {
        val ordinary = "1. Which is right?\nA. red\nB. green\n答案：A"
        assertTrue(QuizImportParser.parseStandardText(ordinary).questions.none { it.type == QuestionType.CLOZE })
        val reading = "阅读理解\nAn ordinary paragraph.\n1. A. red B. green C. blue D. white\n2. A. red B. green C. blue D. white"
        assertTrue(QuizImportParser.parseStandardText(reading).questions.none { it.type == QuestionType.CLOZE })
    }
}
