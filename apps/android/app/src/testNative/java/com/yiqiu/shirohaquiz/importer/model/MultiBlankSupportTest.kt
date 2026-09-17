package com.yiqiu.shirohaquiz.importer.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MultiBlankSupportTest {
    @Test
    fun normalizeGroupsKeepsAllDistinctAlternativeAnswers() {
        val groups = listOf(listOf("主答案", "备选1", "备选2", "备选3", "备选4", "备选4"))

        assertEquals(
            listOf(listOf("主答案", "备选1", "备选2", "备选3", "备选4")),
            MultiBlankSupport.normalizeGroups(groups)
        )
    }

    @Test
    fun editingStructuredAnswersDoesNotTruncateAlternatives() {
        val question = Question(
            type = QuestionType.BLANK,
            question = "填写答案（ ）",
            blankAnswers = listOf(listOf("主答案"))
        )
        val alternatives = listOf("主答案", "备选1", "备选2", "备选3", "备选4")

        val updated = MultiBlankSupport.withBlankAnswers(question, listOf(alternatives))

        assertEquals(alternatives, updated.blankAnswers.single())
        assertEquals(listOf("主答案"), updated.answer)
    }
}
