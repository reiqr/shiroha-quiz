package com.yiqiu.shirohaquiz.ui.text

/**
 * UI-only LaTeX text formatter.
 *
 * It keeps the stored question text untouched and only converts a safe subset of
 * common LaTeX fragments into readable plain math text for display.
 */
object LatexDisplayFormatter {
    private val commandReplacements = linkedMapOf(
        "\\\\leq" to "≤",
        "\\\\le" to "≤",
        "\\\\geq" to "≥",
        "\\\\ge" to "≥",
        "\\\\neq" to "≠",
        "\\\\ne" to "≠",
        "\\\\approx" to "≈",
        "\\\\sim" to "≈",
        "\\\\times" to "×",
        "\\\\cdot" to "·",
        "\\\\div" to "÷",
        "\\\\pm" to "±",
        "\\\\mp" to "∓",
        "\\\\to" to "→",
        "\\\\rightarrow" to "→",
        "\\\\leftarrow" to "←",
        "\\\\infty" to "∞",
        "\\\\sum" to "∑",
        "\\\\prod" to "∏",
        "\\\\int" to "∫",
        "\\\\pi" to "π",
        "\\\\theta" to "θ",
        "\\\\alpha" to "α",
        "\\\\beta" to "β",
        "\\\\gamma" to "γ",
        "\\\\delta" to "δ",
        "\\\\lambda" to "λ",
        "\\\\mu" to "μ",
        "\\\\sigma" to "σ",
        "\\\\omega" to "ω",
        "\\\\Delta" to "Δ",
        "\\\\Omega" to "Ω",
        "\\\\%" to "%"
    )

