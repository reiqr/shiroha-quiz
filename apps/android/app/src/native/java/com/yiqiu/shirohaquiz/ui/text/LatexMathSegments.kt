package com.yiqiu.shirohaquiz.ui.text

internal data class LatexMathSegment(
    val start: Int,
    val end: Int,
    val formula: String,
    val display: Boolean
)

internal fun findLatexMathSegments(text: String): List<LatexMathSegment> {
    if ('$' !in text && '\\' !in text) return emptyList()
    val result = mutableListOf<LatexMathSegment>()
    var index = 0
    while (index < text.length && result.size < 48) {
        if (text[index] == CODE_TICK || text.startsWith("~~~", index)) {
            val marker = text[index]
            val start = index
            while (text.getOrNull(index) == marker) index++
            val fence = text.substring(start, index)
            val close = text.indexOf(fence, index)
            if (close >= 0) index = close + fence.length
            else if (fence.length >= 3) break
            continue
        }
        val opener = when {
            text.startsWith("$$", index) -> "$$"
            text.startsWith("\\[", index) -> "\\["
            text.startsWith("\\(", index) -> "\\("
            text[index] == '$' && text.getOrNull(index + 1)?.isWhitespace() == false -> "$"
            else -> {
                index += if (text[index] == '\\') 2 else 1
                continue
            }
        }
        val closer = when (opener) {
            "\\[" -> "\\]"
            "\\(" -> "\\)"
            else -> opener
        }
        val display = opener == "$$" || opener == "\\["
        val start = index + opener.length
        var cursor = start
        // Bound work per delimiter and leave long or malformed input as authored.
        while (cursor < text.length && cursor - start <= 1024) {
            val char = text[cursor]
            if (!display && (char == '\n' || char == '\r')) break
            if (text.startsWith(closer, cursor)) {
                val end = cursor + closer.length
                if (opener == "$" && (cursor == start || text[cursor - 1].isWhitespace() ||
                        text.getOrNull(end)?.let { it == '$' || it.isDigit() } == true)
                ) break
                val formula = text.substring(start, cursor).trim()
                if (formula.isNotEmpty()) result += LatexMathSegment(index, end, formula, display)
                index = end
                break
            }
            cursor += if (char == '\\') 2 else 1
        }
        if (index < start) index = start
    }
    return result
}

private const val CODE_TICK = '\u0060'
