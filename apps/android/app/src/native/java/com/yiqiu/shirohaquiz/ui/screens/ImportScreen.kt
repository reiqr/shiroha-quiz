package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ai.AiReviewSuggestion
import com.yiqiu.shirohaquiz.ai.ShirohaAiClient
import com.yiqiu.shirohaquiz.importer.model.ImportResult
import com.yiqiu.shirohaquiz.importer.model.ImportWarning
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.importer.assets.QuestionImageBinder
import com.yiqiu.shirohaquiz.importer.assets.QuestionImportAssetExtractor
import com.yiqiu.shirohaquiz.importer.model.WarningLevel
import com.yiqiu.shirohaquiz.importer.parser.QuizImportParser
import com.yiqiu.shirohaquiz.importer.parser.TextImportDecoder
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.LoadingIllustration
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaMotion
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var reviewFilterName by rememberSaveable { mutableStateOf(ReviewFilter.ALL.name) }
    var statusText by rememberSaveable {
        mutableStateOf("请选择题库文件。")
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

    fun clearParsedResult(clearImages: Boolean = false) {
        importResult = null
        editableQuestions = emptyList()
        reviewMode = false
        reviewIndex = 0
        reviewFilterName = ReviewFilter.ALL.name
        previewOnlyAnomaly = false
        aiReviewedQuestionIds = emptyList()
        aiAnalyzedQuestionIds = emptyList()
        if (clearImages) importedImages = emptyList()
    }

    fun applyParsedResult(result: ImportResult) {
        importResult = result
        editableQuestions = result.questions
        reviewIndex = 0
        reviewFilterName = ReviewFilter.ALL.name
        aiReviewedQuestionIds = emptyList()
        aiAnalyzedQuestionIds = emptyList()
        val hardCount = result.warnings.count { it.level == WarningLevel.ERROR }
        val softCount = result.warnings.count { it.level == WarningLevel.WARNING }
        statusText = "已完成${if (useDualImport) "双文件" else "原生"}解析：${result.questions.size} 题，硬错误 $hardCount 条，可确认提示 $softCount 条。"
        isStatusWarn = hardCount > 0
    }

    if (rawFullEditorMode) {
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
        isImportBusy = true
        busyText = "正在读取题库文件……"
        statusText = "正在读取：$selectedFileName"
        isStatusWarn = false
        clearParsedResult(clearImages = true)
        importScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    readImportedContent(context, uri, selectedFileName)
                }
            }
            isImportBusy = false
            val content = result.getOrNull()
            if (content == null || content.text.isBlank()) {
                statusText = "当前导入还不能稳定读取这个文件。建议优先使用 docx / txt / json。"
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
    }

    val answerFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || isImportBusy) return@rememberLauncherForActivityResult
        selectedAnswerFileName = queryFileName(context, uri)
        isImportBusy = true
        busyText = "正在读取答案文件……"
        statusText = "正在读取答案文件：$selectedAnswerFileName"
        isStatusWarn = false
        importScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    readImportedText(context, uri, selectedAnswerFileName)
                }
            }
            isImportBusy = false
            val text = result.getOrNull()
            if (text.isNullOrBlank()) {
                statusText = "答案文件暂时还不能稳定读取，请优先使用 txt 或可复制文本的文档。"
                isStatusWarn = true
            } else {
                answerText = text
                answerTextEditorExpanded = text.length <= LARGE_TEXT_PREVIEW_THRESHOLD
                clearParsedResult()
                statusText = "已读取答案文件：$selectedAnswerFileName。"
                isStatusWarn = false
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
                    val parsedResult = if (dualSnapshot) {
                        QuizImportParser.parseDualText(rawSnapshot, answerSnapshot)
                    } else {
                        QuizImportParser.parseStandardText(rawSnapshot)
                    }
                    if (!dualSnapshot && imagesSnapshot.isNotEmpty()) {
                        QuestionImageBinder.attach(parsedResult, imagesSnapshot)
                    } else {
                        parsedResult
                    }
                }
            }
            isImportBusy = false
            result.onSuccess { finalResult ->
                applyParsedResult(finalResult)
            }.onFailure { error ->
                statusText = "解析失败：${error.message ?: "请检查题库文件格式"}"
                isStatusWarn = true
            }
        }
    }

    if (reviewMode && importResult != null) {
        val warnings = importResult?.warnings.orEmpty()
        val reviewFilter = reviewFilterFromName(reviewFilterName)
        NativeQuestionReviewScreen(
            questions = editableQuestions,
            warnings = warnings,
            filter = reviewFilter,
            currentIndex = reviewIndex.coerceIn(0, (editableQuestions.size - 1).coerceAtLeast(0)),
            onFilterChange = { filter ->
                reviewFilterName = filter.name
                firstMatchingQuestionIndex(editableQuestions, warnings, filter)?.let { reviewIndex = it }
            },
            onIndexChange = { index ->
                if (editableQuestions.isNotEmpty()) {
                    reviewIndex = index.coerceIn(0, editableQuestions.lastIndex)
                }
            },
            onQuestionChange = { index, question ->
                editableQuestions = editableQuestions.mapIndexed { currentIndex, item ->
                    if (currentIndex == index) question else item
                }
            },
            onDeleteQuestion = { index ->
                val nextQuestions = editableQuestions.filterIndexed { currentIndex, _ -> currentIndex != index }
                editableQuestions = nextQuestions
                reviewIndex = index.coerceAtMost((nextQuestions.size - 1).coerceAtLeast(0))
                firstMatchingQuestionIndex(nextQuestions, warnings, reviewFilterFromName(reviewFilterName))?.let { reviewIndex = it }
                statusText = "已从核对列表中移除 1 题。保存题库时会使用当前核对后的题目。"
                isStatusWarn = false
            },
            onBack = { reviewMode = false }
        )
        return
    }

    val shouldPickAnswerFile = useDualImport && selectedFileName != "未选择文件"

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
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
                            text = "支持 docx / txt / json 等文本型题库。",
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
                        }
                    }
                )
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
                    else -> "手动粘贴"
                }
                val rawTextHint = when {
                    useDualImport -> "题库文件内容，可在解析前核对调整。"
                    hasRawText || hasSelectedFile -> "可在解析前核对或调整导入文本。"
                    else -> "也可以直接粘贴题库文本后解析。"
                }
                val rawTextActionButtonWidth = 128.dp
                val rawTextActionButtonHeight = ShirohaDimens.ActionButtonMinHeight
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                    else -> 104.dp
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
                NativeImportSummary(displayResult)

                val warnings = importResult?.warnings.orEmpty()
                val anomalyQuestions = editableQuestions.filter { question ->
                    questionMatchesFilter(question, warningsForQuestion(question, warnings), ReviewFilter.ANOMALY)
                }
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
                            text = "AI 核对",
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
                            text = "AI 解析",
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
                                val usingFallbackTargets = QuizRepository.aiOnlyAnomaly && anomalyAnalysisTargets.isEmpty() && allAnalysisTargets.isNotEmpty()
                                val analysisTargetPool = if (QuizRepository.aiOnlyAnomaly && anomalyAnalysisTargets.isNotEmpty()) {
                                    anomalyAnalysisTargets
                                } else {
                                    allAnalysisTargets
                                }
                                if (analysisTargetPool.isEmpty()) {
                                    statusText = "AI 解析：当前没有缺少解析或解析过短的题目。"
                                    isStatusWarn = false
                                    return@ActionPillButton
                                }
                                val analyzedIdSet = aiAnalyzedQuestionIds.toSet()
                                val remainingAnalysisTargets = analysisTargetPool.filterNot { it.id in analyzedIdSet }
                                if (remainingAnalysisTargets.isEmpty()) {
                                    statusText = "AI 解析：当前范围已全部尝试。若仍有题目缺解析，可能是模型未返回对应结果；可重新解析题库或调整单次题数后重试。"
                                    isStatusWarn = false
                                    return@ActionPillButton
                                }
                                val aiTargetQuestions = remainingAnalysisTargets.take(QuizRepository.aiMaxQuestions)
                                val processedBefore = analysisTargetPool.count { it.id in analyzedIdSet }.coerceAtMost(analysisTargetPool.size)
                                statusText = if (usingFallbackTargets) {
                                    "AI 解析中：当前没有异常题，已改为处理缺解析题；本次处理 ${aiTargetQuestions.size} 道，进度 ${processedBefore + 1}-${processedBefore + aiTargetQuestions.size}/${analysisTargetPool.size}。"
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
                                        val changedIds = suggestionMap.keys
                                        val nextAnalyzedIds = (aiAnalyzedQuestionIds + aiTargetQuestions.map { it.id }).distinct()
                                        editableQuestions = editableQuestions.map { question ->
                                            val suggestion = suggestionMap[question.id]
                                            if (suggestion != null && shouldApplyAiAnalysis(question)) {
                                                question.copy(analysis = suggestion.analysis)
                                            } else {
                                                question
                                            }
                                        }
                                        importResult = displayResult.copy(questions = editableQuestions)
                                        aiAnalyzedQuestionIds = nextAnalyzedIds
                                        val nextAnalyzedIdSet = nextAnalyzedIds.toSet()
                                        val skippedCount = (aiTargetQuestions.size - changedIds.size).coerceAtLeast(0)
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
                    ActionPillButton(
                        icon = Icons.Rounded.Save,
                        text = "保存为当前题库",
                        primary = true,
                        onClick = {
                            val bankName = selectedFileName.substringBeforeLast('.').ifBlank { "导入题库" }
                            QuizRepository.importBank(context, bankName, editableQuestions)
                            statusText = "已写入原生题库：$bankName，共 ${editableQuestions.size} 题。现在可以切到首页、练习或考试查看。"
                            isStatusWarn = false
                            onImportSaved()
                        }
                    )
                }

                NativeImportPreview(
                    questions = previewQuestions,
                    totalQuestionCount = editableQuestions.size,
                    onlyShowAnomaly = previewOnlyAnomaly
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
            text = { Text("请先在 个人偏好 → AI 设置 中配置接口。") },
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
private const val LARGE_TEXT_PREVIEW_CHARS = 1200

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Edit",
            title = title,
            subtitle = ""
        )
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
                        icon = Icons.Rounded.CheckCircle,
                        text = "完成",
                        primary = true,
                        modifier = Modifier
                            .width(128.dp)
                            .height(ShirohaDimens.ActionButtonMinHeight),
                        fillWidthContent = true,
                        onClick = onBack
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
            .clickable(onClick = onClick),
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
            .clickable(onClick = onClick),
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
private fun NativeImportSummary(result: ImportResult) {
    val hardCount = result.warnings.count { it.level == WarningLevel.ERROR }
    val softCount = result.warnings.count { it.level == WarningLevel.WARNING }
    val answeredCount = result.questions.count { it.answer.isNotEmpty() }
    val imageQuestionCount = result.questions.count { it.images.isNotEmpty() }

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
        }
        if (result.warnings.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            result.warnings.take(6).forEach { warning ->
                NoticeCard("第 ${warning.questionNumber ?: "-"} 题：${warning.message}")
                Spacer(Modifier.height(8.dp))
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
    onlyShowAnomaly: Boolean
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NativeQuestionReviewScreen(
    questions: List<Question>,
    warnings: List<ImportWarning>,
    filter: ReviewFilter,
    currentIndex: Int,
    onFilterChange: (ReviewFilter) -> Unit,
    onIndexChange: (Int) -> Unit,
    onQuestionChange: (Int, Question) -> Unit,
    onDeleteQuestion: (Int) -> Unit,
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

    val allIndices = questions.indices.toList()
    val filteredIndices = questions.indices.filter { index ->
        questionMatchesFilter(questions[index], warningsForQuestion(questions[index], warnings), filter)
    }
    val visibleIndices = if (filter == ReviewFilter.ALL) allIndices else filteredIndices
    val safeIndex = when {
        questions.isEmpty() -> 0
        filter == ReviewFilter.ALL -> currentIndex.coerceIn(0, questions.lastIndex)
        currentIndex in visibleIndices -> currentIndex
        visibleIndices.isNotEmpty() -> visibleIndices.first()
        else -> currentIndex.coerceIn(0, questions.lastIndex)
    }
    val question = questions[safeIndex]
    val questionWarnings = warningsForQuestion(question, warnings)
    val visiblePosition = visibleIndices.indexOf(safeIndex).takeIf { it >= 0 } ?: 0
    val anomalyIndices = questions.indices.filter { index ->
        questionMatchesFilter(questions[index], warningsForQuestion(questions[index], warnings), ReviewFilter.ANOMALY)
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
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
                    ReviewTypeChip(
                        text = "${reviewFilterLabel(item)} ${reviewFilterCount(questions, warnings, item)}",
                        selected = filter == item,
                        onClick = { onFilterChange(item) }
                    )
                }
            }
            if (filter != ReviewFilter.ALL && visibleIndices.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                NoticeCard("当前筛选下没有需要核对的题目，可以切换到“全部”继续浏览。", warning = false)
            }
        }

        if (filter != ReviewFilter.ALL && visibleIndices.isEmpty()) {
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
                            if (filter != ReviewFilter.ALL) {
                                append(" · ${reviewFilterLabel(filter)} ${visiblePosition + 1}/${visibleIndices.size}")
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
                    icon = Icons.Rounded.ArrowBack,
                    text = "返回",
                    modifier = Modifier.weight(1f),
                    onClick = onBack
                )
                ReviewCompactButton(
                    icon = Icons.Rounded.ArrowBack,
                    text = if (filter == ReviewFilter.ALL) "上一题" else "上一条",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val target = previousIndexInList(visibleIndices, safeIndex) ?: (safeIndex - 1)
                        onIndexChange(target)
                    }
                )
                ReviewCompactButton(
                    icon = Icons.Rounded.ArrowForward,
                    text = if (filter == ReviewFilter.ALL) "下一题" else "下一条",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val target = nextIndexInList(visibleIndices, safeIndex) ?: (safeIndex + 1)
                        onIndexChange(target)
                    }
                )
            }
            if (anomalyIndices.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReviewCompactButton(
                        icon = Icons.Rounded.ArrowBack,
                        text = "上一异常",
                        modifier = Modifier.weight(1f),
                        onClick = { previousIndexInList(anomalyIndices, safeIndex)?.let(onIndexChange) }
                    )
                    ReviewCompactButton(
                        icon = Icons.Rounded.ArrowForward,
                        text = "下一异常",
                        modifier = Modifier.weight(1f),
                        onClick = { nextIndexInList(anomalyIndices, safeIndex)?.let(onIndexChange) }
                    )
                }
            }
        }

        if (filter != ReviewFilter.ALL && visibleIndices.size > 1) {
            ReviewFilteredJumpList(
                questions = questions,
                indices = visibleIndices,
                currentIndex = safeIndex,
                warnings = warnings,
                onIndexChange = onIndexChange
            )
        }

        if (questionWarnings.isNotEmpty()) {
            GlassCard {
                Text(
                    text = "本题提示",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                questionWarnings.forEach { warning ->
                    NoticeCard(warning.message, warning = warning.level != WarningLevel.NORMAL)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

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
                        onQuestionChange(safeIndex, question.copy(number = value))
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("题号") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = question.category,
                    onValueChange = { value ->
                        onQuestionChange(safeIndex, question.copy(category = value))
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
                            onQuestionChange(safeIndex, normalizeAfterTypeChange(question, type))
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = question.question,
                onValueChange = { value ->
                    onQuestionChange(safeIndex, question.copy(question = value))
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
                    onClick = { onQuestionChange(safeIndex, question.copy(images = emptyList())) }
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
                            onQuestionChange(safeIndex, question.copy(options = updated))
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
                            onQuestionChange(safeIndex, question.copy(options = updated))
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
                        onQuestionChange(safeIndex, question.copy(options = question.options + Option(key, "")))
                    }
                )
                ActionPillButton(
                    icon = Icons.Rounded.RemoveCircle,
                    text = "删除最后选项",
                    primary = false,
                    onClick = {
                        if (question.options.isNotEmpty()) {
                            onQuestionChange(safeIndex, question.copy(options = question.options.dropLast(1)))
                        }
                    }
                )
                ActionPillButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "补齐判断选项",
                    primary = false,
                    onClick = {
                        onQuestionChange(
                            safeIndex,
                            question.copy(
                                type = QuestionType.JUDGE,
                                options = defaultJudgeOptions(),
                                answer = if (question.answer.isEmpty()) listOf("A") else question.answer
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
            OutlinedTextField(
                value = answerInputText(question),
                onValueChange = { value ->
                    onQuestionChange(safeIndex, question.copy(answer = parseReviewAnswer(value, question.type)))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("答案：单选填 A，多选填 ABC 或 A,B,C，判断填 正确/错误") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = question.analysis,
                onValueChange = { value ->
                    onQuestionChange(safeIndex, question.copy(analysis = value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                minLines = 5,
                label = { Text("解析") },
                textStyle = MaterialTheme.typography.bodyMedium
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
                text = "如果解析器把说明文字、页眉页脚或废片段识别成题目，可以直接删除本题。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            ActionPillButton(
                icon = Icons.Rounded.Delete,
                text = "删除本题",
                primary = false,
                onClick = { onDeleteQuestion(safeIndex) }
            )
        }
    }
}


private fun shouldShowAiStatusInImport(text: String): Boolean {
    return text.startsWith("AI ") ||
        text.startsWith("AI核对") ||
        text.startsWith("AI解析") ||
        text.contains("AI 核对") ||
        text.contains("AI 解析")
}

private fun suggestionsToImportWarnings(
    suggestions: List<AiReviewSuggestion>,
    questions: List<Question>
): List<ImportWarning> {
    val questionById = questions.associateBy { it.id }
    return suggestions
        .filter { suggestion -> suggestion.status.lowercase() != "ok" || suggestion.needHumanReview }
        .mapNotNull { suggestion ->
            val question = questionById[suggestion.questionId] ?: return@mapNotNull null
            val issueText = suggestion.issueTypes.takeIf { it.isNotEmpty() }?.joinToString("、").orEmpty()
            val message = buildString {
                append("AI建议")
                if (issueText.isNotBlank()) append("[$issueText]")
                if (suggestion.reason.isNotBlank()) append("：").append(suggestion.reason)
                if (suggestion.suggestion.isNotBlank()) append("；建议：").append(suggestion.suggestion)
            }
            ImportWarning(
                level = if (suggestion.status.lowercase() == "error") WarningLevel.ERROR else WarningLevel.WARNING,
                questionNumber = question.number,
                message = message.ifBlank { "AI 建议人工确认本题。" }
            )
        }
}

private fun mergeAiWarnings(
    currentWarnings: List<ImportWarning>,
    aiWarnings: List<ImportWarning>,
    processedQuestions: List<Question>
): List<ImportWarning> {
    val processedNumbers = processedQuestions.map { it.number }.toSet()
    val keptWarnings = currentWarnings.filterNot { warning ->
        warning.questionNumber in processedNumbers && isAiImportWarning(warning)
    }
    return keptWarnings + aiWarnings
}

private fun isAiImportWarning(warning: ImportWarning): Boolean {
    return warning.message.startsWith("AI建议") || warning.message.startsWith("AI 建议")
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
    return clean.isBlank() || clean.length < 20 || clean == "无" || clean == "暂无解析"
}


private enum class ReviewFilter {
    ALL,
    ANOMALY,
    NO_ANSWER,
    IMAGE,
    HARD_ERROR
}

private fun reviewFilterFromName(name: String): ReviewFilter {
    return runCatching { ReviewFilter.valueOf(name) }.getOrDefault(ReviewFilter.ALL)
}

private fun reviewFilterLabel(filter: ReviewFilter): String = when (filter) {
    ReviewFilter.ALL -> "全部"
    ReviewFilter.ANOMALY -> "仅异常"
    ReviewFilter.NO_ANSWER -> "仅无答案"
    ReviewFilter.IMAGE -> "仅图片题"
    ReviewFilter.HARD_ERROR -> "仅硬错误"
}

private fun reviewFilterCount(
    questions: List<Question>,
    warnings: List<ImportWarning>,
    filter: ReviewFilter
): Int {
    if (filter == ReviewFilter.ALL) return questions.size
    return questions.indices.count { index ->
        questionMatchesFilter(questions[index], warningsForQuestion(questions[index], warnings), filter)
    }
}

private fun firstMatchingQuestionIndex(
    questions: List<Question>,
    warnings: List<ImportWarning>,
    filter: ReviewFilter
): Int? {
    if (filter == ReviewFilter.ALL) return questions.indices.firstOrNull()
    return questions.indices.firstOrNull { index ->
        questionMatchesFilter(questions[index], warningsForQuestion(questions[index], warnings), filter)
    }
}

private fun warningsForQuestion(question: Question, warnings: List<ImportWarning>): List<ImportWarning> {
    return warnings.filter { warning ->
        warning.questionNumber == question.number || warning.questionNumber == question.number.trimStart('0')
    }
}

private fun questionMatchesFilter(
    question: Question,
    warnings: List<ImportWarning>,
    filter: ReviewFilter
): Boolean {
    return when (filter) {
        ReviewFilter.ALL -> true
        ReviewFilter.ANOMALY -> hasReviewAnomaly(question, warnings)
        ReviewFilter.NO_ANSWER -> question.answer.isEmpty()
        ReviewFilter.IMAGE -> question.images.isNotEmpty()
        ReviewFilter.HARD_ERROR -> hasHardReviewError(question, warnings)
    }
}

private fun hasReviewAnomaly(question: Question, warnings: List<ImportWarning>): Boolean {
    return warnings.any { it.level != WarningLevel.NORMAL } ||
        question.answer.isEmpty() ||
        question.question.isBlank() ||
        hasHardReviewError(question, warnings) ||
        (question.type in listOf(QuestionType.SINGLE, QuestionType.MULTIPLE) && question.options.size < 2)
}

private fun hasHardReviewError(question: Question, warnings: List<ImportWarning>): Boolean {
    return warnings.any { it.level == WarningLevel.ERROR } ||
        question.question.isBlank() ||
        (question.type in listOf(QuestionType.SINGLE, QuestionType.MULTIPLE) && question.options.isEmpty())
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
    onIndexChange: (Int) -> Unit
) {
    GlassCard {
        Text(
            text = "当前筛选列表",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        indices.take(12).forEach { index ->
            val question = questions[index]
            val warningCount = warningsForQuestion(question, warnings).count { it.level != WarningLevel.NORMAL }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIndexChange(index) },
                shape = RoundedCornerShape(ShirohaRadius.Md),
                color = if (index == currentIndex) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardWhite78,
                border = BorderStroke(
                    1.dp,
                    if (index == currentIndex) ShirohaColors.LineSelected else ShirohaColors.LineStrong
                )
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
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
            }
            Spacer(Modifier.height(8.dp))
        }
        if (indices.size > 12) {
            NoticeCard("当前筛选共有 ${indices.size} 题，这里先显示前 12 题；可用“上一条 / 下一条”继续核对。", warning = false)
        }
    }
}

private fun normalizeAfterTypeChange(question: Question, type: QuestionType): Question {
    return when (type) {
        QuestionType.JUDGE -> question.copy(
            type = type,
            options = if (question.options.isEmpty()) defaultJudgeOptions() else question.options,
            answer = normalizeJudgeAnswer(question.answer)
        )
        QuestionType.SINGLE,
        QuestionType.MULTIPLE -> question.copy(type = type)
        QuestionType.BLANK,
        QuestionType.SHORT -> question.copy(type = type)
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

private fun readImportedContent(
    context: Context,
    uri: Uri,
    fileName: String
): QuestionImportAssetExtractor.ImportedContent? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return QuestionImportAssetExtractor.decode(context, bytes, fileName)
}

private fun readImportedText(context: Context, uri: Uri, fileName: String): String? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return TextImportDecoder.decode(bytes, fileName)
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

