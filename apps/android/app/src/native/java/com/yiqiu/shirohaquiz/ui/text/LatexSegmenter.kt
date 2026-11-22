package com.yiqiu.shirohaquiz.ui.text

/**
 * 把题库原文切成「纯文本」与「数学公式」两种片段。
 *
 * 设计约束：
 * - 只做切分，不做任何内容改写，原始文本与原始公式都完整保留，便于渲染失败时回退。
 * - 代码块（```...```）、行内代码（`...`）内部不识别公式，避免被误当作数学。
 * - 支持 `$...$`、`$$...$$`、`\(...\)`、`\[...\]` 四种定界符。
 * - `\$` 视为转义后的普通美元符号，不作为定界符。
 *
 * 该对象不依赖 Android，可直接在 JVM 单元测试中覆盖。
 */
sealed class LatexSegment {
    /** 普通文本（原样保留）。 */
    data class PlainText(val text: String) : LatexSegment()

    /**
     * 数学公式。
     * @param latex 去掉定界符后的公式体。
     * @param display 是否为独立公式（$$ / \[ \]）。
     * @param raw 含定界符的原始片段，渲染失败时用于回退显示。
     */
    data class Math(val latex: String, val display: Boolean, val raw: String) : LatexSegment()
}

object LatexSegmenter {

    private const val FENCE = "```"
    private const val MAX_FORMULAS = 48

    /** 行内公式最长扫描长度，防止未闭合的 `$` 把整段题干吞掉。 */
    private const val MAX_INLINE_LENGTH = 2048

    /** 独立公式最长扫描长度。 */
    private const val MAX_DISPLAY_LENGTH = 16384

    private const val MAX_TEXT_LENGTH = 32768

    fun containsMath(text: String): Boolean {
        if (text.isEmpty()) return false
        if (!looksLikeMathSource(text)) return false
        return split(text).any { it is LatexSegment.Math }
    }

    /** 快速预判，避免对大量纯文本做完整切分。 */
    fun looksLikeMathSource(text: String): Boolean {
        return text.indexOf('$') >= 0 || text.contains("\\(") || text.contains("\\[")
    }

