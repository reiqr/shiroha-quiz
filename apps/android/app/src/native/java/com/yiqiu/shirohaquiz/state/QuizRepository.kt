package com.yiqiu.shirohaquiz.state

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionImage
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.util.LauncherIconSwitcher
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class QuizBank(
    val id: String,
    val name: String,
    val questions: List<Question>
)

data class ExamSummary(
    val total: Int,
    val answered: Int,
    val correct: Int,
    val durationSeconds: Int,
    val remainingSeconds: Int,
    val autoSubmitted: Boolean,
    val totalScore: Double = 0.0,
    val earnedScore: Double = 0.0
)

data class WrongQuestionEntry(
    val bankId: String,
    val bankName: String,
    val question: Question,
    val lastAnswer: List<String>,
    val source: String,
    val timestamp: Long,
    val wrongCount: Int = 1,
    val rightCount: Int = 0,
    val reviewRightCount: Int = 0,
    val streakCorrectCount: Int = 0,
    val lastWrongAt: Long = timestamp,
    val lastCorrectAt: Long? = null,
    val status: String = WrongStatus.REVIEWING.label,
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long? = null,
    val reviewLevel: Int = 0
)

data class SlashedQuestionEntry(
    val bankId: String,
    val questionKey: String,
    val slashedAt: Long
)

data class FavoriteQuestionEntry(
    val bankId: String,
    val bankName: String,
    val question: Question,
    val favoritedAt: Long
)

enum class WrongStatus(val label: String) {
    REVIEWING("复习中"),
    NOT_MASTERED("未掌握"),
    MASTERED("已掌握");

    companion object {
        fun normalize(value: String?): String {
            return values().firstOrNull { it.label == value }?.label ?: NOT_MASTERED.label
        }
    }
}

data class WrongBookSmartReviewSummary(
    val total: Int,
    val notMastered: Int,
    val masteredReview: Int
)

data class StudyQuestionResult(
    val question: Question,
    val userAnswer: List<String>,
    val correct: Boolean,
    val answerText: String,
    val earnedScore: Double? = null,
    val maxScore: Double? = null
)

data class StudyRecord(
    val id: String,
    val bankId: String?,
    val bankName: String,
    val source: String,
    val title: String,
    val total: Int,
    val correct: Int,
    val timestamp: Long,
    val durationSeconds: Int? = null,
    val autoSubmitted: Boolean = false,
    val startedAt: Long? = null,
    val earnedScore: Double? = null,
    val totalScore: Double? = null,
    val questionResults: List<StudyQuestionResult> = emptyList()
)

data class QuestionCheckResult(
    val question: Question,
    val userAnswer: List<String>,
    val correct: Boolean,
    val answerText: String
)

object QuizRepository {
    const val PRACTICE_MODE_INSTANT = "instant_feedback"
    const val PRACTICE_MODE_BATCH = "batch_review"

    private const val PREFS_NAME = "shiroha_quiz_native"
    private const val KEY_BANKS = "banks"
    private const val KEY_ACTIVE_BANK_ID = "active_bank_id"
    private const val KEY_WRONG_BOOK = "wrong_book"
    private const val KEY_SLASHED_QUESTIONS = "slashed_questions"
    private const val KEY_FAVORITE_QUESTIONS = "favorite_questions"
    private const val KEY_STUDY_RECORDS = "study_records"
    private const val KEY_PRACTICE_NEXT_REQUIRES_RESULT = "practice_next_requires_result"
    private const val KEY_REMEMBER_PRACTICE_SETTINGS = "remember_practice_settings"
    private const val KEY_SWIPE_NAVIGATION_ENABLED = "swipe_navigation_enabled"
    private const val KEY_PRACTICE_AUTO_SUBMIT_ENABLED = "practice_auto_submit_enabled"
    private const val KEY_PRACTICE_AUTO_NEXT_ENABLED = "practice_auto_next_enabled"
    private const val KEY_PRACTICE_INLINE_ANSWER_SETTINGS_ENABLED = "practice_inline_answer_settings_enabled"
    private const val KEY_PRACTICE_RECITE_MODE_ENABLED = "practice_recite_mode_enabled"
    private const val KEY_PRACTICE_SLASH_ENABLED = "practice_slash_enabled"
    private const val KEY_PRACTICE_QUICK_EDIT_ENABLED = "practice_quick_edit_enabled"
    private const val KEY_WRONG_BOOK_SMART_REVIEW_ENABLED = "wrong_book_smart_review_enabled"
    private const val KEY_PRACTICE_PREFERRED_COUNT_MODE = "practice_preferred_count_mode"
    private const val KEY_PRACTICE_PREFERRED_CUSTOM_COUNT = "practice_preferred_custom_count"
    private const val KEY_PRACTICE_PREFERRED_ORDER_MODE = "practice_preferred_order_mode"
    private const val KEY_PRACTICE_PREFERRED_TYPE_NAMES = "practice_preferred_type_names"
    private const val KEY_PRACTICE_PREFERRED_MODE = "practice_preferred_mode"
    private const val KEY_PRACTICE_PREFERRED_BATCH_SIZE_MODE = "practice_preferred_batch_size_mode"
    private const val KEY_PRACTICE_PREFERRED_BATCH_CUSTOM_SIZE = "practice_preferred_batch_custom_size"
    private const val KEY_REMEMBER_EXAM_SETTINGS = "remember_exam_settings"
    private const val KEY_EXAM_PREFERRED_COUNT_MODE = "exam_preferred_count_mode"
    private const val KEY_EXAM_PREFERRED_CUSTOM_COUNT = "exam_preferred_custom_count"
    private const val KEY_EXAM_PREFERRED_DURATION_MINUTES = "exam_preferred_duration_minutes"
    private const val KEY_EXAM_PREFERRED_GROUP_MODE = "exam_preferred_group_mode"
    private const val KEY_EXAM_PREFERRED_TYPE_COUNTS = "exam_preferred_type_counts"
    private const val KEY_EXAM_PREFERRED_TYPE_SCORES = "exam_preferred_type_scores"
    private const val KEY_STARTUP_SPLASH_ENABLED = "startup_splash_enabled"
    private const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled"
    private const val KEY_SHIROHA_MODE_ENABLED = "shiroha_mode_enabled"
    private const val KEY_QUESTION_FONT_SIZE_MODE = "question_font_size_mode"
    private const val KEY_OPTION_FONT_SIZE_MODE = "option_font_size_mode"
    private const val KEY_COMPACT_OPTIONS_ENABLED = "compact_options_enabled"
    private const val KEY_AI_PROVIDER = "ai_provider"
    private const val KEY_AI_API_BASE_URL = "ai_api_base_url"
    private const val KEY_AI_API_KEY = "ai_api_key"
    private const val KEY_AI_MODEL_NAME = "ai_model_name"
    private const val KEY_AI_REFACTOR_ENABLED = "ai_refactor_enabled"
    private const val KEY_AI_REVIEW_ENABLED = "ai_review_enabled"
    private const val KEY_AI_ANALYSIS_ENABLED = "ai_analysis_enabled"
    private const val KEY_AI_ONLY_ANOMALY = "ai_only_anomaly"
    private const val KEY_AI_REQUIRE_CONFIRM = "ai_require_confirm"
    private const val KEY_AI_MAX_QUESTIONS = "ai_max_questions"
    private const val KEY_AI_TIMEOUT_SECONDS = "ai_timeout_seconds"
    private const val KEY_AI_REFACTOR_MAX_CHARS = "ai_refactor_max_chars"
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    val banks = mutableStateListOf<QuizBank>()
    val wrongBook = mutableStateListOf<WrongQuestionEntry>()
    val slashedQuestions = mutableStateListOf<SlashedQuestionEntry>()
    val favoriteQuestions = mutableStateListOf<FavoriteQuestionEntry>()
    val studyRecords = mutableStateListOf<StudyRecord>()

    var activeBankId by mutableStateOf<String?>(null)
    var practiceQuestions by mutableStateOf<List<Question>>(emptyList())
        private set
    var practiceSourceLabel by mutableStateOf("当前题库")
        private set
    var practiceIndex by mutableStateOf(0)
    var selectedAnswer by mutableStateOf<List<String>>(emptyList())
    var practiceLastResult by mutableStateOf<QuestionCheckResult?>(null)
        private set
    var practiceMode by mutableStateOf(PRACTICE_MODE_INSTANT)
        private set
    var practiceBatchSubmitted by mutableStateOf(false)
        private set
    var practiceBatchGroupSize by mutableStateOf(10)
        private set
    var practiceBatchGroupStartIndex by mutableStateOf(0)
        private set
    val practiceDraftAnswers = mutableStateMapOf<String, List<String>>()
    var practiceNextRequiresResult by mutableStateOf(false)
        private set
    var rememberPracticeSettingsEnabled by mutableStateOf(true)
        private set
    var swipeNavigationEnabled by mutableStateOf(true)
        private set
    var practiceAutoSubmitEnabled by mutableStateOf(true)
        private set
    var practiceAutoNextEnabled by mutableStateOf(false)
        private set
    var practiceInlineAnswerSettingsEnabled by mutableStateOf(false)
        private set
    var practiceReciteModeEnabled by mutableStateOf(false)
        private set
    var practiceSlashEnabled by mutableStateOf(false)
        private set
    var practiceQuickEditEnabled by mutableStateOf(false)
        private set
    var wrongBookSmartReviewEnabled by mutableStateOf(false)
        private set
    var preferredPracticeQuestionCountMode by mutableStateOf("custom")
        private set
    var preferredPracticeCustomQuestionCount by mutableStateOf(20)
        private set
    var preferredPracticeOrderMode by mutableStateOf("random")
        private set
    private var preferredPracticeTypeNames by mutableStateOf("")
    var preferredPracticeMode by mutableStateOf(PRACTICE_MODE_INSTANT)
        private set
    var preferredPracticeBatchSizeMode by mutableStateOf("10")
        private set
    var preferredPracticeBatchCustomSize by mutableStateOf(10)
        private set
    var rememberExamSettingsEnabled by mutableStateOf(true)
        private set
    var preferredExamQuestionCountMode by mutableStateOf("100")
        private set
    var preferredExamCustomQuestionCount by mutableStateOf(20)
        private set
    var preferredExamDurationMinutes by mutableStateOf(30)
        private set
    var preferredExamGroupMode by mutableStateOf("random")
        private set
    private var preferredExamTypeCountsText by mutableStateOf("")
    private var preferredExamTypeScoresText by mutableStateOf("")
    var startupSplashEnabled by mutableStateOf(true)
        private set
    var darkThemeEnabled by mutableStateOf(false)
        private set
    var shirohaModeEnabled by mutableStateOf(false)
        private set
    var questionFontSizeMode by mutableStateOf("standard")
        private set
    var optionFontSizeMode by mutableStateOf("standard")
        private set
    var compactOptionsEnabled by mutableStateOf(false)
        private set
    var aiProvider by mutableStateOf("DeepSeek")
        private set
    var aiApiBaseUrl by mutableStateOf("")
        private set
    var aiApiKey by mutableStateOf("")
        private set
    var aiModelName by mutableStateOf("")
        private set
    var aiRefactorEnabled by mutableStateOf(false)
        private set
    var aiReviewEnabled by mutableStateOf(false)
        private set
    var aiAnalysisEnabled by mutableStateOf(false)
        private set
    var aiOnlyAnomaly by mutableStateOf(true)
        private set
    var aiRequireConfirm by mutableStateOf(true)
        private set
    var aiMaxQuestions by mutableStateOf(20)
        private set
    var aiTimeoutSeconds by mutableStateOf(60)
        private set
    var aiRefactorMaxChars by mutableStateOf(30000)
        private set
    val practiceSessionResults = mutableStateMapOf<String, Boolean>()
    val practiceAnswerResults = mutableStateMapOf<String, StudyQuestionResult>()
    private val practiceQuestionBankIds = mutableStateMapOf<String, String>()
    private var practiceStartedAt by mutableStateOf<Long?>(null)

    private var initialized by mutableStateOf(false)
    private var appContext: Context? = null

    var examQuestions by mutableStateOf<List<Question>>(emptyList())
        private set
    var examIndex by mutableStateOf(0)
        private set
    var examDurationSeconds by mutableStateOf(0)
        private set
    var examRemainingSeconds by mutableStateOf(0)
        private set
    var examFinished by mutableStateOf(false)
        private set
    var examAutoSubmitted by mutableStateOf(false)
        private set
    val examAnswers = mutableStateMapOf<String, List<String>>()
    var examTypeScores by mutableStateOf(defaultExamTypeScores())
        private set

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val banksJson = prefs.getString(KEY_BANKS, null)
        val restoredBanks = runCatching { parseBanksJson(banksJson) }.getOrDefault(emptyList())
        val restoredWrongBook = runCatching {
            parseWrongBookJson(prefs.getString(KEY_WRONG_BOOK, null))
        }.getOrDefault(emptyList())
        val restoredSlashedQuestions = runCatching {
            parseSlashedQuestionsJson(prefs.getString(KEY_SLASHED_QUESTIONS, null))
        }.getOrDefault(emptyList())
        val restoredFavoriteQuestions = runCatching {
            parseFavoriteQuestionsJson(prefs.getString(KEY_FAVORITE_QUESTIONS, null))
        }.getOrDefault(emptyList())
        val restoredStudyRecords = runCatching {
            parseStudyRecordsJson(prefs.getString(KEY_STUDY_RECORDS, null))
        }.getOrDefault(emptyList())

        banks.clear()
        wrongBook.clear()
        slashedQuestions.clear()
        favoriteQuestions.clear()
        studyRecords.clear()

