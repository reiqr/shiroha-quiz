package com.yiqiu.shirohaquiz.state

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionImage
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.util.LauncherIconSwitcher
import com.yiqiu.shirohaquiz.util.SafeZipReader
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val DEFAULT_BANK_GROUP_NAME = "未分组"

data class QuizBank(
    val id: String,
    val name: String,
    val questions: List<Question>,
    val groupName: String = DEFAULT_BANK_GROUP_NAME
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
    val userBlankAnswers: List<String> = emptyList(),
    val correct: Boolean,
    val answerText: String,
    val earnedScore: Double? = null,
    val maxScore: Double? = null,
    val autoScored: Boolean = true,
    val sourceBankId: String? = null,
    val sourceBankName: String? = null
)

data class PracticeQuestionSource(
    val bankId: String,
    val bankName: String,
    val groupName: String,
    val question: Question
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
    val questionResults: List<StudyQuestionResult> = emptyList(),
    val scopeType: String? = null,
    val scopeName: String? = null
)

data class QuestionCheckResult(
    val question: Question,
    val userAnswer: List<String>,
    val userBlankAnswers: List<String> = emptyList(),
    val correct: Boolean,
    val answerText: String,
    val autoScored: Boolean = true
)

object QuizRepository {
    data class JsonImportPreview(
        val banks: List<QuizBank>,
        val kind: String,
        val message: String,
        val importBanksOnly: Boolean = true
    )

    const val PRACTICE_MODE_INSTANT = "instant_feedback"
    const val PRACTICE_MODE_BATCH = "batch_review"
    const val PRACTICE_ORDER_RANDOM = "random"
    const val PRACTICE_ORDER_SEQUENTIAL = "ordered"
    const val SEQUENTIAL_START_LAST = "last"
    const val SEQUENTIAL_START_FIRST = "first"
    const val SEQUENTIAL_START_CUSTOM = "custom"
    const val WRONG_BOOK_SCOPE_CURRENT_BANK = "current_bank"
    const val WRONG_BOOK_SCOPE_ALL_BANKS = "all_banks"
    const val PRACTICE_SCOPE_BANK = "BANK"
    const val PRACTICE_SCOPE_GROUP = "GROUP"

    private const val PREFS_NAME = "shiroha_quiz_native"
    private const val KEY_BANKS = "banks"
    private const val KEY_ACTIVE_BANK_ID = "active_bank_id"
    private const val KEY_PRACTICE_SCOPE_TYPE = "practice_scope_type"
    private const val KEY_PRACTICE_SCOPE_VALUE = "practice_scope_value"
    private const val KEY_WRONG_BOOK = "wrong_book"
    private const val KEY_SLASHED_QUESTIONS = "slashed_questions"
    private const val KEY_FAVORITE_QUESTIONS = "favorite_questions"
    private const val KEY_STUDY_RECORDS = "study_records"
    private const val KEY_PRACTICE_NEXT_REQUIRES_RESULT = "practice_next_requires_result"
    private const val KEY_REMEMBER_PRACTICE_SETTINGS = "remember_practice_settings"
    private const val KEY_SWIPE_NAVIGATION_ENABLED = "swipe_navigation_enabled"
    private const val KEY_PRACTICE_AUTO_SUBMIT_ENABLED = "practice_auto_submit_enabled"
    private const val KEY_PRACTICE_AUTO_NEXT_ENABLED = "practice_auto_next_enabled"
    private const val KEY_PRACTICE_BATCH_AUTO_NEXT_ENABLED = "practice_batch_auto_next_enabled"
    private const val KEY_PRACTICE_INLINE_ANSWER_SETTINGS_ENABLED = "practice_inline_answer_settings_enabled"
    private const val KEY_PRACTICE_RECITE_MODE_ENABLED = "practice_recite_mode_enabled"
    private const val KEY_PRACTICE_SLASH_ENABLED = "practice_slash_enabled"
    private const val KEY_PRACTICE_QUICK_EDIT_ENABLED = "practice_quick_edit_enabled"
    private const val KEY_PRACTICE_OPTION_SHUFFLE_ENABLED = "practice_option_shuffle_enabled"
    private const val KEY_SCREEN_READER_ASSIST_ENABLED = "screen_reader_assist_enabled"
    private const val KEY_EXAM_AUTO_NEXT_ENABLED = "exam_auto_next_enabled"
    private const val KEY_EXAM_OPTION_SHUFFLE_ENABLED = "exam_option_shuffle_enabled"
    private const val KEY_WRONG_BOOK_SMART_REVIEW_ENABLED = "wrong_book_smart_review_enabled"
    private const val KEY_WRONG_BOOK_SCOPE_MODE = "wrong_book_scope_mode"
    private const val KEY_WRONG_BOOK_ADVANCED_REVIEW_SETTINGS_ENABLED = "wrong_book_advanced_review_settings_enabled"
    private const val KEY_PRACTICE_PREFERRED_COUNT_MODE = "practice_preferred_count_mode"
    private const val KEY_PRACTICE_PREFERRED_CUSTOM_COUNT = "practice_preferred_custom_count"
    private const val KEY_PRACTICE_PREFERRED_ORDER_MODE = "practice_preferred_order_mode"
    private const val KEY_PRACTICE_SEQUENTIAL_PROGRESS = "practice_sequential_progress"
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
    private const val KEY_CARD_ANIMATION_ENABLED = "card_animation_enabled"
    private const val KEY_SHIROHA_MODE_ENABLED = "shiroha_mode_enabled"
    private const val KEY_TABLET_SIDE_NAVIGATION_ENABLED = "tablet_side_navigation_enabled"
    private const val KEY_QUESTION_ID_INTEGRITY_MIGRATION_VERSION = "question_id_integrity_migration_version"
    private const val QUESTION_ID_INTEGRITY_MIGRATION_VERSION = 1
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
    private const val KEY_AI_SINGLE_QUESTION_ANALYSIS_ENABLED = "ai_single_question_analysis_enabled"
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
    var practiceScopeType by mutableStateOf(PRACTICE_SCOPE_BANK)
        private set
    var practiceScopeValue by mutableStateOf("")
        private set
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
    var practiceBatchAutoNextEnabled by mutableStateOf(false)
        private set
    var practiceInlineAnswerSettingsEnabled by mutableStateOf(false)
        private set
    var practiceReciteModeEnabled by mutableStateOf(false)
        private set
    var practiceSlashEnabled by mutableStateOf(false)
        private set
    var practiceQuickEditEnabled by mutableStateOf(false)
        private set
    var practiceOptionShuffleEnabled by mutableStateOf(false)
        private set
    var screenReaderAssistEnabled by mutableStateOf(false)
        private set
    var examAutoNextEnabled by mutableStateOf(false)
        private set
    var examOptionShuffleEnabled by mutableStateOf(false)
        private set
    var wrongBookSmartReviewEnabled by mutableStateOf(false)
        private set
    var wrongBookScopeMode by mutableStateOf(WRONG_BOOK_SCOPE_ALL_BANKS)
        private set
    var wrongBookAdvancedReviewSettingsEnabled by mutableStateOf(true)
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
    var cardAnimationEnabled by mutableStateOf(true)
        private set
    var shirohaModeEnabled by mutableStateOf(false)
        private set
    var tabletSideNavigationEnabled by mutableStateOf(false)
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
    var aiSingleQuestionAnalysisEnabled by mutableStateOf(false)
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
    val practiceSequentialProgress = mutableStateMapOf<String, Int>()
    private val practiceQuestionBankIds = mutableStateMapOf<String, String>()
    private var practiceQuestionSessionKeys by mutableStateOf<List<String>>(emptyList())
    private var practiceStartedAt by mutableStateOf<Long?>(null)
    private var practiceSessionScopeType: String? = null
    private var practiceSessionScopeName: String? = null
    private var practiceSequentialBankId: String? = null
    private var practiceSequentialStartIndex: Int? = null
    var practiceOptionShuffleSeed by mutableStateOf(0L)
        private set
    private var practiceSequentialNextIndexAfterComplete: Int? = null

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
    var examOptionShuffleSeed by mutableStateOf(0L)
        private set
    var examOptionShuffleSessionEnabled by mutableStateOf(false)
        private set
    val examAnswers = mutableStateMapOf<String, List<String>>()
    var examTypeScores by mutableStateOf(defaultExamTypeScores())
        private set

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val needsQuestionIdIntegrityMigration =
            prefs.getInt(KEY_QUESTION_ID_INTEGRITY_MIGRATION_VERSION, 0) < QUESTION_ID_INTEGRITY_MIGRATION_VERSION
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
        val restoredSequentialProgress = runCatching {
            parseSequentialProgressJson(prefs.getString(KEY_PRACTICE_SEQUENTIAL_PROGRESS, null))
        }.getOrDefault(emptyMap())

        banks.clear()
        wrongBook.clear()
        slashedQuestions.clear()
        favoriteQuestions.clear()
        studyRecords.clear()
        practiceSequentialProgress.clear()

        val restoredBankRepairs = if (needsQuestionIdIntegrityMigration) {
            restoredBanks
                .map(::sanitizeBankWithQuestionIdRepair)
                .filterNot { result ->
                    val bank = result.bank
                    bank.id == "demo-bank" || (bank.name == "示例题库" && bank.questions.isEmpty())
                }
        } else {
            emptyList()
        }
        val sanitizedRestoredBanks = if (needsQuestionIdIntegrityMigration) {
            restoredBankRepairs.map { it.bank }
        } else {
            restoredBanks
                .map(::sanitizeBankWithoutQuestionIdRepair)
                .filterNot { bank ->
                    bank.id == "demo-bank" || (bank.name == "示例题库" && bank.questions.isEmpty())
                }
        }
        val restoredRepairByBankId = restoredBankRepairs
            .filter { it.idChanged }
            .associateBy { it.bank.id }
        val hasQuestionIdRepairs = restoredRepairByBankId.isNotEmpty()
        val migratedWrongBook = if (hasQuestionIdRepairs) {
            migrateWrongEntriesForQuestionIdRepair(restoredWrongBook, restoredRepairByBankId)
        } else {
            restoredWrongBook
        }
        val migratedSlashedQuestions = if (hasQuestionIdRepairs) {
            migrateSlashedEntriesForQuestionIdRepair(restoredSlashedQuestions, restoredRepairByBankId)
        } else {
            restoredSlashedQuestions
        }
        val migratedFavoriteQuestions = if (hasQuestionIdRepairs) {
            migrateFavoriteEntriesForQuestionIdRepair(restoredFavoriteQuestions, restoredRepairByBankId)
        } else {
            restoredFavoriteQuestions
        }
        val migratedStudyRecords = if (hasQuestionIdRepairs) {
            migrateStudyRecordsForQuestionIdRepair(restoredStudyRecords, restoredRepairByBankId)
        } else {
            restoredStudyRecords
        }
        banks.addAll(sanitizedRestoredBanks)
        activeBankId = prefs.getString(KEY_ACTIVE_BANK_ID, sanitizedRestoredBanks.firstOrNull()?.id)
            ?.takeIf { id -> sanitizedRestoredBanks.any { it.id == id } }
            ?: sanitizedRestoredBanks.firstOrNull()?.id
        practiceScopeType = normalizePracticeScopeType(prefs.getString(KEY_PRACTICE_SCOPE_TYPE, PRACTICE_SCOPE_BANK))
        practiceScopeValue = prefs.getString(KEY_PRACTICE_SCOPE_VALUE, activeBankId.orEmpty()).orEmpty()
        ensureValidPracticeScope()
        practiceNextRequiresResult = prefs.getBoolean(KEY_PRACTICE_NEXT_REQUIRES_RESULT, false)
        rememberPracticeSettingsEnabled = prefs.getBoolean(KEY_REMEMBER_PRACTICE_SETTINGS, true)
        swipeNavigationEnabled = prefs.getBoolean(KEY_SWIPE_NAVIGATION_ENABLED, true)
        practiceAutoSubmitEnabled = prefs.getBoolean(KEY_PRACTICE_AUTO_SUBMIT_ENABLED, true)
        practiceAutoNextEnabled = prefs.getBoolean(KEY_PRACTICE_AUTO_NEXT_ENABLED, false)
        practiceBatchAutoNextEnabled = prefs.getBoolean(KEY_PRACTICE_BATCH_AUTO_NEXT_ENABLED, false)
        practiceInlineAnswerSettingsEnabled = prefs.getBoolean(KEY_PRACTICE_INLINE_ANSWER_SETTINGS_ENABLED, false)
        practiceReciteModeEnabled = prefs.getBoolean(KEY_PRACTICE_RECITE_MODE_ENABLED, false)
        practiceSlashEnabled = prefs.getBoolean(KEY_PRACTICE_SLASH_ENABLED, false)
        practiceQuickEditEnabled = prefs.getBoolean(KEY_PRACTICE_QUICK_EDIT_ENABLED, false)
        practiceOptionShuffleEnabled = prefs.getBoolean(KEY_PRACTICE_OPTION_SHUFFLE_ENABLED, false)
        screenReaderAssistEnabled = prefs.getBoolean(KEY_SCREEN_READER_ASSIST_ENABLED, false)
        examAutoNextEnabled = prefs.getBoolean(KEY_EXAM_AUTO_NEXT_ENABLED, false)
        examOptionShuffleEnabled = prefs.getBoolean(KEY_EXAM_OPTION_SHUFFLE_ENABLED, false)
        wrongBookSmartReviewEnabled = prefs.getBoolean(KEY_WRONG_BOOK_SMART_REVIEW_ENABLED, false)
        wrongBookScopeMode = normalizeWrongBookScopeMode(
            prefs.getString(KEY_WRONG_BOOK_SCOPE_MODE, WRONG_BOOK_SCOPE_ALL_BANKS) ?: WRONG_BOOK_SCOPE_ALL_BANKS
        )
        wrongBookAdvancedReviewSettingsEnabled = prefs.getBoolean(
            KEY_WRONG_BOOK_ADVANCED_REVIEW_SETTINGS_ENABLED,
            true
        )
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
        cardAnimationEnabled = prefs.getBoolean(KEY_CARD_ANIMATION_ENABLED, true)
        shirohaModeEnabled = prefs.getBoolean(KEY_SHIROHA_MODE_ENABLED, false)
        tabletSideNavigationEnabled = prefs.getBoolean(KEY_TABLET_SIDE_NAVIGATION_ENABLED, false)
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
        aiSingleQuestionAnalysisEnabled = prefs.getBoolean(KEY_AI_SINGLE_QUESTION_ANALYSIS_ENABLED, false)
        aiOnlyAnomaly = prefs.getBoolean(KEY_AI_ONLY_ANOMALY, true)
        aiRequireConfirm = prefs.getBoolean(KEY_AI_REQUIRE_CONFIRM, true)
        aiMaxQuestions = prefs.getInt(KEY_AI_MAX_QUESTIONS, 20).coerceIn(5, 100)
        aiTimeoutSeconds = prefs.getInt(KEY_AI_TIMEOUT_SECONDS, 60).coerceIn(15, 180)
        aiRefactorMaxChars = prefs.getInt(KEY_AI_REFACTOR_MAX_CHARS, 30000).coerceIn(5000, 80000)

