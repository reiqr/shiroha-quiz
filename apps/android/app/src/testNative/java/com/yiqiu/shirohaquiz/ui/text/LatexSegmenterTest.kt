package com.yiqiu.shirohaquiz.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 公式切分回归测试：覆盖 Issue #145 列出的全部样例。
 */
class LatexSegmenterTest {

    private fun mathOf(text: String): List<LatexSegment.Math> =
        LatexSegmenter.split(text).filterIsInstance<LatexSegment.Math>()

    private fun plainJoined(text: String): String =
        LatexSegmenter.split(text).filterIsInstance<LatexSegment.PlainText>()
            .joinToString("") { it.text }

    @Test
    fun `行内公式 sin x`() {
        val math = mathOf("$\\sin x$")
        assertEquals(1, math.size)
        assertEquals("\\sin x", math[0].latex)
        assertFalse(math[0].display)
        assertEquals("$\\sin x$", math[0].raw)
    }

    @Test
    fun `行内公式 cos 1`() {
        val math = mathOf("$\\cos 1$")
        assertEquals(1, math.size)
        assertEquals("\\cos 1", math[0].latex)
    }

    @Test
    fun `分式 dfrac`() {
        val math = mathOf("$\\dfrac{\\sin x}{x}$")
        assertEquals(1, math.size)
        assertEquals("\\dfrac{\\sin x}{x}", math[0].latex)
    }

    @Test
    fun `极限与分式组合`() {
        val math = mathOf("$\\lim_{x\\to0}\\dfrac{\\sin x}{x}$")
        assertEquals(1, math.size)
        assertEquals("\\lim_{x\\to0}\\dfrac{\\sin x}{x}", math[0].latex)
    }

    @Test
    fun `积分`() {
        val math = mathOf("$\\int_0^1 x^2\\,dx$")
        assertEquals(1, math.size)
        assertEquals("\\int_0^1 x^2\\,dx", math[0].latex)
    }

    @Test
    fun `求和`() {
        val math = mathOf("$\\sum_{k=1}^n k$")
        assertEquals(1, math.size)
        assertEquals("\\sum_{k=1}^n k", math[0].latex)
    }

    @Test
    fun `矩阵环境`() {
        val math = mathOf("$\\begin{bmatrix}1&2\\\\3&4\\end{bmatrix}$")
        assertEquals(1, math.size)
        assertEquals("\\begin{bmatrix}1&2\\\\3&4\\end{bmatrix}", math[0].latex)
    }

    @Test
    fun `分段函数环境`() {
        val math = mathOf("$\\begin{cases}x^2,&x\\ge0\\\\-x,&x<0\\end{cases}$")
        assertEquals(1, math.size)
        assertEquals("\\begin{cases}x^2,&x\\ge0\\\\-x,&x<0\\end{cases}", math[0].latex)
    }

    @Test
    fun `独立公式双美元符号`() {
        val math = mathOf("$$\\dfrac{a}{b}$$")
        assertEquals(1, math.size)
        assertTrue(math[0].display)
        assertEquals("\\dfrac{a}{b}", math[0].latex)
    }

    @Test
    fun `独立公式支持跨行`() {
        val text = "$$\n\\begin{bmatrix}1&2\\\\3&4\\end{bmatrix}\n$$"
        val math = mathOf(text)
        assertEquals(1, math.size)
        assertTrue(math[0].display)
    }

    @Test
    fun `反斜杠圆括号定界符`() {
        val math = mathOf("行内 \\(\\sin x\\) 公式")
        assertEquals(1, math.size)
        assertFalse(math[0].display)
        assertEquals("\\sin x", math[0].latex)
        assertEquals("行内  公式", plainJoined("行内 \\(\\sin x\\) 公式"))
    }

    @Test
    fun `反斜杠方括号定界符`() {
        val math = mathOf("\\[\\int_0^1 x^2\\,dx\\]")
        assertEquals(1, math.size)
        assertTrue(math[0].display)
    }

