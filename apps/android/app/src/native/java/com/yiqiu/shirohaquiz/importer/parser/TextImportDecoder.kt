package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.util.SafeZipReader
import java.nio.charset.Charset
import java.util.Locale
import java.util.zip.ZipInputStream

object TextImportDecoder {
    private const val MAX_OMML_DEPTH = 32
    private const val WORD_MANUAL_BREAK_MARKER = "\uE000SHIROHA_WORD_MANUAL_BREAK\uE001"

    private const val answerLabelPattern =
        "答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答"
    private const val analysisLabelPattern =
        "答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明"

    private val answerLabelAtLineStartRegex = Regex(
        """^\s*(?:(?:[\[【]\s*(?:$answerLabelPattern)\s*[\]】])|(?:(?:本题)?(?:$answerLabelPattern)\s*(?:[:：]|为|是)))"""
    )
    private val analysisLabelAtLineStartRegex = Regex(
        """^\s*(?:(?:[\[【]\s*(?:$analysisLabelPattern)\s*[\]】])|(?:(?:$analysisLabelPattern)\s*[:：]))"""
    )

    sealed class DecodeResult {
        data class Success(val text: String) : DecodeResult()
        data class Failure(
            val userMessage: String,
            val technicalMessage: String? = null
        ) : DecodeResult()
    }

    private data class RawZipEntry(
        val name: String,
        val bytes: ByteArray
    )

    private enum class ScriptStyle {
        Normal,
        Superscript,
        Subscript
    }

    fun decode(bytes: ByteArray, fileName: String): String? {
        return when (val result = decodeDetailed(bytes, fileName)) {
            is DecodeResult.Success -> result.text
            is DecodeResult.Failure -> null
        }
    }

    fun decodeDetailed(bytes: ByteArray, fileName: String): DecodeResult {
        if (bytes.isEmpty()) return DecodeResult.Success("")
        val lowerName = fileName.lowercase(Locale.ROOT)
        val zipEntriesResult = if (looksLikeZip(bytes)) readZipEntriesResult(bytes) else null
        val zipEntries = zipEntriesResult?.fold(onSuccess = { it }, onFailure = { null })

        return when {
            lowerName.endsWith(".docx") -> decodeDocxDetailed(bytes, zipEntriesResult, zipEntries)
            lowerName.endsWith(".xlsx") -> decodeXlsxDetailed(bytes, zipEntriesResult, zipEntries)
            zipEntries?.any { it.name == "word/document.xml" } == true -> {
                decodeDocxFromEntries(zipEntries).toDecodeResult(
                    emptyMessage = "已读取 Word 文档，但没有找到可用正文内容。"
                )
            }
            zipEntries?.any { it.name.startsWith("xl/worksheets/") } == true -> {
                decodeXlsxFromEntries(zipEntries).toDecodeResult(
                    emptyMessage = "已读取 Excel 文件，但没有找到可用工作表内容。"
                )
            }
            else -> DecodeResult.Success(QuestionTextNormalizer.normalize(decodePlainText(bytes).orEmpty()))
        }
    }

    private fun decodePlainText(bytes: ByteArray): String? {
        val utf8 = bytes.toString(Charsets.UTF_8)
        if ('�' !in utf8) return utf8
        return try {
            bytes.toString(Charset.forName("GB18030"))
        } catch (_: Exception) {
            utf8
        }
    }

    private fun looksLikeZip(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 'P'.code.toByte() &&
            bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 3.toByte() &&
            bytes[3] == 4.toByte()
    }

