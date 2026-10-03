from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8-sig")


def write(path: str, text: str) -> None:
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"pattern not found: {label}")
    return text.replace(old, new, 1)


# 1) 设置页：移除“文档识别”入口，但保留 OCR 页面本身。
me_path = "apps/android/app/src/native/java/com/yiqiu/shirohaquiz/ui/screens/MeScreen.kt"
me = read(me_path)
me = me.replace("import androidx.compose.material.icons.rounded.DocumentScanner\n", "", 1)
me = replace_once(
    me,
    "    onOpenDocumentRecognition: () -> Unit,\n",
    "",
    "MeScreen onOpenDocumentRecognition parameter",
)
me = replace_once(
    me,
    '''            Spacer(Modifier.height(10.dp))
            FeaturePlanStrip(
                icon = Icons.Rounded.DocumentScanner,
                title = "文档识别",
                desc = "使用在线 OCR 解析 PDF。",
                onClick = onOpenDocumentRecognition
            )
''',
    "",
    "MeScreen document recognition card",
)
write(me_path, me)


# 2) AppShell：只删除传给 MeScreen 的 OCR 回调；ImportScreen 继续保留。
shell_path = "apps/android/app/src/native/java/com/yiqiu/shirohaquiz/ui/app/ShirohaAppShell.kt"
shell = read(shell_path)
marker = "MainTab.Me -> MeScreen("
idx = shell.find(marker)
if idx < 0:
    raise RuntimeError("MeScreen call not found in ShirohaAppShell")
head, tail = shell[:idx], shell[idx:]
target = "                            onOpenDocumentRecognition = { navigateTo(MainTab.DocumentRecognition) },\n"
if target not in tail:
    raise RuntimeError("MeScreen OCR callback not found in ShirohaAppShell")
tail = tail.replace(target, "", 1)
shell = head + tail
write(shell_path, shell)


# 3) 导入页：移除“填入示例”，把“在线解析 PDF”移动到右侧按钮位。
import_path = "apps/android/app/src/native/java/com/yiqiu/shirohaquiz/ui/screens/ImportScreen.kt"
imp = read(import_path)
imp = imp.replace("import androidx.compose.material.icons.rounded.Refresh\n", "", 1)
old_buttons = '''                ActionPillButton(
                    icon = Icons.Rounded.Refresh,
                    text = "填入示例",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (!isImportBusy) {
                            useDualImport = false
                            selectedFileName = "示例题库"
                            rawText = sampleImportText()
                            rawTextEditorExpanded = true
                            answerTextEditorExpanded = true
                            importedImages = emptyList()
                            clearParsedResult()
                            statusText = "已填入示例题库。"
                            isStatusWarn = false
                        }
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            ActionPillButton(
                icon = Icons.Rounded.DocumentScanner,
                text = "在线解析 PDF",
                primary = false,
                enabled = !isImportBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                fillWidthContent = true,
                onClick = onOpenDocumentRecognition
            )
'''
new_buttons = '''                ActionPillButton(
                    icon = Icons.Rounded.DocumentScanner,
                    text = "在线解析 PDF",
                    primary = false,
                    enabled = !isImportBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = onOpenDocumentRecognition
                )
            }
'''
imp = replace_once(imp, old_buttons, new_buttons, "ImportScreen import buttons")
imp, sample_count = re.subn(
    r'\nprivate fun sampleImportText\(\): String = """.*?"""\.trimIndent\(\)\n',
    "\n",
    imp,
    count=1,
    flags=re.S,
)
if sample_count != 1:
    raise RuntimeError(f"sampleImportText removal count={sample_count}")
write(import_path, imp)


# 4) 当前用户文档同步入口描述，避免仍写“我的 -> 文档识别”。
doc_impl_path = "docs/native/MinerU与WebDAV功能实施记录.md"
doc_impl = read(doc_impl_path)
doc_impl = doc_impl.replace(
    "- 文档识别：我的 → 文档识别；导入页 → 在线解析 PDF。",
    "- 文档识别：导入页 → 在线解析 PDF。",
)
write(doc_impl_path, doc_impl)

guide_path = "docs/题库导入策略与使用指南/Shiroha Quiz 题库导入策略与使用指南.md"
guide = read(guide_path)
guide = guide.replace(
    "1. 原生版进入 **我的 → 文档识别**，或在导入页选择在线解析 PDF。",
    "1. 原生版在导入页选择 **在线解析 PDF**。",
)
write(guide_path, guide)


# 5) 将现有仓库用户文档和 Excel 模板打包进原生 APK assets。
assets_root = ROOT / "apps/android/app/src/native/assets"
assets_docs = assets_root / "docs"
assets_templates = assets_root / "templates"
assets_docs.mkdir(parents=True, exist_ok=True)
assets_templates.mkdir(parents=True, exist_ok=True)

