package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.state.StudyQuestionResult
import com.yiqiu.shirohaquiz.state.StudyRecord
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

private enum class RecordQuestionFilter(val label: String) {
    ALL("全部题目"),
    WRONG_ONLY("只看错题")
}

@Composable
fun RecordDetailScreen(
    recordId: String?,
    onBack: () -> Unit
) {
    val record = QuizRepository.findStudyRecord(recordId)
    var questionFilter by remember { mutableStateOf(RecordQuestionFilter.ALL) }

    if (record == null) {
        LazyColumn(
            modifier = Modifier.padding(
                horizontal = ShirohaSpacing.Xl,
                vertical = ShirohaSpacing.Sm
            ),
            verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
        ) {
            item {
                ShirohaHeader(
                    kicker = "Record Detail",
                    title = "记录详情",
                    subtitle = ""
                )
            }
            item {
                GlassCard { NoticeCard("没有找到这条学习记录。", warning = true) }
            }
            item {
                ActionPillButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    text = "返回记录",
                    primary = false,
                    onClick = onBack
                )
            }
        }
        return
    }

    val indexedResults = remember(record.questionResults, questionFilter) {
        record.questionResults
            .mapIndexed { index, result -> index + 1 to result }
            .filter { (_, result) -> questionFilter == RecordQuestionFilter.ALL || !result.correct }
    }

    LazyColumn(
        modifier = Modifier.padding(
            horizontal = ShirohaSpacing.Xl,
            vertical = ShirohaSpacing.Sm
        ),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        item {
            ShirohaHeader(
                kicker = "Record Detail",
                title = "记录详情",
                subtitle = ""
            )
        }
        item {
            RecordSummaryCard(record = record, onBack = onBack)
        }

        if (record.questionResults.isEmpty()) {
            item {
                GlassCard {
                    NoticeCard("这是一条旧记录，当时没有保存逐题详情，只能查看摘要。")
                }
            }
        } else {
            item {
                Text(
                    text = "逐题结果",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecordQuestionFilter.entries.forEach { item ->
                            ActionPillButton(
                                icon = if (item == RecordQuestionFilter.WRONG_ONLY) Icons.Rounded.Close else Icons.Rounded.CheckCircle,
                                text = item.label,
                                primary = questionFilter == item,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                fillWidthContent = true,
                                onClick = { questionFilter = item }
                            )
                        }
                    }
                }
            }

            if (indexedResults.isEmpty()) {
                item {
                    GlassCard { NoticeCard("这条记录里没有错题。") }
                }
            } else {
                items(
                    items = indexedResults,
                    key = { (index, result) ->
                        "${result.sourceBankId.orEmpty()}#${result.question.id}#$index"
                    }
                ) { (index, result) ->
                    QuestionResultCard(index = index, result = result)
                }
            }
        }
    }
}

@Composable
private fun RecordSummaryCard(
    record: StudyRecord,
    onBack: () -> Unit
) {
    val wrong = (record.total - record.correct).coerceAtLeast(0)
    val accuracy = if (record.total == 0) 0 else record.correct * 100 / record.total
    val startedAt = record.startedAt ?: record.durationSeconds?.let { record.timestamp - it * 1000L } ?: record.timestamp

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(record.source, selected = true)
                StatusChip(record.bankName)
            }
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                text = "返回",
                primary = false,
                onClick = onBack
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = record.title.ifBlank { "学习记录" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        Text("题量：${record.total} 题", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("进入时间：${formatRecordTime(startedAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("完成时间：${formatRecordTime(record.timestamp)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("用时：${record.durationSeconds?.let(::formatDuration) ?: "未记录"}${if (record.autoSubmitted) " · 自动交卷" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("正确：${record.correct} 题 · 错误：$wrong 题 · 正确率：$accuracy%", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (record.totalScore != null && record.earnedScore != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "得分：${record.earnedScore.trimScore()} / ${record.totalScore.trimScore()} 分",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuestionResultCard(
    index: Int,
    result: StudyQuestionResult
) {
    val question = result.question
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip("第 $index 题", selected = true)
                StatusChip(typeLabelForRecord(question.type))
            }
            StatusChip(if (result.correct) "正确" else "错误", selected = result.correct)
        }
        result.sourceBankName?.takeIf { it.isNotBlank() }?.let { sourceBankName ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "来源：$sourceBankName",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = question.question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (question.images.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            QuestionImagesBlock(question.images, maxPreviewHeight = 260.dp, showMeta = false)
        }
        if (question.options.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            question.options.forEach { option ->
                Text(
                    text = "${option.key}. ${option.text}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                imageVector = if (result.correct) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                contentDescription = null,
                tint = if (result.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                text = if (result.correct) "本题回答正确" else "本题回答错误",
                color = if (result.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        val structuredBlank = MultiBlankSupport.hasStructuredAnswers(question)
        val shownUserBlankAnswers = result.userBlankAnswers.ifEmpty {
            if (structuredBlank) result.userAnswer else emptyList()
        }
        Text(
            text = if (structuredBlank) {
                "你的答案：\n${MultiBlankSupport.userAnswerText(shownUserBlankAnswers)}"
            } else {
                "你的答案：${result.userAnswer.joinToString(" / ").ifBlank { "未作答" }}"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (structuredBlank) {
                "正确答案：\n${result.answerText.ifBlank { MultiBlankSupport.expectedAnswerText(question.blankAnswers) }}"
            } else {
                "正确答案：${result.answerText.ifBlank { question.answer.joinToString(" / ").ifBlank { "未识别答案" } }}"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (result.maxScore != null && result.earnedScore != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "得分：${result.earnedScore.trimScore()} / ${result.maxScore.trimScore()} 分",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (question.analysis.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "解析：${question.analysis}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun typeLabelForRecord(type: QuestionType): String = when (type) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}
