package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.ExamSummary
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.EmptyStateIllustration
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.MetricGlassCard
import com.yiqiu.shirohaquiz.ui.components.MultiBlankAnswerInputs
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuizOptionCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import com.yiqiu.shirohaquiz.ui.text.LatexDisplayFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.abs

private enum class ExamGroupMode {
    RANDOM,
    CUSTOM
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamScreen(
    onBackHome: () -> Unit,
    onGoPractice: () -> Unit,
    onOpenRecord: (String) -> Unit
) {
    val context = LocalContext.current
    val activeBank = QuizRepository.activeBank()
    val typeAvailableCounts = remember(activeBank?.id, activeBank?.questions?.size) {
        QuizRepository.questionTypeCounts(activeBank?.questions.orEmpty())
            .filterKeys { it in examTypeOrder }
    }
    val availableExamTypes = remember(typeAvailableCounts) {
        examTypeOrder.filter { (typeAvailableCounts[it] ?: 0) > 0 }.toSet()
    }
    val availableExamCount = typeAvailableCounts.values.sum()
    val initialExamCountChoice = remember(
        activeBank?.id,
        availableExamCount,
        QuizRepository.rememberExamSettingsEnabled,
        QuizRepository.preferredExamQuestionCountMode,
        QuizRepository.preferredExamCustomQuestionCount
    ) {
        if (QuizRepository.rememberExamSettingsEnabled) {
            resolveExamQuestionCount(
                mode = QuizRepository.preferredExamQuestionCountMode,
                customCount = QuizRepository.preferredExamCustomQuestionCount,
                availableCount = availableExamCount
            )
        } else {
            resolveExamQuestionCount(
                mode = if (availableExamCount >= 100) "100" else "custom",
                customCount = availableExamCount.coerceAtMost(100).coerceAtLeast(1),
                availableCount = availableExamCount
            )
        }
    }
    var selectedQuestionCount by remember(activeBank?.id, initialExamCountChoice.count) {
        mutableIntStateOf(initialExamCountChoice.count)
    }
    var selectedQuestionCountMode by remember(activeBank?.id, initialExamCountChoice.mode) {
        mutableStateOf(initialExamCountChoice.mode)
    }
    var selectedDurationMinutes by remember(
        activeBank?.id,
        QuizRepository.rememberExamSettingsEnabled,
        QuizRepository.preferredExamDurationMinutes
    ) {
        mutableIntStateOf(if (QuizRepository.rememberExamSettingsEnabled) QuizRepository.preferredExamDurationMinutes else 30)
    }
    var groupMode by remember(activeBank?.id, QuizRepository.rememberExamSettingsEnabled, QuizRepository.preferredExamGroupMode) {
        mutableStateOf(
            if (QuizRepository.rememberExamSettingsEnabled && QuizRepository.preferredExamGroupMode == "custom") {
                ExamGroupMode.CUSTOM
            } else {
                ExamGroupMode.RANDOM
            }
        )
    }
    var typeCountTexts by remember(activeBank?.id, QuizRepository.rememberExamSettingsEnabled, typeAvailableCounts) {
        mutableStateOf(
            if (QuizRepository.rememberExamSettingsEnabled) {
                QuizRepository.preferredExamTypeCountTexts(typeAvailableCounts)
            } else {
                defaultTypeCountTextMap()
            }
        )
    }
    var typeScoreTexts by remember(activeBank?.id, QuizRepository.rememberExamSettingsEnabled) {
        mutableStateOf(
            if (QuizRepository.rememberExamSettingsEnabled) {
                QuizRepository.preferredExamTypeScoreTexts()
            } else {
                defaultTypeScoreTextMap()
            }
        )
    }
    var showGroupSettings by remember { mutableStateOf(false) }

    val examQuestion = QuizRepository.currentExamQuestion()
    val examSummary = QuizRepository.examSummary()

    if (QuizRepository.examQuestions.isNotEmpty() && !QuizRepository.examFinished) {
        LaunchedEffect(QuizRepository.examRemainingSeconds, QuizRepository.examFinished) {
            if (!QuizRepository.examFinished && QuizRepository.examRemainingSeconds > 0) {
                delay(1000)
                QuizRepository.tickExam()
            }
        }
    }

    val isActiveExamRunning = QuizRepository.examQuestions.isNotEmpty() && !QuizRepository.examFinished && examQuestion != null
    var examStatusExpanded by rememberSaveable(QuizRepository.examQuestions.size) { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Sm)) {
            Text(
                text = "Exam",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "考试模式",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                if (isActiveExamRunning && examStatusExpanded) {
                    ExamCollapseTextPill(
                        text = "收起",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(64.dp)
                            .height(28.dp),
                        onClick = { examStatusExpanded = false }
                    )
                }
                ActionPillButton(
                    icon = Icons.Rounded.PlayArrow,
                    text = "切换练习",
                    primary = false,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(44.dp),
                    onClick = onGoPractice
                )
            }
        }

