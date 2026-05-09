package com.yiqiu.shirohaquiz.importer.model

data class Question(
    val number: String,
    val type: QuestionType,
    val question: String,
    val options: List<Option> = emptyList(),
    val answer: List<String> = emptyList(),
    val analysis: String = ""
)
