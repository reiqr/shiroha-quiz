package com.yiqiu.shirohaquiz.importer.score

import com.yiqiu.shirohaquiz.importer.model.ImportIssue
import com.yiqiu.shirohaquiz.importer.model.Question

object ImportStrategyScorer {
    fun score(questions: List<Question>, issues: List<ImportIssue>): Int {
        val hardErrors = issues.count { it.isHardError }
        val softErrors = issues.size - hardErrors
        val answered = questions.count { it.answer.isNotEmpty() }
        return answered * 100 - hardErrors * 120 - softErrors * 30
    }
}
