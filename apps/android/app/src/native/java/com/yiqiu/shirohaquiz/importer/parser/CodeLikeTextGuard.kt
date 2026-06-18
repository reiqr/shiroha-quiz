package com.yiqiu.shirohaquiz.importer.parser

/**
 * 仅拦截高置信度的代码语法外形，避免函数调用、成员访问、字符串和注释
 * 被题号、选项、括号答案或空括号规则误识别。
 *
 * 这里不是编程语言解析器；正常题目结构仍由原有标准规则决定。
 */
internal object CodeLikeTextGuard {
    private val emptyParenthesesRegex = Regex("""[\(（]\s*[\)）]""")
    private val decimalExpressionRegex = Regex(
        """^\s*\d+\.\d+(?:[fFdDlL])?(?=\s*(?:[+\-*/%<>=]|\.\d|$))"""
    )
    private val dottedNumericRegex = Regex("""^\s*(?:\d{1,3}\.){2,}\d{1,4}(?=\s|$|[/:+\-*])""")
    private val directCallRegex = Regex(
        """^\s*(?:[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*|[A-Za-z_]\w*\s*\[[^\]]+])\s*\([^\r\n]*\)\s*(?:[;{]\s*)?$"""
    )
    private val controlCallRegex = Regex(
        """^\s*(?:if|while|for|switch|catch|assert|when)\s*\([^\r\n]*\)\s*(?:[;{]\s*)?$""",
        RegexOption.IGNORE_CASE
    )
    private val assignmentRegex = Regex(
        """^\s*(?:[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*|[A-Za-z_]\w*\s*\[[^\]]+])\s*(?:=|\+=|-=|\*=|/=|%=|:=)\s*.+;?\s*$"""
    )
    private val memberAccessSequenceRegex = Regex(
        """^\s*[A-Ga-g]\.[A-Za-z_]\w*(?:\([^\r\n]*?\))?(?:\s*[,;]\s*[A-Ga-g]\.[A-Za-z_]\w*(?:\([^\r\n]*?\))?){1,}\s*$"""
    )

    fun isProtectedParenthesizedToken(text: String, range: IntRange, token: String): Boolean {
        if (range.isEmpty()) return false
        val openIndex = range.first
        if (isIndexInStringOrComment(text, openIndex)) return true
        if (isCallLikeOpenParenthesis(text, openIndex)) return true
        return isMixedCaseChoiceToken(token)
    }

    fun hasUnprotectedEmptyParentheses(text: String): Boolean {
        return emptyParenthesesRegex.findAll(text).any { match ->
            !isProtectedParenthesizedToken(text, match.range, "")
        }
    }

    fun looksLikeStandaloneCodeExpression(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return false
        if (startsWithComment(trimmed)) return true
        if (hasUnprotectedEmptyParentheses(trimmed)) return false
        if (containsCjk(trimmed)) return false
        return directCallRegex.matches(trimmed) ||
            controlCallRegex.matches(trimmed) ||
            assignmentRegex.matches(trimmed) ||
            memberAccessSequenceRegex.matches(trimmed)
    }

    fun looksLikeNumericCodeOrDataLine(line: String): Boolean {
        val trimmed = line.trim()
        if (containsCjk(trimmed)) return false
        return decimalExpressionRegex.containsMatchIn(trimmed) || dottedNumericRegex.containsMatchIn(trimmed)
    }

    fun isIndexInStringOrComment(text: String, index: Int): Boolean {
        if (index !in text.indices) return false
        return protectedRanges(text).any { index in it }
    }

    fun looksLikeLeadingCodeOption(line: String, markerStart: Int, contentStart: Int): Boolean {
        if (markerStart !in line.indices || contentStart !in 0..line.length) return false
        if (isIndexInStringOrComment(line, markerStart)) return true

        val markerText = line.substring(markerStart, contentStart)
        val markerLead = markerText.trimStart().firstOrNull()
        if (
            markerLead in setOf('(', '（', '[', '【', '〔', '〖', '《') &&
            markerStart > 0 &&
            line.getOrNull(markerStart - 1)?.let { isAsciiIdentifierPart(it) || it == ']' || it == ')' } == true
        ) {
            return true
        }

        val tail = line.substring(contentStart)
        val trimmedTail = tail.trimStart()
        if (
            trimmedTail.startsWith("->") || trimmedTail.startsWith("=>") ||
            trimmedTail.startsWith("::") || trimmedTail.startsWith("=")
        ) {
            return true
        }

        val usesDotMarker = '.' in markerText || '．' in markerText
        if (!usesDotMarker || tail.firstOrNull()?.isWhitespace() == true) return false

        val identifier = Regex("""^[A-Za-z_]\w*""").find(tail) ?: return false
        val afterIndex = identifier.range.last + 1
        val after = tail.getOrNull(afterIndex)
        if (after in setOf('(', '[', '.', '=', ':')) return true
        if (tail.contains("::") || tail.contains("->") || tail.contains("=>")) return true

        if (after == ',' || after == ';') {
            val rest = tail.substring(afterIndex + 1)
            if (Regex("""\s*[A-Ga-g]\.[A-Za-z_]\w*""").containsMatchIn(rest)) return true
        }
        return false
    }

