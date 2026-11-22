package com.yiqiu.shirohaquiz.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexMathSegmentsTest {
    private fun segments(text: String) = LatexSegmenter.split(text).filterIsInstance<LatexSegment.Math>()

    @Test
    fun detectsIssue145FormulasAndMixedDelimiters() {
        val stem = "For \$x\\ne0\$, let \$f(x)=\\dfrac{|x|}{x}\\dfrac{\\sin(x-1)+\\sin 1}{x}\$."
        val segments = segments(stem)
        assertEquals(
            listOf("x\\ne0", "f(x)=\\dfrac{|x|}{x}\\dfrac{\\sin(x-1)+\\sin 1}{x}"),
            segments.map { it.latex }
        )
        assertEquals("\$x\\ne0\$", segments[0].raw)
        assertEquals("\\cos 1,\\ -\\cos 1", segments("\$\\cos 1,\\ -\\cos 1\$").single().latex)
        val mixed = "\\(a_1^2\\)\n\$\$\\int_0^1 x\\,dx\$\$\n" +
            "\\[\\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}\\]"
        assertEquals(listOf(false, true, true), segments(mixed).map { it.display })
    }

    @Test
    fun leavesCodeCurrencyAndMalformedInputAlone() {
        assertTrue(segments("Costs \$5 and \$10.").isEmpty())
        assertTrue(segments("\$x\$2").isEmpty())
        assertEquals(listOf("x^2"), segments("Costs \$5 and \$10; then \$x^2\$.").map { it.latex })
        assertTrue(segments("\\\$x\$").isEmpty())
        assertTrue(segments("\u0060\$x\$\u0060\n~~~\n\$x\$\n~~~").isEmpty())
        assertTrue(segments("\$x\nnext line\$ and \\(unclosed").isEmpty())
        assertTrue(segments("\\[" + "x".repeat(16385) + "\\]").isEmpty())
    }

    @Test
    fun formulaLimitPreservesUnrenderedTail() {
        val text = "\$x\$ ".repeat(49) + "tail"
        val pieces = LatexSegmenter.split(text)
        assertEquals(48, pieces.count { it is LatexSegment.Math })
        assertEquals(LatexSegment.PlainText(" \$x\$ tail"), pieces.last())
        assertEquals(text, pieces.joinToString("") {
            when (it) {
                is LatexSegment.Math -> it.raw
                is LatexSegment.PlainText -> it.text
            }
        })
    }
}
