package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.importer.model.QuestionType
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
    onGoExam: () -> Unit = {}
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
        mutableStateOf(availableTypes.toSet().ifEmpty { QuizRepository.objectiveQuestionTypes() })
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                horizontalArrangement = Arrangement.SpaceBetween
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
            IllustrationHeroCard(
                title = "选好参数，开始练习。",
                subtitle = "",
                imageRes = R.drawable.illus_practice_hint,
                imageSize = 96.dp
            )
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
                selectedQuestionCount = selectedQuestionCount.coerceAtMost(bank.questions.size),
                onToggleType = { type ->
                    val updated = if (selectedTypes.contains(type)) selectedTypes - type else selectedTypes + type
                    selectedTypes = updated
                },
                onSelectQuestionCount = { selectedQuestionCount = it },
                onStartPractice = {
                    val safeTypes = selectedTypes.ifEmpty { QuizRepository.objectiveQuestionTypes() }
                    val available = bank.questions.count { it.type in safeTypes }
                    val count = selectedQuestionCount.coerceIn(1, available.coerceAtLeast(1))
                    QuizRepository.startPracticeSession(
                        questionCount = count,
                        allowedTypes = safeTypes,
                        sourceLabel = "当前题库",
                        randomize = true
                    )
                }
            )
            return
        }

        if (question == null) {
            GlassCard { NoticeCard("当前练习没有可显示的题目，请重新开始练习。") }
            return
        }

        PracticeProgressCard(
            currentIndex = QuizRepository.practiceIndex + 1,
            total = practiceQuestions.size,
            answered = QuizRepository.practiceAnsweredCount(),
            correct = QuizRepository.practiceCorrectCount()
        )

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
                            selected = QuizRepository.selectedAnswer.contains(option.key),
                            onClick = {
                                QuizRepository.toggleAnswer(
                                    key = option.key,
                                    multiple = question.type == QuestionType.MULTIPLE
                                )
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
                    onClick = { QuizRepository.nextQuestion() }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    Icons.Rounded.CheckCircle,
                    "提交答案",
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = { QuizRepository.submitPracticeQuestion() }
                )
                ActionPillButton(
                    Icons.AutoMirrored.Rounded.TextSnippet,
                    "查看解析",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (QuizRepository.practiceLastResult == null) {
                            QuizRepository.submitPracticeQuestion()
                        }
                    }
                )
            }

            if (result != null) {
                Spacer(Modifier.height(16.dp))
                NoticeCard(
                    text = if (result.correct) "回答正确" else "回答错误",
                    warning = !result.correct
                )
                Spacer(Modifier.height(8.dp))
                NoticeCard("正确答案：${result.answerText}", warning = false)
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
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "共 $totalQuestions 题 · 先选择题型和题量，再开始练习",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("题型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            practiceTypeOrder.filter { (availableCounts[it] ?: 0) > 0 }.forEach { type ->
                ActionPillButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "${typeLabel(type)} ${availableCounts[type] ?: 0}",
                    primary = type in selectedTypes,
                    onClick = { onToggleType(type) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("题量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        onClick = { onSelectQuestionCount(count) }
                    )
                }
        }
        Spacer(Modifier.height(16.dp))
        ActionPillButton(
            icon = Icons.Rounded.PlayArrow,
            text = "开始练习",
            primary = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            fillWidthContent = true,
            onClick = onStartPractice
        )
    }
}


@Composable
private fun CompactPracticeChip(
    text: String,
    selected: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) ShirohaColors.BrandPrimarySoft else Color.White.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
            .height(34.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, ShirohaColors.LineStrong)
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
                text = "退出练习",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