        if (activeBank == null || activeBank.questions.isEmpty() || availableExamCount == 0) {
            EmptyStateIllustration(
                title = "还没有可用于考试的客观题",
                message = "当前考试先支持单选题、多选题和判断题。",
                action = { Spacer(Modifier.height(14.dp)) }
            )
            GlassCard {
                ActionPillButton(
                    icon = Icons.Rounded.PlayArrow,
                    text = "返回首页",
                    primary = true,
                    onClick = onBackHome
                )
            }
            return
        }

        if (QuizRepository.examQuestions.isEmpty() && !QuizRepository.examFinished) {
            ExamSetupPanel(
                bankName = activeBank.name,
                totalQuestions = activeBank.questions.size,
                availableExamCount = availableExamCount,
                typeAvailableCounts = typeAvailableCounts,
                selectedQuestionCount = selectedQuestionCount.coerceAtMost(availableExamCount),
                selectedQuestionCountMode = selectedQuestionCountMode,
                onSelectQuestionCount = { count, mode ->
                    selectedQuestionCount = count
                    selectedQuestionCountMode = mode
                    QuizRepository.rememberExamSettings(
                        context = context,
                        questionCountMode = mode,
                        customQuestionCount = if (mode == "custom") count else selectedQuestionCount
                    )
                },
                selectedDurationMinutes = selectedDurationMinutes,
                onSelectDuration = { minutes ->
                    selectedDurationMinutes = minutes
                    QuizRepository.rememberExamSettings(context = context, durationMinutes = minutes)
                },
                groupMode = groupMode,
                onSelectGroupMode = { mode ->
                    groupMode = mode
                    QuizRepository.rememberExamSettings(context = context, groupMode = mode.preferenceKey())
                },
                typeCountTexts = typeCountTexts,
                typeScoreTexts = typeScoreTexts,
                showGroupSettings = showGroupSettings,
                onOpenGroupSettings = { showGroupSettings = true },
                onCloseGroupSettings = { showGroupSettings = false },
                onUpdateTypeCount = { type, value ->
                    val max = typeAvailableCounts[type] ?: 0
                    val clean = value.filter { it.isDigit() }.take(4)
                    val bounded = clean.toIntOrNull()?.coerceIn(0, max)?.toString() ?: clean
                    val updated = typeCountTexts + (type to bounded)
                    typeCountTexts = updated
                    QuizRepository.rememberExamSettings(context = context, typeCountTexts = updated)
                },
                onUpdateTypeScore = { type, value ->
                    val updated = typeScoreTexts + (type to value.filter { it.isDigit() || it == '.' }.take(4))
                    typeScoreTexts = updated
                    QuizRepository.rememberExamSettings(context = context, typeScoreTexts = updated)
                },
                onStartExam = {
                    QuizRepository.rememberExamSettings(
                        context = context,
                        questionCountMode = selectedQuestionCountMode,
                        customQuestionCount = selectedQuestionCount,
                        durationMinutes = selectedDurationMinutes,
                        groupMode = groupMode.preferenceKey(),
                        typeCountTexts = typeCountTexts,
                        typeScoreTexts = typeScoreTexts
                    )
                    if (groupMode == ExamGroupMode.RANDOM) {
                        val finalCount = selectedQuestionCount.coerceIn(1, availableExamCount)
                        val autoScore = 100.0 / finalCount
                        val scoreMap = examTypeOrder.associateWith { autoScore }
                        QuizRepository.startExam(
                            questionCount = finalCount,
                            durationMinutes = selectedDurationMinutes,
                            allowedTypes = availableExamTypes,
                            typeScores = scoreMap,
                            randomize = true
                        )
                    } else {
                        val typeCounts = examTypeOrder.associateWith { type ->
                            val max = typeAvailableCounts[type] ?: 0
                            typeCountTexts[type].orEmpty().toIntOrNull()?.coerceIn(0, max) ?: 0
                        }.filterValues { it > 0 }
                        val scoreMap = examTypeOrder.associateWith { type ->
                            typeScoreTexts[type].orEmpty().toDoubleOrNull()?.coerceAtLeast(0.0) ?: defaultTypeScore(type)
                        }
                        QuizRepository.startExamByTypeCounts(
                            typeCounts = typeCounts,
                            durationMinutes = selectedDurationMinutes,
                            typeScores = scoreMap
                        )
                    }
                }
            )
            return
        }

