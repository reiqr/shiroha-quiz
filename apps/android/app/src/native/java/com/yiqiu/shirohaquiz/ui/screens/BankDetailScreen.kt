package com.yiqiu.shirohaquiz.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.DEFAULT_BANK_GROUP_NAME
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.AiAnalysisFillPanel
import com.yiqiu.shirohaquiz.ui.components.EmptyStateIllustration
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.MultiBlankAnswerEditor
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaDangerConfirmDialog
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import com.yiqiu.shirohaquiz.ui.util.bankDisplayPath

@Composable
fun BankDetailScreen(
    bankId: String?,
    onBack: () -> Unit,
    onGoPractice: () -> Unit,
    onGoExam: () -> Unit,
    onOpenReview: () -> Unit
) {
    val context = LocalContext.current
    val bank = if (bankId == null) {
        QuizRepository.activeBank()
    } else {
        QuizRepository.banks.firstOrNull { it.id == bankId }
    }
    val isActive = bank?.id == QuizRepository.activeBank()?.id
    var showSlashedList by remember(bank?.id) { mutableStateOf(false) }
    var showDeleteBankConfirm by remember(bank?.id) { mutableStateOf(false) }

    BackHandler(enabled = showSlashedList) { showSlashedList = false }

    if (showDeleteBankConfirm) {
        bank?.let { targetBank ->
            ShirohaDangerConfirmDialog(
                title = "确认删除题库？",
                message = "将删除“${targetBank.name}”，并清理这份题库关联的错题、斩题和学习记录。操作不可撤销。",
                confirmText = "确认删除",
                onDismiss = { showDeleteBankConfirm = false },
                onConfirm = {
                    QuizRepository.deleteBank(context, targetBank.id)
                    showDeleteBankConfirm = false
                    onBack()
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Bank Detail",
            title = bank?.let { bankDisplayPath(it.groupName, it.name) } ?: "题库详情",
            subtitle = "题库摘要、题型分布和快速操作。"
        )

        if (bank == null) {
            EmptyStateIllustration(
                title = "没有找到对应题库",
                message = "这通常说明题库已经被切换或删除。返回上一页重新选择一份题库即可。",
                imageRes = R.drawable.illus_empty_state_webp,
                action = {
                    Spacer(Modifier.height(12.dp))
                }
            )
            GlassCard {
                ActionPillButton(
                    icon = Icons.Rounded.Done,
                    text = "返回",
                    primary = true,
                    onClick = onBack
                )
            }
            return
        }

        if (showSlashedList) {
            SlashedQuestionListCard(
                bank = bank,
                onBack = { showSlashedList = false },
                onRestore = { question -> QuizRepository.restoreSlashedQuestion(context, bank.id, question) }
            )
            return
        }

        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "题库摘要",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                ActionPillButton(
                    icon = Icons.Rounded.Done,
                    text = if (isActive) "当前题库" else "设为当前",
                    primary = true,
                    modifier = Modifier.height(44.dp),
                    onClick = {
                        if (!isActive) {
                            QuizRepository.setActiveBank(context, bank.id)
                        }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip("${bank.questions.size} 题", selected = true)
                StatusChip(bank.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME }, selected = false)
                StatusChip(if (isActive) "活动题库" else "可切换题库", selected = isActive)
                Spacer(Modifier.weight(1f))
                SlashedBankChip(
                    count = QuizRepository.slashedQuestionCount(bank.id),
                    onClick = { showSlashedList = true }
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "单选 ${bank.questions.count { it.type == QuestionType.SINGLE }} · 多选 ${bank.questions.count { it.type == QuestionType.MULTIPLE }} · 判断 ${bank.questions.count { it.type == QuestionType.JUDGE }} · 主观 ${bank.questions.count { it.type == QuestionType.BLANK || it.type == QuestionType.SHORT }}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.PlayArrow,
                    text = "进入练习",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    fillWidthContent = true,
                    onClick = onGoPractice
                )
                ActionPillButton(
                    icon = Icons.Rounded.Timer,
                    text = "进入考试",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    fillWidthContent = true,
                    onClick = onGoExam
                )
            }
        }

        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "题目预览",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                ActionPillButton(
                    icon = Icons.Rounded.Edit,
                    text = "二次核对",
                    primary = false,
                    modifier = Modifier.height(42.dp),
                    onClick = onOpenReview
                )
            }
            Spacer(Modifier.height(12.dp))
            NoticeCard("这里只显示前 5 题。点击“二次核对”可进入完整沉浸核对页，逐题查看和修改整份题库。", warning = false)
            Spacer(Modifier.height(12.dp))
            bank.questions.take(5).forEach { question ->
                QuestionPreviewBlock(
                    question = question,
                    editable = false,
                    onEdit = {}
                )
            }
        }

        if (bank.id != "demo-bank") {
            GlassCard {
                Text(
                    text = "危险操作",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                NoticeCard("删除题库后，会一起清理这份原生题库关联的本地记录。")
                Spacer(Modifier.height(12.dp))
                ActionPillButton(
                    icon = Icons.Rounded.DeleteOutline,
                    text = "删除这份题库",
                    primary = false,
                    onClick = { showDeleteBankConfirm = true }
                )
            }
        }
    }
}

