package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.DEFAULT_BANK_GROUP_NAME
import com.yiqiu.shirohaquiz.state.FavoriteQuestionEntry
import com.yiqiu.shirohaquiz.state.QuizBank
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FAVORITE_SCOPE_ALL = "all"
private const val FAVORITE_SCOPE_BANK_PREFIX = "bank:"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoriteScreen(
    onBack: () -> Unit,
    onGoPractice: () -> Unit
) {
    val banks = QuizRepository.banks.toList()
    val favorites = QuizRepository.favoriteQuestions.toList()
    var selectedScopeKey by rememberSaveable { mutableStateOf(FAVORITE_SCOPE_ALL) }
    var showScopeDialog by remember { mutableStateOf(false) }

    val selectedBankId = selectedScopeKey
        .takeIf { it.startsWith(FAVORITE_SCOPE_BANK_PREFIX) }
        ?.removePrefix(FAVORITE_SCOPE_BANK_PREFIX)
    val selectedBank = selectedBankId?.let { id -> banks.firstOrNull { it.id == id } }
    val effectiveScopeKey = if (selectedBankId == null || selectedBank != null) {
        selectedScopeKey
    } else {
        FAVORITE_SCOPE_ALL
    }
    val visibleFavorites = selectedBank?.let { bank ->
        favorites.filter { it.bankId == bank.id }
    } ?: favorites

    if (showScopeDialog) {
        FavoriteScopeDialog(
            banks = banks,
            favorites = favorites,
            selectedScopeKey = effectiveScopeKey,
            onSelect = { key ->
                selectedScopeKey = key
                showScopeDialog = false
            },
            onDismiss = { showScopeDialog = false }
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
        }

        if (favorites.isNotEmpty()) {
            GlassCard {
            Text(
                text = if (selectedBank == null) {
                    "收藏 ${favorites.size} 题"
                } else {
                    "收藏 ${visibleFavorites.size} / ${favorites.size} 题"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "收藏数据统一保存，可按全部题库或单个题库查看和练习。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "收藏范围",
                style = MaterialTheme.typography.labelLarge,
                color = ShirohaColors.TextSecondary,
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
                            text = selectedBank?.name ?: "全部题库",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedBank?.let { bank ->
                                val groupName = bank.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME }
                                "$groupName · 收藏 ${visibleFavorites.size} 题"
                            } ?: "当前显示全部题库的 ${favorites.size} 道收藏",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "选择收藏范围",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.PlayArrow,
                    text = if (selectedBank == null) "练习收藏题" else "练习当前 ${visibleFavorites.size} 题",
                    primary = true,
                    enabled = visibleFavorites.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (QuizRepository.startFavoritePractice(visibleFavorites)) {
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

            }
            }
        }

        if (favorites.isNotEmpty()) {
            if (visibleFavorites.isEmpty()) {
                item {
                    GlassCard {
                        NoticeCard("当前题库暂无收藏题。可以切换到其他题库或全部题库。")
                    }
                }
            } else {
                items(
                    items = visibleFavorites,
                    key = { entry -> "${entry.bankId}#${entry.question.id}" }
                ) { entry ->
                    FavoriteQuestionPreview(entry)
                }
            }
        }
    }
}

@Composable
private fun FavoriteScopeDialog(
    banks: List<QuizBank>,
    favorites: List<FavoriteQuestionEntry>,
    selectedScopeKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val favoriteCountByBank = favorites.groupingBy { it.bankId }.eachCount()
    val groupedBanks = banks
        .groupBy { it.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME } }
        .entries
        .sortedBy { entry -> if (entry.key == DEFAULT_BANK_GROUP_NAME) "" else entry.key }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择收藏范围") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FavoriteScopeOption(
                    title = "全部题库",
                    desc = "共 ${favorites.size} 道收藏",
                    selected = selectedScopeKey == FAVORITE_SCOPE_ALL,
                    onClick = { onSelect(FAVORITE_SCOPE_ALL) }
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
                        val key = FAVORITE_SCOPE_BANK_PREFIX + bank.id
                        FavoriteScopeOption(
                            title = bank.name,
                            desc = "收藏 ${favoriteCountByBank[bank.id] ?: 0} 题 · 共 ${bank.questions.size} 题",
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
private fun FavoriteScopeOption(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteQuestionPreview(entry: FavoriteQuestionEntry) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(typeLabel(entry.question.type))
                StatusChip(entry.bankName)
                StatusChip("收藏 ${formatTimestamp(entry.favoritedAt)}")
            }
            IconButton(onClick = { QuizRepository.removeFavoriteQuestion(entry) }) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "取消收藏",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = favoriteQuestionDisplayTitle(entry),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (entry.question.images.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            QuestionImagesBlock(
                images = entry.question.images,
                maxPreviewHeight = 260.dp,
                showMeta = false
            )
        }
        if (entry.question.options.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            entry.question.options.forEach { option ->
                Text(
                    text = "${option.key}. ${option.text}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
            }
        }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun favoriteQuestionDisplayTitle(entry: FavoriteQuestionEntry): String {
    val number = entry.question.number.trim()
    val stem = entry.question.question.trim()
    val safeNumber = number.takeUnless(::isSuspiciousFavoriteQuestionNumber)

    return when {
        safeNumber.isNullOrBlank() -> stem
        stem.isBlank() -> safeNumber
        else -> "$safeNumber. $stem"
    }
}

private fun isSuspiciousFavoriteQuestionNumber(number: String): Boolean {
    if (number.isBlank()) return false
    val compact = number.replace(Regex("""\s+"""), "")
    if (compact.length > 24) return true

    val numericParts = Regex("""\d+""").findAll(compact).count()
    val dashCount = compact.count { it == '-' || it == '－' || it == '—' || it == '–' }
    return numericParts >= 4 && dashCount >= 3
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
