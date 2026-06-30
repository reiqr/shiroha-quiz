package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ai.AiRefactorResult
import com.yiqiu.shirohaquiz.ai.AiReviewSuggestion
import com.yiqiu.shirohaquiz.ai.ShirohaAiClient
import com.yiqiu.shirohaquiz.importer.model.ImportDiagnostics
import com.yiqiu.shirohaquiz.importer.model.ImportResult
import com.yiqiu.shirohaquiz.importer.model.ImportWarning
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.importer.assets.QuestionImageBinder
import com.yiqiu.shirohaquiz.importer.assets.QuestionImportAssetExtractor
import com.yiqiu.shirohaquiz.importer.model.WarningLevel
import com.yiqiu.shirohaquiz.importer.parser.QuizImportParser
import com.yiqiu.shirohaquiz.importer.parser.TextImportDecoder
import com.yiqiu.shirohaquiz.importer.validate.ImportValidator
import com.yiqiu.shirohaquiz.state.DEFAULT_BANK_GROUP_NAME
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.AiAnalysisFillPanel
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.LoadingIllustration
import com.yiqiu.shirohaquiz.ui.components.MultiBlankAnswerEditor
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.ShirohaDangerConfirmDialog
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaMotion
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import com.yiqiu.shirohaquiz.ui.util.bankDisplayPath
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportScreen(
    onImportSaved: () -> Unit,
    onOpenPreference: () -> Unit
) {
    val context = LocalContext.current
    val importScope = rememberCoroutineScope()
    var rawText by remember { mutableStateOf("") }
    var answerText by remember { mutableStateOf("") }
    var importedImages by remember { mutableStateOf<List<QuestionImportAssetExtractor.ExtractedImportImage>>(emptyList()) }
    var selectedFileName by rememberSaveable { mutableStateOf("未选择文件") }
    var selectedAnswerFileName by rememberSaveable { mutableStateOf("未选择答案文件") }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var editableQuestions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var reviewMode by rememberSaveable { mutableStateOf(false) }
    var reviewIndex by rememberSaveable { mutableStateOf(0) }
    var reviewEditingIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var reviewEditFromFilterList by rememberSaveable { mutableStateOf(false) }
    var reviewFilterListFocusTick by rememberSaveable { mutableStateOf(0) }
    var reviewFilterName by rememberSaveable { mutableStateOf(ReviewFilter.ALL.name) }
    var statusText by rememberSaveable {
        mutableStateOf("")
    }
    var isStatusWarn by rememberSaveable { mutableStateOf(false) }
    var useDualImport by rememberSaveable { mutableStateOf(false) }
    var isImportBusy by rememberSaveable { mutableStateOf(false) }
    var busyText by rememberSaveable { mutableStateOf("") }
    var rawTextEditorExpanded by rememberSaveable { mutableStateOf(true) }
    var answerTextEditorExpanded by rememberSaveable { mutableStateOf(true) }
    var previewOnlyAnomaly by rememberSaveable { mutableStateOf(false) }
    var showAiConfigPrompt by rememberSaveable { mutableStateOf(false) }
    var rawFullEditorMode by rememberSaveable { mutableStateOf(false) }
    var answerFullEditorMode by rememberSaveable { mutableStateOf(false) }
    var aiReviewedQuestionIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var aiAnalyzedQuestionIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var aiAnalysisAppliedQuestionIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var aiReviewSuggestions by remember { mutableStateOf<List<AiReviewSuggestion>>(emptyList()) }
    var saveMode by rememberSaveable { mutableStateOf(ImportSaveMode.NEW_BANK.name) }
    var newBankGroupName by rememberSaveable { mutableStateOf(DEFAULT_BANK_GROUP_NAME) }
    var newBankName by rememberSaveable { mutableStateOf("导入题库") }
    var appendTargetBankId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedFileName) {
        val defaultBankName = defaultImportBankName(selectedFileName)
        if (newBankName.isBlank() || newBankName == "导入题库" || newBankName == "未选择文件") {
            newBankName = defaultBankName
        }
    }

    fun clearParsedResult(clearImages: Boolean = false) {
        importResult = null
        editableQuestions = emptyList()
        reviewMode = false
        reviewIndex = 0
        reviewEditingIndex = null
        reviewFilterName = ReviewFilter.ALL.name
        previewOnlyAnomaly = false
        aiReviewedQuestionIds = emptyList()
        aiAnalyzedQuestionIds = emptyList()
        aiAnalysisAppliedQuestionIds = emptyList()
        aiReviewSuggestions = emptyList()
        if (clearImages) importedImages = emptyList()
    }

    fun applyParsedResult(result: ImportResult) {
        val resultWithExtraWarnings = result.copy(
            warnings = refreshImportWarningsForQuestions(result.warnings, result.questions)
        )
        importResult = resultWithExtraWarnings
        editableQuestions = resultWithExtraWarnings.questions
        reviewIndex = 0
        reviewEditingIndex = null
        reviewFilterName = ReviewFilter.ALL.name
        aiReviewedQuestionIds = emptyList()
        aiAnalyzedQuestionIds = emptyList()
        aiAnalysisAppliedQuestionIds = emptyList()
        aiReviewSuggestions = emptyList()
        val hardCount = resultWithExtraWarnings.warnings.count { it.level == WarningLevel.ERROR }
        val softCount = resultWithExtraWarnings.warnings.count { it.level == WarningLevel.WARNING }
        statusText = "已完成${if (useDualImport) "双文件" else "原生"}解析：${resultWithExtraWarnings.questions.size} 题，硬错误 $hardCount 条，可确认提示 $softCount 条。"
        isStatusWarn = hardCount > 0 || softCount > 0
    }

    fun syncEditableQuestions(
        nextQuestions: List<Question>,
        baseWarnings: List<ImportWarning> = importResult?.warnings.orEmpty()
    ) {
        editableQuestions = nextQuestions
        importResult = importResult?.copy(
            questions = nextQuestions,
            warnings = refreshImportWarningsForQuestions(baseWarnings, nextQuestions)
        )
    }

    if (rawFullEditorMode) {
        BackHandler { rawFullEditorMode = false }
        FullImportTextEditorScreen(
            title = if (useDualImport) "编辑题目文本" else "编辑原始文本",
            value = rawText,
            placeholder = "把标准题库文本粘贴到这里，或通过文件导入后在这里调整。",
            onValueChange = {
                rawText = it
                if (importResult != null || editableQuestions.isNotEmpty() || reviewMode || importedImages.isNotEmpty()) {
                    clearParsedResult(clearImages = true)
                }
            },
            onBack = { rawFullEditorMode = false }
        )
        return
    }

    if (answerFullEditorMode) {
        BackHandler { answerFullEditorMode = false }
        FullImportTextEditorScreen(
            title = "编辑答案文本",
            value = answerText,
            placeholder = "粘贴答案文本，或通过上方按钮选择答案文件。",
            onValueChange = {
                answerText = it
                if (importResult != null || editableQuestions.isNotEmpty() || reviewMode) {
                    clearParsedResult()
                }
            },
            onBack = { answerFullEditorMode = false }
        )
        return
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || isImportBusy) return@rememberLauncherForActivityResult
        selectedFileName = queryFileName(context, uri)
        val fileSizeCheck = checkImportFileSize(context, uri, selectedFileName)
        if (fileSizeCheck.blockMessage != null) {
            statusText = fileSizeCheck.blockMessage
            isStatusWarn = true
            return@rememberLauncherForActivityResult
        }
        isImportBusy = true
        busyText = "正在读取题库文件……"
        statusText = fileSizeCheck.warnMessage ?: "正在读取：$selectedFileName"
        isStatusWarn = fileSizeCheck.warnMessage != null
        clearParsedResult(clearImages = true)
        importScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    readImportedContent(context, uri, selectedFileName)
                }
            }
            isImportBusy = false
            when (val decodeResult = result.getOrElse {
                QuestionImportAssetExtractor.DecodeResult.Failure("文件无法读取，可能已损坏或没有访问权限。")
            }) {
                is QuestionImportAssetExtractor.DecodeResult.Success -> {
                    val content = decodeResult.content
                    if (content.text.isBlank()) {
                        statusText = "已读取文件，但没有发现可用文本内容。请确认文档不是空白、扫描图片或旧版 xls。"
                        isStatusWarn = true
                    } else {
                        rawText = content.text
                        importedImages = content.images
                        rawTextEditorExpanded = content.text.length <= LARGE_TEXT_PREVIEW_THRESHOLD
                        statusText = if (content.images.isNotEmpty()) {
                            "已读取：$selectedFileName，含 ${content.images.size} 张图片。"
                        } else {
                            "已读取：$selectedFileName。"
                        }
                        isStatusWarn = false
                    }
                }
                is QuestionImportAssetExtractor.DecodeResult.Failure -> {
                    statusText = decodeResult.userMessage
                    isStatusWarn = true
                }
            }
        }
    }

    val answerFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || isImportBusy) return@rememberLauncherForActivityResult
        selectedAnswerFileName = queryFileName(context, uri)
        val fileSizeCheck = checkImportFileSize(context, uri, selectedAnswerFileName)
        if (fileSizeCheck.blockMessage != null) {
            statusText = fileSizeCheck.blockMessage
            isStatusWarn = true
            return@rememberLauncherForActivityResult
        }
        isImportBusy = true
        busyText = "正在读取答案文件……"
        statusText = fileSizeCheck.warnMessage ?: "正在读取答案文件：$selectedAnswerFileName"
        isStatusWarn = fileSizeCheck.warnMessage != null
        importScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    readImportedText(context, uri, selectedAnswerFileName)
                }
            }
            isImportBusy = false
            when (val decodeResult = result.getOrElse {
                TextImportDecoder.DecodeResult.Failure("答案文件无法读取，可能已损坏或没有访问权限。")
            }) {
                is TextImportDecoder.DecodeResult.Success -> {
                    val text = decodeResult.text
                    if (text.isBlank()) {
                        statusText = "已读取答案文件，但没有发现可用文本内容。请确认文件不是空白、扫描图片或旧版 xls。"
                        isStatusWarn = true
                    } else {
                        answerText = text
                        answerTextEditorExpanded = text.length <= LARGE_TEXT_PREVIEW_THRESHOLD
                        clearParsedResult()
                        statusText = "已读取答案文件：$selectedAnswerFileName。"
                        isStatusWarn = false
                    }
                }
                is TextImportDecoder.DecodeResult.Failure -> {
                    statusText = decodeResult.userMessage
                    isStatusWarn = true
                }
            }
        }
    }

    fun startParse() {
        if (isImportBusy) return
        if (rawText.isBlank() || (useDualImport && answerText.isBlank())) {
            statusText = if (useDualImport) {
                "请同时提供题目文本和答案文本，再开始双文件解析。"
            } else {
                "请先提供题库文本，再开始原生解析。"
            }
            isStatusWarn = true
            return
        }

        val rawSnapshot = rawText
        val answerSnapshot = answerText
        val imagesSnapshot = importedImages
        val dualSnapshot = useDualImport
        isImportBusy = true
        busyText = if (dualSnapshot) "正在合并题目和答案……" else "正在解析题库文本……"
        statusText = busyText
        isStatusWarn = false
        clearParsedResult()
        importScope.launch {
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    val jsonPreview = if (!dualSnapshot) QuizRepository.parseImportJsonPreview(rawSnapshot) else null
                    if (jsonPreview != null && jsonPreview.banks.size > 1) {
                        throw IllegalArgumentException("${jsonPreview.message} 请在 我的 → 数据管理 → 导入题库 中导入多题库 JSON / 备份包。")
                    }
                    val jsonBank = jsonPreview?.banks?.singleOrNull()
                    val parsedResult = if (jsonBank != null) {
                        ImportResult(
                            questions = jsonBank.questions,
                            strategyName = "JSON题库导入",
                            warnings = emptyList(),
                            diagnostics = ImportDiagnostics(
                                normalizedLength = rawSnapshot.length,
                                blockCount = jsonBank.questions.size,
                                answeredCount = jsonBank.questions.count { it.answer.isNotEmpty() },
                                candidateCount = 1,
                                notes = listOf(jsonPreview.message)
                            )
                        )
                    } else if (dualSnapshot) {
                        QuizImportParser.parseDualText(rawSnapshot, answerSnapshot)
                    } else {
                        QuizImportParser.parseStandardText(rawSnapshot)
                    }
                    val finalResult = if (jsonBank == null && imagesSnapshot.isNotEmpty()) {
                        QuestionImageBinder.attach(parsedResult, imagesSnapshot)
                    } else {
                        parsedResult
                    }
                    finalResult to jsonPreview
                }
            }
            isImportBusy = false
            result.onSuccess { (finalResult, jsonPreview) ->
                val jsonBank = jsonPreview?.banks?.singleOrNull()
                if (jsonBank != null) {
                    newBankName = jsonBank.name.ifBlank { defaultImportBankName(selectedFileName) }
                    newBankGroupName = jsonBank.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME }
                }
                applyParsedResult(finalResult)
                if (jsonPreview != null && jsonBank != null) {
                    statusText = jsonPreview.message
                    isStatusWarn = false
                }
            }.onFailure { error ->
                statusText = "解析失败：${error.message ?: "请检查题库文件格式"}"
                isStatusWarn = true
            }
        }
    }

    fun deleteReviewQuestionAt(index: Int) {
        val deletedQuestionId = editableQuestions.getOrNull(index)?.id
        val nextQuestions = editableQuestions.filterIndexed { currentIndex, _ -> currentIndex != index }
        val baseWarnings = importResult?.warnings.orEmpty().filterNot { warning ->
            deletedQuestionId != null && (importWarningQuestionId(warning) ?: aiWarningQuestionId(warning)) == deletedQuestionId
        }
        syncEditableQuestions(nextQuestions, baseWarnings)
        if (deletedQuestionId != null) {
            aiReviewSuggestions = aiReviewSuggestions.filterNot { it.questionId == deletedQuestionId }
            aiReviewedQuestionIds = aiReviewedQuestionIds.filterNot { it == deletedQuestionId }
            aiAnalyzedQuestionIds = aiAnalyzedQuestionIds.filterNot { it == deletedQuestionId }
            aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds.filterNot { it == deletedQuestionId }
        }
        reviewEditingIndex = null
        reviewEditFromFilterList = false
        reviewIndex = index.coerceAtMost((nextQuestions.size - 1).coerceAtLeast(0))
        firstMatchingQuestionIndex(
            questions = nextQuestions,
            warnings = importResult?.warnings.orEmpty(),
            aiSuggestions = aiReviewSuggestions,
            aiReviewedQuestionIds = aiReviewedQuestionIds,
            aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
            aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
            filter = reviewFilterFromName(reviewFilterName)
        )?.let { reviewIndex = it }
        statusText = "已从核对列表中移除 1 题。保存题库时会使用当前核对后的题目。"
        isStatusWarn = false
    }

    val editingReviewIndex = reviewEditingIndex
    if (reviewMode && importResult != null && editingReviewIndex != null && editableQuestions.isNotEmpty()) {
        BackHandler {
            reviewEditingIndex = null
            if (reviewEditFromFilterList) {
                reviewFilterListFocusTick += 1
            }
            reviewEditFromFilterList = false
        }
        val safeEditingIndex = editingReviewIndex.coerceIn(0, editableQuestions.lastIndex)
        val editingQuestion = editableQuestions[safeEditingIndex]
        ReviewQuestionEditScreen(
            question = editingQuestion,
            questionIndex = safeEditingIndex,
            totalCount = editableQuestions.size,
            questionWarnings = warningsForQuestion(editingQuestion, importResult?.warnings.orEmpty()),
            questionAiSuggestions = aiReviewSuggestions.filter { it.questionId == editingQuestion.id && isActionableAiSuggestion(it) },
            aiReviewedQuestionIds = aiReviewedQuestionIds,
            aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
            aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
            onQuestionChange = { question ->
                syncEditableQuestions(
                    editableQuestions.mapIndexed { currentIndex, item ->
                        if (currentIndex == safeEditingIndex) question else item
                    }
                )
            },
            onApplyAiSuggestion = { suggestion ->
                val currentQuestion = editableQuestions.getOrNull(safeEditingIndex)
                if (currentQuestion != null && canApplyAiSuggestion(suggestion)) {
                    val nextQuestion = applyAiReviewSuggestion(currentQuestion, suggestion)
                    val nextQuestions = editableQuestions.mapIndexed { currentIndex, item ->
                        if (currentIndex == safeEditingIndex) nextQuestion else item
                    }
                    val baseWarnings = importResult?.warnings.orEmpty().filterNot { warning ->
                        isAiImportWarning(warning) && aiWarningBelongsToQuestion(warning, currentQuestion)
                    }
                    syncEditableQuestions(nextQuestions, baseWarnings)
                    aiReviewSuggestions = aiReviewSuggestions.filterNot { it.questionId == suggestion.questionId }
                    statusText = "已采纳 1 条 AI 核对建议，保存题库前仍可继续人工调整。"
                    isStatusWarn = false
                }
            },
            onDeleteQuestion = { deleteReviewQuestionAt(safeEditingIndex) },
            onBack = {
                reviewEditingIndex = null
                if (reviewEditFromFilterList) {
                    reviewFilterListFocusTick += 1
                }
                reviewEditFromFilterList = false
            }
        )
        return
    }

    if (reviewMode && importResult != null) {
        BackHandler {
            reviewEditingIndex = null
            reviewMode = false
        }
        val warnings = importResult?.warnings.orEmpty()
        val reviewFilter = reviewFilterFromName(reviewFilterName)
        NativeQuestionReviewScreen(
            questions = editableQuestions,
            warnings = warnings,
            aiSuggestions = aiReviewSuggestions,
            aiReviewedQuestionIds = aiReviewedQuestionIds,
            aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
            aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
            filter = reviewFilter,
            currentIndex = reviewIndex.coerceIn(0, (editableQuestions.size - 1).coerceAtLeast(0)),
            focusFilterListTick = reviewFilterListFocusTick,
            onFilterChange = { filter ->
                reviewFilterName = filter.name
                firstMatchingQuestionIndex(
                    questions = editableQuestions,
                    warnings = warnings,
                    aiSuggestions = aiReviewSuggestions,
                    aiReviewedQuestionIds = aiReviewedQuestionIds,
                    aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                    aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
                    filter = filter
                )?.let { reviewIndex = it }
            },
            onIndexChange = { index ->
                if (editableQuestions.isNotEmpty()) {
                    reviewIndex = index.coerceIn(0, editableQuestions.lastIndex)
                }
            },
            onQuestionChange = { index, question ->
                syncEditableQuestions(
                    editableQuestions.mapIndexed { currentIndex, item ->
                        if (currentIndex == index) question else item
                    }
                )
            },
            onApplyAiSuggestion = { index, suggestion ->
                val currentQuestion = editableQuestions.getOrNull(index)
                if (currentQuestion != null && canApplyAiSuggestion(suggestion)) {
                    val nextQuestion = applyAiReviewSuggestion(currentQuestion, suggestion)
                    val nextQuestions = editableQuestions.mapIndexed { currentIndex, item ->
                        if (currentIndex == index) nextQuestion else item
                    }
                    val baseWarnings = importResult?.warnings.orEmpty().filterNot { warning ->
                        isAiImportWarning(warning) && aiWarningBelongsToQuestion(warning, currentQuestion)
                    }
                    syncEditableQuestions(nextQuestions, baseWarnings)
                    aiReviewSuggestions = aiReviewSuggestions.filterNot { it.questionId == suggestion.questionId }
                    statusText = "已采纳 1 条 AI 核对建议，保存题库前仍可继续人工调整。"
                    isStatusWarn = false
                }
            },
            onDeleteQuestion = { index -> deleteReviewQuestionAt(index) },
            onEditQuestion = { index, focusFilterListOnBack ->
                if (editableQuestions.isNotEmpty()) {
                    reviewIndex = index.coerceIn(0, editableQuestions.lastIndex)
                    reviewEditingIndex = index.coerceIn(0, editableQuestions.lastIndex)
                    reviewEditFromFilterList = focusFilterListOnBack
                }
            },
            onBack = {
                reviewEditingIndex = null
                reviewMode = false
            }
        )
        return
    }

    val shouldPickAnswerFile = useDualImport && selectedFileName != "未选择文件"
    val importScrollState = rememberScrollState()
    var importViewportHeightPx by remember { mutableIntStateOf(0) }
    var importTopContentHeightPx by remember { mutableIntStateOf(0) }
    var importEditorHeaderHeightPx by remember { mutableIntStateOf(0) }
    val importLayoutDensity = LocalDensity.current
    val useAdaptiveInitialEditorHeight = !isImportBusy &&
        !useDualImport &&
        importResult == null &&
        rawText.isBlank() &&
        selectedFileName == "未选择文件"
    val adaptiveInitialEditorHeight = if (
        useAdaptiveInitialEditorHeight &&
        importViewportHeightPx > 0 &&
        importTopContentHeightPx > 0 &&
        importEditorHeaderHeightPx > 0
    ) {
        with(importLayoutDensity) {
            (importViewportHeightPx - importTopContentHeightPx - importEditorHeaderHeightPx).toDp()
                .minus(ShirohaSpacing.Sm)
                .minus(ShirohaSpacing.Xxl)
                .minus(ShirohaSpacing.Lg)
                .minus(ShirohaSpacing.Xl)
                .minus(ShirohaSpacing.Xl)
                .minus(12.dp)
                .coerceAtLeast(104.dp)
        }
    } else {
        104.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { importViewportHeightPx = it.height }
            .verticalScroll(importScrollState)
            .padding(
                start = ShirohaSpacing.Xl,
                top = ShirohaSpacing.Sm,
                end = ShirohaSpacing.Xl,
                bottom = ShirohaSpacing.Xxl
            ),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { importTopContentHeightPx = it.height },
            verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
        ) {
            ShirohaHeader(
                kicker = "Import",
                title = "导入题库",
                subtitle = ""
            )

            ImportStepHeroCard()

            GlassCard {
            Text(
                text = "导入方式",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ShirohaColors.CardWhite62,
                border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSoft)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = "选择题库文件",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "选择题库文件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "支持 docx / txt / json / xlsx / csv",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (useDualImport) {
                    "当前题库文件：$selectedFileName\n当前答案文件：$selectedAnswerFileName"
                } else {
                    "当前文件：$selectedFileName"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.FileOpen,
                    text = if (shouldPickAnswerFile) "选择答案文件" else "选择题库文件",
                    primary = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (!isImportBusy) {
                            if (shouldPickAnswerFile) {
                                answerFilePicker.launch(arrayOf("*/*"))
                            } else {
                                filePicker.launch(arrayOf("*/*"))
                            }
                        }
                    }
                )
                ActionPillButton(
                    icon = Icons.Rounded.Refresh,
                    text = "填入示例",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (!isImportBusy) {
                            useDualImport = false
                            selectedFileName = "示例题库"
                            rawText = sampleImportText()
                            rawTextEditorExpanded = true
                            answerTextEditorExpanded = true
                            importedImages = emptyList()
                            clearParsedResult()
                            statusText = "已填入示例题库。"
                            isStatusWarn = false
                        }
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ImportModeChip(
                    icon = Icons.Rounded.Description,
                    text = "标准导入",
                    selected = !useDualImport,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    onClick = {
                        if (!isImportBusy) {
                            useDualImport = false
                            clearParsedResult()
                            statusText = "已切换到标准导入。"
                            isStatusWarn = false
                        }
                    }
                )
                ImportModeChip(
                    icon = Icons.Rounded.AutoAwesome,
                    text = "双文件导入",
                    selected = useDualImport,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    onClick = {
                        if (!isImportBusy) {
                            useDualImport = true
                            clearParsedResult()
                            statusText = "已切换到双文件导入。先选择题库文件，再选择答案文件。"
                            isStatusWarn = false
                        }
                    }
                )
            }
        }

            val generalStatusText = statusText.takeIf { it.isNotBlank() && !shouldShowAiStatusInImport(it) }
            if (generalStatusText != null && (isStatusWarn || !isImportBusy)) {
                NoticeCard(generalStatusText, warning = isStatusWarn)
            }
        }

        if (isImportBusy) {
            LoadingIllustration(
                text = busyText.ifBlank { "正在处理导入任务……" },
                imageRes = R.drawable.illus_loading_state_webp
            )
        } else {
            GlassCard {
                val hasRawText = rawText.isNotBlank()
                val hasSelectedFile = selectedFileName != "未选择文件"
                val rawTextTitle = when {
                    useDualImport -> "题目文本"
                    hasRawText || hasSelectedFile -> "原始文本"
                    else -> "粘贴导入"
                }
                val rawTextHint = when {
                    useDualImport -> "题库文件内容，可在解析前核对调整。"
                    hasRawText || hasSelectedFile -> "可在解析前核对或调整导入文本。"
                    else -> "粘贴题库文本后解析。"
                }
                val rawTextActionButtonWidth = 128.dp
                val rawTextActionButtonHeight = ShirohaDimens.ActionButtonMinHeight
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { importEditorHeaderHeightPx = it.height },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rawTextTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = rawTextHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = "开始解析",
                        primary = true,
                        modifier = Modifier
                            .width(rawTextActionButtonWidth)
                            .height(rawTextActionButtonHeight),
                        fillWidthContent = true,
                        onClick = { startParse() }
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (!rawTextEditorExpanded && rawText.length > LARGE_TEXT_PREVIEW_THRESHOLD) {
                    LargeImportTextPreview(
                        text = rawText,
                        label = "题目文本较长，已收起全文编辑以减少卡顿。",
                        showEditButton = false,
                        onEditFullText = { rawFullEditorMode = true }
                    )
                } else {
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = {
                            rawText = it
                            rawTextEditorExpanded = it.length <= LARGE_TEXT_PREVIEW_THRESHOLD
                            if (importResult != null || editableQuestions.isNotEmpty() || reviewMode || importedImages.isNotEmpty()) {
                                clearParsedResult(clearImages = true)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                when {
                                    useDualImport -> 160.dp
                                    hasRawText || hasSelectedFile -> 190.dp
                                    else -> adaptiveInitialEditorHeight
                                }
                            ),
                        enabled = true,
                        minLines = when {
                            useDualImport -> 7
                            hasRawText || hasSelectedFile -> 9
                            else -> 4
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("把标准题库文本粘贴到这里，或通过上方选择文件导入。") }
                    )
                }
                if (hasRawText) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ActionPillButton(
                            icon = Icons.Rounded.Edit,
                            text = "编辑全文",
                            primary = false,
                            modifier = Modifier
                                .width(rawTextActionButtonWidth)
                                .height(rawTextActionButtonHeight),
                            fillWidthContent = true,
                            onClick = { rawFullEditorMode = true }
                        )
                    }
                }

                if (useDualImport) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "答案文本",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    if (!answerTextEditorExpanded && answerText.length > LARGE_TEXT_PREVIEW_THRESHOLD) {
                        LargeImportTextPreview(
                            text = answerText,
                            label = "答案文本较长，已收起全文编辑以减少卡顿。",
                            onEditFullText = { answerFullEditorMode = true }
                        )
                    } else {
                        OutlinedTextField(
                            value = answerText,
                            onValueChange = {
                                answerText = it
                                answerTextEditorExpanded = it.length <= LARGE_TEXT_PREVIEW_THRESHOLD
                                if (importResult != null || editableQuestions.isNotEmpty() || reviewMode) {
                                    clearParsedResult()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            enabled = true,
                            minLines = 7,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            placeholder = { Text("粘贴答案文本，或通过上方按钮选择答案文件。") }
                        )
                    }
                }
            }

            importResult?.let { result ->
                val displayResult = result.copy(questions = editableQuestions)
                NativeImportSummary(
                    result = displayResult,
                    aiSuggestions = aiReviewSuggestions,
                    aiReviewedQuestionIds = aiReviewedQuestionIds,
                    aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                    aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds
                )

                val warnings = importResult?.warnings.orEmpty()
                val anomalyQuestions = editableQuestions.filter { question ->
                    questionMatchesFilter(question, warningsForQuestion(question, warnings), ReviewFilter.ANOMALY)
                }
                val aiSuggestionCount = editableQuestions.count { question ->
                    aiSuggestionsForQuestion(question, aiReviewSuggestions).any(::isActionableAiSuggestion)
                }
                val aiApplicableCount = editableQuestions.count { question ->
                    aiSuggestionsForQuestion(question, aiReviewSuggestions).any(::canApplyAiSuggestion)
                }
                val missingAnalysisCount = editableQuestions.count(::shouldApplyAiAnalysis)
                val aiAnalysisAppliedCount = editableQuestions.count { it.id in aiAnalysisAppliedQuestionIds.toSet() }
                val previewQuestions = if (previewOnlyAnomaly) anomalyQuestions else editableQuestions

                GlassCard {
                    Text(
                        text = "核对与写入",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "人工核对，可用AI辅助，最后确认保存",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "人工核对",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionPillButton(
                            icon = Icons.Rounded.Edit,
                            text = "进入沉浸核对",
                            primary = false,
                            onClick = {
                                if (editableQuestions.isNotEmpty()) {
                                    reviewFilterName = ReviewFilter.ALL.name
                                    reviewIndex = reviewIndex.coerceIn(0, editableQuestions.lastIndex)
                                    reviewMode = true
                                }
                            }
                        )
                        ActionPillButton(
                            icon = Icons.Rounded.CheckCircle,
                            text = if (previewOnlyAnomaly) "显示全部题" else "仅看异常题",
                            primary = previewOnlyAnomaly,
                            onClick = {
                                val nextOnlyAnomaly = !previewOnlyAnomaly
                                previewOnlyAnomaly = nextOnlyAnomaly
                                statusText = if (nextOnlyAnomaly) {
                                    "快速预览已切换为仅显示异常题，共 ${anomalyQuestions.size} 题。"
                                } else {
                                    "快速预览已切换为全部题目。"
                                }
                                isStatusWarn = false
                            }
                        )
                        ActionPillButton(
                            icon = Icons.Rounded.Description,
                            text = "看缺解析 $missingAnalysisCount",
                            primary = false,
                            enabled = missingAnalysisCount > 0,
                            onClick = {
                                reviewFilterName = ReviewFilter.MISSING_ANALYSIS.name
                                firstMatchingQuestionIndex(
                                    questions = editableQuestions,
                                    warnings = warnings,
                                    aiSuggestions = aiReviewSuggestions,
                                    aiReviewedQuestionIds = aiReviewedQuestionIds,
                                    aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                                    aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
                                    filter = ReviewFilter.MISSING_ANALYSIS
                                )?.let { reviewIndex = it }
                                reviewMode = true
                            }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "AI 辅助",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionPillButton(
                            icon = Icons.Rounded.AutoAwesome,
                            text = "AI重构",
                            primary = QuizRepository.isAiConfigured() && QuizRepository.aiRefactorEnabled,
                            modifier = Modifier.alpha(if (QuizRepository.isAiConfigured() && QuizRepository.aiRefactorEnabled) 1f else ShirohaDimens.DisabledAlpha),
                            enabled = editableQuestions.isNotEmpty() && rawText.isNotBlank() && !isImportBusy,
                            onClick = {
                                if (!QuizRepository.isAiConfigured()) {
                                    showAiConfigPrompt = true
                                    statusText = "AI 重构：请先在个人偏好 → AI 设置中配置接口。"
                                    isStatusWarn = true
                                    return@ActionPillButton
                                }
                                if (!QuizRepository.aiRefactorEnabled) {
                                    statusText = "AI 重构未启用，请先在个人偏好 → AI 设置中开启。"
                                    isStatusWarn = true
                                    return@ActionPillButton
                                }
                                val sourceText = rawText.trim()
                                val answerSourceText = answerText.trim()
                                if (sourceText.isBlank()) {
                                    statusText = "AI 重构需要原始文本，请先导入或粘贴题库文本。"
                                    isStatusWarn = true
                                    return@ActionPillButton
                                }
                                val sourceLength = sourceText.length + answerSourceText.length
                                if (sourceLength > QuizRepository.aiRefactorMaxChars) {
                                    statusText = "AI 重构：原文约 ${sourceLength} 字，超过当前上限 ${QuizRepository.aiRefactorMaxChars} 字。请在 AI 设置中调大上限，或拆分题库后处理。"
                                    isStatusWarn = true
                                    return@ActionPillButton
                                }
                                val warningTexts = displayResult.warnings.map { warning ->
                                    val numberText = warning.questionNumber?.takeIf { it.isNotBlank() }?.let { "题号$it：" }.orEmpty()
                                    "${warning.level.name}：$numberText${warning.message}"
                                }.distinct().take(120)
                                val beforeCount = editableQuestions.size
                                statusText = "AI 重构中：优先清洗原文并重新本地解析，必要时再使用 AI 直接重构结果。"
                                isStatusWarn = false
                                importScope.launch {
                                    isImportBusy = true
                                    busyText = "AI 重构中……"
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            val refactorResult = ShirohaAiClient.refactorQuestions(
                                                apiBaseUrl = QuizRepository.aiApiBaseUrl,
                                                apiKey = QuizRepository.aiApiKey,
                                                modelName = QuizRepository.aiModelName,
                                                rawText = sourceText,
                                                answerText = answerSourceText,
                                                currentQuestions = editableQuestions,
                                                warnings = warningTexts,
                                                timeoutSeconds = QuizRepository.aiTimeoutSeconds
                                            )
                                            val cleanedText = refactorResult.cleanedText.orEmpty().trim()
                                            val cleanedAnswerText = refactorResult.cleanedAnswerText.orEmpty().trim()
                                            val reparsedResult = if (cleanedText.isNotBlank()) {
                                                val parsed = if (cleanedAnswerText.isNotBlank()) {
                                                    QuizImportParser.parseDualText(cleanedText, cleanedAnswerText)
                                                } else {
                                                    QuizImportParser.parseStandardText(cleanedText)
                                                }
                                                if (importedImages.isNotEmpty()) {
                                                    QuestionImageBinder.attach(parsed, importedImages)
                                                } else {
                                                    parsed
                                                }
                                            } else {
                                                null
                                            }
                                            AiRefactorApplyResult(refactorResult = refactorResult, reparsedResult = reparsedResult)
                                        }
                                    }.onSuccess { applyResult ->
                                        val refactorResult = applyResult.refactorResult
                                        val reparsedResult = applyResult.reparsedResult
                                        val directQuestions = refactorResult.questions
                                        val shouldUseReparsed = reparsedResult != null && reparsedResult.questions.isNotEmpty() &&
                                            (directQuestions.isEmpty() || reparsedResult.questions.size >= directQuestions.size || reparsedResult.questions.size >= beforeCount)

                                        if (shouldUseReparsed && reparsedResult != null) {
                                            val refactoredQuestions = reparsedResult.questions
                                            val nextWarnings = refreshImportWarningsForQuestions(
                                                aiRefactorImportWarnings(refactorResult.notes, "AI重构已清洗原文并重新本地解析，请人工确认题量、题干、选项、答案和解析后再保存。") +
                                                    reparsedResult.warnings,
                                                refactoredQuestions
                                            )
                                            val nextResult = reparsedResult.copy(
                                                strategyName = "AI重构重解析 + ${reparsedResult.strategyName}",
                                                warnings = nextWarnings,
                                                diagnostics = reparsedResult.diagnostics.copy(
                                                    notes = (reparsedResult.diagnostics.notes + "AI重构：已清洗原文并重新本地解析，由 $beforeCount 题解析为 ${refactoredQuestions.size} 题。" + refactorResult.notes).distinct()
                                                )
                                            )
                                            importResult = nextResult
                                            editableQuestions = refactoredQuestions
                                            reviewIndex = 0
                                            reviewFilterName = ReviewFilter.ALL.name
                                            previewOnlyAnomaly = false
                                            aiReviewedQuestionIds = emptyList()
                                            aiAnalyzedQuestionIds = emptyList()
                                            aiAnalysisAppliedQuestionIds = emptyList()
                                            aiReviewSuggestions = emptyList()
                                            statusText = "AI 重构完成：已清洗原文并重新本地解析，由 $beforeCount 题得到 ${refactoredQuestions.size} 题。请先人工核对，再继续 AI 核对或 AI 解析。"
                                            isStatusWarn = nextWarnings.isNotEmpty()
                                        } else if (directQuestions.isNotEmpty()) {
                                            val refactoredQuestions = directQuestions
                                            val nextWarnings = refreshImportWarningsForQuestions(
                                                aiRefactorImportWarnings(refactorResult.notes, "AI重构已生成新的待核对结果，请人工确认题量、题干、选项、答案和解析后再保存。"),
                                                refactoredQuestions
                                            )
                                            val nextResult = displayResult.copy(
                                                questions = refactoredQuestions,
                                                strategyName = "${displayResult.strategyName} + AI重构",
                                                warnings = nextWarnings,
                                                diagnostics = displayResult.diagnostics.copy(
                                                    notes = (displayResult.diagnostics.notes + "AI重构：由 $beforeCount 题重整为 ${refactoredQuestions.size} 题。" + refactorResult.notes).distinct()
                                                )
                                            )
                                            importResult = nextResult
                                            editableQuestions = refactoredQuestions
                                            reviewIndex = 0
                                            reviewFilterName = ReviewFilter.ALL.name
                                            previewOnlyAnomaly = false
                                            aiReviewedQuestionIds = emptyList()
                                            aiAnalyzedQuestionIds = emptyList()
                                            aiAnalysisAppliedQuestionIds = emptyList()
                                            aiReviewSuggestions = emptyList()
                                            statusText = "AI 重构完成：由 $beforeCount 题重整为 ${refactoredQuestions.size} 题。请先人工核对，再继续 AI 核对或 AI 解析。"
                                            isStatusWarn = nextWarnings.isNotEmpty()
                                        } else if (reparsedResult != null) {
                                            statusText = "AI 重构已返回清洗文本，但本地重解析未得到可用题目，当前待核对结果未改动。"
                                            isStatusWarn = true
                                        } else {
                                            statusText = "AI 重构完成但没有返回可用清洗文本或题目，当前待核对结果未改动。"
                                            isStatusWarn = true
                                        }
                                    }.onFailure { error ->
                                        statusText = "AI 重构失败：${error.message ?: "请检查接口配置"}"
                                        isStatusWarn = true
                                    }
                                    isImportBusy = false
                                    busyText = ""
                                }
                            }
                        )
                        ActionPillButton(
                            icon = Icons.Rounded.AutoAwesome,
                            text = "AI核对",
                            primary = QuizRepository.isAiConfigured() && QuizRepository.aiReviewEnabled,
                            modifier = Modifier.alpha(if (QuizRepository.isAiConfigured() && QuizRepository.aiReviewEnabled) 1f else ShirohaDimens.DisabledAlpha),
                            enabled = editableQuestions.isNotEmpty() && !isImportBusy,
                            onClick = {
                                if (!QuizRepository.isAiConfigured()) {
                                    showAiConfigPrompt = true
                                    statusText = "AI 核对：请先在个人偏好 → AI 设置中配置接口。"
                                    isStatusWarn = true
                                    return@ActionPillButton
                                }
                                if (!QuizRepository.aiReviewEnabled) {
                                    statusText = "AI 核对未启用，请先在个人偏好 → AI 设置中开启。"
                                    isStatusWarn = true
                                    return@ActionPillButton
                                }
                                val baseReviewQuestions = if (QuizRepository.aiOnlyAnomaly) anomalyQuestions else editableQuestions
                                val reviewedIdSet = aiReviewedQuestionIds.toSet()
                                val remainingReviewQuestions = baseReviewQuestions.filterNot { it.id in reviewedIdSet }
                                val limitedQuestions = remainingReviewQuestions.take(QuizRepository.aiMaxQuestions)
                                if (baseReviewQuestions.isEmpty()) {
                                    statusText = if (QuizRepository.aiOnlyAnomaly) {
                                        "AI 核对：当前开启了仅处理异常题，但没有可核对的异常题。"
                                    } else {
                                        "AI 核对：当前没有可供核对的题目。"
                                    }
                                    isStatusWarn = true
                                    return@ActionPillButton
                                }
                                if (limitedQuestions.isEmpty()) {
                                    statusText = "AI 核对：当前范围已全部处理。如需重新核对，请重新解析题库或切换处理范围。"
                                    isStatusWarn = false
                                    return@ActionPillButton
                                }
                                val processedBefore = baseReviewQuestions.count { it.id in reviewedIdSet }.coerceAtMost(baseReviewQuestions.size)
                                statusText = "AI 核对中：本次处理 ${limitedQuestions.size} 题，进度 ${processedBefore + 1}-${processedBefore + limitedQuestions.size}/${baseReviewQuestions.size}。"
                                isStatusWarn = false
                                importScope.launch {
                                    isImportBusy = true
                                    busyText = "AI 核对中……"
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            ShirohaAiClient.reviewQuestions(
                                                apiBaseUrl = QuizRepository.aiApiBaseUrl,
                                                apiKey = QuizRepository.aiApiKey,
                                                modelName = QuizRepository.aiModelName,
                                                questions = limitedQuestions,
                                                timeoutSeconds = QuizRepository.aiTimeoutSeconds
                                            )
                                        }
                                    }.onSuccess { suggestions ->
                                        aiReviewSuggestions = mergeAiReviewSuggestions(aiReviewSuggestions, suggestions, limitedQuestions)
                                        val aiWarnings = suggestionsToImportWarnings(suggestions, editableQuestions)
                                        importResult = displayResult.copy(
                                            warnings = mergeAiWarnings(
                                                currentWarnings = displayResult.warnings,
                                                aiWarnings = aiWarnings,
                                                processedQuestions = limitedQuestions
                                            )
                                        )
                                        val nextReviewedIds = (aiReviewedQuestionIds + limitedQuestions.map { it.id }).distinct()
                                        aiReviewedQuestionIds = nextReviewedIds
                                        val processedAfter = baseReviewQuestions.count { it.id in nextReviewedIds.toSet() }.coerceAtMost(baseReviewQuestions.size)
                                        previewOnlyAnomaly = aiWarnings.isNotEmpty() || previewOnlyAnomaly
                                        statusText = if (aiWarnings.isEmpty()) {
                                            "AI 核对完成：本批未发现重点问题，已处理 ${processedAfter}/${baseReviewQuestions.size} 题。"
                                        } else {
                                            "AI 核对完成：本批生成 ${aiWarnings.size} 条建议，已处理 ${processedAfter}/${baseReviewQuestions.size} 题。"
                                        } + if (processedAfter < baseReviewQuestions.size) " 可继续点击 AI 核对处理下一批。" else " 当前范围已处理完。"
                                        isStatusWarn = aiWarnings.isNotEmpty()
                                    }.onFailure { error ->
                                        statusText = "AI 核对失败：${error.message ?: "请检查接口配置"}"
                                        isStatusWarn = true
                                    }
                                    isImportBusy = false
                                    busyText = ""
                                }
                            }
                        )
                        ActionPillButton(
                            icon = Icons.Rounded.AutoAwesome,
                            text = "AI解析",
                            primary = QuizRepository.isAiConfigured() && QuizRepository.aiAnalysisEnabled,
                            modifier = Modifier.alpha(if (QuizRepository.isAiConfigured() && QuizRepository.aiAnalysisEnabled) 1f else ShirohaDimens.DisabledAlpha),
                            enabled = editableQuestions.isNotEmpty() && !isImportBusy,
                            onClick = {
                                if (!QuizRepository.isAiConfigured()) {
                                    showAiConfigPrompt = true
                                    statusText = "AI 解析：请先在个人偏好 → AI 设置中配置接口。"
                                    isStatusWarn = true
                                    return@ActionPillButton
                                }
                                if (!QuizRepository.aiAnalysisEnabled) {
                                    statusText = "AI 解析未启用，请先在个人偏好 → AI 设置中开启。"
                                    isStatusWarn = true
                                    return@ActionPillButton
                                }
                                val allAnalysisTargets = editableQuestions.filter(::shouldApplyAiAnalysis)
                                val anomalyAnalysisTargets = anomalyQuestions.filter(::shouldApplyAiAnalysis)
                                if (allAnalysisTargets.isEmpty()) {
                                    statusText = "AI 解析：当前没有缺少解析或解析过短的题目。"
                                    isStatusWarn = false
                                    return@ActionPillButton
                                }
                                val analyzedIdSet = aiAnalyzedQuestionIds.toSet()
                                val remainingAnomalyTargets = anomalyAnalysisTargets.filterNot { it.id in analyzedIdSet }
                                val remainingAllTargets = allAnalysisTargets.filterNot { it.id in analyzedIdSet }
                                val useAnomalyScope = QuizRepository.aiOnlyAnomaly && remainingAnomalyTargets.isNotEmpty()
                                val usingFallbackTargets = QuizRepository.aiOnlyAnomaly && !useAnomalyScope && remainingAllTargets.isNotEmpty()
                                val analysisTargetPool = if (useAnomalyScope) anomalyAnalysisTargets else allAnalysisTargets
                                val remainingAnalysisTargets = if (useAnomalyScope) remainingAnomalyTargets else remainingAllTargets
                                if (remainingAnalysisTargets.isEmpty()) {
                                    statusText = "AI 解析：当前缺解析题已全部尝试。若仍有题目缺解析，可能是模型未返回对应结果；可重新解析题库或调整单次题数后重试。"
                                    isStatusWarn = false
                                    return@ActionPillButton
                                }
                                val aiTargetQuestions = remainingAnalysisTargets.take(QuizRepository.aiMaxQuestions)
                                val processedBefore = analysisTargetPool.count { it.id in analyzedIdSet }.coerceAtMost(analysisTargetPool.size)
                                statusText = if (usingFallbackTargets) {
                                    "AI 解析中：异常题范围已无未尝试解析目标，已改为处理全部缺解析题；本次处理 ${aiTargetQuestions.size} 道，进度 ${processedBefore + 1}-${processedBefore + aiTargetQuestions.size}/${analysisTargetPool.size}。"
                                } else {
                                    "AI 解析中：本次处理 ${aiTargetQuestions.size} 道缺解析题，进度 ${processedBefore + 1}-${processedBefore + aiTargetQuestions.size}/${analysisTargetPool.size}。"
                                }
                                isStatusWarn = false
                                importScope.launch {
                                    isImportBusy = true
                                    busyText = "AI 解析生成中……"
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            ShirohaAiClient.generateAnalysis(
                                                apiBaseUrl = QuizRepository.aiApiBaseUrl,
                                                apiKey = QuizRepository.aiApiKey,
                                                modelName = QuizRepository.aiModelName,
                                                questions = aiTargetQuestions,
                                                timeoutSeconds = QuizRepository.aiTimeoutSeconds
                                            )
                                        }
                                    }.onSuccess { suggestions ->
                                        val suggestionMap = suggestions.associateBy { it.questionId }
                                        val appliedIds = mutableListOf<String>()
                                        val nextAnalyzedIds = (aiAnalyzedQuestionIds + aiTargetQuestions.map { it.id }).distinct()
                                        val nextQuestions = editableQuestions.map { question ->
                                            val suggestion = suggestionMap[question.id]
                                            if (suggestion != null && shouldApplyAiAnalysis(question) && suggestion.analysis.trim().isNotBlank()) {
                                                appliedIds += question.id
                                                applyAiAnalysisSuggestion(question, suggestion.analysis)
                                            } else {
                                                question
                                            }
                                        }
                                        syncEditableQuestions(nextQuestions, displayResult.warnings)
                                        aiAnalyzedQuestionIds = nextAnalyzedIds
                                        aiAnalysisAppliedQuestionIds = (aiAnalysisAppliedQuestionIds + appliedIds).distinct()
                                        val changedIds = appliedIds.toSet()
                                        val nextAnalyzedIdSet = nextAnalyzedIds.toSet()
                                        val skippedCount = (aiTargetQuestions.size - suggestionMap.keys.size).coerceAtLeast(0)
                                        val remainingAnalysisCount = editableQuestions.count { question ->
                                            shouldApplyAiAnalysis(question) && question.id !in nextAnalyzedIdSet
                                        }
                                        statusText = if (changedIds.isEmpty()) {
                                            "AI 解析完成：本批没有可写入的解析建议。"
                                        } else {
                                            "AI 解析完成：已为 ${changedIds.size} 道题写入待核对解析，保存前请人工确认。"
                                        } + if (skippedCount > 0) {
                                            " 本批有 ${skippedCount} 道未返回解析，已跳过以避免反复卡住。"
                                        } else {
                                            ""
                                        } + if (remainingAnalysisCount > 0) {
                                            " 仍有约 ${remainingAnalysisCount} 道缺解析题，可继续点击 AI 解析处理下一批。"
                                        } else {
                                            " 当前范围已处理完。"
                                        }
                                        isStatusWarn = false
                                    }.onFailure { error ->
                                        statusText = "AI 解析失败：${error.message ?: "请检查接口配置"}"
                                        isStatusWarn = true
                                    }
                                    isImportBusy = false
                                    busyText = ""
                                }
                            }
                        )

                        ActionPillButton(
                            icon = Icons.Rounded.CheckCircle,
                            text = "看AI建议 $aiSuggestionCount",
                            primary = aiSuggestionCount > 0,
                            enabled = aiSuggestionCount > 0,
                            onClick = {
                                reviewFilterName = ReviewFilter.AI_SUGGESTION.name
                                firstMatchingQuestionIndex(
                                    questions = editableQuestions,
                                    warnings = warnings,
                                    aiSuggestions = aiReviewSuggestions,
                                    aiReviewedQuestionIds = aiReviewedQuestionIds,
                                    aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                                    aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
                                    filter = ReviewFilter.AI_SUGGESTION
                                )?.let { reviewIndex = it }
                                reviewMode = true
                            }
                        )
                        ActionPillButton(
                            icon = Icons.Rounded.CheckCircle,
                            text = "看可采纳 $aiApplicableCount",
                            primary = aiApplicableCount > 0,
                            enabled = aiApplicableCount > 0,
                            onClick = {
                                reviewFilterName = ReviewFilter.AI_APPLICABLE.name
                                firstMatchingQuestionIndex(
                                    questions = editableQuestions,
                                    warnings = warnings,
                                    aiSuggestions = aiReviewSuggestions,
                                    aiReviewedQuestionIds = aiReviewedQuestionIds,
                                    aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                                    aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
                                    filter = ReviewFilter.AI_APPLICABLE
                                )?.let { reviewIndex = it }
                                reviewMode = true
                            }
                        )
                        ActionPillButton(
                            icon = Icons.Rounded.Description,
                            text = "看AI补解析 $aiAnalysisAppliedCount",
                            primary = aiAnalysisAppliedCount > 0,
                            enabled = aiAnalysisAppliedCount > 0,
                            onClick = {
                                reviewFilterName = ReviewFilter.AI_ANALYZED.name
                                firstMatchingQuestionIndex(
                                    questions = editableQuestions,
                                    warnings = warnings,
                                    aiSuggestions = aiReviewSuggestions,
                                    aiReviewedQuestionIds = aiReviewedQuestionIds,
                                    aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                                    aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
                                    filter = ReviewFilter.AI_ANALYZED
                                )?.let { reviewIndex = it }
                                reviewMode = true
                            }
                        )
                    }
                    val aiStatusText = statusText.takeIf { shouldShowAiStatusInImport(it) }
                    if (aiStatusText != null) {
                        Spacer(Modifier.height(10.dp))
                        NoticeCard(aiStatusText, warning = isStatusWarn)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "确认写入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionPillButton(
                            icon = Icons.Rounded.Save,
                            text = "新建题库",
                            primary = saveMode == ImportSaveMode.NEW_BANK.name,
                            modifier = Modifier.weight(1f),
                            fillWidthContent = true,
                            onClick = { saveMode = ImportSaveMode.NEW_BANK.name }
                        )
                        ActionPillButton(
                            icon = Icons.Rounded.Add,
                            text = "追加题目",
                            primary = saveMode == ImportSaveMode.APPEND_TO_BANK.name,
                            modifier = Modifier.weight(1f),
                            fillWidthContent = true,
                            onClick = {
                                saveMode = ImportSaveMode.APPEND_TO_BANK.name
                                if (appendTargetBankId == null) {
                                    appendTargetBankId = QuizRepository.activeBank()?.id ?: QuizRepository.banks.firstOrNull()?.id
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (saveMode == ImportSaveMode.NEW_BANK.name) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newBankGroupName,
                                onValueChange = { newBankGroupName = it },
                                label = { Text("一级分组") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newBankName,
                                onValueChange = { newBankName = it },
                                label = { Text("二级题库名") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            ActionPillButton(
                                icon = Icons.Rounded.Save,
                                text = "保存新题库",
                                primary = true,
                                onClick = {
                                    val cleanGroupName = newBankGroupName.trim().ifBlank { DEFAULT_BANK_GROUP_NAME }
                                    val bankName = newBankName.trim().ifBlank { defaultImportBankName(selectedFileName) }
                                    QuizRepository.importBank(context, bankName, editableQuestions, cleanGroupName)
                                    statusText = "已新建题库：$cleanGroupName / $bankName，共 ${editableQuestions.size} 题。"
                                    isStatusWarn = false
                                    onImportSaved()
                                }
                            )
                        }
                    } else {
                        val appendTargetBank = QuizRepository.banks.firstOrNull { it.id == appendTargetBankId }
                            ?: QuizRepository.activeBank()
                            ?: QuizRepository.banks.firstOrNull()
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NoticeCard("追加导入会保留旧题并添加新题；本版不会自动去重，也不会替换原题库。", warning = false)
                            if (QuizRepository.banks.isEmpty()) {
                                NoticeCard("当前没有可追加的旧题库，请先保存为新题库。", warning = true)
                            } else {
                                Text(
                                    text = "选择目标题库",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuizRepository.banks.forEach { bank ->
                                        val selected = bank.id == appendTargetBank?.id
                                        ActionPillButton(
                                            icon = Icons.Rounded.Done,
                                            text = bankDisplayPath(bank.groupName, bank.name),
                                            primary = selected,
                                            onClick = { appendTargetBankId = bank.id }
                                        )
                                    }
                                }
                                ActionPillButton(
                                    icon = Icons.Rounded.Add,
                                    text = "追加到选中题库",
                                    primary = true,
                                    enabled = appendTargetBank != null,
                                    onClick = {
                                        val target = appendTargetBank ?: return@ActionPillButton
                                        val oldCount = target.questions.size
                                        val success = QuizRepository.appendQuestionsToBank(context, target.id, editableQuestions)
                                        statusText = if (success) {
                                            "已追加到：${bankDisplayPath(target.groupName, target.name)}，新增 ${editableQuestions.size} 题，当前共 ${oldCount + editableQuestions.size} 题。"
                                        } else {
                                            "追加失败：没有找到目标题库。"
                                        }
                                        isStatusWarn = !success
                                        if (success) onImportSaved()
                                    }
                                )
                            }
                        }
                    }
                }

                NativeImportPreview(
                    questions = previewQuestions,
                    totalQuestionCount = editableQuestions.size,
                    onlyShowAnomaly = previewOnlyAnomaly,
                    aiSuggestions = aiReviewSuggestions,
                    aiReviewedQuestionIds = aiReviewedQuestionIds,
                    aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                    aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds
                )
            }

            if (importResult == null && rawText.isNotBlank()) {
                LoadingIllustration(
                    text = "准备好以后，点击“开始解析”。",
                    imageRes = R.drawable.illus_loading_state_webp
                )
            }
        }
    }

    if (showAiConfigPrompt) {
        AlertDialog(
            onDismissRequest = { showAiConfigPrompt = false },
            title = { Text("需要配置 AI 接口") },
            text = { Text("请先在 我的 → AI 设置 中配置接口。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAiConfigPrompt = false
                        onOpenPreference()
                    }
                ) {
                    Text("去配置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiConfigPrompt = false }) {
                    Text("稍后再说")
                }
            }
        )
    }
}

