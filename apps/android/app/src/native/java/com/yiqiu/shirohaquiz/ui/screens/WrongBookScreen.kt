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
import com.yiqiu.shirohaquiz.ui.components.IllustrationHeroCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaDangerConfirmDialog
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
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
    val wrongBook = QuizRepository.wrongBookEntriesForCurrentScope()
    val allWrongBook = QuizRepository.wrongBook.toList()
    val scopeLabel = QuizRepository.currentWrongBookScopeLabel()
    val scopeMode = QuizRepository.wrongBookScopeMode
    val activeBank = QuizRepository.activeBank()
    var filter by remember { mutableStateOf(WrongBookFilter.NOT_MASTERED) }
    var sort by remember { mutableStateOf(WrongBookSort.RECENT_WRONG) }
    var showClearWrongBookConfirm by remember { mutableStateOf(false) }
    val filteredEntries = remember(wrongBook, filter, sort) {
        wrongBook.filterBy(filter).sortBy(sort)
    }
    val reviewEntries = filteredEntries.filter { it.status != WrongStatus.MASTERED.label && !QuizRepository.isQuestionSlashed(it.bankId, it.question) }
    val notMasteredCount = wrongBook.count { it.status != WrongStatus.MASTERED.label }
    val masteredCount = wrongBook.count { it.status == WrongStatus.MASTERED.label }
    val smartReviewEnabled = QuizRepository.wrongBookSmartReviewEnabled
    val smartReviewSummary = QuizRepository.todayWrongBookSmartReviewSummary()
    val isCurrentBankScope = scopeMode == QuizRepository.WRONG_BOOK_SCOPE_CURRENT_BANK

    if (showClearWrongBookConfirm) {
        ShirohaDangerConfirmDialog(
            title = if (isCurrentBankScope) "确认清空当前题库错题？" else "确认清空全部错题？",
            message = if (isCurrentBankScope) {
                "这会移除当前题库错题记录，包括错题次数、掌握状态和复习统计。其他题库错题不会受影响。"
            } else {
                "这会移除全部题库的错题记录，包括错题次数、掌握状态和复习统计。操作不可撤销。"
            },
            confirmText = if (isCurrentBankScope) "清空当前题库" else "清空全部",
            onDismiss = { showClearWrongBookConfirm = false },
            onConfirm = {
                QuizRepository.clearWrongBookForCurrentScope()
                showClearWrongBookConfirm = false
            }
        )
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Wrong Book",
            title = "错题本",
            subtitle = ""
        )

        if (wrongBook.isEmpty()) {
            EmptyStateIllustration(
                title = if (allWrongBook.isEmpty()) "错题本还是空的" else "${scopeLabel}暂无错题",
                message = when {
                    allWrongBook.isEmpty() -> "继续练习或考试后，错题会自动进入这里。"
                    isCurrentBankScope && activeBank == null -> "请先在题库管理中设为当前题库，或到设置中切换为全部题库错题。"
                    isCurrentBankScope -> "当前题库没有错题。需要查看其他错题时，可到 我的 → 错题本 中切换为全部题库。"
                    else -> "当前范围下没有错题。"
                },
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

        IllustrationHeroCard(
            title = "错题需要慢慢消化。",
            subtitle = "筛错题，集中复盘",
            imageRes = R.drawable.illus_wrongbook_hint_webp,
            modifier = Modifier.height(ShirohaDimens.HeroCardHeight),
            imageSize = ShirohaDimens.HeroImageSize
        )

        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "错题 ${wrongBook.size} 条",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusChip(scopeLabel, selected = isCurrentBankScope)
                    }
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
                    onClick = { showClearWrongBookConfirm = true }
                )
            }

            if (smartReviewEnabled) {
                Spacer(Modifier.height(16.dp))
                WrongBookSmartReviewSection(
                    total = smartReviewSummary.total,
                    notMastered = smartReviewSummary.notMastered,
                    masteredReview = smartReviewSummary.masteredReview,
                    onStart = {
                        if (QuizRepository.startTodayWrongBookReview()) {
                            onGoPractice()
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "掌握筛选",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WrongBookFilter.entries.forEach { item ->
                    ActionPillButton(
                        icon = Icons.Rounded.CheckCircle,
                        text = item.label,
                        primary = filter == item,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        fillWidthContent = true,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WrongBookSort.entries.forEach { item ->
                    ActionPillButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = item.label,
                        primary = sort == item,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        fillWidthContent = true,
                        textMaxLines = 2,
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
                    text = if (smartReviewEnabled) "刷当前筛选" else if (isCurrentBankScope) "刷当前题库" else "刷错题",
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


@Composable
private fun WrongBookSmartReviewSection(
    total: Int,
    notMastered: Int,
    masteredReview: Int,
    onStart: () -> Unit
) {
    Text(
        text = "今日复习",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "未掌握 $notMastered · 已掌握回顾 $masteredReview",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(10.dp))
    ActionPillButton(
        icon = Icons.Rounded.PlayArrow,
        text = if (total > 0) "开始今日复习" else "今日暂无到期",
        primary = total > 0,
        modifier = Modifier
            .fillMaxWidth(0.58f)
            .height(44.dp),
        fillWidthContent = true,
        onClick = {
            if (total > 0) onStart()
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrongQuestionPreview(entry: WrongQuestionEntry) {
    var showRemoveConfirm by remember(entry.bankId, entry.question.id) { mutableStateOf(false) }

    if (showRemoveConfirm) {
        ShirohaDangerConfirmDialog(
            title = "确认移出这道错题？",
            message = "这会从错题本中移出本题，并清除这道题当前的错题复习状态。原题库中的题目不会被删除。",
            confirmText = "确认移出",
            onDismiss = { showRemoveConfirm = false },
            onConfirm = {
                QuizRepository.removeWrongQuestion(entry)
                showRemoveConfirm = false
            }
        )
    }

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
                onClick = { showRemoveConfirm = true }
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
