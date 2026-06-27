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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.DEFAULT_BANK_GROUP_NAME
import com.yiqiu.shirohaquiz.state.QuizBank
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.state.WrongQuestionEntry
import com.yiqiu.shirohaquiz.state.WrongStatus
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.EmptyStateIllustration
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.IllustrationHeroCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.ShirohaDangerConfirmDialog
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import java.text.SimpleDateFormat
import java.util.Calendar
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

private enum class WrongBookReviewCountMode(val label: String) {
    TEN("10题"),
    TWENTY("20题"),
    CUSTOM("自定义"),
    ALL("全部")
}

private const val WRONG_BOOK_PAGE_SCOPE_ALL = "all"
private const val WRONG_BOOK_PAGE_SCOPE_BANK_PREFIX = "bank:"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrongBookScreen(
    onBack: () -> Unit,
    onGoPractice: () -> Unit
) {
    val banks = QuizRepository.banks.toList()
    val allWrongBook = QuizRepository.wrongBook.toList()
    var selectedScopeKey by rememberSaveable { mutableStateOf(WRONG_BOOK_PAGE_SCOPE_ALL) }
    var showScopeDialog by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(WrongBookFilter.NOT_MASTERED) }
    var sort by remember { mutableStateOf(WrongBookSort.RECENT_WRONG) }
    var selectedTypes by remember { mutableStateOf(QuestionType.entries.toSet()) }
    var reviewCountMode by remember { mutableStateOf(WrongBookReviewCountMode.ALL) }
    var customReviewCount by remember { mutableStateOf(10) }
    var customReviewCountText by remember { mutableStateOf("10") }
    var showCustomReviewCountDialog by remember { mutableStateOf(false) }
    var showClearWrongBookConfirm by remember { mutableStateOf(false) }

    val selectedBankId = selectedScopeKey
        .takeIf { it.startsWith(WRONG_BOOK_PAGE_SCOPE_BANK_PREFIX) }
        ?.removePrefix(WRONG_BOOK_PAGE_SCOPE_BANK_PREFIX)
    val selectedBank = selectedBankId?.let { id -> banks.firstOrNull { it.id == id } }
    val effectiveScopeKey = if (selectedBankId == null || selectedBank != null) {
        selectedScopeKey
    } else {
        WRONG_BOOK_PAGE_SCOPE_ALL
    }
    val wrongBook = selectedBank?.let { bank ->
        allWrongBook.filter { it.bankId == bank.id }
    } ?: allWrongBook
    val scopeLabel = selectedBank?.name ?: "全部题库"
    val isSingleBankScope = selectedBank != null

    val advancedReviewSettingsEnabled = QuizRepository.wrongBookAdvancedReviewSettingsEnabled
    val masteryFilteredEntries = remember(wrongBook, filter) {
        wrongBook.filterBy(filter)
    }
    val availableTypes = remember(masteryFilteredEntries) {
        QuestionType.entries.filter { type -> masteryFilteredEntries.any { it.question.type == type } }
    }
    val filteredEntries = remember(masteryFilteredEntries, selectedTypes, sort, advancedReviewSettingsEnabled) {
        masteryFilteredEntries
            .filter { entry -> !advancedReviewSettingsEnabled || entry.question.type in selectedTypes }
            .sortBy(sort)
    }
    val reviewCandidates = filteredEntries.filter {
        it.status != WrongStatus.MASTERED.label && !QuizRepository.isQuestionSlashed(it.bankId, it.question)
    }
    val selectedReviewCount = if (advancedReviewSettingsEnabled) {
        resolveWrongBookReviewCount(
            mode = reviewCountMode,
            customCount = customReviewCount,
            availableCount = reviewCandidates.size
        )
    } else {
        reviewCandidates.size
    }
    val reviewEntries = reviewCandidates.take(selectedReviewCount)
    val notMasteredCount = wrongBook.count { it.status != WrongStatus.MASTERED.label }
    val masteredCount = wrongBook.count { it.status == WrongStatus.MASTERED.label }
    val smartReviewEnabled = QuizRepository.wrongBookSmartReviewEnabled
    val smartReviewEntries = remember(wrongBook, smartReviewEnabled) {
        if (!smartReviewEnabled) {
            emptyList()
        } else {
            val now = System.currentTimeMillis()
            wrongBook
                .filterNot { QuizRepository.isQuestionSlashed(it.bankId, it.question) }
                .filter { entry -> isWrongEntryDueForPageReview(entry, now) }
                .sortedWith(
                    compareBy<WrongQuestionEntry> { if (it.status == WrongStatus.MASTERED.label) 1 else 0 }
                        .thenBy { it.nextReviewAt ?: 0L }
                        .thenByDescending { it.wrongCount }
                        .thenByDescending { it.lastWrongAt }
                )
        }
    }
    val smartReviewNotMastered = smartReviewEntries.count { it.status != WrongStatus.MASTERED.label }
    val smartReviewMastered = smartReviewEntries.count { it.status == WrongStatus.MASTERED.label }

    if (showClearWrongBookConfirm) {
        ShirohaDangerConfirmDialog(
            title = if (isSingleBankScope) "确认清空“${selectedBank?.name.orEmpty()}”错题？" else "确认清空全部错题？",
            message = if (isSingleBankScope) {
                "这会移除该题库的错题记录，包括错题次数、掌握状态和复习统计。其他题库错题不会受影响。"
            } else {
                "这会移除全部题库的错题记录，包括错题次数、掌握状态和复习统计。操作不可撤销。"
            },
            confirmText = if (isSingleBankScope) "清空当前范围" else "清空全部",
            onDismiss = { showClearWrongBookConfirm = false },
            onConfirm = {
                if (selectedBank == null) {
                    QuizRepository.clearWrongBook()
                } else {
                    wrongBook.toList().forEach(QuizRepository::removeWrongQuestion)
                }
                showClearWrongBookConfirm = false
            }
        )
    }

    if (showCustomReviewCountDialog) {
        WrongBookReviewCountDialog(
            value = customReviewCountText,
            maxCount = reviewCandidates.size.coerceAtLeast(1),
            onValueChange = { customReviewCountText = it },
            onDismiss = { showCustomReviewCountDialog = false },
            onConfirm = { count ->
                customReviewCount = count
                reviewCountMode = WrongBookReviewCountMode.CUSTOM
                showCustomReviewCountDialog = false
            }
        )
    }

    if (showScopeDialog) {
        WrongBookScopeDialog(
            banks = banks,
            wrongBook = allWrongBook,
            selectedScopeKey = effectiveScopeKey,
            onSelect = { key ->
                selectedScopeKey = key
                selectedTypes = QuestionType.entries.toSet()
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
            kicker = "Wrong Book",
            title = "错题本",
            subtitle = ""
        )

        if (allWrongBook.isEmpty()) {
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
        }

        if (allWrongBook.isNotEmpty()) {
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
                        StatusChip(if (isSingleBankScope) "单题库" else "全部题库", selected = isSingleBankScope)
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
                    enabled = wrongBook.isNotEmpty(),
                    modifier = Modifier.height(42.dp),
                    onClick = { showClearWrongBookConfirm = true }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "错题范围",
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
                            text = scopeLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedBank?.let { bank ->
                                val groupName = bank.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME }
                                "$groupName · 错题 ${wrongBook.size} 条"
                            } ?: "当前显示全部题库的 ${allWrongBook.size} 道错题",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "选择错题范围",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (wrongBook.isEmpty()) {
                Spacer(Modifier.height(14.dp))
                NoticeCard("当前题库暂无错题。可以切换到其他题库或全部题库。")
            } else {
                if (smartReviewEnabled) {
                    Spacer(Modifier.height(16.dp))
                    WrongBookSmartReviewSection(
                        total = smartReviewEntries.size,
                        notMastered = smartReviewNotMastered,
                        masteredReview = smartReviewMastered,
                        onStart = {
                            if (
                                smartReviewEntries.isNotEmpty() &&
                                QuizRepository.startWrongBookPractice(
                                    entries = smartReviewEntries,
                                    includeMastered = true,
                                    sourceLabel = "今日复习"
                                )
                            ) {
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

                if (advancedReviewSettingsEnabled) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "题型",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    if (availableTypes.isEmpty()) {
                        NoticeCard("当前掌握筛选下没有可选择的题型。")
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val allAvailableSelected = availableTypes.all { it in selectedTypes }
                            ActionPillButton(
                                icon = Icons.Rounded.CheckCircle,
                                text = "全部题型",
                                primary = allAvailableSelected,
                                modifier = Modifier.height(42.dp),
                                onClick = { selectedTypes = QuestionType.entries.toSet() }
                            )
                            availableTypes.forEach { type ->
                                val count = masteryFilteredEntries.count { it.question.type == type }
                                ActionPillButton(
                                    icon = Icons.Rounded.CheckCircle,
                                    text = "${typeLabel(type)} $count",
                                    primary = type in selectedTypes,
                                    modifier = Modifier.height(42.dp),
                                    onClick = {
                                        selectedTypes = if (type in selectedTypes) {
                                            selectedTypes - type
                                        } else {
                                            selectedTypes + type
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "单次复习数量",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "当前可复习 ${reviewCandidates.size} 题",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WrongBookReviewCountMode.entries.forEach { item ->
                            val label = when (item) {
                                WrongBookReviewCountMode.CUSTOM -> {
                                    if (reviewCountMode == WrongBookReviewCountMode.CUSTOM) "自定义 ${selectedReviewCount}题" else item.label
                                }
                                WrongBookReviewCountMode.ALL -> "全部 ${reviewCandidates.size}题"
                                else -> item.label
                            }
                            ActionPillButton(
                                icon = Icons.Rounded.PlayArrow,
                                text = label,
                                primary = reviewCountMode == item,
                                enabled = reviewCandidates.isNotEmpty(),
                                modifier = Modifier.height(42.dp),
                                onClick = {
                                    if (item == WrongBookReviewCountMode.CUSTOM) {
                                        customReviewCountText = customReviewCount
                                            .coerceIn(1, reviewCandidates.size.coerceAtLeast(1))
                                            .toString()
                                        showCustomReviewCountDialog = true
                                    } else {
                                        reviewCountMode = item
                                    }
                                }
                            )
                        }
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
                        text = when {
                            reviewEntries.isEmpty() -> "暂无可复习题目"
                            advancedReviewSettingsEnabled -> "开始复习 ${reviewEntries.size} 题"
                            else -> "刷错题"
                        },
                        primary = reviewEntries.isNotEmpty(),
                        enabled = reviewEntries.isNotEmpty(),
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
                        text = when {
                            advancedReviewSettingsEnabled && selectedTypes.none { it in availableTypes } -> "请至少选择一种有错题的题型。"
                            filter == WrongBookFilter.MASTERED -> "已掌握题不会进入手动复习。需要复习时可先标为未掌握。"
                            else -> "当前筛选下没有需要复习的错题。"
                        }
                    )
                }
            }
        }

            }
            }
        }

        if (wrongBook.isNotEmpty()) {
            if (filteredEntries.isEmpty()) {
                item {
                    GlassCard { NoticeCard("当前筛选下没有错题。") }
                }
            } else {
                items(
                    items = filteredEntries,
                    key = { entry -> "${entry.bankId}#${entry.question.id}" }
                ) { entry ->
                    WrongQuestionPreview(entry)
                }
            }
        }
    }
}

@Composable
private fun WrongBookScopeDialog(
    banks: List<QuizBank>,
    wrongBook: List<WrongQuestionEntry>,
    selectedScopeKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val wrongCountByBank = wrongBook.groupingBy { it.bankId }.eachCount()
    val groupedBanks = banks
        .groupBy { it.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME } }
        .entries
        .sortedBy { entry -> if (entry.key == DEFAULT_BANK_GROUP_NAME) "" else entry.key }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择错题范围") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WrongBookScopeOption(
                    title = "全部题库",
                    desc = "共 ${wrongBook.size} 道错题",
                    selected = selectedScopeKey == WRONG_BOOK_PAGE_SCOPE_ALL,
                    onClick = { onSelect(WRONG_BOOK_PAGE_SCOPE_ALL) }
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
                        val key = WRONG_BOOK_PAGE_SCOPE_BANK_PREFIX + bank.id
                        WrongBookScopeOption(
                            title = bank.name,
                            desc = "错题 ${wrongCountByBank[bank.id] ?: 0} 题 · 共 ${bank.questions.size} 题",
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
private fun WrongBookScopeOption(
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

@Composable
private fun WrongBookReviewCountDialog(
    value: String,
    maxCount: Int,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义单次复习数量") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "请输入 1～$maxCount 之间的题数。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() }.take(4)) },
                    singleLine = true,
                    label = { Text("复习题量") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val count = value.toIntOrNull()?.coerceIn(1, maxCount) ?: 1
                    onConfirm(count)
                }
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
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
            text = wrongQuestionDisplayTitle(entry),
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
            text = if (MultiBlankSupport.hasStructuredAnswers(entry.question)) {
                "正确答案：\n${MultiBlankSupport.expectedAnswerText(entry.question.blankAnswers)}"
            } else {
                "正确答案：${entry.question.answer.joinToString(" / ").ifBlank { "未识别答案" }}"
            },
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

private fun wrongQuestionDisplayTitle(entry: WrongQuestionEntry): String {
    val number = entry.question.number.trim()
    val stem = entry.question.question.trim()
    val safeNumber = number.takeUnless(::isSuspiciousWrongQuestionNumber)

    return when {
        safeNumber.isNullOrBlank() -> stem
        stem.isBlank() -> safeNumber
        else -> "$safeNumber. $stem"
    }
}

private fun isSuspiciousWrongQuestionNumber(number: String): Boolean {
    if (number.isBlank()) return false
    val compact = number.replace(Regex("""\s+"""), "")
    if (compact.length > 24) return true

    val numericParts = Regex("""\d+""").findAll(compact).count()
    val dashCount = compact.count { it == '-' || it == '－' || it == '—' || it == '–' }
    return numericParts >= 4 && dashCount >= 3
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

private fun resolveWrongBookReviewCount(
    mode: WrongBookReviewCountMode,
    customCount: Int,
    availableCount: Int
): Int {
    if (availableCount <= 0) return 0
    return when (mode) {
        WrongBookReviewCountMode.TEN -> 10.coerceAtMost(availableCount)
        WrongBookReviewCountMode.TWENTY -> 20.coerceAtMost(availableCount)
        WrongBookReviewCountMode.CUSTOM -> customCount.coerceIn(1, availableCount)
        WrongBookReviewCountMode.ALL -> availableCount
    }
}

private fun isWrongEntryDueForPageReview(entry: WrongQuestionEntry, now: Long): Boolean {
    val dueAt = entry.nextReviewAt ?: when (entry.status) {
        WrongStatus.MASTERED.label -> return false
        else -> startOfPageDay(now)
    }
    return dueAt <= now
}

private fun startOfPageDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
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
