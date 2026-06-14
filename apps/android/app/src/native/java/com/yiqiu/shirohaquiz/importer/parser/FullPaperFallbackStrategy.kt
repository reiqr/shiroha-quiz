package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.assets.QuestionImageMarker
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType

/**
 * Low-priority fallback for full exam papers.
 *
 * This strategy is intentionally conservative: it is only enabled when the text looks like a
 * complete public-exam style paper and the standard parser has low answer coverage. It keeps
 * normal question-bank parsing untouched.
 */
object FullPaperFallbackStrategy {
    private data class QuestionSegment(
        val index: Int,
        val category: String,
        val text: String
    )

    private data class AnswerSegment(
        val targetQuestionSegmentIndex: Int?,
        val text: String
    )

    private sealed class PaperChunk {
        data class Questions(val segment: QuestionSegment) : PaperChunk()
        data class Answers(val segment: AnswerSegment) : PaperChunk()
    }

    private enum class Mode { QUESTION, ANSWER }

    private val paperFrontMatterRegex = Regex(
        """^(?:说明\s*[:：]|注意事项|密卷|绝密|祝各位考生|时间[:：]|考试时间[:：]|请仔细阅读|监考老师|请用\s*2B|答题卡|请勿|在考试结束|全部测验到此结束)"""
    )

    private val answerHeadingRegex = Regex(
        """^(?:答案|答案解析|参考答案|参考答案解析|试题答案|解析|答案及解析|答案与解析)[:：]?$"""
    )

    private val answerLikeLineRegex = Regex(
        """^\s*(?:第\s*)?\d{1,4}\s*(?:题)?\s*[.、．:：]?\s*(?:[【\[]\s*(?:答案|解析)\s*[】\]]|(?:答案|解析)\s*[:：]|[A-Ga-g](?:\s|$|[\u4e00-\u9fa5]))""",
        RegexOption.IGNORE_CASE
    )

    private val questionStartRegex = Regex(
        """^\s*(?:[【\[]\s*)?\d{1,4}(?:\s*[】\]])?\s*[.、．:：)）]?\s*\S+"""
    )

    private val materialIntroRegex = Regex(
        """(根据(?:以下|下列|上述|给定)?(?:资料|材料|图表|统计资料).*回答\s*\d+\s*[~～\-—至到]\s*\d+\s*题)"""
    )
    private val materialIntroLineRegex = Regex(
        """^\s*(?:[一二三四五六七八九十0-9]+[、.．:：]\s*)?根据(?:以下|下列|上述|给定)?(?:资料|材料|图表|统计资料).*回答\s*\d{1,4}\s*[~～\-—至到]\s*\d{1,4}\s*题"""
    )
    private val questionLikeRegex = Regex(
        """(问|哪|何|多少|几个|几|下列|以下|选择|选出|正确|错误|最|能够|能由|意在|主要|填入|划线|划横线|\?|？|\(\s*\)|（\s*）)"""
    )
    private val tableRowLikeRegex = Regex(
        """^\s*\d+(?:\.\d+)?\s+(?:\d+(?:\.\d+)?\s+){1,}|^\s*\d+(?:\.\d+)?\s*(?:第一|第二|第三|固定资产|社会消费|地方财政|实际利用|进出口|指标|占全国|增长速度|产业产值)"""
    )

    fun shouldTry(text: String, standardQuestions: List<Question>): Boolean {
        if (!looksLikeFullPaper(text)) return false
        if (standardQuestions.isEmpty()) return true

        val answeredCoverage = standardQuestions.count { it.answer.isNotEmpty() }.toDouble() / standardQuestions.size.toDouble()
        val suspiciousQuestionCount = standardQuestions.count { question ->
            val stem = question.question.take(40)
            stem.contains("说明") || stem.contains("绝密") || stem.contains("密卷") || stem.contains("注意事项")
        }
        val suspiciousSubjectiveCount = standardQuestions.count(::looksLikeMisparsedObjectiveQuestion)
        return answeredCoverage < 0.85 || suspiciousQuestionCount > 0 || suspiciousSubjectiveCount >= 2
    }

    fun looksLikeFullPaper(text: String): Boolean {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return false
        val sectionHits = lines.count { FullPaperSectionParser.parseExamSection(it) != null }
        val paperSignals = listOf("说明：", "说明:", "注意事项", "密卷", "绝密", "答题卡", "考试结束")
            .count { signal -> text.contains(signal) }
        val answerSignals = lines.count { answerHeadingRegex.matches(it) || answerLikeLineRegex.containsMatchIn(it) }
        val materialSignals = lines.count { materialIntroRegex.containsMatchIn(it) }
        return sectionHits >= 2 ||
            (sectionHits >= 1 && answerSignals >= 5) ||
            (paperSignals >= 2 && answerSignals >= 5) ||
            (materialSignals >= 1 && answerSignals >= 5)
    }

