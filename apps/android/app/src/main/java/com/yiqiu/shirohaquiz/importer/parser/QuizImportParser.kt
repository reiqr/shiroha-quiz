package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.ImportResult
import com.yiqiu.shirohaquiz.importer.score.ImportStrategyScorer
import com.yiqiu.shirohaquiz.importer.validate.ImportValidator

object QuizImportParser {
    fun parseStandardText(raw: String): ImportResult {
        val normalized = QuestionTextNormalizer.normalize(raw)
        val questions = StandardQuestionParser.parse(normalized)
        val issues = ImportValidator.validate(questions)
        ImportStrategyScorer.score(questions, issues)
        return ImportResult(
            strategyName = "标准题库原生解析",
            questions = questions,
            issues = issues
        )
    }
}
