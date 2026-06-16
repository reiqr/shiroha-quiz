package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.DEFAULT_BANK_GROUP_NAME
import com.yiqiu.shirohaquiz.state.QuizBank
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaDangerConfirmDialog
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import com.yiqiu.shirohaquiz.ui.util.bankDisplayPath

@Composable
fun BankListScreen(
    onBack: () -> Unit,
    onOpenQuestionSearch: () -> Unit,
    onOpenBankDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val activeBank = QuizRepository.activeBank()
    val practiceScopeType = QuizRepository.practiceScopeType
    val practiceScopeValue = QuizRepository.practiceScopeValue
    var editTarget by remember { mutableStateOf<QuizBank?>(null) }
    var editNameText by remember { mutableStateOf("") }
    var moveTarget by remember { mutableStateOf<QuizBank?>(null) }
    var moveGroupText by remember { mutableStateOf(DEFAULT_BANK_GROUP_NAME) }
    var deleteTarget by remember { mutableStateOf<QuizBank?>(null) }
    var collapsedGroups by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    if (editTarget != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("编辑题库信息") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { editNameText = it },
                        label = { Text("二级题库名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val bank = editTarget
                        if (bank != null && editNameText.isNotBlank()) {
                            QuizRepository.renameBank(
                                context = context,
                                bankId = bank.id,
                                newName = editNameText
                            )
                        }
                        editTarget = null
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("取消") }
            }
        )
    }

    if (moveTarget != null) {
        AlertDialog(
            onDismissRequest = { moveTarget = null },
            title = { Text("移动到分组") },
            text = {
                OutlinedTextField(
                    value = moveGroupText,
                    onValueChange = { moveGroupText = it },
                    label = { Text("目标一级分组") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val bank = moveTarget
                        if (bank != null && moveGroupText.isNotBlank()) {
                            QuizRepository.updateBankInfo(
                                context = context,
                                bankId = bank.id,
                                newGroupName = moveGroupText,
                                newName = bank.name
                            )
                        }
                        moveTarget = null
                    }
                ) { Text("移动") }
            },
            dismissButton = {
                TextButton(onClick = { moveTarget = null }) { Text("取消") }
            }
        )
    }

    deleteTarget?.let { bank ->
        ShirohaDangerConfirmDialog(
            title = "确认删除题库？",
            message = "将删除“${bankDisplayPath(bank)}”，并清理这份题库关联的错题、斩题和学习记录。操作不可撤销。",
            confirmText = "确认删除",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                QuizRepository.deleteBank(context, bank.id)
                deleteTarget = null
            }
        )
    }

    val groupedBanks = QuizRepository.banks
        .groupBy { it.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME } }
        .entries
        .sortedBy { entry -> if (entry.key == DEFAULT_BANK_GROUP_NAME) "" else entry.key }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Banks",
            title = "题库管理",
            subtitle = "管理当前题库与默认练习范围。"
        )

        GlassCard(contentPadding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "当前题库：${activeBank?.name ?: "尚未选择"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "练习范围：${QuizRepository.currentPracticeScopeLabel()} · ${QuizRepository.currentPracticeScopeSummary()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        GlassCard(
            modifier = Modifier.shirohaNoRippleClickable(onClick = onOpenQuestionSearch),
            contentPadding = 18.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "搜索题目",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "搜索题目",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "搜题干、选项、答案或解析",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "进入题目搜索",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        groupedBanks.forEach { entry ->
            val groupName = entry.key
            val banksInGroup = entry.value
            val isExpanded = groupName !in collapsedGroups
            val totalQuestions = banksInGroup.sumOf { it.questions.size }

            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .shirohaNoRippleClickable {
                                collapsedGroups = if (isExpanded) {
                                    (collapsedGroups + groupName).distinct()
                                } else {
                                    collapsedGroups - groupName
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                            contentDescription = if (isExpanded) "收起分组" else "展开分组",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${banksInGroup.size} 个题库 · $totalQuestions 题",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    CompactBankStateChip(
                        text = if (practiceScopeType == QuizRepository.PRACTICE_SCOPE_GROUP && practiceScopeValue == groupName) "当前练习范围" else "设为练习范围",
                        selected = practiceScopeType == QuizRepository.PRACTICE_SCOPE_GROUP && practiceScopeValue == groupName,
                        onClick = {
                            if (!(practiceScopeType == QuizRepository.PRACTICE_SCOPE_GROUP && practiceScopeValue == groupName)) {
                                QuizRepository.setPracticeGroupScope(context, groupName)
                            }
                        }
                    )
                }

                if (isExpanded) {
                    Spacer(Modifier.height(12.dp))
                    banksInGroup.forEach { bank ->
                        BankCard(
                            bank = bank,
                            isActive = bank.id == activeBank?.id,
                            isPracticeScope = practiceScopeType == QuizRepository.PRACTICE_SCOPE_BANK && practiceScopeValue == bank.id,
                            onOpenBankDetail = onOpenBankDetail,
                            onSetActive = { QuizRepository.setActiveBank(context, bank.id) },
                            onEdit = {
                                editTarget = bank
                                editNameText = bank.name
                            },
                            onMove = {
                                moveTarget = bank
                                moveGroupText = bank.groupName.ifBlank { DEFAULT_BANK_GROUP_NAME }
                            },
                            onDelete = {
                                if (bank.id != "demo-bank") {
                                    deleteTarget = bank
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            ActionPillButton(
                icon = Icons.Rounded.ArrowBack,
                text = "返回首页",
                primary = false,
                modifier = Modifier.height(44.dp),
                onClick = onBack
            )
        }
        Spacer(Modifier.height(ShirohaSpacing.Xl))
    }
}

@Composable
private fun BankCard(
    bank: QuizBank,
    isActive: Boolean,
    isPracticeScope: Boolean,
    onOpenBankDetail: (String) -> Unit,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val singleCount = bank.questions.count { it.type == QuestionType.SINGLE }
    val multipleCount = bank.questions.count { it.type == QuestionType.MULTIPLE }
    val judgeCount = bank.questions.count { it.type == QuestionType.JUDGE }
    val subjectiveCount = bank.questions.count { it.type == QuestionType.BLANK || it.type == QuestionType.SHORT }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shirohaNoRippleClickable { onOpenBankDetail(bank.id) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ShirohaRadius.Lg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ShirohaColors.LineSoft)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip("${bank.questions.size} 题", selected = true)
                if (isPracticeScope) {
                    Spacer(Modifier.width(6.dp))
                    StatusChip("练习范围", selected = true)
                }
                Spacer(Modifier.weight(1f))
                CompactBankStateChip(
                    text = if (isActive) "当前题库" else "设为当前",
                    selected = isActive && isPracticeScope,
                    onClick = {
                        if (!isActive || !isPracticeScope) onSetActive()
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = bank.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "单选 $singleCount · 多选 $multipleCount · 判断 $judgeCount · 主观 $subjectiveCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.Visibility,
                    text = "详情",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    fillWidthContent = true,
                    onClick = { onOpenBankDetail(bank.id) }
                )
                ActionPillButton(
                    icon = Icons.Rounded.Edit,
                    text = "编辑",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    fillWidthContent = true,
                    onClick = onEdit
                )
                Box(modifier = Modifier.weight(1f)) {
                    ActionPillButton(
                        icon = Icons.Rounded.MoreVert,
                        text = "更多",
                        primary = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        fillWidthContent = true,
                        onClick = { moreMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("移动到分组") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                onMove()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除题库") },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBankStateChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.shirohaNoRippleClickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ShirohaRadius.Pill),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.84f),
        border = if (selected) null else BorderStroke(1.dp, ShirohaColors.LineStrong)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Done,
                contentDescription = text,
                modifier = Modifier.size(14.dp),
                tint = if (selected) Color.White else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}
