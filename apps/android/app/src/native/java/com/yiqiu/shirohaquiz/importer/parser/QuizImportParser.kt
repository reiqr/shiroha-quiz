package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.ImportDiagnostics
import com.yiqiu.shirohaquiz.importer.model.ImportResult
import com.yiqiu.shirohaquiz.importer.model.ImportWarning
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.importer.model.WarningLevel
import com.yiqiu.shirohaquiz.importer.score.ImportStrategyScorer
import com.yiqiu.shirohaquiz.importer.validate.ImportValidator

object QuizImportParser {
    fun parseStandardText(raw: String): ImportResult {
        val normalized = QuestionTextNormalizer.normalize(raw)
        val candidates = mutableListOf<Candidate>()
        val hasAnswerSection = AnswerSectionParser.hasAnswerSection(normalized)
        val questionArea = if (hasAnswerSection) {
            AnswerSectionParser.splitSections(normalized).first
        } else {
            normalized
        }

        val rewrittenQuestionArea = SharedStemQuestionFallbackParser.rewrite(questionArea)
        val primaryQuestions = QuestionParser.parseStandardFirst(rewrittenQuestionArea ?: questionArea)
        val primaryCandidate = if (hasAnswerSection) {
            val answerEntries = AnswerSectionParser.parse(normalized)
            val merged = DualFileMerger.mergeAuto(primaryQuestions, answerEntries)
            buildCandidate(
                name = buildPrimaryStrategyName(
                    hasSharedStemRewrite = rewrittenQuestionArea != null,
                    hasAnswerSection = true,
                    mergeName = merged.name
                ),
                questions = merged.questions,
                extraWarnings = merged.warnings
            )
        } else {
            buildCandidate(
                name = buildPrimaryStrategyName(
                    hasSharedStemRewrite = rewrittenQuestionArea != null,
                    hasAnswerSection = false
                ),
                questions = primaryQuestions
            )
        }
        candidates += primaryCandidate

        val tableCandidate = ExcelQuestionTableParser.parse(normalized)
            .takeIf { it.isNotEmpty() }
            ?.let { buildCandidate("Excel/CSV 表格题库解析", it) }
            ?.also { candidates += it }

        // 整卷兜底是否需要启动，必须依据答案合并前的题目结构判断。
        val fullPaperCandidate = if (FullPaperFallbackStrategy.shouldTry(normalized, primaryQuestions)) {
            FullPaperFallbackStrategy.parse(normalized)
                .takeIf { it.isNotEmpty() }
                ?.let { buildCandidate("整卷真题复杂兜底解析", it) }
                ?.also { candidates += it }
        } else {
            null
        }

        val best = chooseBestCandidate(
            normalized = normalized,
            primaryCandidate = primaryCandidate,
            tableCandidate = tableCandidate,
            fullPaperCandidate = fullPaperCandidate
        )

        return buildResult(
            normalized = normalized,
            candidates = candidates,
            best = best
        )
    }

