package com.yiqiu.shirohaquiz.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
            onBack = { openedDoc = null },
            onOpenDoc = { openedDoc = it }
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
    onBack: () -> Unit,
    onOpenDoc: (LocalDocSpec) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val documentResult = remember(spec.assetPath) {
        runCatching {
            context.assets.open(spec.assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }
    val documentText = documentResult.getOrNull()
    val parsed = remember(documentText) {
        documentText?.let(::parseMarkdown) ?: ParsedMarkdown(emptyList(), emptyMap())
    }

    fun openMarkdownLink(target: String) {
        val raw = target.trim()
        if (raw.isBlank()) return
        val path = raw.substringBefore('#')
        val anchor = raw.substringAfter('#', "").ifBlank { null }
        val linkedDoc = resolveLocalDocSpec(path)

        when {
            anchor != null && (path.isBlank() || linkedDoc == spec) -> {
                val blockIndex = parsed.anchors[anchor] ?: return
                scope.launch {
                    // item 0 is the document header.
                    listState.animateScrollToItem(blockIndex + 1)
                }
            }
            linkedDoc != null -> onOpenDoc(linkedDoc)
            raw.startsWith("https://") || raw.startsWith("http://") -> {
                runCatching { uriHandler.openUri(raw) }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.padding(
            horizontal = ShirohaSpacing.Xl,
            vertical = ShirohaSpacing.Sm
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ShirohaHeader(
                kicker = "Docs",
                title = spec.title,
                subtitle = "本地离线文档"
            )
        }

        if (documentText == null) {
            item {
                NoticeCard(
                    text = "文档读取失败：${documentResult.exceptionOrNull()?.message ?: "未知错误"}",
                    warning = true
                )
            }
        } else {
            itemsIndexed(parsed.blocks) { _, block ->
                MarkdownBlockView(
                    block = block,
                    onLink = ::openMarkdownLink
                )
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                text = "返回标准导入格式",
                primary = false,
                modifier = Modifier.height(42.dp),
                onClick = onBack
            )
        }
    }
}

private data class ParsedMarkdown(
    val blocks: List<MarkdownBlock>,
    val anchors: Map<String, Int>
)

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class ListItem(val text: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    data class Code(val text: String) : MarkdownBlock()
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

private val INLINE_TOKEN = Regex(
    """(!?\[[^]]*]\([^)]+\)|\*\*[^*]+\*\*|__[^_]+__|`[^`]+`)"""
)

private val HTML_ANCHOR = Regex(
    """^<(?:span|a)\s+(?:id|name)=["']([^"']+)["'][^>]*>(?:\s*</(?:span|a)>)?\s*$""",
    RegexOption.IGNORE_CASE
)

private fun parseMarkdown(source: String): ParsedMarkdown {
    val lines = source.replace("\r\n", "\n").replace('\r', '\n').lines()
    val blocks = mutableListOf<MarkdownBlock>()
    val anchors = linkedMapOf<String, Int>()
    val paragraph = mutableListOf<String>()
    val code = mutableListOf<String>()
    var index = 0
    var inCode = false

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(
                paragraph.joinToString("\n") { it.trim() }.trim()
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

        val anchorId = HTML_ANCHOR.matchEntire(trimmed)?.groupValues?.getOrNull(1)
        if (!anchorId.isNullOrBlank()) {
            flushParagraph()
            anchors[anchorId] = blocks.size
            index += 1
            continue
        }

        if (isMarkdownTableStart(lines, index)) {
            flushParagraph()
            val headers = splitMarkdownTableRow(lines[index])
            index += 2 // Skip the header separator row.
            val rows = mutableListOf<List<String>>()
            while (index < lines.size) {
                val tableLine = lines[index].trim()
                if (!tableLine.startsWith("|") || !tableLine.endsWith("|")) break
                rows += splitMarkdownTableRow(lines[index])
                index += 1
            }
            blocks += MarkdownBlock.Table(headers = headers, rows = rows)
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
                blocks += MarkdownBlock.Heading(3, plainMarkdownText(trimmed.removePrefix("### ")))
            }
            trimmed.startsWith("## ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(2, plainMarkdownText(trimmed.removePrefix("## ")))
            }
            trimmed.startsWith("# ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(1, plainMarkdownText(trimmed.removePrefix("# ")))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                blocks += MarkdownBlock.ListItem("• ${trimmed.drop(2)}")
            }
            trimmed.matches(Regex("^\\d+[.)]\\s+.*")) -> {
                flushParagraph()
                blocks += MarkdownBlock.ListItem(trimmed)
            }
            trimmed.startsWith(">") -> {
                flushParagraph()
                blocks += MarkdownBlock.Quote(trimmed.removePrefix(">").trimStart())
            }
            else -> paragraph += line
        }
        index += 1
    }

    flushParagraph()
    flushCode()
    return ParsedMarkdown(blocks = blocks, anchors = anchors)
}

private fun isMarkdownTableStart(lines: List<String>, index: Int): Boolean {
    if (index + 1 >= lines.size) return false
    val header = lines[index].trim()
    val separator = lines[index + 1].trim()
    if (!header.startsWith("|") || !header.endsWith("|")) return false
    if (!separator.startsWith("|") || !separator.endsWith("|")) return false
    val separatorCells = splitMarkdownTableRow(separator)
    return separatorCells.isNotEmpty() && separatorCells.all { cell ->
        cell.replace(" ", "").matches(Regex("^:?-{3,}:?$"))
    }
}

private fun splitMarkdownTableRow(line: String): List<String> {
    return line.trim().trim('|').split('|').map { it.trim() }
}

private fun plainMarkdownText(value: String): String {
    return value
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("!\\[([^]]*)]\\([^)]+\\)"), "$1")
        .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .trim()
}

private fun resolveLocalDocSpec(target: String): LocalDocSpec? {
    if (target.isBlank()) return null
    val normalized = target.lowercase()
    return when {
        "standard_format.md" in normalized || "标准题库格式示例" in target -> LOCAL_IMPORT_DOCS[0]
        "import_format_support.md" in normalized || "题库导入格式支持说明" in target -> LOCAL_IMPORT_DOCS[1]
        "import_strategy_guide.md" in normalized || "题库导入策略与使用指南" in target -> LOCAL_IMPORT_DOCS[2]
        "import_parser_notes.md" in normalized || "题目导入解析方法说明" in target -> LOCAL_IMPORT_DOCS[3]
        else -> null
    }
}

@Composable
private fun MarkdownBlockView(
    block: MarkdownBlock,
    onLink: (String) -> Unit
) {
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
        is MarkdownBlock.Paragraph -> MarkdownInlineText(
            source = block.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onLink = onLink
        )
        is MarkdownBlock.ListItem -> MarkdownInlineText(
            source = block.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onLink = onLink
        )
        is MarkdownBlock.Quote -> GlassCard {
            MarkdownInlineText(
                source = block.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onLink = onLink
            )
        }
        is MarkdownBlock.Code -> GlassCard {
            SelectionContainer {
                Text(
                    text = block.text,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        is MarkdownBlock.Table -> MarkdownTableView(block, onLink)
        MarkdownBlock.Divider -> Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
        )
    }
}

@Composable
private fun MarkdownInlineText(
    source: String,
    style: TextStyle,
    color: Color,
    onLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val annotated = remember(source, linkColor, codeBackground) {
        buildMarkdownAnnotatedText(
            source = source,
            linkColor = linkColor,
            codeBackground = codeBackground
        )
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style.copy(color = color),
        onClick = { offset ->
            annotated.getStringAnnotations(
                tag = "markdown-link",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                onLink(annotation.item)
            }
        }
    )
}

private fun buildMarkdownAnnotatedText(
    source: String,
    linkColor: Color,
    codeBackground: Color
): AnnotatedString {
    val normalized = source
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace("&nbsp;", " ")
    return buildAnnotatedString {
        var cursor = 0
        INLINE_TOKEN.findAll(normalized).forEach { match ->
            if (match.range.first > cursor) {
                appendWithoutHtml(normalized.substring(cursor, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("![") -> {
                    val split = token.indexOf("](")
                    append(if (split > 2) token.substring(2, split) else "")
                }
                token.startsWith("[") -> {
                    val split = token.indexOf("](")
                    if (split > 1) {
                        val label = token.substring(1, split)
                        val target = token.substring(split + 2, token.length - 1)
                        pushStringAnnotation("markdown-link", target)
                        pushStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                        append(label)
                        pop()
                        pop()
                    } else {
                        append(token)
                    }
                }
                token.startsWith("**") && token.endsWith("**") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                    append(token.substring(2, token.length - 2))
                    pop()
                }
                token.startsWith("__") && token.endsWith("__") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                    append(token.substring(2, token.length - 2))
                    pop()
                }
                token.startsWith("`") && token.endsWith("`") -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground
                        )
                    )
                    append(token.substring(1, token.length - 1))
                    pop()
                }
                else -> append(token)
            }
            cursor = match.range.last + 1
        }
        if (cursor < normalized.length) {
            appendWithoutHtml(normalized.substring(cursor))
        }
    }
}

