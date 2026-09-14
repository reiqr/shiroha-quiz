package com.yiqiu.shirohaquiz.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.document.DocumentRecognitionManager
import com.yiqiu.shirohaquiz.document.DocumentRecognitionTask
import com.yiqiu.shirohaquiz.document.DocumentTaskStage
import com.yiqiu.shirohaquiz.document.MinerUMode
import com.yiqiu.shirohaquiz.document.MinerUModelVersion
import com.yiqiu.shirohaquiz.document.MinerUSettings
import com.yiqiu.shirohaquiz.document.SelectedDocument
import com.yiqiu.shirohaquiz.document.isWorking
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@Composable
fun DocumentRecognitionScreen(
    onBack: () -> Unit,
    onUseResultForImport: () -> Unit
) {
    BackHandler(onBack = onBack)
    val settings = DocumentRecognitionManager.settings
    val document = DocumentRecognitionManager.selectedDocument
    val task = DocumentRecognitionManager.task
    val message = DocumentRecognitionManager.uiMessage
    val resultText = DocumentRecognitionManager.resultText
    val isLocked = task?.isWorking == true || DocumentRecognitionManager.isPreparingDocument
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showReplaceTaskDialog by rememberSaveable { mutableStateOf(false) }
    var tokenDraft by remember { mutableStateOf("") }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(task?.id, task?.stage) {
        if (task != null && !task.isWorking) {
            DocumentRecognitionManager.acknowledgeVisibleTask()
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(DocumentRecognitionManager::selectPdf)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = ShirohaSpacing.Xl,
                top = ShirohaSpacing.Sm,
                end = ShirohaSpacing.Xl,
                bottom = ShirohaSpacing.Xxl
            ),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "OCR",
            title = "文档识别",
            subtitle = "使用 MinerU 在线解析 PDF，核对文本后再进入题库导入。"
        )

        message?.let {
            NoticeCard(
                text = it,
                warning = it.contains("失败") || it.contains("无法") || it.contains("超过") || it.contains("无效")
            )
        }

        DocumentFileCard(
            document = document,
            preparing = DocumentRecognitionManager.isPreparingDocument,
            enabled = !isLocked,
            onChoose = {
                DocumentRecognitionManager.clearUiMessage()
                if (task == null) {
                    filePicker.launch(arrayOf("application/pdf"))
                } else {
                    showReplaceTaskDialog = true
                }
            }
        )

        RecognitionSettingsCard(
            settings = settings,
            enabled = !isLocked,
            preciseTokenConfigured = DocumentRecognitionManager.preciseTokenConfigured,
            tokenDraft = tokenDraft,
            tokenVisible = tokenVisible,
            showAdvanced = showAdvanced,
            onSettingsChange = DocumentRecognitionManager::updateSettings,
            onTokenDraftChange = { tokenDraft = it.take(4_096) },
            onTokenVisibilityChange = { tokenVisible = !tokenVisible },
            onSaveToken = {
                if (DocumentRecognitionManager.savePreciseToken(tokenDraft)) tokenDraft = ""
            },
            onClearToken = {
                DocumentRecognitionManager.clearPreciseToken()
                tokenDraft = ""
            },
            onToggleAdvanced = { showAdvanced = !showAdvanced }
        )

        if (task == null) {
            ActionPillButton(
                icon = Icons.Rounded.CloudUpload,
                text = "开始在线识别",
                primary = true,
                enabled = document != null && !DocumentRecognitionManager.isPreparingDocument &&
                    (settings.mode == MinerUMode.FREE || DocumentRecognitionManager.preciseTokenConfigured),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                fillWidthContent = true,
                onClick = { showPrivacyDialog = true }
            )
        } else {
            RecognitionTaskCard(task = task)
            RecognitionTaskActions(task = task, enabled = !isLocked)
        }

        if (task?.stage == DocumentTaskStage.READY) {
            RecognitionResultCard(
                text = resultText,
                hasImageReferences = task.hasImageReferences,
                onTextChange = DocumentRecognitionManager::updateResultText,
                onUseForImport = {
                    if (DocumentRecognitionManager.prepareImportDraft(resultText)) {
                        onUseResultForImport()
                    }
                }
            )
        }

        NoticeCard(
            text = "隐私提示：PDF 会上传至 MinerU 服务器处理。请勿上传含敏感、涉密或无权处理的文档。Token 仅加密保存在本机，不进入题库备份。",
            warning = false
        )

        ActionPillButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            text = "返回",
            primary = false,
            modifier = Modifier.height(42.dp),
            onClick = onBack
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("确认上传 PDF？") },
            text = {
                Text(
                    "文件“${document?.fileName.orEmpty()}”将上传至 MinerU 在线解析。识别结果只会先进入本地预览，不会自动创建或覆盖题库。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPrivacyDialog = false
                        DocumentRecognitionManager.startRecognition()
                    }
                ) { Text("同意并开始") }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("取消") }
            }
        )
    }

    if (showReplaceTaskDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceTaskDialog = false },
            title = { Text("更换 PDF？") },
            text = { Text("选择新的 PDF 后，当前本地识别任务及结果会被清除；MinerU 服务器上的任务无法由 App 取消。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReplaceTaskDialog = false
                        filePicker.launch(arrayOf("application/pdf"))
                    }
                ) { Text("继续选择") }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceTaskDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DocumentFileCard(
    document: SelectedDocument?,
    preparing: Boolean,
    enabled: Boolean,
    onChoose: () -> Unit
) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PDF 文件",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when {
                        preparing -> "正在复制文件并读取页数……"
                        document == null -> "选择需要在线识别的 PDF"
                        else -> document.fileName
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (document != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(text = formatDocumentSize(document.sizeBytes), selected = true)
                StatusChip(text = if (document.pageCount > 0) "${document.pageCount} 页" else "页数待检测")
            }
        }
        Spacer(Modifier.height(14.dp))
        ActionPillButton(
            icon = Icons.Rounded.FileOpen,
            text = if (document == null) "选择 PDF" else "更换 PDF",
            primary = false,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            fillWidthContent = true,
            onClick = onChoose
        )
    }
}