    private fun decodeDocxDetailed(
        bytes: ByteArray,
        zipEntriesResult: Result<List<RawZipEntry>>?,
        zipEntries: List<RawZipEntry>?
    ): DecodeResult {
        if (zipEntriesResult?.isFailure == true) {
            return DecodeResult.Failure(
                userMessage = "文件无法读取，可能已损坏。请重新导出或另存为 docx 后再试。",
                technicalMessage = zipEntriesResult.exceptionOrNull()?.message
            )
        }
        if (!looksLikeZip(bytes)) {
            return DecodeResult.Failure("这个文件不是标准 docx 文档。请用 Word 或 WPS 另存为 docx 后再导入。")
        }
        val entries = zipEntries ?: return readZipEntriesResult(bytes)
            .fold(
                onSuccess = { readEntries ->
                    if (readEntries.isEmpty()) {
                        DecodeResult.Failure("文件无法读取，可能已损坏。请重新导出或另存为 docx 后再试。")
                    } else {
                        decodeDocxFromEntries(readEntries).toDecodeResult(
                            emptyMessage = "这个 docx 结构不完整，缺少可读取的正文内容。"
                        )
                    }
                },
                onFailure = { error ->
                    DecodeResult.Failure(
                        userMessage = "文件无法读取，可能已损坏。请重新导出或另存为 docx 后再试。",
                        technicalMessage = error.message
                    )
                }
            )
        if (entries.isEmpty()) {
            return DecodeResult.Failure("文件无法读取，可能已损坏。请重新导出或另存为 docx 后再试。")
        }
        return decodeDocxFromEntries(entries).toDecodeResult(
            emptyMessage = "这个 docx 结构不完整，缺少可读取的正文内容。"
        )
    }

    private fun decodeDocxFromEntries(entries: List<RawZipEntry>): String? {
        val documentXml = entries.firstOrNull { it.name == "word/document.xml" }?.bytes?.toString(Charsets.UTF_8)
            ?: return null
        return extractTextFromWordXml(documentXml)
    }

    private fun decodeXlsxDetailed(
        bytes: ByteArray,
        zipEntriesResult: Result<List<RawZipEntry>>?,
        zipEntries: List<RawZipEntry>?
    ): DecodeResult {
        if (zipEntriesResult?.isFailure == true) {
            return DecodeResult.Failure(
                userMessage = "文件无法读取，可能已损坏。请重新导出或另存为 xlsx 后再试。",
                technicalMessage = zipEntriesResult.exceptionOrNull()?.message
            )
        }
        if (!looksLikeZip(bytes)) {
            return DecodeResult.Failure("这个文件不是标准 xlsx 表格。请用 Excel 或 WPS 另存为 xlsx 后再导入。")
        }
        val entries = zipEntries ?: return readZipEntriesResult(bytes)
            .fold(
                onSuccess = { readEntries ->
                    if (readEntries.isEmpty()) {
                        DecodeResult.Failure("文件无法读取，可能已损坏。请重新导出或另存为 xlsx 后再试。")
                    } else {
                        decodeXlsxFromEntries(readEntries).toDecodeResult(
                            emptyMessage = "这个 xlsx 没有可读取的工作表内容。"
                        )
                    }
                },
                onFailure = { error ->
                    DecodeResult.Failure(
                        userMessage = "文件无法读取，可能已损坏。请重新导出或另存为 xlsx 后再试。",
                        technicalMessage = error.message
                    )
                }
            )
        if (entries.isEmpty()) {
            return DecodeResult.Failure("文件无法读取，可能已损坏。请重新导出或另存为 xlsx 后再试。")
        }
        return decodeXlsxFromEntries(entries).toDecodeResult(
            emptyMessage = "这个 xlsx 没有可读取的工作表内容。"
        )
    }

    private fun decodeXlsxFromEntries(entries: List<RawZipEntry>): String? {
        val sharedStrings = entries.firstOrNull { it.name == "xl/sharedStrings.xml" }
            ?.bytes
            ?.toString(Charsets.UTF_8)
            ?.let(::extractSpreadsheetSharedStrings)
            .orEmpty()

        val sheets = entries
            .filter { it.name.startsWith("xl/worksheets/") && it.name.endsWith(".xml") }
            .sortedWith(compareBy<RawZipEntry> { sheetOrder(it.name) }.thenBy { it.name })

        if (sheets.isEmpty()) return null

        return sheets
            .mapNotNull { entry ->
                extractSpreadsheetRows(entry.bytes.toString(Charsets.UTF_8), sharedStrings)
                    .takeIf { it.isNotBlank() }
            }
            .joinToString("\n\n")
            .takeIf { it.isNotBlank() }
    }