    fun split(text: String): List<LatexSegment> {
        if (text.isEmpty()) return emptyList()
        if (text.length > MAX_TEXT_LENGTH || !looksLikeMathSource(text)) {
            return listOf(LatexSegment.PlainText(text))
        }
        val segments = ArrayList<LatexSegment>()
        val plain = StringBuilder(text.length)
        var index = 0
        var formulaCount = 0
        val length = text.length

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                segments.add(LatexSegment.PlainText(plain.toString()))
                plain.setLength(0)
            }
        }

        while (index < length) {
            if (formulaCount >= MAX_FORMULAS) {
                plain.append(text, index, length)
                break
            }
            val ch = text[index]

            // 围栏代码块整体按纯文本处理
            if (text.startsWith(FENCE, index) || text.startsWith("~~~", index)) {
                var afterOpening = index + 3
                while (text.getOrNull(afterOpening) == ch) afterOpening++
                val fence = text.substring(index, afterOpening)
                val close = text.indexOf(fence, afterOpening)
                val end = if (close < 0) length else close + fence.length
                plain.append(text, index, end)
                index = end
                continue
            }

            // 行内代码
            if (ch == '`') {
                val close = findInlineCodeEnd(text, index + 1)
                if (close > 0) {
                    plain.append(text, index, close + 1)
                    index = close + 1
                    continue
                }
                plain.append(ch)
                index++
                continue
            }

            // 反斜杠开头的定界符与转义
            if (ch == '\\' && index + 1 < length) {
                when (text[index + 1]) {
                    '(' -> {
                        val parsed = parseDelimited(
                            text = text,
                            contentStart = index + 2,
                            closer = "\\)",
                            display = false,
                            rawStart = index,
                            allowNewline = false,
                            maxScan = MAX_INLINE_LENGTH
                        )
                        if (parsed != null) {
                            flushPlain()
                            segments.add(parsed.segment)
                            formulaCount++
                            index = parsed.end
                            continue
                        }
                    }

                    '[' -> {
                        val parsed = parseDelimited(
                            text = text,
                            contentStart = index + 2,
                            closer = "\\]",
                            display = true,
                            rawStart = index,
                            allowNewline = true,
                            maxScan = MAX_DISPLAY_LENGTH
                        )
                        if (parsed != null) {
                            flushPlain()
                            segments.add(parsed.segment)
                            formulaCount++
                            index = parsed.end
                            continue
                        }
                    }

                    '$' -> {
                        // 转义美元符号，不参与公式识别
                        plain.append('\\').append('$')
                        index += 2
                        continue
                    }

                    '\\' -> {
                        // 连续两个反斜杠（如 LaTeX 换行）原样保留，避免被当作 \[ 定界符
                        plain.append('\\').append('\\')
                        index += 2
                        continue
                    }
                }
                plain.append(ch)
                index++
                continue
            }

            if (ch == '$') {
                if (index + 1 < length && text[index + 1] == '$') {
                    val parsed = parseDelimited(
                        text = text,
                        contentStart = index + 2,
                        closer = "$$",
                        display = true,
                        rawStart = index,
                        allowNewline = true,
                        maxScan = MAX_DISPLAY_LENGTH
                    )
                    if (parsed != null) {
                        flushPlain()
                        segments.add(parsed.segment)
                        formulaCount++
                        index = parsed.end
                        continue
                    }
                    plain.append("$$")
                    index += 2
                    continue
                } else if (text.getOrNull(index + 1)?.isWhitespace() == false) {
                    val parsed = parseDelimited(
                        text = text,
                        contentStart = index + 1,
                        closer = "$",
                        display = false,
                        rawStart = index,
                        allowNewline = false,
                        maxScan = MAX_INLINE_LENGTH
                    )
                    if (parsed != null) {
                        flushPlain()
                        segments.add(parsed.segment)
                        formulaCount++
                        index = parsed.end
                        continue
                    }
                }
                plain.append(ch)
                index++
                continue
            }

            plain.append(ch)
            index++
        }

        flushPlain()
        return segments
    }

    private class Parsed(val segment: LatexSegment.Math, val end: Int)

    private fun parseDelimited(
        text: String,
        contentStart: Int,
        closer: String,
        display: Boolean,
        rawStart: Int,
        allowNewline: Boolean,
        maxScan: Int
    ): Parsed? {
        if (contentStart > text.length) return null
        var cursor = contentStart
        val limit = minOf(text.length, contentStart + maxScan)
        while (cursor < limit) {
            // 先判断结束定界符：\( \) 与 \[ \] 的定界符本身以反斜杠开头
            if (text.startsWith(closer, cursor)) {
                val content = text.substring(contentStart, cursor)
                if (content.isBlank()) return null
                val rawEnd = cursor + closer.length
                if (closer == "$" && (content.last().isWhitespace() ||
                        text.getOrNull(rawEnd)?.let { it == '$' || it.isDigit() } == true)
                ) return null
                return Parsed(
                    LatexSegment.Math(
                        latex = content,
                        display = display,
                        raw = text.substring(rawStart, rawEnd)
                    ),
                    rawEnd
                )
            }
            val ch = text[cursor]
            if (!allowNewline && (ch == '\n' || ch == '\r')) return null
            if (ch == '\\' && cursor + 1 < text.length) {
                if (!allowNewline && text[cursor + 1] in "\r\n") return null
                cursor += 2
                continue
            }
            // 未闭合的公式不应跨越代码块
            if (ch == '`' && text.startsWith(FENCE, cursor)) return null
            cursor++
        }
        return null
    }

    /** 返回行内代码的结束反引号下标；找不到或跨行则返回 -1。 */
    private fun findInlineCodeEnd(text: String, startIndex: Int): Int {
        var cursor = startIndex
        while (cursor < text.length) {
            val ch = text[cursor]
            if (ch == '`') return cursor
            if (ch == '\n' || ch == '\r') return -1
            cursor++
        }
        return -1
    }
}

internal fun formatPlainMathText(text: String): String =
    if (LatexSegmenter.looksLikeMathSource(text)) text else LatexDisplayFormatter.format(text)
