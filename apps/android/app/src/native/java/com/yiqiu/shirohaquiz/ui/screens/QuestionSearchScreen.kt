package com.yiqiu.shirohaquiz.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.DEFAULT_BANK_GROUP_NAME
import com.yiqiu.shirohaquiz.state.QuestionSearchEngine
import com.yiqiu.shirohaquiz.state.QuestionSearchMatchedField
import com.yiqiu.shirohaquiz.state.QuestionSearchResult
import com.yiqiu.shirohaquiz.state.QuestionSearchScope
import com.yiqiu.shirohaquiz.state.QuizBank
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import com.yiqiu.shirohaquiz.ui.text.LatexDisplayFormatter
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import com.yiqiu.shirohaquiz.ui.util.bankDisplayPath

private const val SCOPE_ACTIVE = "active"
private const val SCOPE_ALL = "all"
private const val SCOPE_BANK_PREFIX = "bank:"

@Composable
fun QuestionSearchScreen(
    onBack: () -> Unit,
    onOpenBankDetail: (String) -> Unit
) {
    val banks = QuizRepository.banks
    val activeBank = QuizRepository.activeBank()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedScopeKey by rememberSaveable {
        mutableStateOf(if (activeBank != null) SCOPE_ACTIVE else SCOPE_ALL)
    }
    var showScopeDialog by remember { mutableStateOf(false) }
    var expandedResultKeys by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    val scope = selectedScopeKey.toSearchScope(activeBank?.id)
    val scopeLabel = selectedScopeKey.scopeLabel(banks, activeBank)
    val results = QuestionSearchEngine.search(
        banks = banks,
        activeBankId = activeBank?.id,
        query = query,
        scope = scope
    )

    if (showScopeDialog) {
        SearchScopeDialog(
            banks = banks,
            activeBank = activeBank,
            selectedScopeKey = selectedScopeKey,
            onSelect = { key ->
                selectedScopeKey = key
                showScopeDialog = false
            },
            onDismiss = { showScopeDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Search",
            title = "题目搜索",
            subtitle = "搜题干、选项、答案或解析。"
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            ActionPillButton(
                icon = Icons.Rounded.ArrowBack,
                text = "返回题库管理",
                primary = false,
                onClick = onBack
            )
        }

        GlassCard(contentPadding = 16.dp) {
            Text(
                text = "搜索范围",
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
                            text = "可切换当前题库、全部题库或指定题库",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "选择搜索范围",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "搜索"
                    )
                },
                placeholder = { Text("搜题干、选项、答案或解析") },
                singleLine = true
            )
        }

        when {
            query.isBlank() -> SearchEmptyState(
                title = "输入关键词开始搜索",
                desc = "可以输入题干片段、选项内容、答案或解析里的关键词。"
            )

            results.isEmpty() -> SearchEmptyState(
                title = "没有找到相关题目",
                desc = "可以换一个关键词，或切换到全部题库搜索。"
            )

            else -> {
                Text(
                    text = "共找到 ${results.size} 题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                results.forEach { result ->
                    val expanded = result.key in expandedResultKeys
                    QuestionSearchResultCard(
                        result = result,
                        expanded = expanded,
                        onToggleExpanded = {
                            expandedResultKeys = if (expanded) {
                                expandedResultKeys - result.key
                            } else {
                                (expandedResultKeys + result.key).distinct()
                            }
                        },
                        onOpenBankDetail = onOpenBankDetail
                    )
                }
            }
        }

        Spacer(Modifier.height(ShirohaSpacing.Xl))
    }
}