    fun looksLikeCodeOptionChunk(
        line: String,
        markerStart: Int,
        contentStart: Int,
        endExclusive: Int
    ): Boolean {
        if (markerStart !in line.indices || contentStart !in 0..line.length) return false
        val safeEnd = endExclusive.coerceIn(contentStart, line.length)
        val markerText = line.substring(markerStart, contentStart)
        val rawChunk = line.substring(contentStart, safeEnd)
        if (rawChunk.firstOrNull()?.isWhitespace() == true) return false
        if ('.' !in markerText && '．' !in markerText) return false

        val chunk = rawChunk.trim()
        val identifier = Regex("""^[A-Za-z_]\w*""").find(chunk) ?: return false
        val after = chunk.getOrNull(identifier.range.last + 1)
        if (after in setOf('(', '[', '.', '=', ':')) return true
        if (chunk.contains("::") || chunk.contains("->") || chunk.contains("=>")) return true
        if ((chunk.endsWith(',') || chunk.endsWith(';')) && identifier.value.length + 1 == chunk.length) return true
        return false
    }

    private fun isCallLikeOpenParenthesis(text: String, openIndex: Int): Boolean {
        if (openIndex <= 0) return false
        val immediate = text[openIndex - 1]
        if (isAsciiIdentifierPart(immediate) || immediate == ']' || immediate == ')') return true
        if (!immediate.isWhitespace()) return false

        var tokenEnd = openIndex - 2
        while (tokenEnd >= 0 && text[tokenEnd].isWhitespace()) tokenEnd -= 1
        if (tokenEnd < 0 || !isAsciiIdentifierPart(text[tokenEnd])) return false

        var tokenStart = tokenEnd
        while (tokenStart > 0 && isAsciiIdentifierPart(text[tokenStart - 1])) tokenStart -= 1
        val token = text.substring(tokenStart, tokenEnd + 1)
        val prefix = text.substring(0, tokenStart).trimEnd()
        if (token.lowercase() in setOf("if", "while", "for", "switch", "catch", "assert", "when")) return true
        val prefixTail = prefix.lastOrNull()
        return prefix.isBlank() || prefixTail?.let {
            it in setOf('=', '.', '(', '[', '{', ',', ';', ':', '+', '-', '*', '/', '%', '!', '&', '|', '<', '>')
        } == true
    }

    private fun isMixedCaseChoiceToken(token: String): Boolean {
        val compact = token.replace(Regex("""[\s,，、;；/\\]+"""), "")
        if (compact.isBlank() || !Regex("""^[A-Ga-g]+$""").matches(compact)) return false
        return compact.any { it.isUpperCase() } && compact.any { it.isLowerCase() }
    }

    private fun startsWithComment(text: String): Boolean {
        return text.startsWith("//") || text.startsWith("#") ||
            text.startsWith("/*") || text.startsWith("*/")
    }

    private fun protectedRanges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var index = 0
        while (index < text.length) {
            val ch = text[index]
            if (ch == '/' && text.getOrNull(index + 1) == '/' && isCommentBoundary(text, index)) {
                ranges += index..text.lastIndex
                break
            }
            if (ch == '#' && isCommentBoundary(text, index)) {
                ranges += index..text.lastIndex
                break
            }
            if (ch == '/' && text.getOrNull(index + 1) == '*') {
                val end = text.indexOf("*/", startIndex = index + 2)
                val close = if (end >= 0) end + 1 else text.lastIndex
                ranges += index..close
                index = close + 1
                continue
            }
            if (ch == '"' || ch == '\'' || ch == '`') {
                if (ch == '\'' && isApostropheInsideWord(text, index)) {
                    index += 1
                    continue
                }
                val start = index
                index += 1
                var escaping = false
                while (index < text.length) {
                    val current = text[index]
                    when {
                        escaping -> escaping = false
                        current == '\\' -> escaping = true
                        current == ch -> {
                            ranges += start..index
                            index += 1
                            break
                        }
                    }
                    index += 1
                }
                if (ranges.none { start in it }) {
                    ranges += start..text.lastIndex
                }
                continue
            }
            index += 1
        }
        return ranges
    }

    private fun isCommentBoundary(text: String, index: Int): Boolean {
        if (index == 0) return true
        val previous = text[index - 1]
        return previous.isWhitespace() || previous in setOf(';', '{', '}', '(', ')', '[', ']')
    }

    private fun isApostropheInsideWord(text: String, index: Int): Boolean {
        val before = text.getOrNull(index - 1)
        val after = text.getOrNull(index + 1)
        return before?.let(::isAsciiIdentifierPart) == true && after?.let(::isAsciiIdentifierPart) == true
    }

    private fun containsCjk(text: String): Boolean = text.any { it in '\u3400'..'\u9FFF' }

    private fun isAsciiIdentifierPart(char: Char): Boolean {
        return char in 'A'..'Z' || char in 'a'..'z' || char.isDigit() || char == '_'
    }
}
