package com.yiqiu.shirohaquiz.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexMathSegmentsTest {
    @Test
    fun detectsIssue145FormulasAndMixedDelimiters() {
        val stem = "For \$x\\ne0\$, let \$f(x)=\\dfrac{|x|}{x}\\dfrac{\\sin(x-1)+\\sin 1}{x}\$."
        val segments = findLatexMathSegments(stem)
        assertEquals(
            listOf("x\\ne0", "f(x)=\\dfrac{|x|}{x}\\dfrac{\\sin(x-1)+\\sin 1}{x}"),
            segments.map { it.formula }
        )
        assertEquals("\$x\\ne0\$", stem.substring(segments[0].start, segments[0].end))
        assertEquals("\\cos 1,\\ -\\cos 1", findLatexMathSegments("\$\\cos 1,\\ -\\cos 1\$").single().formula)
        val mixed = "\\(a_1^2\\)\n\$\$\\int_0^1 x\\,dx\$\$\n" +
            "\\[\\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}\\]"
        assertEquals(listOf(false, true, true), findLatexMathSegments(mixed).map { it.display })
    }

    @Test
    fun leavesCodeCurrencyAndMalformedInputAlone() {
        assertTrue(findLatexMathSegments("Costs \$5 and \$10.").isEmpty())
        assertTrue(findLatexMathSegments("\\\$x\$").isEmpty())
        assertTrue(findLatexMathSegments("\u0060\$x\$\u0060\n~~~\n\$x\$\n~~~").isEmpty())
        assertTrue(findLatexMathSegments("\$x\nnext line\$ and \\(unclosed").isEmpty())
        assertTrue(findLatexMathSegments("\\[" + "x".repeat(1025) + "\\]").isEmpty())
    }
}