@Composable
private fun SlashedBankChip(
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(32.dp)
            .shirohaNoRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = ShirohaColors.BrandPrimarySoft,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSelected)
    ) {
        Text(
            text = "斩 $count",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SlashedQuestionListCard(
    bank: com.yiqiu.shirohaquiz.state.QuizBank,
    onBack: () -> Unit,
    onRestore: (Question) -> Unit
) {
    val slashed = QuizRepository.slashedQuestionsForBank(bank.id)
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "当前题库斩题本",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${bank.name} · 共 ${slashed.size} 题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ActionPillButton(
                icon = Icons.Rounded.Done,
                text = "返回",
                primary = false,
                modifier = Modifier.height(42.dp),
                onClick = onBack
            )
        }
        Spacer(Modifier.height(12.dp))
        if (slashed.isEmpty()) {
            NoticeCard("暂无已斩题。开启斩题功能后，练习时点击题目右上角“斩”按钮，可将一眼会的题移出后续练习。", warning = false)
        } else {
            NoticeCard("恢复后，这道题会重新进入后续练习。", warning = false)
            Spacer(Modifier.height(12.dp))
            slashed.forEach { question ->
                QuestionPreviewBlock(
                    question = question,
                    editable = false,
                    onEdit = {}
                )
                ActionPillButton(
                    icon = Icons.Rounded.Done,
                    text = "恢复本题",
                    primary = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    fillWidthContent = true,
                    onClick = { onRestore(question) }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun QuestionPreviewBlock(
    question: Question,
    editable: Boolean,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(typeLabel(question.type))
        if (editable) {
            Spacer(Modifier.weight(1f))
            ActionPillButton(
                icon = Icons.Rounded.Edit,
                text = "修改",
                primary = false,
                modifier = Modifier.height(38.dp),
                onClick = onEdit
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = "${question.number}. ${question.question}",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold
    )
    if (question.options.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = question.options.joinToString("  ") { "${it.key}. ${it.text}" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (editable) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (MultiBlankSupport.hasStructuredAnswers(question)) {
                "答案：\n${MultiBlankSupport.expectedAnswerText(question.blankAnswers)}"
            } else {
                "答案：${question.answer.joinToString(" / ").ifBlank { "未识别" }}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun QuestionEditDialog(
    question: Question,
    onDismiss: () -> Unit,
    onSave: (Question) -> Unit
) {
    var stem by remember(question.id) { mutableStateOf(question.question) }
    var optionsText by remember(question.id) { mutableStateOf(formatOptions(question.options)) }
    var answerText by remember(question.id) { mutableStateOf(question.answer.joinToString(" ")) }
    var blankAnswerDrafts by remember(question.id) { mutableStateOf(question.blankAnswers) }
    var analysisText by remember(question.id) { mutableStateOf(question.analysis) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改题目") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "题型：${typeLabel(question.type)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = stem,
                    onValueChange = { stem = it },
                    label = { Text("题干") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = optionsText,
                    onValueChange = { optionsText = it },
                    label = { Text("选项，每行一个，例如 A. 选项内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                val isStructuredBlank = question.type == QuestionType.BLANK && blankAnswerDrafts.isNotEmpty()
                val detectedBlankCount = MultiBlankSupport.countExplicitBlanks(stem)
                if (isStructuredBlank) {
                    MultiBlankAnswerEditor(
                        blankAnswers = blankAnswerDrafts,
                        detectedBlankCount = detectedBlankCount,
                        onChange = { groups ->
                            blankAnswerDrafts = groups
                            answerText = MultiBlankSupport.compatibilityAnswer(groups).firstOrNull().orEmpty()
                        },
                        onDisable = {
                            answerText = MultiBlankSupport.compatibilityAnswer(blankAnswerDrafts).firstOrNull().orEmpty()
                            blankAnswerDrafts = emptyList()
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        label = { Text("答案，例如 A 或 A B") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (question.type == QuestionType.BLANK && detectedBlankCount > 1) {
                        TextButton(
                            onClick = {
                                blankAnswerDrafts = MultiBlankSupport.initialGroups(stem, parseAnswer(answerText, QuestionType.BLANK))
                            }
                        ) { Text("启用多空答案") }
                    }
                }
                OutlinedTextField(
                    value = analysisText,
                    onValueChange = { analysisText = it },
                    label = { Text("解析") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                AiAnalysisFillPanel(
                    question = question.copy(
                        question = stem.trim(),
                        options = parseOptions(optionsText),
                        answer = if (question.type == QuestionType.BLANK && blankAnswerDrafts.isNotEmpty()) {
                            MultiBlankSupport.compatibilityAnswer(blankAnswerDrafts)
                        } else {
                            parseAnswer(answerText, question.type)
                        },
                        blankAnswers = if (question.type == QuestionType.BLANK) blankAnswerDrafts else emptyList(),
                        analysis = analysisText.trim()
                    ),
                    currentAnalysis = analysisText,
                    onApplyAnalysis = { analysisText = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        question.copy(
                            question = stem.trim(),
                            options = parseOptions(optionsText),
                            answer = if (question.type == QuestionType.BLANK && blankAnswerDrafts.isNotEmpty()) {
                                MultiBlankSupport.compatibilityAnswer(blankAnswerDrafts)
                            } else {
                                parseAnswer(answerText, question.type)
                            },
                            blankAnswers = if (question.type == QuestionType.BLANK) blankAnswerDrafts else emptyList(),
                            analysis = analysisText.trim()
                        )
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun formatOptions(options: List<Option>): String {
    return options.joinToString("\n") { "${it.key}. ${it.text}" }
}

private fun parseOptions(raw: String): List<Option> {
    return raw.lines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) return@mapNotNull null
        val match = Regex("^([A-Ga-g])\\s*[.．、:：]?\\s*(.+)$").find(trimmed)
        if (match != null) {
            Option(
                key = match.groupValues[1].uppercase(),
                text = match.groupValues[2].trim()
            )
        } else {
            null
        }
    }
}

private fun parseAnswer(raw: String, type: QuestionType): List<String> {
    val clean = raw.trim()
    if (clean.isBlank()) return emptyList()
    if (type == QuestionType.BLANK || type == QuestionType.SHORT) return listOf(clean)
    return clean.split(Regex("[\\s,，、/]+"))
        .map { it.trim().uppercase() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun typeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}