    private val superscriptMap = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ'
    )

    private val subscriptMap = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )

    private val fencedCodeBlockRegex = Regex("""```[\s\S]*?```""")

    fun format(text: String): String {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        if (normalized.isEmpty()) return normalized

        val formatted = when {
            fencedCodeBlockRegex.containsMatchIn(normalized) -> formatAroundFencedCodeBlocks(normalized)
            looksLikeIndentedCode(normalized) -> normalized
            !mightContainLatex(normalized) -> normalized
            else -> formatInternal(normalized)
        }
        return formatted
            .replace("\t", "    ")
            .trimOuterWhitespacePreservingIndent()
    }

    private fun formatAroundFencedCodeBlocks(text: String): String {
        val builder = StringBuilder()
        var cursor = 0
        fencedCodeBlockRegex.findAll(text).forEach { match ->
            if (match.range.first > cursor) {
                builder.append(formatNonCodeText(text.substring(cursor, match.range.first)))
            }
            builder.append(match.value)
            cursor = match.range.last + 1
        }
        if (cursor < text.length) builder.append(formatNonCodeText(text.substring(cursor)))
        return builder.toString()
    }

    private fun formatNonCodeText(text: String): String {
        return if (mightContainLatex(text)) formatInternal(text) else text
    }

    private fun looksLikeIndentedCode(text: String): Boolean {
        if ('\t' in text) return true
        if (text.lineSequence().any { it.startsWith("    ") }) return true
        return text.lineSequence().any { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("#!") || trimmed.startsWith("//") || trimmed.startsWith("/*")
        }
    }

    private fun mightContainLatex(text: String): Boolean {
        return '$' in text || '\\' in text || "^{" in text || "_{" in text
    }

    private fun formatInternal(text: String): String {
        var working = unwrapFormulaDelimiters(text)
        working = convertLatexEnvironments(working)
        working = convertFractionsAndRoots(working)
        working = replaceCommands(working)
        working = convertScripts(working)
        working = cleanupLatexNoise(working)
        return working
    }

    private fun unwrapFormulaDelimiters(text: String): String {
        var working = text
        working = working.replace(Regex("""\\\[([\s\S]*?)\\\]""")) { match -> formatInternal(match.groupValues[1].trim()) }
        working = working.replace(Regex("""\\\(([\s\S]*?)\\\)""")) { match -> formatInternal(match.groupValues[1].trim()) }
        working = working.replace(Regex("""\$\$([\s\S]*?)\$\$""")) { match -> formatInternal(match.groupValues[1].trim()) }
        working = working.replace(Regex("""(?<!\\)\$([^$\n]+?)\$""")) { match -> formatInternal(match.groupValues[1].trim()) }
        return working
    }

    private fun convertLatexEnvironments(text: String): String {
        var working = text
        working = working.replace(Regex("""\\begin\{cases\}([\s\S]*?)\\end\{cases\}""")) { match ->
            formatCasesEnvironment(match.groupValues[1])
        }
        working = unwrapTextCommands(working)
        return working
    }

    private fun formatCasesEnvironment(content: String): String {
        val rows = splitCasesRows(content)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (rows.isEmpty()) return ""

        val formattedRows = rows.map { row ->
            val columns = splitTopLevelAmpersand(row)
                .map { cleanCasesCell(formatInternal(it.trim())) }
                .filter { it.isNotEmpty() }
            when (columns.size) {
                0 -> ""
                1 -> columns[0]
                else -> {
                    val value = columns.first()
                    val condition = columns.drop(1).joinToString("，")
                    if (condition.isBlank()) value else "$value（$condition）"
                }
            }
        }.filter { it.isNotEmpty() }

        return formattedRows.joinToString("；")
    }

    private fun splitCasesRows(content: String): List<String> {
        val rows = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        while (index < content.length) {
            val char = content[index]
            if (char == '\\' && index + 1 < content.length && content[index + 1] == '\\') {
                rows += current.toString()
                current.clear()
                index += 2
                if (index < content.length && content[index] == '[') {
                    index = skipOptionalBracket(content, index)
                }
                continue
            }
            current.append(char)
            index++
        }
        rows += current.toString()
        return rows
    }

    private fun splitTopLevelAmpersand(row: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var braceDepth = 0
        var bracketDepth = 0
        var index = 0
        while (index < row.length) {
            val char = row[index]
            when (char) {
                '\\' -> {
                    current.append(char)
                    if (index + 1 < row.length) {
                        current.append(row[index + 1])
                        index += 2
                        continue
                    }
                }
                '{' -> {
                    braceDepth++
                    current.append(char)
                }
                '}' -> {
                    if (braceDepth > 0) braceDepth--
                    current.append(char)
                }
                '[' -> {
                    bracketDepth++
                    current.append(char)
                }
                ']' -> {
                    if (bracketDepth > 0) bracketDepth--
                    current.append(char)
                }
                '&' -> {
                    if (braceDepth == 0 && bracketDepth == 0) {
                        parts += current.toString()
                        current.clear()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
            index++
        }
        parts += current.toString()
        return parts
    }

    private fun unwrapTextCommands(text: String): String {
        return unwrapSimpleCommand(text, "text")
            .let { unwrapSimpleCommand(it, "mathrm") }
            .let { unwrapSimpleCommand(it, "operatorname") }
            .let { unwrapSimpleCommand(it, "mbox") }
    }

    private fun unwrapSimpleCommand(text: String, command: String): String {
        val prefix = "\\$command"
        val builder = StringBuilder()
        var index = 0
        while (index < text.length) {
            if (text.startsWith(prefix, index)) {
                val start = skipWhitespace(text, index + prefix.length)
                val content = readBraceContent(text, start)
                if (content != null) {
                    builder.append(content.content)
                    index = content.nextIndex
                    continue
                }
            }
            builder.append(text[index])
            index++
        }
        return builder.toString()
    }

    private fun cleanCasesCell(text: String): String {
        return text
            .trim()
            .trimEnd(',', '，', ';', '；')
            .trim()
    }

    private fun convertFractionsAndRoots(text: String): String {
        val builder = StringBuilder()
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("\\frac", index) -> {
                    val firstStart = skipWhitespace(text, index + "\\frac".length)
                    val numerator = readBraceContent(text, firstStart)
                    val denominator = numerator?.let { readBraceContent(text, skipWhitespace(text, it.nextIndex)) }
                    if (numerator != null && denominator != null) {
                        val top = formatInternal(numerator.content.trim())
                        val bottom = formatInternal(denominator.content.trim())
                        builder.append(formatFraction(top, bottom))
                        index = denominator.nextIndex
                    } else {
                        builder.append(text[index])
                        index++
                    }
                }
                text.startsWith("\\dfrac", index) || text.startsWith("\\tfrac", index) -> {
                    val commandLength = if (text.startsWith("\\dfrac", index)) "\\dfrac".length else "\\tfrac".length
                    val firstStart = skipWhitespace(text, index + commandLength)
                    val numerator = readBraceContent(text, firstStart)
                    val denominator = numerator?.let { readBraceContent(text, skipWhitespace(text, it.nextIndex)) }
                    if (numerator != null && denominator != null) {
                        val top = formatInternal(numerator.content.trim())
                        val bottom = formatInternal(denominator.content.trim())
                        builder.append(formatFraction(top, bottom))
                        index = denominator.nextIndex
                    } else {
                        builder.append(text[index])
                        index++
                    }
                }
                text.startsWith("\\sqrt", index) -> {
                    val contentStart = skipOptionalBracket(text, skipWhitespace(text, index + "\\sqrt".length))
                    val radicand = readBraceContent(text, skipWhitespace(text, contentStart))
                    if (radicand != null) {
                        builder.append("√(").append(formatInternal(radicand.content.trim())).append(')')
                        index = radicand.nextIndex
                    } else {
                        builder.append(text[index])
                        index++
                    }
                }
                else -> {
                    builder.append(text[index])
                    index++
                }
            }
        }
        return builder.toString()
    }

    private fun replaceCommands(text: String): String {
        var working = text
        commandReplacements.forEach { (pattern, replacement) ->
            working = working.replace(Regex(pattern), replacement)
        }
        return working
    }

    private fun convertScripts(text: String): String {
        val builder = StringBuilder()
        var index = 0
        while (index < text.length) {
            val marker = text[index]
            if (marker == '^' || marker == '_') {
                val isSuperscript = marker == '^'
                val valueStart = index + 1
                val brace = readBraceContent(text, valueStart)
                val raw = when {
                    brace != null -> brace.content
                    valueStart < text.length -> text[valueStart].toString()
                    else -> ""
                }
                if (raw.isNotEmpty()) {
                    val converted = convertScriptContent(formatInternal(raw), isSuperscript)
                    builder.append(converted)
                    index = brace?.nextIndex ?: (valueStart + 1)
                    continue
                }
            }
            builder.append(text[index])
            index++
        }
        return builder.toString()
    }

    private fun convertScriptContent(content: String, superscript: Boolean): String {
        val map = if (superscript) superscriptMap else subscriptMap
        val compact = content.trim()
        if (compact.isEmpty()) return ""
        val converted = compact.map { map[it] }
        return if (converted.all { it != null }) {
            converted.joinToString("")
        } else {
            if (superscript) "^($compact)" else "_($compact)"
        }
    }

    private fun cleanupLatexNoise(text: String): String {
        return text
            .replace(Regex("""\\(?:left|right)"""), "")
            .replace(Regex("""\\[,;:!]"""), " ")
            .replace(Regex("""\\ """), " ")
            .replace("\\{", "{")
            .replace("\\}", "}")
            .replace("\\_", "_")
            .replace("\\$", "$")
    }

    private fun formatFraction(numerator: String, denominator: String): String {
        val top = numerator.trim()
        val bottom = denominator.trim()
        return "${wrapFractionPart(top)}/${wrapFractionPart(bottom)}"
    }

    private fun wrapFractionPart(part: String): String {
        val simple = Regex("""^[A-Za-z0-9.πθαβγδλμσωΔΩ∞²³⁰¹⁴⁵⁶⁷⁸⁹₀-₉ₐₑₕᵢⱼₖₗₘₙₒₚᵣₛₜᵤᵥₓ]+$""")
        return if (simple.matches(part)) part else "($part)"
    }

    private data class BraceContent(val content: String, val nextIndex: Int)

    private fun readBraceContent(text: String, startIndex: Int): BraceContent? {
        if (startIndex >= text.length || text[startIndex] != '{') return null
        var depth = 0
        val content = StringBuilder()
        var index = startIndex
        while (index < text.length) {
            val char = text[index]
            when (char) {
                '{' -> {
                    if (depth > 0) content.append(char)
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0) return BraceContent(content.toString(), index + 1)
                    if (depth > 0) content.append(char)
                }
                else -> if (depth > 0) content.append(char)
            }
            index++
        }
        return null
    }

    private fun skipWhitespace(text: String, startIndex: Int): Int {
        var index = startIndex
        while (index < text.length && text[index].isWhitespace()) index++
        return index
    }

    private fun skipOptionalBracket(text: String, startIndex: Int): Int {
        if (startIndex >= text.length || text[startIndex] != '[') return startIndex
        var depth = 0
        var index = startIndex
        while (index < text.length) {
            when (text[index]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return index + 1
                }
            }
            index++
        }
        return startIndex
    }

    private fun String.trimOuterWhitespacePreservingIndent(): String {
        val rows = split('\n')
        val first = rows.indexOfFirst { it.isNotBlank() }
        if (first < 0) return ""
        val last = rows.indexOfLast { it.isNotBlank() }
        return rows.subList(first, last + 1).joinToString("\n")
    }
}