    fun parseDualText(questionText: String, answerText: String): ImportResult {
        val normalizedQuestion = QuestionTextNormalizer.normalize(questionText)
        val normalizedAnswer = QuestionTextNormalizer.normalize(answerText)
        val questionCandidates = mutableListOf<Candidate>()

        val rewrittenQuestion = SharedStemQuestionFallbackParser.rewrite(normalizedQuestion)
        val primaryQuestionCandidate = buildCandidate(
            name = buildPrimaryStrategyName(
                hasSharedStemRewrite = rewrittenQuestion != null,
                hasAnswerSection = false
            ),
            questions = QuestionParser.parseStandardFirst(rewrittenQuestion ?: normalizedQuestion)
        )
        questionCandidates += primaryQuestionCandidate

        val tableQuestionCandidate = ExcelQuestionTableParser.parse(normalizedQuestion)
            .takeIf { it.isNotEmpty() }
            ?.let { buildCandidate("Excel/CSV 表格题目解析", it) }
            ?.also { questionCandidates += it }

        val fullPaperQuestionCandidate = if (
            FullPaperFallbackStrategy.shouldTry(normalizedQuestion, primaryQuestionCandidate.questions)
        ) {
            FullPaperFallbackStrategy.parse(normalizedQuestion)
                .takeIf { it.isNotEmpty() }
                ?.let { buildCandidate("整卷真题复杂题目兜底解析", it) }
                ?.also { questionCandidates += it }
        } else {
            null
        }

        val answerCandidates = buildList {
            val plainAnswers = AnswerParser.parse(normalizedAnswer)
            if (plainAnswers.isNotEmpty()) add("普通答案表" to plainAnswers)
            val sectionAnswers = AnswerSectionParser.parse(normalizedAnswer)
            if (sectionAnswers.isNotEmpty()) add("答案分区表" to sectionAnswers)
            val fullParsedAnswers = QuestionParser.parseStandardFirst(normalizedAnswer)
                .filter { it.answer.isNotEmpty() }
                .mapIndexed { index, question ->
                    ParsedAnswerEntry(
                        number = question.number,
                        answer = question.answer,
                        analysis = question.analysis,
                        type = question.type,
                        sequence = index
                    )
                }
            if (fullParsedAnswers.isNotEmpty()) add("完整题库答案兜底" to fullParsedAnswers)
            val mixed = (plainAnswers + sectionAnswers + fullParsedAnswers)
                .distinctBy { Triple(it.type, it.number, it.sequence) }
            if (mixed.isNotEmpty()) add("混合答案来源" to mixed)
        }

        val mergedCandidates = mutableListOf<Candidate>()
        fun mergeQuestionCandidate(questionCandidate: Candidate?): Candidate? {
            questionCandidate ?: return null
            val localMerged = answerCandidates.map { (answerStrategy, answers) ->
                val merged = DualFileMerger.mergeAuto(questionCandidate.questions, answers)
                buildCandidate(
                    name = "${questionCandidate.name} + $answerStrategy/${merged.name}",
                    questions = merged.questions,
                    extraWarnings = merged.warnings
                ).also { mergedCandidates += it }
            }
            return localMerged.maxByOrNull { it.score } ?: questionCandidate
        }

        // 各题目策略先分别完成答案合并，再按受控规则选择题目策略，避免提前淘汰
        // “未合并答案时看不出优势、合并后才正确”的候选。
        val mergedPrimaryCandidate = mergeQuestionCandidate(primaryQuestionCandidate) ?: primaryQuestionCandidate
        val mergedTableCandidate = mergeQuestionCandidate(tableQuestionCandidate)
        val mergedFullPaperCandidate = mergeQuestionCandidate(fullPaperQuestionCandidate)
        val best = chooseBestCandidate(
            normalized = normalizedQuestion,
            primaryCandidate = mergedPrimaryCandidate,
            tableCandidate = mergedTableCandidate,
            fullPaperCandidate = mergedFullPaperCandidate
        )
        val diagnosticCandidates = questionCandidates + mergedCandidates

        return buildResult(
            normalized = normalizedQuestion + "\n" + normalizedAnswer,
            candidates = diagnosticCandidates,
            best = best
        )
    }

    private fun buildPrimaryStrategyName(
        hasSharedStemRewrite: Boolean,
        hasAnswerSection: Boolean,
        mergeName: String = ""
    ): String {
        val localFallback = if (hasSharedStemRewrite) {
            "标准优先解析（单题紧凑修复 + 共用题干局部兜底）"
        } else {
            "标准优先解析（单题紧凑修复）"
        }
        return if (hasAnswerSection) {
            "$localFallback + 答案集中区识别/$mergeName"
        } else {
            localFallback
        }
    }

