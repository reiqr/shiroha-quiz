package com.yiqiu.shirohaquiz.importer.parser

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object TextImportDecoder {
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
        if (bytes.isEmpty()) return ""
        val lowerName = fileName.lowercase(Locale.ROOT)
        val zipEntries = if (looksLikeZip(bytes)) runCatching { readZipEntries(bytes) }.getOrNull() else null

        return when {
            lowerName.endsWith(".docx") -> zipEntries?.let(::decodeDocxFromEntries) ?: decodeDocx(bytes)
            lowerName.endsWith(".xlsx") -> zipEntries?.let(::decodeXlsxFromEntries) ?: decodeXlsx(bytes)
            zipEntries?.any { it.name == "word/document.xml" } == true -> decodeDocxFromEntries(zipEntries)
            zipEntries?.any { it.name.startsWith("xl/worksheets/") } == true -> decodeXlsxFromEntries(zipEntries)
            else -> decodePlainText(bytes)
        }?.let(QuestionTextNormalizer::normalize)
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

    private fun decodeDocx(bytes: ByteArray): String? {
        return runCatching { decodeDocxFromEntries(readZipEntries(bytes)) }.getOrNull()
    }

    private fun decodeDocxFromEntries(entries: List<RawZipEntry>): String? {
        val documentXml = entries.firstOrNull { it.name == "word/document.xml" }?.bytes?.toString(Charsets.UTF_8)
            ?: return null
        return extractTextFromWordXml(documentXml)
    }

    private fun decodeXlsx(bytes: ByteArray): String? {
        return runCatching { decodeXlsxFromEntries(readZipEntries(bytes)) }.getOrNull()
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
            """<m:oMathPara\b[\s\S]*?</m:oMathPara>|<m:oMath\b[\s\S]*?</m:oMath>|<w:r\b[\s\S]*?</w:r>|<w:tab\s*/>|<w:br\s*/>|<w:t\b[^>]*>[\s\S]*?</w:t>"""
        )
        val matches = tokenRegex.findAll(xml).toList()
        if (matches.isEmpty()) return extractPlainTextFromXml(xml)

        return buildString {
            matches.forEach { match ->
                val token = match.value
                when {
                    token.startsWith("<w:tab") -> append('\t')
                    token.startsWith("<w:br") -> append('\n')
                    token.startsWith("<m:oMath") -> append(extractOmmlText(token))
                    token.startsWith("<w:r") -> append(extractWordRunText(token))
                    token.startsWith("<w:t") -> append(extractTextTagValue(token))
                }
            }
        }.replace(Regex("""[ \t]{2,}"""), " ")
    }

    private fun extractWordRunText(xml: String): String {
        if ("<m:oMath" in xml || "<m:oMathPara" in xml) return extractOmmlText(xml)
        val script = detectVertAlign(xml)
        val tokenRegex = Regex("""<w:tab\s*/>|<w:br\s*/>|<(?:w|m):t\b[^>]*>[\s\S]*?</(?:w|m):t>""")
        return buildString {
            tokenRegex.findAll(xml).forEach { match ->
                val token = match.value
                when {
                    token.startsWith("<w:tab") -> append('\t')
                    token.startsWith("<w:br") -> append('\n')
                    else -> append(convertScript(extractTextTagValue(token), script))
                }
            }
        }
    }

    private fun extractOmmlText(xml: String): String {
        var working = xml
        working = replaceOmmlScriptBlock(working, "sSubSup") { block ->
            val base = extractOmmlChildText(block, "e")
            val sub = convertScript(extractOmmlChildText(block, "sub"), ScriptStyle.Subscript)
            val sup = convertScript(extractOmmlChildText(block, "sup"), ScriptStyle.Superscript)
            base + sub + sup
        }
        working = replaceOmmlScriptBlock(working, "sSup") { block ->
            val base = extractOmmlChildText(block, "e")
            val sup = convertScript(extractOmmlChildText(block, "sup"), ScriptStyle.Superscript)
            base + sup
        }
        working = replaceOmmlScriptBlock(working, "sSub") { block ->
            val base = extractOmmlChildText(block, "e")
            val sub = convertScript(extractOmmlChildText(block, "sub"), ScriptStyle.Subscript)
            base + sub
        }
        working = Regex("""<m:f\b[\s\S]*?</m:f>""").replace(working) { match ->
            val numerator = extractOmmlChildText(match.value, "num")
            val denominator = extractOmmlChildText(match.value, "den")
            if (numerator.isNotBlank() && denominator.isNotBlank()) "($numerator)/($denominator)" else extractPlainTextFromXml(match.value)
        }
        working = Regex("""<m:rad\b[\s\S]*?</m:rad>""").replace(working) { match ->
            val degree = extractOmmlChildText(match.value, "deg")
            val radicand = extractOmmlChildText(match.value, "e")
            when {
                degree.isNotBlank() && radicand.isNotBlank() -> "${degree}\u221A($radicand)"
                radicand.isNotBlank() -> "\u221A($radicand)"
                else -> extractPlainTextFromXml(match.value)
            }
        }
        working = Regex("""<m:chr\b[^>]*(?:m:val|val)="([^"]+)"[^>]*/?>""").replace(working) { match ->
            decodeXmlEntities(match.groupValues[1])
        }
        return extractPlainTextFromXml(working)
    }

    private fun replaceOmmlScriptBlock(
        xml: String,
        tag: String,
        replacement: (String) -> String
    ): String {
        return Regex("""<m:$tag\b[\s\S]*?</m:$tag>""").replace(xml) { match -> replacement(match.value) }
    }

    private fun extractOmmlChildText(xml: String, childName: String): String {
        val childXml = Regex("""<m:$childName\b[\s\S]*?</m:$childName>""")
            .find(xml)
            ?.value
            ?: return ""
        val inner = childXml
            .replace(Regex("""^<m:$childName\b[^>]*>"""), "")
            .replace(Regex("""</m:$childName>$"""), "")
        return extractOmmlText(inner)
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
        return xml
            .replace(Regex("""<(?:w|m|a|r|v|is|si|c|row)?:?t\b[^>]*>([\s\S]*?)</(?:w|m|a|r|v|is|si|c|row)?:?t>""")) { match ->
                decodeXmlEntities(match.groupValues[1])
            }
            .replace(Regex("""<w:tab\s*/>"""), "\t")
            .replace(Regex("""<w:br\s*/>"""), "\n")
            .replace(Regex("""<[^>]+>"""), "")
            .let(::decodeXmlEntities)
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
                    val data = zip.readBytes(entry, maxEntrySize)
                    totalSize += data.size
                    if (totalSize > maxTotalSize) break
                    add(RawZipEntry(entry.name, data))
                }
            }
        }
    }

    private fun ZipInputStream.readBytes(entry: ZipEntry, maxSize: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            total += read
            if (total > maxSize) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
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
