package com.yiqiu.shirohaquiz.ai

import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

private const val DEFAULT_AI_TIMEOUT_SECONDS = 60

data class AiReviewSuggestion(
    val questionId: String,
    val status: String,
    val issueTypes: List<String>,
    val reason: String,
    val suggestion: String,
    val suggestedType: String?,
    val suggestedAnswer: List<String>,
    val suggestedQuestion: String?,
    val suggestedOptions: List<Option>,
    val suggestedAnalysis: String?,
    val riskLevel: String,
    val canApply: Boolean,
    val needHumanReview: Boolean,
    val confidence: Double
)

data class AiAnalysisSuggestion(
    val questionId: String,
    val analysis: String,
    val needHumanReview: Boolean,
    val confidence: Double
)

data class AiSingleQuestionAnalysis(
    val questionId: String,
    val suggestedAnswer: String,
    val matchesLocalAnswer: Boolean?,
    val analysis: String,
    val confidence: String,
    val needsReview: Boolean,
    val warning: String
)

data class AiRefactorResult(
    val mode: String,
    val cleanedText: String?,
    val cleanedAnswerText: String?,
    val questions: List<Question>,
    val notes: List<String>
)

object ShirohaAiClient {
    fun testConnection(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        timeoutSeconds: Int = DEFAULT_AI_TIMEOUT_SECONDS
    ): String {
        validateConfig(apiBaseUrl, apiKey, modelName)
        val content = requestChatCompletion(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            modelName = modelName,
            systemPrompt = "你是 Shiroha Quiz 的接口连通性测试助手，只返回简短确认。",
            userPayload = "请只回复 JSON：{\"ok\":true,\"message\":\"connected\"}",
            timeoutSeconds = timeoutSeconds.coerceIn(15, 180),
            maxTokens = 64
        )
        return if (content.isBlank()) "连接成功。" else "连接成功：${content.take(40)}"
    }

    fun reviewQuestions(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        questions: List<Question>,
        timeoutSeconds: Int = DEFAULT_AI_TIMEOUT_SECONDS
    ): List<AiReviewSuggestion> {
        validateConfig(apiBaseUrl, apiKey, modelName)
        if (questions.isEmpty()) return emptyList()
        val content = requestChatCompletion(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            modelName = modelName,
            systemPrompt = AiPrompts.AI_REVIEW_SYSTEM_PROMPT,
            userPayload = JSONObject()
                .put("task", "review_questions")
                .put("outputFormat", reviewOutputContract())
                .put("questions", questionsToJson(questions))
                .toString(),
            timeoutSeconds = timeoutSeconds.coerceIn(15, 180)
        )
        return parseReviewSuggestions(content)
    }

    fun generateAnalysis(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        questions: List<Question>,
        timeoutSeconds: Int = DEFAULT_AI_TIMEOUT_SECONDS
    ): List<AiAnalysisSuggestion> {
        validateConfig(apiBaseUrl, apiKey, modelName)
        if (questions.isEmpty()) return emptyList()
        val content = requestChatCompletion(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            modelName = modelName,
            systemPrompt = AiPrompts.AI_ANALYSIS_SYSTEM_PROMPT,
            userPayload = JSONObject()
                .put("task", "generate_analysis")
                .put("outputFormat", analysisOutputContract())
                .put("questions", questionsToJson(questions))
                .toString(),
            timeoutSeconds = timeoutSeconds.coerceIn(15, 180)
        )
        return parseAnalysisSuggestions(content)
    }

    fun analyzeSingleQuestion(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        question: Question,
        userAnswer: List<String> = emptyList(),
        timeoutSeconds: Int = DEFAULT_AI_TIMEOUT_SECONDS
    ): AiSingleQuestionAnalysis {
        validateConfig(apiBaseUrl, apiKey, modelName)
        val content = requestChatCompletion(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            modelName = modelName,
            systemPrompt = AiPrompts.AI_SINGLE_QUESTION_ANALYSIS_SYSTEM_PROMPT,
            userPayload = JSONObject()
                .put("task", "single_question_analysis")
                .put("outputFormat", singleQuestionAnalysisOutputContract())
                .put("question", questionsToJson(listOf(question)).optJSONObject(0))
                .put("userAnswer", JSONArray().also { array -> userAnswer.forEach { array.put(it) } })
                .put("note", "AI 结果只用于练习复盘参考，不会自动修改题库答案或解析。")
                .toString(),
            timeoutSeconds = timeoutSeconds.coerceIn(15, 180)
        )
        return parseSingleQuestionAnalysis(content, question.id)
    }

