from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KT = ROOT / "apps/android/app/src/native/java/com/yiqiu/shirohaquiz/ui/screens/StandardImportFormatScreen.kt"
STANDARD = ROOT / "apps/android/app/src/native/assets/docs/standard_format.md"
FORMAT_SUPPORT = ROOT / "apps/android/app/src/native/assets/docs/import_format_support.md"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"pattern not found: {label}")
    return text.replace(old, new, 1)


text = KT.read_text(encoding="utf-8-sig")

# Imports used by the richer local Markdown renderer.
import_anchor = "import androidx.compose.foundation.layout.Column\n"
extra_imports = """import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
"""
if "import androidx.compose.foundation.lazy.LazyColumn\n" not in text:
    text = replace_once(text, import_anchor, import_anchor + extra_imports, "foundation imports")

runtime_anchor = "import androidx.compose.runtime.remember\n"
runtime_imports = "import androidx.compose.runtime.rememberCoroutineScope\n"
if runtime_imports not in text:
    text = replace_once(text, runtime_anchor, runtime_anchor + runtime_imports, "runtime imports")

ui_anchor = "import androidx.compose.ui.platform.LocalContext\n"
ui_imports = """import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
"""
if "import androidx.compose.ui.platform.LocalUriHandler\n" not in text:
    text = replace_once(text, ui_anchor, ui_anchor + ui_imports, "ui imports")

font_anchor = "import androidx.compose.ui.text.font.FontFamily\n"
font_imports = "import androidx.compose.ui.text.font.FontStyle\n"
if font_imports not in text:
    text = replace_once(text, font_anchor, font_anchor + font_imports, "font imports")

style_anchor = "import androidx.compose.ui.text.font.FontWeight\n"
style_imports = "import androidx.compose.ui.text.style.TextDecoration\n"
if style_imports not in text:
    text = replace_once(text, style_anchor, style_anchor + style_imports, "style imports")

unit_anchor = "import androidx.compose.ui.unit.dp\n"
coroutines_import = "import kotlinx.coroutines.launch\n"
if coroutines_import not in text:
    text = replace_once(text, unit_anchor, unit_anchor + coroutines_import, "coroutines import")

# Cross-document links inside the local docs should stay inside the app.
text = replace_once(
    text,
    """        LocalMarkdownDocumentScreen(
            spec = activeDoc,
            onBack = { openedDoc = null }
        )
""",
    """        LocalMarkdownDocumentScreen(
            spec = activeDoc,
            onBack = { openedDoc = null },
            onOpenDoc = { openedDoc = it }
        )
""",
    "local markdown screen call",
)

start_marker = "@Composable\nprivate fun LocalMarkdownDocumentScreen("
end_marker = "private fun copyAssetToUri("
start = text.find(start_marker)
end = text.find(end_marker, start)
if start < 0 or end < 0:
    raise RuntimeError("markdown renderer section markers not found")

new_section = r'''@Composable
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
'''

text = text[:start] + new_section + "\n\n" + text[end:]
KT.write_text(text, encoding="utf-8")

# Keep the App-local user docs aligned with the native import support actually exposed to users.
standard = STANDARD.read_text(encoding="utf-8-sig")
standard = standard.replace("Excel `.xlsx` / `.xls`", "Excel `.xlsx` / `.csv`")
STANDARD.write_text(standard, encoding="utf-8")

format_support = FORMAT_SUPPORT.read_text(encoding="utf-8-sig")
format_support = format_support.replace(
    "原生版支持直接导入 `.xlsx` / `.xls` 文件。解析器会自动识别表格中的列，提取题干、选项、答案和解析。",
    "原生版支持直接导入 `.xlsx` / `.csv` 文件。旧版 `.xls` / `.xlsm` 请先在 Excel 或 WPS 中另存为 `.xlsx` 或 `.csv`；解析器会自动识别表格中的列，提取题干、选项、答案和解析。",
)
FORMAT_SUPPORT.write_text(format_support, encoding="utf-8")
