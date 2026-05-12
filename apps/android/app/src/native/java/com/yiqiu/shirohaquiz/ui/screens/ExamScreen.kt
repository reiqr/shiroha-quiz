package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.ExamSummary
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.EmptyStateIllustration
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.MetricGlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuizOptionCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import kotlinx.coroutines.delay

private enum class ExamGroupMode {
    RANDOM,
    CUSTOM
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamScreen(
    onBackHome: () -> Unit,
    onGoPractice: () -> Unit
) {
    val activeBank = QuizRepository.activeBank()
    val typeAvailableCounts = remember(activeBank?.id, activeBank?.questions?.size) {
        QuizRepository.questionTypeCounts(activeBank?.questions.orEmpty())
            .filterKeys { it in examTypeOrder }
    }
    val availableExamTypes = remember(typeAvailableCounts) {
        examTypeOrder.filter { (typeAvailableCounts[it] ?: 0) > 0 }.toSet()
    }
    val availableExamCount = typeAvailableCounts.values.sum()
    var selectedQuestionCount by remember(activeBank?.id) {
        mutableIntStateOf(availableExamCount.coerceAtMost(100).coerceAtLeast(1))
    }
    var selectedDurationMinutes by remember { mutableIntStateOf(30) }
    var groupMode by remember(activeBank?.id) { mutableStateOf(ExamGroupMode.RANDOM) }
    var typeCountTexts by remember(activeBank?.id) { mutableStateOf(defaultTypeCountTextMap()) }
    var typeScoreTexts by remember(activeBank?.id) { mutableStateOf(defaultTypeScoreTextMap()) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "考试模式",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
                ActionPillButton(
                    icon = Icons.Rounded.PlayArrow,
                    text = "切换练习",
                    primary = false,
                    modifier = Modifier.height(44.dp),
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
                onSelectQuestionCount = { selectedQuestionCount = it },
                selectedDurationMinutes = selectedDurationMinutes,
                onSelectDuration = { selectedDurationMinutes = it },
                groupMode = groupMode,
                onSelectGroupMode = { groupMode = it },
                typeCountTexts = typeCountTexts,
                typeScoreTexts = typeScoreTexts,
                showGroupSettings = showGroupSettings,
                onOpenGroupSettings = { showGroupSettings = true },
                onCloseGroupSettings = { showGroupSettings = false },
                onUpdateTypeCount = { type, value ->
                    val max = typeAvailableCounts[type] ?: 0
                    val clean = value.filter { it.isDigit() }.take(4)
                    val bounded = clean.toIntOrNull()?.coerceIn(0, max)?.toString() ?: clean
                    typeCountTexts = typeCountTexts + (type to bounded)
                },
                onUpdateTypeScore = { type, value ->
                    typeScoreTexts = typeScoreTexts + (type to value.filter { it.isDigit() || it == '.' }.take(4))
                },
                onStartExam = {
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
            ActiveExamPanel()
            return
        }

        FinishedExamPanel(
            examSummary = examSummary,
            onRestart = { QuizRepository.resetExam() },
            onGoPractice = {
                QuizRepository.resetExam()
                onGoPractice()
            },
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
    onSelectQuestionCount: (Int) -> Unit,
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
            listOf(50, 100, availableExamCount)
                .filter { it > 0 }
                .map { it.coerceAtMost(availableExamCount) }
                .distinct()
                .forEach { count ->
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = if (count == availableExamCount) "全部 $count 题" else "$count 题",
                        primary = selectedQuestionCount == count,
                        onClick = { onSelectQuestionCount(count) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveExamPanel() {
    val examQuestion = QuizRepository.currentExamQuestion() ?: return
    val questionType = examQuestion.type
    var showAnswerCard by remember { mutableStateOf(false) }
    var showSubmitConfirm by remember { mutableStateOf(false) }
    val unansweredCount = QuizRepository.examQuestions.size - QuizRepository.examAnsweredCount()

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ExamMetricCard(
            title = "剩余时间",
            value = formatExamSeconds(QuizRepository.examRemainingSeconds),
            modifier = Modifier.weight(1f)
        )
        GlassCard(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text("已答题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(QuizRepository.examAnsweredCount().toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.ListAlt,
                text = "答题卡",
                primary = false,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                fillWidthContent = true,
                onClick = { showAnswerCard = true }
            )
        }
    }

    GlassCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("第 ${QuizRepository.examIndex + 1} / ${QuizRepository.examQuestions.size} 题", selected = true)
            StatusChip(typeLabel(questionType))
            StatusChip("剩余 ${formatExamSeconds(QuizRepository.examRemainingSeconds)}")
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = examQuestion.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 34.sp
        )
        if (examQuestion.images.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            QuestionImagesBlock(examQuestion.images, maxPreviewHeight = 360.dp, showMeta = true)
        }
        Spacer(Modifier.height(18.dp))

        when (examQuestion.type) {
            QuestionType.SINGLE,
            QuestionType.MULTIPLE,
            QuestionType.JUDGE -> {
                val currentAnswer = QuizRepository.examAnswers[examQuestion.id].orEmpty()
                examQuestion.options.forEach { option ->
                    QuizOptionCard(
                        label = option.key,
                        text = option.text,
                        selected = currentAnswer.contains(option.key),
                        onClick = {
                            QuizRepository.toggleExamAnswer(
                                key = option.key,
                                multiple = examQuestion.type == QuestionType.MULTIPLE
                            )
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            QuestionType.BLANK,
            QuestionType.SHORT -> {
                NoticeCard("这道题属于 ${typeLabel(examQuestion.type)}。当前考试模式先完成客观题流程。")
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPillButton(Icons.AutoMirrored.Rounded.ArrowBack, "上一题", primary = false, onClick = { QuizRepository.previousExamQuestion() })
            ActionPillButton(Icons.AutoMirrored.Rounded.ArrowForward, "下一题", primary = false, onClick = { QuizRepository.nextExamQuestion() })
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPillButton(
                Icons.Rounded.CheckCircle,
                "立即交卷",
                primary = true,
                onClick = {
                    if (unansweredCount > 0) {
                        showSubmitConfirm = true
                    } else {
                        QuizRepository.submitExam()
                    }
                }
            )
            ActionPillButton(Icons.Rounded.Timer, "结束本场", primary = false, onClick = { QuizRepository.resetExam() })
        }
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


@OptIn(ExperimentalLayoutApi::class)
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip("当前", selected = true)
                    StatusChip("已答")
                    StatusChip("未答")
                }
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuizRepository.examQuestions.forEachIndexed { index, question ->
                            val answeredQuestion = QuizRepository.examAnswers[question.id].orEmpty().isNotEmpty()
                            val currentQuestion = index == QuizRepository.examIndex
                            val icon = when {
                                currentQuestion -> Icons.AutoMirrored.Rounded.ListAlt
                                answeredQuestion -> Icons.Rounded.CheckCircle
                                else -> Icons.Rounded.Timer
                            }
                            ActionPillButton(
                                icon = icon,
                                text = (index + 1).toString(),
                                primary = currentQuestion,
                                modifier = Modifier.height(38.dp),
                                onClick = { onJumpToQuestion(index) }
                            )
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
    onRestart: () -> Unit,
    onGoPractice: () -> Unit,
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
            ActionPillButton(Icons.Rounded.PlayArrow, "去练习页", primary = false, onClick = onGoPractice)
            ActionPillButton(Icons.Rounded.Timer, "返回首页", primary = false, onClick = onBackHome)
        }
    }
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