    internal fun extractTextFromWordXml(xml: String): String {
        val paragraphs = Regex("""<w:p\b[\s\S]*?</w:p>""")
            .findAll(xml)
            .map { extractTextFromWordParagraphXml(it.value).trimEnd() }
            .filter { it.isNotBlank() }
            .toList()

        val text = if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n")
        } else {
            extractPlainTextFromXml(xml)
        }

        return text
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    internal fun extractTextFromWordParagraphXml(xml: String): String {
        val tokenRegex = Regex(
            """<m:oMathPara\b[\s\S]*?</m:oMathPara>|<m:oMath\b[\s\S]*?</m:oMath>|<w:r\b[\s\S]*?</w:r>|<w:tab\s*/>|<w:(?:br|cr)\b[^>]*/>|<w:t\b[^>]*>[\s\S]*?</w:t>"""
        )
        val matches = tokenRegex.findAll(xml).toList()
        if (matches.isEmpty()) return extractPlainTextFromXml(xml)

        val extracted = buildString {
            matches.forEach { match ->
                val token = match.value
                when {
                    token.startsWith("<w:tab") -> append('\t')
                    token.startsWith("<w:br") || token.startsWith("<w:cr") -> append(extractWordBreak(token))
                    token.startsWith("<m:oMath") -> append(extractOmmlText(token))
                    token.startsWith("<w:r") -> append(extractWordRunText(token))
                    token.startsWith("<w:t") -> append(extractTextTagValue(token))
                }
            }
        }.replace(Regex("""[ \t]{2,}"""), " ")

        return restoreAnswerAnalysisManualBreaks(extracted)
    }

    private fun extractWordBreak(token: String): String {
        if (Regex("""<w:br\s*/>""").matches(token)) return "\n"
        val isTextWrappingBreak = token.startsWith("<w:cr") ||
            Regex("""\bw:type\s*=\s*["']textWrapping["']""").containsMatchIn(token)
        return if (isTextWrappingBreak) WORD_MANUAL_BREAK_MARKER else ""
    }

    private fun restoreAnswerAnalysisManualBreaks(text: String): String {
        if (WORD_MANUAL_BREAK_MARKER !in text) return text
        val parts = text.split(WORD_MANUAL_BREAK_MARKER)
        return buildString(text.length) {
            append(parts.first())
            parts.drop(1).forEach { rightPart ->
                val leftLine = toString().substringAfterLast('\n').trimStart()
                val rightLine = rightPart.substringBefore('\n').trimStart()
                if (answerLabelAtLineStartRegex.containsMatchIn(leftLine) &&
                    analysisLabelAtLineStartRegex.containsMatchIn(rightLine)
                ) {
                    append('\n')
                }
                append(rightPart)
            }
        }
    }

    private fun extractWordRunText(xml: String): String {
        if ("<m:oMath" in xml || "<m:oMathPara" in xml) return extractOmmlText(xml)
        val script = detectVertAlign(xml)
        val tokenRegex = Regex("""<w:tab\s*/>|<w:(?:br|cr)\b[^>]*/>|<(?:w|m):t\b[^>]*>[\s\S]*?</(?:w|m):t>""")
        return buildString {
            tokenRegex.findAll(xml).forEach { match ->
                val token = match.value
                when {
                    token.startsWith("<w:tab") -> append('\t')
                    token.startsWith("<w:br") || token.startsWith("<w:cr") -> append(extractWordBreak(token))
                    else -> append(convertScript(extractTextTagValue(token), script))
                }
            }
        }
    }

