package com.yiqiu.shirohaquiz.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import com.yiqiu.shirohaquiz.ui.components.shirohaNoRippleClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
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
import com.yiqiu.shirohaquiz.ui.components.ShirohaDangerConfirmDialog
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
    onOpenRecords: () -> Unit,
    onOpenAppearancePreference: () -> Unit,
    onOpenPracticePreference: () -> Unit,
    onOpenWrongBookPreference: () -> Unit,
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
            subtitle = "欢迎关注，喜欢的话请给我⭐️⭐️",
            imageRes = R.drawable.illus_me_settings,
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
                icon = Icons.Rounded.DarkMode,
                title = "外观偏好",
                desc = "主题、卡片动画、Shiroha 模式、开屏图和应用图标。",
                onClick = onOpenAppearancePreference
            )
            Spacer(Modifier.height(10.dp))
            FeaturePlanStrip(
                icon = Icons.Rounded.Tune,
                title = "刷题偏好",
                desc = "默认答题方式、每组题数和切题习惯。",
                onClick = onOpenPracticePreference
            )
            Spacer(Modifier.height(10.dp))
            FeaturePlanStrip(
                icon = Icons.Rounded.FactCheck,
                title = "错题本",
                desc = "错题收录、掌握判定与智能复习。",
                onClick = onOpenWrongBookPreference
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
                icon = Icons.Rounded.Article,
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
                            .shirohaNoRippleClickable {
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        banks.forEach { bank ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shirohaNoRippleClickable {
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
        modifier = modifier.shirohaNoRippleClickable(onClick = onClick),
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
fun AppearancePreferenceScreen(
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
            kicker = "Appearance",
            title = "外观偏好",
            subtitle = "管理主题、卡片动画、Shiroha 模式、开屏图和应用图标。"
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
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "卡片动画",
                desc = "显示卡片轻微淡入上移动画；如果设备性能较弱或遇到页面卡顿，可尝试关闭。",
                checked = QuizRepository.cardAnimationEnabled,
                onCheckedChange = { enabled -> QuizRepository.setCardAnimationEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "Shiroha 模式",
                desc = "显示开屏图、页面插画并切换 Shiroha 图标。",
                checked = QuizRepository.shirohaModeEnabled,
                onCheckedChange = { enabled -> QuizRepository.setShirohaModeEnabled(context, enabled) }
            )
            if (QuizRepository.shirohaModeEnabled) {
                Spacer(Modifier.height(12.dp))
                PreferenceSwitchRow(
                    title = "保留开屏启动图片",
                    desc = "开启后下次启动显示学习主题开屏图。",
                    checked = QuizRepository.startupSplashEnabled,
                    onCheckedChange = { enabled -> QuizRepository.setStartupSplashEnabled(context, enabled) }
                )
            }
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "平板侧边导航",
                desc = "开启后可在较宽屏幕上将底部导航移到左侧。",
                checked = QuizRepository.tabletSideNavigationEnabled,
                onCheckedChange = { enabled -> QuizRepository.setTabletSideNavigationEnabled(context, enabled) }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "提示：部分桌面可能会缓存应用图标，切换后可能需要稍等片刻才刷新。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }




        GlassCard {
            Text(
                text = "阅读显示",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            ReadingSizeChoiceRow(
                title = "题干字号",
                desc = "调整练习和考试中的题干阅读大小。",
                currentMode = QuizRepository.questionFontSizeMode,
                onSelect = { mode -> QuizRepository.setQuestionFontSizeMode(context, mode) }
            )
            Spacer(Modifier.height(14.dp))
            ReadingSizeChoiceRow(
                title = "选项字号",
                desc = "调整练习和考试中的选项文字大小。",
                currentMode = QuizRepository.optionFontSizeMode,
                onSelect = { mode -> QuizRepository.setOptionFontSizeMode(context, mode) }
            )
            Spacer(Modifier.height(14.dp))
            PreferenceSwitchRow(
                title = "紧凑选项模式",
                desc = "减少选项卡片背景和间距，适合长题快速阅读。",
                checked = QuizRepository.compactOptionsEnabled,
                onCheckedChange = { enabled -> QuizRepository.setCompactOptionsEnabled(context, enabled) }
            )
        }

        BackToSettingsButton(onBack = onBack)
    }
}



@Composable
private fun ReadingSizeChoiceRow(
    title: String,
    desc: String,
    currentMode: String,
    onSelect: (String) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = desc,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "small" to "小",
            "standard" to "标准",
            "large" to "大"
        ).forEach { (mode, label) ->
            ActionPillButton(
                icon = Icons.Rounded.Tune,
                text = label,
                primary = currentMode == mode,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                fillWidthContent = true,
                onClick = { onSelect(mode) }
            )
        }
    }
}


@Composable
fun WrongBookPreferenceScreen(
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
            kicker = "Wrong Book",
            title = "错题本",
            subtitle = "管理错题收录、掌握判定和智能复习。"
        )

        WrongBookExplanationCard(
            icon = Icons.Rounded.FactCheck,
            title = "默认错题本逻辑",
            paragraphs = listOf(
                "练习、考试和错题复习中答错的题会自动进入错题本。错题本会保留“错 X 次 / 对 Y 次”的历史累计表现。",
                "连续答对 2 次后，题目会自动标记为已掌握；后续再次答错会清零连续正确次数，并回到未掌握。",
                "背题模式不会加入错题，也不会推进掌握进度。斩题是用户主动移出普通练习池，不等同于已掌握。"
            )
        )

        GlassCard {
            Text(
                text = "错题练习面板",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "错题练习高级设置",
                desc = "开启后可筛选题型并设置单次复习数量；关闭后使用原来的简洁练习面板。",
                checked = QuizRepository.wrongBookAdvancedReviewSettingsEnabled,
                onCheckedChange = { enabled ->
                    QuizRepository.setWrongBookAdvancedReviewSettingsEnabled(context, enabled)
                }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "关闭后仍保留题库范围、掌握筛选、排序和今日复习，并默认复习当前筛选下的全部题型与全部可练习错题。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        WrongBookExplanationCard(
            icon = Icons.Rounded.AutoAwesome,
            title = "智能复习逻辑",
            paragraphs = listOf(
                "开启后，错题本会根据答题表现安排今日待复习题目。未掌握题会更频繁出现，已掌握题会进入较长间隔回顾。",
                "如果复习中再次答错，题目会回到未掌握，并提前进入后续复习；连续答对后会逐步拉长复习间隔。",
                "开启后，错题本会显示今日复习入口，首页学习状态中的复习数量也会切换为同一套今日待复习数量。普通练习和考试仍保持原流程，但答题结果会同步更新错题复习计划。"
            )
        )

        GlassCard {
            Text(
                text = "错题智能复习",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "启用错题智能复习",
                desc = "显示今日复习入口，并同步首页待复习数量。",
                checked = QuizRepository.wrongBookSmartReviewEnabled,
                onCheckedChange = { enabled -> QuizRepository.setWrongBookSmartReviewEnabled(context, enabled) }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "关闭后，错题本仍按原有规则统计与判定掌握。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BackToSettingsButton(onBack = onBack)
    }
}

@Composable
private fun WrongBookExplanationCard(
    icon: ImageVector,
    title: String,
    paragraphs: List<String>
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            paragraphs.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PracticePreferenceScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showDefaultBatchSizeDialog by remember { mutableStateOf(false) }
    var defaultBatchSizeText by remember(QuizRepository.preferredPracticeBatchCustomSize) {
        mutableStateOf(QuizRepository.preferredPracticeBatchCustomSize.coerceAtLeast(1).toString())
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Practice",
            title = "刷题偏好",
            subtitle = "管理默认答题方式、每组题数和练习交互。"
        )

        GlassCard {
            Text(
                text = "刷题体验",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "记住上次练习设置",
                desc = "进入练习页时恢复上次题量、题型、组题方式和答题方式。",
                checked = QuizRepository.rememberPracticeSettingsEnabled,
                onCheckedChange = { enabled -> QuizRepository.setRememberPracticeSettingsEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "记住上次考试设置",
                desc = "进入考试页时恢复上次题量、时长和组题方式。",
                checked = QuizRepository.rememberExamSettingsEnabled,
                onCheckedChange = { enabled -> QuizRepository.setRememberExamSettingsEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "启用滑动切题",
                desc = "在练习页和考试页左滑下一题、右滑上一题。",
                checked = QuizRepository.swipeNavigationEnabled,
                onCheckedChange = { enabled -> QuizRepository.setSwipeNavigationEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "选择后立即判题",
                desc = "仅即时练习生效。单选题和判断题选择后立即显示对错、答案和解析。",
                checked = QuizRepository.practiceAutoSubmitEnabled,
                onCheckedChange = { enabled -> QuizRepository.setPracticeAutoSubmitEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "答对后自动下一题",
                desc = "即时练习答对自动切题，答错停留。",
                checked = QuizRepository.practiceAutoNextEnabled,
                onCheckedChange = { enabled -> QuizRepository.setPracticeAutoNextEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "批量选后自动下一题",
                desc = "单选和判断选后自动切题。",
                checked = QuizRepository.practiceBatchAutoNextEnabled,
                onCheckedChange = { enabled -> QuizRepository.setPracticeBatchAutoNextEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "练习页答题方式切换",
                desc = "显示即时反馈、批量做题和每组题数。",
                checked = QuizRepository.practiceInlineAnswerSettingsEnabled,
                onCheckedChange = { enabled -> QuizRepository.setPracticeInlineAnswerSettingsEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "背题模式",
                desc = "进入练习后直接显示答案和解析，不提交答案，不计入正确率和错题。",
                checked = QuizRepository.practiceReciteModeEnabled,
                onCheckedChange = { enabled -> QuizRepository.setPracticeReciteModeEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "斩题功能",
                desc = "开启后练习页显示斩题按钮，可将已掌握题移出后续练习。",
                checked = QuizRepository.practiceSlashEnabled,
                onCheckedChange = { enabled -> QuizRepository.setPracticeSlashEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "题目快速编辑",
                desc = "练习时可从题目右上角直接修改当前题目。",
                checked = QuizRepository.practiceQuickEditEnabled,
                onCheckedChange = { enabled -> QuizRepository.setPracticeQuickEditEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "练习时打乱选项顺序",
                desc = "固定显示 A/B/C/D，仅随机对应的选项内容；本次练习内顺序固定，不影响题库原始答案。",
                checked = QuizRepository.practiceOptionShuffleEnabled,
                onCheckedChange = { enabled -> QuizRepository.setPracticeOptionShuffleEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            PreferenceSwitchRow(
                title = "考试时打乱选项顺序",
                desc = "固定显示 A/B/C/D，仅随机对应的选项内容；同一场考试内顺序固定，不影响题库原始答案和考试记录。",
                checked = QuizRepository.examOptionShuffleEnabled,
                onCheckedChange = { enabled -> QuizRepository.setExamOptionShuffleEnabled(context, enabled) }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "默认答题方式",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.AutoStories,
                    text = "即时反馈",
                    primary = QuizRepository.preferredPracticeMode == QuizRepository.PRACTICE_MODE_INSTANT,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    fillWidthContent = true,
                    onClick = { QuizRepository.setPreferredPracticeMode(context, QuizRepository.PRACTICE_MODE_INSTANT) }
                )
                ActionPillButton(
                    icon = Icons.Rounded.Description,
                    text = "批量做题",
                    primary = QuizRepository.preferredPracticeMode == QuizRepository.PRACTICE_MODE_BATCH,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    fillWidthContent = true,
                    onClick = { QuizRepository.setPreferredPracticeMode(context, QuizRepository.PRACTICE_MODE_BATCH) }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "默认每组题数",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Rounded.AutoStories,
                    text = "10题",
                    primary = QuizRepository.preferredPracticeBatchSizeMode == "10",
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    fillWidthContent = true,
                    onClick = { QuizRepository.setPreferredPracticeBatchSize(context, "10") }
                )
                ActionPillButton(
                    icon = Icons.Rounded.Description,
                    text = "20题",
                    primary = QuizRepository.preferredPracticeBatchSizeMode == "20",
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    fillWidthContent = true,
                    onClick = { QuizRepository.setPreferredPracticeBatchSize(context, "20") }
                )
                ActionPillButton(
                    icon = Icons.Rounded.Settings,
                    text = "自定义",
                    primary = QuizRepository.preferredPracticeBatchSizeMode == "custom",
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    fillWidthContent = true,
                    onClick = {
                        defaultBatchSizeText = QuizRepository.preferredPracticeBatchCustomSize.coerceAtLeast(1).toString()
                        showDefaultBatchSizeDialog = true
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "批量做题时每组提交一次。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

    if (showDefaultBatchSizeDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultBatchSizeDialog = false },
            title = { Text("自定义默认每组题数") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "请输入批量做题默认每组题数。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = defaultBatchSizeText,
                        onValueChange = { value -> defaultBatchSizeText = value.filter { it.isDigit() }.take(3) },
                        singleLine = true,
                        label = { Text("每组题数") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val count = defaultBatchSizeText.toIntOrNull()?.coerceAtLeast(1) ?: 10
                        QuizRepository.setPreferredPracticeBatchSize(context, "custom", count)
                        showDefaultBatchSizeDialog = false
                    }
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showDefaultBatchSizeDialog = false }) { Text("取消") } }
        )
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
            subtitle = "管理导入核对、题目页分析和接口配置"
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
    var limitStatusText by remember { mutableStateOf<String?>(null) }
    var statusWarning by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showClearAiConfigConfirm by remember { mutableStateOf(false) }
    val aiScope = rememberCoroutineScope()

    if (showClearAiConfigConfirm) {
        ShirohaDangerConfirmDialog(
            title = "确认清除 AI 配置？",
            message = "这会清除当前保存的 API 地址、API Key 和模型名称，并关闭所有 AI 功能开关。清除后需要重新填写才能继续使用 AI。",
            confirmText = "确认清除",
            onDismiss = { showClearAiConfigConfirm = false },
            onConfirm = {
                QuizRepository.clearAiConfig(context)
                apiBaseUrl = ""
                apiKey = ""
                modelName = ""
                statusText = "AI 配置已清除。"
                statusWarning = true
                showClearAiConfigConfirm = false
            }
        )
    }

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
                onClick = { showClearAiConfigConfirm = true }
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
            title = "启用导入页 AI 解析",
            desc = "导入核对阶段，优先为缺少解析或解析过短的题目生成建议。",
            checked = QuizRepository.aiAnalysisEnabled,
            onCheckedChange = { enabled -> QuizRepository.setAiAnalysisEnabled(context, enabled) }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceSwitchRow(
            title = "启用题目页 AI 分析",
            desc = "练习提交后显示单题 AI 分析入口，AI 只给参考答案和解析，不自动修改题库。",
            checked = QuizRepository.aiSingleQuestionAnalysisEnabled,
            onCheckedChange = { enabled -> QuizRepository.setAiSingleQuestionAnalysisEnabled(context, enabled) }
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = "保存范围：单次题数 5–100，超时 15–180 秒，AI重构原文上限 5000–80000 字；超出范围会自动修正。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                limitStatusText = "处理限制已保存：单次 ${QuizRepository.aiMaxQuestions} 题，超时 ${QuizRepository.aiTimeoutSeconds} 秒，AI重构原文上限 ${QuizRepository.aiRefactorMaxChars} 字。"
            }
        ) {
            Text("保存处理限制")
        }
        limitStatusText?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
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
        modifier = modifier.shirohaNoRippleClickable(onClick = onClick),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .shirohaNoRippleClickable { onCheckedChange(!checked) }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .shirohaNoRippleClickable(onClick = onClick)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
