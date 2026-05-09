package com.yiqiu.shirohaquiz.importer.model

data class ImportIssue(
    val questionNumber: String,
    val message: String,
    val isHardError: Boolean
)
