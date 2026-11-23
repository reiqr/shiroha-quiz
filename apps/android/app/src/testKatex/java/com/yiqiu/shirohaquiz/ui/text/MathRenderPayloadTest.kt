package com.yiqiu.shirohaquiz.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 注入脚本的序列化/转义测试：确保不可信题库内容无法突破字符串字面量。
 */
class MathRenderPayloadTest {

    private val style = MathRenderStyle(
        fontSizePx = 22f,
        lineHeightPx = 29f,
        colorArgb = 0xFF1F2937.toInt(),
        fontWeight = 600,
        textAlign = "left"
    )

    @Test
    fun skipsScriptsThatExceedTheBridgeBudgetAfterEscaping() {
        val text = "<".repeat(25000) + " \$x^2\$"
        val layout = MathRenderLayout(LatexSegmenter.split(text), style, 320f)
        assertEquals(null, MathRenderPayload.buildBoundedScript(layout, 7))
        assertTrue(MathRenderPayload.buildBoundedScript(layout.copy(segments = LatexSegmenter.split("\$x^2\$")), 8) != null)
    }

    @Test
    fun `公式与文本都进入 segments`() {
        val segments = LatexSegmenter.split("已知 \$\\sin x\$ 的导数")
        val script = MathRenderPayload.buildScript(MathRenderLayout(segments, style, 320f), 7)
        assertTrue(script.startsWith("window.ShirohaRender("))
        assertTrue(script.endsWith(");"))
        assertTrue(script.contains("\"gen\":7"))
        assertTrue(script.contains("\"k\":\"m\""))
        assertTrue(script.contains("\"k\":\"t\""))
        assertTrue(script.contains("\\\\sin x"))
    }

    @Test
    fun `独立公式带 display 标记`() {
        val segments = LatexSegmenter.split("$$\n\\int_0^1 x^2\\,dx\n$$")
        val script = MathRenderPayload.buildScript(MathRenderLayout(segments, style, 320f), 1)
        assertTrue(script.contains("\"d\":1"))
    }

    @Test
    fun `HTML 标签被转义为 unicode 转义`() {
        val segments = listOf(
            LatexSegment.PlainText("<script>alert(1)</script>")
        )
        val script = MathRenderPayload.buildScript(MathRenderLayout(segments, style, 320f), 1)
        assertFalse(script.contains("<script>"))
        assertTrue(script.contains("\\u003cscript\\u003e"))
    }

    @Test
    fun `引号与反斜杠被正确转义`() {
        val segments = listOf(
            LatexSegment.PlainText("引号 \" 与反斜杠 \\ 与换行 \n")
        )
        val script = MathRenderPayload.buildScript(MathRenderLayout(segments, style, 320f), 1)
        assertTrue(script.contains("\\\""))
        assertTrue(script.contains("\\\\"))
        assertTrue(script.contains("\\n"))
    }

    @Test
    fun `脚本闭合标签无法注入`() {
        val segments = listOf(
            LatexSegment.PlainText("</script><img src=x onerror=alert(1)>")
        )
        val script = MathRenderPayload.buildScript(MathRenderLayout(segments, style, 320f), 1)
        assertFalse(script.contains("</script>"))
        assertFalse(script.contains("<img"))
    }

    @Test
    fun `原始公式用于渲染失败回退`() {
        val segments = LatexSegmenter.split("$\\notACommand{x}$")
        val script = MathRenderPayload.buildScript(MathRenderLayout(segments, style, 320f), 1)
        assertTrue(script.contains("\"r\":\"$\\notACommand{x}$\"") || script.contains("notACommand"))
    }

    @Test
    fun `颜色转 CSS 十六进制`() {
        assertEquals("#1F2937", MathRenderPayload.colorToCss(0xFF1F2937.toInt()))
        assertEquals("#000000", MathRenderPayload.colorToCss(0xFF000000.toInt()))
    }

    @Test
    fun `maxLines 写入样式`() {
        val clamped = style.copy(maxLines = 3)
        val script = MathRenderPayload.buildScript(
            MathRenderLayout(LatexSegmenter.split("\$x\$"), clamped, 320f),
            1
        )
        assertTrue(script.contains("\"maxLines\":3"))
    }

    @Test
    fun `布局缓存键区分字号颜色与宽度`() {
        val layout = MathRenderLayout(emptyList(), style, 320f)
        assertFalse(layout == layout.copy(style = style.copy(fontSizePx = 18f)))
        assertFalse(layout == layout.copy(style = style.copy(colorArgb = 0xFF000000.toInt())))
        assertFalse(layout == layout.copy(widthDp = 240f))
    }
}
