package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.BorderStroke
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.AiAnalysisFillPanel
import com.yiqiu.shirohaquiz.ui.components.EmptyStateIllustration
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.MultiBlankAnswerEditor
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.ShirohaDangerConfirmDialog
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BankReviewScreen(
    bankId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bank = QuizRepository.banks.firstOrNull { it.id == bankId } ?: QuizRepository.activeBank()

    if (bank == null) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
        ) {
            ShirohaHeader(
                kicker = "Review",
                title = "题库核对",
                subtitle = "没有找到需要核对的题库。"
            )
            EmptyStateIllustration(
                title = "题库不存在",
                message = "这份题库可能已经被删除或切换。",
                imageRes = R.drawable.illus_empty_state_webp
            )
            ActionPillButton(
                icon = Icons.Rounded.ArrowBack,
                text = "返回详情",
                primary = false,
                onClick = onBack
            )
        }
        return
    }

    val editableQuestions = remember(bank.id) { bank.questions.toMutableStateList() }
    var currentIndex by rememberSaveable(bank.id) { mutableStateOf(0) }
    var filterName by rememberSaveable(bank.id) { mutableStateOf(BankReviewFilter.ALL.name) }
    var query by rememberSaveable(bank.id) { mutableStateOf("") }
    var showRemoveImagesConfirm by rememberSaveable(bank.id) { mutableStateOf(false) }
    var showDeleteLastOptionConfirm by rememberSaveable(bank.id) { mutableStateOf(false) }
    var showDeleteQuestionConfirm by rememberSaveable(bank.id) { mutableStateOf(false) }
    val filter = bankReviewFilterFromName(filterName)

    val allIndices = editableQuestions.indices.toList()
    val filteredByType = editableQuestions.indices.filter { index ->
        bankQuestionMatchesFilter(editableQuestions[index], filter)
    }
    val searchText = query.trim()
    val visibleIndices = (if (filter == BankReviewFilter.ALL) allIndices else filteredByType).filter { index ->
        if (searchText.isBlank()) true else bankQuestionMatchesQuery(editableQuestions[index], searchText)
    }

    val safeIndex = when {
        editableQuestions.isEmpty() -> 0
        visibleIndices.isEmpty() -> currentIndex.coerceIn(0, editableQuestions.lastIndex)
        currentIndex in visibleIndices -> currentIndex
        else -> visibleIndices.first()
    }
    if (editableQuestions.isNotEmpty() && safeIndex != currentIndex) currentIndex = safeIndex

    if (showRemoveImagesConfirm && editableQuestions.isNotEmpty()) {
        ShirohaDangerConfirmDialog(
            title = "确认移除本题图片？",
            message = "这会从当前核对题目中移除已绑定图片。需要点击保存返回后才会写入题库。",
            confirmText = "确认移除",
            onDismiss = { showRemoveImagesConfirm = false },
            onConfirm = {
                val target = editableQuestions.getOrNull(safeIndex)
                if (target != null) {
                    editableQuestions[safeIndex] = target.copy(images = emptyList())
                }
                showRemoveImagesConfirm = false
            }
        )
    }

    if (showDeleteLastOptionConfirm && editableQuestions.isNotEmpty()) {
        ShirohaDangerConfirmDialog(
            title = "确认删除最后一个选项？",
            message = "这会删除当前题目的最后一个选项。需要点击保存返回后才会写入题库。",
            confirmText = "确认删除",
            onDismiss = { showDeleteLastOptionConfirm = false },
            onConfirm = {
                val target = editableQuestions.getOrNull(safeIndex)
                if (target != null && target.options.isNotEmpty()) {
                    editableQuestions[safeIndex] = target.copy(options = target.options.dropLast(1))
                }
                showDeleteLastOptionConfirm = false
            }
        )
    }

    if (showDeleteQuestionConfirm && editableQuestions.isNotEmpty()) {
        ShirohaDangerConfirmDialog(
            title = "确认删除本题？",
            message = "这会从当前核对列表中删除本题。需要点击保存返回后才会真正写入题库。",
            confirmText = "确认删除",
            onDismiss = { showDeleteQuestionConfirm = false },
            onConfirm = {
                if (safeIndex in editableQuestions.indices) {
                    editableQuestions.removeAt(safeIndex)
                    currentIndex = safeIndex.coerceAtMost((editableQuestions.size - 1).coerceAtLeast(0))
                }
                showDeleteQuestionConfirm = false
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
            kicker = "Review",
            title = "题库核对",
            subtitle = "${bank.name} · ${editableQuestions.size} 题。"
        )

        GlassCard {
            Text(
                text = "核对筛选",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BankReviewFilter.values().forEach { item ->
                    ReviewTypeChip(
                        text = "${bankReviewFilterLabel(item)} ${bankReviewFilterCount(editableQuestions, item)}",
                        selected = filter == item,
                        onClick = {
                            filterName = item.name
                            currentIndex = firstMatchingQuestionIndex(editableQuestions, item, query) ?: 0
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { value ->
                    query = value
                    currentIndex = firstMatchingQuestionIndex(editableQuestions, filter, value) ?: currentIndex
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索题干 / 选项 / 答案") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true
            )
            if (visibleIndices.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                NoticeCard("当前筛选下没有题目。可以切回“全部”或清空搜索关键词。", warning = false)
            }
        }

        if (editableQuestions.isEmpty()) {
            GlassCard {
                NoticeCard("当前题库已经没有题目。保存后这份题库会变成空题库。", warning = true)
                Spacer(Modifier.height(12.dp))
                ActionPillButton(
                    icon = Icons.Rounded.Done,
                    text = "保存并返回",
                    primary = true,
                    onClick = {
                        QuizRepository.replaceBankQuestions(context, bank.id, editableQuestions)
                        onBack()
                    }
                )
            }
            return@Column
        }

        if (visibleIndices.isNotEmpty()) {
            val question = editableQuestions[safeIndex]
            val visiblePosition = visibleIndices.indexOf(safeIndex).takeIf { it >= 0 } ?: 0
            val anomalyIndices = editableQuestions.indices.filter { index ->
                bankQuestionMatchesFilter(editableQuestions[index], BankReviewFilter.ANOMALY)
            }

            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "第 ${safeIndex + 1} / ${editableQuestions.size} 题",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append(typeLabel(question.type))
                                append(" · 答案：")
                                append(answerDisplayText(question))
                                if (filter != BankReviewFilter.ALL || searchText.isNotBlank()) {
                                    append(" · 当前筛选 ${visiblePosition + 1}/${visibleIndices.size}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ReviewCompactButton(
                        icon = Icons.Rounded.CheckCircle,
                        text = "保存返回",
                        primary = true,
                        onClick = {
                            QuizRepository.replaceBankQuestions(context, bank.id, editableQuestions)
                            onBack()
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReviewCompactButton(
                        icon = Icons.Rounded.ArrowBack,
                        text = "放弃",
                        modifier = Modifier.weight(1f),
                        onClick = onBack
                    )
                    ReviewCompactButton(
                        icon = Icons.Rounded.ArrowBack,
                        text = if (filter == BankReviewFilter.ALL && searchText.isBlank()) "上一题" else "上一条",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val target = previousIndexInList(visibleIndices, safeIndex) ?: (safeIndex - 1).coerceAtLeast(0)
                            currentIndex = target
                        }
                    )
                    ReviewCompactButton(
                        icon = Icons.Rounded.ArrowForward,
                        text = if (filter == BankReviewFilter.ALL && searchText.isBlank()) "下一题" else "下一条",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val target = nextIndexInList(visibleIndices, safeIndex) ?: (safeIndex + 1).coerceAtMost(editableQuestions.lastIndex)
                            currentIndex = target
                        }
                    )
                }
                if (anomalyIndices.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReviewCompactButton(
                            icon = Icons.Rounded.ArrowBack,
                            text = "上一异常",
                            modifier = Modifier.weight(1f),
                            onClick = { previousIndexInList(anomalyIndices, safeIndex)?.let { currentIndex = it } }
                        )
                        ReviewCompactButton(
                            icon = Icons.Rounded.ArrowForward,
                            text = "下一异常",
                            modifier = Modifier.weight(1f),
                            onClick = { nextIndexInList(anomalyIndices, safeIndex)?.let { currentIndex = it } }
                        )
                    }
                }
            }

            if ((filter != BankReviewFilter.ALL || searchText.isNotBlank()) && visibleIndices.size > 1) {
                ReviewFilteredJumpList(
                    questions = editableQuestions,
                    indices = visibleIndices,
                    currentIndex = safeIndex,
                    onIndexChange = { currentIndex = it }
                )
            }

            if (bankReviewTips(question).isNotEmpty()) {
                GlassCard {
                    Text(
                        text = "本题提示",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    bankReviewTips(question).forEach { tip ->
                        NoticeCard(tip, warning = true)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            GlassCard {
                Text(
                    text = "题目内容",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = question.number,
                        onValueChange = { value ->
                            editableQuestions[safeIndex] = question.copy(number = value)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("题号") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = question.category,
                        onValueChange = { value ->
                            editableQuestions[safeIndex] = question.copy(category = value)
                        },
                        modifier = Modifier.weight(1.4f),
                        label = { Text("分区/来源") },
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuestionType.values().forEach { type ->
                        ReviewTypeChip(
                            text = typeLabel(type),
                            selected = question.type == type,
                            onClick = { editableQuestions[safeIndex] = normalizeAfterTypeChange(question, type) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = question.question,
                    onValueChange = { value ->
                        editableQuestions[safeIndex] = question.copy(question = value)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    minLines = 6,
                    label = { Text("题干") },
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            if (question.images.isNotEmpty()) {
                GlassCard {
                    Text(
                        text = "题目图片",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    NoticeCard("图片来自原题库资源。请确认图片是否属于本题。", warning = false)
                    Spacer(Modifier.height(12.dp))
                    QuestionImagesBlock(question.images, maxPreviewHeight = 360.dp, showMeta = true)
                    Spacer(Modifier.height(12.dp))
                    ActionPillButton(
                        icon = Icons.Rounded.Delete,
                        text = "移除本题图片",
                        primary = false,
                        onClick = { showRemoveImagesConfirm = true }
                    )
                }
            }

            GlassCard {
                Text(
                    text = "选项",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                if (question.options.isEmpty()) {
                    NoticeCard("当前题目没有选项。判断题可以补齐判断选项，选择题可以添加选项。", warning = false)
                    Spacer(Modifier.height(12.dp))
                }
                question.options.forEachIndexed { optionIndex, option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = option.key,
                            onValueChange = { value ->
                                val updated = question.options.mapIndexed { currentOptionIndex, item ->
                                    if (currentOptionIndex == optionIndex) item.copy(key = value.uppercase().take(2)) else item
                                }
                                editableQuestions[safeIndex] = question.copy(options = updated)
                            },
                            modifier = Modifier.width(74.dp),
                            label = { Text("项") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = option.text,
                            onValueChange = { value ->
                                val updated = question.options.mapIndexed { currentOptionIndex, item ->
                                    if (currentOptionIndex == optionIndex) item.copy(text = value) else item
                                }
                                editableQuestions[safeIndex] = question.copy(options = updated)
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("选项内容") }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionPillButton(
                        icon = Icons.Rounded.Add,
                        text = "添加选项",
                        primary = false,
                        onClick = {
                            val key = nextOptionKey(question.options)
                            editableQuestions[safeIndex] = question.copy(options = question.options + Option(key, ""))
                        }
                    )
                    ActionPillButton(
                        icon = Icons.Rounded.RemoveCircle,
                        text = "删除最后选项",
                        primary = false,
                        onClick = {
                            if (question.options.isNotEmpty()) {
                                showDeleteLastOptionConfirm = true
                            }
                        }
                    )
                    ActionPillButton(
                        icon = Icons.Rounded.CheckCircle,
                        text = "补齐判断选项",
                        primary = false,
                        onClick = {
                            editableQuestions[safeIndex] = question.copy(
                                type = QuestionType.JUDGE,
                                options = defaultJudgeOptions(),
                                answer = if (question.answer.isEmpty()) listOf("A") else question.answer,
                                blankAnswers = emptyList()
                            )
                        }
                    )
                }
            }

            GlassCard {
                Text(
                    text = "答案与解析",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                val detectedBlankCount = MultiBlankSupport.countExplicitBlanks(question.question)
                if (question.type == QuestionType.BLANK && question.blankAnswers.isNotEmpty()) {
                    MultiBlankAnswerEditor(
                        blankAnswers = question.blankAnswers,
                        detectedBlankCount = detectedBlankCount,
                        onChange = { groups ->
                            editableQuestions[safeIndex] = MultiBlankSupport.withBlankAnswers(question, groups)
                        },
                        onDisable = {
                            editableQuestions[safeIndex] = question.copy(
                                answer = MultiBlankSupport.compatibilityAnswer(question.blankAnswers),
                                blankAnswers = emptyList()
                            )
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = answerInputText(question),
                        onValueChange = { value ->
                            editableQuestions[safeIndex] = question.copy(
                                answer = parseReviewAnswer(value, question.type),
                                blankAnswers = emptyList()
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("答案：单选填 A，多选填 ABC 或 A,B,C，判断填 正确/错误") },
                        singleLine = true
                    )
                    if (question.type == QuestionType.BLANK && detectedBlankCount > 1) {
                        Spacer(Modifier.height(10.dp))
                        ActionPillButton(
                            icon = Icons.Rounded.Add,
                            text = "启用多空答案",
                            primary = false,
                            onClick = {
                                editableQuestions[safeIndex] = MultiBlankSupport.withBlankAnswers(
                                    question,
                                    MultiBlankSupport.initialGroups(question.question, question.answer)
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = question.analysis,
                    onValueChange = { value ->
                        editableQuestions[safeIndex] = question.copy(analysis = value)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    minLines = 5,
                    label = { Text("解析") },
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                AiAnalysisFillPanel(
                    question = question,
                    currentAnalysis = question.analysis,
                    onApplyAnalysis = { value ->
                        editableQuestions[safeIndex] = question.copy(analysis = value)
                    }
                )
            }

            GlassCard {
                Text(
                    text = "危险操作",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "如果解析器把说明文字、页眉页脚或碎片段识别成题目，可以删除本题。删除后需要点击保存返回才会真正写入题库。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                ActionPillButton(
                    icon = Icons.Rounded.Delete,
                    text = "删除本题",
                    primary = false,
                    onClick = { showDeleteQuestionConfirm = true }
                )
            }
        }
    }
}

private enum class BankReviewFilter {
    ALL,
    ANOMALY,
    NO_ANSWER,
    IMAGE,
    HARD_ERROR
}

private fun bankReviewFilterFromName(name: String): BankReviewFilter {
    return runCatching { BankReviewFilter.valueOf(name) }.getOrDefault(BankReviewFilter.ALL)
}

private fun bankReviewFilterLabel(filter: BankReviewFilter): String = when (filter) {
    BankReviewFilter.ALL -> "全部"
    BankReviewFilter.ANOMALY -> "可疑项"
    BankReviewFilter.NO_ANSWER -> "无答案"
    BankReviewFilter.IMAGE -> "图片题"
    BankReviewFilter.HARD_ERROR -> "硬错误"
}

private fun bankReviewFilterCount(
    questions: List<Question>,
    filter: BankReviewFilter
): Int {
    if (filter == BankReviewFilter.ALL) return questions.size
    return questions.count { bankQuestionMatchesFilter(it, filter) }
}

private fun firstMatchingQuestionIndex(
    questions: List<Question>,
    filter: BankReviewFilter,
    query: String
): Int? {
    return questions.indices.firstOrNull { index ->
        bankQuestionMatchesFilter(questions[index], filter) && bankQuestionMatchesQuery(questions[index], query.trim())
    }
}

private fun bankQuestionMatchesFilter(question: Question, filter: BankReviewFilter): Boolean {
    return when (filter) {
        BankReviewFilter.ALL -> true
        BankReviewFilter.ANOMALY -> bankReviewTips(question).isNotEmpty()
        BankReviewFilter.NO_ANSWER -> question.answer.isEmpty()
        BankReviewFilter.IMAGE -> question.images.isNotEmpty()
        BankReviewFilter.HARD_ERROR -> bankHasHardReviewError(question)
    }
}

private fun bankQuestionMatchesQuery(question: Question, query: String): Boolean {
    if (query.isBlank()) return true
    val normalized = query.lowercase()
    return question.question.lowercase().contains(normalized) ||
        question.number.lowercase().contains(normalized) ||
        question.category.lowercase().contains(normalized) ||
        question.answer.joinToString(" ").lowercase().contains(normalized) ||
        question.blankAnswers.flatten().joinToString(" ").lowercase().contains(normalized) ||
        question.options.any { option ->
            option.key.lowercase().contains(normalized) || option.text.lowercase().contains(normalized)
        }
}

private fun bankReviewTips(question: Question): List<String> {
    val tips = mutableListOf<String>()
    if (question.question.isBlank()) tips += "题干为空。"
    if (question.answer.isEmpty()) tips += "未识别到答案。"
    if (question.type in listOf(QuestionType.SINGLE, QuestionType.MULTIPLE) && question.options.size < 2) {
        tips += "选择题选项数量偏少。"
    }
    if (bankHasHardReviewError(question)) tips += "存在硬错误，建议优先处理。"
    return tips.distinct()
}

private fun bankHasHardReviewError(question: Question): Boolean {
    return question.question.isBlank() ||
        (question.type in listOf(QuestionType.SINGLE, QuestionType.MULTIPLE) && question.options.isEmpty())
}

private fun previousIndexInList(indices: List<Int>, currentIndex: Int): Int? {
    if (indices.isEmpty()) return null
    return indices.lastOrNull { it < currentIndex } ?: indices.lastOrNull()
}

private fun nextIndexInList(indices: List<Int>, currentIndex: Int): Int? {
    if (indices.isEmpty()) return null
    return indices.firstOrNull { it > currentIndex } ?: indices.firstOrNull()
}

@Composable
private fun ReviewFilteredJumpList(
    questions: List<Question>,
    indices: List<Int>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit
) {
    GlassCard {
        Text(
            text = "当前筛选列表",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        indices.take(12).forEach { index ->
            val question = questions[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shirohaNoRippleClickable { onIndexChange(index) },
                shape = RoundedCornerShape(ShirohaRadius.Md),
                color = if (index == currentIndex) ShirohaColors.BrandPrimarySoft else Color.White.copy(alpha = 0.72f),
                border = BorderStroke(
                    1.dp,
                    if (index == currentIndex) ShirohaColors.LineSelected else ShirohaColors.LineStrong
                )
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = "第 ${index + 1} 题 · ${typeLabel(question.type)} · 答案：${answerDisplayText(question)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = question.question.ifBlank { "题干为空" }.take(70),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (indices.size > 12) {
            NoticeCard("当前筛选共 ${indices.size} 题，这里先显示前 12 题；可以用“上一条 / 下一条”继续核对。", warning = false)
        }
    }
}

@Composable
private fun ReviewTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.shirohaNoRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (selected) ShirohaColors.BrandPrimarySoft else Color.White.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (selected) ShirohaColors.LineSelected else ShirohaColors.LineStrong)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReviewCompactButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.shirohaNoRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (primary) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.82f),
        border = if (primary) null else BorderStroke(1.dp, ShirohaColors.LineStrong)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(15.dp),
                tint = if (primary) Color.White else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun normalizeAfterTypeChange(question: Question, type: QuestionType): Question {
    return when (type) {
        QuestionType.JUDGE -> question.copy(
            type = type,
            options = if (question.options.isEmpty()) defaultJudgeOptions() else question.options,
            answer = normalizeJudgeAnswer(question.answer),
            blankAnswers = emptyList()
        )

        QuestionType.SINGLE,
        QuestionType.MULTIPLE -> question.copy(type = type, blankAnswers = emptyList())

        QuestionType.BLANK -> question.copy(type = type)
        QuestionType.SHORT -> question.copy(type = type, blankAnswers = emptyList())
    }
}

private fun normalizeJudgeAnswer(answer: List<String>): List<String> {
    if (answer.isEmpty()) return emptyList()
    return answer.mapNotNull { value ->
        when (value.trim().uppercase()) {
            "A", "正确", "对", "是", "TRUE", "T", "√", "✔", "✅", "☑" -> "A"
            "B", "错误", "错", "否", "FALSE", "F", "×", "X", "✖", "❌" -> "B"
            else -> value.trim().takeIf { it.isNotBlank() }
        }
    }
}

private fun defaultJudgeOptions(): List<Option> = listOf(
    Option("A", "正确"),
    Option("B", "错误")
)

private fun nextOptionKey(options: List<Option>): String {
    val used = options.map { it.key.uppercase() }.toSet()
    return ('A'..'H').firstOrNull { it.toString() !in used }?.toString() ?: "${options.size + 1}"
}

private fun parseReviewAnswer(text: String, type: QuestionType): List<String> {
    val clean = text.trim()
    if (clean.isBlank()) return emptyList()

    if (type == QuestionType.BLANK || type == QuestionType.SHORT) return listOf(clean)

    if (type == QuestionType.JUDGE) {
        normalizeJudgeAnswer(listOf(clean)).takeIf { it.isNotEmpty() }?.let { return it }
    }

    val compactLetters = clean.uppercase().replace(Regex("[\\s,，、;；/]+"), "")
    if (compactLetters.matches(Regex("^[A-H]{1,8}$"))) {
        return compactLetters.map { it.toString() }.distinct()
    }

    return clean
        .replace("，", ",")
        .replace("、", ",")
        .replace("/", ",")
        .replace("；", ",")
        .replace(";", ",")
        .split(Regex("[\\s,]+"))
        .map { token -> token.trim() }
        .filter { it.isNotBlank() }
        .flatMap { token ->
            if (token.uppercase().matches(Regex("^[A-H]{2,8}$"))) {
                token.uppercase().map { it.toString() }
            } else {
                normalizeJudgeAnswer(listOf(token)).ifEmpty { listOf(token.uppercase()) }
            }
        }
        .distinct()
}

private fun answerInputText(question: Question): String {
    if (question.type == QuestionType.JUDGE && question.answer.size == 1) {
        return when (question.answer.first().trim().uppercase()) {
            "A", "正确", "对", "是", "TRUE", "T", "√", "✔", "✅", "☑" -> "正确"
            "B", "错误", "错", "否", "FALSE", "F", "×", "X", "✖", "❌" -> "错误"
            else -> question.answer.first()
        }
    }
    return question.answer.joinToString(",")
}

private fun answerDisplayText(question: Question): String {
    if (MultiBlankSupport.hasStructuredAnswers(question)) {
        return MultiBlankSupport.expectedAnswerText(question.blankAnswers)
    }
    val value = answerInputText(question)
    return value.ifBlank { "未识别答案" }
}

private fun typeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}
