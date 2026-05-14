package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaMotion
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.QuestionCheckResult
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.IllustrationHeroCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuizOptionCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeScreen(
    onGoExam: () -> Unit = {},
    onOpenRecords: () -> Unit = {}
) {
    val bank = QuizRepository.activeBank()
    val practiceQuestions = QuizRepository.activePracticeQuestions()
    val question = QuizRepository.currentPracticeQuestion()
    val result = QuizRepository.practiceLastResult
    val availableCounts = remember(bank?.id, bank?.questions?.size) {
        QuizRepository.questionTypeCounts(bank?.questions.orEmpty())
    }
    val availableTypes = practiceTypeOrder.filter { (availableCounts[it] ?: 0) > 0 }
    var selectedQuestionCount by remember(bank?.id) {
        mutableIntStateOf(bank?.questions?.size?.coerceAtMost(20) ?: 20)
    }
    var selectedTypes by remember(bank?.id) {
        mutableStateOf(
            availableTypes
                .filter { it in QuizRepository.objectiveQuestionTypes() }
                .toSet()
                .ifEmpty { availableTypes.toSet() }
                .ifEmpty { QuizRepository.objectiveQuestionTypes() }
        )
    }
    var practiceOrderMode by rememberSaveable(bank?.id) { mutableStateOf("random") }

    val selectedAvailable = remember(availableCounts, selectedTypes) {
        availableCounts.entries.sumOf { (type, count) -> if (type in selectedTypes) count else 0 }
    }
    val startPracticeWithSettings = {
        val safeTypes = selectedTypes.ifEmpty { QuizRepository.objectiveQuestionTypes() }
        val available = bank?.questions?.count { it.type in safeTypes } ?: 0
        if (available > 0) {
            val count = selectedQuestionCount.coerceIn(1, available)
            QuizRepository.startPracticeSession(
                questionCount = count,
                allowedTypes = safeTypes,
                sourceLabel = "当前题库",
                randomize = practiceOrderMode == "random"
            )
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
                text = "Practice",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "练习模式",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
                ActionPillButton(
                    icon = Icons.Rounded.Timer,
                    text = "切换考试",
                    primary = false,
                    modifier = Modifier.height(44.dp),
                    onClick = onGoExam
                )
            }
        }

        val isPracticeRunning = QuizRepository.practiceQuestions.isNotEmpty()
        if (!isPracticeRunning) {
            CompactPracticeSetupHero()
        }

        if (bank == null || bank.questions.isEmpty()) {
            GlassCard {
                NoticeCard("还没有可练习题目。请先在导入页创建题库。")
            }
            return
        }

        if (QuizRepository.practiceQuestions.isEmpty()) {
            PracticeSetupPanel(
                bankName = bank.name,
                totalQuestions = bank.questions.size,
                availableCounts = availableCounts,
                selectedTypes = selectedTypes,
                selectedQuestionCount = selectedQuestionCount.coerceAtMost(selectedAvailable.coerceAtLeast(1)),
                practiceOrderMode = practiceOrderMode,
                onSelectPracticeOrderMode = { practiceOrderMode = it },
                onToggleType = { type ->
                    val updated = if (selectedTypes.contains(type)) selectedTypes - type else selectedTypes + type
                    selectedTypes = updated
                    val newAvailable = availableCounts.entries.sumOf { (itemType, count) -> if (itemType in updated) count else 0 }
                    if (newAvailable > 0) {
                        selectedQuestionCount = selectedQuestionCount.coerceAtMost(newAvailable)
                    }
                },
                onSelectQuestionCount = { selectedQuestionCount = it },
                onStartPractice = startPracticeWithSettings
            )
            return
        }

        if (question == null) {
            GlassCard { NoticeCard("当前练习没有可显示的题目，请重新开始练习。") }
            return
        }

        val savedResult = QuizRepository.practiceAnswerResults[question.id]
        val effectiveResult = result ?: savedResult?.let { saved ->
            QuestionCheckResult(
                question = question,
                userAnswer = saved.userAnswer,
                correct = saved.correct,
                answerText = saved.answerText
            )
        }
        val isSubmitted = effectiveResult != null
        val canGoNext = !QuizRepository.practiceNextRequiresResult || isSubmitted
        val displayedSelection = effectiveResult?.userAnswer ?: QuizRepository.selectedAnswer
        val isPracticeComplete = practiceQuestions.isNotEmpty() &&
            QuizRepository.practiceAnsweredCount() >= practiceQuestions.size

        PracticeProgressCard(
            currentIndex = QuizRepository.practiceIndex + 1,
            total = practiceQuestions.size,
            answered = QuizRepository.practiceAnsweredCount(),
            correct = QuizRepository.practiceCorrectCount()
        )

        if (isPracticeComplete) {
            PracticeCompletionCard(
                total = practiceQuestions.size,
                answered = QuizRepository.practiceAnsweredCount(),
                correct = QuizRepository.practiceCorrectCount(),
                onRestart = {
                    QuizRepository.endPracticeSession()
                    startPracticeWithSettings()
                },
                onOpenRecords = {
                    QuizRepository.endPracticeSession()
                    onOpenRecords()
                },
                onExit = { QuizRepository.endPracticeSession() }
            )
        }

        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactPracticeChip("第 ${QuizRepository.practiceIndex + 1} / ${practiceQuestions.size} 题", selected = true)
                    CompactPracticeChip(typeLabel(question.type))
                }
                CompactExitPracticeButton(
                    onClick = { QuizRepository.endPracticeSession() }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (question.images.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                QuestionImagesBlock(question.images, maxPreviewHeight = 360.dp, showMeta = true)
            }
            Spacer(Modifier.height(14.dp))

            when (question.type) {
                QuestionType.SINGLE,
                QuestionType.MULTIPLE,
                QuestionType.JUDGE -> {
                    question.options.forEach { option ->
                        QuizOptionCard(
                            label = option.key,
                            text = option.text,
                            selected = displayedSelection.contains(option.key),
                            onClick = {
                                if (!isSubmitted) {
                                    QuizRepository.toggleAnswer(
                                        key = option.key,
                                        multiple = question.type == QuestionType.MULTIPLE
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                QuestionType.BLANK,
                QuestionType.SHORT -> {
                    NoticeCard("这道题属于 ${typeLabel(question.type)}，当前先展示参考答案和解析。")
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    Icons.AutoMirrored.Rounded.TextSnippet,
                    "查看解析",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (!isSubmitted) {
                            QuizRepository.submitPracticeQuestion()
                        }
                    }
                )
                ActionPillButton(
                    Icons.Rounded.CheckCircle,
                    if (isSubmitted) "已提交" else "提交答案",
                    primary = !isSubmitted,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (!isSubmitted) {
                            QuizRepository.submitPracticeQuestion()
                        }
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
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
                    onClick = { QuizRepository.previousQuestion() }
                )
                ActionPillButton(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    "下一题",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    enabled = canGoNext,
                    onClick = { QuizRepository.nextQuestion() }
                )
            }

            if (effectiveResult != null) {
                Spacer(Modifier.height(16.dp))
                NoticeCard(
                    text = if (effectiveResult.correct) "回答正确" else "回答错误",
                    warning = !effectiveResult.correct
                )
                Spacer(Modifier.height(8.dp))
                NoticeCard("正确答案：${effectiveResult.answerText}", warning = false)
                if (question.analysis.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "解析：${question.analysis}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeSetupPanel(
    bankName: String,
    totalQuestions: Int,
    availableCounts: Map<QuestionType, Int>,
    selectedTypes: Set<QuestionType>,
    selectedQuestionCount: Int,
    practiceOrderMode: String,
    onSelectPracticeOrderMode: (String) -> Unit,
    onToggleType: (QuestionType) -> Unit,
    onSelectQuestionCount: (Int) -> Unit,
    onStartPractice: () -> Unit
) {
    val selectedAvailable = availableCounts.entries.sumOf { (type, count) -> if (type in selectedTypes) count else 0 }
    GlassCard {
        Text(
            text = bankName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "共 $totalQuestions 题 · 选范围后开始",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))

        Text("组题方式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionPillButton(
                icon = Icons.Rounded.PlayArrow,
                text = "随机刷题",
                primary = practiceOrderMode == "random",
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                fillWidthContent = true,
                onClick = { onSelectPracticeOrderMode("random") }
            )
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.TextSnippet,
                text = "顺序刷题",
                primary = practiceOrderMode == "ordered",
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                fillWidthContent = true,
                onClick = { onSelectPracticeOrderMode("ordered") }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (practiceOrderMode == "random") "从已选题型中随机抽题。" else "按题库原顺序练习。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(10.dp))
        Text("题型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            practiceTypeOrder.filter { (availableCounts[it] ?: 0) > 0 }.forEach { type ->
                val note = if (type in QuizRepository.objectiveQuestionTypes()) "" else " · 主观"
                ActionPillButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "${typeLabel(type)} ${availableCounts[type] ?: 0}$note",
                    primary = type in selectedTypes,
                    modifier = Modifier.height(44.dp),
                    onClick = { onToggleType(type) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("题量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "可用 $selectedAvailable 题",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            val safeAvailable = selectedAvailable.coerceAtLeast(1)
            val halfCount = (safeAvailable / 2).coerceAtLeast(1)
            buildList {
                if (safeAvailable >= 20) add(20 to "20 题")
                if (safeAvailable >= 50) add(50 to "50 题")
                if (safeAvailable >= 100) add(100 to "100 题")
                add(halfCount to "一半 $halfCount 题")
                add(safeAvailable to "全部 $safeAvailable 题")
            }
                .distinctBy { it.first }
                .forEach { (count, label) ->
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = label,
                        primary = selectedQuestionCount == count,
                        modifier = Modifier.height(44.dp),
                        onClick = { onSelectQuestionCount(count) }
                    )
                }
        }
        if (selectedAvailable <= 0) {
            Spacer(Modifier.height(10.dp))
            NoticeCard("当前筛选没有可练习题目，请至少选择一种有题目的题型。", warning = true)
        }
        Spacer(Modifier.height(12.dp))
        ActionPillButton(
            icon = Icons.Rounded.PlayArrow,
            text = "开始练习",
            primary = selectedAvailable > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            fillWidthContent = true,
            onClick = { if (selectedAvailable > 0) onStartPractice() }
        )
    }
}

@Composable
private fun CompactPracticeSetupHero() {
    val density = LocalDensity.current
    val floatDistancePx = with(density) { ShirohaMotion.HeroFloatDistance.toPx() }
    val heroFloat = rememberInfiniteTransition(label = "practice_illustration_float")
    val imageOffsetY by heroFloat.animateFloat(
        initialValue = -floatDistancePx,
        targetValue = floatDistancePx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ShirohaMotion.HeroFloatMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "practice_illustration_float_y"
    )

    GlassCard(
        modifier = Modifier.height(ShirohaDimens.HeroCardHeight),
        contentPadding = ShirohaSpacing.Xl
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PracticeSetupStepCard(index = "1", text = "选好参数", selected = true)
                PracticeSetupStepCard(index = "2", text = "开始练习", selected = false)
                PracticeSetupStepCard(index = "3", text = "记录结果", selected = false)
            }
            Box(
                modifier = Modifier.size(ShirohaDimens.HeroImageFrameSize),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.illus_practice_hint_webp),
                    contentDescription = "练习提示",
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
private fun PracticeSetupStepCard(
    index: String,
    text: String,
    selected: Boolean
) {
    Surface(
        modifier = Modifier
            .width(ShirohaDimens.StepPillWidth)
            .defaultMinSize(minHeight = ShirohaDimens.StepPillMinHeight),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (selected) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardMuted,
        border = BorderStroke(ShirohaDimens.Hairline, if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft)
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
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else ShirohaColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
private fun CompactPracticeChip(
    text: String,
    selected: Boolean = false
) {
    Surface(
        modifier = Modifier.defaultMinSize(minHeight = 32.dp),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (selected) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardMuted,
        border = BorderStroke(ShirohaDimens.Hairline, if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else ShirohaColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun CompactExitPracticeButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 34.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = ShirohaColors.CardWhite86,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineStrong)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "退出练习",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "退出",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun PracticeCompletionCard(
    total: Int,
    answered: Int,
    correct: Int,
    onRestart: () -> Unit,
    onOpenRecords: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (answered == 0) 0 else correct * 100 / answered
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "本轮练习完成",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "共 $total 题 · 已提交 $answered 题 · 正确 $correct 题 · 正确率 $accuracy%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionPillButton(
                icon = Icons.Rounded.PlayArrow,
                text = "再练一组",
                primary = true,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                fillWidthContent = true,
                onClick = onRestart
            )
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.TextSnippet,
                text = "查看记录",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                fillWidthContent = true,
                onClick = onOpenRecords
            )
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                text = "返回设置",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                fillWidthContent = true,
                onClick = onExit
            )
        }
    }
}

@Composable
private fun PracticeProgressCard(
    currentIndex: Int,
    total: Int,
    answered: Int,
    correct: Int
) {
    val accuracy = if (answered == 0) 0 else correct * 100 / answered
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("刷题进度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("$currentIndex / $total", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "已提交 $answered 题 · 正确 $correct 题 · 正确率 $accuracy%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val practiceTypeOrder = listOf(
    QuestionType.SINGLE,
    QuestionType.MULTIPLE,
    QuestionType.JUDGE,
    QuestionType.BLANK,
    QuestionType.SHORT
)

private fun typeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}