    @Test
    fun `中文与行内公式混排保留原文顺序`() {
        val text = "已知 \$x\\ne0\$，求 \$f(x)\$ 的极限。"
        val segments = LatexSegmenter.split(text)
        assertEquals(5, segments.size)
        assertTrue(segments[0] is LatexSegment.PlainText)
        assertEquals("已知 ", (segments[0] as LatexSegment.PlainText).text)
        assertEquals("x\\ne0", (segments[1] as LatexSegment.Math).latex)
        assertEquals("，求 ", (segments[2] as LatexSegment.PlainText).text)
        assertEquals("f(x)", (segments[3] as LatexSegment.Math).latex)
        assertEquals(" 的极限。", (segments[4] as LatexSegment.PlainText).text)
    }

    @Test
    fun `真实极限选择题题干`() {
        val stem = "对 \$x\\ne0\$，设 \$f(x)=\\dfrac{|x|}{x}\\dfrac{\\sin(x-1)+\\sin 1}{x}\$。" +
            "它在 \$0\$ 处的左、右极限依次是多少？"
        val math = mathOf(stem)
        assertEquals(3, math.size)
        assertEquals("x\\ne0", math[0].latex)
        assertEquals("f(x)=\\dfrac{|x|}{x}\\dfrac{\\sin(x-1)+\\sin 1}{x}", math[1].latex)
        assertEquals("0", math[2].latex)
    }

    @Test
    fun `选项文本`() {
        val options = listOf(
            "$\\cos 1,\\ \\cos 1$",
            "$\\cos 1,\\ -\\cos 1$",
            "$-\\cos 1,\\ \\cos 1$"
        )
        options.forEach { option ->
            val math = mathOf(option)
            assertEquals(1, math.size)
            assertFalse(math[0].display)
        }
        assertEquals("-\\cos 1,\\ \\cos 1", mathOf(options[2])[0].latex)
    }

    @Test
    fun `纯文本不含公式`() {
        val text = "下列哪一项是正确的？\nA. 选项一\nB. 选项二"
        assertEquals(0, mathOf(text).size)
        assertFalse(LatexSegmenter.containsMath(text))
        assertEquals(text, plainJoined(text))
    }

    @Test
    fun `围栏代码块不被识别为公式`() {
        val text = "示例代码：\n```kotlin\nval price = \"\$x\$ 与 \$y\$\"\n```\n以上是代码"
        assertEquals(0, mathOf(text).size)
        assertTrue(plainJoined(text).contains("val price"))
    }

    @Test
    fun `行内代码不被识别为公式`() {
        val text = "配置项 `\$HOME` 与公式 \$x\$ 不同"
        val math = mathOf(text)
        assertEquals(1, math.size)
        assertEquals("x", math[0].latex)
        assertTrue(plainJoined(text).contains("`\$HOME`"))
    }

    @Test
    fun `转义美元符号不开启公式`() {
        val text = "售价 \\$5 元，与 \$x\$ 无关"
        val math = mathOf(text)
        assertEquals(1, math.size)
        assertEquals("x", math[0].latex)
        assertTrue(plainJoined(text).contains("\$5 元"))
    }

    @Test
    fun `未闭合美元符号保留为原文`() {
        val text = "这里只有一个 \$ 符号"
        assertEquals(0, mathOf(text).size)
        assertEquals(text, plainJoined(text))
    }

    @Test
    fun `行内公式不跨行`() {
        val text = "第一行 \$a\n第二行 b\$ 结束"
        assertEquals(0, mathOf(text).size)
    }

    @Test
    fun `空公式内容不当作公式`() {
        val text = "空 \$\$ 内容"
        assertEquals(0, mathOf(text).size)
    }

    @Test
    fun `双反斜杠不会被当成方括号定界符`() {
        val text = "行一 \\\\[1mm] 行二"
        assertEquals(0, mathOf(text).size)
    }

    @Test
    fun `连续公式与文本`() {
        val text = "\$\\sin x\$、\$\\cos x\$、\$\\tan x\$"
        val math = mathOf(text)
        assertEquals(3, math.size)
        assertEquals("\\sin x", math[0].latex)
        assertEquals("\\cos x", math[1].latex)
        assertEquals("\\tan x", math[2].latex)
    }
}