        val sanitizedRestoredBanks = restoredBanks
            .map(::sanitizeBank)
            .filterNot { bank -> bank.id == "demo-bank" || (bank.name == "示例题库" && bank.questions.isEmpty()) }
        banks.addAll(sanitizedRestoredBanks)
        activeBankId = prefs.getString(KEY_ACTIVE_BANK_ID, sanitizedRestoredBanks.firstOrNull()?.id)
            ?.takeIf { id -> sanitizedRestoredBanks.any { it.id == id } }
            ?: sanitizedRestoredBanks.firstOrNull()?.id
        practiceNextRequiresResult = prefs.getBoolean(KEY_PRACTICE_NEXT_REQUIRES_RESULT, false)
        rememberPracticeSettingsEnabled = prefs.getBoolean(KEY_REMEMBER_PRACTICE_SETTINGS, true)
        swipeNavigationEnabled = prefs.getBoolean(KEY_SWIPE_NAVIGATION_ENABLED, true)
        practiceAutoSubmitEnabled = prefs.getBoolean(KEY_PRACTICE_AUTO_SUBMIT_ENABLED, true)
        practiceAutoNextEnabled = prefs.getBoolean(KEY_PRACTICE_AUTO_NEXT_ENABLED, false)
        practiceInlineAnswerSettingsEnabled = prefs.getBoolean(KEY_PRACTICE_INLINE_ANSWER_SETTINGS_ENABLED, false)
        practiceReciteModeEnabled = prefs.getBoolean(KEY_PRACTICE_RECITE_MODE_ENABLED, false)
        practiceSlashEnabled = prefs.getBoolean(KEY_PRACTICE_SLASH_ENABLED, false)
        practiceQuickEditEnabled = prefs.getBoolean(KEY_PRACTICE_QUICK_EDIT_ENABLED, false)
        wrongBookSmartReviewEnabled = prefs.getBoolean(KEY_WRONG_BOOK_SMART_REVIEW_ENABLED, false)
        preferredPracticeQuestionCountMode = normalizePracticeCountMode(
            prefs.getString(KEY_PRACTICE_PREFERRED_COUNT_MODE, "custom") ?: "custom"
        )
        preferredPracticeCustomQuestionCount = prefs.getInt(KEY_PRACTICE_PREFERRED_CUSTOM_COUNT, 20).coerceAtLeast(1)
        preferredPracticeOrderMode = normalizePracticeOrderMode(
            prefs.getString(KEY_PRACTICE_PREFERRED_ORDER_MODE, "random") ?: "random"
        )
        preferredPracticeTypeNames = prefs.getString(KEY_PRACTICE_PREFERRED_TYPE_NAMES, "") ?: ""
        preferredPracticeMode = normalizePracticeMode(
            prefs.getString(KEY_PRACTICE_PREFERRED_MODE, PRACTICE_MODE_INSTANT) ?: PRACTICE_MODE_INSTANT
        )
        preferredPracticeBatchSizeMode = normalizePracticeBatchSizeMode(
            prefs.getString(KEY_PRACTICE_PREFERRED_BATCH_SIZE_MODE, "10") ?: "10"
        )
        preferredPracticeBatchCustomSize = prefs.getInt(KEY_PRACTICE_PREFERRED_BATCH_CUSTOM_SIZE, 10).coerceAtLeast(1)
        rememberExamSettingsEnabled = prefs.getBoolean(KEY_REMEMBER_EXAM_SETTINGS, true)
        preferredExamQuestionCountMode = normalizeExamCountMode(
            prefs.getString(KEY_EXAM_PREFERRED_COUNT_MODE, "100") ?: "100"
        )
        preferredExamCustomQuestionCount = prefs.getInt(KEY_EXAM_PREFERRED_CUSTOM_COUNT, 20).coerceAtLeast(1)
        preferredExamDurationMinutes = prefs.getInt(KEY_EXAM_PREFERRED_DURATION_MINUTES, 30).coerceIn(1, 999)
        preferredExamGroupMode = normalizeExamGroupMode(
            prefs.getString(KEY_EXAM_PREFERRED_GROUP_MODE, "random") ?: "random"
        )
        preferredExamTypeCountsText = prefs.getString(KEY_EXAM_PREFERRED_TYPE_COUNTS, "") ?: ""
        preferredExamTypeScoresText = prefs.getString(KEY_EXAM_PREFERRED_TYPE_SCORES, "") ?: ""
        startupSplashEnabled = prefs.getBoolean(KEY_STARTUP_SPLASH_ENABLED, true)
        darkThemeEnabled = prefs.getBoolean(KEY_DARK_THEME_ENABLED, false)
        shirohaModeEnabled = prefs.getBoolean(KEY_SHIROHA_MODE_ENABLED, false)
        questionFontSizeMode = normalizeReadingSizeMode(prefs.getString(KEY_QUESTION_FONT_SIZE_MODE, "standard"))
        optionFontSizeMode = normalizeReadingSizeMode(prefs.getString(KEY_OPTION_FONT_SIZE_MODE, "standard"))
        compactOptionsEnabled = prefs.getBoolean(KEY_COMPACT_OPTIONS_ENABLED, false)
        aiProvider = prefs.getString(KEY_AI_PROVIDER, "DeepSeek") ?: "DeepSeek"
        aiApiBaseUrl = prefs.getString(KEY_AI_API_BASE_URL, "") ?: ""
        aiApiKey = prefs.getString(KEY_AI_API_KEY, "") ?: ""
        aiModelName = prefs.getString(KEY_AI_MODEL_NAME, "") ?: ""
        aiRefactorEnabled = prefs.getBoolean(KEY_AI_REFACTOR_ENABLED, false)
        aiReviewEnabled = prefs.getBoolean(KEY_AI_REVIEW_ENABLED, false)
        aiAnalysisEnabled = prefs.getBoolean(KEY_AI_ANALYSIS_ENABLED, false)
        aiOnlyAnomaly = prefs.getBoolean(KEY_AI_ONLY_ANOMALY, true)
        aiRequireConfirm = prefs.getBoolean(KEY_AI_REQUIRE_CONFIRM, true)
        aiMaxQuestions = prefs.getInt(KEY_AI_MAX_QUESTIONS, 20).coerceIn(5, 100)
        aiTimeoutSeconds = prefs.getInt(KEY_AI_TIMEOUT_SECONDS, 60).coerceIn(15, 180)
        aiRefactorMaxChars = prefs.getInt(KEY_AI_REFACTOR_MAX_CHARS, 30000).coerceIn(5000, 80000)