    fun refactorQuestions(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        rawText: String,
        answerText: String,
        currentQuestions: List<Question>,
        warnings: List<String>,
        timeoutSeconds: Int = DEFAULT_AI_TIMEOUT_SECONDS
    ): AiRefactorResult {
        validateConfig(apiBaseUrl, apiKey, modelName)
        require(rawText.trim().isNotBlank()) { "AI 重构需要原始文本。" }
        val content = requestChatCompletion(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            modelName = modelName,
            systemPrompt = AiPrompts.AI_REFACTOR_SYSTEM_PROMPT,
            userPayload = JSONObject()
                .put("task", "refactor_questions")
                .put("outputFormat", refactorOutputContract())
                .put("sourceText", rawText)
                .put("answerText", answerText)
                .put("currentQuestionCount", currentQuestions.size)
                .put("currentQuestions", questionsToJson(currentQuestions))
                .put("warnings", JSONArray().also { array -> warnings.forEach { array.put(it) } })
                .toString(),
            timeoutSeconds = timeoutSeconds.coerceIn(15, 180)
        )
        return parseRefactorResult(content)
    }

    private fun requestChatCompletion(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        systemPrompt: String,
        userPayload: String,
        timeoutSeconds: Int,
        maxTokens: Int? = null
    ): String {
        val endpoint = normalizeChatEndpoint(apiBaseUrl)
        val timeoutMillis = timeoutSeconds.coerceIn(15, 180) * 1000
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMillis.coerceAtMost(30000)
            readTimeout = timeoutMillis
            doOutput = true
            useCaches = false
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
        }

        return try {
            val body = JSONObject()
                .put("model", modelName.trim())
                .put("temperature", 0.1)
                .put(
                    "messages",
                    JSONArray()
                        .put(JSONObject().put("role", "system").put("content", systemPrompt.trim()))
                        .put(JSONObject().put("role", "user").put("content", userPayload))
                )
            if (maxTokens != null) body.put("max_tokens", maxTokens)

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }

            if (code !in 200..299) {
                throw IllegalStateException(formatHttpError(code, responseText))
            }