        if (!QuizRepository.examFinished && examQuestion != null) {
            ActiveExamPanel(
                examStatusExpanded = examStatusExpanded,
                onExamStatusExpandedChange = { examStatusExpanded = it }
            )
            return
        }

        FinishedExamPanel(
            examSummary = examSummary,
            recordId = QuizRepository.studyRecords.firstOrNull { it.source == "考试" }?.id,
            onRestart = { QuizRepository.resetExam() },
            onOpenRecord = onOpenRecord,
            onBackHome = {
                QuizRepository.resetExam()
                onBackHome()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExamSetupPanel(
    bankName: String,
    totalQuestions: Int,
    availableExamCount: Int,
    typeAvailableCounts: Map<QuestionType, Int>,
    selectedQuestionCount: Int,
    selectedQuestionCountMode: String,
    onSelectQuestionCount: (Int, String) -> Unit,
    selectedDurationMinutes: Int,
    onSelectDuration: (Int) -> Unit,
    groupMode: ExamGroupMode,
    onSelectGroupMode: (ExamGroupMode) -> Unit,
    typeCountTexts: Map<QuestionType, String>,
    typeScoreTexts: Map<QuestionType, String>,
    showGroupSettings: Boolean,
    onOpenGroupSettings: () -> Unit,
    onCloseGroupSettings: () -> Unit,
    onUpdateTypeCount: (QuestionType, String) -> Unit,
    onUpdateTypeScore: (QuestionType, String) -> Unit,
    onStartExam: () -> Unit
) {
    var customDurationText by remember(selectedDurationMinutes) {
        mutableStateOf(selectedDurationMinutes.toString())
    }
    var showCustomCountDialog by remember { mutableStateOf(false) }
    var customQuestionCountText by remember(availableExamCount) {
        mutableStateOf(selectedQuestionCount.coerceIn(1, availableExamCount.coerceAtLeast(1)).toString())
    }
    val bankNameSize = when {
        bankName.length > 24 -> 14.sp
        bankName.length > 16 -> 16.sp
        else -> 18.sp
    }
    val customCounts = examTypeOrder.associateWith { type ->
        val max = typeAvailableCounts[type] ?: 0
        typeCountTexts[type].orEmpty().toIntOrNull()?.coerceIn(0, max) ?: 0
    }
    val customQuestionCount = customCounts.values.sum()
    val customTotalScore = customCounts.entries.sumOf { (type, count) ->
        count * (typeScoreTexts[type].orEmpty().toDoubleOrNull() ?: defaultTypeScore(type))
    }
    val groupSummary = if (groupMode == ExamGroupMode.RANDOM) {
        "自动从客观题里随机抽题，并把本场总分折算为 100 分。"
    } else {
        if (customQuestionCount == 0) "点击设置每种题型的题量和分值。" else "预计 $customQuestionCount 题 · ${customTotalScore.trimScoreText()} 分"
    }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "考试设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = bankName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = bankNameSize),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "共 $totalQuestions 题 · 可考试 $availableExamCount 题",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ActionPillButton(
                icon = Icons.Rounded.PlayArrow,
                text = "开始考试",
                primary = true,
                modifier = Modifier.height(46.dp),
                onClick = onStartExam
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("题量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val safeAvailable = availableExamCount.coerceAtLeast(1)
            ActionPillButton(
                icon = Icons.Rounded.PlayArrow,
                text = "自定义",
                primary = selectedQuestionCountMode == "custom",
                onClick = {
                    customQuestionCountText = selectedQuestionCount.coerceIn(1, safeAvailable).toString()
                    showCustomCountDialog = true
                }
            )
            buildList {
                if (safeAvailable >= 50) add(Triple(50, "50 题", "50"))
                if (safeAvailable >= 100) add(Triple(100, "100 题", "100"))
                add(Triple(safeAvailable, "全部 $safeAvailable 题", "all"))
            }
                .distinctBy { it.first }
                .forEach { (count, label, mode) ->
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = label,
                        primary = selectedQuestionCountMode == mode,
                        onClick = { onSelectQuestionCount(count, mode) }
                    )
                }
        }

        Spacer(Modifier.height(18.dp))
        Text("时长", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 60, 120).forEach { minutes ->
                ActionPillButton(
                    icon = Icons.Rounded.Timer,
                    text = "$minutes 分钟",
                    primary = selectedDurationMinutes == minutes,
                    onClick = {
                        customDurationText = minutes.toString()
                        onSelectDuration(minutes)
                    }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = customDurationText,
            onValueChange = { value ->
                val cleanValue = value.filter { it.isDigit() }.take(3)
                customDurationText = cleanValue
                cleanValue.toIntOrNull()?.coerceIn(1, 999)?.let(onSelectDuration)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("自定义时间（分钟）") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )

        Spacer(Modifier.height(18.dp))
        Text("组题方式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPillButton(
                icon = Icons.Rounded.RestartAlt,
                text = "随机组题",
                primary = groupMode == ExamGroupMode.RANDOM,
                modifier = Modifier.weight(1f).height(46.dp),
                fillWidthContent = true,
                onClick = { onSelectGroupMode(ExamGroupMode.RANDOM) }
            )
            ActionPillButton(
                icon = Icons.Rounded.CheckCircle,
                text = "自定义组题",
                primary = groupMode == ExamGroupMode.CUSTOM,
                modifier = Modifier.weight(1f).height(46.dp),
                fillWidthContent = true,
                onClick = { onSelectGroupMode(ExamGroupMode.CUSTOM) }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (groupMode == ExamGroupMode.RANDOM) "自动组题" else "自定义组题",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = groupSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (groupMode == ExamGroupMode.CUSTOM) {
                ActionPillButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "设置",
                    primary = false,
                    modifier = Modifier.height(44.dp),
                    onClick = onOpenGroupSettings
                )
            }
        }
    }

    if (showCustomCountDialog) {
        CustomQuestionCountDialog(
            title = "自定义考试题量",
            value = customQuestionCountText,
            maxCount = availableExamCount.coerceAtLeast(1),
            onValueChange = { customQuestionCountText = it },
            onDismiss = { showCustomCountDialog = false },
            onConfirm = { count ->
                onSelectQuestionCount(count, "custom")
                showCustomCountDialog = false
            }
        )
    }

    if (showGroupSettings) {
        AlertDialog(
            onDismissRequest = onCloseGroupSettings,
            title = { Text("自定义组题") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "按题型设置抽取数量和每题分值。数量为 0 表示不考该题型。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        examTypeOrder.forEach { type ->
                            val available = typeAvailableCounts[type] ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(typeLabel(type), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text("可用 $available 题", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedTextField(
                                    value = typeCountTexts[type].orEmpty(),
                                    onValueChange = { onUpdateTypeCount(type, it) },
                                    modifier = Modifier.width(76.dp),
                                    singleLine = true,
                                    label = { Text("题") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                                )
                                OutlinedTextField(
                                    value = typeScoreTexts[type].orEmpty().ifBlank { defaultTypeScore(type).trimScoreText() },
                                    onValueChange = { onUpdateTypeScore(type, it) },
                                    modifier = Modifier.width(76.dp),
                                    singleLine = true,
                                    label = { Text("分") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onCloseGroupSettings) { Text("完成") } },
            dismissButton = {
                TextButton(onClick = {
                    examTypeOrder.forEach { type ->
                        val available = typeAvailableCounts[type] ?: 0
                        onUpdateTypeCount(type, available.toString())
                    }
                }) { Text("填满") }
            }
        )
    }
}

@Composable
private fun ExamMetricCard(
    title: String,
    value: String,
    content: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content?.invoke()
    }
}

@Composable
private fun ExamCollapseTextPill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .shirohaNoRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = ShirohaColors.CardWhite62,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSoft)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = ShirohaColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExamCollapsedStatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .shirohaNoRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = ShirohaColors.CardWhite86,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineStrong)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveExamPanel(
    examStatusExpanded: Boolean,
    onExamStatusExpandedChange: (Boolean) -> Unit
) {
    val examAutoNextScope = rememberCoroutineScope()
    val examQuestion = QuizRepository.currentExamQuestion() ?: return
    val questionType = examQuestion.type
    var showAnswerCard by remember { mutableStateOf(false) }
    var showSubmitConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val answeredCount = QuizRepository.examAnsweredCount()
    val unansweredCount = QuizRepository.examQuestions.size - answeredCount
    val displayOptions = remember(
        examQuestion.id,
        examQuestion.options,
        QuizRepository.examOptionShuffleSessionEnabled,
        QuizRepository.examOptionShuffleSeed
    ) {
        examDisplayOptions(
            question = examQuestion,
            shuffleEnabled = QuizRepository.examOptionShuffleSessionEnabled,
            sessionSeed = QuizRepository.examOptionShuffleSeed
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(if (examStatusExpanded) 8.dp else 6.dp)) {
    if (examStatusExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp)
                ) {
                    Text(
                        formatExamSeconds(QuizRepository.examRemainingSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("剩余时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("已答题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(answeredCount.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    ActionPillButton(
                        icon = Icons.AutoMirrored.Rounded.ListAlt,
                        text = "答题卡",
                        primary = false,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(108.dp)
                            .height(38.dp),
                        fillWidthContent = true,
                        onClick = { showAnswerCard = true }
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExamCollapsedStatusPill(
                icon = Icons.Rounded.Timer,
                text = formatExamSeconds(QuizRepository.examRemainingSeconds),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                onClick = { onExamStatusExpandedChange(true) }
            )
            ExamCollapsedStatusPill(
                icon = Icons.AutoMirrored.Rounded.ListAlt,
                text = "答题卡 $answeredCount/${QuizRepository.examQuestions.size}",
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                onClick = { showAnswerCard = true }
            )
        }
    }

    val questionCardModifier = if (QuizRepository.swipeNavigationEnabled) {
        Modifier.questionSwipeNavigation(
            onSwipeLeft = { QuizRepository.nextExamQuestion() },
            onSwipeRight = { QuizRepository.previousExamQuestion() }
        )
    } else {
        Modifier
    }

    GlassCard(modifier = questionCardModifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                StatusChip(
                    "${QuizRepository.examIndex + 1}/${QuizRepository.examQuestions.size}",
                    selected = true
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                StatusChip(typeLabel(questionType))
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                StatusChip(formatExamSeconds(QuizRepository.examRemainingSeconds))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = LatexDisplayFormatter.format(examQuestion.question),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = QuizRepository.questionFontSizeSp().sp,
                lineHeight = QuizRepository.questionLineHeightSp().sp
            ),
            fontWeight = FontWeight.SemiBold
        )
        if (examQuestion.images.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            QuestionImagesBlock(examQuestion.images, maxPreviewHeight = 360.dp, showMeta = true)
        }
        Spacer(Modifier.height(22.dp))

        when (examQuestion.type) {
            QuestionType.SINGLE,
            QuestionType.MULTIPLE,
            QuestionType.JUDGE -> {
                val currentAnswer = QuizRepository.examAnswers[examQuestion.id].orEmpty()
                displayOptions.forEach { option ->
                    QuizOptionCard(
                        label = option.displayKey,
                        text = option.text,
                        selected = currentAnswer.any { answer ->
                            answer.trim().equals(option.originalKey, ignoreCase = true)
                        },
                        onClick = {
                            val answerQuestionId = examQuestion.id
                            val answerQuestionIndex = QuizRepository.examIndex
                            QuizRepository.toggleExamAnswer(
                                key = option.originalKey,
                                multiple = examQuestion.type == QuestionType.MULTIPLE
                            )
                            val shouldAutoNext = QuizRepository.examAutoNextEnabled &&
                                (examQuestion.type == QuestionType.SINGLE || examQuestion.type == QuestionType.JUDGE) &&
                                answerQuestionIndex < QuizRepository.examQuestions.lastIndex
                            if (shouldAutoNext) {
                                examAutoNextScope.launch {
                                    delay(180)
                                    val stillOnAnsweredQuestion = QuizRepository.examIndex == answerQuestionIndex &&
                                        QuizRepository.currentExamQuestion()?.id == answerQuestionId
                                    if (stillOnAnsweredQuestion && !QuizRepository.examFinished) {
                                        QuizRepository.nextExamQuestion()
                                    }
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(if (QuizRepository.compactOptionsEnabled) 8.dp else 10.dp))
                }
            }

            QuestionType.BLANK -> {
                val currentAnswer = QuizRepository.examAnswers[examQuestion.id].orEmpty()
                if (MultiBlankSupport.hasStructuredAnswers(examQuestion)) {
                    MultiBlankAnswerInputs(
                        blankCount = examQuestion.blankAnswers.size,
                        values = currentAnswer,
                        enabled = true,
                        onValueChange = QuizRepository::updateExamBlankAnswer
                    )
                } else {
                    OutlinedTextField(
                        value = currentAnswer.firstOrNull().orEmpty(),
                        onValueChange = QuizRepository::updateExamTextAnswer,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("你的答案") },
                        placeholder = { Text("请输入填空内容") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            QuestionType.SHORT -> {
                NoticeCard("这道题属于简答题。当前考试模式暂不自动判分。")
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionPillButton(
                Icons.AutoMirrored.Rounded.ArrowBack,
                "上一题",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                fillWidthContent = true,
                onClick = { QuizRepository.previousExamQuestion() }
            )
            ActionPillButton(
                Icons.AutoMirrored.Rounded.ArrowForward,
                "下一题",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                fillWidthContent = true,
                onClick = { QuizRepository.nextExamQuestion() }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionPillButton(
                Icons.AutoMirrored.Rounded.ExitToApp,
                "结束本场",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                fillWidthContent = true,
                onClick = { showExitConfirm = true }
            )
            ActionPillButton(
                Icons.Rounded.CheckCircle,
                "立即交卷",
                primary = true,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                fillWidthContent = true,
                onClick = {
                    if (unansweredCount > 0) {
                        showSubmitConfirm = true
                    } else {
                        QuizRepository.submitExam()
                    }
                }
            )
        }
    }
    }

    if (showExitConfirm) {
        ConfirmExitExamDialog(
            onDismiss = { showExitConfirm = false },
            onConfirmExit = {
                showExitConfirm = false
                QuizRepository.resetExam()
            }
        )
    }

    if (showAnswerCard) {
        ExamAnswerCardDialog(
            onDismiss = { showAnswerCard = false },
            onJumpToQuestion = { index ->
                QuizRepository.jumpToExamQuestion(index)
                showAnswerCard = false
            },
            onSubmit = {
                showAnswerCard = false
                if (unansweredCount > 0) {
                    showSubmitConfirm = true
                } else {
                    QuizRepository.submitExam()
                }
            }
        )
    }

    if (showSubmitConfirm) {
        ConfirmSubmitExamDialog(
            unansweredCount = unansweredCount,
            onDismiss = { showSubmitConfirm = false },
            onGoAnswerCard = {
                showSubmitConfirm = false
                showAnswerCard = true
            },
            onConfirmSubmit = {
                showSubmitConfirm = false
                QuizRepository.submitExam()
            }
        )
    }
}


private data class ExamDisplayOption(
    val displayKey: String,
    val originalKey: String,
    val text: String
)

private fun examDisplayOptions(
    question: Question,
    shuffleEnabled: Boolean,
    sessionSeed: Long
): List<ExamDisplayOption> {
    val canShuffle = shuffleEnabled &&
        (question.type == QuestionType.SINGLE || question.type == QuestionType.MULTIPLE) &&
        question.options.size > 1
    val orderedOptions = if (canShuffle) {
        val seedSource = "${question.id}|${question.number}|${question.question}"
        val seed = sessionSeed xor seedSource.hashCode().toLong()
        question.options.toMutableList().also { options ->
            java.util.Collections.shuffle(options, java.util.Random(seed))
        }
    } else {
        question.options
    }
    return orderedOptions.mapIndexed { index, option ->
        ExamDisplayOption(
            displayKey = if (canShuffle) examDisplayKey(index) else option.key,
            originalKey = option.key,
            text = option.text
        )
    }
}

private fun examDisplayKey(index: Int): String {
    return if (index in 0..25) ('A'.code + index).toChar().toString() else (index + 1).toString()
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExamAnswerNumberChip(
    text: String,
    current: Boolean,
    answered: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(ShirohaRadius.Pill)
    Surface(
        modifier = modifier.shirohaNoRippleClickable(onClick = onClick),
        shape = shape,
        color = when {
            current -> MaterialTheme.colorScheme.primary
            answered -> ShirohaColors.BrandPrimarySoft
            else -> ShirohaColors.CardWhite86
        },
        border = BorderStroke(
            ShirohaDimens.Hairline,
            when {
                current -> MaterialTheme.colorScheme.primary
                answered -> ShirohaColors.LineSelected
                else -> ShirohaColors.LineStrong
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (current) ShirohaColors.TextOnBrand else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExamAnswerCardDialog(
    onDismiss: () -> Unit,
    onJumpToQuestion: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    val total = QuizRepository.examQuestions.size
    val answered = QuizRepository.examAnsweredCount()
    val unanswered = total - answered

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("答题卡") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricGlassCard("已答", answered.toString(), "", Modifier.weight(1f))
                    MetricGlassCard("未答", unanswered.toString(), "", Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip("当前", selected = true)
                    StatusChip("已答")
                    StatusChip("未答")
                }
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuizRepository.examQuestions.chunked(5).forEachIndexed { rowIndex, rowQuestions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowQuestions.forEachIndexed { columnIndex, question ->
                                val index = rowIndex * 5 + columnIndex
                                val answeredQuestion = QuizRepository.isExamQuestionAnswered(question)
                                val currentQuestion = index == QuizRepository.examIndex
                                ExamAnswerNumberChip(
                                    text = (index + 1).toString(),
                                    current = currentQuestion,
                                    answered = answeredQuestion,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    onClick = { onJumpToQuestion(index) }
                                )
                            }
                            repeat(5 - rowQuestions.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit) { Text("交卷") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("继续答题") }
        }
    )
}

@Composable
private fun ConfirmExitExamDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确定要退出吗？") },
        text = {
            Text(
                text = "结束本场后，本场考试进度和已选答案将不会保留。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmExit) { Text("确定退出") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("继续考试") }
        }
    )
}

@Composable
private fun ConfirmSubmitExamDialog(
    unansweredCount: Int,
    onDismiss: () -> Unit,
    onGoAnswerCard: () -> Unit,
    onConfirmSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("还有未答题") },
        text = {
            Text(
                text = "当前还有 $unansweredCount 道题未作答。确认交卷后，未答题会按错误处理。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmSubmit) { Text("仍然交卷") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onGoAnswerCard) { Text("查看答题卡") }
                TextButton(onClick = onDismiss) { Text("继续答题") }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinishedExamPanel(
    examSummary: ExamSummary,
    recordId: String?,
    onRestart: () -> Unit,
    onOpenRecord: (String) -> Unit,
    onBackHome: () -> Unit
) {
    GlassCard {
        Text("考试结果", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (examSummary.autoSubmitted) {
            NoticeCard("本场考试因为倒计时结束而自动交卷。", warning = false)
            Spacer(Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricGlassCard("总题数", examSummary.total.toString(), "", Modifier.weight(1f))
            MetricGlassCard("答题数", examSummary.answered.toString(), "", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricGlassCard("正确数", examSummary.correct.toString(), "", Modifier.weight(1f))
            MetricGlassCard("正确率", "${examAccuracy(examSummary)}%", "", Modifier.weight(1f))
        }
        if (examSummary.totalScore > 0.0) {
            Spacer(Modifier.height(12.dp))
            MetricGlassCard(
                label = "得分",
                value = "${examSummary.earnedScore.trimScoreText()} / ${examSummary.totalScore.trimScoreText()}",
                desc = "按题型分值设置计算",
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPillButton(Icons.Rounded.RestartAlt, "再来一场", primary = true, onClick = onRestart)
            ActionPillButton(
                Icons.AutoMirrored.Rounded.ListAlt,
                "查看详情",
                primary = false,
                onClick = { recordId?.let(onOpenRecord) }
            )
            ActionPillButton(Icons.Rounded.Timer, "返回首页", primary = false, onClick = onBackHome)
        }
    }
}

@Composable
private fun Modifier.questionSwipeNavigation(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier {
    val thresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    val maxOffsetPx = with(LocalDensity.current) { 28.dp.toPx() }
    val swipeOffset = remember { Animatable(0f) }
    val swipeScope = rememberCoroutineScope()

    fun resetSwipeOffset() {
        swipeScope.launch { swipeOffset.animateTo(0f, animationSpec = tween(durationMillis = 130)) }
    }

    val offsetFraction = if (maxOffsetPx > 0f) {
        (abs(swipeOffset.value) / maxOffsetPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    return this
        .graphicsLayer {
            translationX = swipeOffset.value
            alpha = 1f - offsetFraction * 0.04f
        }
        .pointerInput(onSwipeLeft, onSwipeRight, thresholdPx, maxOffsetPx) {
            var dragAmount = 0f
            detectHorizontalDragGestures(
                onDragStart = {
                    dragAmount = 0f
                    swipeScope.launch { swipeOffset.stop() }
                },
                onHorizontalDrag = { _, dragDelta ->
                    dragAmount += dragDelta
                    val visualOffset = (dragAmount * 0.34f).coerceIn(-maxOffsetPx, maxOffsetPx)
                    swipeScope.launch { swipeOffset.snapTo(visualOffset) }
                },
                onDragCancel = {
                    dragAmount = 0f
                    resetSwipeOffset()
                },
                onDragEnd = {
                    when {
                        dragAmount <= -thresholdPx -> onSwipeLeft()
                        dragAmount >= thresholdPx -> onSwipeRight()
                    }
                    dragAmount = 0f
                    resetSwipeOffset()
                }
            )
        }
}

@Composable
private fun CustomQuestionCountDialog(
    title: String,
    value: String,
    maxCount: Int,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "请输入 1～$maxCount 之间的题数。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() }.take(4)) },
                    singleLine = true,
                    label = { Text("题量") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val count = value.toIntOrNull()?.coerceIn(1, maxCount) ?: 1
                    onConfirm(count)
                }
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private data class ExamQuestionCountChoice(
    val count: Int,
    val mode: String
)

private fun resolveExamQuestionCount(
    mode: String,
    customCount: Int,
    availableCount: Int
): ExamQuestionCountChoice {
    val safeAvailable = availableCount.coerceAtLeast(1)
    return when (mode) {
        "50" -> if (safeAvailable >= 50) {
            ExamQuestionCountChoice(50, "50")
        } else {
            ExamQuestionCountChoice(customCount.coerceIn(1, safeAvailable), "custom")
        }
        "100" -> if (safeAvailable >= 100) {
            ExamQuestionCountChoice(100, "100")
        } else {
            ExamQuestionCountChoice(customCount.coerceIn(1, safeAvailable), "custom")
        }
        "all" -> ExamQuestionCountChoice(safeAvailable, "all")
        else -> ExamQuestionCountChoice(customCount.coerceIn(1, safeAvailable), "custom")
    }
}

private fun ExamGroupMode.preferenceKey(): String {
    return if (this == ExamGroupMode.CUSTOM) "custom" else "random"
}

private val examTypeOrder = listOf(
    QuestionType.SINGLE,
    QuestionType.MULTIPLE,
    QuestionType.JUDGE
)

private fun defaultTypeCountTextMap(): Map<QuestionType, String> = examTypeOrder.associateWith { "0" }

private fun defaultTypeScoreTextMap(): Map<QuestionType, String> = examTypeOrder.associateWith {
    defaultTypeScore(it).trimScoreText()
}

private fun defaultTypeScore(type: QuestionType): Double = when (type) {
    QuestionType.SINGLE -> 1.0
    QuestionType.MULTIPLE -> 2.0
    QuestionType.JUDGE -> 1.0
    QuestionType.BLANK -> 2.0
    QuestionType.SHORT -> 5.0
}

private fun Double.trimScoreText(): String {
    return if (this % 1.0 == 0.0) this.toInt().toString() else "%.2f".format(this).trimEnd('0').trimEnd('.')
}

private fun formatExamSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun examAccuracy(summary: ExamSummary): Int {
    if (summary.total == 0) return 0
    return summary.correct * 100 / summary.total
}

private fun typeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}
