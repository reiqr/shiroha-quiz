package com.yiqiu.shirohaquiz.ui.text

import java.util.Locale

/** 传给 KaTeX 页面的排版样式。CSS 像素与 Compose 的 dp 一一对应。 */
internal data class MathRenderStyle(
    val fontSizePx: Float,
    val lineHeightPx: Float,
    val colorArgb: Int,
    val fontWeight: Int,
    val textAlign: String,
    val maxLines: Int = 0
)

internal data class MathRenderLayout(
    val segments: List<LatexSegment>,
    val style: MathRenderStyle,
    val widthDp: Float
)

/**
 * 把切分结果序列化成注入 WebView 的脚本。
 *
 * 安全要点：所有文本与公式都经过 JSON 字符串转义，`<` `>` `&` 转成 `\uXXXX`，
 * 因此不可信题库内容只能以字符串字面量出现，不可能变成 HTML 标签或脚本语句。
 */
internal object MathRenderPayload {

    fun buildScript(layout: MathRenderLayout, generation: Int): String {
        val (segments, style, widthDp) = layout
        val builder = StringBuilder(256 + segments.size * 48)
        builder.append("window.ShirohaRender(")
        builder.append('{')
        builder.append("\"gen\":").append(generation).append(',')
        builder.append("\"widthDp\":").append(widthDp).append(',')

        builder.append("\"style\":{")
        builder.append("\"fontSizePx\":").append(formatNumber(style.fontSizePx)).append(',')
        builder.append("\"lineHeightPx\":").append(formatNumber(style.lineHeightPx)).append(',')
        builder.append("\"color\":\"").append(colorToCss(style.colorArgb)).append("\",")
        builder.append("\"fontWeight\":\"").append(style.fontWeight).append("\",")
        builder.append("\"textAlign\":\"").append(escapeJson(style.textAlign)).append('"').append(',')
        builder.append("\"maxLines\":").append(style.maxLines)
        builder.append("},")

        builder.append("\"segments\":[")
        segments.forEachIndexed { index, segment ->
            if (index > 0) builder.append(',')
            when (segment) {
                is LatexSegment.PlainText -> {
                    builder.append("{\"k\":\"t\",\"v\":\"")
                    builder.append(escapeJson(segment.text))
                    builder.append("\"}")
                }

                is LatexSegment.Math -> {
                    builder.append("{\"k\":\"m\",\"v\":\"")
                    builder.append(escapeJson(segment.latex))
                    builder.append("\",\"r\":\"")
                    builder.append(escapeJson(segment.raw))
                    builder.append("\",\"d\":")
                    builder.append(if (segment.display) "1" else "0")
                    builder.append('}')
                }
            }
        }
        builder.append(']')

        builder.append("});")
        return builder.toString()
    }

    /** 供单元测试与调试使用。 */
    fun segmentsToken(segments: List<LatexSegment>): String {
        return segments.joinToString("|") { segment ->
            when (segment) {
                is LatexSegment.PlainText -> "t:${segment.text}"
                is LatexSegment.Math -> "m${if (segment.display) "B" else "I"}:${segment.latex}"
            }
        }
    }

    fun colorToCss(argb: Int): String {
        return String.format(Locale.US, "#%06X", argb and 0xFFFFFF)
    }

    private fun formatNumber(value: Float): String {
        if (!value.isFinite() || value <= 0f) return "16"
        val rounded = Math.round(value * 100f) / 100f
        val asInt = rounded.toInt()
        return if (rounded == asInt.toFloat()) asInt.toString() else rounded.toString()
    }

    fun escapeJson(raw: String): String {
        val builder = StringBuilder(raw.length + 8)
        for (ch in raw) {
            when (ch) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '<' -> builder.append("\\u003c")
                '>' -> builder.append("\\u003e")
                '&' -> builder.append("\\u0026")
                '\u2028' -> builder.append("\\u2028")
                '\u2029' -> builder.append("\\u2029")
                else -> {
                    if (ch.code < 0x20) {
                        builder.append(String.format(Locale.US, "\\u%04x", ch.code))
                    } else {
                        builder.append(ch)
                    }
                }
            }
        }
        return builder.toString()
    }
}
