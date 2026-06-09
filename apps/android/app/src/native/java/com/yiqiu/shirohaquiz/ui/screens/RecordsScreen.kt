package com.yiqiu.shirohaquiz.ui.screens

import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.state.StudyRecord
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.EmptyStateIllustration
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.IllustrationHeroCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class RecordListFilter(val label: String) {
    ALL("全部记录"),
    WRONG_ONLY("只看错题")
}

@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    onOpenRecord: (String) -> Unit = {}
) {
    val records = QuizRepository.studyRecords.toList()
    var filter by remember { mutableStateOf(RecordListFilter.ALL) }
    val filteredRecords = remember(records, filter) {
        when (filter) {
            RecordListFilter.ALL -> records
            RecordListFilter.WRONG_ONLY -> records.filter { record -> (record.total - record.correct) > 0 }
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
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
                    text = "返回首页",
                    primary = false,
                    onClick = onBack
                )
            }
            return
        }

        IllustrationHeroCard(
            title = "学习记录会在这里慢慢积累",
            subtitle = "练习和考试都会收录到这里。",
            imageRes = R.drawable.illus_rest_state_webp,
            imageSize = 88.dp
        )

        GlassCard {
            Text(
                text = "记录筛选",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecordListFilter.entries.forEach { item ->
                    ActionPillButton(
                        icon = if (item == RecordListFilter.WRONG_ONLY) Icons.Rounded.Close else Icons.Rounded.CheckCircle,
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
        }

        if (filteredRecords.isEmpty()) {
            GlassCard {
                Text(
                    text = "当前筛选下没有错题记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            filteredRecords.forEach { record ->
                RecordCard(
                    record = record,
                    onClick = { onOpenRecord(record.id) }
                )
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: StudyRecord,
    onClick: () -> Unit
) {
    val wrong = (record.total - record.correct).coerceAtLeast(0)
    val accuracy = if (record.total == 0) 0 else record.correct * 100 / record.total
    val finishTime = record.timestamp
    val isExam = record.source.contains("考试")
    val meaningfulTitle = meaningfulRecordTitle(record)
    val footerText = recordFooterText(record)

    GlassCard(modifier = Modifier.shirohaNoRippleClickable(onClick = onClick)) {
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
            Text(
                text = formatRecordTime(finishTime),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${record.total} 题 · 对 ${record.correct} · 错 $wrong",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
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
        Spacer(Modifier.height(6.dp))
        Text(
            text = footerText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
