package com.yiqiu.shirohaquiz.importer.model

data class ImportResult(
    val strategyName: String,
    val questions: List<Question>,
    val issues: List<ImportIssue>
)
