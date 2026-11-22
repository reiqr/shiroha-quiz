package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.runtime.mutableStateOf
import com.yiqiu.shirohaquiz.document.DocumentImportDraft
import com.yiqiu.shirohaquiz.document.DocumentImportTarget
import com.yiqiu.shirohaquiz.importer.assets.QuestionImportAssetExtractor

internal class ImportSourceDraft {
    val rawText = mutableStateOf("")
    val answerText = mutableStateOf("")
    val selectedFileName = mutableStateOf("未选择文件")
    val selectedAnswerFileName = mutableStateOf("未选择答案文件")
    val useDualImport = mutableStateOf(false)
    val importedImages = mutableStateOf<List<QuestionImportAssetExtractor.ExtractedImportImage>>(emptyList())
    val importedImageAssignments = mutableStateOf<Map<String, Int>>(emptyMap())
    val ocrImportActive = mutableStateOf(false)

    fun accept(draft: DocumentImportDraft) {
        when (draft.target) {
            DocumentImportTarget.QUESTIONS -> {
                rawText.value = draft.text
                selectedFileName.value = draft.sourceFileName
                importedImages.value = draft.images
                importedImageAssignments.value = draft.imageAssignments
            }
            DocumentImportTarget.ANSWERS -> {
                answerText.value = draft.text
                selectedAnswerFileName.value = draft.sourceFileName
                useDualImport.value = true
            }
        }
        ocrImportActive.value = true
    }

    fun reset() {
        rawText.value = ""
        answerText.value = ""
        selectedFileName.value = "未选择文件"
        selectedAnswerFileName.value = "未选择答案文件"
        useDualImport.value = false
        importedImages.value = emptyList()
        importedImageAssignments.value = emptyMap()
        ocrImportActive.value = false
    }
}
