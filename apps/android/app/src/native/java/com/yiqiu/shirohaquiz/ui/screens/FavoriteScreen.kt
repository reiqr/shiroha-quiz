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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.FavoriteQuestionEntry
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoriteScreen(
    onBack: () -> Unit,
    onGoPractice: () -> Unit
) {
    val favorites = QuizRepository.favoriteQuestions.toList()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Favorites",
            title = "收藏夹",
            subtitle = "集中查看和练习你主动标记的题目。"
        )

        if (favorites.isEmpty()) {
            GlassCard {
                NoticeCard("收藏夹还是空的。练习时点击题目右上角星标即可收藏。")
                Spacer(Modifier.height(12.dp))
                ActionPillButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    text = "返回首页",
                    primary = false,
                    modifier = Modifier.height(44.dp),
                    onClick = onBack
                )
            }
            return
        }

        GlassCard {
            Text(
                text = "收藏 ${favorites.size} 题",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "收藏夹由你手动标记，可用于集中回看和再次练习。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.PlayArrow,
                    text = "练习收藏题",
                    primary = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (QuizRepository.startFavoritePractice(favorites)) {
                            onGoPractice()
                        }
                    }
                )
                ActionPillButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    text = "返回",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = onBack
                )
            }
        }

        favorites.forEach { entry ->
            FavoriteQuestionPreview(entry)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteQuestionPreview(entry: FavoriteQuestionEntry) {
    GlassCard {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusChip(typeLabel(entry.question.type))
            StatusChip(entry.bankName)
            StatusChip("收藏 ${formatTimestamp(entry.favoritedAt)}")
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
        if (entry.question.analysis.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "解析：${entry.question.analysis}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionPillButton(
                icon = Icons.Rounded.Star,
                text = "已收藏",
                primary = true,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                fillWidthContent = true,
                onClick = {}
            )
            ActionPillButton(
                icon = Icons.Rounded.DeleteOutline,
                text = "取消收藏",
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                fillWidthContent = true,
                onClick = { QuizRepository.removeFavoriteQuestion(entry) }
            )
        }
    }
}

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
