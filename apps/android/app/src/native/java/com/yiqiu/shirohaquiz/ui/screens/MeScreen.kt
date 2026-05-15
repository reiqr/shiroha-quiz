package com.yiqiu.shirohaquiz.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.ai.ShirohaAiClient
import com.yiqiu.shirohaquiz.state.QuizBank
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.IllustrationHeroCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeScreen(
    onOpenWrongBook: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenPreference: () -> Unit,
    onOpenAiSettings: () -> Unit,
    onOpenDataManagement: () -> Unit,
    onOpenStandardFormat: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Me",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(ShirohaSpacing.Sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设置与资料",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { QuizRepository.setDarkThemeEnabled(context, !QuizRepository.darkThemeEnabled) }
                ) {
                    Icon(
                        imageVector = if (QuizRepository.darkThemeEnabled) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (QuizRepository.darkThemeEnabled) "切换浅色模式" else "切换暗夜模式",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        IllustrationHeroCard(
            title = "我是Shiroha",
            subtitle = "欢迎关注我的一点一点进步",
            imageRes = R.drawable.illus_home_welcome_webp,
            modifier = Modifier.height(ShirohaDimens.HeroCardHeight),
            imageSize = ShirohaDimens.HeroImageSize
        )

        GlassCard {
            Text(
                text = "功能与档案",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            FeaturePlanStrip(
                icon = Icons.Rounded.Settings,
                title = "个人偏好",
                desc = "外观、开屏和练习行为。",
                onClick = onOpenPreference
            )
            Spacer(Modifier.height(10.dp))
            FeaturePlanStrip(
                icon = Icons.Rounded.AutoAwesome,
                title = "AI 设置",
                desc = "接口、AI重构、AI核对、AI解析和处理限制。",
                onClick = onOpenAiSettings
            )
            Spacer(Modifier.height(10.dp))
            FeaturePlanStrip(
                icon = Icons.Rounded.Save,
                title = "数据管理",
                desc = "导入、导出、备份和清除本地数据。",
                onClick = onOpenDataManagement
            )
            Spacer(Modifier.height(10.dp))
            FeaturePlanStrip(
                icon = Icons.Rounded.AutoStories,
                title = "标准导入格式",
                desc = "查看题库文本、答案和解析的推荐写法。",
                onClick = onOpenStandardFormat
            )
            Spacer(Modifier.height(10.dp))
            FeaturePlanStrip(
                icon = Icons.Rounded.Description,
                title = "关于 Shiroha Quiz",
                desc = "项目地址、版本说明和开源信息。",
                onClick = onOpenAbout
            )
        }
    }
}


@Composable
fun DataManagementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf<String?>(null) }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showBankExportDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedBankIds by remember { mutableStateOf(QuizRepository.banks.map { it.id }.toSet()) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val bytes = pendingExportBytes
        if (uri != null && bytes != null) {
            val ok = writeBytesToUri(context, uri, bytes)
            statusText = if (ok) "已导出 ZIP 备份到所选位置。" else "导出失败：无法写入文件。"
        }
        pendingExportBytes = null
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = readBytesFromUri(context, uri)
        val fileName = uri.lastPathSegment.orEmpty()
        statusText = if (bytes == null || bytes.isEmpty()) {
            "导入失败：没有读取到有效内容。"
        } else {
            QuizRepository.importBackupBytes(context, fileName, bytes)
        }
        selectedBankIds = QuizRepository.banks.map { it.id }.toSet()
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Data",
            title = "数据管理",
            subtitle = "管理本地题库导入、导出、备份和清除。"
        )

        GlassCard {
            Text(
                text = "题库与备份",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DataActionTile(
                    icon = Icons.Rounded.FileOpen,
                    title = "导入题库",
                    desc = "导入 ZIP / JSON",
                    modifier = Modifier.weight(1f),
                    onClick = { importBackupLauncher.launch(arrayOf("application/zip", "application/json", "text/*", "*/*")) }
                )
                DataActionTile(
                    icon = Icons.Rounded.Description,
                    title = "导出题库",
                    desc = "多选导出",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedBankIds = QuizRepository.banks.map { it.id }.toSet()
                        showBankExportDialog = true
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DataActionTile(
                    icon = Icons.Rounded.Save,
                    title = "导出全部",
                    desc = "完整备份",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        pendingExportBytes = QuizRepository.exportFullBackupZip()
                        createDocumentLauncher.launch("shiroha_full_backup_${backupTimeStamp()}.zip")
                    }
                )
                DataActionTile(
                    icon = Icons.Rounded.Delete,
                    title = "清除数据",
                    desc = "需二次确认",
                    modifier = Modifier.weight(1f),
                    warning = true,
                    onClick = { showClearDialog = true }
                )
            }

            statusText?.let { message ->
                Spacer(Modifier.height(10.dp))
                NoticeCard(text = message, warning = message.contains("失败") || message.contains("清除"))
            }
        }

        BackToSettingsButton(onBack = onBack)
    }

    if (showBankExportDialog) {
        BankExportDialog(
            banks = QuizRepository.banks,
            selectedBankIds = selectedBankIds,
            onSelectedChange = { selectedBankIds = it },
            onDismiss = { showBankExportDialog = false },
            onExport = {
                if (selectedBankIds.isEmpty()) {
                    statusText = "请至少选择一个题库。"
                } else {
                    pendingExportBytes = QuizRepository.exportBanksBackupZip(selectedBankIds)
                    createDocumentLauncher.launch("shiroha_selected_banks_${backupTimeStamp()}.zip")
                }
                showBankExportDialog = false
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清除本地全部数据？") },
            text = { Text("这会删除本地题库、错题本、学习记录和当前状态。操作不可撤销，建议先导出全部数据。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        QuizRepository.clearAllLocalData(context)
                        selectedBankIds = emptySet()
                        statusText = "已清除本地全部数据。"
                        showClearDialog = false
                    }
                ) { Text("确认清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun BankExportDialog(
    banks: List<QuizBank>,
    selectedBankIds: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择要导出的题库") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (banks.isEmpty()) {
                    Text("当前没有可导出的题库。")
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectedChange(if (selectedBankIds.size == banks.size) emptySet() else banks.map { it.id }.toSet())
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedBankIds.size == banks.size,
                            onCheckedChange = { checked ->
                                onSelectedChange(if (checked) banks.map { it.id }.toSet() else emptySet())
                            }
                        )
                        Text("全选")
                    }
                    banks.forEach { bank ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectedChange(
                                        if (bank.id in selectedBankIds) selectedBankIds - bank.id else selectedBankIds + bank.id
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = bank.id in selectedBankIds,
                                onCheckedChange = { checked ->
                                    onSelectedChange(if (checked) selectedBankIds + bank.id else selectedBankIds - bank.id)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bank.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${bank.questions.size} 题",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onExport, enabled = selectedBankIds.isNotEmpty()) { Text("导出选中") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DataActionTile(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    warning: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = ShirohaColors.CardWhite68,
        border = BorderStroke(ShirohaDimens.Hairline, if (warning) ShirohaColors.StateWarningSoft else ShirohaColors.LineSoft)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (warning) ShirohaColors.IconWarning else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
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
    }
}

@Composable
fun PersonalPreferenceScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Preference",
            title = "个人偏好",
            subtitle = ""
        )

        GlassCard {
            Text(
                text = "外观设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            ThemeChoiceRow(
                darkThemeEnabled = QuizRepository.darkThemeEnabled,
                onThemeChange = { enabled -> QuizRepository.setDarkThemeEnabled(context, enabled) }
            )
        }

        GlassCard {
            Text(
                text = "启动与练习",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "保留开屏启动图片",
                desc = "下次启动时显示学习主题开屏图。",
                checked = QuizRepository.startupSplashEnabled,
                onCheckedChange = { enabled -> QuizRepository.setStartupSplashEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "限制练习下一题",
                desc = "提交答案或查看解析后，才能进入下一题。",
                checked = QuizRepository.practiceNextRequiresResult,
                onCheckedChange = { enabled -> QuizRepository.setPracticeNextRequiresResult(context, enabled) }
            )
        }

        BackToSettingsButton(onBack = onBack)
    }
}


@Composable
fun AiSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "AI",
            title = "AI 设置",
            subtitle = "管理 AI重构、AI核对、AI解析和接口配置"
        )

        AiSettingsPanel(context = context)

        BackToSettingsButton(onBack = onBack)
    }
}


