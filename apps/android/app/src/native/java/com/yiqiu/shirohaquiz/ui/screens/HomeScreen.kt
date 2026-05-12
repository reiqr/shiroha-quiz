package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.IllustrationHeroCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import java.util.Calendar

@Composable
fun HomeScreen(
    onGoImport: () -> Unit,
    onGoPractice: () -> Unit,
    onGoExam: () -> Unit,
    onOpenBankList: () -> Unit,
    onOpenBankDetail: (String) -> Unit,
    onOpenWrongBook: () -> Unit,
    onOpenRecords: () -> Unit
) {
    val activeBank = QuizRepository.activeBank()
    val bankCount = QuizRepository.banks.size
    val questionCount = activeBank?.questions?.size ?: 0
    val bankName = activeBank?.name ?: "尚未导入题库"
    val todayPracticeCount = QuizRepository.studyRecords.count { record ->
        record.source == "练习" && isToday(record.timestamp)
    }
    val pendingReviewCount = QuizRepository.wrongBookActiveCount()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Shiroha Quiz",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )

        IllustrationHeroCard(
            title = "欢迎回来",
            subtitle = "继续练习、考试或查看学习记录。",
            imageRes = R.drawable.illus_home_welcome_v2,
            imageSize = 82.dp
        )

        TodayStatusCard(
            bankName = bankName,
            todayPracticeCount = todayPracticeCount,
            pendingReviewCount = pendingReviewCount,
            onGoPractice = onGoPractice,
            onGoExam = onGoExam
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeShortcutCard(
                icon = Icons.Rounded.AutoStories,
                label = "题库数量",
                value = bankCount.toString(),
                desc = "进入题库管理",
                modifier = Modifier.weight(1f),
                onClick = onOpenBankList
            )
            HomeShortcutCard(
                icon = Icons.Rounded.Description,
                label = "当前题量",
                value = questionCount.toString(),
                desc = "查看当前题库详情",
                modifier = Modifier.weight(1f),
                onClick = {
                    activeBank?.let { onOpenBankDetail(it.id) }
                }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeShortcutCard(
                icon = Icons.Rounded.Warning,
                label = "错题本",
                value = QuizRepository.wrongBook.size.toString(),
                desc = "打开错题本",
                modifier = Modifier.weight(1f),
                onClick = onOpenWrongBook
            )
            HomeShortcutCard(
                icon = Icons.Rounded.Timer,
                label = "学习记录",
                value = QuizRepository.studyRecords.size.toString(),
                desc = "查看学习记录",
                modifier = Modifier.weight(1f),
                onClick = onOpenRecords
            )
        }
    }
}

@Composable
private fun TodayStatusCard(
    bankName: String,
    todayPracticeCount: Int,
    pendingReviewCount: Int,
    onGoPractice: () -> Unit,
    onGoExam: () -> Unit
) {
    GlassCard {
        Text(
            text = "今日学习状态",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        MiniStatusCard(
            title = "当前题库",
            value = bankName,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStatusCard(
                title = "今日练习",
                value = "${todayPracticeCount} 次",
                modifier = Modifier.weight(1f)
            )
            MiniStatusCard(
                title = "待复习",
                value = "${pendingReviewCount} 题",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionPillButton(
                icon = Icons.Rounded.PlayArrow,
                text = "进入练习",
                primary = false,
                fillWidthContent = true,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                onClick = onGoPractice
            )
            ActionPillButton(
                icon = Icons.Rounded.Timer,
                text = "开始考试",
                primary = false,
                fillWidthContent = true,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                onClick = onGoExam
            )
        }
    }
}

@Composable
private fun MiniStatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, ShirohaColors.LineSoft)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeShortcutCard(
    icon: ImageVector,
    label: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, ShirohaColors.LineSoft)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun isToday(timestamp: Long): Boolean {
    if (timestamp <= 0L) return false
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}