    private fun chooseBestCandidate(
        normalized: String,
        primaryCandidate: Candidate,
        tableCandidate: Candidate?,
        fullPaperCandidate: Candidate?
    ): Candidate {
        var best = primaryCandidate

        if (tableCandidate != null && shouldUseSpecializedTable(primaryCandidate, tableCandidate)) {
            best = tableCandidate
        }

        if (
            fullPaperCandidate != null &&
            shouldUseFullPaper(normalized, best, fullPaperCandidate)
        ) {
            best = fullPaperCandidate
        }

        return best
    }

    private fun shouldUseSpecializedTable(primary: Candidate, table: Candidate): Boolean {
        if (primary.questions.isEmpty()) return true
        val primaryErrors = primary.warnings.count { it.level == WarningLevel.ERROR }
        val tableErrors = table.warnings.count { it.level == WarningLevel.ERROR }
        val countFloor = (primary.questions.size * 0.8).toInt().coerceAtLeast(1)
        val preservesMostQuestions = table.questions.size >= countFloor
        val materiallyCleaner = tableErrors < primaryErrors || table.score >= primary.score + 30
        return preservesMostQuestions && materiallyCleaner
    }

    private fun shouldUseFullPaper(
        normalized: String,
        primary: Candidate,
        fullPaper: Candidate
    ): Boolean {
        if (!FullPaperFallbackStrategy.looksLikeFullPaper(normalized)) return false
        if (fullPaper.questions.isEmpty()) return false
        if (primary.questions.isEmpty()) return true

        val primaryCount = primary.questions.size
        val fullCount = fullPaper.questions.size
        val primaryErrors = primary.warnings.count { it.level == WarningLevel.ERROR }
        val fullErrors = fullPaper.warnings.count { it.level == WarningLevel.ERROR }
        val primarySuspiciousSubjective = primary.questions.count(::looksLikeMisparsedObjectiveQuestion)
        val fullSuspiciousSubjective = fullPaper.questions.count(::looksLikeMisparsedObjectiveQuestion)
        val primaryShortStem = primary.questions.count {
            it.question.trim().length <= 3 && it.options.isEmpty()
        }
        val fullShortStem = fullPaper.questions.count {
            it.question.trim().length <= 3 && it.options.isEmpty()
        }
        val primaryFrontMatter = primary.questions.count { question ->
            containsFrontMatterContamination(question.question)
        }
        val fullFrontMatter = fullPaper.questions.count { question ->
            containsFrontMatterContamination(question.question)
        }
        val primaryValidObjective = primary.questions.count(::isStructurallyValidObjectiveQuestion)
        val fullValidObjective = fullPaper.questions.count(::isStructurallyValidObjectiveQuestion)

        if (downgradesExplicitSubjectiveQuestions(primary.questions, fullPaper.questions)) return false

        val primaryOverallUnreliable =
            primaryErrors >= (primaryCount / 5).coerceAtLeast(2) ||
                primarySuspiciousSubjective >= (primaryCount / 5).coerceAtLeast(2) ||
                primaryShortStem >= 3 ||
                primaryFrontMatter > 0

        // 双文件/密集整卷中，标准路径可能只得到少量“看起来正常”的题，因而没有错误告警。
        // 此时允许结构完整的整卷候选凭“明确补回多题”接管，但必须同时满足客观结构、
        // 错误数和前言污染均不退化，避免重新回到按总分自由覆盖标准结果。
        val recoveryGain = (primaryCount / 10).coerceAtLeast(2)
        val strongMissingQuestionRecovery =
            fullCount >= primaryCount + recoveryGain &&
                fullValidObjective >= primaryValidObjective + recoveryGain &&
                fullErrors <= primaryErrors &&
                fullShortStem <= primaryShortStem &&
                fullFrontMatter <= primaryFrontMatter &&
                fullValidObjective * 10 >= fullCount * 7
        if (!primaryOverallUnreliable && !strongMissingQuestionRecovery) return false

        val countIsReasonable = fullCount >= (primaryCount * 0.7).toInt().coerceAtLeast(3)
        val concreteStructureGain =
            fullErrors < primaryErrors ||
                strongMissingQuestionRecovery ||
                fullSuspiciousSubjective < primarySuspiciousSubjective ||
                fullValidObjective >= primaryValidObjective + 2 ||
                fullShortStem < primaryShortStem ||
                fullFrontMatter < primaryFrontMatter
        val scoreDoesNotRegressMaterially = fullPaper.score >= primary.score - 20

        return countIsReasonable && concreteStructureGain && scoreDoesNotRegressMaterially
    }