@Composable
private fun RecognitionSettingsCard(
    settings: MinerUSettings,
    enabled: Boolean,
    preciseTokenConfigured: Boolean,
    tokenDraft: String,
    tokenVisible: Boolean,
    showAdvanced: Boolean,
    onSettingsChange: (MinerUSettings) -> Unit,
    onTokenDraftChange: (String) -> Unit,
    onTokenVisibilityChange: () -> Unit,
    onSaveToken: () -> Unit,
    onClearToken: () -> Unit,
    onToggleAdvanced: () -> Unit
) {
    GlassCard {
        Text("识别设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecognitionChoiceTile(
                title = "免费模式",
                desc = "免 Token · 10MB / 20页",
                selected = settings.mode == MinerUMode.FREE,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onSettingsChange(settings.copy(mode = MinerUMode.FREE)) }
            )
            RecognitionChoiceTile(
                title = "精准模式",
                desc = "Token · 200MB / 200页",
                selected = settings.mode == MinerUMode.PRECISE,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onSettingsChange(settings.copy(mode = MinerUMode.PRECISE)) }
            )
        }

        Spacer(Modifier.height(14.dp))
        Text("PDF 类型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecognitionChoiceTile(
                title = "文字 PDF",
                desc = "已有可选中文本",
                selected = !settings.isOcr,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onSettingsChange(settings.copy(isOcr = false)) }
            )
            RecognitionChoiceTile(
                title = "扫描件 OCR",
                desc = "页面主要是图片",
                selected = settings.isOcr,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onSettingsChange(settings.copy(isOcr = true)) }
            )
        }

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = settings.pageRange,
            onValueChange = { onSettingsChange(settings.copy(pageRange = it)) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("页码范围（可选）") },
            placeholder = {
                Text(if (settings.mode == MinerUMode.FREE) "例如 1-20" else "例如 2,4-10")
            },
            supportingText = {
                Text(
                    if (settings.mode == MinerUMode.FREE) {
                        "免费模式仅支持单页或连续范围。"
                    } else {
                        "精准模式支持逗号分隔的页码范围。"
                    }
                )
            }
        )

        if (settings.mode == MinerUMode.PRECISE) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (preciseTokenConfigured) "MinerU Token · 已安全保存" else "MinerU Token",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (preciseTokenConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tokenDraft,
                onValueChange = onTokenDraftChange,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                placeholder = { Text(if (preciseTokenConfigured) "留空表示继续使用已保存 Token" else "粘贴 API 管理页创建的 Token") },
                trailingIcon = {
                    IconButton(onClick = onTokenVisibilityChange) {
                        Icon(
                            imageVector = if (tokenVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (tokenVisible) "隐藏 Token" else "显示 Token"
                        )
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPillButton(
                    icon = Icons.Rounded.Save,
                    text = "保存 Token",
                    primary = false,
                    enabled = enabled && tokenDraft.isNotBlank(),
                    modifier = Modifier.height(42.dp),
                    onClick = onSaveToken
                )
                if (preciseTokenConfigured) {
                    ActionPillButton(
                        icon = Icons.Rounded.Delete,
                        text = "清除",
                        primary = false,
                        enabled = enabled,
                        modifier = Modifier.height(42.dp),
                        onClick = onClearToken
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onToggleAdvanced,
            enabled = enabled,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                imageVector = if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null
            )
            Spacer(Modifier.width(4.dp))
            Text(if (showAdvanced) "收起高级设置" else "高级设置")
        }
        if (showAdvanced) {
            RecognitionSwitchRow(
                title = "识别表格",
                desc = "复杂表格建议使用精准模式。",
                checked = settings.enableTable,
                enabled = enabled,
                onCheckedChange = { onSettingsChange(settings.copy(enableTable = it)) }
            )
            RecognitionSwitchRow(
                title = "识别公式",
                desc = "保留公式的 Markdown / LaTeX 结构。",
                checked = settings.enableFormula,
                enabled = enabled,
                onCheckedChange = { onSettingsChange(settings.copy(enableFormula = it)) }
            )
            Spacer(Modifier.height(8.dp))
            Text("主要语言", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageTile("中英", "ch", settings, enabled, Modifier.weight(1f), onSettingsChange)
                LanguageTile("英文", "en", settings, enabled, Modifier.weight(1f), onSettingsChange)
                LanguageTile("日文", "japan", settings, enabled, Modifier.weight(1f), onSettingsChange)
            }
            if (settings.mode == MinerUMode.PRECISE) {
                Spacer(Modifier.height(12.dp))
                Text("识别模型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinerUModelVersion.entries.forEach { model ->
                        RecognitionChoiceTile(
                            title = model.displayName,
                            desc = if (model == MinerUModelVersion.VLM) "复杂版式优先" else "常规文档",
                            selected = settings.modelVersion == model,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            onClick = { onSettingsChange(settings.copy(modelVersion = model)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecognitionTaskCard(task: DocumentRecognitionTask) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (task.stage == DocumentTaskStage.READY) Icons.Rounded.CheckCircle else Icons.Rounded.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("识别任务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusChip(
                text = if (task.mode == MinerUMode.FREE) "免费" else "精准",
                selected = true
            )
        }
        Spacer(Modifier.height(14.dp))
        if (task.isWorking) {
            val determinate = task.totalPages > 0
            LinearProgressIndicator(
                progress = if (determinate) {
                    (task.extractedPages.toFloat() / task.totalPages.toFloat()).coerceIn(0f, 1f)
                } else {
                    when (task.stage) {
                        DocumentTaskStage.REQUESTING_UPLOAD -> 0.08f
                        DocumentTaskStage.UPLOADING -> 0.24f
                        DocumentTaskStage.QUEUED -> 0.40f
                        DocumentTaskStage.RUNNING -> 0.62f
                        DocumentTaskStage.DOWNLOADING -> 0.90f
                        else -> 0f
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }
        Text(
            text = task.message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.stage == DocumentTaskStage.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        task.errorMessage?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (task.pageRange.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("识别范围：${task.pageRange}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecognitionTaskActions(task: DocumentRecognitionTask, enabled: Boolean = true) {
    when (task.stage) {
        DocumentTaskStage.QUEUED,
        DocumentTaskStage.RUNNING -> ActionPillButton(
            icon = Icons.Rounded.Pause,
            text = "暂停查询",
            primary = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            fillWidthContent = true,
            enabled = enabled,
            onClick = DocumentRecognitionManager::pausePolling
        )
        DocumentTaskStage.PAUSED -> ActionPillButton(
            icon = Icons.Rounded.PlayArrow,
            text = "继续查询",
            primary = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            fillWidthContent = true,
            enabled = enabled,
            onClick = DocumentRecognitionManager::resumePolling
        )
        DocumentTaskStage.FAILED -> ActionPillButton(
            icon = Icons.Rounded.Refresh,
            text = "重新提交",
            primary = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            fillWidthContent = true,
            enabled = enabled,
            onClick = DocumentRecognitionManager::retryRecognition
        )
        DocumentTaskStage.READY -> ActionPillButton(
            icon = Icons.Rounded.Refresh,
            text = "重新识别此 PDF",
            primary = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            fillWidthContent = true,
            enabled = enabled,
            onClick = DocumentRecognitionManager::retryRecognition
        )
        else -> Unit
    }
}

@Composable
private fun RecognitionResultCard(
    text: String,
    hasImageReferences: Boolean,
    onTextChange: (String) -> Unit,
    onUseForImport: () -> Unit
) {
    GlassCard {
        Text("识别文本预览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "这里仍是草稿。请先核对题号、选项、答案和解析，再交给现有导入解析器。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasImageReferences) {
            Spacer(Modifier.height(10.dp))
            NoticeCard(
                text = "识别结果包含图片引用。本阶段只导入文字并保留图片位置提示；图片下载、调整和题目绑定将在下一补丁实现。",
                warning = true
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 520.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            label = { Text("可编辑识别结果") }
        )
        Spacer(Modifier.height(12.dp))
        ActionPillButton(
            icon = Icons.Rounded.PlayArrow,
            text = "使用此文本导入",
            primary = true,
            enabled = text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            fillWidthContent = true,
            onClick = onUseForImport
        )
    }
}

@Composable
private fun RecognitionChoiceTile(
    title: String,
    desc: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.shirohaNoRippleClickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Md),
        color = if (selected) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardWhite78,
        border = BorderStroke(
            ShirohaDimens.Hairline,
            if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecognitionSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shirohaNoRippleClickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageTile(
    title: String,
    value: String,
    settings: MinerUSettings,
    enabled: Boolean,
    modifier: Modifier,
    onSettingsChange: (MinerUSettings) -> Unit
) {
    Surface(
        modifier = modifier.shirohaNoRippleClickable(enabled = enabled) {
            onSettingsChange(settings.copy(language = value))
        },
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (settings.language == value) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardWhite78,
        border = BorderStroke(
            ShirohaDimens.Hairline,
            if (settings.language == value) ShirohaColors.LineSelected else ShirohaColors.LineSoft
        )
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (settings.language == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

private fun formatDocumentSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