asset_sources = {
    "docs/标准题库格式示例/标准题库格式示例.md": assets_docs / "standard_format.md",
    "docs/题库导入格式支持说明/Shiroha_Quiz_题库导入格式支持说明.md": assets_docs / "import_format_support.md",
    "docs/题库导入策略与使用指南/Shiroha Quiz 题库导入策略与使用指南.md": assets_docs / "import_strategy_guide.md",
    "docs/题目导入解析方法说明/Shiroha Quiz 题目导入解析方法说明.md": assets_docs / "import_parser_notes.md",
}
for src, dst in asset_sources.items():
    src_path = ROOT / src
    if not src_path.exists():
        raise RuntimeError(f"asset source missing: {src}")
    text = src_path.read_text(encoding="utf-8-sig")
    dst.write_text(text, encoding="utf-8")

template_src = ROOT / "docs/标准题库格式示例/Shiroha_Quiz_Excel题库导入通用模板.xlsx"
if not template_src.exists():
    raise RuntimeError("Excel template source missing")
shutil.copy2(template_src, assets_templates / "Shiroha_Quiz_Excel题库导入通用模板.xlsx")


# 6) 标准导入格式页：改为简要说明 + 本地 Markdown 文档阅读 + 本地 Excel 模板导出。
standard_path = "apps/android/app/src/native/java/com/yiqiu/shirohaquiz/ui/screens/StandardImportFormatScreen.kt"
standard = r'''package com.yiqiu.shirohaquiz.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
private const val TEMPLATE_ASSET = "templates/Shiroha_Quiz_Excel题库导入通用模板.xlsx"
private const val TEMPLATE_FILE_NAME = "Shiroha_Quiz_Excel题库导入通用模板.xlsx"

private data class LocalDocSpec(
    val title: String,
    val desc: String,
    val assetPath: String
)

private val LOCAL_IMPORT_DOCS = listOf(
    LocalDocSpec(
        title = "标准题库格式示例",
        desc = "单选、多选、判断、填空、简答等标准写法与完整示例。",
        assetPath = "docs/standard_format.md"
    ),
    LocalDocSpec(
        title = "题库导入格式支持说明",
        desc = "查看 TXT、JSON、CSV、XLSX、DOCX 等格式的支持范围。",
        assetPath = "docs/import_format_support.md"
    ),
    LocalDocSpec(
        title = "题库导入策略与使用指南",
        desc = "不同来源题库应该使用哪一种导入方式。",
        assetPath = "docs/import_strategy_guide.md"
    ),
    LocalDocSpec(
        title = "题目导入解析方法说明",
        desc = "了解题号、题型、答案、解析与复杂文本的识别规则。",
        assetPath = "docs/import_parser_notes.md"
    )
)

@Composable
fun StandardImportFormatScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var openedDoc by remember { mutableStateOf<LocalDocSpec?>(null) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    val templateExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XLSX_MIME)
    ) { uri ->
        if (uri != null) {
            exportStatus = if (copyAssetToUri(context, TEMPLATE_ASSET, uri)) {
                "Excel 通用模板已保存到所选位置。"
            } else {
                "模板保存失败，请重新选择保存位置后再试。"
            }
        }
    }

    val activeDoc = openedDoc
    if (activeDoc != null) {
        BackHandler { openedDoc = null }
        LocalMarkdownDocumentScreen(
            spec = activeDoc,
            onBack = { openedDoc = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Format",
            title = "标准导入格式",
            subtitle = "常用规则直接看，详细说明与模板均已随 App 离线提供。"
        )

        GlassCard {
            Text(
                text = "快速说明",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "• 推荐每道题独立成块，并保留清晰题号。\n" +
                    "• 选择题保留 A. B. C. D. 等选项标记。\n" +
                    "• 答案单独写成“答案：A / ABC / 正确”等形式。\n" +
                    "• 有解析时单独写“解析：……”，没有解析可以省略。\n" +
                    "• 扫描 PDF 请从导入页使用“在线解析 PDF”。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GlassCard {
            Text(
                text = "详细文档",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "以下 Markdown 文档保存在 App 本地，不需要访问 GitHub，也不需要联网。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LOCAL_IMPORT_DOCS.forEachIndexed { index, spec ->
                Text(
                    text = spec.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                ActionPillButton(
                    icon = if (index == 0) Icons.Rounded.Article else Icons.Rounded.Description,
                    text = spec.title,
                    primary = index == 0,
                    modifier = Modifier.fillMaxWidth(),
                    fillWidthContent = true,
                    onClick = { openedDoc = spec }
                )
                if (index != LOCAL_IMPORT_DOCS.lastIndex) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        GlassCard {
            Text(
                text = "Excel 通用模板",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "模板同样内置在 App 中。点击后选择手机中的保存位置即可导出，无需联网下载。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            ActionPillButton(
                icon = Icons.Rounded.TableChart,
                text = "导出 Excel 通用模板",
                primary = false,
                modifier = Modifier.fillMaxWidth(),
                fillWidthContent = true,
                onClick = { templateExporter.launch(TEMPLATE_FILE_NAME) }
            )
            exportStatus?.let { status ->
                Spacer(Modifier.height(10.dp))
                NoticeCard(
                    text = status,
                    warning = status.contains("失败")
                )
            }
        }

        ActionPillButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            text = "返回设置",
            primary = false,
            modifier = Modifier.height(42.dp),
            onClick = onBack
        )
    }
}

@Composable
private fun LocalMarkdownDocumentScreen(
    spec: LocalDocSpec,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val documentResult = remember(spec.assetPath) {
        runCatching {
            context.assets.open(spec.assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }
    val documentText = documentResult.getOrNull()
    val blocks = remember(documentText) {
        documentText?.let(::parseMarkdown).orEmpty()
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Docs",
            title = spec.title,
            subtitle = "本地离线文档"
        )

        if (documentText == null) {
            NoticeCard(
                text = "文档读取失败：${documentResult.exceptionOrNull()?.message ?: "未知错误"}",
                warning = true
            )
        } else {
            GlassCard {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        blocks.forEach { block ->
                            MarkdownBlockView(block)
                        }
                    }
                }
            }
        }

        ActionPillButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            text = "返回标准导入格式",
            primary = false,
            modifier = Modifier.height(42.dp),
            onClick = onBack
        )
    }
}

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class ListItem(val text: String) : MarkdownBlock()
    data class Code(val text: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

private fun parseMarkdown(source: String): List<MarkdownBlock> {
    val lines = source.replace("\r\n", "\n").replace('\r', '\n').lines()
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    var index = 0
    var inCode = false
    val code = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(
                paragraph.joinToString("\n") { cleanMarkdownInline(it.trim()) }.trim()
            )
            paragraph.clear()
        }
    }

    fun flushCode() {
        if (code.isNotEmpty()) {
            blocks += MarkdownBlock.Code(code.joinToString("\n").trimEnd())
            code.clear()
        }
    }

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            if (inCode) {
                flushCode()
                inCode = false
            } else {
                inCode = true
            }
            index += 1
            continue
        }

        if (inCode) {
            code += line
            index += 1
            continue
        }

        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            flushParagraph()
            val table = mutableListOf<String>()
            while (index < lines.size) {
                val tableLine = lines[index].trim()
                if (!tableLine.startsWith("|") || !tableLine.endsWith("|")) break
                table += tableLine
                index += 1
            }
            blocks += MarkdownBlock.Code(table.joinToString("\n"))
            continue
        }

        when {
            trimmed.isBlank() -> flushParagraph()
            trimmed.matches(Regex("^-{3,}$")) -> {
                flushParagraph()
                blocks += MarkdownBlock.Divider
            }
            trimmed.startsWith("### ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(3, cleanMarkdownInline(trimmed.removePrefix("### ")))
            }
            trimmed.startsWith("## ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(2, cleanMarkdownInline(trimmed.removePrefix("## ")))
            }
            trimmed.startsWith("# ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(1, cleanMarkdownInline(trimmed.removePrefix("# ")))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                blocks += MarkdownBlock.ListItem("• ${cleanMarkdownInline(trimmed.drop(2))}")
            }
            trimmed.matches(Regex("^\\d+[.)]\\s+.*")) -> {
                flushParagraph()
                blocks += MarkdownBlock.ListItem(cleanMarkdownInline(trimmed))
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Paragraph("› ${cleanMarkdownInline(trimmed.removePrefix("> "))}")
            }
            else -> paragraph += line
        }
        index += 1
    }

    flushParagraph()
    flushCode()
    return blocks
}

private fun cleanMarkdownInline(value: String): String {
    return value
        .replace(Regex("!\\[([^]]*)]\\([^)]+\\)"), "$1")
        .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .trim()
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock) {
    when (block) {
        is MarkdownBlock.Heading -> Text(
            text = block.text,
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        is MarkdownBlock.Paragraph -> Text(
            text = block.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        is MarkdownBlock.ListItem -> Text(
            text = block.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        is MarkdownBlock.Code -> GlassCard {
            Text(
                text = block.text,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        MarkdownBlock.Divider -> Text(
            text = "────────────",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun copyAssetToUri(
    context: Context,
    assetPath: String,
    uri: Uri
): Boolean {
    return runCatching {
        val output = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
        output.use { target ->
            context.assets.open(assetPath).use { source ->
                source.copyTo(target)
            }
        }
        true
    }.getOrDefault(false)
}
'''
write(standard_path, standard)

print("native import/settings/docs update applied")