            val response = JSONObject(responseText)
            val choices = response.optJSONArray("choices") ?: JSONArray()
            val first = choices.optJSONObject(0)
                ?: throw IllegalStateException("AI 接口没有返回 choices，请检查模型名称或接口兼容性。")
            val message = first.optJSONObject("message")
            val content = message?.optString("content").orEmpty()
            if (content.isBlank()) {
                throw IllegalStateException("AI 接口返回内容为空，请检查模型是否支持 chat/completions。")
            }
            content
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw IllegalStateException("AI 请求超时，请检查网络或适当调大超时秒数。", error)
        } catch (error: UnknownHostException) {
            throw IllegalStateException("无法连接 AI 接口域名，请检查 API 地址或网络。", error)
        } catch (error: IOException) {
            throw IllegalStateException("AI 网络请求失败：${error.message ?: "请检查接口地址和网络"}", error)
        } catch (error: JSONException) {
            throw IllegalStateException("AI 接口返回格式无法解析，请确认接口是否兼容 OpenAI chat/completions。", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun validateConfig(apiBaseUrl: String, apiKey: String, modelName: String) {
        require(apiBaseUrl.trim().isNotBlank()) { "请先填写 API 地址。" }
        require(apiKey.trim().isNotBlank()) { "请先填写 API Key。" }
        require(modelName.trim().isNotBlank()) { "请先填写模型名称。" }
    }

    private fun normalizeChatEndpoint(apiBaseUrl: String): String {
        val clean = apiBaseUrl.trim().trimEnd('/')
        require(clean.startsWith("https://")) {
            "API 地址必须以 https:// 开头，不支持明文 HTTP。"
        }
        return if (clean.endsWith("/chat/completions")) clean else "$clean/chat/completions"
    }

    private fun formatHttpError(code: Int, responseText: String): String {
        val apiMessage = extractApiErrorMessage(responseText)
        val hint = when (code) {
            400 -> "请求格式或模型参数不被接口接受"
            401, 403 -> "API Key 无效、余额不足或无权限"
            404 -> "API 地址或模型路径不正确"
            429 -> "请求过于频繁或额度受限"
            in 500..599 -> "服务商接口暂时异常"
            else -> "接口返回异常"
        }
        return "AI 接口请求失败：HTTP $code，$hint${if (apiMessage.isNotBlank()) "：$apiMessage" else ""}"
    }

    private fun extractApiErrorMessage(responseText: String): String {
        if (responseText.isBlank()) return ""
        val parsed = runCatching { JSONObject(responseText) }.getOrNull() ?: return responseText.take(160)
        val error = parsed.optJSONObject("error")
        val message = error?.optString("message").orEmpty().ifBlank { parsed.optString("message") }
        return message.take(160)
    }

    private fun questionsToJson(questions: List<Question>): JSONArray {
        val array = JSONArray()
        questions.forEachIndexed { index, question ->
            val options = JSONArray()
            question.options.forEach { option ->
                options.put(
                    JSONObject()
                        .put("key", option.key)
                        .put("text", option.text)
                )
            }

            val answers = JSONArray()
            question.answer.forEach { answers.put(it) }

            array.put(
                JSONObject()
                    .put("index", index)
                    .put("questionId", question.id)
                    .put("number", question.number)
                    .put("type", question.type.name.lowercase())
                    .put("question", question.question)
                    .put("options", options)
                    .put("answer", answers)
                    .put("analysis", question.analysis)
                    .put("category", question.category)
            )
        }
        return array
    }

    private fun parseReviewSuggestions(content: String): List<AiReviewSuggestion> {
        val root = JSONObject(extractJsonObject(content))
        val items = root.optJSONArray("items") ?: JSONArray()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val issueTypesJson = item.optJSONArray("issueTypes") ?: JSONArray()
            val suggestedAnswerJson = item.optJSONArray("suggestedAnswer") ?: JSONArray()
            val suggestedOptionsJson = item.optJSONArray("suggestedOptions") ?: JSONArray()
            val suggestedQuestion = item.optString("suggestedQuestion").nullIfBlankOrLiteralNull()
            val suggestedType = item.optString("suggestedType").nullIfBlankOrLiteralNull()
            val suggestedAnswer = (0 until suggestedAnswerJson.length())
                .map { suggestedAnswerJson.optString(it).trim().uppercase() }
                .filter { it.isNotBlank() }
            val suggestedOptions = (0 until suggestedOptionsJson.length()).mapNotNull { optionIndex ->
                val option = suggestedOptionsJson.optJSONObject(optionIndex) ?: return@mapNotNull null
                val key = option.optString("key").trim().uppercase()
                val value = option.optString("text").trim()
                if (key.isBlank() && value.isBlank()) null else Option(key, value)
            }
            val suggestedAnalysis = item.optString("suggestedAnalysis").nullIfBlankOrLiteralNull()
            val status = item.optString("status", "warning").ifBlank { "warning" }
            val issueTypes = (0 until issueTypesJson.length())
                .map { issueTypesJson.optString(it) }
                .filter { it.isNotBlank() }
            val riskLevel = item.optString("riskLevel", "needs_confirm").ifBlank { "needs_confirm" }
            val hasStructuredSuggestion = suggestedType != null ||
                suggestedAnswer.isNotEmpty() ||
                suggestedQuestion != null ||
                suggestedOptions.isNotEmpty() ||
                suggestedAnalysis != null
            val defaultCanApply = hasStructuredSuggestion && !riskLevel.equals("hard_error", ignoreCase = true)
            val canApply = item.optBoolean("canApply", defaultCanApply) &&
                !riskLevel.equals("hard_error", ignoreCase = true)
            val defaultNeedHumanReview = !status.equals("ok", ignoreCase = true) ||
                issueTypes.isNotEmpty() ||
                riskLevel.equals("hard_error", ignoreCase = true) ||
                hasStructuredSuggestion
            val needHumanReview = item.optBoolean("needHumanReview", defaultNeedHumanReview)
            AiReviewSuggestion(
                questionId = item.optString("questionId"),
                status = status,
                issueTypes = issueTypes,
                reason = item.optString("reason"),
                suggestion = item.optString("suggestion"),
                suggestedType = suggestedType,
                suggestedAnswer = suggestedAnswer,
                suggestedQuestion = suggestedQuestion,
                suggestedOptions = suggestedOptions,
                suggestedAnalysis = suggestedAnalysis,
                riskLevel = riskLevel,
                canApply = canApply,
                needHumanReview = needHumanReview,
                confidence = item.optDouble("confidence", 0.0)
            ).takeIf { it.questionId.isNotBlank() }
        }
    }

    private fun parseAnalysisSuggestions(content: String): List<AiAnalysisSuggestion> {
        val root = JSONObject(extractJsonObject(content))
        val items = root.optJSONArray("items") ?: JSONArray()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            AiAnalysisSuggestion(
                questionId = item.optString("questionId"),
                analysis = item.optString("analysis"),
                needHumanReview = item.optBoolean("needHumanReview", false),
                confidence = item.optDouble("confidence", 0.0)
            ).takeIf { it.questionId.isNotBlank() && it.analysis.isNotBlank() }
        }
    }

    private fun parseSingleQuestionAnalysis(content: String, fallbackQuestionId: String): AiSingleQuestionAnalysis {
        val root = JSONObject(extractJsonObject(content))
        val answerJson = root.optJSONArray("suggestedAnswer")
        val suggestedAnswer = if (answerJson != null) {
            (0 until answerJson.length())
                .map { answerJson.optString(it).trim() }
                .filter { it.isNotBlank() }
                .joinToString(" / ")
        } else {
            root.optString("suggestedAnswer").trim()
        }
        val hasMatchField = root.has("matchesLocalAnswer") && !root.isNull("matchesLocalAnswer")
        val matchesLocalAnswer = if (hasMatchField) root.optBoolean("matchesLocalAnswer") else null
        val confidence = root.optString("confidence", "MEDIUM").trim().uppercase().ifBlank { "MEDIUM" }
        return AiSingleQuestionAnalysis(
            questionId = root.optString("questionId", fallbackQuestionId).ifBlank { fallbackQuestionId },
            suggestedAnswer = suggestedAnswer.ifBlank { "无法判断" },
            matchesLocalAnswer = matchesLocalAnswer,
            analysis = root.optString("analysis").trim().ifBlank { "AI 未返回有效解析。" },
            confidence = confidence,
            needsReview = root.optBoolean("needsReview", confidence == "LOW"),
            warning = root.optString("warning").trim()
        )
    }

    private fun parseRefactorResult(content: String): AiRefactorResult {
        val root = JSONObject(extractJsonObject(content))
        val questionsJson = root.optJSONArray("questions") ?: root.optJSONArray("items") ?: JSONArray()
        val notesJson = root.optJSONArray("notes") ?: JSONArray()
        val notes = (0 until notesJson.length())
            .map { notesJson.optString(it).trim() }
            .filter { it.isNotBlank() }
        val questions = (0 until questionsJson.length()).mapNotNull { index ->
            val item = questionsJson.optJSONObject(index) ?: return@mapNotNull null
            val questionText = item.optString("question").trim()
            if (questionText.isBlank()) return@mapNotNull null
            val optionsJson = item.optJSONArray("options") ?: JSONArray()
            val options = (0 until optionsJson.length()).mapNotNull { optionIndex ->
                val option = optionsJson.optJSONObject(optionIndex) ?: return@mapNotNull null
                val key = option.optString("key").trim().uppercase()
                val text = option.optString("text").trim()
                if (key.isBlank() && text.isBlank()) null else Option(key, text)
            }
            val answerJson = item.optJSONArray("answer")
            val answers = if (answerJson != null) {
                (0 until answerJson.length()).map { answerJson.optString(it).trim().uppercase() }.filter { it.isNotBlank() }
            } else {
                item.optString("answer").split(Regex("[,，、\\s]+"))
                    .map { it.trim().uppercase() }
                    .filter { it.isNotBlank() }
            }
            Question(
                number = item.optString("number", (index + 1).toString()).trim().ifBlank { (index + 1).toString() },
                type = parseQuestionType(item.optString("type")),
                question = questionText,
                options = options,
                answer = answers,
                analysis = item.optString("analysis").trim(),
                category = item.optString("category").trim()
            )
        }
        val cleanedText = root.optString("cleanedText").nullIfBlankOrLiteralNull()
        val cleanedAnswerText = root.optString("cleanedAnswerText").nullIfBlankOrLiteralNull()
        val mode = root.optString("mode").trim().ifBlank {
            if (!cleanedText.isNullOrBlank()) "clean_text" else "direct_questions"
        }
        return AiRefactorResult(
            mode = mode,
            cleanedText = cleanedText,
            cleanedAnswerText = cleanedAnswerText,
            questions = questions,
            notes = notes
        )
    }

    private fun parseQuestionType(value: String): QuestionType {
        return when (value.trim().lowercase()) {
            "single", "single_choice", "choice", "单选", "单选题" -> QuestionType.SINGLE
            "multiple", "multiple_choice", "multi", "多选", "多选题" -> QuestionType.MULTIPLE
            "judge", "true_false", "判断", "判断题" -> QuestionType.JUDGE
            "blank", "fill_blank", "填空", "填空题" -> QuestionType.BLANK
            "short", "essay", "subjective", "简答", "简答题", "问答", "问答题", "面试题" -> QuestionType.SHORT
            else -> QuestionType.SHORT
        }
    }

    private fun String.nullIfBlankOrLiteralNull(): String? {
        val clean = trim()
        return clean.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    private fun extractJsonObject(content: String): String {
        val clean = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw IllegalStateException("AI 返回内容不是有效 JSON。")
        }
        return clean.substring(start, end + 1)
    }

    private fun reviewOutputContract(): JSONObject {
        return JSONObject()
            .put(
                "items",
                JSONArray().put(
                    JSONObject()
                        .put("questionId", "题目ID")
                        .put("status", "ok / warning / error")
                        .put("issueTypes", JSONArray().put("answer_mismatch"))
                        .put("reason", "发现问题的原因")
                        .put("suggestion", "建议如何人工核对或修改")
                        .put("riskLevel", "auto_safe / needs_confirm / hard_error")
                        .put("canApply", true)
                        .put("suggestedType", "single / multiple / judge / blank / short / null")
                        .put("suggestedAnswer", JSONArray().put("A"))
                        .put("suggestedQuestion", "建议题干；无建议时返回 null")
                        .put(
                            "suggestedOptions",
                            JSONArray().put(JSONObject().put("key", "A").put("text", "建议选项文本"))
                        )
                        .put("suggestedAnalysis", "建议解析；无建议时返回 null")
                        .put("needHumanReview", true)
                        .put("confidence", 0.82)
                )
            )
    }

    private fun analysisOutputContract(): JSONObject {
        return JSONObject()
            .put(
                "items",
                JSONArray().put(
                    JSONObject()
                        .put("questionId", "题目ID")
                        .put("analysis", "生成的解析内容；简答/面试题可返回参考作答或答题思路")
                        .put("needHumanReview", false)
                        .put("confidence", 0.86)
                )
            )
    }

    private fun singleQuestionAnalysisOutputContract(): JSONObject {
        return JSONObject()
            .put("questionId", "题目ID")
            .put("suggestedAnswer", "AI 独立判断的参考答案")
            .put("matchesLocalAnswer", true)
            .put("analysis", "AI 解析、排除依据或简答题参考要点")
            .put("confidence", "HIGH / MEDIUM / LOW")
            .put("needsReview", false)
            .put("warning", "不确定或疑似题库答案异常时填写；否则为空")
    }

    private fun refactorOutputContract(): JSONObject {
        return JSONObject()
            .put("mode", "clean_text / direct_questions")
            .put("cleanedText", "优先返回清洗后的标准题库文本；direct_questions 模式可为空")
            .put("cleanedAnswerText", "如仍需双文件解析，可返回清洗后的答案文本；否则为空")
            .put(
                "questions",
                JSONArray().put(
                    JSONObject()
                        .put("number", "1")
                        .put("type", "single / multiple / judge / blank / short")
                        .put("question", "题干")
                        .put("options", JSONArray().put(JSONObject().put("key", "A").put("text", "选项文本")))
                        .put("answer", JSONArray().put("A"))
                        .put("analysis", "解析；没有可靠来源时可为空")
                        .put("category", "分区或来源；没有可为空")
                )
            )
            .put("notes", JSONArray().put("重构说明和需要人工确认的点"))
    }
}
