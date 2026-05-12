package com.yiqiu.shirohaquiz.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
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
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.state.WrongQuestionEntry
import com.yiqiu.shirohaquiz.state.WrongStatus
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.EmptyStateIllustration
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WrongBookFilter(val label: String) {
    NOT_MASTERED("未掌握"),
    MASTERED("已掌握"),
    ALL("全部")
}

private enum class WrongBookSort(val label: String) {
    RECENT_WRONG("最近错"),
    WRONG_COUNT("错误次数"),
    MASTERY("掌握程度")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrongBookScreen(
    onBack: () -> Unit,
    onGoPractice: () -> Unit
) {
    val wrongBook = QuizRepository.wrongBook.toList()
    var filter by remember { mutableStateOf(WrongBookFilter.NOT_MASTERED) }
    var sort by remember { mutableStateOf(WrongBookSort.RECENT_WRONG) }
    val filteredEntries = remember(wrongBook, filter, sort) {
        wrongBook.filterBy(filter).sortBy(sort)
    }
    val reviewEntries = filteredEntries.filter { it.status != WrongStatus.MASTERED.label }
    val notMasteredCount = wrongBook.count { it.status != WrongStatus.MASTERED.label }
    val masteredCount = wrongBook.count { it.status == WrongStatus.MASTERED.label }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Wrong Book",
            title = "错题本",
            subtitle = "按掌握程度沉淀错题，再集中复习。"
        )

        if (wrongBook.isEmpty()) {
            EmptyStateIllustration(
                title = "错题本还是空的",
                message = "继续练习或考试后，错题会自动进入这里。",
                imageRes = R.drawable.illus_wrongbook_hint_webp,
                action = {
                    Spacer(Modifier.height(12.dp))
                    ActionPillButton(
                        icon = Icons.AutoMirrored.Rounded.Undo,
                        text = "返回首页",
                        primary = false,
                        onClick = onBack
                    )
                }
            )
            return
        }

        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "错题 ${wrongBook.size} 条",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "未掌握 $notMasteredCount · 已掌握 $masteredCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ActionPillButton(
                    icon = Icons.Rounded.DeleteOutline,
                    text = "清空",
                    primary = false,
                    modifier = Modifier.height(42.dp),
                    onClick = { QuizRepository.clearWrongBook() }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "掌握筛选",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WrongBookFilter.entries.forEach { item ->
                    ActionPillButton(
                        icon = Icons.Rounded.CheckCircle,
                        text = item.label,
                        primary = filter == item,
                        onClick = { filter = item }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "排序",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WrongBookSort.entries.forEach { item ->
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = item.label,
                        primary = sort == item,
                        onClick = { sort = item }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.PlayArrow,
                    text = "刷错题",
                    primary = reviewEntries.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (reviewEntries.isNotEmpty() && QuizRepository.startWrongBookPractice(reviewEntries)) {
                            onGoPractice()
                        }
                    }
                )
                ActionPillButton(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    text = "返回",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = onBack
                )
            }

            if (reviewEntries.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                NoticeCard(
                    text = if (filter == WrongBookFilter.MASTERED) {
                        "已掌握题不会进入刷错题。需要复习时可先标为未掌握。"
                    } else {
                        "当前筛选下没有需要复习的错题。"
                    }
                )
            }
        }

        if (filteredEntries.isEmpty()) {
            GlassCard { NoticeCard("当前筛选下没有错题。") }
        } else {
            filteredEntries.forEach { entry ->
                WrongQuestionPreview(entry)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrongQuestionPreview(entry: WrongQuestionEntry) {
    GlassCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(displayWrongStatus(entry.status), selected = entry.status != WrongStatus.MASTERED.label)
            StatusChip(typeLabel(entry.question.type))
            StatusChip(entry.bankName)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "${entry.question.number}. ${entry.question.question}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "正确答案：${entry.question.answer.joinToString(" / ").ifBlank { "未识别答案" }}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "上次答案：${entry.lastAnswer.joinToString(" / ").ifBlank { "未作答" }}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "错 ${entry.wrongCount} 次 · 对 ${entry.rightCount} 次 · 最近错误 ${formatTimestamp(entry.lastWrongAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionPillButton(
                icon = Icons.Rounded.CheckCircle,
                text = if (entry.status == WrongStatus.MASTERED.label) "重新复习" else "标记掌握",
                primary = entry.status != WrongStatus.MASTERED.label,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                fillWidthContent = true,
                onClick = {
                    QuizRepository.markWrongQuestionMastered(
                        entry = entry,
                        mastered = entry.status != WrongStatus.MASTERED.label
                    )
                }
            )
            ActionPillButton(
                icon = Icons.Rounded.DeleteOutline,
                text = "移出",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                fillWidthContent = true,
                onClick = { QuizRepository.removeWrongQuestion(entry) }
            )
        }
    }
}

private fun List<WrongQuestionEntry>.filterBy(filter: WrongBookFilter): List<WrongQuestionEntry> {
    return when (filter) {
        WrongBookFilter.NOT_MASTERED -> filter { it.status != WrongStatus.MASTERED.label }
        WrongBookFilter.MASTERED -> filter { it.status == WrongStatus.MASTERED.label }
        WrongBookFilter.ALL -> this
    }
}

private fun List<WrongQuestionEntry>.sortBy(sort: WrongBookSort): List<WrongQuestionEntry> {
    return when (sort) {
        WrongBookSort.RECENT_WRONG -> sortedByDescending { it.lastWrongAt }
        WrongBookSort.WRONG_COUNT -> sortedWith(compareByDescending<WrongQuestionEntry> { it.wrongCount }.thenByDescending { it.lastWrongAt })
        WrongBookSort.MASTERY -> sortedWith(compareBy<WrongQuestionEntry> { statusRank(it.status) }.thenByDescending { it.wrongCount })
    }
}

private fun statusRank(status: String): Int = when (status) {
    WrongStatus.MASTERED.label -> 1
    else -> 0
}

private fun displayWrongStatus(status: String): String =
    if (status == WrongStatus.MASTERED.label) "已掌握" else "未掌握"

private fun typeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