@Composable
private fun AiSettingsPanel(context: Context) {
    var provider by remember { mutableStateOf(QuizRepository.aiProvider) }
    var apiBaseUrl by remember {
        mutableStateOf(QuizRepository.aiApiBaseUrl.ifBlank { aiProviderPreset(QuizRepository.aiProvider)?.apiBaseUrl.orEmpty() })
    }
    var apiKey by remember { mutableStateOf(QuizRepository.aiApiKey) }
    var modelName by remember {
        mutableStateOf(QuizRepository.aiModelName.ifBlank { aiProviderPreset(QuizRepository.aiProvider)?.modelName.orEmpty() })
    }
    var maxQuestions by remember { mutableStateOf(QuizRepository.aiMaxQuestions.toString()) }
    var timeoutSeconds by remember { mutableStateOf(QuizRepository.aiTimeoutSeconds.toString()) }
    var refactorMaxChars by remember { mutableStateOf(QuizRepository.aiRefactorMaxChars.toString()) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var statusWarning by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    val aiScope = rememberCoroutineScope()

    GlassCard {
        Text(
            text = "接口配置",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        AiProviderChoiceRow(
            selectedProvider = provider,
            onProviderChange = { selected ->
                provider = selected
                aiProviderPreset(selected)?.let { preset ->
                    if (shouldReplaceAiEndpoint(apiBaseUrl)) apiBaseUrl = preset.apiBaseUrl
                    if (shouldReplaceAiModel(modelName)) modelName = preset.modelName
                    statusText = "已切换到 $selected，并填入推荐地址和模型；如使用代理或兼容服务，可继续手动修改。"
                    statusWarning = false
                } ?: run {
                    statusText = "已切换到自定义接口，请按服务商要求填写 API 地址和模型名称。"
                    statusWarning = false
                }
            }
        )
        aiProviderPreset(provider)?.let { preset ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = preset.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = apiBaseUrl,
            onValueChange = { apiBaseUrl = it },
            label = { Text("API 地址") },
            placeholder = { Text("https://api.example.com/v1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            placeholder = { Text("sk-...") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it },
            label = { Text("模型名称") },
            placeholder = { Text("deepseek-chat / gpt-4o-mini / 自定义模型") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(ShirohaSpacing.Sm)) {
            TextButton(
                onClick = {
                    QuizRepository.setAiInterfaceConfig(
                        context = context,
                        provider = provider,
                        apiBaseUrl = apiBaseUrl,
                        apiKey = apiKey,
                        modelName = modelName
                    )
                    statusText = "AI 接口配置已保存。"
                    statusWarning = false
                }
            ) {
                Text("保存配置")
            }
            TextButton(
                enabled = !isTestingConnection,
                onClick = {
                    if (apiBaseUrl.isBlank() || apiKey.isBlank() || modelName.isBlank()) {
                        statusText = "请先填写 API 地址、API Key 和模型名称。"
                        statusWarning = true
                        return@TextButton
                    }
                    isTestingConnection = true
                    statusText = "正在测试 AI 接口连接……"
                    statusWarning = false
                    aiScope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                ShirohaAiClient.testConnection(
                                    apiBaseUrl = apiBaseUrl,
                                    apiKey = apiKey,
                                    modelName = modelName,
                                    timeoutSeconds = timeoutSeconds.toIntOrNull() ?: QuizRepository.aiTimeoutSeconds
                                )
                            }
                        }
                        result.onSuccess { message ->
                            statusText = message.ifBlank { "AI 接口连接成功。" }
                            statusWarning = false
                        }.onFailure { error ->
                            statusText = "AI 接口连接失败：${error.message ?: "请检查配置"}"
                            statusWarning = true
                        }
                        isTestingConnection = false
                    }
                }
            ) {
                Text(if (isTestingConnection) "测试中" else "测试连接")
            }
            TextButton(
                onClick = {
                    QuizRepository.clearAiConfig(context)
                    apiBaseUrl = ""
                    apiKey = ""
                    modelName = ""
                    statusText = "AI 配置已清除。"
                    statusWarning = true
                }
            ) {
                Text("清除配置")
            }
        }

        statusText?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (statusWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "AI 辅助策略",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        PreferenceSwitchRow(
            title = "启用 AI 重构",
            desc = "题量异常、切题混乱时，根据原文重整待核对结果。",
            checked = QuizRepository.aiRefactorEnabled,
            onCheckedChange = { enabled -> QuizRepository.setAiRefactorEnabled(context, enabled) }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceSwitchRow(
            title = "启用 AI 核对",
            desc = "检查题型、答案、选项和解析异常。",
            checked = QuizRepository.aiReviewEnabled,
            onCheckedChange = { enabled -> QuizRepository.setAiReviewEnabled(context, enabled) }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceSwitchRow(
            title = "启用 AI 解析",
            desc = "优先为缺少解析或解析过短的题目生成建议。",
            checked = QuizRepository.aiAnalysisEnabled,
            onCheckedChange = { enabled -> QuizRepository.setAiAnalysisEnabled(context, enabled) }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceSwitchRow(
            title = "仅处理异常题",
            desc = "减少请求数量，优先处理导入异常和疑似错误。",
            checked = QuizRepository.aiOnlyAnomaly,
            onCheckedChange = { enabled -> QuizRepository.setAiOnlyAnomaly(context, enabled) }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceSwitchRow(
            title = "结果需人工确认",
            desc = "AI 结果只作为建议，不直接写入题库。",
            checked = QuizRepository.aiRequireConfirm,
            onCheckedChange = { enabled -> QuizRepository.setAiRequireConfirm(context, enabled) }
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "成本与安全",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(ShirohaSpacing.Sm)) {
            OutlinedTextField(
                value = maxQuestions,
                onValueChange = { value -> maxQuestions = value.filter { it.isDigit() }.take(3) },
                label = { Text("单次题数") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = timeoutSeconds,
                onValueChange = { value -> timeoutSeconds = value.filter { it.isDigit() }.take(3) },
                label = { Text("超时秒数") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = refactorMaxChars,
            onValueChange = { value -> refactorMaxChars = value.filter { it.isDigit() }.take(5) },
            label = { Text("AI重构原文上限") },
            placeholder = { Text("30000") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = {
                QuizRepository.setAiProcessingLimits(
                    context = context,
                    maxQuestions = maxQuestions.toIntOrNull() ?: QuizRepository.aiMaxQuestions,
                    timeoutSeconds = timeoutSeconds.toIntOrNull() ?: QuizRepository.aiTimeoutSeconds
                )
                QuizRepository.setAiRefactorMaxChars(
                    context = context,
                    maxChars = refactorMaxChars.toIntOrNull() ?: QuizRepository.aiRefactorMaxChars
                )
                maxQuestions = QuizRepository.aiMaxQuestions.toString()
                timeoutSeconds = QuizRepository.aiTimeoutSeconds.toString()
                refactorMaxChars = QuizRepository.aiRefactorMaxChars.toString()
                statusText = "AI 处理限制已保存。"
                statusWarning = false
            }
        ) {
            Text("保存处理限制")
        }
        Spacer(Modifier.height(8.dp))
        NoticeCard(
            text = "AI 功能会消耗接口额度。AI 重构会优先清洗原文并重新本地解析，必要时才使用 AI 直接重构题目；原文不足上限时会全部发送。AI 核对只生成核对提示；AI 解析会写入待核对解析，最终仍需手动保存题库。",
            warning = false
        )
    }
}

@Composable
private fun AiProviderChoiceRow(
    selectedProvider: String,
    onProviderChange: (String) -> Unit
) {
    val providers = listOf("DeepSeek", "OpenAI 兼容", "自定义接口")
    Column(verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(ShirohaSpacing.Sm)) {
            providers.take(2).forEach { provider ->
                ThemeChoiceTile(
                    icon = Icons.Rounded.AutoAwesome,
                    title = provider,
                    selected = selectedProvider == provider,
                    modifier = Modifier.weight(1f),
                    onClick = { onProviderChange(provider) }
                )
            }
        }
        ThemeChoiceTile(
            icon = Icons.Rounded.Settings,
            title = providers.last(),
            selected = selectedProvider == providers.last(),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onProviderChange(providers.last()) }
        )
    }
}

private data class AiProviderPreset(
    val apiBaseUrl: String,
    val modelName: String,
    val hint: String
)

private fun aiProviderPreset(provider: String): AiProviderPreset? = when (provider) {
    "DeepSeek" -> AiProviderPreset(
        apiBaseUrl = "https://api.deepseek.com",
        modelName = "deepseek-chat",
        hint = "DeepSeek 推荐地址：https://api.deepseek.com；默认模型：deepseek-chat。"
    )
    "OpenAI 兼容" -> AiProviderPreset(
        apiBaseUrl = "https://api.openai.com/v1",
        modelName = "gpt-4o-mini",
        hint = "OpenAI 兼容接口通常填写到 /v1，程序会自动拼接 /chat/completions。"
    )
    else -> null
}

private fun shouldReplaceAiEndpoint(value: String): Boolean {
    val clean = value.trim().trimEnd('/')
    return clean.isBlank() || clean in setOf(
        "https://api.example.com/v1",
        "https://api.deepseek.com",
        "https://api.deepseek.com/v1",
        "https://api.openai.com/v1"
    )
}

private fun shouldReplaceAiModel(value: String): Boolean {
    val clean = value.trim()
    return clean.isBlank() || clean in setOf("deepseek-chat", "gpt-4o-mini")
}


@Composable
private fun ThemeChoiceRow(
    darkThemeEnabled: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ShirohaSpacing.Sm)
    ) {
        ThemeChoiceTile(
            icon = Icons.Rounded.LightMode,
            title = "浅色模式",
            selected = !darkThemeEnabled,
            modifier = Modifier.weight(1f),
            onClick = { onThemeChange(false) }
        )
        ThemeChoiceTile(
            icon = Icons.Rounded.DarkMode,
            title = "暗夜模式",
            selected = darkThemeEnabled,
            modifier = Modifier.weight(1f),
            onClick = { onThemeChange(true) }
        )
    }
}

@Composable
private fun ThemeChoiceTile(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Md),
        color = if (selected) ShirohaColors.BrandPrimarySoft else ShirohaColors.CardWhite78,
        border = BorderStroke(ShirohaDimens.Hairline, if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun PreferenceInfoRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun FeaturePlanStrip(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun BackToSettingsButton(
    onBack: () -> Unit
) {
    ActionPillButton(
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        text = "返回设置",
        primary = false,
        modifier = Modifier.height(42.dp),
        onClick = onBack
    )
}

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()
}

private fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(bytes)
            output.flush()
        } ?: return false
        true
    }.getOrDefault(false)
}

private fun backupTimeStamp(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}
