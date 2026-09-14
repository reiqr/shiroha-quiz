package com.yiqiu.shirohaquiz.document

import com.yiqiu.shirohaquiz.importer.assets.QuestionImageBinder
import com.yiqiu.shirohaquiz.importer.assets.QuestionImageMarker
import com.yiqiu.shirohaquiz.importer.assets.QuestionImportAssetExtractor
import com.yiqiu.shirohaquiz.importer.model.*
import org.junit.Assert.*
import org.junit.Test

class DocumentImageBindingTest {
    @Test fun canonicalMinerUMarkerUsesExistingBinder() {
        val marker = QuestionImageMarker.canonical("img_0001")
        val image = image("image1", marker)
        val parsed = result(listOf(Question(type = QuestionType.SINGLE, question = "看图作答。\n$marker", answer = listOf("A"))))
        val bound = QuestionImageBinder.attach(parsed, listOf(image))
        assertEquals(listOf("image1"), bound.questions.single().images.map { it.id })
        assertFalse(bound.questions.single().question.contains("SHIROHA_IMAGE"))
        assertEquals(listOf("A"), bound.questions.single().answer)
    }

    @Test fun manualAssignmentMovesOnlyImageAndKeepsQuestionContent() {
        val marker = QuestionImageMarker.canonical("img_0001")
        val image = image("image1", marker)
        val questions = listOf(Question(type = QuestionType.SINGLE, question = "第一题$marker", answer = listOf("A")),
            Question(type = QuestionType.SHORT, question = "第二题", answer = listOf("检查设备、核对参数")))
        val automaticallyBound = QuestionImageBinder.attach(result(questions), listOf(image))
        val manuallyBound = DocumentImageBinding.applyAssignments(automaticallyBound, listOf(image), mapOf("image1" to 1))
        assertTrue(manuallyBound.questions[0].images.isEmpty())
        assertEquals("image1", manuallyBound.questions[1].images.single().id)
        assertEquals(automaticallyBound.questions.map { it.question }, manuallyBound.questions.map { it.question })
        assertEquals(questions.map { it.answer }, manuallyBound.questions.map { it.answer })
        assertEquals(questions.map { it.type }, manuallyBound.questions.map { it.type })
    }

    @Test fun outOfRangeAssignmentReportsError() {
        val asset = image("image1", QuestionImageMarker.canonical("img_0001"))
        val output = DocumentImageBinding.applyAssignments(result(listOf(Question(type = QuestionType.SHORT, question = "题目"))),
            listOf(asset), mapOf("image1" to 2))
        assertTrue(output.warnings.any { it.level == WarningLevel.ERROR })
    }

    private fun result(questions: List<Question>) = ImportResult(questions, "standard", emptyList(), ImportDiagnostics())
    private fun image(id: String, marker: String) = QuestionImportAssetExtractor.ExtractedImportImage(marker,
        QuestionImage(id = id, localPath = "image/$id.png", order = 1))
}