    fun parse(text: String): List<Question> {
        val chunks = splitIntoPaperChunks(text)
        if (chunks.none { it is PaperChunk.Questions }) return emptyList()

        val questionsBySegment = mutableMapOf<Int, List<Question>>()
        val orderedQuestions = mutableListOf<Question>()
        chunks.forEach { chunk ->
            if (chunk is PaperChunk.Questions) {
                val parsed = StandardQuestionParser.parse(
                    text = preprocessQuestionSegment(chunk.segment.text),
                    forcedType = null,
                    category = chunk.segment.category,
                    allowUnnumbered = false
                ).map { question ->
                    question.copy(category = chunk.segment.category.ifBlank { question.category })
                }.filter(::isValidPaperQuestion)
                questionsBySegment[chunk.segment.index] = parsed
                orderedQuestions += parsed
            }
        }

        if (orderedQuestions.isEmpty()) return emptyList()

        val answerSegments = chunks.mapNotNull { (it as? PaperChunk.Answers)?.segment }
        if (answerSegments.isEmpty()) return normalizeFullPaperQuestions(orderedQuestions)

        var mergedQuestions: List<Question> = orderedQuestions
        val globalAnswers = mutableListOf<ParsedAnswerEntry>()

        answerSegments.forEach { segment ->
            val answers = AnswerParser.parse(segment.text)
            if (answers.isEmpty()) return@forEach
            val targetIndex = segment.targetQuestionSegmentIndex
            val targetQuestions = targetIndex?.let { questionsBySegment[it] }.orEmpty()

            val shouldMergeGlobally = targetQuestions.isEmpty() || answers.size > (targetQuestions.size * 1.5).toInt().coerceAtLeast(4)
            if (shouldMergeGlobally) {
                globalAnswers += answers
            } else {
                val mergedSegment = mergeFullPaperSegmentAnswers(targetQuestions, answers)
                mergedQuestions = replaceSegmentQuestions(mergedQuestions, targetQuestions, mergedSegment)
            }
        }

        if (globalAnswers.isNotEmpty()) {
            mergedQuestions = DualFileMerger.mergeAuto(mergedQuestions, globalAnswers).questions
        }

        return normalizeFullPaperQuestions(mergedQuestions)
    }

    private fun splitIntoPaperChunks(text: String): List<PaperChunk> {
        val chunks = mutableListOf<PaperChunk>()
        var mode = Mode.QUESTION
        var category = ""
        var started = false
        var segmentIndex = 0
        var lastQuestionSegmentIndex: Int? = null
        val buffer = mutableListOf<String>()

        fun flush() {
            val body = buffer.joinToString("\n").trim()
            if (body.isBlank()) {
                buffer.clear()
                return
            }
            if (mode == Mode.QUESTION) {
                segmentIndex += 1
                lastQuestionSegmentIndex = segmentIndex
                chunks += PaperChunk.Questions(QuestionSegment(segmentIndex, category, body))
            } else {
                chunks += PaperChunk.Answers(AnswerSegment(lastQuestionSegmentIndex, body))
            }
            buffer.clear()
        }

        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isBlank()) return@forEach

            val section = FullPaperSectionParser.parseExamSection(line)
            if (section != null) {
                flush()
                started = true
                mode = Mode.QUESTION
                category = section
                return@forEach
            }

            if (!started) {
                if (paperFrontMatterRegex.containsMatchIn(line)) return@forEach
                if (!questionStartRegex.containsMatchIn(line)) return@forEach
                started = true
            }

            if (answerHeadingRegex.matches(line)) {
                flush()
                mode = Mode.ANSWER
                return@forEach
            }

            if (mode == Mode.QUESTION && answerLikeLineRegex.containsMatchIn(line) && buffer.isNotEmpty()) {
                val nextWindowQuestionCount = buffer.count { questionStartRegex.containsMatchIn(it.trim()) }
                if (nextWindowQuestionCount >= 3 || buffer.joinToString("\n").length > 500) {
                    flush()
                    mode = Mode.ANSWER
                }
            }