private const val LARGE_TEXT_PREVIEW_THRESHOLD = 5000
private const val AI_WARNING_ID_MARKER = "__AI_QID__="
private const val IMPORT_WARNING_ID_MARKER = "__IMPORT_QID__="
private const val LARGE_TEXT_PREVIEW_CHARS = 1200
private const val IMPORT_FILE_WARN_BYTES = 30L * 1024L * 1024L
private const val IMPORT_FILE_BLOCK_BYTES = 80L * 1024L * 1024L

@Composable
private fun LargeImportTextPreview(
    text: String,
    label: String,
    showEditButton: Boolean = true,
    onEditFullText: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShirohaRadius.Lg),
        color = ShirohaColors.CardWhite78,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSoft)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (showEditButton) {
                    Spacer(Modifier.width(10.dp))
                    ActionPillButton(
                        icon = Icons.Rounded.Edit,
                        text = "编辑全文",
                        primary = false,
                        onClick = onEditFullText
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SelectionContainer {
                Text(
                    text = text.take(LARGE_TEXT_PREVIEW_CHARS).trimEnd() + if (text.length > LARGE_TEXT_PREVIEW_CHARS) "\n……" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FullImportTextEditorScreen(
    title: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onBack: () -> Unit
) {
    var showFindReplaceDialog by rememberSaveable { mutableStateOf(false) }
    var findText by rememberSaveable { mutableStateOf("") }
    var replaceText by rememberSaveable { mutableStateOf("") }
    var useRegexFind by rememberSaveable { mutableStateOf(false) }
    var findReplaceError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Sm)) {
            Text(
                text = "Edit",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                EditorSaveButton(onClick = onBack)
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(ShirohaRadius.Lg),
            color = ShirohaColors.CardSoft,
            border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSoft)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ShirohaSpacing.Xl)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "全文编辑",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillButton(
                        icon = Icons.Rounded.Search,
                        text = "查找",
                        primary = false,
                        onClick = { showFindReplaceDialog = true }
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    minLines = 16,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text(placeholder) }
                )
            }
        }
    }

    if (showFindReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showFindReplaceDialog = false },
            title = { Text("查找 / 替换") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = findText,
                        onValueChange = { findText = it },
                        label = { Text("查找内容") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        label = { Text("替换内容（留空则删除）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (useRegexFind) "正则模式已开启" else "普通文本匹配",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            useRegexFind = !useRegexFind
                            findReplaceError = null
                        }) {
                            Text(if (useRegexFind) "关闭正则" else "使用正则")
                        }
                    }
                    Text(
                        text = if (useRegexFind) {
                            "会按正则表达式替换全部匹配内容；替换内容不填时，将直接删除匹配文本。"
                        } else {
                            "会替换全部匹配内容；替换内容不填时，将直接删除匹配文本。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    findReplaceError?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = findText.isNotBlank(),
                    onClick = {
                        val updatedText = if (useRegexFind) {
                            runCatching { Regex(findText).replace(value, replaceText) }
                                .onFailure { error ->
                                    findReplaceError = "正则表达式无效：${error.message ?: "请检查语法"}"
                                }
                                .getOrNull()
                        } else {
                            value.replace(findText, replaceText)
                        }
                        if (updatedText != null) {
                            onValueChange(updatedText)
                            findReplaceError = null
                            showFindReplaceDialog = false
                        }
                    }
                ) {
                    Text(if (replaceText.isBlank()) "删除" else "全部替换")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFindReplaceDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun EditorSaveButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(ShirohaRadius.Pill)
    Surface(
        modifier = Modifier
            .height(38.dp)
            .shirohaNoRippleClickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(ShirohaDimens.Hairline, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = "保存更改",
                modifier = Modifier.size(16.dp),
                tint = ShirohaColors.TextOnBrand
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "保存更改",
                color = ShirohaColors.TextOnBrand,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ImportModeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ActionPillButton(
        icon = icon,
        text = text,
        primary = selected,
        modifier = modifier,
        fillWidthContent = true,
        onClick = onClick
    )
}


@Composable
private fun ReviewTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 32.dp)
            .shirohaNoRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (selected) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardMuted,
        border = BorderStroke(
            1.dp,
            if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.primary else ShirohaColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun ReviewCompactButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 38.dp)
            .shirohaNoRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (primary) MaterialTheme.colorScheme.primary else ShirohaColors.CardWhite86,
        border = BorderStroke(ShirohaDimens.Hairline, if (primary) MaterialTheme.colorScheme.primary else ShirohaColors.LineStrong)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(15.dp),
                tint = if (primary) ShirohaColors.TextOnBrand else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                color = if (primary) ShirohaColors.TextOnBrand else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NativeImportSummary(
    result: ImportResult,
    aiSuggestions: List<AiReviewSuggestion>,
    aiReviewedQuestionIds: List<String>,
    aiAnalyzedQuestionIds: List<String>,
    aiAnalysisAppliedQuestionIds: List<String>
) {
    val hardCount = result.warnings.count { it.level == WarningLevel.ERROR }
    val softCount = result.warnings.count { it.level == WarningLevel.WARNING }
    val answeredCount = result.questions.count { it.answer.isNotEmpty() }
    val imageQuestionCount = result.questions.count { it.images.isNotEmpty() }
    val missingAnalysisCount = result.questions.count(::shouldApplyAiAnalysis)
    val aiReviewedIdSet = aiReviewedQuestionIds.toSet()
    val aiAnalyzedIdSet = aiAnalyzedQuestionIds.toSet()
    val aiAnalysisAppliedIdSet = aiAnalysisAppliedQuestionIds.toSet()
    val aiReviewSuggestionCount = result.questions.count { question ->
        aiSuggestionsForQuestion(question, aiSuggestions).any(::isActionableAiSuggestion)
    }
    val aiApplicableCount = result.questions.count { question ->
        aiSuggestionsForQuestion(question, aiSuggestions).any(::canApplyAiSuggestion)
    }
    val aiNeedConfirmCount = result.questions.count { question ->
        aiSuggestionsForQuestion(question, aiSuggestions).any(::isNeedHumanReviewAiSuggestion)
    }
    val aiHardErrorCount = result.questions.count { question ->
        aiSuggestionsForQuestion(question, aiSuggestions).any(::isHardErrorAiSuggestion)
    }
    val aiReviewedCount = result.questions.count { it.id in aiReviewedIdSet }
    val aiAnalyzedCount = result.questions.count { it.id in aiAnalyzedIdSet }
    val aiAnalysisAppliedCount = result.questions.count { it.id in aiAnalysisAppliedIdSet }

    GlassCard {
        Text(
            text = "解析结果",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusChip("策略：${result.strategyName}", selected = true)
            StatusChip("识别题数：${result.questions.size}", selected = true)
            StatusChip("已识别答案：$answeredCount", selected = answeredCount == result.questions.size)
            if (imageQuestionCount > 0) StatusChip("图片题：$imageQuestionCount", selected = true)
            StatusChip("硬错误：$hardCount", selected = hardCount == 0)
            StatusChip("提示：$softCount", selected = softCount == 0)
            StatusChip("缺/短解析：$missingAnalysisCount", selected = missingAnalysisCount == 0)
            if (aiReviewedCount > 0) StatusChip("AI已核对：$aiReviewedCount", selected = true)
            if (aiReviewSuggestionCount > 0) StatusChip("AI建议：$aiReviewSuggestionCount", selected = true)
            if (aiApplicableCount > 0) StatusChip("可采纳：$aiApplicableCount", selected = true)
            if (aiNeedConfirmCount > 0) StatusChip("需确认：$aiNeedConfirmCount", selected = false)
            if (aiHardErrorCount > 0) StatusChip("AI硬错误：$aiHardErrorCount", selected = false)
            if (aiAnalyzedCount > 0) StatusChip("AI已尝试解析：$aiAnalyzedCount", selected = true)
            if (aiAnalysisAppliedCount > 0) StatusChip("AI已补解析：$aiAnalysisAppliedCount", selected = true)
        }
        if (result.warnings.isNotEmpty()) {
            val (globalWarnings, questionWarnings) = result.warnings.partition { warning ->
                result.questions.none { question -> warningBelongsToQuestion(warning, question) }
            }
            if (globalWarnings.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "全局提示",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                globalWarnings.forEach { warning ->
                    NoticeCard(
                        text = importWarningSummaryText(warning, result.questions),
                        warning = warning.level != WarningLevel.NORMAL
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (questionWarnings.isNotEmpty()) {
                Spacer(Modifier.height(if (globalWarnings.isEmpty()) 14.dp else 6.dp))
                Text(
                    text = "题目提示",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                questionWarnings.take(6).forEach { warning ->
                    NoticeCard(
                        text = importWarningSummaryText(warning, result.questions),
                        warning = warning.level != WarningLevel.NORMAL
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (questionWarnings.size > 6) {
                    Text(
                        text = "另有 ${questionWarnings.size - 6} 条题目提示，请在沉浸核对中查看对应题目。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (result.diagnostics.notes.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "候选策略诊断",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            result.diagnostics.notes.take(4).forEach { note ->
                Text(
                    text = "• $note",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun NativeImportPreview(
    questions: List<Question>,
    totalQuestionCount: Int,
    onlyShowAnomaly: Boolean,
    aiSuggestions: List<AiReviewSuggestion>,
    aiReviewedQuestionIds: List<String>,
    aiAnalyzedQuestionIds: List<String>,
    aiAnalysisAppliedQuestionIds: List<String>
) {
    GlassCard {
        Text(
            text = if (onlyShowAnomaly) "异常题预览" else "快速预览",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        if (questions.isEmpty()) {
            NoticeCard(
                text = if (onlyShowAnomaly) "当前没有可预览的异常题。" else "当前没有可预览的题目。",
                warning = false
            )
            return@GlassCard
        }
        questions.take(8).forEach { question ->
            val answerText = answerDisplayText(question)
            val optionText = question.options.joinToString("  ") { "${it.key}. ${it.text}" }

            Text(
                text = "${question.number}. ${typeLabel(question.type)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            SelectionContainer {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (question.images.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                QuestionImagesBlock(question.images, maxPreviewHeight = 220.dp, showMeta = true)
            }
            if (optionText.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = optionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "答案：$answerText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val questionTags = importPreviewQuestionTags(
                question = question,
                aiSuggestions = aiSuggestionsForQuestion(question, aiSuggestions),
                aiReviewedQuestionIds = aiReviewedQuestionIds,
                aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds
            )
            if (questionTags.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = questionTags,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (question.analysis.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "解析：${question.analysis}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(18.dp))
        }
        if (questions.size > 8) {
            NoticeCard("当前仅展示前 8 题用于快速预览，完整核对请进入沉浸核对。", warning = false)
        } else if (onlyShowAnomaly && questions.size < totalQuestionCount) {
            NoticeCard("当前仅预览异常题。点击上方“显示全部题”可恢复全部预览。", warning = false)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun NativeQuestionReviewScreen(
    questions: List<Question>,
    warnings: List<ImportWarning>,
    aiSuggestions: List<AiReviewSuggestion>,
    aiReviewedQuestionIds: List<String>,
    aiAnalyzedQuestionIds: List<String>,
    aiAnalysisAppliedQuestionIds: List<String>,
    filter: ReviewFilter,
    currentIndex: Int,
    focusFilterListTick: Int,
    onFilterChange: (ReviewFilter) -> Unit,
    onIndexChange: (Int) -> Unit,
    onQuestionChange: (Int, Question) -> Unit,
    onApplyAiSuggestion: (Int, AiReviewSuggestion) -> Unit,
    onDeleteQuestion: (Int) -> Unit,
    onEditQuestion: (Int, Boolean) -> Unit,
    onBack: () -> Unit
) {
    if (questions.isEmpty()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
        ) {
            ShirohaHeader(
                kicker = "Review",
                title = "沉浸核对",
                subtitle = "当前没有可核对的题目。"
            )
            ActionPillButton(
                icon = Icons.Rounded.ArrowBack,
                text = "返回导入页",
                primary = false,
                onClick = onBack
            )
        }
        return
    }

    val activeFilterCount = reviewFilterCount(
        questions = questions,
        warnings = warnings,
        aiSuggestions = aiSuggestions,
        aiReviewedQuestionIds = aiReviewedQuestionIds,
        aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
        aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
        filter = filter
    )
    val activeFilter = if (filter != ReviewFilter.ALL && activeFilterCount <= 0) ReviewFilter.ALL else filter
    val allIndices = questions.indices.toList()
    val filteredIndices = questions.indices.filter { index ->
        val candidate = questions[index]
        questionMatchesFilter(
            question = candidate,
            warnings = warningsForQuestion(candidate, warnings),
            filter = activeFilter,
            aiSuggestions = aiSuggestionsForQuestion(candidate, aiSuggestions),
            aiReviewedQuestionIds = aiReviewedQuestionIds,
            aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
            aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds
        )
    }
    val visibleIndices = if (activeFilter == ReviewFilter.ALL) allIndices else filteredIndices
    val safeIndex = when {
        questions.isEmpty() -> 0
        activeFilter == ReviewFilter.ALL -> currentIndex.coerceIn(0, questions.lastIndex)
        currentIndex in visibleIndices -> currentIndex
        visibleIndices.isNotEmpty() -> visibleIndices.first()
        else -> currentIndex.coerceIn(0, questions.lastIndex)
    }
    val question = questions[safeIndex]
    val questionWarnings = warningsForQuestion(question, warnings)
    val questionAiSuggestions = aiSuggestions.filter { it.questionId == question.id && isActionableAiSuggestion(it) }
    val visiblePosition = visibleIndices.indexOf(safeIndex).takeIf { it >= 0 } ?: 0
    val reviewScrollState = rememberScrollState()
    val filterListBringIntoViewRequester = remember { BringIntoViewRequester() }
    var filterListRootY by remember { mutableStateOf(0f) }
    val focusTopOffsetPx = with(LocalDensity.current) { 24.dp.toPx() }

    LaunchedEffect(focusFilterListTick, activeFilter, visibleIndices.size, safeIndex) {
        if (focusFilterListTick > 0 && activeFilter != ReviewFilter.ALL && visibleIndices.size > 1) {
            delay(120)
            val targetScroll = (reviewScrollState.value + filterListRootY - focusTopOffsetPx)
                .roundToInt()
                .coerceAtLeast(0)
            reviewScrollState.animateScrollTo(targetScroll)
            filterListBringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(reviewScrollState)
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Review",
            title = "沉浸核对",
            subtitle = "逐题核对导入结果，可先筛选异常或无答案。"
        )

        GlassCard {
            Text(
                text = "核对筛选",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReviewFilter.values().forEach { item ->
                    val count = reviewFilterCount(
                        questions = questions,
                        warnings = warnings,
                        aiSuggestions = aiSuggestions,
                        aiReviewedQuestionIds = aiReviewedQuestionIds,
                        aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                        aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
                        filter = item
                    )
                    if (item == ReviewFilter.ALL || count > 0) {
                        ReviewTypeChip(
                            text = "${reviewFilterLabel(item)} $count",
                            selected = activeFilter == item,
                            onClick = { onFilterChange(item) }
                        )
                    }
                }
            }
            if (activeFilter != ReviewFilter.ALL && visibleIndices.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                NoticeCard("当前筛选下没有需要核对的题目，可以切换到“全部”继续浏览。", warning = false)
            }
        }

        if (activeFilter != ReviewFilter.ALL && visibleIndices.isEmpty()) {
            ActionPillButton(
                icon = Icons.Rounded.ArrowBack,
                text = "返回导入页",
                primary = false,
                onClick = onBack
            )
            return@Column
        }

        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "第 ${safeIndex + 1} / ${questions.size} 题",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(typeLabel(question.type))
                            append(" · 答案：")
                            append(answerDisplayText(question))
                            if (activeFilter != ReviewFilter.ALL) {
                                append(" · ${reviewFilterLabel(activeFilter)} ${visiblePosition + 1}/${visibleIndices.size}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ReviewCompactButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "保存返回",
                    primary = true,
                    onClick = onBack
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReviewCompactButton(
                    icon = Icons.Rounded.Edit,
                    text = "编辑本题",
                    modifier = Modifier.weight(1f),
                    onClick = { onEditQuestion(safeIndex, false) }
                )
                ReviewCompactButton(
                    icon = Icons.Rounded.ArrowBack,
                    text = if (activeFilter == ReviewFilter.ALL) "上一题" else "上一条",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val target = previousIndexInList(visibleIndices, safeIndex) ?: (safeIndex - 1)
                        onIndexChange(target)
                    }
                )
                ReviewCompactButton(
                    icon = Icons.Rounded.ArrowForward,
                    text = if (activeFilter == ReviewFilter.ALL) "下一题" else "下一条",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val target = nextIndexInList(visibleIndices, safeIndex) ?: (safeIndex + 1)
                        onIndexChange(target)
                    }
                )
            }
        }

        ReviewQuestionAssistBlocks(
            question = question,
            questionWarnings = questionWarnings,
            questionAiSuggestions = questionAiSuggestions,
            aiReviewedQuestionIds = aiReviewedQuestionIds,
            aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
            aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
            onApplyAiSuggestion = { suggestion -> onApplyAiSuggestion(safeIndex, suggestion) }
        )

        if (activeFilter != ReviewFilter.ALL && visibleIndices.size > 1) {
            ReviewFilteredJumpList(
                questions = questions,
                indices = visibleIndices,
                currentIndex = safeIndex,
                warnings = warnings,
                onIndexChange = onIndexChange,
                onEditQuestion = { index -> onEditQuestion(index, true) },
                modifier = Modifier
                    .bringIntoViewRequester(filterListBringIntoViewRequester)
                    .onGloballyPositioned { coordinates ->
                        filterListRootY = coordinates.positionInRoot().y
                    }
            )
        }

        ReviewQuestionEditorContent(
            question = question,
            onQuestionChange = { updatedQuestion -> onQuestionChange(safeIndex, updatedQuestion) },
            onDeleteQuestion = { onDeleteQuestion(safeIndex) }
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewQuestionEditScreen(
    question: Question,
    questionIndex: Int,
    totalCount: Int,
    questionWarnings: List<ImportWarning>,
    questionAiSuggestions: List<AiReviewSuggestion>,
    aiReviewedQuestionIds: List<String>,
    aiAnalyzedQuestionIds: List<String>,
    aiAnalysisAppliedQuestionIds: List<String>,
    onQuestionChange: (Question) -> Unit,
    onApplyAiSuggestion: (AiReviewSuggestion) -> Unit,
    onDeleteQuestion: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Edit",
            title = "编辑题目",
            subtitle = "第 ${questionIndex + 1} / $totalCount 题 · ${typeLabel(question.type)} · 答案：${answerDisplayText(question)}"
        )

        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "题目编辑",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "修改内容会同步回沉浸核对列表。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ReviewCompactButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "保存更改",
                    primary = true,
                    onClick = onBack
                )
            }
        }

        ReviewQuestionAssistBlocks(
            question = question,
            questionWarnings = questionWarnings,
            questionAiSuggestions = questionAiSuggestions,
            aiReviewedQuestionIds = aiReviewedQuestionIds,
            aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
            aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds,
            onApplyAiSuggestion = onApplyAiSuggestion
        )

        ReviewQuestionEditorContent(
            question = question,
            onQuestionChange = onQuestionChange,
            onDeleteQuestion = onDeleteQuestion
        )
    }
}

@Composable
private fun ReviewQuestionAssistBlocks(
    question: Question,
    questionWarnings: List<ImportWarning>,
    questionAiSuggestions: List<AiReviewSuggestion>,
    aiReviewedQuestionIds: List<String>,
    aiAnalyzedQuestionIds: List<String>,
    aiAnalysisAppliedQuestionIds: List<String>,
    onApplyAiSuggestion: (AiReviewSuggestion) -> Unit
) {
    if (questionWarnings.isNotEmpty()) {
        GlassCard {
            Text(
                text = "本题提示",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            questionWarnings.forEach { warning ->
                NoticeCard(displayImportWarningMessage(warning.message), warning = warning.level != WarningLevel.NORMAL)
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (question.id in aiReviewedQuestionIds.toSet() && questionAiSuggestions.isEmpty()) {
        GlassCard {
            Text(
                text = "AI 核对状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            NoticeCard("AI 已核对本题：未发现需要显示的重点问题。", warning = false)
        }
    }

    if (shouldApplyAiAnalysis(question) || question.id in aiAnalyzedQuestionIds.toSet() || question.id in aiAnalysisAppliedQuestionIds.toSet()) {
        GlassCard {
            Text(
                text = "AI 解析状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            NoticeCard(
                text = analysisStatusText(
                    question = question,
                    aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
                    aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds
                ),
                warning = shouldApplyAiAnalysis(question) && question.id !in aiAnalysisAppliedQuestionIds.toSet()
            )
        }
    }

    if (questionAiSuggestions.isNotEmpty()) {
        GlassCard {
            Text(
                text = "AI 核对建议",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            questionAiSuggestions.forEach { suggestion ->
                AiReviewSuggestionCard(
                    suggestion = suggestion,
                    onApply = { onApplyAiSuggestion(suggestion) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewQuestionEditorContent(
    question: Question,
    onQuestionChange: (Question) -> Unit,
    onDeleteQuestion: (() -> Unit)? = null
) {
    var showRemoveImagesConfirm by remember(question.id) { mutableStateOf(false) }
    var showDeleteLastOptionConfirm by remember(question.id) { mutableStateOf(false) }
    var showDeleteQuestionConfirm by remember(question.id) { mutableStateOf(false) }

    if (showRemoveImagesConfirm) {
        ShirohaDangerConfirmDialog(
            title = "确认移除本题图片？",
            message = "这会从当前核对题目中移除已绑定图片。保存题库时会使用移除图片后的题目。",
            confirmText = "确认移除",
            onDismiss = { showRemoveImagesConfirm = false },
            onConfirm = {
                onQuestionChange(question.copy(images = emptyList()))
                showRemoveImagesConfirm = false
            }
        )
    }

    if (showDeleteLastOptionConfirm) {
        ShirohaDangerConfirmDialog(
            title = "确认删除最后一个选项？",
            message = "这会删除当前题目的最后一个选项。保存题库时会使用删除后的题目。",
            confirmText = "确认删除",
            onDismiss = { showDeleteLastOptionConfirm = false },
            onConfirm = {
                if (question.options.isNotEmpty()) {
                    onQuestionChange(question.copy(options = question.options.dropLast(1)))
                }
                showDeleteLastOptionConfirm = false
            }
        )
    }

    if (showDeleteQuestionConfirm) {
        ShirohaDangerConfirmDialog(
            title = "确认删除本题？",
            message = "这会从当前核对列表中删除本题。保存题库时会使用删除后的题目列表。",
            confirmText = "确认删除",
            onDismiss = { showDeleteQuestionConfirm = false },
            onConfirm = {
                onDeleteQuestion?.invoke()
                showDeleteQuestionConfirm = false
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)) {
        GlassCard {
            Text(
                text = "题目内容",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = question.number,
                    onValueChange = { value ->
                        onQuestionChange(question.copy(number = value))
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("题号") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = question.category,
                    onValueChange = { value ->
                        onQuestionChange(question.copy(category = value))
                    },
                    modifier = Modifier.weight(1.4f),
                    label = { Text("分区/来源") },
                    singleLine = true
                )
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuestionType.values().forEach { type ->
                    ReviewTypeChip(
                        text = typeLabel(type),
                        selected = question.type == type,
                        onClick = {
                            onQuestionChange(normalizeAfterTypeChange(question, type))
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = question.question,
                onValueChange = { value ->
                    onQuestionChange(question.copy(question = value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                minLines = 6,
                label = { Text("题干") },
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }

        if (question.images.isNotEmpty()) {
            GlassCard {
                Text(
                    text = "题目图片",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                NoticeCard("图片按导入文档中的位置自动绑定。资料分析类共享图片会引用到后续题目；请重点核对图片是否属于本题。", warning = false)
                Spacer(Modifier.height(12.dp))
                QuestionImagesBlock(question.images, maxPreviewHeight = 360.dp, showMeta = true)
                Spacer(Modifier.height(12.dp))
                ActionPillButton(
                    icon = Icons.Rounded.Delete,
                    text = "移除本题图片",
                    primary = false,
                    onClick = { showRemoveImagesConfirm = true }
                )
            }
        }

        GlassCard {
            Text(
                text = "选项",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            if (question.options.isEmpty()) {
                NoticeCard("当前题目没有选项。判断题可以点击“补齐判断选项”，选择题可以点击“添加选项”。", warning = false)
                Spacer(Modifier.height(12.dp))
            }
            question.options.forEachIndexed { optionIndex, option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = option.key,
                        onValueChange = { value ->
                            val updated = question.options.mapIndexed { currentIndex, item ->
                                if (currentIndex == optionIndex) item.copy(key = value.uppercase().take(2)) else item
                            }
                            onQuestionChange(question.copy(options = updated))
                        },
                        modifier = Modifier.width(74.dp),
                        label = { Text("项") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = option.text,
                        onValueChange = { value ->
                            val updated = question.options.mapIndexed { currentIndex, item ->
                                if (currentIndex == optionIndex) item.copy(text = value) else item
                            }
                            onQuestionChange(question.copy(options = updated))
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("选项内容") }
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.Add,
                    text = "添加选项",
                    primary = false,
                    onClick = {
                        val key = nextOptionKey(question.options)
                        onQuestionChange(question.copy(options = question.options + Option(key, "")))
                    }
                )
                ActionPillButton(
                    icon = Icons.Rounded.RemoveCircle,
                    text = "删除最后选项",
                    primary = false,
                    onClick = {
                        if (question.options.isNotEmpty()) {
                            showDeleteLastOptionConfirm = true
                        }
                    }
                )
                ActionPillButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "补齐判断选项",
                    primary = false,
                    onClick = {
                        onQuestionChange(
                            question.copy(
                                type = QuestionType.JUDGE,
                                options = defaultJudgeOptions(),
                                answer = if (question.answer.isEmpty()) listOf("A") else question.answer,
                                blankAnswers = emptyList()
                            )
                        )
                    }
                )
            }
        }

        GlassCard {
            Text(
                text = "答案与解析",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "单选 A，多选 ABC，判断 正确/错误；多空填空可逐空配置主答案与备选答案。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            val detectedBlankCount = MultiBlankSupport.countExplicitBlanks(question.question)
            if (question.type == QuestionType.BLANK && question.blankAnswers.isNotEmpty()) {
                MultiBlankAnswerEditor(
                    blankAnswers = question.blankAnswers,
                    detectedBlankCount = detectedBlankCount,
                    onChange = { groups -> onQuestionChange(MultiBlankSupport.withBlankAnswers(question, groups)) },
                    onDisable = {
                        onQuestionChange(
                            question.copy(
                                answer = MultiBlankSupport.compatibilityAnswer(question.blankAnswers),
                                blankAnswers = emptyList()
                            )
                        )
                    }
                )
            } else {
                OutlinedTextField(
                    value = answerInputText(question),
                    onValueChange = { value ->
                        onQuestionChange(
                            question.copy(
                                answer = parseReviewAnswer(value, question.type),
                                blankAnswers = emptyList()
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("答案") },
                    singleLine = true
                )
                if (question.type == QuestionType.BLANK && detectedBlankCount > 1) {
                    Spacer(Modifier.height(10.dp))
                    ActionPillButton(
                        icon = Icons.Rounded.Add,
                        text = "启用多空答案",
                        primary = false,
                        onClick = {
                            onQuestionChange(
                                MultiBlankSupport.withBlankAnswers(
                                    question,
                                    MultiBlankSupport.initialGroups(question.question, question.answer)
                                )
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = question.analysis,
                onValueChange = { value ->
                    onQuestionChange(question.copy(analysis = value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                minLines = 5,
                label = { Text("解析") },
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(10.dp))
            AiAnalysisFillPanel(
                question = question,
                currentAnalysis = question.analysis,
                onApplyAnalysis = { value ->
                    onQuestionChange(question.copy(analysis = value))
                }
            )
        }

        GlassCard {
            Text(
                text = "危险操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "误识别的说明、页眉页脚或碎片，可删除本题。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            ActionPillButton(
                icon = Icons.Rounded.Delete,
                text = "删除本题",
                primary = false,
                onClick = { showDeleteQuestionConfirm = true }
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiReviewSuggestionCard(
    suggestion: AiReviewSuggestion,
    onApply: () -> Unit
) {
    val riskText = when (suggestion.riskLevel.lowercase()) {
        "auto_safe" -> "低风险"
        "hard_error" -> "硬错误"
        else -> "需确认"
    }
    val issueText = suggestion.issueTypes.takeIf { it.isNotEmpty() }?.joinToString("、").orEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ShirohaColors.CardWhite62,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSoft)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = buildString {
                    append("AI 建议")
                    append(" · ").append(riskText)
                    if (suggestion.confidence > 0.0) append(" · 置信度 ").append((suggestion.confidence * 100).toInt()).append("%")
                    if (issueText.isNotBlank()) append(" · ").append(issueText)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            if (suggestion.reason.isNotBlank()) {
                Text(
                    text = suggestion.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (suggestion.suggestion.isNotBlank()) {
                Text(
                    text = "建议：${suggestion.suggestion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val applySummary = aiSuggestionApplySummary(suggestion)
            if (applySummary.isNotBlank()) {
                Text(
                    text = "可采纳内容：$applySummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = if (canApplyAiSuggestion(suggestion)) "采纳 AI 建议" else "仅提示，需手动处理",
                    primary = canApplyAiSuggestion(suggestion),
                    enabled = canApplyAiSuggestion(suggestion),
                    onClick = onApply
                )
            }
        }
    }
}


private data class AiRefactorApplyResult(
    val refactorResult: AiRefactorResult,
    val reparsedResult: ImportResult?
)

private fun shouldShowAiStatusInImport(text: String): Boolean {
    return text.startsWith("AI ") ||
        text.startsWith("AI核对") ||
        text.startsWith("AI解析") ||
        text.contains("AI 重构") ||
        text.contains("AI 核对") ||
        text.contains("AI 解析")
}


private fun aiRefactorImportWarnings(
    notes: List<String>,
    baseMessage: String = "AI重构已生成新的待核对结果，请人工确认题量、题干、选项、答案和解析后再保存。"
): List<ImportWarning> {
    val base = listOf(
        ImportWarning(
            level = WarningLevel.WARNING,
            questionNumber = null,
            message = baseMessage
        )
    )
    val noteWarnings = notes.take(10).map { note ->
        ImportWarning(
            level = WarningLevel.WARNING,
            questionNumber = null,
            message = "AI重构提示：$note"
        )
    }
    return dedupeImportWarnings(base + noteWarnings)
}

private fun aiSuggestionsForQuestion(
    question: Question,
    suggestions: List<AiReviewSuggestion>
): List<AiReviewSuggestion> {
    return suggestions.filter { it.questionId == question.id && isActionableAiSuggestion(it) }
}

private fun isHardErrorAiSuggestion(suggestion: AiReviewSuggestion): Boolean {
    return suggestion.riskLevel.equals("hard_error", ignoreCase = true)
}

private fun isNeedHumanReviewAiSuggestion(suggestion: AiReviewSuggestion): Boolean {
    return isActionableAiSuggestion(suggestion) && (
        suggestion.needHumanReview ||
            !canApplyAiSuggestion(suggestion) ||
            (suggestion.riskLevel.equals("needs_confirm", ignoreCase = true) || suggestion.riskLevel.equals("needs_review", ignoreCase = true))
        )
}

private fun importPreviewQuestionTags(
    question: Question,
    aiSuggestions: List<AiReviewSuggestion>,
    aiReviewedQuestionIds: List<String>,
    aiAnalyzedQuestionIds: List<String>,
    aiAnalysisAppliedQuestionIds: List<String>
): String {
    val tags = mutableListOf<String>()
    if (shouldApplyAiAnalysis(question)) tags += "缺/短解析"
    if (question.id in aiReviewedQuestionIds.toSet()) tags += "AI已核对"
    if (question.id in aiReviewedQuestionIds.toSet() && aiSuggestions.isEmpty()) tags += "未发现重点问题"
    if (question.id in aiAnalyzedQuestionIds.toSet()) tags += "AI已尝试解析"
    if (question.id in aiAnalysisAppliedQuestionIds.toSet()) tags += "AI已补解析"
    if (aiSuggestions.isNotEmpty()) tags += "AI建议"
    if (aiSuggestions.any(::canApplyAiSuggestion)) tags += "可采纳"
    if (aiSuggestions.any(::isNeedHumanReviewAiSuggestion)) tags += "需确认"
    if (aiSuggestions.any(::isHardErrorAiSuggestion)) tags += "AI硬错误"
    return tags.distinct().joinToString(" · ")
}

private fun analysisStatusText(
    question: Question,
    aiAnalyzedQuestionIds: List<String>,
    aiAnalysisAppliedQuestionIds: List<String>
): String {
    val parts = mutableListOf<String>()
    if (shouldApplyAiAnalysis(question)) parts += "当前仍缺少有效解析或解析偏短"
    if (question.id in aiAnalyzedQuestionIds.toSet()) parts += "AI 已尝试解析本题"
    if (question.id in aiAnalysisAppliedQuestionIds.toSet()) parts += "AI 已写入补充解析，保存前请人工确认"
    if (parts.isEmpty()) parts += "当前解析长度基本正常"
    return parts.joinToString("；")
}

private fun suggestionsToImportWarnings(
    suggestions: List<AiReviewSuggestion>,
    questions: List<Question>
): List<ImportWarning> {
    val questionById = questions.associateBy { it.id }
    return suggestions
        .filter(::isActionableAiSuggestion)
        .mapNotNull { suggestion ->
            val question = questionById[suggestion.questionId] ?: return@mapNotNull null
            val issueText = suggestion.issueTypes.takeIf { it.isNotEmpty() }?.joinToString("、").orEmpty()
            val message = buildString {
                append("AI建议")
                if (issueText.isNotBlank()) append("[").append(issueText).append("]")
                if (suggestion.reason.isNotBlank()) append("：").append(suggestion.reason)
                if (suggestion.suggestion.isNotBlank()) append("；建议：").append(suggestion.suggestion)
                val applySummary = aiSuggestionApplySummary(suggestion)
                if (applySummary.isNotBlank()) {
                    append(if (canApplyAiSuggestion(suggestion)) "；可采纳：" else "；建议内容：")
                    append(applySummary)
                }
                append("；").append(AI_WARNING_ID_MARKER).append(question.id)
            }
            val hard = suggestion.status.equals("error", ignoreCase = true) ||
                suggestion.riskLevel.equals("hard_error", ignoreCase = true)
            ImportWarning(
                level = if (hard) WarningLevel.ERROR else WarningLevel.WARNING,
                questionNumber = question.number,
                message = message.ifBlank { "AI 建议人工确认本题；$AI_WARNING_ID_MARKER${question.id}" }
            )
        }
}

private fun importWarningSummaryText(warning: ImportWarning, questions: List<Question>): String {
    val question = questions.firstOrNull { warningBelongsToQuestion(warning, it) }
    val prefix = if (question != null) {
        buildString {
            append("第 ")
            append(question.number.ifBlank { warning.questionNumber ?: "-" })
            append(" 题")
            append(" · ")
            append(typeLabel(question.type))
            val category = question.category.trim()
            if (category.isNotBlank()) {
                append(" · ")
                append(category)
            }
        }
    } else if (warning.questionNumber.isNullOrBlank()) {
        "全局提示"
    } else {
        "第 ${warning.questionNumber} 题"
    }
    return "$prefix：${displayImportWarningMessage(warning.message)}"
}

private fun mergeAiWarnings(
    currentWarnings: List<ImportWarning>,
    aiWarnings: List<ImportWarning>,
    processedQuestions: List<Question>
): List<ImportWarning> {
    val processedIds = processedQuestions.map { it.id }.toSet()
    val processedNumbers = processedQuestions.map { normalizeQuestionNumber(it.number) }.toSet()
    val keptWarnings = currentWarnings.filterNot { warning ->
        if (!isAiImportWarning(warning)) return@filterNot false
        val warningQuestionId = aiWarningQuestionId(warning)
        if (warningQuestionId != null) warningQuestionId in processedIds
        else normalizeQuestionNumber(warning.questionNumber.orEmpty()) in processedNumbers
    }
    return dedupeImportWarnings(keptWarnings + aiWarnings)
}

private fun dedupeImportWarnings(warnings: List<ImportWarning>): List<ImportWarning> {
    val seen = mutableSetOf<String>()
    return warnings.filter { warning ->
        val key = listOf(
            warning.level.name,
            normalizeQuestionNumber(warning.questionNumber.orEmpty()),
            importWarningQuestionId(warning) ?: aiWarningQuestionId(warning).orEmpty(),
            normalizeImportWarningForDedupe(warning.message)
        ).joinToString("|")
        seen.add(key)
    }
}

private fun refreshImportWarningsForQuestions(
    currentWarnings: List<ImportWarning>,
    questions: List<Question>
): List<ImportWarning> {
    val preservedWarnings = currentWarnings.filterNot(::isReplaceableLocalImportWarning)
    return dedupeImportWarnings(
        preservedWarnings + validateQuestionsWithIdMarkers(questions) + duplicateQuestionNumberWarnings(questions)
    )
}

private fun validateQuestionsWithIdMarkers(questions: List<Question>): List<ImportWarning> {
    return questions.flatMap { question ->
        ImportValidator.validate(listOf(question)).map { warning ->
            warning.copy(message = warning.message.withImportWarningQuestionId(question.id))
        }
    }
}

private fun String.withImportWarningQuestionId(questionId: String): String {
    return "${displayImportWarningMessage(this)}；$IMPORT_WARNING_ID_MARKER$questionId"
}

private fun isReplaceableLocalImportWarning(warning: ImportWarning): Boolean {
    val message = displayImportWarningMessage(warning.message)
    return message in replaceableLocalImportWarningMessages ||
        message.startsWith("同一分区/题型内题号重复") ||
        isMultiBlankLocalWarning(message)
}

private fun isMultiBlankLocalWarning(message: String): Boolean {
    return (message.contains("个题空") && (
        message.contains("未识别逐空答案") ||
            message.contains("答案数量无法对应") ||
            message.contains("当前配置了")
        )) || message == "多空填空题存在未配置答案的题空"
}

private val replaceableLocalImportWarningMessages = setOf(
    "题干为空",
    "单选题缺少足够选项",
    "单选题未识别到答案",
    "单选题出现多个答案",
    "多选题缺少足够选项",
    "多选题未识别到答案",
    "答案选项不在当前题目选项范围内",
    "判断题缺少对/错选项，已尝试自动补全",
    "判断题未识别到答案",
    "判断题答案不是标准对/错标记",
    "主观题未识别到参考答案"
)

private fun normalizeImportWarningForDedupe(message: String): String {
    return displayImportWarningMessage(message)
        .replace(Regex("\\s+"), "")
        .trim('；', ';', '。', ' ', '\n', '\t')
}

private fun isAiImportWarning(warning: ImportWarning): Boolean {
    return warning.message.startsWith("AI建议") || warning.message.startsWith("AI 建议")
}

private fun aiWarningQuestionId(warning: ImportWarning): String? {
    return markerValue(warning.message, AI_WARNING_ID_MARKER)
}

private fun importWarningQuestionId(warning: ImportWarning): String? {
    return markerValue(warning.message, IMPORT_WARNING_ID_MARKER)
}

private fun markerValue(message: String, marker: String): String? {
    val markerIndex = message.indexOf(marker)
    if (markerIndex < 0) return null
    return message.substring(markerIndex + marker.length)
        .substringBefore(' ')
        .substringBefore('；')
        .substringBefore(';')
        .trim()
        .takeIf { it.isNotBlank() }
}

private fun warningBelongsToQuestion(warning: ImportWarning, question: Question): Boolean {
    val warningQuestionId = importWarningQuestionId(warning) ?: aiWarningQuestionId(warning)
    if (warningQuestionId != null) return warningQuestionId == question.id
    return normalizeQuestionNumber(warning.questionNumber.orEmpty()) == normalizeQuestionNumber(question.number)
}

private fun aiWarningBelongsToQuestion(warning: ImportWarning, question: Question): Boolean {
    return warningBelongsToQuestion(warning, question)
}

private fun displayImportWarningMessage(message: String): String {
    val markerIndex = listOf(
        message.indexOf(AI_WARNING_ID_MARKER),
        message.indexOf(IMPORT_WARNING_ID_MARKER)
    ).filter { it >= 0 }.minOrNull() ?: return message
    return message.substring(0, markerIndex).trimEnd('；', ';', ' ', '\n', '\t')
}

private fun canApplyAiSuggestion(suggestion: AiReviewSuggestion): Boolean {
    return suggestion.canApply && !suggestion.riskLevel.equals("hard_error", ignoreCase = true)
}

private fun isActionableAiSuggestion(suggestion: AiReviewSuggestion): Boolean {
    val hasStructuredSuggestion = suggestion.suggestedType != null ||
        suggestion.suggestedAnswer.isNotEmpty() ||
        suggestion.suggestedQuestion != null ||
        suggestion.suggestedOptions.isNotEmpty() ||
        suggestion.suggestedAnalysis != null
    val pureOk = suggestion.status.equals("ok", ignoreCase = true) &&
        suggestion.issueTypes.isEmpty() &&
        !suggestion.needHumanReview &&
        !canApplyAiSuggestion(suggestion) &&
        !hasStructuredSuggestion
    return !pureOk && (
        !suggestion.status.equals("ok", ignoreCase = true) ||
            suggestion.needHumanReview ||
            canApplyAiSuggestion(suggestion) ||
            suggestion.issueTypes.isNotEmpty() ||
            hasStructuredSuggestion
        )
}

private fun mergeAiReviewSuggestions(
    current: List<AiReviewSuggestion>,
    incoming: List<AiReviewSuggestion>,
    processedQuestions: List<Question>
): List<AiReviewSuggestion> {
    val processedIds = processedQuestions.map { it.id }.toSet()
    val kept = current.filterNot { it.questionId in processedIds }
    return kept + incoming.filter(::isActionableAiSuggestion)
}

private fun aiSuggestionApplySummary(suggestion: AiReviewSuggestion): String {
    val parts = mutableListOf<String>()
    suggestion.suggestedType?.let { parts += "题型→${suggestedTypeLabel(it)}" }
    if (suggestion.suggestedAnswer.isNotEmpty()) parts += "答案→${suggestion.suggestedAnswer.joinToString("")}"
    if (suggestion.suggestedQuestion != null) parts += "题干"
    if (suggestion.suggestedOptions.isNotEmpty()) parts += "选项 ${suggestion.suggestedOptions.size} 项"
    if (suggestion.suggestedAnalysis != null) parts += "解析"
    return parts.joinToString("、")
}

private fun suggestedTypeLabel(type: String): String = when (type.lowercase()) {
    "single" -> "单选题"
    "multiple" -> "多选题"
    "judge" -> "判断题"
    "blank" -> "填空题"
    "short" -> "简答题"
    else -> type
}

private fun applyAiReviewSuggestion(question: Question, suggestion: AiReviewSuggestion): Question {
    var next = question
    suggestion.suggestedType?.let { typeText ->
        suggestedQuestionType(typeText)?.let { targetType ->
            next = normalizeAfterTypeChange(next, targetType)
        }
    }
    suggestion.suggestedQuestion?.let { suggestedQuestion ->
        next = next.copy(question = suggestedQuestion)
    }
    if (suggestion.suggestedOptions.isNotEmpty()) {
        next = next.copy(options = suggestion.suggestedOptions)
    } else if (next.type == QuestionType.JUDGE && next.options.isEmpty()) {
        next = next.copy(options = defaultJudgeOptions())
    }
    if (suggestion.suggestedAnswer.isNotEmpty()) {
        next = next.copy(
            answer = normalizeSuggestedAnswer(suggestion.suggestedAnswer, next.type),
            blankAnswers = emptyList()
        )
    }
    suggestion.suggestedAnalysis?.let { suggestedAnalysis ->
        next = next.copy(analysis = suggestedAnalysis)
    }
    return next
}

private fun suggestedQuestionType(type: String): QuestionType? = when (type.trim().lowercase()) {
    "single", "单选", "单选题" -> QuestionType.SINGLE
    "multiple", "multi", "多选", "多选题" -> QuestionType.MULTIPLE
    "judge", "true_false", "判断", "判断题" -> QuestionType.JUDGE
    "blank", "填空", "填空题" -> QuestionType.BLANK
    "short", "essay", "简答", "简答题" -> QuestionType.SHORT
    else -> null
}

private fun normalizeSuggestedAnswer(answer: List<String>, type: QuestionType): List<String> {
    val normalized = answer
        .flatMap { item -> item.split(',', '，', '、', '/', ' ') }
        .map { it.trim().uppercase() }
        .filter { it.isNotBlank() }
    return when (type) {
        QuestionType.SINGLE -> normalized.take(1)
        QuestionType.JUDGE -> normalized.map { value ->
            when (value) {
                "正确", "对", "TRUE", "T" -> "A"
                "错误", "错", "FALSE", "F" -> "B"
                else -> value.take(1)
            }
        }.take(1)
        QuestionType.MULTIPLE -> normalized.distinct().sorted()
        else -> normalized
    }
}

private fun analysisTargetQuestions(
    questions: List<Question>,
    anomalyQuestions: List<Question>,
    onlyAnomaly: Boolean
): List<Question> {
    val source = if (onlyAnomaly) anomalyQuestions else questions
    return source.filter(::shouldApplyAiAnalysis)
}

private fun shouldApplyAiAnalysis(question: Question): Boolean {
    val clean = question.analysis.trim()
    val missingOrShortAnalysis = clean.isBlank() || clean.length < 20 || clean == "无" || clean == "暂无解析"
    val subjectiveMissingAnswer = question.type == QuestionType.SHORT && question.answer.isEmpty() && question.options.isEmpty()
    return missingOrShortAnalysis || subjectiveMissingAnswer
}

private fun applyAiAnalysisSuggestion(question: Question, generatedAnalysis: String): Question {
    val clean = generatedAnalysis.trim()
    if (clean.isBlank()) return question
    return if (question.type == QuestionType.SHORT && question.answer.isEmpty() && question.options.isEmpty()) {
        question.copy(
            answer = listOf(clean),
            analysis = clean
        )
    } else {
        question.copy(analysis = clean)
    }
}



private fun duplicateQuestionNumberWarnings(questions: List<Question>): List<ImportWarning> {
    val duplicateGroups = questions
        .filter { it.number.trim().isNotBlank() }
        .groupBy { duplicateQuestionNumberScopeKey(it) }
        .filterValues { it.size > 1 }
    return duplicateGroups.values.flatten().map { question ->
        ImportWarning(
            level = WarningLevel.WARNING,
            questionNumber = question.number,
            message = "同一分区/题型内题号重复：建议人工确认。导入预览已按内部题目ID区分，避免仅按题号混淆。；$IMPORT_WARNING_ID_MARKER${question.id}"
        )
    }
}

private fun duplicateQuestionNumberScopeKey(question: Question): String {
    return listOf(
        normalizeQuestionCategoryScope(question.category),
        question.type.name,
        normalizeQuestionNumber(question.number)
    ).joinToString("|")
}

private fun normalizeQuestionCategoryScope(category: String): String {
    return category.trim().replace(Regex("""\s+"""), " ")
}

private fun normalizeQuestionNumber(number: String): String {
    val clean = number.trim()
    val trimmedZero = clean.trimStart('0')
    return trimmedZero.ifBlank { clean }
}

private enum class ReviewFilter {
    ALL,
    ANOMALY,
    NO_ANSWER,
    MISSING_ANALYSIS,
    IMAGE,
    HARD_ERROR,
    AI_REVIEWED,
    AI_SUGGESTION,
    AI_APPLICABLE,
    AI_NEED_REVIEW,
    AI_HARD_ERROR,
    AI_ANALYZED
}

private fun reviewFilterFromName(name: String): ReviewFilter {
    return runCatching { ReviewFilter.valueOf(name) }.getOrDefault(ReviewFilter.ALL)
}

private fun reviewFilterLabel(filter: ReviewFilter): String = when (filter) {
    ReviewFilter.ALL -> "全部"
    ReviewFilter.ANOMALY -> "仅异常"
    ReviewFilter.NO_ANSWER -> "仅无答案"
    ReviewFilter.MISSING_ANALYSIS -> "缺/短解析"
    ReviewFilter.IMAGE -> "仅图片题"
    ReviewFilter.HARD_ERROR -> "仅硬错误"
    ReviewFilter.AI_REVIEWED -> "AI已核对"
    ReviewFilter.AI_SUGGESTION -> "仅AI建议"
    ReviewFilter.AI_APPLICABLE -> "仅可采纳"
    ReviewFilter.AI_NEED_REVIEW -> "仅需确认"
    ReviewFilter.AI_HARD_ERROR -> "AI硬错误"
    ReviewFilter.AI_ANALYZED -> "AI已补解析"
}

private fun reviewFilterCount(
    questions: List<Question>,
    warnings: List<ImportWarning>,
    aiSuggestions: List<AiReviewSuggestion> = emptyList(),
    aiReviewedQuestionIds: List<String> = emptyList(),
    aiAnalyzedQuestionIds: List<String> = emptyList(),
    aiAnalysisAppliedQuestionIds: List<String> = emptyList(),
    filter: ReviewFilter
): Int {
    if (filter == ReviewFilter.ALL) return questions.size
    return questions.indices.count { index ->
        val question = questions[index]
        questionMatchesFilter(
            question = question,
            warnings = warningsForQuestion(question, warnings),
            filter = filter,
            aiSuggestions = aiSuggestionsForQuestion(question, aiSuggestions),
            aiReviewedQuestionIds = aiReviewedQuestionIds,
            aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
            aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds
        )
    }
}

private fun firstMatchingQuestionIndex(
    questions: List<Question>,
    warnings: List<ImportWarning>,
    aiSuggestions: List<AiReviewSuggestion> = emptyList(),
    aiReviewedQuestionIds: List<String> = emptyList(),
    aiAnalyzedQuestionIds: List<String> = emptyList(),
    aiAnalysisAppliedQuestionIds: List<String> = emptyList(),
    filter: ReviewFilter
): Int? {
    if (filter == ReviewFilter.ALL) return questions.indices.firstOrNull()
    return questions.indices.firstOrNull { index ->
        val question = questions[index]
        questionMatchesFilter(
            question = question,
            warnings = warningsForQuestion(question, warnings),
            filter = filter,
            aiSuggestions = aiSuggestionsForQuestion(question, aiSuggestions),
            aiReviewedQuestionIds = aiReviewedQuestionIds,
            aiAnalyzedQuestionIds = aiAnalyzedQuestionIds,
            aiAnalysisAppliedQuestionIds = aiAnalysisAppliedQuestionIds
        )
    }
}

private fun warningsForQuestion(question: Question, warnings: List<ImportWarning>): List<ImportWarning> {
    return dedupeImportWarnings(
        warnings.filter { warning ->
            warningBelongsToQuestion(warning, question)
        }
    )
}

private fun questionMatchesFilter(
    question: Question,
    warnings: List<ImportWarning>,
    filter: ReviewFilter,
    aiSuggestions: List<AiReviewSuggestion> = emptyList(),
    aiReviewedQuestionIds: List<String> = emptyList(),
    aiAnalyzedQuestionIds: List<String> = emptyList(),
    aiAnalysisAppliedQuestionIds: List<String> = emptyList()
): Boolean {
    return when (filter) {
        ReviewFilter.ALL -> true
        ReviewFilter.ANOMALY -> hasReviewAnomaly(question, warnings)
        ReviewFilter.NO_ANSWER -> question.answer.isEmpty()
        ReviewFilter.MISSING_ANALYSIS -> shouldApplyAiAnalysis(question)
        ReviewFilter.IMAGE -> question.images.isNotEmpty()
        ReviewFilter.HARD_ERROR -> hasHardReviewError(question, warnings)
        ReviewFilter.AI_REVIEWED -> question.id in aiReviewedQuestionIds.toSet()
        ReviewFilter.AI_SUGGESTION -> aiSuggestions.any(::isActionableAiSuggestion)
        ReviewFilter.AI_APPLICABLE -> aiSuggestions.any(::canApplyAiSuggestion)
        ReviewFilter.AI_NEED_REVIEW -> aiSuggestions.any(::isNeedHumanReviewAiSuggestion)
        ReviewFilter.AI_HARD_ERROR -> aiSuggestions.any(::isHardErrorAiSuggestion)
        ReviewFilter.AI_ANALYZED -> question.id in aiAnalysisAppliedQuestionIds.toSet()
    }
}

private fun hasReviewAnomaly(question: Question, warnings: List<ImportWarning>): Boolean {
    return hasSoftReviewWarning(question, warnings) || hasHardReviewError(question, warnings)
}

private fun hasSoftReviewWarning(question: Question, warnings: List<ImportWarning>): Boolean {
    val hasWarning = warnings.any { it.level == WarningLevel.WARNING }
    val choiceType = question.type in listOf(QuestionType.SINGLE, QuestionType.MULTIPLE)
    val optionCountWarning = choiceType && question.options.size == 1
    return hasWarning || question.answer.isEmpty() || optionCountWarning
}

private fun hasHardReviewError(question: Question, warnings: List<ImportWarning>): Boolean {
    if (warnings.any { it.level == WarningLevel.ERROR }) return true
    if (question.question.isBlank()) return true
    val choiceType = question.type in listOf(QuestionType.SINGLE, QuestionType.MULTIPLE)
    if (choiceType && question.options.isEmpty()) return true
    if (question.type == QuestionType.SINGLE && question.answer.size > 1) return true
    if (choiceType && question.answer.isNotEmpty()) {
        val optionKeys = question.options.map { it.key.uppercase() }.toSet()
        if (optionKeys.isNotEmpty() && question.answer.any { it.uppercase() !in optionKeys }) return true
    }
    if (question.type == QuestionType.JUDGE && question.answer.isNotEmpty()) {
        val allowed = setOf("A", "B", "正确", "错误", "对", "错")
        if (question.answer.any { it.uppercase() !in allowed }) return true
    }
    return false
}

private fun previousIndexInList(indices: List<Int>, currentIndex: Int): Int? {
    if (indices.isEmpty()) return null
    return indices.lastOrNull { it < currentIndex } ?: indices.lastOrNull()
}

private fun nextIndexInList(indices: List<Int>, currentIndex: Int): Int? {
    if (indices.isEmpty()) return null
    return indices.firstOrNull { it > currentIndex } ?: indices.firstOrNull()
}

@Composable
private fun ReviewFilteredJumpList(
    questions: List<Question>,
    indices: List<Int>,
    currentIndex: Int,
    warnings: List<ImportWarning>,
    onIndexChange: (Int) -> Unit,
    onEditQuestion: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Text(
            text = "当前筛选列表",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        val pageSize = 5
        val currentPosition = indices.indexOf(currentIndex).takeIf { it >= 0 } ?: 0
        val pageStart = (currentPosition / pageSize) * pageSize
        val pageEnd = (pageStart + pageSize).coerceAtMost(indices.size)
        val pageIndices = indices.subList(pageStart, pageEnd)

        pageIndices.forEach { index ->
            val question = questions[index]
            val warningCount = warningsForQuestion(question, warnings).count { it.level != WarningLevel.NORMAL }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShirohaRadius.Md),
                color = if (index == currentIndex) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardWhite78,
                border = BorderStroke(
                    1.dp,
                    if (index == currentIndex) ShirohaColors.LineSelected else ShirohaColors.LineStrong
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .shirohaNoRippleClickable { onIndexChange(index) }
                    ) {
                        Text(
                            text = "第 ${index + 1} 题 · ${typeLabel(question.type)} · 答案：${answerDisplayText(question)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = question.question.ifBlank { "题干为空" }.take(70),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (warningCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "提示 $warningCount 条",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    ReviewCompactButton(
                        icon = Icons.Rounded.Edit,
                        text = "编辑",
                        onClick = { onEditQuestion(index) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (indices.size > pageSize) {
            NoticeCard(
                "当前筛选共有 ${indices.size} 题，正在显示第 ${pageStart + 1}-${pageEnd} 条；可用“上一条 / 下一条”继续核对。",
                warning = false
            )
        }
    }
}

private fun normalizeAfterTypeChange(question: Question, type: QuestionType): Question {
    return when (type) {
        QuestionType.JUDGE -> question.copy(
            type = type,
            options = if (question.options.isEmpty()) defaultJudgeOptions() else question.options,
            answer = normalizeJudgeAnswer(question.answer),
            blankAnswers = emptyList()
        )
        QuestionType.SINGLE,
        QuestionType.MULTIPLE -> question.copy(type = type, blankAnswers = emptyList())
        QuestionType.BLANK -> question.copy(type = type)
        QuestionType.SHORT -> question.copy(type = type, blankAnswers = emptyList())
    }
}

private fun normalizeJudgeAnswer(answer: List<String>): List<String> {
    if (answer.isEmpty()) return emptyList()
    return answer.mapNotNull { value ->
        when (value.trim().uppercase()) {
            "A", "正确", "对", "是", "TRUE", "T", "√", "✓", "✔", "✅", "☑" -> "A"
            "B", "错误", "错", "否", "FALSE", "F", "×", "X", "✘", "✖", "❌", "❎" -> "B"
            else -> value.trim().takeIf { it.isNotBlank() }
        }
    }
}

private fun defaultJudgeOptions(): List<Option> = listOf(
    Option("A", "正确"),
    Option("B", "错误")
)

private fun nextOptionKey(options: List<Option>): String {
    val used = options.map { it.key.uppercase() }.toSet()
    return ('A'..'H').firstOrNull { it.toString() !in used }?.toString() ?: "${options.size + 1}"
}

private fun parseReviewAnswer(text: String, type: QuestionType): List<String> {
    val clean = text.trim()
    if (clean.isBlank()) return emptyList()

    if (type == QuestionType.BLANK || type == QuestionType.SHORT) return listOf(clean)

    if (type == QuestionType.JUDGE) {
        normalizeJudgeAnswer(listOf(clean)).takeIf { it.isNotEmpty() }?.let { return it }
    }

    val compactLetters = clean.uppercase().replace(Regex("[\\s,，、/／;；]+"), "")
    if (compactLetters.matches(Regex("^[A-H]{1,8}$"))) {
        return compactLetters.map { it.toString() }.distinct()
    }

    return clean
        .replace("，", ",")
        .replace("、", ",")
        .replace("/", ",")
        .replace("／", ",")
        .replace("；", ",")
        .replace(";", ",")
        .split(Regex("[\\s,]+"))
        .map { token -> token.trim() }
        .filter { it.isNotBlank() }
        .flatMap { token ->
            if (token.uppercase().matches(Regex("^[A-H]{2,8}$"))) {
                token.uppercase().map { it.toString() }
            } else {
                normalizeJudgeAnswer(listOf(token)).ifEmpty { listOf(token.uppercase()) }
            }
        }
        .distinct()
}

private fun answerInputText(question: Question): String {
    if (question.type == QuestionType.JUDGE && question.answer.size == 1) {
        return when (question.answer.first().trim().uppercase()) {
            "A", "正确", "对", "是", "TRUE", "T", "√", "✓", "✔", "✅", "☑" -> "正确"
            "B", "错误", "错", "否", "FALSE", "F", "×", "X", "✘", "✖", "❌", "❎" -> "错误"
            else -> question.answer.first()
        }
    }
    return question.answer.joinToString(",")
}

private fun answerDisplayText(question: Question): String {
    if (MultiBlankSupport.hasStructuredAnswers(question)) {
        return MultiBlankSupport.expectedAnswerText(question.blankAnswers)
    }
    val value = answerInputText(question)
    return value.ifBlank { "未识别答案" }
}

private fun typeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}

private fun queryFileName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex("_display_name")
        if (index >= 0 && it.moveToFirst()) {
            return it.getString(index) ?: "未命名文件"
        }
    }
    return uri.lastPathSegment ?: "未命名文件"
}

private data class ImportFileSizeCheck(
    val warnMessage: String? = null,
    val blockMessage: String? = null
)

private fun checkImportFileSize(context: Context, uri: Uri, fileName: String): ImportFileSizeCheck {
    val size = queryFileSize(context, uri) ?: return ImportFileSizeCheck()
    return when {
        size > IMPORT_FILE_BLOCK_BYTES -> ImportFileSizeCheck(
            blockMessage = "文件过大：$fileName（约 ${formatFileSize(size)}）。建议先压缩图片、拆分题库，或改用标准文本/Excel/JSON 导入。"
        )
        size > IMPORT_FILE_WARN_BYTES -> ImportFileSizeCheck(
            warnMessage = "文件较大：$fileName（约 ${formatFileSize(size)}），含大图或复杂 Word 内容时可能读取较慢。"
        )
        else -> ImportFileSizeCheck()
    }
}

private fun queryFileSize(context: Context, uri: Uri): Long? {
    val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.SIZE)
        if (index >= 0 && it.moveToFirst()) {
            val size = it.getLong(index)
            if (size > 0) return size
        }
    }
    return null
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes.toDouble() / 1024.0 / 1024.0
    return if (mb >= 10) {
        "${mb.roundToInt()} MB"
    } else {
        String.format(Locale.ROOT, "%.1f MB", mb)
    }
}

private fun readImportedContent(
    context: Context,
    uri: Uri,
    fileName: String
): QuestionImportAssetExtractor.DecodeResult {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return QuestionImportAssetExtractor.DecodeResult.Failure("文件无法读取，请确认文件仍可访问。")
    return QuestionImportAssetExtractor.decodeDetailed(context, bytes, fileName)
}

private fun readImportedText(context: Context, uri: Uri, fileName: String): TextImportDecoder.DecodeResult {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return TextImportDecoder.DecodeResult.Failure("文件无法读取，请确认文件仍可访问。")
    return TextImportDecoder.decodeDetailed(bytes, fileName)
}

private fun sampleImportText(): String = """
1. 安全帽的主要作用是（A）
A. 保护头部
B. 装饰作用
C. 增加重量
D. 无实际作用
答案：A
解析：安全帽用于减轻坠落物和碰撞对头部造成的伤害。

2. 雨天驾驶时应注意哪些事项（AB）
A. 降低车速
B. 加大跟车距离
C. 急打方向
D. 紧急制动
答案：AB
解析：雨天路滑，应平稳控制车辆并留足安全距离。

3. 国家安全生产方针是“安全第一，预防为主”。（对）
答案：对
解析：这是一道基础判断题，答案为正确。
""".trimIndent()

private fun sampleAnswerText(): String = """
1. A
2. AB
3. 对
""".trimIndent()

@Composable
private fun ImportStepHeroCard() {
    val density = LocalDensity.current
    val floatDistancePx = with(density) { ShirohaMotion.HeroFloatDistance.toPx() }
    val heroFloat = rememberInfiniteTransition(label = "import_illustration_float")
    val imageOffsetY by heroFloat.animateFloat(
        initialValue = -floatDistancePx,
        targetValue = floatDistancePx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ShirohaMotion.HeroFloatMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "import_illustration_float_y"
    )

    GlassCard(
        modifier = Modifier.height(ShirohaDimens.HeroCardHeight),
        contentPadding = ShirohaSpacing.Xl
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ImportStepPill(index = "1", text = "导入文件", selected = true)
                ImportStepPill(index = "2", text = "核对结果", selected = false)
                ImportStepPill(index = "3", text = "创建题库", selected = false)
            }
            if (QuizRepository.shirohaModeEnabled) {
                Box(
                    modifier = Modifier.size(ShirohaDimens.HeroImageFrameSize),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.illus_import_hint_webp),
                        contentDescription = null,
                        modifier = Modifier
                            .size(ShirohaDimens.HeroImageFrameSize)
                            .graphicsLayer { translationY = imageOffsetY }
                            .alpha(ShirohaDimens.HeroImageAlpha),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private enum class ImportSaveMode {
    NEW_BANK,
    APPEND_TO_BANK
}

private fun defaultImportBankName(fileName: String): String {
    return fileName
        .takeIf { it.isNotBlank() && it != "未选择文件" }
        ?.substringBeforeLast('.')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "导入题库"
}

@Composable
private fun ImportStepPill(
    index: String,
    text: String,
    selected: Boolean
) {
    Surface(
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (selected) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardMuted,
        border = BorderStroke(ShirohaDimens.Hairline, if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft),
        modifier = Modifier
            .width(ShirohaDimens.StepPillWidth)
            .defaultMinSize(minHeight = ShirohaDimens.StepPillMinHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ShirohaDimens.StepPillHorizontalPadding, vertical = ShirohaDimens.StepPillVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$index  $text",
                color = if (selected) MaterialTheme.colorScheme.primary else ShirohaColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
