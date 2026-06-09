package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.IllustrationHeroCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
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
    onOpenFavorites: () -> Unit,
    onOpenRecords: () -> Unit
) {
    val activeBank = QuizRepository.activeBank()
    val bankCount = QuizRepository.banks.size
    val bankName = activeBank?.name ?: "尚未导入题库"
    val todayPracticeCount = QuizRepository.studyRecords
        .filter { record ->
            record.source in listOf("练习", "错题练习", "今日复习", "收藏练习") && isToday(record.timestamp)
        }
        .sumOf { record -> record.total }
    val smartReviewEnabled = QuizRepository.wrongBookSmartReviewEnabled
    val pendingReviewCount = if (smartReviewEnabled) {
        QuizRepository.todayWrongBookSmartReviewCount()
    } else {
        QuizRepository.wrongBookActiveCount()
    }
    val pendingReviewTitle = if (smartReviewEnabled) "今日待复习" else "待复习"
    val homeSectionGap = ShirohaSpacing.Lg

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val shortcutGridMinHeight = 224.dp * density.fontScale.coerceIn(1f, 1.2f)
        var compactHomeLayout by remember(maxHeight, shortcutGridMinHeight) { mutableStateOf(false) }
        val homeScrollState = rememberScrollState()
        val homeContentModifier = if (compactHomeLayout) {
            Modifier
                .fillMaxSize()
                .verticalScroll(homeScrollState)
        } else {
            Modifier.fillMaxSize()
        }
        Column(
            modifier = homeContentModifier
                .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm)
        ) {
            ShirohaHeader(
                kicker = "Shiroha Quiz",
                title = "首页",
                subtitle = ""
            )

            Spacer(Modifier.height(homeSectionGap))

            IllustrationHeroCard(
                title = "欢迎回来",
                subtitle = "继续练习、考试或查看学习记录。",
                imageRes = R.drawable.illus_home_welcome,
                modifier = Modifier.height(ShirohaDimens.HeroCardHeight),
                imageSize = ShirohaDimens.HeroImageSize
            )

            Spacer(Modifier.height(homeSectionGap))

            TodayStatusCard(
                bankName = bankName,
                todayPracticeCount = todayPracticeCount,
                pendingReviewTitle = pendingReviewTitle,
                pendingReviewCount = pendingReviewCount,
                onGoPractice = onGoPractice,
                onGoExam = onGoExam
            )

            Spacer(Modifier.height(homeSectionGap))

            HomeShortcutGrid(
                bankCount = bankCount,
                favoriteCount = QuizRepository.favoriteQuestions.size,
                wrongCount = QuizRepository.wrongBookEntriesForCurrentScope().size,
                recordCount = QuizRepository.studyRecords.size,
                onOpenBankList = onOpenBankList,
                onOpenWrongBook = onOpenWrongBook,
                onOpenFavorites = onOpenFavorites,
                onOpenRecords = onOpenRecords,
                modifier = if (compactHomeLayout) {
                    Modifier
                        .fillMaxWidth()
                        .height(shortcutGridMinHeight)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = homeSectionGap)
                        .onSizeChanged { size ->
                            val gridHeight = with(density) { size.height.toDp() }
                            if (gridHeight < shortcutGridMinHeight) {
                                compactHomeLayout = true
                            }
                        }
                }
            )

            if (compactHomeLayout) {
                Spacer(
                    Modifier
                        .height(homeSectionGap)
                        .navigationBarsPadding()
                )
            }
        }
    }
}

@Composable
private fun TodayStatusCard(
    bankName: String,
    todayPracticeCount: Int,
    pendingReviewTitle: String,
    pendingReviewCount: Int,
    onGoPractice: () -> Unit,
    onGoExam: () -> Unit
) {
    val currentBankHeight = 60.dp
    val metricCardHeight = 56.dp
    val actionButtonHeight = 46.dp

    GlassCard(contentPadding = ShirohaSpacing.Lg) {
        Text(
            text = "今日学习状态",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        MiniStatusCard(
            title = "当前题库",
            value = bankName,
            modifier = Modifier
                .fillMaxWidth()
                .height(currentBankHeight)
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStatusCard(
                title = "今日练题",
                value = "${todayPracticeCount} 题",
                modifier = Modifier
                    .weight(1f)
                    .height(metricCardHeight)
            )
            MiniStatusCard(
                title = pendingReviewTitle,
                value = "${pendingReviewCount} 题",
                modifier = Modifier
                    .weight(1f)
                    .height(metricCardHeight)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactHomeActionButton(
                icon = Icons.Rounded.PlayArrow,
                text = "继续练习",
                modifier = Modifier
                    .weight(1f)
                    .height(actionButtonHeight),
                onClick = onGoPractice
            )
            CompactHomeActionButton(
                icon = Icons.Rounded.Timer,
                text = "模拟考试",
                modifier = Modifier
                    .weight(1f)
                    .height(actionButtonHeight),
                onClick = onGoExam
            )
        }
    }
}

@Composable
private fun CompactHomeActionButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.shirohaNoRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = ShirohaColors.CardWhite86,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineStrong)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp)
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
        color = ShirohaColors.CardWhite62,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSoft)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
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
private fun HomeShortcutGrid(
    bankCount: Int,
    favoriteCount: Int,
    wrongCount: Int,
    recordCount: Int,
    onOpenBankList: () -> Unit,
    onOpenWrongBook: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeShortcutCard(
                icon = Icons.Rounded.Warning,
                label = "错题本",
                value = wrongCount.toString(),
                desc = "复习错题",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onOpenWrongBook
            )
            HomeShortcutCard(
                icon = Icons.Rounded.Star,
                label = "收藏夹",
                value = favoriteCount.toString(),
                desc = "查看收藏",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onOpenFavorites
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeShortcutCard(
                icon = Icons.Rounded.AutoStories,
                label = "题库管理",
                value = bankCount.toString(),
                desc = "管理题库",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onOpenBankList
            )
            HomeShortcutCard(
                icon = Icons.Rounded.Timer,
                label = "学习记录",
                value = recordCount.toString(),
                desc = "查看记录",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onOpenRecords
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
            .shirohaNoRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = ShirohaColors.CardWhite72,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSoft)
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