            buffer += raw
        }
        flush()
        return chunks
    }

    private fun preprocessQuestionSegment(text: String): String {
        return text.lineSequence()
            .filterNot { line ->
                val trimmed = line.trim()
                isPaperFrontMatterLine(trimmed) || answerHeadingRegex.matches(trimmed) || materialIntroLineRegex.containsMatchIn(trimmed)
            }
            .flatMap { line -> repairDenseQuestionLine(line).lineSequence() }
            .joinToString("\n")
    }

    private fun isPaperFrontMatterLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return false
        if (paperFrontMatterRegex.containsMatchIn(trimmed)) return true
        if (questionStartRegex.containsMatchIn(trimmed)) return false
        if (trimmed.length > 160) return false
        return Regex(
            """(?:注意事项|绝密|密卷|祝各位考生|请仔细阅读|监考老师|请用\s*2B|答题卡|考试时间\s*[:：]|请勿|考试结束)"""
        ).containsMatchIn(trimmed)
    }

    private fun containsPaperFrontMatterContamination(stem: String): Boolean {
        val prefix = stem.trim().take(160)
        if (prefix.isBlank()) return false
        if (paperFrontMatterRegex.containsMatchIn(prefix)) return true
        return Regex(
            """(?:^|\s)(?:注意事项|绝密|密卷|祝各位考生|请仔细阅读|监考老师|请用\s*2B|答题卡|考试时间|请勿|考试结束)\s*[:：]?"""
        ).containsMatchIn(prefix)
    }

    private fun repairDenseQuestionLine(raw: String): String {
        var line = raw
        if (line.length >= 80) {
            line = line.replace(
                Regex("""(?<!^)(?<![\d.])(\d{1,3}\s*[.、．:：]\s*(?=[\u4e00-\u9fa5]))"""),
                "\n$1"
            )
        }
        if (line.length >= 40) {
            line = line
                .replace(Regex("""([^\n\s])([A-Ga-g]\s*[.、．:：)）]\s*)"""), "$1\n$2")
                .replace(Regex("""\s+([A-Ga-g]\s*[.、．:：)）]\s*)"""), "\n$1")
        }
        return line
    }

    private fun isValidPaperQuestion(question: Question): Boolean {
        val stem = question.question.trim()
        if (stem.isBlank()) return false
        if (containsPaperFrontMatterContamination(stem)) return false
        if (QuestionImageMarker.contains(stem)) return true
        if (tableRowLikeRegex.containsMatchIn(stem)) return false
        if (Regex("""^\d+\s*表\d*""").containsMatchIn(stem)) return false
        if (stem.contains("表") && stem.contains("指标")) return false
        if (stem.contains("增长速度") && stem.contains("占全国")) return false
        if (stem.length <= 2 && question.options.isEmpty()) return false
        if (answerLikeLineRegex.containsMatchIn(stem) && question.options.isEmpty()) return false
        if (question.options.isEmpty() && !questionLikeRegex.containsMatchIn(stem)) return false
        return true
    }


    private fun mergeFullPaperSegmentAnswers(
        targetQuestions: List<Question>,
        answers: List<ParsedAnswerEntry>
    ): List<Question> {
        if (targetQuestions.isEmpty() || answers.isEmpty()) return targetQuestions
        val byNumber = DualFileMerger.mergeByNumber(targetQuestions, answers).questions
        val beforeAnswered = targetQuestions.count { it.answer.isNotEmpty() }
        val byNumberGain = byNumber.count { it.answer.isNotEmpty() } - beforeAnswered
        val numberHitCount = targetQuestions.count { question ->
            answers.any { it.number == question.number }
        }
        if (byNumberGain > 0 || numberHitCount >= (targetQuestions.size / 2).coerceAtLeast(3)) {
            return byNumber
        }
        return DualFileMerger.mergeBySequence(targetQuestions, answers).questions
    }

    private fun replaceSegmentQuestions(
        all: List<Question>,
        oldSegment: List<Question>,
        newSegment: List<Question>
    ): MutableList<Question> {
        if (oldSegment.isEmpty()) return all.toMutableList()
        val firstIndex = all.indexOfFirst { it.id == oldSegment.first().id }
        if (firstIndex < 0) return all.toMutableList()
        val result = all.toMutableList()
        repeat(oldSegment.size) { result.removeAt(firstIndex) }
        result.addAll(firstIndex, newSegment)
        return result
    }

    private fun normalizeFullPaperQuestions(questions: List<Question>): List<Question> {
        val scopedQuestions = assignImplicitSectionCategoriesForFullPaper(questions)
        return scopedQuestions.map { question ->
            val objectiveAnswer = question.answer.filter { Regex("""^[A-G]$""").matches(it) }
            val normalizedType = when {
                question.options.size >= 2 && objectiveAnswer.size > 1 -> QuestionType.MULTIPLE
                question.options.size >= 2 && objectiveAnswer.size == 1 && question.type != QuestionType.JUDGE -> QuestionType.SINGLE
                else -> question.type
            }
            val category = enrichMaterialHint(question.category, question.question)
            question.copy(type = normalizedType, category = category)
        }
    }

    private fun looksLikeMisparsedObjectiveQuestion(question: Question): Boolean {
        if (question.type != QuestionType.SHORT && question.type != QuestionType.BLANK) return false
        if (isExplicitSubjectiveCategory(question.category)) return false
        if (looksLikeSubjectivePrompt(question.question)) return false
        if (question.options.isNotEmpty()) return false

        val stem = question.question.trim()
        if (CompactQuestionRepair.hasCompactOptionSequence(stem)) return true
        return questionLikeRegex.containsMatchIn(stem) &&
            Regex("""(?:下列|以下|哪项|哪个|选择|选出|正确|错误|不正确|最合适|最符合)""").containsMatchIn(stem)
    }

    private fun isExplicitSubjectiveCategory(category: String): Boolean {
        return Regex("""(?:简答|填空|问答|论述|主观|案例分析|材料分析)""").containsMatchIn(category)
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

    private fun assignImplicitSectionCategoriesForFullPaper(questions: List<Question>): List<Question> {
        if (questions.isEmpty()) return questions
        var lastNumericNumber: Int? = null
        var sectionIndex = 1
        return questions.map { question ->
            val numericNumber = question.number.trim().toIntOrNull()
            if (numericNumber == 1 && lastNumericNumber != null && (lastNumericNumber ?: 0) >= 2) {
                sectionIndex += 1
            }
            if (numericNumber != null) {
                lastNumericNumber = numericNumber
            }
            if (question.category.isNotBlank()) {
                question
            } else {
                question.copy(category = implicitFullPaperSectionTitle(question.type, sectionIndex))
            }
        }
    }

    private fun implicitFullPaperSectionTitle(type: QuestionType, sectionIndex: Int): String {
        val label = when (type) {
            QuestionType.SINGLE -> "单选题"
            QuestionType.MULTIPLE -> "多选题"
            QuestionType.JUDGE -> "判断题"
            QuestionType.BLANK -> "填空题"
            QuestionType.SHORT -> "简答题"
        }
        return "第${sectionIndex}组/$label"
    }

    private fun enrichMaterialHint(category: String, stem: String): String {
        return if (materialIntroRegex.containsMatchIn(stem) && !category.contains("材料")) {
            listOf(category, "材料题").filter { it.isNotBlank() }.joinToString(" / ")
        } else {
            category
        }
    }
}

