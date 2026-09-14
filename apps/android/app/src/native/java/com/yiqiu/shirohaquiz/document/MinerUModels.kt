package com.yiqiu.shirohaquiz.document

enum class MinerUMode {
    FREE,
    PRECISE
}

enum class MinerUModelVersion(val apiValue: String, val displayName: String) {
    PIPELINE("pipeline", "Pipeline"),
    VLM("vlm", "VLM（推荐）")
}

data class MinerUSettings(
    val mode: MinerUMode = MinerUMode.FREE,
    val language: String = "ch",
    val isOcr: Boolean = true,
    val enableTable: Boolean = true,
    val enableFormula: Boolean = true,
    val modelVersion: MinerUModelVersion = MinerUModelVersion.VLM,
    val pageRange: String = ""
)

data class SelectedDocument(
    val fileName: String,
    val localPath: String,
    val sizeBytes: Long,
    val pageCount: Int
)

enum class DocumentTaskStage {
    REQUESTING_UPLOAD,
    UPLOADING,
    QUEUED,
    RUNNING,
    DOWNLOADING,
    READY,
    PAUSED,
    FAILED
}

data class DocumentRecognitionTask(
    val id: String,
    val mode: MinerUMode,
    val fileName: String,
    val localPdfPath: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val pageRange: String,
    val taskId: String? = null,
    val batchId: String? = null,
    val stage: DocumentTaskStage = DocumentTaskStage.REQUESTING_UPLOAD,
    val serviceState: String = "",
    val extractedPages: Int = 0,
    val totalPages: Int = 0,
    val rawResultPath: String? = null,
    val resultPath: String? = null,
    val resultSourceUrl: String? = null,
    val hasImageReferences: Boolean = false,
    val message: String = "正在准备识别任务……",
    val errorMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DocumentImportDraft(
    val id: String,
    val sourceFileName: String,
    val text: String,
    val hasImageReferences: Boolean
)

data class MinerUUploadTicket(
    val taskId: String? = null,
    val batchId: String? = null,
    val uploadUrl: String
)

data class MinerUTaskStatus(
    val state: String,
    val resultUrl: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val extractedPages: Int = 0,
    val totalPages: Int = 0
)

val DocumentRecognitionTask.isWorking: Boolean
    get() = stage in setOf(
        DocumentTaskStage.REQUESTING_UPLOAD,
        DocumentTaskStage.UPLOADING,
        DocumentTaskStage.QUEUED,
        DocumentTaskStage.RUNNING,
        DocumentTaskStage.DOWNLOADING
    )