        wrongBook.addAll(migratedWrongBook.map(::sanitizeWrongEntry))
        slashedQuestions.addAll(sanitizeSlashedEntries(migratedSlashedQuestions, sanitizedRestoredBanks))
        favoriteQuestions.addAll(sanitizeFavoriteEntries(migratedFavoriteQuestions, sanitizedRestoredBanks))
        practiceSequentialProgress.putAll(sanitizeSequentialProgress(restoredSequentialProgress, sanitizedRestoredBanks))
        studyRecords.addAll(migratedStudyRecords)
        val questionIdIntegrityChanged = needsQuestionIdIntegrityMigration && (
            restoredBankRepairs.any { it.idChanged } ||
                migratedWrongBook != restoredWrongBook ||
                migratedSlashedQuestions != restoredSlashedQuestions ||
                migratedFavoriteQuestions != restoredFavoriteQuestions ||
                migratedStudyRecords != restoredStudyRecords
            )
        if (sanitizedRestoredBanks != restoredBanks || questionIdIntegrityChanged) persist()
        if (needsQuestionIdIntegrityMigration) {
            prefs.edit()
                .putInt(KEY_QUESTION_ID_INTEGRITY_MIGRATION_VERSION, QUESTION_ID_INTEGRITY_MIGRATION_VERSION)
                .apply()
        }
    }

    fun importBank(
        context: Context,
        name: String,
        questions: List<Question>,
        groupName: String = DEFAULT_BANK_GROUP_NAME
    ) {
        appContext = context.applicationContext
        val cleanGroupName = normalizeBankGroupName(groupName)
        val cleanName = uniqueBankNameInGroup(name.trim().ifBlank { "导入题库" }, cleanGroupName)
        val importedAssetDir = File(context.filesDir, "question_assets/import_json_${System.currentTimeMillis()}").apply { mkdirs() }
        val bank = sanitizeBank(
            QuizBank(
                id = "bank_${System.currentTimeMillis()}",
                name = cleanName,
                questions = questions.map { question ->
                    normalizeImportedQuestionAssets(question, emptyMap(), importedAssetDir)
                },
                groupName = cleanGroupName
            )
        )
        banks += bank
        activeBankId = bank.id
        practiceScopeType = PRACTICE_SCOPE_BANK
        practiceScopeValue = bank.id
        resetPracticeState()
        resetExam()
        persist()
    }

    fun appendQuestionsToBank(context: Context, bankId: String, newQuestions: List<Question>): Boolean {
        appContext = context.applicationContext
        val bankIndex = banks.indexOfFirst { it.id == bankId }
        if (bankIndex < 0) return false
        val bank = banks[bankIndex]
        val importedAssetDir = File(context.filesDir, "question_assets/import_json_${System.currentTimeMillis()}").apply { mkdirs() }
        val appendedQuestions = sanitizeBank(
            bank.copy(
                questions = bank.questions + newQuestions.map { question ->
                    normalizeImportedQuestionAssets(question, emptyMap(), importedAssetDir)
                }
            )
        ).questions
        banks[bankIndex] = bank.copy(questions = appendedQuestions)
        if (activeBankId == bankId) {
            resetPracticeState()
            resetExam()
        }
        persist()
        return true
    }

    fun setActiveBank(context: Context, bankId: String) {
        appContext = context.applicationContext
        if (banks.none { it.id == bankId }) return
        activeBankId = bankId
        practiceScopeType = PRACTICE_SCOPE_BANK
        practiceScopeValue = bankId
        // The current practice session is a snapshot. Switching the default bank/scope
        // only affects the next session and must not discard an unfinished practice.
        resetExam()
        persist()
    }

    fun setPracticeGroupScope(context: Context, groupName: String): Boolean {
        appContext = context.applicationContext
        val cleanGroupName = normalizeBankGroupName(groupName)
        if (banks.none { normalizeBankGroupName(it.groupName) == cleanGroupName }) return false
        practiceScopeType = PRACTICE_SCOPE_GROUP
        practiceScopeValue = cleanGroupName
        // Keep an already-started practice unchanged; this scope is used next time.
        persist()
        return true
    }

    fun setPracticeBankScope(context: Context, bankId: String): Boolean {
        appContext = context.applicationContext
        if (banks.none { it.id == bankId }) return false
        practiceScopeType = PRACTICE_SCOPE_BANK
        practiceScopeValue = bankId
        // Keep an already-started practice unchanged; this scope is used next time.
        persist()
        return true
    }

    fun deleteBank(context: Context, bankId: String) {
        appContext = context.applicationContext
        val removingBank = banks.firstOrNull { it.id == bankId } ?: return
        val removedGroupName = normalizeBankGroupName(removingBank.groupName)
        val removingActive = activeBankId == bankId
        banks.removeAll { it.id == bankId }
        wrongBook.removeAll { it.bankId == bankId }
        slashedQuestions.removeAll { it.bankId == bankId }
        favoriteQuestions.removeAll { it.bankId == bankId }
        practiceSequentialProgress.remove(bankId)
        practiceSequentialProgress.remove("BANK:$bankId")
        studyRecords.removeAll { it.bankId == bankId }

        if (removingActive || banks.none { it.id == activeBankId }) {
            activeBankId = banks.firstOrNull()?.id
        }
        clampGroupSequentialProgress(removedGroupName)
        ensureValidPracticeScope()
        resetPracticeState()
        resetExam()
        persist()
    }


    fun renameBank(context: Context, bankId: String, newName: String): Boolean {
        val currentGroup = banks.firstOrNull { it.id == bankId }?.groupName ?: DEFAULT_BANK_GROUP_NAME
        return updateBankInfo(context, bankId, currentGroup, newName)
    }

    fun updateBankInfo(context: Context, bankId: String, newGroupName: String, newName: String): Boolean {
        appContext = context.applicationContext
        val index = banks.indexOfFirst { it.id == bankId }
        if (index < 0) return false
        val cleanGroupName = normalizeBankGroupName(newGroupName)
        val cleanName = uniqueBankNameForRename(
            rawName = newName.trim().ifBlank { "未命名题库" },
            groupName = cleanGroupName,
            bankId = bankId
        )
        val current = banks[index]
        val oldGroupName = normalizeBankGroupName(current.groupName)
        banks[index] = current.copy(name = cleanName, groupName = cleanGroupName)
        if (oldGroupName != cleanGroupName) {
            clampGroupSequentialProgress(oldGroupName)
            clampGroupSequentialProgress(cleanGroupName)
        }

        for (i in wrongBook.indices) {
            val entry = wrongBook[i]
            if (entry.bankId == bankId) {
                wrongBook[i] = entry.copy(bankName = cleanName)
            }
        }
        for (i in favoriteQuestions.indices) {
            val entry = favoriteQuestions[i]
            if (entry.bankId == bankId) {
                favoriteQuestions[i] = entry.copy(bankName = cleanName)
            }
        }
        for (i in studyRecords.indices) {
            val record = studyRecords[i]
            val updatedResults = record.questionResults.map { result ->
                if (result.sourceBankId == bankId) result.copy(sourceBankName = cleanName) else result
            }
            if (record.bankId == bankId || updatedResults != record.questionResults) {
                studyRecords[i] = record.copy(
                    bankName = if (record.bankId == bankId) cleanName else record.bankName,
                    questionResults = updatedResults
                )
            }
        }
        if (practiceScopeType == PRACTICE_SCOPE_GROUP && practiceScopeValue == oldGroupName && banks.none { normalizeBankGroupName(it.groupName) == oldGroupName }) {
            fallbackPracticeScopeToActiveBank()
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
        val cleanQuestions = sanitizeBank(bank.copy(questions = questions)).questions
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
        val bankProgressKey = "BANK:$bankId"
        val legacyProgress = practiceSequentialProgress.remove(bankId)
        val savedProgress = practiceSequentialProgress[bankProgressKey] ?: legacyProgress
        savedProgress?.let { index ->
            if (cleanQuestions.isEmpty()) {
                practiceSequentialProgress.remove(bankProgressKey)
            } else {
                practiceSequentialProgress[bankProgressKey] = index.coerceIn(0, cleanQuestions.lastIndex)
            }
        }
        val affectedGroupKey = "GROUP:${normalizeBankGroupName(banks[bankIndex].groupName)}"
        practiceSequentialProgress[affectedGroupKey]?.let { index ->
            val groupSize = banks
                .filter { normalizeBankGroupName(it.groupName) == normalizeBankGroupName(banks[bankIndex].groupName) }
                .sumOf { it.questions.count { question -> !isQuestionSlashed(it.id, question) } }
            if (groupSize <= 0) practiceSequentialProgress.remove(affectedGroupKey)
            else practiceSequentialProgress[affectedGroupKey] = index.coerceIn(0, groupSize - 1)
        }

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

    fun isGroupPracticeScope(): Boolean = practiceScopeType == PRACTICE_SCOPE_GROUP

    fun currentPracticeScopeKey(): String {
        ensureValidPracticeScope()
        return if (practiceScopeType == PRACTICE_SCOPE_GROUP) {
            "GROUP:${normalizeBankGroupName(practiceScopeValue)}"
        } else {
            "BANK:${currentPracticeScopeBank()?.id.orEmpty()}"
        }
    }

    fun currentPracticeScopeLabel(): String {
        ensureValidPracticeScope()
        return if (practiceScopeType == PRACTICE_SCOPE_GROUP) {
            normalizeBankGroupName(practiceScopeValue)
        } else {
            currentPracticeScopeBank()?.name ?: "当前题库"
        }
    }

    fun currentPracticeScopeSummary(): String {
        val scopeBanks = currentPracticeScopeBanks()
        val total = scopeBanks.sumOf { bank -> bank.questions.count { !isQuestionSlashed(bank.id, it) } }
        return if (practiceScopeType == PRACTICE_SCOPE_GROUP) {
            "${scopeBanks.size} 个题库 · $total 题"
        } else {
            "$total 题"
        }
    }

    fun currentPracticeScopeBanks(): List<QuizBank> {
        ensureValidPracticeScope()
        return if (practiceScopeType == PRACTICE_SCOPE_GROUP) {
            val cleanGroup = normalizeBankGroupName(practiceScopeValue)
            banks.filter { normalizeBankGroupName(it.groupName) == cleanGroup }
        } else {
            listOfNotNull(currentPracticeScopeBank())
        }
    }

    private fun currentPracticeScopeBank(): QuizBank? {
        val scoped = banks.firstOrNull { it.id == practiceScopeValue }
        return scoped ?: activeBank()
    }

    fun activePracticePoolSources(bank: QuizBank? = null): List<PracticeQuestionSource> {
        val scopeBanks = bank?.let(::listOf) ?: currentPracticeScopeBanks()
        return scopeBanks.flatMap { sourceBank ->
            sourceBank.questions
                .filterNot { isQuestionSlashed(sourceBank.id, it) }
                .map { question ->
                    PracticeQuestionSource(
                        bankId = sourceBank.id,
                        bankName = sourceBank.name,
                        groupName = normalizeBankGroupName(sourceBank.groupName),
                        question = question
                    )
                }
        }
    }

    fun currentPracticeQuestion(): Question? {
        val questions = activePracticeQuestions()
        if (questions.isEmpty()) return null
        val minIndex = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchStartIndex() else 0
        val maxIndex = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchEndIndex() else questions.lastIndex
        val safeIndex = practiceIndex.coerceIn(minIndex, maxIndex)
        if (safeIndex != practiceIndex) practiceIndex = safeIndex
        return questions[safeIndex]
    }

    fun currentPracticeSessionKey(): String? = practiceSessionKeyAt(practiceIndex)

    fun practiceSessionKeyAt(index: Int): String? = practiceQuestionSessionKeys.getOrNull(index)

    fun currentPracticeSourceBank(): QuizBank? = bankForPracticeIndex(practiceIndex)

    fun canSlashCurrentPracticeQuestion(): Boolean {
        if (!practiceSlashEnabled || currentPracticeQuestion() == null) return false
        return practiceSourceLabel !in setOf("错题本", "今日复习", "收藏夹")
    }

    fun activePracticeQuestions(): List<Question> {
        return practiceQuestions.ifEmpty { activePracticePoolQuestions() }
    }

    fun activePracticePoolQuestions(bank: QuizBank? = null): List<Question> {
        return activePracticePoolSources(bank).map { it.question }
    }

    fun practiceAvailableQuestionCount(bank: QuizBank? = null): Int = activePracticePoolSources(bank).size

    fun sequentialPracticeProgressIndex(bank: QuizBank? = null, allowedTypes: Set<QuestionType> = QuestionType.values().toSet()): Int {
        val sourceSize = sequentialPracticeSource(bank, allowedTypes).size
        if (sourceSize <= 0) return 0
        val progressKey = sequentialProgressKey(bank)
        return (practiceSequentialProgress[progressKey] ?: 0).coerceIn(0, sourceSize - 1)
    }

    fun sequentialPracticeRangePreview(
        questionCount: Int,
        allowedTypes: Set<QuestionType>,
        startMode: String = SEQUENTIAL_START_LAST,
        customStartNumber: Int = 1,
        bank: QuizBank? = null
    ): Pair<Int, Int>? {
        val source = sequentialPracticeSource(bank, allowedTypes)
        if (source.isEmpty()) return null
        val startIndex = resolveSequentialPracticeStartIndex(
            progressKey = sequentialProgressKey(bank),
            sourceSize = source.size,
            startMode = startMode,
            customStartNumber = customStartNumber
        )
        val count = questionCount.coerceIn(1, source.size)
        val endIndex = (startIndex + count - 1).coerceAtMost(source.lastIndex)
        return (startIndex + 1) to (endIndex + 1)
    }

    fun startSequentialPracticeSession(
        questionCount: Int,
        allowedTypes: Set<QuestionType>,
        startMode: String = SEQUENTIAL_START_LAST,
        customStartNumber: Int = 1,
        practiceMode: String = PRACTICE_MODE_INSTANT,
        batchGroupSize: Int = preferredPracticeBatchGroupSize()
    ): Boolean {
        val source = sequentialPracticeSource(null, allowedTypes)
        if (source.isEmpty()) return false
        val progressKey = sequentialProgressKey(null)
        val startIndex = resolveSequentialPracticeStartIndex(
            progressKey = progressKey,
            sourceSize = source.size,
            startMode = startMode,
            customStartNumber = customStartNumber
        )
        val count = questionCount.coerceIn(1, source.size)
        val selectedSources = source.drop(startIndex).take(count)
        if (selectedSources.isEmpty()) return false
        val started = startPracticeSession(
            questionCount = selectedSources.size,
            allowedTypes = allowedTypes.ifEmpty { objectiveQuestionTypes() },
            sourceItems = selectedSources,
            sourceLabel = currentPracticeScopeLabel(),
            randomize = false,
            practiceMode = practiceMode,
            batchGroupSize = batchGroupSize.coerceIn(1, selectedSources.size),
            sessionScopeType = practiceScopeType,
            sessionScopeName = currentPracticeScopeLabel()
        )
        if (started) {
            val nextIndex = startIndex + selectedSources.size
            practiceSequentialBankId = progressKey
            practiceSequentialStartIndex = startIndex
            practiceSequentialNextIndexAfterComplete = if (nextIndex >= source.size) 0 else nextIndex
        }
        return started
    }

    fun resetSequentialPracticeProgress(context: Context, bankId: String = "") {
        appContext = context.applicationContext
        val key = if (bankId.isNotBlank()) "BANK:$bankId" else currentPracticeScopeKey()
        practiceSequentialProgress[key] = 0
        persist()
    }

    private fun sequentialPracticeSource(bank: QuizBank?, allowedTypes: Set<QuestionType>): List<PracticeQuestionSource> {
        val selectedTypes = allowedTypes.ifEmpty { objectiveQuestionTypes() }
        return activePracticePoolSources(bank).filter { it.question.type in selectedTypes }
    }

    private fun sequentialProgressKey(bank: QuizBank?): String {
        return bank?.let { "BANK:${it.id}" } ?: currentPracticeScopeKey()
    }

    private fun resolveSequentialPracticeStartIndex(
        progressKey: String,
        sourceSize: Int,
        startMode: String,
        customStartNumber: Int
    ): Int {
        if (sourceSize <= 0) return 0
        return when (startMode) {
            SEQUENTIAL_START_FIRST -> 0
            SEQUENTIAL_START_CUSTOM -> (customStartNumber - 1).coerceIn(0, sourceSize - 1)
            else -> (practiceSequentialProgress[progressKey] ?: 0).coerceIn(0, sourceSize - 1)
        }
    }

    fun startPracticeSession(
        questionCount: Int,
        allowedTypes: Set<QuestionType>,
        sourceQuestions: List<Question>? = null,
        sourceLabel: String = "",
        randomize: Boolean = true,
        practiceMode: String = PRACTICE_MODE_INSTANT,
        batchGroupSize: Int = preferredPracticeBatchGroupSize(),
        sourceBankIds: Map<String, String>? = null,
        sourceItems: List<PracticeQuestionSource>? = null,
        sessionScopeType: String? = null,
        sessionScopeName: String? = null
    ): Boolean {
        val selectedTypes = allowedTypes.ifEmpty { objectiveQuestionTypes() }
        val fallbackBank = activeBank()
        val rawItems = when {
            sourceItems != null -> sourceItems
            sourceQuestions != null -> sourceQuestions.map { question ->
                val bankId = sourceBankIds?.get(question.id) ?: fallbackBank?.id.orEmpty()
                val bank = banks.firstOrNull { it.id == bankId } ?: fallbackBank
                PracticeQuestionSource(
                    bankId = bank?.id.orEmpty(),
                    bankName = bank?.name.orEmpty(),
                    groupName = normalizeBankGroupName(bank?.groupName),
                    question = question
                )
            }
            else -> activePracticePoolSources()
        }
        val filteredItems = rawItems.filter { it.question.type in selectedTypes }
        if (filteredItems.isEmpty()) return false
        val count = questionCount.coerceIn(1, filteredItems.size)
        val selectedItems = if (randomize) filteredItems.shuffled().take(count) else filteredItems.take(count)
        practiceQuestions = selectedItems.map { it.question }
        practiceQuestionSessionKeys = buildPracticeSessionKeys(selectedItems)
        practiceSourceLabel = sourceLabel.ifBlank { currentPracticeScopeLabel() }
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
        practiceOptionShuffleSeed = System.currentTimeMillis()
        selectedItems.forEachIndexed { index, item ->
            val sessionKey = practiceQuestionSessionKeys[index]
            if (item.bankId.isNotBlank()) practiceQuestionBankIds[sessionKey] = item.bankId
        }
        practiceSessionScopeType = when {
            !sessionScopeType.isNullOrBlank() -> normalizePracticeScopeType(sessionScopeType)
            sourceItems == null && sourceQuestions == null -> practiceScopeType
            else -> null
        }
        practiceSessionScopeName = sessionScopeName?.takeIf { it.isNotBlank() } ?: when {
            practiceSessionScopeType == PRACTICE_SCOPE_GROUP -> {
                if (sourceItems == null && sourceQuestions == null) currentPracticeScopeLabel() else practiceSourceLabel
            }
            selectedItems.map { it.bankId }.distinct().size == 1 -> selectedItems.firstOrNull()?.bankName
            else -> practiceSourceLabel
        }
        practiceStartedAt = System.currentTimeMillis()
        practiceSequentialBankId = null
        practiceSequentialStartIndex = null
        practiceSequentialNextIndexAfterComplete = null
        return true
    }

    private fun buildPracticeSessionKeys(items: List<PracticeQuestionSource>): List<String> {
        val occurrences = mutableMapOf<String, Int>()
        return items.map { item ->
            val base = "${item.bankId}#${item.question.id}"
            val occurrence = occurrences.getOrDefault(base, 0)
            occurrences[base] = occurrence + 1
            if (occurrence == 0) base else "$base#$occurrence"
        }
    }

    fun startWrongBookPractice(
        entries: List<WrongQuestionEntry> = reviewDueWrongEntries(),
        includeMastered: Boolean = false,
        sourceLabel: String = "错题本"
    ): Boolean {
        val selectedEntries = entries
            .filter { includeMastered || it.status != WrongStatus.MASTERED.label }
            .filterNot { isQuestionSlashed(it.bankId, it.question) }
        val sourceItems = selectedEntries
            .distinctBy { it.bankId + "#" + it.question.id }
            .map { entry ->
                PracticeQuestionSource(
                    bankId = entry.bankId,
                    bankName = entry.bankName,
                    groupName = banks.firstOrNull { it.id == entry.bankId }?.groupName ?: DEFAULT_BANK_GROUP_NAME,
                    question = entry.question
                )
            }
        if (sourceItems.isEmpty()) return false
        return startPracticeSession(
            questionCount = sourceItems.size,
            allowedTypes = QuestionType.values().toSet(),
            sourceItems = sourceItems,
            sourceLabel = sourceLabel,
            randomize = false
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

        val currentIndex = practiceIndex.coerceIn(0, practiceQuestions.lastIndex)
        practiceQuestions = practiceQuestions.toMutableList().also { it[currentIndex] = sanitizedUpdated }
        val currentSessionKey = practiceSessionKeyAt(currentIndex).orEmpty()
        val mappedBankId = practiceQuestionBankIds[currentSessionKey] ?: targetBank.id
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

        practiceSessionResults.remove(currentSessionKey)
        practiceAnswerResults.remove(currentSessionKey)
        practiceDraftAnswers.remove(currentSessionKey)
        if (practiceLastResult?.question?.id == current.id) practiceLastResult = null
        selectedAnswer = emptyList()
        persist()
        return true
    }

    fun updateCurrentPracticeQuestionAnalysis(analysis: String): Boolean {
        val normalizedAnalysis = normalizeJsonMultilineText(analysis).trim()
        if (normalizedAnalysis.isBlank()) return false
        val current = currentPracticeQuestion() ?: return false
        val targetBank = bankForPracticeQuestion(current) ?: return false
        val bankIndex = banks.indexOfFirst { it.id == targetBank.id }
        if (bankIndex < 0) return false
        val questionIndex = banks[bankIndex].questions.indexOfFirst { it.id == current.id }
        if (questionIndex < 0) return false

        val sourceQuestion = banks[bankIndex].questions[questionIndex]
        val updatedQuestion = sanitizeQuestion(sourceQuestion.copy(analysis = normalizedAnalysis))
        val updatedQuestions = banks[bankIndex].questions.toMutableList()
        updatedQuestions[questionIndex] = updatedQuestion
        banks[bankIndex] = banks[bankIndex].copy(questions = updatedQuestions)

        practiceQuestions = practiceQuestions.mapIndexed { index, practiceQuestion ->
            val sessionKey = practiceSessionKeyAt(index).orEmpty()
            val sourceBankId = practiceQuestionBankIds[sessionKey] ?: targetBank.id
            if (sourceBankId == targetBank.id && practiceQuestion.id == current.id) {
                updatedQuestion
            } else {
                practiceQuestion
            }
        }

        practiceQuestionSessionKeys.forEach { sessionKey ->
            val sourceBankId = practiceQuestionBankIds[sessionKey] ?: targetBank.id
            val existing = practiceAnswerResults[sessionKey]
            if (sourceBankId == targetBank.id && existing?.question?.id == current.id) {
                practiceAnswerResults[sessionKey] = existing.copy(question = updatedQuestion)
            }
        }
        practiceLastResult = practiceLastResult?.let { existing ->
            if (existing.question.id == current.id) existing.copy(question = updatedQuestion) else existing
        }

        for (index in wrongBook.indices) {
            val entry = wrongBook[index]
            if (entry.bankId == targetBank.id && entry.question.id == current.id) {
                wrongBook[index] = entry.copy(
                    bankName = banks[bankIndex].name,
                    question = updatedQuestion
                )
            }
        }
        for (index in favoriteQuestions.indices) {
            val entry = favoriteQuestions[index]
            if (entry.bankId == targetBank.id && entry.question.id == current.id) {
                favoriteQuestions[index] = entry.copy(
                    bankName = banks[bankIndex].name,
                    question = updatedQuestion
                )
            }
        }

        persist()
        return true
    }

    fun completePracticeSession() {
        finishPracticeSessionIfNeeded(advanceSequentialProgress = true)
        resetPracticeState()
    }

    fun endPracticeSession() {
        finishPracticeSessionIfNeeded(advanceSequentialProgress = false)
        resetPracticeState()
    }

    fun canSaveSequentialProgressOnPracticeExit(): Boolean {
        return practiceQuestions.isNotEmpty() &&
            practiceSequentialBankId != null &&
            practiceSequentialStartIndex != null &&
            !practiceReciteModeEnabled
    }

    fun endPracticeSessionSavingSequentialProgress() {
        val progressSaved = saveSequentialProgressForPracticeExit()
        finishPracticeSessionIfNeeded(advanceSequentialProgress = false)
        resetPracticeState()
        if (progressSaved) persist()
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
            practiceQuestionSessionKeys = listOf("${entry.bankId}#${entry.question.id}")
            practiceQuestionBankIds[practiceQuestionSessionKeys.first()] = entry.bankId
            practiceSessionScopeType = PRACTICE_SCOPE_BANK
            practiceSessionScopeName = bank.name
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

    fun wrongBookEntriesForCurrentScope(): List<WrongQuestionEntry> {
        // 题库范围已改由错题本页面临时选择。旧设置字段仅保留数据兼容，不再裁剪全局错题数据。
        return wrongBook.toList()
    }

    fun currentWrongBookScopeLabel(): String = "全部题库"

    fun reviewDueWrongEntries(): List<WrongQuestionEntry> {
        return wrongBookEntriesForCurrentScope()
            .filter { it.status != WrongStatus.MASTERED.label }
            .filterNot { isQuestionSlashed(it.bankId, it.question) }
    }

    fun todayWrongBookSmartReviewEntries(now: Long = System.currentTimeMillis()): List<WrongQuestionEntry> {
        if (!wrongBookSmartReviewEnabled) return emptyList()
        return wrongBookEntriesForCurrentScope()
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

    fun clearWrongBookForCurrentScope() {
        // 保留旧 API 兼容调用；页面已自行处理具体题库清空，因此这里统一视为全部范围。
        clearWrongBook()
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
        val question = currentPracticeQuestion() ?: return false
        val bank = currentPracticeSourceBank() ?: return false
        val key = questionKey(question)
        val now = System.currentTimeMillis()
        if (slashedQuestions.none { it.bankId == bank.id && it.questionKey == key }) {
            slashedQuestions.add(0, SlashedQuestionEntry(bank.id, key, now))
        }

        val currentIndex = practiceIndex
        val currentSessionKey = practiceSessionKeyAt(currentIndex).orEmpty()
        practiceQuestions = practiceQuestions.toMutableList().also { if (currentIndex in it.indices) it.removeAt(currentIndex) }
        practiceQuestionSessionKeys = practiceQuestionSessionKeys.toMutableList().also { if (currentIndex in it.indices) it.removeAt(currentIndex) }
        practiceSessionResults.remove(currentSessionKey)
        practiceAnswerResults.remove(currentSessionKey)
        practiceDraftAnswers.remove(currentSessionKey)
        practiceQuestionBankIds.remove(currentSessionKey)
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
        val sourceItems = selectedEntries.map { entry ->
            PracticeQuestionSource(
                bankId = entry.bankId,
                bankName = entry.bankName,
                groupName = banks.firstOrNull { it.id == entry.bankId }?.groupName ?: DEFAULT_BANK_GROUP_NAME,
                question = entry.question
            )
        }
        if (sourceItems.isEmpty()) return false
        return startPracticeSession(
            questionCount = sourceItems.size,
            allowedTypes = QuestionType.values().toSet(),
            sourceItems = sourceItems,
            sourceLabel = "收藏夹",
            randomize = false
        )
    }

    fun toggleAnswer(key: String, multiple: Boolean) {
        val nextAnswer = if (multiple) {
            if (selectedAnswer.contains(key)) selectedAnswer - key else selectedAnswer + key
        } else {
            listOf(key)
        }
        updateCurrentPracticeAnswer(nextAnswer)
    }

    fun updatePracticeTextAnswer(text: String) {
        updateCurrentPracticeAnswer(listOf(text))
    }

    fun updatePracticeBlankAnswer(index: Int, text: String) {
        val question = currentPracticeQuestion() ?: return
        if (!MultiBlankSupport.hasStructuredAnswers(question)) return
        val count = question.blankAnswers.size
        if (index !in 0 until count) return
        val next = MultiBlankSupport.padUserAnswers(selectedAnswer, count).toMutableList()
        next[index] = text
        updateCurrentPracticeAnswer(next)
    }

    private fun updateCurrentPracticeAnswer(answer: List<String>) {
        val question = currentPracticeQuestion()
        val normalizedAnswer = if (question != null && MultiBlankSupport.hasStructuredAnswers(question)) {
            MultiBlankSupport.padUserAnswers(answer, question.blankAnswers.size)
        } else {
            answer.map { it.trim() }.filter { it.isNotBlank() }
        }
        selectedAnswer = normalizedAnswer
        if (practiceMode == PRACTICE_MODE_BATCH && !practiceBatchSubmitted) {
            val sessionKey = currentPracticeSessionKey()
            if (question != null && sessionKey != null) {
                if (normalizedAnswer.none { it.isNotBlank() }) {
                    practiceDraftAnswers.remove(sessionKey)
                } else {
                    practiceDraftAnswers[sessionKey] = normalizedAnswer
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

    fun setPracticeBatchAutoNextEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        practiceBatchAutoNextEnabled = enabled
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

    fun setPracticeOptionShuffleEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        practiceOptionShuffleEnabled = enabled
        persist()
    }

    fun setScreenReaderAssistEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        screenReaderAssistEnabled = enabled
        persist()
    }

    fun setExamAutoNextEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        examAutoNextEnabled = enabled
        persist()
    }

    fun setExamOptionShuffleEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        examOptionShuffleEnabled = enabled
        persist()
    }

    fun setWrongBookSmartReviewEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        wrongBookSmartReviewEnabled = enabled
        persist()
    }

    fun setWrongBookScopeMode(context: Context, mode: String) {
        appContext = context.applicationContext
        wrongBookScopeMode = normalizeWrongBookScopeMode(mode)
        persist()
    }

    fun setWrongBookAdvancedReviewSettingsEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        wrongBookAdvancedReviewSettingsEnabled = enabled
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

    fun setCardAnimationEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        cardAnimationEnabled = enabled
        persist()
    }

    fun setShirohaModeEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        shirohaModeEnabled = enabled
        persist()
        LauncherIconSwitcher.applyShirohaMode(context, enabled)
    }

    fun setTabletSideNavigationEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        tabletSideNavigationEnabled = enabled
        persist()
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
        "small" -> 18
        "large" -> 26
        else -> 22
    }

    fun questionLineHeightSp(): Int = when (questionFontSizeMode) {
        "small" -> 25
        "large" -> 34
        else -> 29
    }

    fun optionFontSizeSp(): Int = when (optionFontSizeMode) {
        "small" -> 14
        "large" -> 19
        else -> 16
    }

    fun optionLineHeightSp(): Int = when (optionFontSizeMode) {
        "small" -> 19
        "large" -> 25
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

    fun setAiSingleQuestionAnalysisEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        aiSingleQuestionAnalysisEnabled = enabled
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
        aiSingleQuestionAnalysisEnabled = false
        persist()
    }

    fun isAiConfigured(): Boolean {
        return aiApiBaseUrl.isNotBlank() && aiApiKey.isNotBlank() && aiModelName.isNotBlank()
    }

    fun submitPracticeQuestion(): QuestionCheckResult? {
        if (practiceMode == PRACTICE_MODE_BATCH && !practiceBatchSubmitted) return null
        val question = currentPracticeQuestion() ?: return null
        val result = evaluateQuestion(question, selectedAnswer)
        val sessionKey = currentPracticeSessionKey() ?: return null
        val bank = currentPracticeSourceBank()
        practiceLastResult = result
        practiceSessionResults[sessionKey] = result.correct
        practiceAnswerResults[sessionKey] = StudyQuestionResult(
            question = question,
            userAnswer = result.userAnswer,
            userBlankAnswers = result.userBlankAnswers,
            correct = result.correct,
            answerText = result.answerText,
            autoScored = result.autoScored,
            sourceBankId = bank?.id,
            sourceBankName = bank?.name
        )

        if (result.autoScored && result.correct) {
            markWrongQuestionRight(bank = bank, question = question)
        } else if (result.autoScored) {
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
        practiceCurrentBatchIndexes().forEach { index ->
            val question = practiceQuestions.getOrNull(index) ?: return@forEach
            val sessionKey = practiceSessionKeyAt(index) ?: return@forEach
            val userAnswer = practiceDraftAnswers[sessionKey].orEmpty()
            val result = evaluateQuestion(question, userAnswer)
            val bank = bankForPracticeIndex(index)
            practiceSessionResults[sessionKey] = result.correct
            practiceAnswerResults[sessionKey] = StudyQuestionResult(
                question = question,
                userAnswer = result.userAnswer,
                userBlankAnswers = result.userBlankAnswers,
                correct = result.correct,
                answerText = result.answerText,
                autoScored = result.autoScored,
                sourceBankId = bank?.id,
                sourceBankName = bank?.name
            )
            if (result.autoScored && result.correct) {
                markWrongQuestionRight(bank = bank, question = question)
            } else if (result.autoScored) {
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

    fun isPracticeDraftAnswered(index: Int): Boolean {
        val question = practiceQuestions.getOrNull(index) ?: return false
        val sessionKey = practiceSessionKeyAt(index) ?: return false
        return MultiBlankSupport.isUserAnswerComplete(question, practiceDraftAnswers[sessionKey].orEmpty())
    }

    fun practiceResultCorrectAt(index: Int): Boolean? {
        return practiceSessionKeyAt(index)?.let { key -> practiceAnswerResults[key]?.correct }
    }

    fun practiceDraftAnsweredCount(): Int {
        val indexes = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchIndexes() else practiceQuestions.indices.toList()
        return indexes.count(::isPracticeDraftAnswered)
    }

    fun practiceCurrentBatchSubmittedCount(): Int {
        return practiceCurrentBatchIndexes().count { index -> practiceSessionKeyAt(index)?.let(practiceAnswerResults::containsKey) == true }
    }

    fun practiceCurrentBatchAutoScoredSubmittedCount(): Int {
        return practiceCurrentBatchIndexes().count { index -> practiceSessionKeyAt(index)?.let { practiceAnswerResults[it]?.autoScored == true } == true }
    }

    fun practiceCurrentBatchCorrectCount(): Int {
        return practiceCurrentBatchIndexes().count { index ->
            val result = practiceSessionKeyAt(index)?.let { practiceAnswerResults[it] }
            result?.autoScored == true && result.correct
        }
    }

    fun practiceWrongQuestionIndexes(): List<Int> {
        val indexes = if (practiceMode == PRACTICE_MODE_BATCH) practiceCurrentBatchIndexes() else practiceQuestions.indices.toList()
        return indexes.mapNotNull { index ->
            practiceQuestions.getOrNull(index) ?: return@mapNotNull null
            val result = practiceSessionKeyAt(index)?.let { practiceAnswerResults[it] }
            if (result?.autoScored == true && !result.correct) index else null
        }
    }

    fun practiceUnsubmittedQuestionIndexes(): List<Int> {
        if (practiceMode == PRACTICE_MODE_BATCH) return emptyList()
        return practiceQuestions.indices.filter { index ->
            val sessionKey = practiceSessionKeyAt(index) ?: return@filter true
            !practiceAnswerResults.containsKey(sessionKey)
        }
    }

    fun submitUnansweredPracticeQuestionsAsBlank(): Int {
        if (practiceMode == PRACTICE_MODE_BATCH || practiceQuestions.isEmpty()) return 0
        val unsubmittedIndexes = practiceUnsubmittedQuestionIndexes()
        if (unsubmittedIndexes.isEmpty()) return 0

        unsubmittedIndexes.forEach { index ->
            val question = practiceQuestions.getOrNull(index) ?: return@forEach
            val sessionKey = practiceSessionKeyAt(index) ?: return@forEach
            val result = evaluateQuestion(question, emptyList())
            val bank = bankForPracticeIndex(index)
            practiceSessionResults[sessionKey] = result.correct
            practiceAnswerResults[sessionKey] = StudyQuestionResult(
                question = question,
                userAnswer = result.userAnswer,
                userBlankAnswers = result.userBlankAnswers,
                correct = result.correct,
                answerText = result.answerText,
                autoScored = result.autoScored,
                sourceBankId = bank?.id,
                sourceBankName = bank?.name
            )
            if (result.autoScored && result.correct) {
                markWrongQuestionRight(bank = bank, question = question)
            } else if (result.autoScored) {
                addWrongQuestion(
                    bank = bank,
                    question = question,
                    userAnswer = result.userAnswer,
                    source = currentPracticeWrongSource()
                )
            }
        }
        restorePracticeSelectionForCurrentQuestion()
        persist()
        return unsubmittedIndexes.size
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
            practiceQuestions.indices.all { index -> practiceSessionKeyAt(index)?.let(practiceAnswerResults::containsKey) == true }
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
        examOptionShuffleSessionEnabled = examOptionShuffleEnabled
        examOptionShuffleSeed = System.currentTimeMillis()
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
        examOptionShuffleSessionEnabled = false
        examOptionShuffleSeed = 0L
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

    fun updateExamTextAnswer(text: String) {
        val question = currentExamQuestion() ?: return
        val cleanAnswer = text.trim()
        if (cleanAnswer.isBlank()) {
            examAnswers.remove(question.id)
        } else {
            examAnswers[question.id] = listOf(cleanAnswer)
        }
    }

    fun updateExamBlankAnswer(index: Int, text: String) {
        val question = currentExamQuestion() ?: return
        if (!MultiBlankSupport.hasStructuredAnswers(question)) return
        val count = question.blankAnswers.size
        if (index !in 0 until count) return
        val next = MultiBlankSupport.padUserAnswers(examAnswers[question.id].orEmpty(), count).toMutableList()
        next[index] = text
        if (next.none { it.isNotBlank() }) {
            examAnswers.remove(question.id)
        } else {
            examAnswers[question.id] = next
        }
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
                userAnswer = result.userAnswer,
                userBlankAnswers = result.userBlankAnswers,
                correct = result.correct,
                answerText = result.answerText,
                earnedScore = if (result.autoScored) { if (result.correct) maxScore else 0.0 } else null,
                maxScore = if (result.autoScored) maxScore else null,
                autoScored = result.autoScored,
                sourceBankId = bank?.id,
                sourceBankName = bank?.name
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
            if (result.autoScored && result.correct) {
                markWrongQuestionRight(bank = bank, question = question)
            } else if (result.autoScored) {
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

    fun isExamQuestionAnswered(question: Question): Boolean {
        return MultiBlankSupport.isUserAnswerComplete(question, examAnswers[question.id].orEmpty())
    }

    fun examAnsweredCount(): Int = examQuestions.count(::isExamQuestionAnswered)

    fun examCorrectCount(): Int = examQuestions.count { question ->
        val result = evaluateQuestion(question, examAnswers[question.id].orEmpty())
        result.autoScored && result.correct
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

    fun examTotalScore(): Double = examQuestions.sumOf { question ->
        if (isAutoScoredQuestionType(question.type)) scoreForExamQuestion(question) else 0.0
    }

    fun examEarnedScore(): Double = examQuestions.sumOf { question ->
        val result = evaluateQuestion(question, examAnswers[question.id].orEmpty())
        if (result.autoScored && result.correct) scoreForExamQuestion(question) else 0.0
    }



    fun exportBanksBackupJson(bankIds: Set<String>): String {
        val selectedBanks = banks.filter { bankIds.contains(it.id) }
        return buildBackupJson(
            kind = "shiroha_quiz_selected_banks",
            selectedBanks = selectedBanks,
            includeWrongBook = false,
            includeSlashedQuestions = false,
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
            includeSlashedQuestions = true,
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
            includeSlashedQuestions = false,
            includeFavorites = false,
            includeStudyRecords = false
        )
    }

    fun exportFullBackupZip(): ByteArray {
        return buildBackupZip(
            kind = "shiroha_quiz_full_backup",
            selectedBanks = banks,
            includeWrongBook = true,
            includeSlashedQuestions = true,
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

    fun parseImportJsonBank(rawText: String): QuizBank? {
        return parseImportJsonPreview(rawText)?.banks?.singleOrNull()
    }

    fun parseImportJsonPreview(rawText: String): JsonImportPreview? {
        return runCatching { parseStandaloneJsonPreview(rawText) }.getOrNull()
    }

    private fun parseStandaloneJsonPreview(rawText: String): JsonImportPreview? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        return if (text.startsWith("[")) {
            val array = JSONArray(text)
            if (looksLikeBankArray(array)) {
                val parsedBanks = parseBanksJson(array.toString())
                if (parsedBanks.isEmpty()) return null
                JsonImportPreview(
                    banks = parsedBanks,
                    kind = "multi_bank_array",
                    message = "检测到多题库 JSON：共 ${parsedBanks.size} 个题库，将只导入题库内容。"
                )
            } else {
                val bank = parseStandaloneQuestionArrayBank(array) ?: return null
                JsonImportPreview(
                    banks = listOf(bank),
                    kind = "question_array",
                    message = "检测到题目数组 JSON：共 ${bank.questions.size} 题，将导入为一个新题库。"
                )
            }
        } else {
            val root = JSONObject(text)
            val banksArray = root.optJSONArray("banks")
            val standaloneBank = parseStandaloneBankJsonObject(root)
            when {
                standaloneBank != null -> JsonImportPreview(
                    banks = listOf(standaloneBank),
                    kind = "single_bank",
                    message = "检测到 JSON 单题库：${standaloneBank.name}，共 ${standaloneBank.questions.size} 题。"
                )
                banksArray != null -> {
                    val parsedBanks = parseBanksJson(banksArray.toString())
                    if (parsedBanks.isEmpty()) return null
                    val webBackup = isWebBackupJson(root)
                    JsonImportPreview(
                        banks = parsedBanks,
                        kind = if (webBackup) "web_backup" else "multi_bank_backup",
                        message = if (webBackup) {
                            "检测到 Web 备份包：共 ${parsedBanks.size} 个题库；将同步可映射的错题本、收藏夹和学习记录，平台专用设置不会跨端覆盖。"
                        } else {
                            "检测到多题库 JSON：共 ${parsedBanks.size} 个题库，将只导入题库内容。"
                        }
                    )
                }
                else -> null
            }
        }
    }

    private fun parseStandaloneJsonBanks(rawText: String): List<QuizBank> {
        return parseStandaloneJsonPreview(rawText)?.banks.orEmpty()
    }

    private fun looksLikeBankArray(array: JSONArray): Boolean {
        if (array.length() == 0) return false
        val firstObject = array.optJSONObject(0) ?: return false
        return firstObject.optJSONArray("questions") != null
    }

    private fun isWebBackupJson(root: JSONObject): Boolean {
        if (!root.has("banks")) return false
        val kind = root.optString("kind")
        if (kind.startsWith("shiroha_quiz_web") || root.has("crossPlatform")) return true
        if (kind.startsWith("shiroha_quiz")) return false
        return root.optString("app").equals("Shiroha Quiz", ignoreCase = true) ||
            root.has("schemaVersion") ||
            root.has("exportType") ||
            root.has("settings") ||
            root.has("favorites") ||
            root.has("records")
    }

    private fun parseStandaloneQuestionArrayBank(array: JSONArray): QuizBank? {
        val questions = parseQuestionsArray(array)
        if (questions.isEmpty()) return null
        return sanitizeBank(
            QuizBank(
                id = "bank_json_${System.currentTimeMillis()}",
                name = "导入题库",
                groupName = DEFAULT_BANK_GROUP_NAME,
                questions = questions
            )
        )
    }

    private fun parseStandaloneBankJsonObject(root: JSONObject): QuizBank? {
        val questionsArray = root.optJSONArray("questions") ?: return null
        val questions = parseQuestionsArray(questionsArray)
        if (questions.isEmpty()) return null

        val rawName = listOf(
            root.optString("name"),
            root.optString("bankName"),
            root.optString("title")
        ).firstOrNull { it.isNotBlank() }.orEmpty()
        val rawGroupName = listOf(
            root.optString("groupName"),
            root.optString("group"),
            root.optString("category")
        ).firstOrNull { it.isNotBlank() }.orEmpty()

        return sanitizeBank(
            QuizBank(
                id = root.optString("id").ifBlank { "bank_json_${System.currentTimeMillis()}" },
                name = rawName.ifBlank { "导入题库" },
                groupName = rawGroupName.ifBlank { DEFAULT_BANK_GROUP_NAME },
                questions = questions
            )
        )
    }

    private data class BackupAsset(
        val backupPath: String,
        val file: File
    )

    private fun buildBackupZip(
        kind: String,
        selectedBanks: List<QuizBank>,
        includeWrongBook: Boolean,
        includeSlashedQuestions: Boolean,
        includeFavorites: Boolean,
        includeStudyRecords: Boolean
    ): ByteArray {
        val assetMapping = mutableMapOf<String, BackupAsset>()
        val root = buildBackupJson(
            kind = kind,
            selectedBanks = selectedBanks,
            includeWrongBook = includeWrongBook,
            includeSlashedQuestions = includeSlashedQuestions,
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
        includeSlashedQuestions: Boolean,
        includeFavorites: Boolean,
        includeStudyRecords: Boolean,
        assetMapping: MutableMap<String, BackupAsset>?
    ): JSONObject {
        val root = JSONObject()
        root.put("app", "Shiroha Quiz")
        root.put("kind", kind)
        root.put("version", 3)
        root.put("schemaVersion", 3)
        root.put("crossPlatformSchemaVersion", 1)
        root.put("exportedBy", "native")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("activeBankId", activeBankId)
        root.put("banks", JSONArray(banksToJson(selectedBanks, assetMapping)))
        if (includeWrongBook) root.put("wrongBook", JSONArray(wrongBookToJson(wrongBook, assetMapping)))
        if (includeSlashedQuestions) root.put("slashedQuestions", JSONArray(slashedQuestionsToJson(slashedQuestions)))
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
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    if (entries.size >= maxEntries) {
                        throw IllegalArgumentException("ZIP entry count exceeded.")
                    }
                    val name = SafeZipReader.normalizeEntryName(entry.name)
                    val data = SafeZipReader.readEntryBytes(
                        zip = zip,
                        entry = entry,
                        maxSize = maxEntrySize,
                        maxTotalRemaining = maxTotalSize - totalSize
                    )
                    totalSize += data.size
                    entries[name] = data
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

    private fun parseBackupTimeMillis(value: Any?, fallback: Long? = null): Long? {
        if (value == null || value == JSONObject.NULL) return fallback
        if (value is Number) return value.toLong().takeIf { it > 0 } ?: fallback
        val raw = value.toString().trim()
        if (raw.isBlank()) return fallback
        raw.toLongOrNull()?.takeIf { it > 0 }?.let { return it }
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        patterns.forEach { pattern ->
            val parsed = runCatching {
                java.text.SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(raw)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return fallback
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
                val array = JSONArray(text)
                if (looksLikeBankArray(array)) {
                    JSONObject().put("banks", array)
                } else {
                    JSONObject()
                        .put("name", "导入题库")
                        .put("groupName", DEFAULT_BANK_GROUP_NAME)
                        .put("questions", array)
                }
            } else {
                JSONObject(text)
            }
        }.getOrElse { return "导入失败：不是有效的 JSON / ZIP 备份文件。" }

        val preview = runCatching { parseStandaloneJsonPreview(text) }
            .getOrElse { return "导入失败：题库数据无法解析。" }
            ?: return "导入失败：备份中没有可用题库。"
        val importedBankRepairs = preview.banks
            .map { bank -> normalizeImportedBankAssets(bank, zipAssets, assetDir) }
            .map(::sanitizeBankWithQuestionIdRepair)
        val importedBanks = importedBankRepairs.map { it.bank }
        val importedRepairByBankId = importedBankRepairs.associateBy { it.bank.id }
        if (importedBanks.isEmpty()) return "导入失败：备份中没有可用题库。"

        val isWebBackup = isWebBackupJson(root)
        val crossPlatformRoot = root.optJSONObject("crossPlatform")
        val canonicalStateRoot = crossPlatformRoot ?: root

        val idMap = mutableMapOf<String, String>()
        val now = System.currentTimeMillis()
        val importedNamesByGroup = mutableMapOf<String, MutableSet<String>>()
        val addedBanks = importedBanks.mapIndexed { index, bank ->
            val newId = "bank_${now}_$index"
            idMap[bank.id] = newId
            val cleanGroupName = normalizeBankGroupName(bank.groupName)
            val reservedNames = importedNamesByGroup.getOrPut(cleanGroupName) { mutableSetOf() }
            val cleanName = uniqueImportedBankName(
                rawName = bank.name,
                groupName = cleanGroupName,
                reservedNames = reservedNames
            )
            reservedNames += cleanName
            bank.copy(
                id = newId,
                name = cleanName,
                groupName = cleanGroupName
            )
        }
        banks.addAll(addedBanks)
        val importedActiveBankId = root.optString("activeBankId")
        val mappedActiveBankId = idMap[importedActiveBankId]
        if (mappedActiveBankId != null) activeBankId = mappedActiveBankId
        else if (activeBankId == null && addedBanks.isNotEmpty()) activeBankId = addedBanks.first().id

        // 旧 Web 备份没有 crossPlatform 时，按旧对象结构映射状态。
        var importedStateCount = 0
        if (isWebBackup && crossPlatformRoot == null) {
            // Web wrongBook: {bankId: [{id, wrongCount, rightCount, lastWrongAt, lastCorrectAt, status}]}
            val webWrongBookObj = root.optJSONObject("wrongBook")
            if (webWrongBookObj != null) {
                val importedWrongBook = mutableListOf<WrongQuestionEntry>()
                val bankIdKeys = webWrongBookObj.keys()
                while (bankIdKeys.hasNext()) {
                    val originalBankId = bankIdKeys.next()
                    val mappedBankId = idMap[originalBankId] ?: continue
                    val mappedBank = addedBanks.firstOrNull { it.id == mappedBankId } ?: continue
                    val entries = webWrongBookObj.optJSONArray(originalBankId) ?: continue
                    for (k in 0 until entries.length()) {
                        val entry = entries.optJSONObject(k) ?: continue
                        val qid = entry.optString("id")
                        val repair = importedRepairByBankId[originalBankId]
                        val repairedQuestion = resolveRepairedQuestionByLegacyId(qid, repair)
                        val question = repairedQuestion
                            ?.let { repaired -> mappedBank.questions.firstOrNull { it.id == repaired.id } }
                            ?: mappedBank.questions.firstOrNull { it.id == qid }
                            ?: continue
                        importedWrongBook.add(
                            WrongQuestionEntry(
                                bankId = mappedBankId,
                                bankName = mappedBank.name,
                                question = question,
                                lastAnswer = emptyList(),
                                source = "web-backup",
                                timestamp = parseBackupTimeMillis(entry.opt("lastWrongAt"), System.currentTimeMillis()) ?: System.currentTimeMillis(),
                                wrongCount = entry.optInt("wrongCount", 1),
                                rightCount = entry.optInt("rightCount", 0),
                                reviewRightCount = entry.optInt("reviewRightCount", 0),
                                streakCorrectCount = entry.optInt("streakCorrectCount", 0),
                                lastWrongAt = parseBackupTimeMillis(entry.opt("lastWrongAt"), System.currentTimeMillis()) ?: System.currentTimeMillis(),
                                lastCorrectAt = if (entry.has("lastCorrectAt") && !entry.isNull("lastCorrectAt")) parseBackupTimeMillis(entry.opt("lastCorrectAt")) else null,
                                status = entry.optString("status", "未掌握").ifBlank { "未掌握" },
                                lastReviewedAt = if (entry.has("lastReviewedAt") && !entry.isNull("lastReviewedAt")) parseBackupTimeMillis(entry.opt("lastReviewedAt")) else null,
                                nextReviewAt = if (entry.has("nextReviewAt") && !entry.isNull("nextReviewAt")) parseBackupTimeMillis(entry.opt("nextReviewAt")) else null,
                                reviewLevel = entry.optInt("reviewLevel", 0)
                            )
                        )
                    }
                }
                wrongBook.addAll(0, importedWrongBook)
                importedStateCount += importedWrongBook.size
            }
            // Web favorites: {bankId: [questionId, ...]}
            val webFavoritesObj = root.optJSONObject("favorites")
            if (webFavoritesObj != null) {
                val importedFavorites = mutableListOf<FavoriteQuestionEntry>()
                val bankIdKeys = webFavoritesObj.keys()
                while (bankIdKeys.hasNext()) {
                    val originalBankId = bankIdKeys.next()
                    val mappedBankId = idMap[originalBankId] ?: continue
                    val mappedBank = addedBanks.firstOrNull { it.id == mappedBankId } ?: continue
                    val qids = webFavoritesObj.optJSONArray(originalBankId) ?: continue
                    for (k in 0 until qids.length()) {
                        val qid = qids.optString(k)
                        val repair = importedRepairByBankId[originalBankId]
                        val repairedQuestion = resolveRepairedQuestionByLegacyId(qid, repair)
                        val question = repairedQuestion
                            ?.let { repaired -> mappedBank.questions.firstOrNull { it.id == repaired.id } }
                            ?: mappedBank.questions.firstOrNull { it.id == qid }
                            ?: continue
                        importedFavorites.add(FavoriteQuestionEntry(question = question, bankId = mappedBankId, bankName = mappedBank.name, favoritedAt = System.currentTimeMillis()))
                    }
                }
                favoriteQuestions.addAll(0, importedFavorites)
                importedStateCount += importedFavorites.size
            }
            // Web records: resolve questionId back to the imported bank so record details remain usable.
            val webRecords = root.optJSONArray("records")
            if (webRecords != null) {
                val importedRecords = mutableListOf<StudyRecord>()
                for (k in 0 until webRecords.length()) {
                    val rec = webRecords.optJSONObject(k) ?: continue
                    val originalBankId = rec.optString("bankId")
                    val mappedBankId = idMap[originalBankId] ?: originalBankId
                    val mappedBank = addedBanks.firstOrNull { it.id == mappedBankId }
                    val details = rec.optJSONArray("details")
                    val questionResults = if (details != null) {
                        buildList {
                            for (d in 0 until details.length()) {
                                val detail = details.optJSONObject(d) ?: continue
                                val nestedQuestion = detail.optJSONObject("question")?.let { runCatching { parseQuestion(it) }.getOrNull() }
                                val questionId = detail.optString("questionId")
                                val repair = importedRepairByBankId[originalBankId]
                                val resolvedQuestion = nestedQuestion?.let { reference ->
                                    resolveRepairedQuestion(reference, repair)
                                        ?.let { repaired -> mappedBank?.questions?.firstOrNull { it.id == repaired.id } }
                                        ?: reference
                                }
                                    ?: resolveRepairedQuestionByLegacyId(questionId, repair)
                                        ?.let { repaired -> mappedBank?.questions?.firstOrNull { it.id == repaired.id } }
                                    ?: mappedBank?.questions?.firstOrNull { it.id == questionId }
                                    ?: Question(
                                        id = questionId.ifBlank { "record_question_${k}_$d" },
                                        type = parseQuestionType(detail.optString("type")),
                                        question = detail.optString("question"),
                                        answer = detail.optJSONArray("answer")?.let { arr ->
                                            buildList { for (i in 0 until arr.length()) add(arr.optString(i)) }
                                        }.orEmpty(),
                                        blankAnswers = parseNestedStringArray(detail.optJSONArray("blankAnswers")),
                                        category = detail.optString("category")
                                    )
                                val chosen = detail.optJSONArray("chosen")?.let { arr ->
                                    buildList { for (i in 0 until arr.length()) add(arr.optString(i)) }
                                }.orEmpty()
                                val referenceAnswer = detail.optJSONArray("answer")?.let { arr ->
                                    buildList { for (i in 0 until arr.length()) add(arr.optString(i)) }
                                }.orEmpty()
                                add(
                                    StudyQuestionResult(
                                        question = resolvedQuestion,
                                        userAnswer = chosen,
                                        userBlankAnswers = parseOrderedStringArray(detail.optJSONArray("userBlankAnswers")).ifEmpty {
                                            if (MultiBlankSupport.hasStructuredAnswers(resolvedQuestion)) chosen else emptyList()
                                        },
                                        correct = detail.optBoolean("correct"),
                                        answerText = if (MultiBlankSupport.hasStructuredAnswers(resolvedQuestion)) {
                                            MultiBlankSupport.expectedAnswerText(resolvedQuestion.blankAnswers)
                                        } else {
                                            referenceAnswer.joinToString(" / ")
                                        },
                                        earnedScore = if (detail.has("score") && !detail.isNull("score")) detail.optDouble("score") else null,
                                        maxScore = if (detail.has("fullScore") && !detail.isNull("fullScore")) detail.optDouble("fullScore") else null,
                                        autoScored = true,
                                        sourceBankId = (idMap[detail.optString("sourceBankId")]
                                            ?: detail.optString("sourceBankId").ifBlank { mappedBankId }),
                                        sourceBankName = run {
                                            val originalSourceBankId = detail.optString("sourceBankId")
                                            val resolvedSourceBankId = idMap[originalSourceBankId]
                                                ?: originalSourceBankId.ifBlank { mappedBankId }
                                            addedBanks.firstOrNull { it.id == resolvedSourceBankId }?.name
                                                ?: detail.optString("sourceBankName").ifBlank { mappedBank?.name.orEmpty() }
                                        }
                                    )
                                )
                            }
                        }
                    } else emptyList()
                    importedRecords.add(
                        StudyRecord(
                            id = rec.optString("id", "rec_${System.currentTimeMillis()}_$k"),
                            bankId = mappedBankId.ifBlank { null },
                            bankName = rec.optString("bankName").ifBlank { mappedBank?.name.orEmpty() },
                            source = "web",
                            title = rec.optString("mode", "练习"),
                            total = rec.optInt("total", questionResults.size),
                            correct = rec.optInt("correct", questionResults.count { it.correct }),
                            timestamp = parseBackupTimeMillis(rec.opt("timestamp"), null)
                                ?: parseBackupTimeMillis(rec.opt("date"), System.currentTimeMillis())
                                ?: System.currentTimeMillis(),
                            durationSeconds = rec.optInt("duration").takeIf { it > 0 },
                            autoSubmitted = rec.optBoolean("autoSubmitted"),
                            startedAt = if (rec.has("startedAt") && !rec.isNull("startedAt")) parseBackupTimeMillis(rec.opt("startedAt")) else null,
                            earnedScore = if (rec.has("score") && !rec.isNull("score")) rec.optDouble("score") else null,
                            totalScore = if (rec.has("totalScore") && !rec.isNull("totalScore")) rec.optDouble("totalScore") else null,
                            questionResults = questionResults,
                            scopeType = rec.optString("scopeType").ifBlank { null },
                            scopeName = rec.optString("scopeName").ifBlank { null }
                        )
                    )
                }
                studyRecords.addAll(0, importedRecords)
                importedStateCount += importedRecords.size
            }
            resetPracticeState()
            resetExam()
            persist()
            return if (importedStateCount > 0) {
                "已导入 ${addedBanks.size} 个题库；已同步错题本、收藏夹和记录。"
            } else {
                "已导入 ${addedBanks.size} 个题库。"
            }
        }

        val importedWrongBook = canonicalStateRoot.optJSONArray("wrongBook")?.let { array ->
            runCatching { parseWrongBookJson(array.toString()) }.getOrDefault(emptyList())
        }.orEmpty()
        val mappedWrongBook = importedWrongBook.mapNotNull { entry ->
            val mappedBankId = idMap[entry.bankId] ?: return@mapNotNull null
            val mappedBank = addedBanks.firstOrNull { it.id == mappedBankId }
            val mappedBankName = mappedBank?.name ?: entry.bankName
            val originalBankId = entry.bankId
            val repair = importedRepairByBankId[originalBankId]
            val mappedQuestion = resolveRepairedQuestion(entry.question, repair)
                ?.let { repaired -> mappedBank?.questions?.firstOrNull { it.id == repaired.id } }
                ?: mappedBank?.questions?.firstOrNull { it.id == entry.question.id }
                ?: normalizeImportedQuestionAssets(entry.question, zipAssets, assetDir)
            sanitizeWrongEntry(
                entry.copy(
                    bankId = mappedBankId,
                    bankName = mappedBankName,
                    question = mappedQuestion
                )
            )
        }
        wrongBook.addAll(0, mappedWrongBook)

        val importedSlashedQuestions = canonicalStateRoot.optJSONArray("slashedQuestions")?.let { array ->
            runCatching { parseSlashedQuestionsJson(array.toString()) }.getOrDefault(emptyList())
        }.orEmpty()
        val mappedSlashedQuestions = importedSlashedQuestions.mapNotNull { entry ->
            val mappedBankId = idMap[entry.bankId] ?: return@mapNotNull null
            val mappedBank = addedBanks.firstOrNull { it.id == mappedBankId } ?: return@mapNotNull null
            val repair = importedRepairByBankId[entry.bankId]
            val repairedQuestionKey = resolveRepairedQuestionByLegacyId(entry.questionKey, repair)?.id ?: entry.questionKey
            if (mappedBank.questions.none { question -> questionKey(question) == repairedQuestionKey }) return@mapNotNull null
            entry.copy(bankId = mappedBankId, questionKey = repairedQuestionKey)
        }.let { sanitizeSlashedEntries(it, addedBanks) }
        slashedQuestions.addAll(0, mappedSlashedQuestions)

        val importedFavorites = canonicalStateRoot.optJSONArray("favoriteQuestions")?.let { array ->
            runCatching { parseFavoriteQuestionsJson(array.toString()) }.getOrDefault(emptyList())
        }.orEmpty()
        val mappedFavorites = importedFavorites.mapNotNull { entry ->
            val mappedBankId = idMap[entry.bankId] ?: return@mapNotNull null
            val mappedBank = addedBanks.firstOrNull { it.id == mappedBankId }
            val mappedBankName = mappedBank?.name ?: entry.bankName
            val originalBankId = entry.bankId
            val repair = importedRepairByBankId[originalBankId]
            val mappedQuestion = resolveRepairedQuestion(entry.question, repair)
                ?.let { repaired -> mappedBank?.questions?.firstOrNull { it.id == repaired.id } }
                ?: mappedBank?.questions?.firstOrNull { it.id == entry.question.id }
                ?: normalizeImportedQuestionAssets(entry.question, zipAssets, assetDir)
            sanitizeFavoriteEntry(
                entry.copy(
                    bankId = mappedBankId,
                    bankName = mappedBankName,
                    question = mappedQuestion
                )
            )
        }
        favoriteQuestions.addAll(0, mappedFavorites)

        val importedRecords = canonicalStateRoot.optJSONArray("studyRecords")?.let { array ->
            runCatching { parseStudyRecordsJson(array.toString()) }.getOrDefault(emptyList())
        }.orEmpty()
        val mappedRecords = importedRecords.map { record ->
            val mappedBankId = record.bankId?.let { originalId -> idMap[originalId] ?: originalId }
            val mappedBank = mappedBankId?.let { id -> addedBanks.firstOrNull { it.id == id } }
            val mappedBankName = mappedBank?.name ?: record.bankName
            record.copy(
                bankId = mappedBankId,
                bankName = mappedBankName,
                questionResults = record.questionResults.map { result ->
                    val mappedSourceBankId = result.sourceBankId?.let { originalId -> idMap[originalId] ?: originalId }
                        ?: mappedBankId
                    val mappedSourceBank = mappedSourceBankId?.let { id -> addedBanks.firstOrNull { it.id == id } }
                    val originalSourceBankId = result.sourceBankId ?: record.bankId
                    val repair = originalSourceBankId?.let { importedRepairByBankId[it] }
                    val mappedQuestion = resolveRepairedQuestion(result.question, repair)
                        ?.let { repaired -> mappedSourceBank?.questions?.firstOrNull { it.id == repaired.id } }
                        ?: mappedSourceBank?.questions?.firstOrNull { it.id == result.question.id }
                        ?: normalizeImportedQuestionAssets(result.question, zipAssets, assetDir)
                    result.copy(
                        question = mappedQuestion,
                        sourceBankId = mappedSourceBank?.id ?: mappedSourceBankId,
                        sourceBankName = mappedSourceBank?.name ?: result.sourceBankName
                    )
                }
            )
        }
        studyRecords.addAll(0, mappedRecords)

        resetPracticeState()
        resetExam()
        persist()
        return "已导入 ${addedBanks.size} 个题库" +
            if (mappedWrongBook.isNotEmpty() || mappedSlashedQuestions.isNotEmpty() || mappedFavorites.isNotEmpty() || mappedRecords.isNotEmpty()) {
                "，同时恢复 ${mappedWrongBook.size} 条错题、${mappedSlashedQuestions.size} 条斩题、${mappedFavorites.size} 条收藏、${mappedRecords.size} 条记录。"
            } else {
                "。"
            }
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
        if (!isAllowedDataImageMimeSuffix(mimeSuffix)) return null
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
            else -> "bin"
        }
    }

    private fun isAllowedDataImageMimeSuffix(mimeSuffix: String): Boolean {
        return when (mimeSuffix.lowercase(Locale.ROOT)) {
            "jpeg", "jpg", "png", "gif", "webp", "bmp" -> true
            else -> false
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

    fun deleteStudyRecord(recordId: String): Boolean {
        if (recordId.isBlank()) return false
        val removed = studyRecords.removeAll { record -> record.id == recordId }
        if (removed) persist()
        return removed
    }

    fun clearAllLocalData(context: Context) {
        appContext = context.applicationContext
        banks.clear()
        wrongBook.clear()
        slashedQuestions.clear()
        favoriteQuestions.clear()
        practiceSequentialProgress.clear()
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
        practiceSequentialProgress.clear()
        studyRecords.clear()
        activeBankId = null
        resetPracticeState()
        initialized = false
        appContext = null
        resetExam()
    }


    private fun saveSequentialProgressForPracticeExit(): Boolean {
        val bankId = practiceSequentialBankId ?: return false
        val startIndex = practiceSequentialStartIndex ?: return false
        if (practiceReciteModeEnabled || practiceQuestions.isEmpty()) return false

        val targetInCurrentSession = if (practiceMode == PRACTICE_MODE_BATCH) {
            val batchStart = practiceCurrentBatchStartIndex()
            if (practiceBatchSubmitted) {
                (practiceCurrentBatchEndIndex() + 1).coerceAtMost(practiceQuestions.size)
            } else {
                batchStart
            }
        } else {
            val currentIndex = practiceIndex.coerceIn(0, practiceQuestions.lastIndex)
            val currentSubmitted = practiceSessionKeyAt(currentIndex)?.let(practiceAnswerResults::containsKey) == true
            if (currentSubmitted) {
                (currentIndex + 1).coerceAtMost(practiceQuestions.size)
            } else {
                currentIndex
            }
        }

        val nextProgressIndex = if (targetInCurrentSession >= practiceQuestions.size) {
            practiceSequentialNextIndexAfterComplete ?: (startIndex + practiceQuestions.size)
        } else {
            startIndex + targetInCurrentSession
        }
        practiceSequentialProgress[bankId] = nextProgressIndex.coerceAtLeast(0)
        return true
    }

    private fun finishPracticeSessionIfNeeded(advanceSequentialProgress: Boolean = false) {
        if (practiceQuestions.isEmpty() || practiceAnswerResults.isEmpty()) return
        val now = System.currentTimeMillis()
        val startedAt = practiceStartedAt ?: now
        val orderedResults = practiceQuestions.indices.mapNotNull { index -> practiceSessionKeyAt(index)?.let { practiceAnswerResults[it] } }
        if (orderedResults.isEmpty()) return
        val correctCount = orderedResults.count { it.autoScored && it.correct }
        val involvedBankIds = practiceQuestionSessionKeys.mapNotNull { practiceQuestionBankIds[it] }.distinct()
        val isGroupSession = practiceSessionScopeType == PRACTICE_SCOPE_GROUP
        val recordBank = if (isGroupSession) null else involvedBankIds.singleOrNull()?.let { bankId -> banks.firstOrNull { it.id == bankId } }
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
                bankName = if (isGroupSession) {
                    practiceSessionScopeName ?: practiceSourceLabel.ifBlank { "分组练习" }
                } else {
                    recordBank?.name ?: practiceSessionScopeName ?: practiceSourceLabel.ifBlank { "未命名题库" }
                },
                source = recordSource,
                title = if (practiceSessionScopeType == PRACTICE_SCOPE_GROUP) "${practiceSessionScopeName.orEmpty()} · 分组练习" else practiceSourceLabel.ifBlank { "练习记录" },
                total = orderedResults.size,
                correct = correctCount,
                timestamp = now,
                durationSeconds = ((now - startedAt) / 1000L).toInt().coerceAtLeast(0),
                autoSubmitted = false,
                startedAt = startedAt,
                questionResults = orderedResults,
                scopeType = practiceSessionScopeType,
                scopeName = practiceSessionScopeName
            )
        )
        if (advanceSequentialProgress) {
            advanceSequentialProgressAfterComplete()
        }
        persist()
    }

    private fun advanceSequentialProgressAfterComplete() {
        val bankId = practiceSequentialBankId ?: return
        val nextIndex = practiceSequentialNextIndexAfterComplete ?: return
        practiceSequentialProgress[bankId] = nextIndex.coerceAtLeast(0)
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
        practiceQuestionSessionKeys = emptyList()
        practiceOptionShuffleSeed = System.currentTimeMillis()
        practiceStartedAt = null
        practiceSessionScopeType = null
        practiceSessionScopeName = null
        practiceSequentialBankId = null
        practiceSequentialStartIndex = null
        practiceSequentialNextIndexAfterComplete = null
    }

    private fun restorePracticeSelectionForCurrentQuestion() {
        val question = currentPracticeQuestion()
        selectedAnswer = when {
            question == null -> emptyList()
            practiceMode == PRACTICE_MODE_BATCH && !practiceBatchSubmitted -> currentPracticeSessionKey()?.let { practiceDraftAnswers[it].orEmpty() }.orEmpty()
            question != null -> currentPracticeSessionKey()?.let { practiceAnswerResults[it]?.userAnswer.orEmpty() }.orEmpty()
            else -> emptyList()
        }
        practiceLastResult = if (question != null && practiceMode == PRACTICE_MODE_BATCH && practiceBatchSubmitted) {
            currentPracticeSessionKey()?.let { practiceAnswerResults[it] }?.let { result ->
                QuestionCheckResult(
                    question = question,
                    userAnswer = result.userAnswer,
                    userBlankAnswers = result.userBlankAnswers,
                    correct = result.correct,
                    answerText = result.answerText,
                    autoScored = result.autoScored
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

    fun questionTypeCounts(questions: List<Question> = activePracticePoolQuestions()): Map<QuestionType, Int> {
        return QuestionType.values().associateWith { type -> questions.count { it.type == type } }
    }

    fun practiceAnsweredCount(): Int = practiceSessionResults.size

    fun practiceAutoScoredAnsweredCount(): Int = practiceAnswerResults.values.count { it.autoScored }

    fun practiceCorrectCount(): Int = practiceAnswerResults.values.count { it.autoScored && it.correct }

    private data class QuestionIdRepair(
        val original: Question,
        val repaired: Question
    )

    private data class BankQuestionIdRepair(
        val bank: QuizBank,
        val repairs: List<QuestionIdRepair>,
        val affectedLegacyIds: Set<String>,
        val idChanged: Boolean
    )

    private fun sanitizeBank(bank: QuizBank): QuizBank {
        return sanitizeBankWithQuestionIdRepair(bank).bank
    }

    private fun sanitizeBankWithoutQuestionIdRepair(bank: QuizBank): QuizBank {
        return bank.copy(
            name = bank.name.ifBlank { "未命名题库" },
            groupName = normalizeBankGroupName(bank.groupName),
            questions = bank.questions.map(::sanitizeQuestion)
        )
    }

    private fun sanitizeBankWithQuestionIdRepair(bank: QuizBank): BankQuestionIdRepair {
        val cleanQuestions = bank.questions.map(::sanitizeQuestion)
        val idCounts = cleanQuestions.groupingBy { it.id }.eachCount()
        val affectedLegacyIds = idCounts
            .filter { (id, count) -> id.isBlank() || count > 1 }
            .keys
        val usedIds = mutableSetOf<String>()
        val repairs = bank.questions.zip(cleanQuestions).map { (originalQuestion, cleanQuestion) ->
            val repairedId = if (cleanQuestion.id.isNotBlank() && usedIds.add(cleanQuestion.id)) {
                cleanQuestion.id
            } else {
                generateUniqueQuestionId(usedIds)
            }
            val repairedQuestion = if (repairedId == cleanQuestion.id) cleanQuestion else cleanQuestion.copy(id = repairedId)
            QuestionIdRepair(
                original = originalQuestion,
                repaired = repairedQuestion
            )
        }
        val repairedBank = bank.copy(
            name = bank.name.ifBlank { "未命名题库" },
            groupName = normalizeBankGroupName(bank.groupName),
            questions = repairs.map { it.repaired }
        )
        return BankQuestionIdRepair(
            bank = repairedBank,
            repairs = repairs,
            affectedLegacyIds = affectedLegacyIds,
            idChanged = affectedLegacyIds.isNotEmpty()
        )
    }

    private fun generateUniqueQuestionId(usedIds: MutableSet<String>): String {
        while (true) {
            val candidate = UUID.randomUUID().toString()
            if (usedIds.add(candidate)) return candidate
        }
    }

    private fun questionMigrationFingerprint(question: Question): String {
        fun normalize(value: String): String {
            return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace(Regex("""\s+"""), " ")
                .trim()
                .lowercase(Locale.ROOT)
        }
        return listOf(
            normalize(question.number),
            question.type.name,
            normalize(question.question),
            question.options.joinToString("|") { option -> "${normalize(option.key)}:${normalize(option.text)}" },
            question.answer.joinToString("|") { normalize(it) },
            question.blankAnswers.joinToString("||") { answers -> answers.joinToString("|") { normalize(it) } },
            normalize(question.category),
            question.images.sortedBy { it.order }.joinToString("|") { image ->
                listOf(image.order.toString(), normalize(image.sourceName), image.width.orEmptyText(), image.height.orEmptyText())
                    .joinToString(":")
            }
        ).joinToString("#")
    }

    private fun Int?.orEmptyText(): String = this?.toString().orEmpty()

    private fun resolveRepairedQuestion(
        reference: Question,
        repair: BankQuestionIdRepair?
    ): Question? {
        if (repair == null) return null
        if (reference.id.isNotBlank() && reference.id !in repair.affectedLegacyIds) return null
        val sameLegacyId = repair.repairs.filter { item -> item.original.id == reference.id }
        val candidates = sameLegacyId.ifEmpty { repair.repairs }
        val fingerprint = questionMigrationFingerprint(reference)
        val exactMatches = candidates.filter { item -> questionMigrationFingerprint(item.original) == fingerprint }
        if (exactMatches.isNotEmpty()) return exactMatches.first().repaired

        val normalizedStem = Normalizer.normalize(reference.question, Normalizer.Form.NFKC).trim()
        if (normalizedStem.isNotBlank()) {
            val stemMatches = candidates.filter { item ->
                item.original.type == reference.type &&
                    Normalizer.normalize(item.original.question, Normalizer.Form.NFKC).trim() == normalizedStem
            }
            if (stemMatches.size == 1) return stemMatches.single().repaired
        }

        if (reference.number.isNotBlank()) {
            val numberMatches = candidates.filter { item ->
                item.original.type == reference.type && item.original.number.trim() == reference.number.trim()
            }
            if (numberMatches.size == 1) return numberMatches.single().repaired
        }

        if (reference.id.isNotBlank()) {
            repair.repairs.firstOrNull { item -> item.repaired.id == reference.id }?.let { return it.repaired }
            if (sameLegacyId.isNotEmpty()) return sameLegacyId.first().repaired
        }
        return null
    }

    private fun resolveRepairedQuestionByLegacyId(
        legacyId: String,
        repair: BankQuestionIdRepair?
    ): Question? {
        if (repair == null || legacyId !in repair.affectedLegacyIds) return null
        return repair.repairs.firstOrNull { item -> item.original.id == legacyId }?.repaired
    }

    private fun migrateWrongEntriesForQuestionIdRepair(
        entries: List<WrongQuestionEntry>,
        repairsByBankId: Map<String, BankQuestionIdRepair>
    ): List<WrongQuestionEntry> {
        return entries.map { entry ->
            val repair = repairsByBankId[entry.bankId] ?: return@map entry
            val repairedQuestion = resolveRepairedQuestion(entry.question, repair) ?: return@map entry
            entry.copy(
                bankName = repair.bank.name,
                question = repairedQuestion
            )
        }
    }

    private fun migrateFavoriteEntriesForQuestionIdRepair(
        entries: List<FavoriteQuestionEntry>,
        repairsByBankId: Map<String, BankQuestionIdRepair>
    ): List<FavoriteQuestionEntry> {
        return entries.map { entry ->
            val repair = repairsByBankId[entry.bankId] ?: return@map entry
            val repairedQuestion = resolveRepairedQuestion(entry.question, repair) ?: return@map entry
            entry.copy(
                bankName = repair.bank.name,
                question = repairedQuestion
            )
        }
    }

    private fun migrateStudyRecordsForQuestionIdRepair(
        records: List<StudyRecord>,
        repairsByBankId: Map<String, BankQuestionIdRepair>
    ): List<StudyRecord> {
        return records.map recordLoop@ { record ->
            val migratedResults = record.questionResults.map resultLoop@ { result ->
                val sourceBankId = result.sourceBankId ?: record.bankId
                val repair = sourceBankId?.let { repairsByBankId[it] } ?: return@resultLoop result
                val repairedQuestion = resolveRepairedQuestion(result.question, repair) ?: return@resultLoop result
                result.copy(
                    question = repairedQuestion,
                    sourceBankId = result.sourceBankId ?: repair.bank.id,
                    sourceBankName = result.sourceBankName?.takeIf { it.isNotBlank() } ?: repair.bank.name
                )
            }
            if (migratedResults == record.questionResults) record else record.copy(questionResults = migratedResults)
        }
    }

    private fun migrateSlashedEntriesForQuestionIdRepair(
        entries: List<SlashedQuestionEntry>,
        repairsByBankId: Map<String, BankQuestionIdRepair>
    ): List<SlashedQuestionEntry> {
        return entries.map { entry ->
            val repair = repairsByBankId[entry.bankId] ?: return@map entry
            val candidates = repair.repairs.filter { item ->
                item.original.id in repair.affectedLegacyIds && questionKey(item.original) == entry.questionKey
            }
            val repairedQuestion = when {
                candidates.size == 1 -> candidates.single().repaired
                candidates.isNotEmpty() && candidates.first().repaired.id == entry.questionKey -> candidates.first().repaired
                else -> null
            } ?: return@map entry
            entry.copy(questionKey = repairedQuestion.id)
        }
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

    private fun sanitizeSequentialProgress(progress: Map<String, Int>, banks: List<QuizBank>): Map<String, Int> {
        val bankById = banks.associateBy { it.id }
        val groups = banks.groupBy { normalizeBankGroupName(it.groupName) }
        return progress.mapNotNull { (rawKey, index) ->
            when {
                rawKey.startsWith("BANK:") -> {
                    val bankId = rawKey.removePrefix("BANK:")
                    val bank = bankById[bankId] ?: return@mapNotNull null
                    if (bank.questions.isEmpty()) return@mapNotNull null
                    rawKey to index.coerceIn(0, bank.questions.lastIndex)
                }
                rawKey.startsWith("GROUP:") -> {
                    val groupName = normalizeBankGroupName(rawKey.removePrefix("GROUP:"))
                    val total = groups[groupName].orEmpty().sumOf { it.questions.size }
                    if (total <= 0) return@mapNotNull null
                    "GROUP:$groupName" to index.coerceIn(0, total - 1)
                }
                else -> {
                    val bank = bankById[rawKey] ?: return@mapNotNull null
                    if (bank.questions.isEmpty()) return@mapNotNull null
                    "BANK:${bank.id}" to index.coerceIn(0, bank.questions.lastIndex)
                }
            }
        }.toMap()
    }

    private fun questionKey(question: Question): String {
        return question.id.ifBlank {
            listOf(
                question.type.name,
                question.question.trim(),
                question.options.joinToString("|") { option -> "${option.key}:${option.text}" },
                question.answer.joinToString("|"),
                question.blankAnswers.joinToString("||") { it.joinToString("|") }
            ).joinToString("#").lowercase(Locale.ROOT)
        }
    }

    private fun sanitizeQuestion(question: Question): Question {
        val blankSanitized = MultiBlankSupport.sanitizeQuestion(question)
        val cleanQuestion = stripEmbeddedAnswerBracket(blankSanitized.question, blankSanitized.answer)
        return if (cleanQuestion == blankSanitized.question) blankSanitized else blankSanitized.copy(question = cleanQuestion)
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
        val structuredBlank = MultiBlankSupport.hasStructuredAnswers(question)
        val normalizedUserAnswer = if (structuredBlank) {
            MultiBlankSupport.padUserAnswers(userAnswer, question.blankAnswers.size)
        } else if (question.type == QuestionType.SHORT) {
            userAnswer.map(::normalizeJsonAnswerText).filter { it.isNotBlank() }
        } else {
            userAnswer.map { it.trim() }.filter { it.isNotBlank() }
        }
        val answerText = if (structuredBlank) {
            MultiBlankSupport.expectedAnswerText(question.blankAnswers)
        } else {
            question.answer
                .joinToString(if (question.type == QuestionType.SHORT) "\n\n" else " / ")
                .ifBlank { "未识别答案" }
        }
        val correct = when (question.type) {
            QuestionType.SINGLE,
            QuestionType.MULTIPLE,
            QuestionType.JUDGE -> normalizedUserAnswer.sorted() == question.answer.sorted() && normalizedUserAnswer.isNotEmpty()
            QuestionType.BLANK -> if (structuredBlank) {
                isStructuredBlankAnswerCorrect(
                    userAnswers = normalizedUserAnswer,
                    acceptedAnswers = question.blankAnswers
                )
            } else {
                isBlankAnswerCorrect(
                    userAnswer = normalizedUserAnswer.joinToString(" "),
                    acceptedAnswers = question.answer
                )
            }
            QuestionType.SHORT -> false
        }

        return QuestionCheckResult(
            question = question,
            userAnswer = normalizedUserAnswer,
            userBlankAnswers = if (structuredBlank) normalizedUserAnswer else emptyList(),
            correct = correct,
            answerText = answerText,
            autoScored = isAutoScoredQuestionType(question.type)
        )
    }

    private fun isAutoScoredQuestionType(type: QuestionType): Boolean {
        return type == QuestionType.SINGLE ||
            type == QuestionType.MULTIPLE ||
            type == QuestionType.JUDGE ||
            type == QuestionType.BLANK
    }


    private fun isStructuredBlankAnswerCorrect(
        userAnswers: List<String>,
        acceptedAnswers: List<List<String>>
    ): Boolean {
        if (acceptedAnswers.isEmpty() || userAnswers.size != acceptedAnswers.size) return false
        return acceptedAnswers.indices.all { index ->
            val user = normalizeBlankAnswer(userAnswers[index])
            if (user.isBlank()) return@all false
            acceptedAnswers[index]
                .map(::normalizeBlankAnswer)
                .filter { it.isNotBlank() }
                .any { it == user }
        }
    }

    private fun isBlankAnswerCorrect(userAnswer: String, acceptedAnswers: List<String>): Boolean {
        val normalizedUserAnswer = normalizeBlankAnswer(userAnswer)
        if (normalizedUserAnswer.isBlank()) return false
        return acceptedAnswers
            .flatMap(::splitAcceptedBlankAnswers)
            .map(::normalizeBlankAnswer)
            .filter { it.isNotBlank() }
            .any { it == normalizedUserAnswer }
    }

    private fun splitAcceptedBlankAnswers(answer: String): List<String> {
        return answer
            .split("/", "／", "|", "｜")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(answer.trim()) }
    }

    private fun normalizeBlankAnswer(value: String): String {
        val compactAnswer = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .trim()
            .lowercase(Locale.ROOT)
            .replace('，', ',')
            .replace('。', '.')
            .replace('；', ';')
            .replace('：', ':')
            .replace('（', '(')
            .replace('）', ')')
            .replace('！', '!')
            .replace('？', '?')
            .replace(Regex("[\\s　]+"), "")

        return compactAnswer.trim { it in blankAnswerBoundaryPunctuation }
    }

    private val blankAnswerBoundaryPunctuation = setOf('.', ',', ';', ':', '!', '?', '。', '，', '；', '：', '！', '？', '、')

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

    private fun clampGroupSequentialProgress(groupName: String) {
        val cleanGroup = normalizeBankGroupName(groupName)
        val key = "GROUP:$cleanGroup"
        val total = banks
            .filter { normalizeBankGroupName(it.groupName) == cleanGroup }
            .sumOf { bank -> bank.questions.count { question -> !isQuestionSlashed(bank.id, question) } }
        if (total <= 0) {
            practiceSequentialProgress.remove(key)
        } else if (practiceSequentialProgress.containsKey(key)) {
            practiceSequentialProgress[key] = (practiceSequentialProgress[key] ?: 0).coerceIn(0, total - 1)
        }
    }

    private fun normalizePracticeScopeType(value: String?): String {
        return if (value == PRACTICE_SCOPE_GROUP) PRACTICE_SCOPE_GROUP else PRACTICE_SCOPE_BANK
    }

    private fun ensureValidPracticeScope() {
        if (practiceScopeType == PRACTICE_SCOPE_GROUP) {
            val cleanGroup = normalizeBankGroupName(practiceScopeValue)
            if (banks.any { normalizeBankGroupName(it.groupName) == cleanGroup }) {
                practiceScopeValue = cleanGroup
                return
            }
        } else if (banks.any { it.id == practiceScopeValue }) {
            return
        }
        fallbackPracticeScopeToActiveBank()
    }

    private fun fallbackPracticeScopeToActiveBank() {
        val fallbackBank = activeBank()
        practiceScopeType = PRACTICE_SCOPE_BANK
        practiceScopeValue = fallbackBank?.id.orEmpty()
    }

    private fun normalizeWrongBookScopeMode(mode: String?): String {
        return when (mode) {
            WRONG_BOOK_SCOPE_CURRENT_BANK -> WRONG_BOOK_SCOPE_CURRENT_BANK
            WRONG_BOOK_SCOPE_ALL_BANKS -> WRONG_BOOK_SCOPE_ALL_BANKS
            else -> WRONG_BOOK_SCOPE_ALL_BANKS
        }
    }

    private fun currentPracticeWrongSource(): String {
        return when (practiceSourceLabel) {
            "错题本" -> "错题练习"
            "今日复习" -> "今日复习"
            "收藏夹" -> "收藏练习"
            else -> "练习"
        }
    }

    private fun bankForPracticeIndex(index: Int): QuizBank? {
        val sessionKey = practiceSessionKeyAt(index)
        val mappedBankId = sessionKey?.let { practiceQuestionBankIds[it] }
        return mappedBankId?.let { bankId -> banks.firstOrNull { it.id == bankId } } ?: activeBank()
    }

    private fun bankForPracticeQuestion(question: Question): QuizBank? {
        val index = practiceQuestions.indices.firstOrNull { practiceQuestions[it] === question }
            ?: practiceQuestions.indexOfFirst { it == question }.takeIf { it >= 0 }
        return index?.let(::bankForPracticeIndex) ?: activeBank()
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

    private fun normalizeBankGroupName(rawName: String?): String {
        return rawName?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_BANK_GROUP_NAME
    }

    private fun uniqueBankNameForRename(rawName: String, groupName: String, bankId: String): String {
        val cleanGroupName = normalizeBankGroupName(groupName)
        val baseName = rawName.ifBlank { "未命名题库" }
        val existingNames = banks
            .filterNot { it.id == bankId }
            .filter { normalizeBankGroupName(it.groupName) == cleanGroupName }
            .map { it.name }
            .toSet()
        if (baseName !in existingNames) return baseName
        var index = 2
        var candidate: String
        do {
            candidate = "$baseName $index"
            index += 1
        } while (candidate in existingNames)
        return candidate
    }

    private fun uniqueBankNameInGroup(rawName: String, groupName: String): String {
        val cleanGroupName = normalizeBankGroupName(groupName)
        val baseName = rawName.ifBlank { "导入题库" }
        val existingNames = banks
            .filter { normalizeBankGroupName(it.groupName) == cleanGroupName }
            .map { it.name }
            .toSet()
        if (baseName !in existingNames) return baseName
        var index = 2
        var candidate: String
        do {
            candidate = "$baseName $index"
            index += 1
        } while (candidate in existingNames)
        return candidate
    }

    private fun uniqueImportedBankName(
        rawName: String,
        groupName: String,
        reservedNames: Set<String> = emptySet()
    ): String {
        val cleanGroupName = normalizeBankGroupName(groupName)
        val baseName = rawName.ifBlank { "导入题库" }
        val existingNames = banks
            .filter { normalizeBankGroupName(it.groupName) == cleanGroupName }
            .map { it.name }
            .toSet()
            .plus(reservedNames)
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
            PRACTICE_ORDER_SEQUENTIAL -> PRACTICE_ORDER_SEQUENTIAL
            else -> PRACTICE_ORDER_RANDOM
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
            .putString(KEY_PRACTICE_SCOPE_TYPE, practiceScopeType)
            .putString(KEY_PRACTICE_SCOPE_VALUE, practiceScopeValue)
            .putString(KEY_WRONG_BOOK, wrongBookToJson(wrongBook))
            .putString(KEY_SLASHED_QUESTIONS, slashedQuestionsToJson(slashedQuestions))
            .putString(KEY_FAVORITE_QUESTIONS, favoriteQuestionsToJson(favoriteQuestions))
            .putString(KEY_STUDY_RECORDS, studyRecordsToJson(studyRecords))
            .putString(KEY_PRACTICE_SEQUENTIAL_PROGRESS, sequentialProgressToJson(practiceSequentialProgress))
            .putBoolean(KEY_PRACTICE_NEXT_REQUIRES_RESULT, practiceNextRequiresResult)
            .putBoolean(KEY_REMEMBER_PRACTICE_SETTINGS, rememberPracticeSettingsEnabled)
            .putBoolean(KEY_SWIPE_NAVIGATION_ENABLED, swipeNavigationEnabled)
            .putBoolean(KEY_PRACTICE_AUTO_SUBMIT_ENABLED, practiceAutoSubmitEnabled)
            .putBoolean(KEY_PRACTICE_AUTO_NEXT_ENABLED, practiceAutoNextEnabled)
            .putBoolean(KEY_PRACTICE_BATCH_AUTO_NEXT_ENABLED, practiceBatchAutoNextEnabled)
            .putBoolean(KEY_PRACTICE_INLINE_ANSWER_SETTINGS_ENABLED, practiceInlineAnswerSettingsEnabled)
            .putBoolean(KEY_PRACTICE_RECITE_MODE_ENABLED, practiceReciteModeEnabled)
            .putBoolean(KEY_PRACTICE_SLASH_ENABLED, practiceSlashEnabled)
            .putBoolean(KEY_PRACTICE_QUICK_EDIT_ENABLED, practiceQuickEditEnabled)
            .putBoolean(KEY_PRACTICE_OPTION_SHUFFLE_ENABLED, practiceOptionShuffleEnabled)
            .putBoolean(KEY_SCREEN_READER_ASSIST_ENABLED, screenReaderAssistEnabled)
            .putBoolean(KEY_EXAM_AUTO_NEXT_ENABLED, examAutoNextEnabled)
            .putBoolean(KEY_EXAM_OPTION_SHUFFLE_ENABLED, examOptionShuffleEnabled)
            .putBoolean(KEY_WRONG_BOOK_SMART_REVIEW_ENABLED, wrongBookSmartReviewEnabled)
            .putString(KEY_WRONG_BOOK_SCOPE_MODE, wrongBookScopeMode)
            .putBoolean(KEY_WRONG_BOOK_ADVANCED_REVIEW_SETTINGS_ENABLED, wrongBookAdvancedReviewSettingsEnabled)
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
            .putBoolean(KEY_CARD_ANIMATION_ENABLED, cardAnimationEnabled)
            .putBoolean(KEY_SHIROHA_MODE_ENABLED, shirohaModeEnabled)
            .putBoolean(KEY_TABLET_SIDE_NAVIGATION_ENABLED, tabletSideNavigationEnabled)
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
            .putBoolean(KEY_AI_SINGLE_QUESTION_ANALYSIS_ENABLED, aiSingleQuestionAnalysisEnabled)
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
            bankJson.put("groupName", normalizeBankGroupName(bank.groupName))
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

    private fun sequentialProgressToJson(progress: Map<String, Int>): String {
        val root = JSONObject()
        progress.forEach { (bankId, index) ->
            if (bankId.isNotBlank()) root.put(bankId, index.coerceAtLeast(0))
        }
        return root.toString()
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
            item.put("scopeType", record.scopeType)
            item.put("scopeName", record.scopeName)
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
            if (result.userBlankAnswers.isNotEmpty() || MultiBlankSupport.hasStructuredAnswers(result.question)) {
                item.put("userBlankAnswers", JSONArray(result.userBlankAnswers))
            }
            item.put("correct", result.correct)
            item.put("answerText", result.answerText)
            item.put("earnedScore", result.earnedScore)
            item.put("maxScore", result.maxScore)
            item.put("autoScored", result.autoScored)
            item.put("sourceBankId", result.sourceBankId)
            item.put("sourceBankName", result.sourceBankName)
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
                        questions = parseQuestionsArray(bankJson.optJSONArray("questions")),
                        groupName = normalizeBankGroupName(bankJson.optString("groupName", DEFAULT_BANK_GROUP_NAME))
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

    private fun parseSequentialProgressJson(text: String?): Map<String, Int> {
        if (text.isNullOrBlank()) return emptyMap()
        val root = JSONObject(text)
        val result = mutableMapOf<String, Int>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key.isNotBlank()) result[key] = root.optInt(key, 0).coerceAtLeast(0)
        }
        return result
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
                        bankId = if (item.has("bankId") && !item.isNull("bankId")) {
                            item.optString("bankId").ifBlank { null }
                        } else {
                            null
                        },
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
                        questionResults = parseStudyQuestionResults(item.optJSONArray("questionResults")),
                        scopeType = item.optString("scopeType").ifBlank { null },
                        scopeName = item.optString("scopeName").ifBlank { null }
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
                val question = parseQuestion(questionJson)
                val userBlankAnswers = parseOrderedStringArray(item.optJSONArray("userBlankAnswers")).ifEmpty {
                    if (MultiBlankSupport.hasStructuredAnswers(question)) userAnswer else emptyList()
                }
                add(
                    StudyQuestionResult(
                        question = question,
                        userAnswer = userAnswer,
                        userBlankAnswers = userBlankAnswers,
                        correct = item.optBoolean("correct"),
                        answerText = item.optString("answerText"),
                        earnedScore = if (item.has("earnedScore") && !item.isNull("earnedScore")) item.optDouble("earnedScore") else null,
                        maxScore = if (item.has("maxScore") && !item.isNull("maxScore")) item.optDouble("maxScore") else null,
                        autoScored = item.optBoolean("autoScored", true),
                        sourceBankId = item.optString("sourceBankId").ifBlank { null },
                        sourceBankName = item.optString("sourceBankName").ifBlank { null }
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

    private fun optionKeyForIndex(index: Int): String {
        return ('A'.code + index).toChar().toString()
    }

    private fun parseFlexibleAnswer(questionJson: JSONObject, questionType: QuestionType): List<String> {
        val answerArray = listOf(
            questionJson.optJSONArray("answer"),
            questionJson.optJSONArray("answers"),
            questionJson.optJSONArray("correctAnswer"),
            questionJson.optJSONArray("correctAnswers")
        ).firstOrNull { it != null }
        if (answerArray != null) {
            return buildList {
                for (k in 0 until answerArray.length()) {
                    val value = normalizeJsonAnswerText(answerArray.optString(k))
                    if (value.isNotBlank()) add(value)
                }
            }
        }
        val answerText = listOf(
            questionJson.optString("answer"),
            questionJson.optString("answers"),
            questionJson.optString("correctAnswer"),
            questionJson.optString("correctAnswers")
        ).firstOrNull { it.isNotBlank() }.orEmpty().let(::normalizeJsonAnswerText)
        if (answerText.isBlank()) return emptyList()
        if (questionType == QuestionType.SHORT) return listOf(answerText)
        return answerText
            .split(Regex("[\\s,，;；/|]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun parseQuestion(questionJson: JSONObject): Question {
        val questionType = parseQuestionType(questionJson.optString("type"))
        val optionsArray = questionJson.optJSONArray("options")
        val optionsObject = questionJson.optJSONObject("options")

        val options = buildList {
            when {
                optionsArray != null -> {
                    for (k in 0 until optionsArray.length()) {
                        val optionJson = optionsArray.optJSONObject(k)
                        if (optionJson != null) {
                            val key = optionJson.optString("key").ifBlank { optionKeyForIndex(k) }
                            val text = listOf(
                                optionJson.optString("text"),
                                optionJson.optString("value"),
                                optionJson.optString("content"),
                                optionJson.optString("label")
                            ).firstOrNull { it.isNotBlank() }.orEmpty()
                            add(Option(key = key, text = text))
                        } else {
                            val text = optionsArray.optString(k).trim()
                            if (text.isNotBlank()) add(Option(key = optionKeyForIndex(k), text = text))
                        }
                    }
                }
                optionsObject != null -> {
                    val keys = optionsObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next().toString()
                        val value = optionsObject.opt(key)
                        val text = when (value) {
                            is JSONObject -> listOf(
                                value.optString("text"),
                                value.optString("value"),
                                value.optString("content"),
                                value.optString("label")
                            ).firstOrNull { it.isNotBlank() }.orEmpty()
                            else -> value?.toString().orEmpty()
                        }.trim()
                        if (key.isNotBlank() && text.isNotBlank()) add(Option(key = key, text = text))
                    }
                }
            }
        }

        val answers = parseFlexibleAnswer(questionJson, questionType)

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
                type = questionType,
                question = normalizeJsonMultilineText(questionJson.optString("question")),
                options = options,
                answer = answers,
                blankAnswers = parseNestedStringArray(questionJson.optJSONArray("blankAnswers")),
                analysis = normalizeJsonMultilineText(questionJson.optString("analysis")),
                category = listOf(
                    questionJson.optString("category"),
                    questionJson.optString("group"),
                    questionJson.optString("volume")
                ).firstOrNull { it.isNotBlank() }.orEmpty(),
                images = images,
                score = parseNullableFiniteDouble(questionJson, "score"),
                subject = questionJson.optString("subject"),
                grade = questionJson.optString("grade"),
                difficulty = questionJson.optString("difficulty"),
                knowledgePoints = parseStringArray(questionJson.optJSONArray("knowledgePoints")),
                tags = parseStringArray(questionJson.optJSONArray("tags")),
                source = questionJson.optString("source"),
                sourceFileId = questionJson.optString("sourceFileId"),
                version = questionJson.optInt("version", 1),
                reviewStatus = questionJson.optString("reviewStatus", "approved"),
                aiConfidence = parseNullableFiniteDouble(questionJson, "aiConfidence"),
                warnings = parseStringArray(questionJson.optJSONArray("warnings"))
            )
        )
    }

    private fun normalizeJsonMultilineText(value: String): String {
        return value.replace("\r\n", "\n").replace('\r', '\n')
    }

    private fun normalizeJsonAnswerText(value: String): String {
        val normalized = normalizeJsonMultilineText(value)
        if ('\n' !in normalized && '\t' !in normalized && "```" !in normalized) {
            return normalized.trim()
        }
        val rows = normalized.split('\n').toMutableList()
        while (rows.isNotEmpty() && rows.first().isBlank()) rows.removeAt(0)
        while (rows.isNotEmpty() && rows.last().isBlank()) rows.removeAt(rows.lastIndex)
        return rows.joinToString("\n")
    }

    private fun parseNullableFiniteDouble(json: JSONObject, key: String): Double? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optDouble(key).takeIf { it.isFinite() }
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

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i).trim()
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun parseOrderedStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                add(array.optString(i))
            }
        }
    }

    private fun parseNestedStringArray(array: JSONArray?): List<List<String>> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val inner = array.optJSONArray(i)
                if (inner != null) {
                    add(buildList {
                        for (j in 0 until inner.length()) {
                            add(inner.optString(j))
                        }
                    })
                } else {
                    val value = array.optString(i)
                    add(if (value.isBlank()) emptyList() else listOf(value))
                }
            }
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
        question.score?.takeIf { it.isFinite() }?.let { questionJson.put("score", it) }

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
        if (question.blankAnswers.isNotEmpty()) {
            val blankAnswersArray = JSONArray()
            question.blankAnswers.forEach { answers ->
                blankAnswersArray.put(JSONArray(answers))
            }
            questionJson.put("blankAnswers", blankAnswersArray)
        }

        val imagesArray = JSONArray()
        question.images.forEach { image ->
            val imageJson = JSONObject()
            imageJson.put("id", image.id)
            val exportPath = registerBackupAsset(image, assetMapping, question.id)
            imageJson.put("localPath", exportPath ?: image.localPath)
            imageJson.put("sourceName", image.sourceName)
            imageJson.put("order", image.order)
            imageJson.put("width", image.width)
            imageJson.put("height", image.height)
            imageJson.put("sizeBytes", image.sizeBytes)
            imagesArray.put(imageJson)
        }
        questionJson.put("images", imagesArray)
        if (question.subject.isNotBlank()) questionJson.put("subject", question.subject)
        if (question.grade.isNotBlank()) questionJson.put("grade", question.grade)
        if (question.difficulty.isNotBlank()) questionJson.put("difficulty", question.difficulty)
        if (question.knowledgePoints.isNotEmpty()) questionJson.put("knowledgePoints", JSONArray(question.knowledgePoints))
        if (question.tags.isNotEmpty()) questionJson.put("tags", JSONArray(question.tags))
        if (question.source.isNotBlank()) questionJson.put("source", question.source)
        if (question.sourceFileId.isNotBlank()) questionJson.put("sourceFileId", question.sourceFileId)
        questionJson.put("version", question.version)
        if (question.reviewStatus != "approved") questionJson.put("reviewStatus", question.reviewStatus)
        question.aiConfidence?.takeIf { it.isFinite() }?.let { questionJson.put("aiConfidence", it) }
        if (question.warnings.isNotEmpty()) questionJson.put("warnings", JSONArray(question.warnings))
        return questionJson
    }
    private fun registerBackupAsset(image: QuestionImage, assetMapping: MutableMap<String, BackupAsset>?, questionId: String): String? {
        if (assetMapping == null || image.localPath.isBlank()) return null
        val file = File(image.localPath)
        if (!file.exists() || !file.isFile) return null
        val ext = file.extension.ifBlank { File(image.sourceName).extension.ifBlank { "bin" } }
        val safeQuestionId = questionId.ifBlank { "question" }
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
            .take(80)
            .ifBlank { "question" }
        val safeId = image.id.ifBlank { file.nameWithoutExtension }
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
            .take(80)
            .ifBlank { "asset" }
        val pathHash = java.lang.Integer.toUnsignedString(image.localPath.hashCode(), 36)
        val backupPath = "assets/${safeQuestionId}_${image.order.coerceAtLeast(1)}_${safeId}_${pathHash}.${ext}"
        assetMapping[image.localPath] = BackupAsset(backupPath = backupPath, file = file)
        return backupPath
    }

}