@Composable
private fun SearchScopeDialog(
    banks: List<QuizBank>,
    activeBank: QuizBank?,
    selectedScopeKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val groupedBanks = banks
        .groupBy { it.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME } }
        .entries
        .sortedBy { entry -> if (entry.key == DEFAULT_BANK_GROUP_NAME) "" else entry.key }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择搜索范围") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeBank?.let { bank ->
                    SearchScopeOption(
                        title = "当前题库：${bank.name}",
                        desc = bankDisplayPath(bank),
                        selected = selectedScopeKey == SCOPE_ACTIVE,
                        onClick = { onSelect(SCOPE_ACTIVE) }
                    )
                }
                SearchScopeOption(
                    title = "全部题库",
                    desc = "搜索所有分组和题库",
                    selected = selectedScopeKey == SCOPE_ALL,
                    onClick = { onSelect(SCOPE_ALL) }
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
                        val key = bank.scopeKey()
                        SearchScopeOption(
                            title = bank.name,
                            desc = "${bank.questions.size} 题",
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
private fun SearchScopeOption(
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
        border = BorderStroke(1.dp, if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft)
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
                if (desc.isNotBlank()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
private fun SearchEmptyState(title: String, desc: String) {
    GlassCard(contentPadding = 18.dp) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestionSearchResultCard(
    result: QuestionSearchResult,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onOpenBankDetail: (String) -> Unit
) {
    val question = result.question

    GlassCard(
        modifier = Modifier.shirohaNoRippleClickable(onClick = onToggleExpanded),
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${result.groupName} / ${result.bankName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ShirohaColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "第 ${result.questionIndex} 题 · ${question.type.label()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            StatusChip(if (expanded) "收起" else "展开", selected = expanded)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = LatexDisplayFormatter.format(question.question).ifBlank { "（空题干）" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "命中：${result.matchedFields.joinToString("、") { it.label }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        val answerText = if (MultiBlankSupport.hasStructuredAnswers(question)) {
            MultiBlankSupport.expectedAnswerText(question.blankAnswers)
        } else {
            question.answer.joinToString("、")
        }
        if (answerText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "答案：${LatexDisplayFormatter.format(answerText)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = ShirohaColors.LineSoft)
            Spacer(Modifier.height(12.dp))
            FullQuestionInfo(question)
            Spacer(Modifier.height(14.dp))
            ActionPillButton(
                icon = Icons.Rounded.Visibility,
                text = "查看所在题库",
                primary = false,
                modifier = Modifier.fillMaxWidth(),
                fillWidthContent = true,
                onClick = { onOpenBankDetail(result.bankId) }
            )
        } else if (question.analysis.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "解析：${LatexDisplayFormatter.format(question.analysis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FullQuestionInfo(question: Question) {
    SearchInfoBlock(label = "题干", text = question.question.ifBlank { "（空题干）" })
    if (question.images.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        QuestionImagesBlock(question.images, maxPreviewHeight = 260.dp, showMeta = true)
    }
    if (question.options.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "选项",
            style = MaterialTheme.typography.labelLarge,
            color = ShirohaColors.TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        question.options.forEach { option ->
            Text(
                text = "${option.key}. ${LatexDisplayFormatter.format(option.text)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
        }
    }
    SearchInfoBlock(
        label = "正确答案",
        text = if (MultiBlankSupport.hasStructuredAnswers(question)) {
            MultiBlankSupport.expectedAnswerText(question.blankAnswers)
        } else {
            question.answer.joinToString("、").ifBlank { "（未填写）" }
        }
    )
    if (question.analysis.isNotBlank()) {
        SearchInfoBlock(label = "解析", text = question.analysis)
    }
    if (question.category.isNotBlank()) {
        SearchInfoBlock(label = "分类", text = question.category)
    }
}

@Composable
private fun SearchInfoBlock(label: String, text: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = ShirohaColors.TextSecondary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = LatexDisplayFormatter.format(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

private fun String.toSearchScope(activeBankId: String?): QuestionSearchScope {
    return when {
        this == SCOPE_ACTIVE && activeBankId != null -> QuestionSearchScope.ActiveBank
        this == SCOPE_ALL -> QuestionSearchScope.AllBanks
        startsWith(SCOPE_BANK_PREFIX) -> QuestionSearchScope.Bank(removePrefix(SCOPE_BANK_PREFIX))
        else -> QuestionSearchScope.AllBanks
    }
}

private fun String.scopeLabel(banks: List<QuizBank>, activeBank: QuizBank?): String {
    return when {
        this == SCOPE_ACTIVE && activeBank != null -> "当前题库：${activeBank.name}"
        this == SCOPE_ALL -> "全部题库"
        startsWith(SCOPE_BANK_PREFIX) -> {
            val bankId = removePrefix(SCOPE_BANK_PREFIX)
            banks.firstOrNull { it.id == bankId }?.let { "指定题库：${it.name}" } ?: "指定题库"
        }
        else -> "全部题库"
    }
}

private fun QuizBank.scopeKey(): String = "$SCOPE_BANK_PREFIX$id"

private fun QuestionType.label(): String = when (this) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}