    private fun extractOmmlText(xml: String, depth: Int = 0): String {
        if (depth > MAX_OMML_DEPTH) return extractPlainTextFromXml(xml)
        var working = xml
        working = replaceOmmlScriptBlock(working, "sSubSup") { block ->
            val base = extractOmmlChildText(block, "e", depth + 1)
            val sub = convertScript(extractOmmlChildText(block, "sub", depth + 1), ScriptStyle.Subscript)
            val sup = convertScript(extractOmmlChildText(block, "sup", depth + 1), ScriptStyle.Superscript)
            base + sub + sup
        }
        working = replaceOmmlScriptBlock(working, "sSup") { block ->
            val base = extractOmmlChildText(block, "e", depth + 1)
            val sup = convertScript(extractOmmlChildText(block, "sup", depth + 1), ScriptStyle.Superscript)
            base + sup
        }
        working = replaceOmmlScriptBlock(working, "sSub") { block ->
            val base = extractOmmlChildText(block, "e", depth + 1)
            val sub = convertScript(extractOmmlChildText(block, "sub", depth + 1), ScriptStyle.Subscript)
            base + sub
        }
        working = Regex("""<m:f\b[\s\S]*?</m:f>""").replace(working) { match ->
            val numerator = extractOmmlChildText(match.value, "num", depth + 1)
            val denominator = extractOmmlChildText(match.value, "den", depth + 1)
            if (numerator.isNotBlank() && denominator.isNotBlank()) "($numerator)/($denominator)" else extractPlainTextFromXml(match.value)
        }
        working = Regex("""<m:rad\b[\s\S]*?</m:rad>""").replace(working) { match ->
            val degree = extractOmmlChildText(match.value, "deg", depth + 1)
            val radicand = extractOmmlChildText(match.value, "e", depth + 1)
            when {
                degree.isNotBlank() && radicand.isNotBlank() -> "${degree}\u221A($radicand)"
                radicand.isNotBlank() -> "\u221A($radicand)"
                else -> extractPlainTextFromXml(match.value)
            }
        }
        working = Regex("""<m:func\b[\s\S]*?</m:func>""").replace(working) { match ->
            formatOmmlFunction(match.value, depth + 1).ifBlank { extractPlainTextFromXml(match.value) }
        }
        working = Regex("""<m:nary\b[\s\S]*?</m:nary>""").replace(working) { match ->
            val operator = extractOmmlNaryOperator(match.value).ifBlank { "∑" }
            val sub = extractOmmlChildText(match.value, "sub", depth + 1)
            val sup = extractOmmlChildText(match.value, "sup", depth + 1)
            val expression = extractOmmlChildText(match.value, "e", depth + 1)
            buildString {
                append(operator)
                if (sub.isNotBlank()) append(convertScript(sub, ScriptStyle.Subscript))
                if (sup.isNotBlank()) append(convertScript(sup, ScriptStyle.Superscript))
                if (expression.isNotBlank()) append(expression)
            }.ifBlank { extractPlainTextFromXml(match.value) }
        }
        working = Regex("""<m:limLow\b[\s\S]*?</m:limLow>""").replace(working) { match ->
            val base = extractOmmlChildText(match.value, "e", depth + 1)
            val limit = extractOmmlChildText(match.value, "lim", depth + 1)
            when {
                base.isNotBlank() && limit.isNotBlank() -> base + convertScript(limit, ScriptStyle.Subscript)
                base.isNotBlank() -> base
                else -> extractPlainTextFromXml(match.value)
            }
        }
        working = Regex("""<m:limUpp\b[\s\S]*?</m:limUpp>""").replace(working) { match ->
            val base = extractOmmlChildText(match.value, "e", depth + 1)
            val limit = extractOmmlChildText(match.value, "lim", depth + 1)
            when {
                base.isNotBlank() && limit.isNotBlank() -> base + convertScript(limit, ScriptStyle.Superscript)
                base.isNotBlank() -> base
                else -> extractPlainTextFromXml(match.value)
            }
        }
        working = Regex("""<m:m\b[\s\S]*?</m:m>""").replace(working) { match ->
            formatOmmlMatrix(match.value, depth + 1).ifBlank { extractPlainTextFromXml(match.value) }
        }
        working = Regex("""<m:acc\b[\s\S]*?</m:acc>""").replace(working) { match ->
            formatOmmlAccent(match.value, depth + 1).ifBlank { extractPlainTextFromXml(match.value) }
        }
        working = Regex("""<m:bar\b[\s\S]*?</m:bar>""").replace(working) { match ->
            formatOmmlBar(match.value, depth + 1).ifBlank { extractPlainTextFromXml(match.value) }
        }
        working = Regex("""<m:d\b[\s\S]*?</m:d>""").replace(working) { match ->
            formatOmmlDelimiter(match.value, depth + 1).ifBlank { extractPlainTextFromXml(match.value) }
        }
        working = Regex("""<m:chr\b[^>]*(?:m:val|val)="([^"]+)"[^>]*/?>""").replace(working) { match ->
            decodeXmlEntities(match.groupValues[1])
        }
        return extractPlainTextFromXml(working)
    }