    private fun looksLikeMisparsedObjectiveQuestion(question: Question): Boolean {
        if (question.type != QuestionType.SHORT && question.type != QuestionType.BLANK) return false
        if (isExplicitSubjectiveQuestion(question)) return false
        if (question.options.isNotEmpty()) return false
        if (CompactQuestionRepair.hasCompactOptionSequence(question.question)) return true
        return Regex("""(?:下列|以下|哪项|哪个|选择|选出|正确|错误|不正确|最合适|最符合)""")
            .containsMatchIn(question.question)
    }

    private fun isStructurallyValidObjectiveQuestion(question: Question): Boolean {
        val isObjective = question.type == QuestionType.SINGLE ||
            question.type == QuestionType.MULTIPLE ||
            question.type == QuestionType.JUDGE
        if (!isObjective || question.options.size < 2) return false
        val optionKeys = question.options.map { it.key.uppercase() }.toSet()
        return question.answer.isEmpty() || question.answer.all { it.uppercase() in optionKeys }
    }

    private fun isExplicitSubjectiveQuestion(question: Question): Boolean {
        if (question.type != QuestionType.SHORT && question.type != QuestionType.BLANK) return false
        if (Regex("""(?:简答|填空|问答|论述|主观|案例分析|材料分析)""").containsMatchIn(question.category)) {
            return true
        }
        if (looksLikeSubjectivePrompt(question.question)) return true
        return question.answer.isNotEmpty() && strictObjectiveAnswer(question.answer) == null
    }

    private fun looksLikeSubjectivePrompt(stem: String): Boolean {
        val normalized = stem.trim()
        val choiceIntent = Regex(
            """(?:下列|以下).{0,24}(?:哪|正确|错误|不正确|符合)|选择|选出|最(?:合适|符合|恰当)|应(?:选择|选)"""
        ).containsMatchIn(normalized)
        if (choiceIntent) return false

        val subjectiveLead = Regex(
            """^(?:请|试)?(?:分别)?(?:说明|简述|阐述|论述|比较|解释|概述|谈谈|回答|分析)"""
        ).containsMatchIn(normalized)
        val subjectivePurpose = Regex(
            """(?:差异|区别|联系|原因|影响|措施|方法|原则|条件|优缺点|意义|作用|过程|依据|适用|如何|为什么|各自|分别)"""
        ).containsMatchIn(normalized)
        val comparisonQuestion = Regex("""(?:有何|有什么).*(?:区别|差异|联系)""").containsMatchIn(normalized)
        return (subjectiveLead && subjectivePurpose) || comparisonQuestion
    }

    private fun containsFrontMatterContamination(stem: String): Boolean {
        val prefix = stem.trim().take(140)
        if (prefix.isBlank()) return false
        return Regex(
            """(?:^|\s)(?:说明\s*[:：]|注意事项|密卷|绝密|祝各位考生|请仔细阅读|监考老师|请用\s*2B|答题卡|请勿|考试时间\s*[:：]|时间\s*[:：])"""
        ).containsMatchIn(prefix)
    }

