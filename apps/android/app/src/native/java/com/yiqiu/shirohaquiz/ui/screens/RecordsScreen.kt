package com.yiqiu.shirohaquiz.ui.screens

import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.state.DEFAULT_BANK_GROUP_NAME
import com.yiqiu.shirohaquiz.state.QuizBank
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.state.StudyRecord
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.EmptyStateIllustration
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.IllustrationHeroCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaDangerConfirmDialog
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val RECORD_SCOPE_ALL = "all"
private const val RECORD_SCOPE_BANK_PREFIX = "bank:"

@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    onOpenRecord: (String) -> Unit = {}
) {
    val records = QuizRepository.studyRecords.toList()
    val banks = QuizRepository.banks.toList()
    var selectedScopeKey by rememberSaveable { mutableStateOf(RECORD_SCOPE_ALL) }
    var showScopeDialog by remember { mutableStateOf(false) }
    var pendingDeleteRecord by remember { mutableStateOf<StudyRecord?>(null) }

    val selectedBankId = selectedScopeKey
        .takeIf { it.startsWith(RECORD_SCOPE_BANK_PREFIX) }
        ?.removePrefix(RECORD_SCOPE_BANK_PREFIX)
    val selectedBank = selectedBankId?.let { id -> banks.firstOrNull { it.id == id } }
    val effectiveScopeKey = if (selectedBankId == null || selectedBank != null) {
        selectedScopeKey
    } else {
        RECORD_SCOPE_ALL
    }
    val bankNameCounts = banks
        .map { it.name.trim() }
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
    val visibleRecords = selectedBank?.let { bank ->
        val bankNameIsUnique = bankNameCounts[bank.name.trim()] == 1
        records.filter { record -> recordMatchesBank(record, bank, bankNameIsUnique) }
    } ?: records

    if (showScopeDialog) {
        RecordScopeDialog(
            banks = banks,
            records = records,
            selectedScopeKey = effectiveScopeKey,
            bankNameCounts = bankNameCounts,
            onSelect = { key ->
                selectedScopeKey = key
                showScopeDialog = false
            },
            onDismiss = { showScopeDialog = false }
        )
    }

    pendingDeleteRecord?.let { targetRecord ->
        ShirohaDangerConfirmDialog(
            title = "删除这条学习记录？",
            message = "删除后无法恢复，但不会影响题库、错题本和收藏夹。",
            confirmText = "删除",
            onDismiss = { pendingDeleteRecord = null },
            onConfirm = {
                QuizRepository.deleteStudyRecord(targetRecord.id)
                pendingDeleteRecord = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
            ) {
        ShirohaHeader(
            kicker = "Records",
            title = "学习记录",
            subtitle = ""
        )

        if (records.isEmpty()) {
            EmptyStateIllustration(
                title = "这里还没有学习记录",
                message = "完成练习或考试后，记录会自动出现在这里。",
                imageRes = R.drawable.illus_rest_state_webp,
                action = { Spacer(Modifier.height(12.dp)) }
            )
            GlassCard {
                ActionPillButton(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    text = "返回",
                    primary = false,
                    onClick = onBack
                )
            }
        }

        if (records.isNotEmpty()) {
            IllustrationHeroCard(
            title = "学习记录会在这里慢慢积累",
            subtitle = "练习和考试都会收录到这里。",
            imageRes = R.drawable.illus_rest_state_webp,
            imageSize = 88.dp
        )

        GlassCard {
            Text(
                text = "记录范围",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shirohaNoRippleClickable { showScopeDialog = true },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(ShirohaRadius.Md),
                color = ShirohaColors.CardWhite86,
                border = BorderStroke(1.dp, ShirohaColors.LineStrong)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedBank?.name ?: "全部记录",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedBank?.let { bank ->
                                val groupName = bank.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME }
                                "$groupName · 显示 ${visibleRecords.size} / ${records.size} 条"
                            } ?: "当前显示全部 ${records.size} 条学习记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "选择记录范围",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (selectedBank != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "包括属于该题库的记录，以及跨题库练习中包含该题库题目的记录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

            }
            }
        }

        if (records.isNotEmpty()) {
            if (visibleRecords.isEmpty()) {
                item {
                    GlassCard {
                        NoticeCard("该题库暂无学习记录。可以切换到其他题库或全部记录。")
                    }
                }
            } else {
                items(
                    items = visibleRecords,
                    key = { record -> record.id }
                ) { record ->
                    RecordCard(
                        record = record,
                        onClick = { onOpenRecord(record.id) },
                        onDelete = { pendingDeleteRecord = record }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordScopeDialog(
    banks: List<QuizBank>,
    records: List<StudyRecord>,
    selectedScopeKey: String,
    bankNameCounts: Map<String, Int>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val groupedBanks = banks
        .groupBy { it.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME } }
        .entries
        .sortedBy { entry -> if (entry.key == DEFAULT_BANK_GROUP_NAME) "" else entry.key }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择记录范围") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecordScopeOption(
                    title = "全部记录",
                    desc = "共 ${records.size} 条学习记录",
                    selected = selectedScopeKey == RECORD_SCOPE_ALL,
                    onClick = { onSelect(RECORD_SCOPE_ALL) }
                )
                groupedBanks.forEach { entry ->
                    Text(
                        text = entry.key,
                        style = MaterialTheme.typography.labelLarge,
                        color = ShirohaColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    entry.value.forEach { bank ->
                        val key = RECORD_SCOPE_BANK_PREFIX + bank.id
                        val bankNameIsUnique = bankNameCounts[bank.name.trim()] == 1
                        val count = records.count { record ->
                            recordMatchesBank(record, bank, bankNameIsUnique)
                        }
                        RecordScopeOption(
                            title = bank.name,
                            desc = "$count 条相关记录 · 共 ${bank.questions.size} 题",
                            selected = selectedScopeKey == key,
                            onClick = { onSelect(key) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun RecordScopeOption(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shirohaNoRippleClickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ShirohaRadius.Md),
        color = if (selected) ShirohaColors.BrandPrimarySoft else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun recordMatchesBank(
    record: StudyRecord,
    bank: QuizBank,
    bankNameIsUnique: Boolean
): Boolean {
    if (record.bankId == bank.id) return true
    if (record.questionResults.any { it.sourceBankId == bank.id }) return true

    if (!bankNameIsUnique) return false

    val bankName = bank.name.trim()
    if (bankName.isBlank()) return false
    if (record.bankId.isNullOrBlank() && record.bankName.trim() == bankName) return true
    return record.questionResults.any { result ->
        result.sourceBankId.isNullOrBlank() && result.sourceBankName?.trim() == bankName
    }
}

@Composable
private fun RecordCard(
    record: StudyRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val wrong = (record.total - record.correct).coerceAtLeast(0)
    val accuracy = if (record.total == 0) 0 else record.correct * 100 / record.total
    val finishTime = record.timestamp
    val isExam = record.source.contains("考试")
    val meaningfulTitle = meaningfulRecordTitle(record)
    val footerText = recordFooterText(record)

    GlassCard(
        modifier = Modifier.shirohaNoRippleClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(record.source, selected = true)
                Text(
                    text = record.bankName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatRecordTime(finishTime),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "删除记录",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (meaningfulTitle != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = meaningfulTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(10.dp))
        }

        Text(
            text = "${record.total} 题 · 对 ${record.correct} · 错 $wrong",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = footerText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Text(
                text = if (isExam && record.totalScore != null && record.earnedScore != null) {
                    "${record.earnedScore.trimScore()} / ${record.totalScore.trimScore()} 分"
                } else {
                    "正确率 $accuracy%"
                },
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}

internal fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

internal fun formatRecordTime(timestamp: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun meaningfulRecordTitle(record: StudyRecord): String? {
    val title = record.title.trim()
    if (title.isBlank()) return null
    val bankName = record.bankName.trim()
    val placeholders = setOf(
        "当前题库",
        "原生考试",
        "练习记录",
        "考试记录",
        "当前练习",
        "当前考试"
    )
    if (title in placeholders) return null
    if (bankName.isNotBlank() && title == bankName) return null
    return title
}

private fun recordFooterText(record: StudyRecord): String {
    val duration = record.durationSeconds?.let { "用时 ${formatDuration(it)}" }
    val detail = if (record.questionResults.isNotEmpty()) "点击查看详情" else "仅保留摘要"
    return listOfNotNull(duration, detail).joinToString(" · ")
}

internal fun Double.trimScore(): String {
    return if (this % 1.0 == 0.0) this.toInt().toString() else "%.1f".format(this)
}