object FullPaperSectionParser {
    private val prefixRegex = Regex(
        """^\s*(?:(?:第[一二三四五六七八九十百0-9]+|[一二三四五六七八九十百0-9]+)\s*(?:部分|模块|章|节)[、.．:：]?|[一二三四五六七八九十百]+[、.．:：]|\([一二三四五六七八九十百]+\)|（[一二三四五六七八九十百]+）|Part\s*[IVX0-9]+|Section\s*[A-Z0-9]+)\s*""",
        RegexOption.IGNORE_CASE
    )

    // 公考/事业编五类主科目别名，按常识→言语→数理→推理→资料顺序排列
    private val aliases = listOf(
        "常识判断", "常识", "公共基础知识", "综合知识", "基础知识",
        "言语理解与表达", "语言理解与表达", "言语理解", "语言理解", "言语表达", "阅读理解", "片段阅读", "选词填空", "逻辑填空", "语句表达", "语句排序", "文章阅读",
        "数量关系", "数学能力", "数学运算", "数字推理", "数学推理", "数量分析",
        "判断推理", "图形推理", "定义判断", "类比推理", "逻辑判断", "演绎推理", "分析推理", "事件排序", "空间推理",
        "资料分析", "材料分析", "资料判断", "统计资料分析", "图表分析", "文字资料", "表格资料", "图形资料", "综合资料分析"
    )

    fun parseExamSection(rawLine: String): String? {
        val line = rawLine.trim().trimEnd(':', '：')
        if (line.isBlank() || line.length > 80) return null
        val simplified = prefixRegex.replace(line, "").trim().trimEnd(':', '：')
        val matched = aliases.firstOrNull { alias -> simplified.contains(alias) || line.contains(alias) } ?: return null
        return canonicalName(matched)
    }

    private fun canonicalName(alias: String): String {
        return when (alias) {
            "语言理解与表达", "言语理解", "语言理解", "言语表达", "阅读理解", "片段阅读", "选词填空", "逻辑填空", "语句表达", "语句排序", "文章阅读" -> "言语理解与表达"
            "数学能力", "数学运算", "数字推理", "数学推理", "数量分析" -> "数量关系"
            "材料分析", "资料判断", "统计资料分析", "图表分析", "文字资料", "表格资料", "图形资料", "综合资料分析" -> "资料分析"
            "常识", "公共基础知识", "综合知识", "基础知识" -> "常识判断"
            "图形推理", "定义判断", "类比推理", "逻辑判断", "演绎推理", "分析推理", "事件排序", "空间推理" -> alias
            else -> alias
        }
    }
}