private fun AnnotatedString.Builder.appendWithoutHtml(value: String) {
    append(value.replace(Regex("<[^>]+>"), ""))
}

@Composable
private fun MarkdownTableView(
    table: MarkdownBlock.Table,
    onLink: (String) -> Unit
) {
    val columnCount = maxOf(
        table.headers.size,
        table.rows.maxOfOrNull { it.size } ?: 0
    )
    if (columnCount <= 3) {
        CompactMarkdownTable(table = table, onLink = onLink)
    } else {
        WideMarkdownTable(table = table, columnCount = columnCount, onLink = onLink)
    }
}

@Composable
private fun CompactMarkdownTable(
    table: MarkdownBlock.Table,
    onLink: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        table.rows.forEach { row ->
            GlassCard {
                table.headers.forEachIndexed { index, header ->
                    val cell = row.getOrNull(index).orEmpty()
                    if (cell.isNotBlank()) {
                        Text(
                            text = plainMarkdownText(header),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(2.dp))
                        MarkdownInlineText(
                            source = cell,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onLink = onLink
                        )
                        if (index != table.headers.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WideMarkdownTable(
    table: MarkdownBlock.Table,
    columnCount: Int,
    onLink: (String) -> Unit
) {
    val horizontalState = rememberScrollState()
    GlassCard {
        Column(
            modifier = Modifier.horizontalScroll(horizontalState),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MarkdownTableRow(
                cells = table.headers,
                columnCount = columnCount,
                header = true,
                onLink = onLink
            )
            table.rows.forEach { row ->
                MarkdownTableRow(
                    cells = row,
                    columnCount = columnCount,
                    header = false,
                    onLink = onLink
                )
            }
        }
    }
}

@Composable
private fun MarkdownTableRow(
    cells: List<String>,
    columnCount: Int,
    header: Boolean,
    onLink: (String) -> Unit
) {
    Row {
        repeat(columnCount) { index ->
            val cell = cells.getOrNull(index).orEmpty()
            if (header) {
                Text(
                    text = plainMarkdownText(cell),
                    modifier = Modifier
                        .width(142.dp)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                MarkdownInlineText(
                    source = cell,
                    modifier = Modifier
                        .width(142.dp)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onLink = onLink
                )
            }
        }
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
