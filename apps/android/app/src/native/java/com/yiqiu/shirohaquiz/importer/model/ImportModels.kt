package com.yiqiu.shirohaquiz.importer.model

import java.util.UUID

data class Question(
    val id: String = UUID.randomUUID().toString(),
    val number: String = "",
    val type: QuestionType,
    val question: String,
    val options: List<Option> = emptyList(),
    val answer: List<String> = emptyList(),
    val blankAnswers: List<List<String>> = emptyList(),
    val analysis: String = "",
    val category: String = "",
    val images: List<QuestionImage> = emptyList(),
    val score: Double? = null,
    val subject: String = "",
    val grade: String = "",
    val difficulty: String = "",
    val knowledgePoints: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val source: String = "",
    val sourceFileId: String = "",
    val version: Int = 1,
    val reviewStatus: String = "approved",
    val aiConfidence: Double? = null,
    val warnings: List<String> = emptyList()
)

data class Option(
    val key: String,
    val text: String
)

data class QuestionImage(
    val id: String = UUID.randomUUID().toString(),
    val localPath: String,
    val sourceName: String = "",
    val order: Int = 0,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long = 0L
)

enum class QuestionType {
    SINGLE,
    MULTIPLE,
    JUDGE,
    BLANK,
    SHORT
}

data class ImportResult(
    val questions: List<Question>,
    val strategyName: String,
    val warnings: List<ImportWarning>,
    val diagnostics: ImportDiagnostics
)

data class ImportWarning(
    val level: WarningLevel,
    val questionNumber: String? = null,
    val message: String
)

enum class WarningLevel {
    NORMAL,
    WARNING,
    ERROR
}

data class ImportDiagnostics(
    val normalizedLength: Int = 0,
    val blockCount: Int = 0,
    val answeredCount: Int = 0,
    val candidateCount: Int = 0,
    val notes: List<String> = emptyList()
)
