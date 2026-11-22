package com.yiqiu.shirohaquiz.ui.screens

import com.yiqiu.shirohaquiz.document.DocumentImportDraft
import com.yiqiu.shirohaquiz.document.DocumentImportTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportSourceDraftTest {
    private val questions = DocumentImportDraft("q", "questions.pdf", "Question text", false,
        imageAssignments = mapOf("image" to 1))
    private val answers = DocumentImportDraft("a", "answers.pdf", "Answer text", false,
        target = DocumentImportTarget.ANSWERS)

    @Test
    fun separateOcrResultsCanArriveInEitherOrder() {
        for (order in listOf(listOf(questions, answers), listOf(answers, questions))) {
            val draft = ImportSourceDraft()
            order.forEach(draft::accept)
            assertEquals("Question text", draft.rawText.value)
            assertEquals("Answer text", draft.answerText.value)
            assertEquals("questions.pdf", draft.selectedFileName.value)
            assertEquals("answers.pdf", draft.selectedAnswerFileName.value)
            assertEquals(mapOf("image" to 1), draft.importedImageAssignments.value)
            assertTrue(draft.useDualImport.value)
            assertTrue(draft.ocrImportActive.value)
            draft.reset()
            assertTrue(draft.rawText.value.isEmpty() && draft.answerText.value.isEmpty())
            assertFalse(draft.useDualImport.value)
        }
    }

    @Test
    fun completeQuestionBankStillUsesSingleFileModeByDefault() {
        val draft = ImportSourceDraft()
        draft.accept(questions)
        assertFalse(draft.useDualImport.value)
        assertEquals("Question text", draft.rawText.value)
    }
}