    private fun strictObjectiveAnswer(answers: List<String>): List<String>? {
        if (answers.isEmpty()) return emptyList()
        val raw = answers.joinToString(",").trim()
            .trim('[', ']', '【', '】', '(', ')', '（', '）')
            .trim()
        if (raw.isBlank()) return emptyList()
        if (!Regex("""^[A-Ga-g\s,，、;；/\\]+$""").matches(raw)) return null
        val compact = raw.replace(Regex("""[\s,，、;；/\\]+"""), "").uppercase()
        if (!Regex("""^[A-G]+$""").matches(compact)) return null
        val letters = compact.map { it.toString() }
        val normalized = letters.distinct()
        if (normalized.size != letters.size) return null
        return normalized.sorted()
    }

    private fun downgradesExplicitSubjectiveQuestions(
        primaryQuestions: List<Question>,
        fullPaperQuestions: List<Question>
    ): Boolean {
        val fullByNumber = fullPaperQuestions.groupBy { it.number }
        return primaryQuestions.withIndex().any { (index, primaryQuestion) ->
            if (!isExplicitSubjectiveQuestion(primaryQuestion)) return@any false
            val candidate = fullByNumber[primaryQuestion.number]?.firstOrNull()
                ?: fullPaperQuestions.getOrNull(index)
                ?: return@any false
            candidate.type == QuestionType.SINGLE ||
                candidate.type == QuestionType.MULTIPLE ||
                candidate.type == QuestionType.JUDGE
        }
    }

    private fun buildCandidate(
        name: String,
        questions: List<Question>,
        extraWarnings: List<ImportWarning> = emptyList()
    ): Candidate {
        val repairedQuestions = questions.map(::repairQuestionForDisplay)
        val warnings = ImportValidator.validate(repairedQuestions) + extraWarnings
        val score = ImportStrategyScorer.score(repairedQuestions, warnings)
        return Candidate(name, repairedQuestions, warnings, score)
    }

    private fun repairQuestionForDisplay(question: Question): Question {
        return if (question.type == QuestionType.JUDGE && question.options.isEmpty()) {
            question.copy(
                options = listOf(
                    com.yiqiu.shirohaquiz.importer.model.Option("A", "正确"),
                    com.yiqiu.shirohaquiz.importer.model.Option("B", "错误")
                )
            )
        } else {
            question
        }
    }

    private fun buildResult(
        normalized: String,
        candidates: List<Candidate>,
        best: Candidate
    ): ImportResult {
        val finalWarnings = if (
            best.questions.isEmpty() && best.warnings.none { it.level == WarningLevel.ERROR }
        ) {
            best.warnings + ImportWarning(WarningLevel.ERROR, null, "未识别到任何题目")
        } else {
            best.warnings
        }
        return ImportResult(
            questions = best.questions,
            strategyName = best.name,
            warnings = finalWarnings,
            diagnostics = ImportDiagnostics(
                normalizedLength = normalized.length,
                blockCount = QuestionBlockSplitter.split(normalized).size,
                answeredCount = best.questions.count { it.answer.isNotEmpty() },
                candidateCount = candidates.size,
                notes = buildDiagnosticNotes(best, candidates)
            )
        )
    }

    private fun buildDiagnosticNotes(best: Candidate, candidates: List<Candidate>): List<String> {
        val typeSummary = best.questions.groupingBy { it.type }.eachCount()
        val typeNote = "题型分布：单选${typeSummary[QuestionType.SINGLE] ?: 0} / 多选${typeSummary[QuestionType.MULTIPLE] ?: 0} / 判断${typeSummary[QuestionType.JUDGE] ?: 0} / 填空${typeSummary[QuestionType.BLANK] ?: 0} / 简答${typeSummary[QuestionType.SHORT] ?: 0}"
        val candidateNotes = candidates
            .distinctBy { it.name }
            .sortedByDescending { it.score }
            .take(5)
            .map { candidate ->
                "${candidate.name}：${candidate.questions.size}题 / 答案${candidate.questions.count { it.answer.isNotEmpty() }} / 分数${candidate.score}"
            }
        return listOf(typeNote) + candidateNotes
    }

    private data class Candidate(
        val name: String,
        val questions: List<Question>,
        val warnings: List<ImportWarning>,
        val score: Int
    )
}