    private fun extractOmmlNaryOperator(xml: String): String {
        return Regex("""<m:chr\b[^>]*(?:m:val|val)="([^"]+)"[^>]*/?>""")
            .find(xml)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::decodeXmlEntities)
            .orEmpty()
    }

    private fun formatOmmlDelimiter(xml: String, depth: Int = 0): String {
        val content = extractOmmlChildText(xml, "e", depth + 1)
        if (content.isBlank()) return ""
        val begin = extractOmmlControlValue(xml, "begChr").ifBlank { "(" }
        val end = extractOmmlControlValue(xml, "endChr").ifBlank { matchingEndDelimiter(begin) }
        return begin + content + end
    }

    private fun formatOmmlMatrix(xml: String, depth: Int = 0): String {
        val rows = Regex("""<m:mr\b[\s\S]*?</m:mr>""")
            .findAll(xml)
            .map { rowMatch -> extractOmmlChildTexts(rowMatch.value, "e", depth + 1).filter { it.isNotBlank() } }
            .filter { it.isNotEmpty() }
            .toList()
        if (rows.isEmpty()) return ""
        val hasMultipleColumns = rows.any { it.size > 1 }
        return if (hasMultipleColumns) {
            rows.joinToString(prefix = "[", postfix = "]", separator = "; ") { row ->
                row.joinToString(prefix = "[", postfix = "]", separator = ", ")
            }
        } else {
            rows.joinToString(prefix = "{", postfix = "}", separator = "; ") { row -> row.first() }
        }
    }

    private fun formatOmmlAccent(xml: String, depth: Int = 0): String {
        val base = extractOmmlChildText(xml, "e", depth + 1)
        if (base.isBlank()) return ""
        val accent = extractOmmlControlValue(xml, "chr")
        return when (accent) {
            "→", "⃗" -> base + "⃗"
            "^", "̂" -> base + "̂"
            "~", "˜", "̃" -> base + "̃"
            "¯", "ˉ", "̄" -> base + "̄"
            "˙", "·", "̇" -> base + "̇"
            "¨", "̈" -> base + "̈"
            "" -> base
            else -> base + accent
        }
    }

    private fun formatOmmlBar(xml: String, depth: Int = 0): String {
        val base = extractOmmlChildText(xml, "e", depth + 1)
        if (base.isBlank()) return ""
        val position = extractOmmlControlValue(xml, "pos").lowercase(Locale.ROOT)
        return if (position == "bot" || position == "bottom") base + "̲" else base + "̄"
    }

    private fun formatOmmlFunction(xml: String, depth: Int = 0): String {
        val rawName = extractOmmlChildText(xml, "fName", depth + 1).trim()
        val argument = extractOmmlChildText(xml, "e", depth + 1).trim()
        if (rawName.isBlank()) return argument
        val normalizedName = normalizeOmmlFunctionName(rawName)
        if (argument.isBlank()) return normalizedName
        return when (normalizedName.lowercase(Locale.ROOT)) {
            "sin", "cos", "tan", "cot", "sec", "csc", "log", "ln", "lg" -> "$normalizedName($argument)"
            "lim" -> normalizedName + argument
            else -> "$normalizedName($argument)"
        }
    }

    private fun normalizeOmmlFunctionName(name: String): String {
        val compact = name.replace(Regex("""\s+"""), "")
        return when (compact.lowercase(Locale.ROOT)) {
            "sin", "sine" -> "sin"
            "cos", "cosine" -> "cos"
            "tan", "tangent" -> "tan"
            "cot" -> "cot"
            "sec" -> "sec"
            "csc" -> "csc"
            "log" -> "log"
            "ln" -> "ln"
            "lg" -> "lg"
            "lim" -> "lim"
            else -> compact.ifBlank { name }
        }
    }

    private fun matchingEndDelimiter(begin: String): String {
        return when (begin) {
            "(" -> ")"
            "[" -> "]"
            "{" -> "}"
            "|" -> "|"
            "‖" -> "‖"
            "⟨" -> "⟩"
            else -> begin
        }
    }

    private fun extractOmmlControlValue(xml: String, tagName: String): String {
        return Regex("""<m:$tagName\b[^>]*(?:m:val|val)=\"([^\"]*)\"[^>]*/?>""")
            .find(xml)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::decodeXmlEntities)
            .orEmpty()
    }

    private fun replaceOmmlScriptBlock(
        xml: String,
        tag: String,
        replacement: (String) -> String
    ): String {
        return Regex("""<m:$tag\b[\s\S]*?</m:$tag>""").replace(xml) { match -> replacement(match.value) }
    }

    private fun extractOmmlChildText(xml: String, childName: String, depth: Int = 0): String {
        if (depth > MAX_OMML_DEPTH) return extractPlainTextFromXml(xml)
        val childXml = Regex("""<m:$childName\b[\s\S]*?</m:$childName>""")
            .find(xml)
            ?.value
            ?: return ""
        val inner = childXml
            .replace(Regex("""^<m:$childName\b[^>]*>"""), "")
            .replace(Regex("""</m:$childName>$"""), "")
        return extractOmmlText(inner, depth)
    }

    private fun extractOmmlChildTexts(xml: String, childName: String, depth: Int = 0): List<String> {
        if (depth > MAX_OMML_DEPTH) return listOf(extractPlainTextFromXml(xml)).filter { it.isNotBlank() }
        return Regex("""<m:$childName\b[\s\S]*?</m:$childName>""")
            .findAll(xml)
            .map { match ->
                val inner = match.value
                    .replace(Regex("""^<m:$childName\b[^>]*>"""), "")
                    .replace(Regex("""</m:$childName>$"""), "")
                extractOmmlText(inner, depth)
            }
            .toList()
    }

    private fun extractSpreadsheetSharedStrings(xml: String): List<String> {
        return Regex("""<(?:[A-Za-z0-9_]+:)?si\b[\s\S]*?</(?:[A-Za-z0-9_]+:)?si>""")
            .findAll(xml)
            .map { extractSpreadsheetRichText(it.value) }
            .toList()
    }

    private fun extractSpreadsheetRows(xml: String, sharedStrings: List<String>): String {
        val rows = Regex("""<(?:[A-Za-z0-9_]+:)?row\b[\s\S]*?</(?:[A-Za-z0-9_]+:)?row>""")
            .findAll(xml)
            .mapNotNull { rowMatch ->
                val cells = linkedMapOf<Int, String>()
                var nextColumn = 0
                Regex("""<(?:[A-Za-z0-9_]+:)?c\b[^>]*/>|<(?:[A-Za-z0-9_]+:)?c\b[\s\S]*?</(?:[A-Za-z0-9_]+:)?c>""").findAll(rowMatch.value).forEach { cellMatch ->
                    val cellXml = cellMatch.value
                    val columnIndex = extractCellColumnIndex(cellXml) ?: nextColumn
                    nextColumn = columnIndex + 1
                    val cellText = extractSpreadsheetCellText(cellXml, sharedStrings)
                    if (!cellText.isNullOrBlank()) {
                        cells[columnIndex] = cellText
                            .replace("\r\n", "\n")
                            .replace('\r', '\n')
                            .replace("\n", "\\n")
                    }
                }
                if (cells.isEmpty()) {
                    null
                } else {
                    val maxColumn = cells.keys.maxOrNull() ?: 0
                    val values = (0..maxColumn).map { cells[it].orEmpty() }.dropLastWhile { it.isBlank() }
                    values.joinToString("\t").takeIf { it.isNotBlank() }
                }
            }
            .toList()
        return rows.joinToString("\n")
    }

    private fun extractSpreadsheetCellText(cellXml: String, sharedStrings: List<String>): String? {
        val cellType = extractAttribute(cellXml, "t")
        return when (cellType) {
            "s" -> {
                val index = extractFirstTagText(cellXml, "v")?.trim()?.toIntOrNull()
                index?.let { sharedStrings.getOrNull(it) }
            }
            "inlineStr" -> Regex("""<(?:[A-Za-z0-9_]+:)?is\b[\s\S]*?</(?:[A-Za-z0-9_]+:)?is>""").find(cellXml)?.value?.let(::extractSpreadsheetRichText)
            "b" -> when (extractFirstTagText(cellXml, "v")?.trim()) {
                "1" -> "TRUE"
                "0" -> "FALSE"
                else -> extractFirstTagText(cellXml, "v")
            }
            else -> {
                Regex("""<(?:[A-Za-z0-9_]+:)?is\b[\s\S]*?</(?:[A-Za-z0-9_]+:)?is>""").find(cellXml)?.value?.let(::extractSpreadsheetRichText)
                    ?: extractFirstTagText(cellXml, "v")
                    ?: extractFirstTagText(cellXml, "t")
            }
        }?.replace("_x000D_", "\n")
    }

    private fun extractSpreadsheetRichText(xml: String): String {
        val runs = Regex("""<(?:[A-Za-z0-9_]+:)?r\b[\s\S]*?</(?:[A-Za-z0-9_]+:)?r>""").findAll(xml).toList()
        val text = if (runs.isNotEmpty()) {
            runs.joinToString("") { run ->
                val script = detectVertAlign(run.value)
                Regex("""<(?:[A-Za-z0-9_]+:)?t\b[^>]*>[\s\S]*?</(?:[A-Za-z0-9_]+:)?t>""")
                    .findAll(run.value)
                    .joinToString("") { convertScript(extractTextTagValue(it.value), script) }
            }
        } else {
            Regex("""<(?:[A-Za-z0-9_]+:)?t\b[^>]*>[\s\S]*?</(?:[A-Za-z0-9_]+:)?t>""")
                .findAll(xml)
                .joinToString("") { extractTextTagValue(it.value) }
        }
        return text.replace("_x000D_", "\n")
    }

    private fun extractPlainTextFromXml(xml: String): String {
        val extracted = xml
            .replace(Regex("""<(?:w|m|a|r|v|is|si|c|row)?:?t\b[^>]*>([\s\S]*?)</(?:w|m|a|r|v|is|si|c|row)?:?t>""")) { match ->
                decodeXmlEntities(match.groupValues[1])
            }
            .replace(Regex("""<w:tab\s*/>"""), "\t")
            .replace(Regex("""<w:(?:br|cr)\b[^>]*/>""")) { match -> extractWordBreak(match.value) }
            .replace(Regex("""<[^>]+>"""), "")
            .let(::decodeXmlEntities)
        return restoreAnswerAnalysisManualBreaks(extracted)
    }

    private fun extractTextTagValue(xml: String): String {
        val match = Regex("""<[^>]+>([\s\S]*?)</[^>]+>""").find(xml) ?: return ""
        return decodeXmlEntities(match.groupValues[1])
    }

    private fun extractFirstTagText(xml: String, tagName: String): String? {
        return Regex("""<(?:[A-Za-z0-9_]+:)?$tagName\b[^>]*>([\s\S]*?)</(?:[A-Za-z0-9_]+:)?$tagName>""")
            .find(xml)
            ?.groupValues
            ?.get(1)
            ?.let(::decodeXmlEntities)
    }

    private fun extractAttribute(xml: String, name: String): String? {
        val openTag = xml.substringBefore('>', "")
        val pattern = "(?:^|\\s)(?:[A-Za-z0-9_]+:)?${Regex.escape(name)}=\"([^\"]*)\""
        return Regex(pattern)
            .find(openTag)
            ?.groupValues
            ?.get(1)
            ?.let(::decodeXmlEntities)
    }

    private fun detectVertAlign(xml: String): ScriptStyle {
        val value = Regex("""<[^>]*vertAlign\b[^>]*(?:[A-Za-z0-9_]+:)?val="([^"]+)"[^>]*/?>""")
            .find(xml)
            ?.groupValues
            ?.get(1)
            ?.lowercase(Locale.ROOT)
        return when (value) {
            "superscript", "super" -> ScriptStyle.Superscript
            "subscript", "sub" -> ScriptStyle.Subscript
            else -> ScriptStyle.Normal
        }
    }

    private fun convertScript(text: String, script: ScriptStyle): String {
        if (script == ScriptStyle.Normal || text.isEmpty()) return text
        return buildString(text.length) {
            text.forEach { ch ->
                append(
                    when (script) {
                        ScriptStyle.Superscript -> superscriptMap[ch] ?: ch
                        ScriptStyle.Subscript -> subscriptMap[ch] ?: ch
                        ScriptStyle.Normal -> ch
                    }
                )
            }
        }
    }

    private fun extractCellColumnIndex(cellXml: String): Int? {
        val ref = extractAttribute(cellXml, "r") ?: return null
        val letters = ref.takeWhile { it.isLetter() }
        if (letters.isBlank()) return null
        var index = 0
        letters.uppercase(Locale.ROOT).forEach { ch ->
            index = index * 26 + (ch.code - 'A'.code + 1)
        }
        return index - 1
    }

    private fun sheetOrder(name: String): Int {
        return Regex("""sheet(\d+)\.xml$""")
            .find(name)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: Int.MAX_VALUE
    }

    private fun decodeXmlEntities(text: String): String {
        val numericDecoded = text
            .replace(Regex("""&#x([0-9a-fA-F]+);""")) { match ->
                codePointToString(match.groupValues[1].toIntOrNull(16))
            }
            .replace(Regex("""&#(\d+);""")) { match ->
                codePointToString(match.groupValues[1].toIntOrNull())
            }
        return numericDecoded
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    private fun codePointToString(codePoint: Int?): String {
        if (codePoint == null || !Character.isValidCodePoint(codePoint)) return ""
        return String(Character.toChars(codePoint))
    }

    private fun String?.toDecodeResult(emptyMessage: String): DecodeResult {
        val normalized = this?.let(QuestionTextNormalizer::normalize).orEmpty()
        return if (normalized.isBlank()) {
            DecodeResult.Failure(emptyMessage)
        } else {
            DecodeResult.Success(normalized)
        }
    }

    private fun readZipEntriesResult(bytes: ByteArray): Result<List<RawZipEntry>> {
        return try {
            Result.success(readZipEntries(bytes))
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun readZipEntries(bytes: ByteArray): List<RawZipEntry> {
        val maxTotalSize = 50L * 1024 * 1024
        val maxEntrySize = 10L * 1024 * 1024
        val maxEntries = 500
        var totalSize = 0L
        return buildList {
            ZipInputStream(bytes.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    if (size >= maxEntries) break
                    val name = SafeZipReader.normalizeEntryName(entry.name)
                    val data = SafeZipReader.readEntryBytes(
                        zip = zip,
                        entry = entry,
                        maxSize = maxEntrySize,
                        maxTotalRemaining = maxTotalSize - totalSize
                    )
                    totalSize += data.size
                    add(RawZipEntry(name, data))
                }
            }
        }
    }
    private val superscriptMap = mapOf(
        '0' to '\u2070', '1' to '\u00B9', '2' to '\u00B2', '3' to '\u00B3', '4' to '\u2074',
        '5' to '\u2075', '6' to '\u2076', '7' to '\u2077', '8' to '\u2078', '9' to '\u2079',
        '+' to '\u207A', '-' to '\u207B', '=' to '\u207C', '(' to '\u207D', ')' to '\u207E',
        'n' to '\u207F', 'i' to '\u2071'
    )

    private val subscriptMap = mapOf(
        '0' to '\u2080', '1' to '\u2081', '2' to '\u2082', '3' to '\u2083', '4' to '\u2084',
        '5' to '\u2085', '6' to '\u2086', '7' to '\u2087', '8' to '\u2088', '9' to '\u2089',
        '+' to '\u208A', '-' to '\u208B', '=' to '\u208C', '(' to '\u208D', ')' to '\u208E',
        'a' to '\u2090', 'e' to '\u2091', 'h' to '\u2095', 'i' to '\u1D62', 'j' to '\u2C7C',
        'k' to '\u2096', 'l' to '\u2097', 'm' to '\u2098', 'n' to '\u2099', 'o' to '\u2092',
        'p' to '\u209A', 'r' to '\u1D63', 's' to '\u209B', 't' to '\u209C', 'u' to '\u1D64',
        'v' to '\u1D65', 'x' to '\u2093'
    )
}