        wrongBook.addAll(restoredWrongBook.map(::sanitizeWrongEntry))
        slashedQuestions.addAll(sanitizeSlashedEntries(restoredSlashedQuestions, sanitizedRestoredBanks))
        favoriteQuestions.addAll(sanitizeFavoriteEntries(restoredFavoriteQuestions, sanitizedRestoredBanks))
        studyRecords.addAll(restoredStudyRecords)
        if (sanitizedRestoredBanks != restoredBanks) persist()
    }

    fun importBank(context: Context, name: String, questions: List<Question>) {
        appContext = context.applicationContext
        val bank = QuizBank(
            id = "bank_${System.currentTimeMillis()}",
            name = name.ifBlank { "导入题库" },
            questions = questions.map(::sanitizeQuestion)
        )
        banks += bank
        activeBankId = bank.id
        resetPracticeState()
        resetExam()
        persist()
    }

    fun setActiveBank(context: Context, bankId: String) {
        appContext = context.applicationContext
        activeBankId = bankId
        resetPracticeState()
        resetExam()
        persist()
    }

    fun deleteBank(context: Context, bankId: String) {
        appContext = context.applicationContext
        val removingActive = activeBankId == bankId
        banks.removeAll { it.id == bankId }
        wrongBook.removeAll { it.bankId == bankId }
        slashedQuestions.removeAll { it.bankId == bankId }
        favoriteQuestions.removeAll { it.bankId == bankId }
        studyRecords.removeAll { it.bankId == bankId }

        if (removingActive || banks.none { it.id == activeBankId }) {
            activeBankId = banks.firstOrNull()?.id
        }
        resetPracticeState()
        resetExam()
        persist()
    }


    fun renameBank(context: Context, bankId: String, newName: String): Boolean {
        appContext = context.applicationContext
        val index = banks.indexOfFirst { it.id == bankId }
        if (index < 0) return false
        val cleanName = uniqueBankNameForRename(newName.trim().ifBlank { "未命名题库" }, bankId)
        val current = banks[index]
        banks[index] = current.copy(name = cleanName)

        for (i in wrongBook.indices) {
            val entry = wrongBook[i]
            if (entry.bankId == bankId) {
                wrongBook[i] = entry.copy(bankName = cleanName)
            }
        }
        for (i in studyRecords.indices) {
            val record = studyRecords[i]
            if (record.bankId == bankId) {
                studyRecords[i] = record.copy(bankName = cleanName)
            }
        }
        persist()
        return true
    }

    fun updateQuestion(context: Context, bankId: String, updatedQuestion: Question): Boolean {
        appContext = context.applicationContext
        val bankIndex = banks.indexOfFirst { it.id == bankId }
        if (bankIndex < 0) return false
        val bank = banks[bankIndex]
        val questionIndex = bank.questions.indexOfFirst { it.id == updatedQuestion.id }
        if (questionIndex < 0) return false

        val cleanQuestion = sanitizeQuestion(updatedQuestion)
        val updatedQuestions = bank.questions.toMutableList().also { it[questionIndex] = cleanQuestion }
        banks[bankIndex] = bank.copy(questions = updatedQuestions)

        for (i in wrongBook.indices) {
            val entry = wrongBook[i]
            if (entry.bankId == bankId && entry.question.id == cleanQuestion.id) {
                wrongBook[i] = entry.copy(
                    bankName = banks[bankIndex].name,
                    question = cleanQuestion
                )
            }
        }

        if (activeBankId == bankId) {
            resetPracticeState()
            resetExam()
        }
        persist()
        return true
    }

    fun replaceBankQuestions(context: Context, bankId: String, questions: List<Question>): Boolean {
        appContext = context.applicationContext
        val bankIndex = banks.indexOfFirst { it.id == bankId }
        if (bankIndex < 0) return false

        val bank = banks[bankIndex]
        val cleanQuestions = questions.map(::sanitizeQuestion)
        banks[bankIndex] = bank.copy(questions = cleanQuestions)

        val questionById = cleanQuestions.associateBy { it.id }
        for (i in wrongBook.indices.reversed()) {
            val entry = wrongBook[i]
            if (entry.bankId == bankId) {
                val updatedQuestion = questionById[entry.question.id]
                if (updatedQuestion == null) {
                    wrongBook.removeAt(i)
                } else {
                    wrongBook[i] = entry.copy(
                        bankName = banks[bankIndex].name,
                        question = updatedQuestion
                    )
                }
            }
        }
        val validQuestionKeys = cleanQuestions.map(::questionKey).toSet()
        slashedQuestions.removeAll { it.bankId == bankId && it.questionKey !in validQuestionKeys }

        if (activeBankId == bankId) {
            resetPracticeState()
            resetExam()
        }
        persist()
        return true
    }

    fun deleteQuestion(context: Context, bankId: String, questionId: String): Boolean {
        appContext = context.applicationContext
        val bank = banks.firstOrNull { it.id == bankId } ?: return false
        val updatedQuestions = bank.questions.filterNot { it.id == questionId }
        if (updatedQuestions.size == bank.questions.size) return false
        return replaceBankQuestions(context, bankId, updatedQuestions)
    }

    fun activeBank(): QuizBank? = banks.firstOrNull { it.id == activeBankId } ?: banks.firstOrNull()

    fun currentPracticeQuestion(): Question? {
        val questions = activePracticeQuestions()
        if (questions.isEmpty()) return null
        val minIndex = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchStartIndex() else 0
        val maxIndex = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchEndIndex() else questions.lastIndex
        val safeIndex = practiceIndex.coerceIn(minIndex, maxIndex)
        if (safeIndex != practiceIndex) practiceIndex = safeIndex
        return questions[safeIndex]
    }

    fun activePracticeQuestions(): List<Question> {
        return practiceQuestions.ifEmpty { activePracticePoolQuestions(activeBank()) }
    }

    fun activePracticePoolQuestions(bank: QuizBank? = activeBank()): List<Question> {
        val currentBank = bank ?: return emptyList()
        return currentBank.questions.filterNot { isQuestionSlashed(currentBank.id, it) }
    }

    fun practiceAvailableQuestionCount(bank: QuizBank? = activeBank()): Int = activePracticePoolQuestions(bank).size

    fun startPracticeSession(
        questionCount: Int,
        allowedTypes: Set<QuestionType>,
        sourceQuestions: List<Question>? = null,
        sourceLabel: String = "当前题库",
        randomize: Boolean = true,
        practiceMode: String = PRACTICE_MODE_INSTANT,
        batchGroupSize: Int = preferredPracticeBatchGroupSize(),
        sourceBankIds: Map<String, String>? = null
    ): Boolean {
        val selectedTypes = allowedTypes.ifEmpty { objectiveQuestionTypes() }
        val bank = activeBank()
        val rawSource = sourceQuestions ?: activePracticePoolQuestions(bank)
        val source = rawSource.filter { it.type in selectedTypes }
        if (source.isEmpty()) return false
        val count = questionCount.coerceIn(1, source.size)
        val selectedQuestions = if (randomize) source.shuffled().take(count) else source.take(count)
        practiceQuestions = selectedQuestions
        practiceSourceLabel = sourceLabel
        practiceIndex = 0
        selectedAnswer = emptyList()
        practiceLastResult = null
        this.practiceMode = normalizePracticeMode(practiceMode)
        practiceBatchSubmitted = false
        practiceBatchGroupSize = batchGroupSize.coerceIn(1, count)
        practiceBatchGroupStartIndex = 0
        practiceSessionResults.clear()
        practiceAnswerResults.clear()
        practiceDraftAnswers.clear()
        practiceQuestionBankIds.clear()
        selectedQuestions.forEach { question ->
            val bankId = sourceBankIds?.get(question.id) ?: bank?.id
            if (!bankId.isNullOrBlank()) practiceQuestionBankIds[question.id] = bankId
        }
        practiceStartedAt = System.currentTimeMillis()
        return true
    }

    fun startWrongBookPractice(
        entries: List<WrongQuestionEntry> = reviewDueWrongEntries(),
        includeMastered: Boolean = false,
        sourceLabel: String = "错题本"
    ): Boolean {
        val selectedEntries = entries
            .filter { includeMastered || it.status != WrongStatus.MASTERED.label }
            .filterNot { isQuestionSlashed(it.bankId, it.question) }
        val questions = selectedEntries
            .map { it.question }
            .distinctBy { it.id }
        if (questions.isEmpty()) return false
        val sourceBankIds = selectedEntries.associate { it.question.id to it.bankId }
        return startPracticeSession(
            questionCount = questions.size,
            allowedTypes = QuestionType.values().toSet(),
            sourceQuestions = questions,
            sourceLabel = sourceLabel,
            randomize = false,
            sourceBankIds = sourceBankIds
        )
    }

    fun startTodayWrongBookReview(): Boolean {
        val entries = todayWrongBookSmartReviewEntries()
        if (entries.isEmpty()) return false
        return startWrongBookPractice(
            entries = entries,
            includeMastered = true,
            sourceLabel = "今日复习"
        )
    }

    fun updateCurrentPracticeQuestion(updated: Question): Boolean {
        val current = currentPracticeQuestion() ?: return false
        val targetBank = bankForPracticeQuestion(current) ?: return false
        val bankIndex = banks.indexOfFirst { it.id == targetBank.id }
        if (bankIndex < 0) return false
        val questionIndex = banks[bankIndex].questions.indexOfFirst { it.id == current.id }
        if (questionIndex < 0) return false

        val sanitizedUpdated = sanitizeQuestion(
            updated.copy(
                id = current.id,
                number = current.number,
                type = current.type,
                category = current.category,
                images = current.images,
                score = current.score
            )
        )

        val updatedQuestions = banks[bankIndex].questions.toMutableList()
        updatedQuestions[questionIndex] = sanitizedUpdated
        banks[bankIndex] = banks[bankIndex].copy(questions = updatedQuestions)

        practiceQuestions = practiceQuestions.map { question ->
            if (question.id == current.id) sanitizedUpdated else question
        }
        val mappedBankId = practiceQuestionBankIds[current.id] ?: targetBank.id
        for (index in wrongBook.indices) {
            val entry = wrongBook[index]
            if (entry.bankId == mappedBankId && entry.question.id == current.id) {
                wrongBook[index] = entry.copy(
                    bankName = banks[bankIndex].name,
                    question = sanitizedUpdated
                )
            }
        }
        for (index in favoriteQuestions.indices) {
            val entry = favoriteQuestions[index]
            if (entry.bankId == mappedBankId && entry.question.id == current.id) {
                favoriteQuestions[index] = entry.copy(
                    bankName = banks[bankIndex].name,
                    question = sanitizedUpdated
                )
            }
        }

        practiceSessionResults.remove(current.id)
        practiceAnswerResults.remove(current.id)
        practiceDraftAnswers.remove(current.id)
        if (practiceLastResult?.question?.id == current.id) practiceLastResult = null
        selectedAnswer = emptyList()
        persist()
        return true
    }

    fun endPracticeSession() {
        finishPracticeSessionIfNeeded()
        resetPracticeState()
    }

    fun nextQuestion() {
        val questions = activePracticeQuestions()
        if (questions.isEmpty()) return
        val maxIndex = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchEndIndex() else questions.lastIndex
        practiceIndex = (practiceIndex + 1).coerceAtMost(maxIndex)
        restorePracticeSelectionForCurrentQuestion()
    }

    fun previousQuestion() {
        val questions = activePracticeQuestions()
        if (questions.isEmpty()) return
        val minIndex = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchStartIndex() else 0
        practiceIndex = (practiceIndex - 1).coerceAtLeast(minIndex)
        restorePracticeSelectionForCurrentQuestion()
    }

    fun openWrongQuestion(entry: WrongQuestionEntry) {
        val bank = banks.firstOrNull { it.id == entry.bankId } ?: return
        val index = bank.questions.indexOfFirst { it.id == entry.question.id }
        if (index >= 0) {
            activeBankId = bank.id
            practiceQuestions = listOf(bank.questions[index])
            practiceSourceLabel = "错题本"
            practiceIndex = 0
            selectedAnswer = emptyList()
            practiceLastResult = null
            practiceMode = PRACTICE_MODE_INSTANT
            practiceBatchSubmitted = false
            practiceBatchGroupSize = 10
            practiceBatchGroupStartIndex = 0
            practiceSessionResults.clear()
            practiceAnswerResults.clear()
            practiceDraftAnswers.clear()
            practiceQuestionBankIds.clear()
            practiceQuestionBankIds[entry.question.id] = entry.bankId
            practiceStartedAt = System.currentTimeMillis()
        }
    }

    fun removeWrongQuestion(entry: WrongQuestionEntry) {
        wrongBook.removeAll { it.bankId == entry.bankId && it.question.id == entry.question.id }
        persist()
    }

    fun markWrongQuestionMastered(entry: WrongQuestionEntry, mastered: Boolean = true) {
        val index = wrongBook.indexOfFirst { it.bankId == entry.bankId && it.question.id == entry.question.id }
        if (index < 0) return
        val now = System.currentTimeMillis()
        val current = wrongBook[index]
        wrongBook[index] = if (mastered) {
            val nextReviewLevel = current.reviewLevel.coerceAtLeast(2)
            current.copy(
                status = WrongStatus.MASTERED.label,
                streakCorrectCount = current.streakCorrectCount.coerceAtLeast(2),
                lastCorrectAt = now,
                lastReviewedAt = now,
                reviewLevel = nextReviewLevel,
                nextReviewAt = nextReviewTimeForLevel(now, nextReviewLevel)
            )
        } else {
            current.copy(
                status = WrongStatus.NOT_MASTERED.label,
                streakCorrectCount = 0,
                lastReviewedAt = now,
                reviewLevel = 0,
                nextReviewAt = startOfDay(now)
            )
        }
        persist()
    }

    fun reviewDueWrongEntries(): List<WrongQuestionEntry> {
        return wrongBook
            .filter { it.status != WrongStatus.MASTERED.label }
            .filterNot { isQuestionSlashed(it.bankId, it.question) }
    }

    fun todayWrongBookSmartReviewEntries(now: Long = System.currentTimeMillis()): List<WrongQuestionEntry> {
        if (!wrongBookSmartReviewEnabled) return emptyList()
        return wrongBook
            .filterNot { isQuestionSlashed(it.bankId, it.question) }
            .filter { entry -> isWrongEntryDueForSmartReview(entry, now) }
            .sortedWith(
                compareBy<WrongQuestionEntry> { if (it.status == WrongStatus.MASTERED.label) 1 else 0 }
                    .thenBy { it.nextReviewAt ?: 0L }
                    .thenByDescending { it.wrongCount }
                    .thenByDescending { it.lastWrongAt }
            )
    }

    fun todayWrongBookSmartReviewSummary(now: Long = System.currentTimeMillis()): WrongBookSmartReviewSummary {
        val entries = todayWrongBookSmartReviewEntries(now)
        return WrongBookSmartReviewSummary(
            total = entries.size,
            notMastered = entries.count { it.status != WrongStatus.MASTERED.label },
            masteredReview = entries.count { it.status == WrongStatus.MASTERED.label }
        )
    }

    fun todayWrongBookSmartReviewCount(): Int = todayWrongBookSmartReviewSummary().total

    fun wrongBookActiveCount(): Int = reviewDueWrongEntries().size

    fun clearWrongBook() {
        wrongBook.clear()
        persist()
    }

    fun slashedQuestionCount(bankId: String): Int {
        val bank = banks.firstOrNull { it.id == bankId } ?: return 0
        val validKeys = bank.questions.map(::questionKey).toSet()
        return slashedQuestions.count { it.bankId == bankId && it.questionKey in validKeys }
    }

    fun slashedQuestionsForBank(bankId: String): List<Question> {
        val bank = banks.firstOrNull { it.id == bankId } ?: return emptyList()
        val slashedKeys = slashedQuestions
            .filter { it.bankId == bankId }
            .map { it.questionKey }
            .toSet()
        return bank.questions.filter { questionKey(it) in slashedKeys }
    }

    fun isQuestionSlashed(bankId: String, question: Question): Boolean {
        val key = questionKey(question)
        return slashedQuestions.any { it.bankId == bankId && it.questionKey == key }
    }

    fun slashCurrentPracticeQuestion(context: Context): Boolean {
        appContext = context.applicationContext
        val bank = activeBank() ?: return false
        val question = currentPracticeQuestion() ?: return false
        val key = questionKey(question)
        val now = System.currentTimeMillis()
        if (slashedQuestions.none { it.bankId == bank.id && it.questionKey == key }) {
            slashedQuestions.add(0, SlashedQuestionEntry(bank.id, key, now))
        }

        val currentIndex = practiceIndex
        practiceQuestions = practiceQuestions.filterNot { questionKey(it) == key }
        practiceSessionResults.remove(question.id)
        practiceAnswerResults.remove(question.id)
        practiceDraftAnswers.remove(question.id)
        practiceQuestionBankIds.remove(question.id)
        practiceLastResult = null
        selectedAnswer = emptyList()

        if (practiceQuestions.isEmpty()) {
            practiceIndex = 0
            practiceBatchSubmitted = false
            practiceBatchGroupStartIndex = 0
        } else {
            practiceIndex = currentIndex.coerceAtMost(practiceQuestions.lastIndex)
            if (practiceMode == PRACTICE_MODE_BATCH) {
                practiceBatchGroupStartIndex = practiceBatchGroupStartIndex.coerceAtMost(practiceQuestions.lastIndex)
                if (practiceIndex < practiceCurrentBatchStartIndex()) practiceIndex = practiceCurrentBatchStartIndex()
                if (practiceIndex > practiceCurrentBatchEndIndex()) practiceIndex = practiceCurrentBatchEndIndex()
            }
            restorePracticeSelectionForCurrentQuestion()
        }

        persist()
        return true
    }

    fun restoreSlashedQuestion(context: Context, bankId: String, question: Question): Boolean {
        appContext = context.applicationContext
        val key = questionKey(question)
        val removed = slashedQuestions.removeAll { it.bankId == bankId && it.questionKey == key }
        if (removed) persist()
        return removed
    }

    fun isQuestionFavorited(bankId: String, question: Question): Boolean {
        return favoriteQuestions.any { it.bankId == bankId && it.question.id == question.id }
    }

    fun isCurrentPracticeQuestionFavorited(): Boolean {
        val question = currentPracticeQuestion() ?: return false
        val bank = bankForPracticeQuestion(question) ?: return false
        return isQuestionFavorited(bank.id, question)
    }

    fun toggleCurrentPracticeFavorite(context: Context): Boolean {
        appContext = context.applicationContext
        val question = currentPracticeQuestion() ?: return false
        val bank = bankForPracticeQuestion(question) ?: return false
        val index = favoriteQuestions.indexOfFirst { it.bankId == bank.id && it.question.id == question.id }
        if (index >= 0) {
            favoriteQuestions.removeAt(index)
        } else {
            favoriteQuestions.add(
                0,
                FavoriteQuestionEntry(
                    bankId = bank.id,
                    bankName = bank.name,
                    question = question,
                    favoritedAt = System.currentTimeMillis()
                )
            )
        }
        persist()
        return true
    }

    fun removeFavoriteQuestion(entry: FavoriteQuestionEntry) {
        favoriteQuestions.removeAll { it.bankId == entry.bankId && it.question.id == entry.question.id }
        persist()
    }

    fun startFavoritePractice(entries: List<FavoriteQuestionEntry> = favoriteQuestions): Boolean {
        val selectedEntries = entries.distinctBy { it.bankId + "#" + it.question.id }
        val questions = selectedEntries.map { it.question }
        if (questions.isEmpty()) return false
        val sourceBankIds = selectedEntries.associate { it.question.id to it.bankId }
        return startPracticeSession(
            questionCount = questions.size,
            allowedTypes = QuestionType.values().toSet(),
            sourceQuestions = questions,
            sourceLabel = "收藏夹",
            randomize = false,
            sourceBankIds = sourceBankIds
        )
    }

    fun toggleAnswer(key: String, multiple: Boolean) {
        val nextAnswer = if (multiple) {
            if (selectedAnswer.contains(key)) selectedAnswer - key else selectedAnswer + key
        } else {
            listOf(key)
        }
        selectedAnswer = nextAnswer
        if (practiceMode == PRACTICE_MODE_BATCH && !practiceBatchSubmitted) {
            val question = currentPracticeQuestion()
            if (question != null) {
                if (nextAnswer.isEmpty()) {
                    practiceDraftAnswers.remove(question.id)
                } else {
                    practiceDraftAnswers[question.id] = nextAnswer
                }
            }
        }
    }

    fun setPracticeNextRequiresResult(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        practiceNextRequiresResult = enabled
        persist()
    }

    fun setRememberPracticeSettingsEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        rememberPracticeSettingsEnabled = enabled
        persist()
    }

    fun setRememberExamSettingsEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        rememberExamSettingsEnabled = enabled
        persist()
    }

    fun setSwipeNavigationEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        swipeNavigationEnabled = enabled
        persist()
    }

    fun setPracticeAutoSubmitEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        practiceAutoSubmitEnabled = enabled
        persist()
    }

    fun setPracticeAutoNextEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        practiceAutoNextEnabled = enabled
        persist()
    }

    fun setPracticeInlineAnswerSettingsEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        practiceInlineAnswerSettingsEnabled = enabled
        persist()
    }

    fun setPracticeReciteModeEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        practiceReciteModeEnabled = enabled
        persist()
    }

    fun setPracticeSlashEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        practiceSlashEnabled = enabled
        persist()
    }

    fun setPracticeQuickEditEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        practiceQuickEditEnabled = enabled
        persist()
    }

    fun setWrongBookSmartReviewEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        wrongBookSmartReviewEnabled = enabled
        persist()
    }

    fun setPreferredPracticeMode(context: Context, mode: String) {
        appContext = context.applicationContext
        preferredPracticeMode = normalizePracticeMode(mode)
        persist()
    }

    fun setPreferredPracticeBatchSize(context: Context, mode: String, customSize: Int? = null) {
        appContext = context.applicationContext
        preferredPracticeBatchSizeMode = normalizePracticeBatchSizeMode(mode)
        customSize?.let { preferredPracticeBatchCustomSize = it.coerceAtLeast(1) }
        persist()
    }

    fun preferredPracticeTypes(): Set<QuestionType> {
        if (preferredPracticeTypeNames.isBlank()) return emptySet()
        return preferredPracticeTypeNames
            .split(',')
            .mapNotNull { raw -> runCatching { QuestionType.valueOf(raw.trim()) }.getOrNull() }
            .toSet()
    }

    fun rememberPracticeSettings(
        context: Context,
        questionCountMode: String? = null,
        customQuestionCount: Int? = null,
        orderMode: String? = null,
        types: Set<QuestionType>? = null,
        practiceMode: String? = null,
        batchSizeMode: String? = null,
        customBatchSize: Int? = null
    ) {
        if (!rememberPracticeSettingsEnabled) return
        appContext = context.applicationContext
        questionCountMode?.let { preferredPracticeQuestionCountMode = normalizePracticeCountMode(it) }
        customQuestionCount?.let { preferredPracticeCustomQuestionCount = it.coerceAtLeast(1) }
        orderMode?.let { preferredPracticeOrderMode = normalizePracticeOrderMode(it) }
        practiceMode?.let { preferredPracticeMode = normalizePracticeMode(it) }
        batchSizeMode?.let { preferredPracticeBatchSizeMode = normalizePracticeBatchSizeMode(it) }
        customBatchSize?.let { preferredPracticeBatchCustomSize = it.coerceAtLeast(1) }
        types?.let { selectedTypes ->
            preferredPracticeTypeNames = selectedTypes
                .map { it.name }
                .sorted()
                .joinToString(",")
        }
        persist()
    }

    fun preferredExamTypeCountTexts(typeAvailableCounts: Map<QuestionType, Int>): Map<QuestionType, String> {
        val savedCounts = decodeQuestionTypeTextMap(preferredExamTypeCountsText)
        return QuestionType.values().associateWith { type ->
            val max = typeAvailableCounts[type] ?: 0
            val count = savedCounts[type]?.toIntOrNull()?.coerceIn(0, max) ?: 0
            count.toString()
        }
    }

    fun preferredExamTypeScoreTexts(): Map<QuestionType, String> {
        val savedScores = decodeQuestionTypeTextMap(preferredExamTypeScoresText)
        return QuestionType.values().associateWith { type ->
            val saved = savedScores[type]?.takeIf { it.toDoubleOrNull() != null }
            saved ?: (defaultExamTypeScores()[type] ?: 1.0).toPreferenceNumberText()
        }
    }

    fun rememberExamSettings(
        context: Context,
        questionCountMode: String? = null,
        customQuestionCount: Int? = null,
        durationMinutes: Int? = null,
        groupMode: String? = null,
        typeCountTexts: Map<QuestionType, String>? = null,
        typeScoreTexts: Map<QuestionType, String>? = null
    ) {
        if (!rememberExamSettingsEnabled) return
        appContext = context.applicationContext
        questionCountMode?.let { preferredExamQuestionCountMode = normalizeExamCountMode(it) }
        customQuestionCount?.let { preferredExamCustomQuestionCount = it.coerceAtLeast(1) }
        durationMinutes?.let { preferredExamDurationMinutes = it.coerceIn(1, 999) }
        groupMode?.let { preferredExamGroupMode = normalizeExamGroupMode(it) }
        typeCountTexts?.let { preferredExamTypeCountsText = encodeQuestionTypeTextMap(it) }
        typeScoreTexts?.let { preferredExamTypeScoresText = encodeQuestionTypeTextMap(it) }
        persist()
    }

    fun setStartupSplashEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        startupSplashEnabled = enabled
        persist()
    }

    fun setDarkThemeEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        darkThemeEnabled = enabled
        persist()
    }

    fun setShirohaModeEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        shirohaModeEnabled = enabled
        persist()
        LauncherIconSwitcher.applyShirohaMode(context, enabled)
    }

    fun setQuestionFontSizeMode(context: Context, mode: String) {
        appContext = context.applicationContext
        questionFontSizeMode = normalizeReadingSizeMode(mode)
        persist()
    }

    fun setOptionFontSizeMode(context: Context, mode: String) {
        appContext = context.applicationContext
        optionFontSizeMode = normalizeReadingSizeMode(mode)
        persist()
    }

    fun setCompactOptionsEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        compactOptionsEnabled = enabled
        persist()
    }

    fun questionFontSizeSp(): Int = when (questionFontSizeMode) {
        "small" -> 20
        "large" -> 24
        else -> 22
    }

    fun questionLineHeightSp(): Int = when (questionFontSizeMode) {
        "small" -> 27
        "large" -> 32
        else -> 29
    }

    fun optionFontSizeSp(): Int = when (optionFontSizeMode) {
        "small" -> 15
        "large" -> 18
        else -> 16
    }

    fun optionLineHeightSp(): Int = when (optionFontSizeMode) {
        "small" -> 20
        "large" -> 24
        else -> 21
    }

    fun setAiInterfaceConfig(
        context: Context,
        provider: String,
        apiBaseUrl: String,
        apiKey: String,
        modelName: String
    ) {
        appContext = context.applicationContext
        aiProvider = provider.ifBlank { "DeepSeek" }
        aiApiBaseUrl = apiBaseUrl.trim()
        aiApiKey = apiKey.trim()
        aiModelName = modelName.trim()
        persist()
    }

    fun setAiRefactorEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        aiRefactorEnabled = enabled
        persist()
    }

    fun setAiReviewEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        aiReviewEnabled = enabled
        persist()
    }

    fun setAiAnalysisEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        aiAnalysisEnabled = enabled
        persist()
    }

    fun setAiOnlyAnomaly(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        aiOnlyAnomaly = enabled
        persist()
    }

    fun setAiRequireConfirm(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        aiRequireConfirm = enabled
        persist()
    }

    fun setAiProcessingLimits(context: Context, maxQuestions: Int, timeoutSeconds: Int) {
        appContext = context.applicationContext
        aiMaxQuestions = maxQuestions.coerceIn(5, 100)
        aiTimeoutSeconds = timeoutSeconds.coerceIn(15, 180)
        persist()
    }

    fun setAiRefactorMaxChars(context: Context, maxChars: Int) {
        appContext = context.applicationContext
        aiRefactorMaxChars = maxChars.coerceIn(5000, 80000)
        persist()
    }

    fun clearAiConfig(context: Context) {
        appContext = context.applicationContext
        aiApiBaseUrl = ""
        aiApiKey = ""
        aiModelName = ""
        aiRefactorEnabled = false
        aiReviewEnabled = false
        aiAnalysisEnabled = false
        persist()
    }

    fun isAiConfigured(): Boolean {
        return aiApiBaseUrl.isNotBlank() && aiApiKey.isNotBlank() && aiModelName.isNotBlank()
    }

    fun submitPracticeQuestion(): QuestionCheckResult? {
        if (practiceMode == PRACTICE_MODE_BATCH && !practiceBatchSubmitted) return null
        val question = currentPracticeQuestion() ?: return null
        val result = evaluateQuestion(question, selectedAnswer)
        practiceLastResult = result
        practiceSessionResults[question.id] = result.correct
        practiceAnswerResults[question.id] = StudyQuestionResult(
            question = question,
            userAnswer = result.userAnswer,
            correct = result.correct,
            answerText = result.answerText
        )

        val bank = bankForPracticeQuestion(question)
        if (result.correct) {
            markWrongQuestionRight(bank = bank, question = question)
        } else {
            addWrongQuestion(
                bank = bank,
                question = question,
                userAnswer = result.userAnswer,
                source = currentPracticeWrongSource()
            )
        }
        persist()
        return result
    }

    fun submitPracticeBatch(): Boolean {
        if (practiceMode != PRACTICE_MODE_BATCH || practiceQuestions.isEmpty() || practiceBatchSubmitted) return false
        practiceCurrentBatchQuestions().forEach { question ->
            val userAnswer = practiceDraftAnswers[question.id].orEmpty()
            val result = evaluateQuestion(question, userAnswer)
            practiceSessionResults[question.id] = result.correct
            practiceAnswerResults[question.id] = StudyQuestionResult(
                question = question,
                userAnswer = result.userAnswer,
                correct = result.correct,
                answerText = result.answerText
            )
            val bank = bankForPracticeQuestion(question)
            if (result.correct) {
                markWrongQuestionRight(bank = bank, question = question)
            } else {
                addWrongQuestion(
                    bank = bank,
                    question = question,
                    userAnswer = result.userAnswer,
                    source = currentPracticeWrongSource()
                )
            }
        }
        practiceBatchSubmitted = true
        practiceIndex = practiceCurrentBatchStartIndex()
        restorePracticeSelectionForCurrentQuestion()
        persist()
        return true
    }

    fun practiceDraftAnsweredCount(): Int {
        val questions = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchQuestions() else practiceQuestions
        return questions.count { question -> practiceDraftAnswers[question.id]?.isNotEmpty() == true }
    }

    fun practiceCurrentBatchSubmittedCount(): Int {
        return practiceCurrentBatchQuestions().count { question -> practiceAnswerResults.containsKey(question.id) }
    }

    fun practiceCurrentBatchCorrectCount(): Int {
        return practiceCurrentBatchQuestions().count { question -> practiceAnswerResults[question.id]?.correct == true }
    }

    fun practiceWrongQuestionIndexes(): List<Int> {
        val indexes = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchIndexes() else practiceQuestions.indices.toList()
        return indexes.mapNotNull { index ->
            val question = practiceQuestions.getOrNull(index) ?: return@mapNotNull null
            if (practiceAnswerResults[question.id]?.correct == false) index else null
        }
    }

    fun goToPracticeQuestion(index: Int) {
        val questions = activePracticeQuestions()
        if (questions.isEmpty()) return
        val minIndex = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchStartIndex() else 0
        val maxIndex = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchEndIndex() else questions.lastIndex
        practiceIndex = index.coerceIn(minIndex, maxIndex)
        restorePracticeSelectionForCurrentQuestion()
    }

    fun preferredPracticeBatchGroupSize(): Int {
        return resolvePracticeBatchGroupSize(preferredPracticeBatchSizeMode, preferredPracticeBatchCustomSize)
    }

    fun resolvePracticeBatchGroupSize(mode: String, customSize: Int): Int {
        return when (normalizePracticeBatchSizeMode(mode)) {
            "20" -> 20
            "custom" -> customSize.coerceAtLeast(1)
            else -> 10
        }
    }

    fun practiceCurrentBatchStartIndex(): Int {
        if (practiceQuestions.isEmpty()) return 0
        return practiceBatchGroupStartIndex.coerceIn(0, practiceQuestions.lastIndex)
    }

    fun practiceCurrentBatchEndIndex(): Int {
        if (practiceQuestions.isEmpty()) return 0
        val start = practiceCurrentBatchStartIndex()
        return (start + practiceBatchGroupSize - 1).coerceAtMost(practiceQuestions.lastIndex)
    }

    fun practiceCurrentBatchIndexes(): List<Int> {
        if (practiceQuestions.isEmpty()) return emptyList()
        return (practiceCurrentBatchStartIndex()..practiceCurrentBatchEndIndex()).toList()
    }

    fun practiceCurrentBatchQuestions(): List<Question> {
        return practiceCurrentBatchIndexes().mapNotNull { index -> practiceQuestions.getOrNull(index) }
    }

    fun practiceCurrentBatchTotal(): Int = practiceCurrentBatchQuestions().size

    fun practiceBatchGroupNumber(): Int {
        if (practiceQuestions.isEmpty()) return 0
        return practiceCurrentBatchStartIndex() / practiceBatchGroupSize + 1
    }

    fun practiceBatchGroupCount(): Int {
        if (practiceQuestions.isEmpty()) return 0
        return (practiceQuestions.size + practiceBatchGroupSize - 1) / practiceBatchGroupSize
    }

    fun canStartNextPracticeBatchGroup(): Boolean {
        return practiceMode == PRACTICE_MODE_BATCH &&
            practiceBatchSubmitted &&
            practiceCurrentBatchEndIndex() < practiceQuestions.lastIndex
    }

    fun startNextPracticeBatchGroup(): Boolean {
        if (!canStartNextPracticeBatchGroup()) return false
        practiceBatchGroupStartIndex = practiceCurrentBatchEndIndex() + 1
        practiceIndex = practiceBatchGroupStartIndex
        selectedAnswer = emptyList()
        practiceLastResult = null
        practiceBatchSubmitted = false
        restorePracticeSelectionForCurrentQuestion()
        return true
    }

    fun isAllPracticeBatchGroupsSubmitted(): Boolean {
        return practiceMode == PRACTICE_MODE_BATCH &&
            practiceBatchSubmitted &&
            practiceCurrentBatchEndIndex() >= practiceQuestions.lastIndex &&
            practiceQuestions.all { question -> practiceAnswerResults.containsKey(question.id) }
    }


    fun startExam(questionCount: Int, durationMinutes: Int): Boolean {
        return startExam(
            questionCount = questionCount,
            durationMinutes = durationMinutes,
            allowedTypes = QuestionType.values().toSet(),
            typeScores = defaultExamTypeScores()
        )
    }

    fun startExam(
        questionCount: Int,
        durationMinutes: Int,
        allowedTypes: Set<QuestionType>,
        typeScores: Map<QuestionType, Double>,
        randomize: Boolean = false
    ): Boolean {
        val selectedTypes = allowedTypes.ifEmpty { objectiveQuestionTypes() }
        val source = activeBank()?.questions.orEmpty().filter { it.type in selectedTypes }
        if (source.isEmpty()) return false

        val count = questionCount.coerceIn(1, source.size)
        val pickedQuestions = if (randomize) source.shuffled().take(count) else source.take(count)
        return beginExam(pickedQuestions, durationMinutes, typeScores)
    }

    fun startExamByTypeCounts(
        typeCounts: Map<QuestionType, Int>,
        durationMinutes: Int,
        typeScores: Map<QuestionType, Double>
    ): Boolean {
        val source = activeBank()?.questions.orEmpty()
        val pickedQuestions = typeCounts.entries.flatMap { (type, count) ->
            if (count <= 0) emptyList() else source.filter { it.type == type }.shuffled().take(count)
        }.shuffled()
        if (pickedQuestions.isEmpty()) return false
        return beginExam(pickedQuestions, durationMinutes, typeScores)
    }

    private fun beginExam(
        questions: List<Question>,
        durationMinutes: Int,
        typeScores: Map<QuestionType, Double>
    ): Boolean {
        if (questions.isEmpty()) return false
        examQuestions = questions
        examTypeScores = defaultExamTypeScores() + typeScores.mapValues { it.value.coerceAtLeast(0.0) }
        examIndex = 0
        examDurationSeconds = durationMinutes.coerceAtLeast(1) * 60
        examRemainingSeconds = examDurationSeconds
        examFinished = false
        examAutoSubmitted = false
        examAnswers.clear()
        return true
    }

    fun resetExam() {
        examQuestions = emptyList()
        examIndex = 0
        examDurationSeconds = 0
        examRemainingSeconds = 0
        examFinished = false
        examAutoSubmitted = false
        examTypeScores = defaultExamTypeScores()
        examAnswers.clear()
    }

    fun currentExamQuestion(): Question? {
        if (examQuestions.isEmpty()) return null
        val safeIndex = examIndex.coerceIn(0, examQuestions.lastIndex)
        if (safeIndex != examIndex) examIndex = safeIndex
        return examQuestions[safeIndex]
    }

    fun nextExamQuestion() {
        if (examQuestions.isEmpty()) return
        examIndex = (examIndex + 1).coerceAtMost(examQuestions.lastIndex)
    }

    fun previousExamQuestion() {
        if (examQuestions.isEmpty()) return
        examIndex = (examIndex - 1).coerceAtLeast(0)
    }

    fun jumpToExamQuestion(index: Int) {
        if (examQuestions.isEmpty()) return
        examIndex = index.coerceIn(0, examQuestions.lastIndex)
    }

    fun toggleExamAnswer(key: String, multiple: Boolean) {
        val question = currentExamQuestion() ?: return
        val current = examAnswers[question.id].orEmpty()
        val updated = if (multiple) {
            if (current.contains(key)) current - key else current + key
        } else {
            listOf(key)
        }
        examAnswers[question.id] = updated
    }

    fun submitExam(autoSubmitted: Boolean = false) {
        if (examQuestions.isEmpty() || examFinished) return
        examFinished = true
        examAutoSubmitted = autoSubmitted

        val summary = examSummary()
        val bank = activeBank()
        val now = System.currentTimeMillis()
        val usedSeconds = summary.durationSeconds - summary.remainingSeconds
        val detailResults = examQuestions.map { question ->
            val userAnswer = examAnswers[question.id].orEmpty()
            val result = evaluateQuestion(question, userAnswer)
            val maxScore = scoreForExamQuestion(question)
            StudyQuestionResult(
                question = question,
                userAnswer = userAnswer,
                correct = result.correct,
                answerText = result.answerText,
                earnedScore = if (result.correct) maxScore else 0.0,
                maxScore = maxScore
            )
        }
        studyRecords.add(
            0,
            StudyRecord(
                id = "exam_${now}",
                bankId = bank?.id,
                bankName = bank?.name ?: "未命名题库",
                source = "考试",
                title = "原生考试",
                total = summary.total,
                correct = summary.correct,
                timestamp = now,
                durationSeconds = usedSeconds,
                autoSubmitted = autoSubmitted,
                startedAt = now - usedSeconds * 1000L,
                earnedScore = summary.earnedScore,
                totalScore = summary.totalScore,
                questionResults = detailResults
            )
        )

        examQuestions.forEach { question ->
            val userAnswer = examAnswers[question.id].orEmpty()
            val result = evaluateQuestion(question, userAnswer)
            if (result.correct) {
                markWrongQuestionRight(bank = bank, question = question)
            } else {
                addWrongQuestion(
                    bank = bank,
                    question = question,
                    userAnswer = userAnswer,
                    source = "考试"
                )
            }
        }
        persist()
    }

    fun tickExam() {
        if (examFinished || examQuestions.isEmpty()) return
        if (examRemainingSeconds > 0) {
            examRemainingSeconds -= 1
        }
        if (examRemainingSeconds <= 0 && !examFinished) {
            submitExam(autoSubmitted = true)
        }
    }

    fun examAnsweredCount(): Int = examQuestions.count { examAnswers[it.id].orEmpty().isNotEmpty() }

    fun examCorrectCount(): Int = examQuestions.count { question ->
        evaluateQuestion(question, examAnswers[question.id].orEmpty()).correct
    }

    fun examSummary(): ExamSummary {
        return ExamSummary(
            total = examQuestions.size,
            answered = examAnsweredCount(),
            correct = examCorrectCount(),
            durationSeconds = examDurationSeconds,
            remainingSeconds = examRemainingSeconds,
            autoSubmitted = examAutoSubmitted,
            totalScore = examTotalScore(),
            earnedScore = examEarnedScore()
        )
    }

    fun examTotalScore(): Double = examQuestions.sumOf { scoreForExamQuestion(it) }

    fun examEarnedScore(): Double = examQuestions.sumOf { question ->
        if (evaluateQuestion(question, examAnswers[question.id].orEmpty()).correct) scoreForExamQuestion(question) else 0.0
    }



    fun exportBanksBackupJson(bankIds: Set<String>): String {
        val selectedBanks = banks.filter { bankIds.contains(it.id) }
        return buildBackupJson(
            kind = "shiroha_quiz_selected_banks",
            selectedBanks = selectedBanks,
            includeWrongBook = false,
            includeFavorites = false,
            includeStudyRecords = false,
            assetMapping = null
        ).toString(2)
    }

    fun exportFullBackupJson(): String {
        return buildBackupJson(
            kind = "shiroha_quiz_full_backup",
            selectedBanks = banks,
            includeWrongBook = true,
            includeFavorites = true,
            includeStudyRecords = true,
            assetMapping = null
        ).toString(2)
    }

    fun exportBanksBackupZip(bankIds: Set<String>): ByteArray {
        val selectedBanks = banks.filter { bankIds.contains(it.id) }
        return buildBackupZip(
            kind = "shiroha_quiz_selected_banks",
            selectedBanks = selectedBanks,
            includeWrongBook = false,
            includeFavorites = false,
            includeStudyRecords = false
        )
    }

    fun exportFullBackupZip(): ByteArray {
        return buildBackupZip(
            kind = "shiroha_quiz_full_backup",
            selectedBanks = banks,
            includeWrongBook = true,
            includeFavorites = true,
            includeStudyRecords = true
        )
    }

    fun importBackupJson(context: Context, rawText: String): String {
        return importBackupBytes(
            context = context,
            fileName = "backup.json",
            bytes = rawText.toByteArray(Charsets.UTF_8)
        )
    }

    fun importBackupBytes(context: Context, fileName: String, bytes: ByteArray): String {
        appContext = context.applicationContext
        if (bytes.isEmpty()) return "导入失败：文件内容为空。"
        val lowerName = fileName.lowercase()
        return if (lowerName.endsWith(".zip") || looksLikeZip(bytes)) {
            importBackupZip(context, bytes)
        } else {
            importBackupJsonInternal(context, bytes.toString(Charsets.UTF_8), emptyMap())
        }
    }

    private data class BackupAsset(
        val backupPath: String,
        val file: File
    )

    private fun buildBackupZip(
        kind: String,
        selectedBanks: List<QuizBank>,
        includeWrongBook: Boolean,
        includeFavorites: Boolean,
        includeStudyRecords: Boolean
    ): ByteArray {
        val assetMapping = mutableMapOf<String, BackupAsset>()
        val root = buildBackupJson(
            kind = kind,
            selectedBanks = selectedBanks,
            includeWrongBook = includeWrongBook,
            includeFavorites = includeFavorites,
            includeStudyRecords = includeStudyRecords,
            assetMapping = assetMapping
        )
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(root.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            assetMapping.values
                .distinctBy { it.backupPath }
                .forEach { asset ->
                    if (asset.file.exists() && asset.file.isFile) {
                        zip.putNextEntry(ZipEntry(asset.backupPath))
                        asset.file.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
        }
        return output.toByteArray()
    }

    private fun buildBackupJson(
        kind: String,
        selectedBanks: List<QuizBank>,
        includeWrongBook: Boolean,
        includeFavorites: Boolean,
        includeStudyRecords: Boolean,
        assetMapping: MutableMap<String, BackupAsset>?
    ): JSONObject {
        val root = JSONObject()
        root.put("kind", kind)
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("activeBankId", activeBankId)
        root.put("banks", JSONArray(banksToJson(selectedBanks, assetMapping)))
        if (includeWrongBook) root.put("wrongBook", JSONArray(wrongBookToJson(wrongBook, assetMapping)))
        if (includeFavorites) root.put("favoriteQuestions", JSONArray(favoriteQuestionsToJson(favoriteQuestions, assetMapping)))
        if (includeStudyRecords) root.put("studyRecords", JSONArray(studyRecordsToJson(studyRecords, assetMapping)))
        return root
    }

    private fun importBackupZip(context: Context, bytes: ByteArray): String {
        val entries = mutableMapOf<String, ByteArray>()
        var totalSize = 0L
        val maxEntrySize = 20L * 1024 * 1024
        val maxTotalSize = 100L * 1024 * 1024
        val maxEntries = 1000
        runCatching {
            ZipInputStream(bytes.inputStream()).use { zip ->
                while (entries.size < maxEntries) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val data = zip.readBytes()
                    if (data.size > maxEntrySize) continue
                    totalSize += data.size
                    if (totalSize > maxTotalSize) break
                    entries[entry.name] = data
                    zip.closeEntry()
                }
            }
        }.getOrElse { return "导入失败：ZIP 备份无法读取。" }

        val jsonBytes = entries["backup.json"]
            ?: entries.entries.firstOrNull { it.key.endsWith(".json", ignoreCase = true) }?.value
            ?: return "导入失败：ZIP 备份中没有 backup.json。"
        val assetEntries = entries.filterKeys { it.startsWith("assets/") }
        return importBackupJsonInternal(context, jsonBytes.toString(Charsets.UTF_8), assetEntries)
    }

    private fun importBackupJsonInternal(
        context: Context,
        rawText: String,
        zipAssets: Map<String, ByteArray>
    ): String {
        appContext = context.applicationContext
        val text = rawText.trim()
        if (text.isBlank()) return "导入失败：文件内容为空。"

        val assetDir = File(context.filesDir, "question_assets/backup_${System.currentTimeMillis()}").apply { mkdirs() }
        val root = runCatching {
            if (text.startsWith("[")) {
                JSONObject().put("banks", JSONArray(text))
            } else {
                JSONObject(text)
            }
        }.getOrElse { return "导入失败：不是有效的备份文件。" }

        val bankArray = root.optJSONArray("banks") ?: return "导入失败：备份中没有题库数据。"
        val importedBanks = runCatching { parseBanksJson(bankArray.toString()) }
            .getOrElse { return "导入失败：题库数据无法解析。" }
            .map { bank -> normalizeImportedBankAssets(bank, zipAssets, assetDir) }
            .map(::sanitizeBank)
        if (importedBanks.isEmpty()) return "导入失败：备份中没有可用题库。"

        val idMap = mutableMapOf<String, String>()
        val now = System.currentTimeMillis()
        val addedBanks = importedBanks.mapIndexed { index, bank ->
            val newId = "bank_${now}_$index"
            idMap[bank.id] = newId
            bank.copy(
                id = newId,
                name = uniqueImportedBankName(bank.name.ifBlank { "导入题库" })
            )
        }
        banks.addAll(addedBanks)
        if (activeBankId == null && addedBanks.isNotEmpty()) activeBankId = addedBanks.first().id

        val importedWrongBook = root.optJSONArray("wrongBook")?.let { array ->
            runCatching { parseWrongBookJson(array.toString()) }.getOrDefault(emptyList())
        }.orEmpty()
        val mappedWrongBook = importedWrongBook.mapNotNull { entry ->
            val mappedBankId = idMap[entry.bankId] ?: return@mapNotNull null
            val mappedBankName = addedBanks.firstOrNull { it.id == mappedBankId }?.name ?: entry.bankName
            sanitizeWrongEntry(
                entry.copy(
                    bankId = mappedBankId,
                    bankName = mappedBankName,
                    question = normalizeImportedQuestionAssets(entry.question, zipAssets, assetDir)
                )
            )
        }
        wrongBook.addAll(0, mappedWrongBook)

        val importedFavorites = root.optJSONArray("favoriteQuestions")?.let { array ->
            runCatching { parseFavoriteQuestionsJson(array.toString()) }.getOrDefault(emptyList())
        }.orEmpty()
        val mappedFavorites = importedFavorites.mapNotNull { entry ->
            val mappedBankId = idMap[entry.bankId] ?: return@mapNotNull null
            val mappedBankName = addedBanks.firstOrNull { it.id == mappedBankId }?.name ?: entry.bankName
            sanitizeFavoriteEntry(
                entry.copy(
                    bankId = mappedBankId,
                    bankName = mappedBankName,
                    question = normalizeImportedQuestionAssets(entry.question, zipAssets, assetDir)
                )
            )
        }
        favoriteQuestions.addAll(0, mappedFavorites)

        val importedRecords = root.optJSONArray("studyRecords")?.let { array ->
            runCatching { parseStudyRecordsJson(array.toString()) }.getOrDefault(emptyList())
        }.orEmpty()
        val mappedRecords = importedRecords.map { record ->
            val mappedBankId = record.bankId?.let { idMap[it] }
            val mappedBankName = mappedBankId?.let { id -> addedBanks.firstOrNull { it.id == id }?.name }
                ?: record.bankName
            record.copy(
                bankId = mappedBankId ?: record.bankId,
                bankName = mappedBankName,
                questionResults = record.questionResults.map { result ->
                    result.copy(question = normalizeImportedQuestionAssets(result.question, zipAssets, assetDir))
                }
            )
        }
        studyRecords.addAll(0, mappedRecords)

        resetPracticeState()
        resetExam()
        persist()
        return "已导入 ${addedBanks.size} 个题库" +
            if (mappedWrongBook.isNotEmpty() || mappedFavorites.isNotEmpty() || mappedRecords.isNotEmpty()) "，同时恢复 ${mappedWrongBook.size} 条错题、${mappedFavorites.size} 条收藏、${mappedRecords.size} 条记录。" else "。"
    }

    private fun remapBankAssets(bank: QuizBank, zipAssets: Map<String, ByteArray>, assetDir: File): QuizBank {
        return bank.copy(questions = bank.questions.map { remapQuestionAssets(it, zipAssets, assetDir) })
    }

    private fun normalizeImportedBankAssets(bank: QuizBank, zipAssets: Map<String, ByteArray>, assetDir: File): QuizBank {
        return bank.copy(questions = bank.questions.map { normalizeImportedQuestionAssets(it, zipAssets, assetDir) })
    }

    private fun normalizeImportedQuestionAssets(question: Question, zipAssets: Map<String, ByteArray>, assetDir: File): Question {
        val convertedQuestionText = convertEmbeddedDataImages(question, assetDir)
        val convertedStructuredImages = convertDataUriQuestionImages(convertedQuestionText, assetDir)
        return remapQuestionAssets(convertedStructuredImages, zipAssets, assetDir)
    }

    private fun remapQuestionAssets(question: Question, zipAssets: Map<String, ByteArray>, assetDir: File): Question {
        if (question.images.isEmpty() || zipAssets.isEmpty()) return question
        return question.copy(
            images = question.images.map { image ->
                val backupPath = image.localPath.replace("\\", "/").removePrefix("/")
                val imageBytes = zipAssets[backupPath]
                if (imageBytes == null) {
                    image
                } else {
                    val outFile = File(assetDir, File(backupPath).name)
                    runCatching { outFile.writeBytes(imageBytes) }
                    image.copy(localPath = outFile.absolutePath, sizeBytes = outFile.length())
                }
            }
        )
    }

    private val embeddedDataImageRegex = Regex(
        """!\[([^\]]*)\]\((data:image/([A-Za-z0-9.+-]+);base64,([A-Za-z0-9+/=\r\n]+))\)""",
        setOf(RegexOption.IGNORE_CASE)
    )

    private fun convertEmbeddedDataImages(question: Question, assetDir: File): Question {
        val rawQuestion = question.question
        if (!rawQuestion.contains("data:image/", ignoreCase = true)) return question

        var nextOrder = (question.images.maxOfOrNull { it.order } ?: question.images.size) + 1
        val addedImages = mutableListOf<QuestionImage>()
        val cleanedQuestion = embeddedDataImageRegex.replace(rawQuestion) { match ->
            val alt = match.groupValues[1].ifBlank { "题目图片$nextOrder" }
            val mimeSuffix = match.groupValues[3].lowercase(Locale.ROOT)
            val base64Text = match.groupValues[4].replace(Regex("""\s+"""), "")
            val bytes = runCatching { Base64.decode(base64Text, Base64.DEFAULT) }.getOrNull()
            if (bytes == null || bytes.isEmpty()) {
                "【$alt】"
            } else {
                val ext = imageExtensionForMimeSuffix(mimeSuffix)
                val safeQuestionId = question.id.ifBlank { "question" }.replace(Regex("[^A-Za-z0-9_.-]"), "_")
                val imageId = "web_${safeQuestionId}_${nextOrder}_${System.currentTimeMillis()}"
                    .replace(Regex("[^A-Za-z0-9_.-]"), "_")
                    .take(96)
                val outFile = File(assetDir, "$imageId.$ext")
                val saved = runCatching { outFile.writeBytes(bytes) }.isSuccess
                if (saved) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    addedImages.add(
                        QuestionImage(
                            id = imageId,
                            localPath = outFile.absolutePath,
                            sourceName = "$alt.$ext",
                            order = nextOrder,
                            width = bounds.outWidth.takeIf { it > 0 },
                            height = bounds.outHeight.takeIf { it > 0 },
                            sizeBytes = outFile.length()
                        )
                    )
                    nextOrder++
                    "\n【$alt】\n"
                } else {
                    "【$alt】"
                }
            }
        }.replace(Regex("""\n{3,}"""), "\n\n").trim()

        if (addedImages.isEmpty()) return question
        return question.copy(
            question = cleanedQuestion,
            images = question.images + addedImages
        )
    }

    private fun convertDataUriQuestionImages(question: Question, assetDir: File): Question {
        if (question.images.isEmpty()) return question
        var changed = false
        val convertedImages = question.images.mapIndexed { index, image ->
            val dataUri = image.localPath.trim()
            if (!dataUri.startsWith("data:image/", ignoreCase = true)) {
                image
            } else {
                val parsed = parseDataImageUri(dataUri)
                if (parsed == null) {
                    image
                } else {
                    val safeImageId = image.id.ifBlank { "json_image_${index + 1}" }
                        .replace(Regex("[^A-Za-z0-9_.-]"), "_")
                        .take(96)
                        .ifBlank { "json_image_${index + 1}" }
                    val outFile = File(assetDir, "$safeImageId.${parsed.extension}")
                    val saved = runCatching { outFile.writeBytes(parsed.bytes) }.isSuccess
                    if (saved) {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(parsed.bytes, 0, parsed.bytes.size, bounds)
                        changed = true
                        image.copy(
                            localPath = outFile.absolutePath,
                            sourceName = image.sourceName.ifBlank { "题目图片${index + 1}.${parsed.extension}" },
                            order = image.order.takeIf { it > 0 } ?: (index + 1),
                            width = image.width ?: bounds.outWidth.takeIf { it > 0 },
                            height = image.height ?: bounds.outHeight.takeIf { it > 0 },
                            sizeBytes = outFile.length()
                        )
                    } else {
                        image
                    }
                }
            }
        }
        return if (changed) question.copy(images = convertedImages) else question
    }

    private data class ParsedDataImage(
        val extension: String,
        val bytes: ByteArray
    )

    private fun parseDataImageUri(dataUri: String): ParsedDataImage? {
        val match = Regex(
            "^data:image/([A-Za-z0-9.+-]+);base64,([A-Za-z0-9+/=\\r\\n\\s]+)$",
            RegexOption.IGNORE_CASE
        ).find(dataUri.trim()) ?: return null
        val mimeSuffix = match.groupValues[1].lowercase(Locale.ROOT)
        val base64Text = match.groupValues[2].replace(Regex("\\s+"), "")
        val bytes = runCatching { Base64.decode(base64Text, Base64.DEFAULT) }.getOrNull()
        if (bytes == null || bytes.isEmpty()) return null
        return ParsedDataImage(
            extension = imageExtensionForMimeSuffix(mimeSuffix),
            bytes = bytes
        )
    }

    private fun imageExtensionForMimeSuffix(mimeSuffix: String): String {
        return when (mimeSuffix.lowercase(Locale.ROOT)) {
            "jpeg", "jpg" -> "jpg"
            "png" -> "png"
            "gif" -> "gif"
            "webp" -> "webp"
            "bmp" -> "bmp"
            "svg+xml" -> "svg"
            else -> "bin"
        }
    }

    private fun looksLikeZip(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 'P'.code.toByte() &&
            bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 3.toByte() &&
            bytes[3] == 4.toByte()
    }

    fun findStudyRecord(recordId: String?): StudyRecord? {
        if (recordId.isNullOrBlank()) return null
        return studyRecords.firstOrNull { it.id == recordId }
    }

    fun clearAllLocalData(context: Context) {
        appContext = context.applicationContext
        banks.clear()
        wrongBook.clear()
        slashedQuestions.clear()
        favoriteQuestions.clear()
        studyRecords.clear()
        activeBankId = null
        resetPracticeState()
        resetExam()
        persist()
    }

    internal fun resetForTesting() {
        banks.clear()
        wrongBook.clear()
        slashedQuestions.clear()
        favoriteQuestions.clear()
        studyRecords.clear()
        activeBankId = null
        resetPracticeState()
        initialized = false
        appContext = null
        resetExam()
    }


    private fun finishPracticeSessionIfNeeded() {
        if (practiceQuestions.isEmpty() || practiceAnswerResults.isEmpty()) return
        val now = System.currentTimeMillis()
        val startedAt = practiceStartedAt ?: now
        val orderedResults = practiceQuestions.mapNotNull { question -> practiceAnswerResults[question.id] }
        if (orderedResults.isEmpty()) return
        val correctCount = orderedResults.count { it.correct }
        val involvedBankIds = practiceQuestions.mapNotNull { practiceQuestionBankIds[it.id] }.distinct()
        val recordBank = involvedBankIds.singleOrNull()?.let { bankId -> banks.firstOrNull { it.id == bankId } }
        val recordSource = when (practiceSourceLabel) {
            "错题本" -> "错题练习"
            "今日复习" -> "今日复习"
            "收藏夹" -> "收藏练习"
            else -> "练习"
        }
        studyRecords.add(
            0,
            StudyRecord(
                id = "practice_${now}",
                bankId = recordBank?.id,
                bankName = recordBank?.name ?: practiceSourceLabel.ifBlank { "未命名题库" },
                source = recordSource,
                title = practiceSourceLabel.ifBlank { "练习记录" },
                total = orderedResults.size,
                correct = correctCount,
                timestamp = now,
                durationSeconds = ((now - startedAt) / 1000L).toInt().coerceAtLeast(0),
                autoSubmitted = false,
                startedAt = startedAt,
                questionResults = orderedResults
            )
        )
        persist()
    }

    private fun resetPracticeState() {
        practiceQuestions = emptyList()
        practiceSourceLabel = "当前题库"
        practiceIndex = 0
        selectedAnswer = emptyList()
        practiceLastResult = null
        practiceMode = PRACTICE_MODE_INSTANT
        practiceBatchSubmitted = false
        practiceBatchGroupSize = 10
        practiceBatchGroupStartIndex = 0
        practiceSessionResults.clear()
        practiceAnswerResults.clear()
        practiceDraftAnswers.clear()
        practiceQuestionBankIds.clear()
        practiceStartedAt = null
    }

    private fun restorePracticeSelectionForCurrentQuestion() {
        val question = currentPracticeQuestion()
        selectedAnswer = when {
            question == null -> emptyList()
            practiceMode == PRACTICE_MODE_BATCH && !practiceBatchSubmitted -> practiceDraftAnswers[question.id].orEmpty()
            question != null -> practiceAnswerResults[question.id]?.userAnswer.orEmpty()
            else -> emptyList()
        }
        practiceLastResult = if (question != null && practiceMode == PRACTICE_MODE_BATCH && practiceBatchSubmitted) {
            practiceAnswerResults[question.id]?.let { result ->
                QuestionCheckResult(
                    question = question,
                    userAnswer = result.userAnswer,
                    correct = result.correct,
                    answerText = result.answerText
                )
            }
        } else {
            null
        }
    }


    fun objectiveQuestionTypes(): Set<QuestionType> = setOf(
        QuestionType.SINGLE,
        QuestionType.MULTIPLE,
        QuestionType.JUDGE
    )

    fun questionTypeCounts(questions: List<Question> = activeBank()?.questions.orEmpty()): Map<QuestionType, Int> {
        return QuestionType.values().associateWith { type -> questions.count { it.type == type } }
    }

    fun practiceAnsweredCount(): Int = practiceSessionResults.size

    fun practiceCorrectCount(): Int = practiceSessionResults.values.count { it }

    private fun sanitizeBank(bank: QuizBank): QuizBank {
        return bank.copy(questions = bank.questions.map(::sanitizeQuestion))
    }

    private fun sanitizeWrongEntry(entry: WrongQuestionEntry): WrongQuestionEntry {
        val normalizedStatus = WrongStatus.normalize(entry.status)
        val normalizedReviewRightCount = entry.reviewRightCount.coerceAtLeast(0)
        val normalizedStreakCorrectCount = entry.streakCorrectCount.coerceAtLeast(0)
        val sanitizedStatus = when {
            normalizedStatus == WrongStatus.MASTERED.label -> WrongStatus.MASTERED.label
            isWrongEntryMastered(normalizedReviewRightCount, normalizedStreakCorrectCount) -> WrongStatus.MASTERED.label
            else -> normalizedStatus
        }
        return entry.copy(
            question = sanitizeQuestion(entry.question),
            wrongCount = entry.wrongCount.coerceAtLeast(0),
            rightCount = entry.rightCount.coerceAtLeast(0),
            reviewRightCount = if (sanitizedStatus == WrongStatus.MASTERED.label && normalizedReviewRightCount == 0 && normalizedStreakCorrectCount == 0) 2 else normalizedReviewRightCount,
            streakCorrectCount = if (sanitizedStatus == WrongStatus.MASTERED.label && normalizedReviewRightCount == 0 && normalizedStreakCorrectCount == 0) 2 else normalizedStreakCorrectCount,
            lastWrongAt = if (entry.lastWrongAt > 0L) entry.lastWrongAt else entry.timestamp,
            status = sanitizedStatus,
            lastReviewedAt = entry.lastReviewedAt?.takeIf { it > 0L },
            nextReviewAt = entry.nextReviewAt?.takeIf { it > 0L },
            reviewLevel = entry.reviewLevel.coerceIn(0, 5)
        )
    }

    private fun sanitizeSlashedEntries(entries: List<SlashedQuestionEntry>, banks: List<QuizBank>): List<SlashedQuestionEntry> {
        val validKeysByBank = banks.associate { bank -> bank.id to bank.questions.map(::questionKey).toSet() }
        return entries
            .filter { entry -> entry.questionKey.isNotBlank() && validKeysByBank[entry.bankId]?.contains(entry.questionKey) == true }
            .distinctBy { it.bankId + "#" + it.questionKey }
    }

    private fun sanitizeFavoriteEntry(entry: FavoriteQuestionEntry): FavoriteQuestionEntry {
        return entry.copy(
            question = sanitizeQuestion(entry.question),
            favoritedAt = entry.favoritedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
    }

    private fun sanitizeFavoriteEntries(entries: List<FavoriteQuestionEntry>, banks: List<QuizBank>): List<FavoriteQuestionEntry> {
        val validBankIds = banks.map { it.id }.toSet()
        return entries
            .filter { entry -> entry.bankId in validBankIds && entry.question.id.isNotBlank() }
            .map(::sanitizeFavoriteEntry)
            .distinctBy { it.bankId + "#" + it.question.id }
    }

    private fun questionKey(question: Question): String {
        return question.id.ifBlank {
            listOf(
                question.type.name,
                question.question.trim(),
                question.options.joinToString("|") { option -> "${option.key}:${option.text}" },
                question.answer.joinToString("|")
            ).joinToString("#").lowercase(Locale.ROOT)
        }
    }

    private fun sanitizeQuestion(question: Question): Question {
        val cleanQuestion = stripEmbeddedAnswerBracket(question.question, question.answer)
        return if (cleanQuestion == question.question) question else question.copy(question = cleanQuestion)
    }

    private fun stripEmbeddedAnswerBracket(stem: String, answer: List<String>): String {
        if (stem.isBlank() || answer.isEmpty()) return stem
        val expected = answer.map { it.trim().uppercase() }.filter { it.isNotBlank() }.sorted()
        if (expected.isEmpty()) return stem

        val match = Regex(
            """[（(]\s*([A-Ga-g]{1,7}|正确|错误|对|错|是|否|√|×|True|False)\s*[）)]""",
            RegexOption.IGNORE_CASE
        ).findAll(stem).toList().asReversed().firstOrNull { hit ->
            embeddedAnswerToKeys(hit.groupValues[1]) == expected
        } ?: return stem

        return stem.removeRange(match.range)
            .replace(Regex("""\s+([?？。；;，,])"""), "$1")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd('，', ',', '；', ';', ':', '：')
            .trim()
    }

    private fun embeddedAnswerToKeys(raw: String): List<String> {
        val value = raw.trim().uppercase()
        return when {
            value.matches(Regex("""^[A-G]{1,7}$""")) -> value.map { it.toString() }.distinct().sorted()
            value in listOf("正确", "对", "是", "√", "TRUE", "T") -> listOf("A")
            value in listOf("错误", "错", "否", "×", "X", "FALSE", "F") -> listOf("B")
            else -> emptyList()
        }
    }

    private fun scoreForExamQuestion(question: Question): Double {
        return question.score ?: examTypeScores[question.type] ?: defaultExamTypeScores()[question.type] ?: 1.0
    }

    private fun defaultExamTypeScores(): Map<QuestionType, Double> = mapOf(
        QuestionType.SINGLE to 1.0,
        QuestionType.MULTIPLE to 2.0,
        QuestionType.JUDGE to 1.0,
        QuestionType.BLANK to 2.0,
        QuestionType.SHORT to 5.0
    )

    private fun evaluateQuestion(question: Question, userAnswer: List<String>): QuestionCheckResult {
        val correctAnswer = question.answer.sorted()
        val normalizedUserAnswer = userAnswer.sorted()
        val isObjective = question.type == QuestionType.SINGLE ||
            question.type == QuestionType.MULTIPLE ||
            question.type == QuestionType.JUDGE

        val correct = isObjective &&
            normalizedUserAnswer.isNotEmpty() &&
            normalizedUserAnswer == correctAnswer

        return QuestionCheckResult(
            question = question,
            userAnswer = userAnswer,
            correct = correct,
            answerText = question.answer.joinToString(" / ").ifBlank { "未识别答案" }
        )
    }

    private fun recordPracticeResult(bank: QuizBank?, question: Question, result: QuestionCheckResult) {
        studyRecords.add(
            0,
            StudyRecord(
                id = "practice_${question.id}_${System.currentTimeMillis()}",
                bankId = bank?.id,
                bankName = bank?.name ?: "未命名题库",
                source = "练习",
                title = question.question.take(24),
                total = 1,
                correct = if (result.correct) 1 else 0,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun addWrongQuestion(
        bank: QuizBank?,
        question: Question,
        userAnswer: List<String>,
        source: String
    ) {
        val bankId = bank?.id ?: return
        val bankName = bank.name
        val now = System.currentTimeMillis()
        val index = wrongBook.indexOfFirst { it.bankId == bankId && it.question.id == question.id }
        if (index >= 0) {
            val current = wrongBook[index]
            val nextWrongCount = current.wrongCount + 1
            wrongBook[index] = current.copy(
                bankName = bankName,
                question = question,
                lastAnswer = userAnswer,
                source = source,
                timestamp = now,
                wrongCount = nextWrongCount,
                reviewRightCount = 0,
                streakCorrectCount = 0,
                lastWrongAt = now,
                status = WrongStatus.NOT_MASTERED.label,
                lastReviewedAt = now,
                nextReviewAt = nextDayStart(now),
                reviewLevel = 0
            )
        } else {
            wrongBook.add(
                0,
                WrongQuestionEntry(
                    bankId = bankId,
                    bankName = bankName,
                    question = question,
                    lastAnswer = userAnswer,
                    source = source,
                    timestamp = now,
                    wrongCount = 1,
                    rightCount = 0,
                    reviewRightCount = 0,
                    streakCorrectCount = 0,
                    lastWrongAt = now,
                    lastCorrectAt = null,
                    status = WrongStatus.NOT_MASTERED.label,
                    lastReviewedAt = now,
                    nextReviewAt = nextDayStart(now),
                    reviewLevel = 0
                )
            )
        }
    }

    private fun markWrongQuestionRight(bank: QuizBank?, question: Question) {
        val bankId = bank?.id ?: return
        val index = wrongBook.indexOfFirst { it.bankId == bankId && it.question.id == question.id }
        if (index < 0) return
        val now = System.currentTimeMillis()
        val current = wrongBook[index]
        val nextRightCount = current.rightCount + 1
        val nextReviewRightCount = current.reviewRightCount + 1
        val nextStreakCorrectCount = current.streakCorrectCount + 1
        val mastered = isWrongEntryMastered(nextReviewRightCount, nextStreakCorrectCount)
        val nextStatus = if (mastered) WrongStatus.MASTERED.label else WrongStatus.REVIEWING.label
        val nextReviewLevel = nextReviewLevelAfterCorrect(current, mastered)
        wrongBook[index] = current.copy(
            question = question,
            timestamp = now,
            rightCount = nextRightCount,
            reviewRightCount = nextReviewRightCount,
            streakCorrectCount = nextStreakCorrectCount,
            lastCorrectAt = now,
            status = nextStatus,
            lastReviewedAt = now,
            reviewLevel = nextReviewLevel,
            nextReviewAt = nextReviewTimeForLevel(now, nextReviewLevel)
        )
    }

    private fun isWrongEntryMastered(reviewRightCount: Int, streakCorrectCount: Int): Boolean {
        return streakCorrectCount >= 2
    }

    private fun currentPracticeWrongSource(): String {
        return when (practiceSourceLabel) {
            "错题本" -> "错题练习"
            "今日复习" -> "今日复习"
            "收藏夹" -> "收藏练习"
            else -> "练习"
        }
    }

    private fun bankForPracticeQuestion(question: Question): QuizBank? {
        val mappedBankId = practiceQuestionBankIds[question.id]
        return mappedBankId?.let { bankId -> banks.firstOrNull { it.id == bankId } } ?: activeBank()
    }

    private fun isWrongEntryDueForSmartReview(entry: WrongQuestionEntry, now: Long): Boolean {
        val dueAt = entry.nextReviewAt ?: when (entry.status) {
            WrongStatus.MASTERED.label -> return false
            else -> startOfDay(now)
        }
        return dueAt <= now
    }

    private fun nextReviewLevelAfterCorrect(entry: WrongQuestionEntry, mastered: Boolean): Int {
        return if (mastered) {
            (entry.reviewLevel.coerceAtLeast(1) + 1).coerceIn(2, 5)
        } else {
            1
        }
    }

    private fun nextReviewTimeForLevel(now: Long, level: Int): Long {
        val days = when (level.coerceAtLeast(0)) {
            0, 1 -> 1
            2 -> 3
            3 -> 7
            4 -> 15
            else -> 30
        }
        return startOfDay(now) + days * DAY_MILLIS
    }

    private fun startOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun nextDayStart(timestamp: Long): Long = startOfDay(timestamp) + DAY_MILLIS

    private fun uniqueBankNameForRename(rawName: String, bankId: String): String {
        val baseName = rawName.ifBlank { "未命名题库" }
        val existingNames = banks.filterNot { it.id == bankId }.map { it.name }.toSet()
        if (baseName !in existingNames) return baseName
        var index = 2
        var candidate: String
        do {
            candidate = "$baseName $index"
            index += 1
        } while (candidate in existingNames)
        return candidate
    }

    private fun uniqueImportedBankName(rawName: String): String {
        val baseName = rawName.ifBlank { "导入题库" }
        val existingNames = banks.map { it.name }.toSet()
        if (baseName !in existingNames) return baseName
        var index = 2
        var candidate: String
        do {
            candidate = "$baseName 导入$index"
            index += 1
        } while (candidate in existingNames)
        return candidate
    }

    private fun normalizeReadingSizeMode(mode: String?): String {
        return when (mode) {
            "small", "standard", "large" -> mode
            else -> "standard"
        }
    }

    private fun normalizePracticeMode(mode: String): String {
        return when (mode) {
            PRACTICE_MODE_BATCH -> PRACTICE_MODE_BATCH
            else -> PRACTICE_MODE_INSTANT
        }
    }

    private fun normalizePracticeBatchSizeMode(mode: String): String {
        return when (mode) {
            "20", "custom" -> mode
            else -> "10"
        }
    }


    private fun normalizePracticeCountMode(mode: String): String {
        return when (mode) {
            "50", "100", "half", "all" -> mode
            else -> "custom"
        }
    }

    private fun normalizePracticeOrderMode(mode: String): String {
        return when (mode) {
            "ordered" -> "ordered"
            else -> "random"
        }
    }

    private fun normalizeExamCountMode(mode: String): String {
        return when (mode) {
            "50", "100", "all" -> mode
            else -> "custom"
        }
    }

    private fun normalizeExamGroupMode(mode: String): String {
        return when (mode) {
            "custom" -> "custom"
            else -> "random"
        }
    }

    private fun encodeQuestionTypeTextMap(values: Map<QuestionType, String>): String {
        return values.entries
            .sortedBy { it.key.name }
            .joinToString(";") { (type, value) -> "${type.name}=${value.trim()}" }
    }

    private fun decodeQuestionTypeTextMap(raw: String): Map<QuestionType, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(';')
            .mapNotNull { pair ->
                val index = pair.indexOf('=')
                if (index <= 0) return@mapNotNull null
                val type = runCatching { QuestionType.valueOf(pair.substring(0, index)) }.getOrNull() ?: return@mapNotNull null
                type to pair.substring(index + 1)
            }
            .toMap()
    }

    private fun Double.toPreferenceNumberText(): String {
        return if (this % 1.0 == 0.0) this.toInt().toString() else this.toString().trimEnd('0').trimEnd('.')
    }

    private fun persist() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_BANKS, banksToJson(banks))
            .putString(KEY_ACTIVE_BANK_ID, activeBankId)
            .putString(KEY_WRONG_BOOK, wrongBookToJson(wrongBook))
            .putString(KEY_SLASHED_QUESTIONS, slashedQuestionsToJson(slashedQuestions))
            .putString(KEY_FAVORITE_QUESTIONS, favoriteQuestionsToJson(favoriteQuestions))
            .putString(KEY_STUDY_RECORDS, studyRecordsToJson(studyRecords))
            .putBoolean(KEY_PRACTICE_NEXT_REQUIRES_RESULT, practiceNextRequiresResult)
            .putBoolean(KEY_REMEMBER_PRACTICE_SETTINGS, rememberPracticeSettingsEnabled)
            .putBoolean(KEY_SWIPE_NAVIGATION_ENABLED, swipeNavigationEnabled)
            .putBoolean(KEY_PRACTICE_AUTO_SUBMIT_ENABLED, practiceAutoSubmitEnabled)
            .putBoolean(KEY_PRACTICE_AUTO_NEXT_ENABLED, practiceAutoNextEnabled)
            .putBoolean(KEY_PRACTICE_INLINE_ANSWER_SETTINGS_ENABLED, practiceInlineAnswerSettingsEnabled)
            .putBoolean(KEY_PRACTICE_RECITE_MODE_ENABLED, practiceReciteModeEnabled)
            .putBoolean(KEY_PRACTICE_SLASH_ENABLED, practiceSlashEnabled)
            .putBoolean(KEY_PRACTICE_QUICK_EDIT_ENABLED, practiceQuickEditEnabled)
            .putBoolean(KEY_WRONG_BOOK_SMART_REVIEW_ENABLED, wrongBookSmartReviewEnabled)
            .putString(KEY_PRACTICE_PREFERRED_COUNT_MODE, preferredPracticeQuestionCountMode)
            .putInt(KEY_PRACTICE_PREFERRED_CUSTOM_COUNT, preferredPracticeCustomQuestionCount)
            .putString(KEY_PRACTICE_PREFERRED_ORDER_MODE, preferredPracticeOrderMode)
            .putString(KEY_PRACTICE_PREFERRED_TYPE_NAMES, preferredPracticeTypeNames)
            .putString(KEY_PRACTICE_PREFERRED_MODE, preferredPracticeMode)
            .putString(KEY_PRACTICE_PREFERRED_BATCH_SIZE_MODE, preferredPracticeBatchSizeMode)
            .putInt(KEY_PRACTICE_PREFERRED_BATCH_CUSTOM_SIZE, preferredPracticeBatchCustomSize)
            .putBoolean(KEY_REMEMBER_EXAM_SETTINGS, rememberExamSettingsEnabled)
            .putString(KEY_EXAM_PREFERRED_COUNT_MODE, preferredExamQuestionCountMode)
            .putInt(KEY_EXAM_PREFERRED_CUSTOM_COUNT, preferredExamCustomQuestionCount)
            .putInt(KEY_EXAM_PREFERRED_DURATION_MINUTES, preferredExamDurationMinutes)
            .putString(KEY_EXAM_PREFERRED_GROUP_MODE, preferredExamGroupMode)
            .putString(KEY_EXAM_PREFERRED_TYPE_COUNTS, preferredExamTypeCountsText)
            .putString(KEY_EXAM_PREFERRED_TYPE_SCORES, preferredExamTypeScoresText)
            .putBoolean(KEY_STARTUP_SPLASH_ENABLED, startupSplashEnabled)
            .putBoolean(KEY_DARK_THEME_ENABLED, darkThemeEnabled)
            .putBoolean(KEY_SHIROHA_MODE_ENABLED, shirohaModeEnabled)
            .putString(KEY_QUESTION_FONT_SIZE_MODE, questionFontSizeMode)
            .putString(KEY_OPTION_FONT_SIZE_MODE, optionFontSizeMode)
            .putBoolean(KEY_COMPACT_OPTIONS_ENABLED, compactOptionsEnabled)
            .putString(KEY_AI_PROVIDER, aiProvider)
            .putString(KEY_AI_API_BASE_URL, aiApiBaseUrl)
            .putString(KEY_AI_API_KEY, aiApiKey)
            .putString(KEY_AI_MODEL_NAME, aiModelName)
            .putBoolean(KEY_AI_REFACTOR_ENABLED, aiRefactorEnabled)
            .putBoolean(KEY_AI_REVIEW_ENABLED, aiReviewEnabled)
            .putBoolean(KEY_AI_ANALYSIS_ENABLED, aiAnalysisEnabled)
            .putBoolean(KEY_AI_ONLY_ANOMALY, aiOnlyAnomaly)
            .putBoolean(KEY_AI_REQUIRE_CONFIRM, aiRequireConfirm)
            .putInt(KEY_AI_MAX_QUESTIONS, aiMaxQuestions)
            .putInt(KEY_AI_TIMEOUT_SECONDS, aiTimeoutSeconds)
            .putInt(KEY_AI_REFACTOR_MAX_CHARS, aiRefactorMaxChars)
            .apply()
    }

    private fun banksToJson(banks: List<QuizBank>, assetMapping: MutableMap<String, BackupAsset>? = null): String {
        val bankArray = JSONArray()
        banks.forEach { bank ->
            val bankJson = JSONObject()
            bankJson.put("id", bank.id)
            bankJson.put("name", bank.name)
            bankJson.put("questions", questionsToJsonArray(bank.questions, assetMapping))
            bankArray.put(bankJson)
        }
        return bankArray.toString()
    }

    private fun wrongBookToJson(entries: List<WrongQuestionEntry>, assetMapping: MutableMap<String, BackupAsset>? = null): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val item = JSONObject()
            item.put("bankId", entry.bankId)
            item.put("bankName", entry.bankName)
            item.put("source", entry.source)
            item.put("timestamp", entry.timestamp)
            item.put("lastAnswer", JSONArray(entry.lastAnswer))
            item.put("wrongCount", entry.wrongCount)
            item.put("rightCount", entry.rightCount)
            item.put("reviewRightCount", entry.reviewRightCount)
            item.put("streakCorrectCount", entry.streakCorrectCount)
            item.put("lastWrongAt", entry.lastWrongAt)
            if (entry.lastCorrectAt != null) item.put("lastCorrectAt", entry.lastCorrectAt)
            if (entry.lastReviewedAt != null) item.put("lastReviewedAt", entry.lastReviewedAt)
            if (entry.nextReviewAt != null) item.put("nextReviewAt", entry.nextReviewAt)
            item.put("reviewLevel", entry.reviewLevel)
            item.put("status", entry.status)
            item.put("question", questionToJson(entry.question, assetMapping))
            array.put(item)
        }
        return array.toString()
    }

    private fun slashedQuestionsToJson(entries: List<SlashedQuestionEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val item = JSONObject()
            item.put("bankId", entry.bankId)
            item.put("questionKey", entry.questionKey)
            item.put("slashedAt", entry.slashedAt)
            array.put(item)
        }
        return array.toString()
    }

    private fun favoriteQuestionsToJson(entries: List<FavoriteQuestionEntry>, assetMapping: MutableMap<String, BackupAsset>? = null): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val item = JSONObject()
            item.put("bankId", entry.bankId)
            item.put("bankName", entry.bankName)
            item.put("favoritedAt", entry.favoritedAt)
            item.put("question", questionToJson(entry.question, assetMapping))
            array.put(item)
        }
        return array.toString()
    }

    private fun studyRecordsToJson(records: List<StudyRecord>, assetMapping: MutableMap<String, BackupAsset>? = null): String {
        val array = JSONArray()
        records.forEach { record ->
            val item = JSONObject()
            item.put("id", record.id)
            item.put("bankId", record.bankId)
            item.put("bankName", record.bankName)
            item.put("source", record.source)
            item.put("title", record.title)
            item.put("total", record.total)
            item.put("correct", record.correct)
            item.put("timestamp", record.timestamp)
            item.put("durationSeconds", record.durationSeconds)
            item.put("autoSubmitted", record.autoSubmitted)
            item.put("startedAt", record.startedAt)
            item.put("earnedScore", record.earnedScore)
            item.put("totalScore", record.totalScore)
            item.put("questionResults", studyQuestionResultsToJson(record.questionResults, assetMapping))
            array.put(item)
        }
        return array.toString()
    }


    private fun studyQuestionResultsToJson(results: List<StudyQuestionResult>, assetMapping: MutableMap<String, BackupAsset>? = null): JSONArray {
        val array = JSONArray()
        results.forEach { result ->
            val item = JSONObject()
            item.put("question", questionToJson(result.question, assetMapping))
            item.put("userAnswer", JSONArray(result.userAnswer))
            item.put("correct", result.correct)
            item.put("answerText", result.answerText)
            item.put("earnedScore", result.earnedScore)
            item.put("maxScore", result.maxScore)
            array.put(item)
        }
        return array
    }

    private fun parseBanksJson(text: String?): List<QuizBank> {
        if (text.isNullOrBlank()) return emptyList()
        val bankArray = JSONArray(text)

        return buildList {
            for (i in 0 until bankArray.length()) {
                val bankJson = bankArray.optJSONObject(i) ?: continue
                add(
                    QuizBank(
                        id = bankJson.optString("id"),
                        name = bankJson.optString("name"),
                        questions = parseQuestionsArray(bankJson.optJSONArray("questions"))
                    )
                )
            }
        }
    }

    private fun parseWrongBookJson(text: String?): List<WrongQuestionEntry> {
        if (text.isNullOrBlank()) return emptyList()
        val array = JSONArray(text)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val questionJson = item.optJSONObject("question") ?: continue
                val answerArray = item.optJSONArray("lastAnswer") ?: JSONArray()
                val lastAnswer = buildList {
                    for (index in 0 until answerArray.length()) {
                        add(answerArray.optString(index))
                    }
                }
                val normalizedStatus = WrongStatus.normalize(item.optString("status", WrongStatus.NOT_MASTERED.label))
                val fallbackReviewRightCount = if (normalizedStatus == WrongStatus.MASTERED.label) 2 else 0
                val fallbackStreakCorrectCount = if (normalizedStatus == WrongStatus.MASTERED.label) 2 else 0
                add(
                    WrongQuestionEntry(
                        bankId = item.optString("bankId"),
                        bankName = item.optString("bankName"),
                        question = parseQuestion(questionJson),
                        lastAnswer = lastAnswer,
                        source = item.optString("source"),
                        timestamp = item.optLong("timestamp"),
                        wrongCount = item.optInt("wrongCount", 1),
                        rightCount = item.optInt("rightCount", 0),
                        reviewRightCount = item.optInt("reviewRightCount", fallbackReviewRightCount),
                        streakCorrectCount = item.optInt("streakCorrectCount", fallbackStreakCorrectCount),
                        lastWrongAt = item.optLong("lastWrongAt", item.optLong("timestamp")),
                        lastCorrectAt = if (item.has("lastCorrectAt") && !item.isNull("lastCorrectAt")) item.optLong("lastCorrectAt") else null,
                        status = normalizedStatus,
                        lastReviewedAt = if (item.has("lastReviewedAt") && !item.isNull("lastReviewedAt")) item.optLong("lastReviewedAt") else null,
                        nextReviewAt = if (item.has("nextReviewAt") && !item.isNull("nextReviewAt")) item.optLong("nextReviewAt") else null,
                        reviewLevel = item.optInt("reviewLevel", if (normalizedStatus == WrongStatus.MASTERED.label) 2 else 0)
                    )
                )
            }
        }
    }

    private fun parseSlashedQuestionsJson(text: String?): List<SlashedQuestionEntry> {
        if (text.isNullOrBlank()) return emptyList()
        val array = JSONArray(text)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val bankId = item.optString("bankId")
                val key = item.optString("questionKey")
                if (bankId.isBlank() || key.isBlank()) continue
                add(
                    SlashedQuestionEntry(
                        bankId = bankId,
                        questionKey = key,
                        slashedAt = item.optLong("slashedAt", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private fun parseFavoriteQuestionsJson(text: String?): List<FavoriteQuestionEntry> {
        if (text.isNullOrBlank()) return emptyList()
        val array = JSONArray(text)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val questionJson = item.optJSONObject("question") ?: continue
                val bankId = item.optString("bankId")
                if (bankId.isBlank()) continue
                add(
                    FavoriteQuestionEntry(
                        bankId = bankId,
                        bankName = item.optString("bankName"),
                        question = parseQuestion(questionJson),
                        favoritedAt = item.optLong("favoritedAt", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private fun parseStudyRecordsJson(text: String?): List<StudyRecord> {
        if (text.isNullOrBlank()) return emptyList()
        val array = JSONArray(text)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    StudyRecord(
                        id = item.optString("id"),
                        bankId = item.optString("bankId").ifBlank { null },
                        bankName = item.optString("bankName"),
                        source = item.optString("source"),
                        title = item.optString("title"),
                        total = item.optInt("total"),
                        correct = item.optInt("correct"),
                        timestamp = item.optLong("timestamp"),
                        durationSeconds = if (item.has("durationSeconds") && !item.isNull("durationSeconds")) {
                            item.optInt("durationSeconds")
                        } else {
                            null
                        },
                        autoSubmitted = item.optBoolean("autoSubmitted"),
                        startedAt = if (item.has("startedAt") && !item.isNull("startedAt")) {
                            item.optLong("startedAt")
                        } else {
                            val durationSeconds = if (item.has("durationSeconds") && !item.isNull("durationSeconds")) item.optInt("durationSeconds") else 0
                            item.optLong("timestamp") - durationSeconds * 1000L
                        },
                        earnedScore = if (item.has("earnedScore") && !item.isNull("earnedScore")) item.optDouble("earnedScore") else null,
                        totalScore = if (item.has("totalScore") && !item.isNull("totalScore")) item.optDouble("totalScore") else null,
                        questionResults = parseStudyQuestionResults(item.optJSONArray("questionResults"))
                    )
                )
            }
        }
    }


    private fun parseStudyQuestionResults(array: JSONArray?): List<StudyQuestionResult> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val questionJson = item.optJSONObject("question") ?: continue
                val userAnswerArray = item.optJSONArray("userAnswer") ?: JSONArray()
                val userAnswer = buildList {
                    for (index in 0 until userAnswerArray.length()) {
                        add(userAnswerArray.optString(index))
                    }
                }
                add(
                    StudyQuestionResult(
                        question = parseQuestion(questionJson),
                        userAnswer = userAnswer,
                        correct = item.optBoolean("correct"),
                        answerText = item.optString("answerText"),
                        earnedScore = if (item.has("earnedScore") && !item.isNull("earnedScore")) item.optDouble("earnedScore") else null,
                        maxScore = if (item.has("maxScore") && !item.isNull("maxScore")) item.optDouble("maxScore") else null
                    )
                )
            }
        }
    }

    private fun parseQuestionsArray(array: JSONArray?): List<Question> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val questionJson = array.optJSONObject(i) ?: continue
                add(parseQuestion(questionJson))
            }
        }
    }

    private fun parseQuestion(questionJson: JSONObject): Question {
        val optionsArray = questionJson.optJSONArray("options") ?: JSONArray()
        val answersArray = questionJson.optJSONArray("answer") ?: JSONArray()

        val options = buildList {
            for (k in 0 until optionsArray.length()) {
                val optionJson = optionsArray.optJSONObject(k) ?: continue
                add(
                    Option(
                        key = optionJson.optString("key"),
                        text = optionJson.optString("text")
                    )
                )
            }
        }

        val answers = buildList {
            for (k in 0 until answersArray.length()) {
                add(answersArray.optString(k))
            }
        }

        val imagesArray = questionJson.optJSONArray("images") ?: JSONArray()
        val images = buildList {
            for (k in 0 until imagesArray.length()) {
                val imageJson = imagesArray.optJSONObject(k) ?: continue
                val path = listOf(
                    imageJson.optString("localPath"),
                    imageJson.optString("dataUrl"),
                    imageJson.optString("dataUri"),
                    imageJson.optString("src"),
                    imageJson.optString("url")
                ).firstOrNull { it.isNotBlank() }.orEmpty()
                if (path.isBlank()) continue
                add(
                    QuestionImage(
                        id = imageJson.optString("id"),
                        localPath = path,
                        sourceName = listOf(
                            imageJson.optString("sourceName"),
                            imageJson.optString("name"),
                            imageJson.optString("alt")
                        ).firstOrNull { it.isNotBlank() }.orEmpty(),
                        order = imageJson.optInt("order", k + 1),
                        width = if (imageJson.has("width") && !imageJson.isNull("width")) imageJson.optInt("width") else null,
                        height = if (imageJson.has("height") && !imageJson.isNull("height")) imageJson.optInt("height") else null,
                        sizeBytes = imageJson.optLong("sizeBytes")
                    )
                )
            }
        }

        return sanitizeQuestion(
            Question(
                id = questionJson.optString("id"),
                number = questionJson.optString("number"),
                type = parseQuestionType(questionJson.optString("type")),
                question = questionJson.optString("question"),
                options = options,
                answer = answers,
                analysis = questionJson.optString("analysis"),
                category = questionJson.optString("category"),
                images = images,
                score = if (questionJson.has("score")) questionJson.optDouble("score") else null
            )
        )
    }

    private fun parseQuestionType(rawType: String?): QuestionType {
        val value = rawType.orEmpty().trim()
        if (value.isBlank()) return QuestionType.SINGLE
        return when (value.lowercase(Locale.ROOT).replace("-", "_").replace(" ", "_")) {
            "single", "single_choice", "singlechoice", "choice", "radio" -> QuestionType.SINGLE
            "multiple", "multiple_choice", "multiplechoice", "multi", "checkbox" -> QuestionType.MULTIPLE
            "judge", "judgement", "judgment", "true_false", "truefalse", "boolean" -> QuestionType.JUDGE
            "blank", "fill_blank", "fillblank", "fill_in_blank" -> QuestionType.BLANK
            "short", "subjective", "essay", "qa", "question_answer" -> QuestionType.SHORT
            else -> runCatching { QuestionType.valueOf(value.uppercase(Locale.ROOT)) }.getOrDefault(QuestionType.SINGLE)
        }
    }

    private fun questionsToJsonArray(questions: List<Question>, assetMapping: MutableMap<String, BackupAsset>? = null): JSONArray {
        val questionsArray = JSONArray()
        questions.forEach { question ->
            questionsArray.put(questionToJson(question, assetMapping))
        }
        return questionsArray
    }

    private fun questionToJson(question: Question, assetMapping: MutableMap<String, BackupAsset>? = null): JSONObject {
        val questionJson = JSONObject()
        questionJson.put("id", question.id)
        questionJson.put("number", question.number)
        questionJson.put("type", question.type.name)
        questionJson.put("question", question.question)
        questionJson.put("analysis", question.analysis)
        questionJson.put("category", question.category)
        if (question.score != null) questionJson.put("score", question.score)

        val optionsArray = JSONArray()
        question.options.forEach { option ->
            val optionJson = JSONObject()
            optionJson.put("key", option.key)
            optionJson.put("text", option.text)
            optionsArray.put(optionJson)
        }
        questionJson.put("options", optionsArray)

        val answersArray = JSONArray()
        question.answer.forEach { answersArray.put(it) }
        questionJson.put("answer", answersArray)

        val imagesArray = JSONArray()
        question.images.forEach { image ->
            val imageJson = JSONObject()
            imageJson.put("id", image.id)
            val exportPath = registerBackupAsset(image, assetMapping)
            imageJson.put("localPath", exportPath ?: image.localPath)
            imageJson.put("sourceName", image.sourceName)
            imageJson.put("order", image.order)
            imageJson.put("width", image.width)
            imageJson.put("height", image.height)
            imageJson.put("sizeBytes", image.sizeBytes)
            imagesArray.put(imageJson)
        }
        questionJson.put("images", imagesArray)
        return questionJson
    }
    private fun registerBackupAsset(image: QuestionImage, assetMapping: MutableMap<String, BackupAsset>?): String? {
        if (assetMapping == null || image.localPath.isBlank()) return null
        val file = File(image.localPath)
        if (!file.exists() || !file.isFile) return null
        val ext = file.extension.ifBlank { File(image.sourceName).extension.ifBlank { "bin" } }
        val safeId = image.id.ifBlank { file.nameWithoutExtension }
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
            .take(80)
            .ifBlank { "asset" }
        val backupPath = "assets/${safeId}.${ext}"
        assetMapping[image.localPath] = BackupAsset(backupPath = backupPath, file = file)
        return backupPath
    }

}
