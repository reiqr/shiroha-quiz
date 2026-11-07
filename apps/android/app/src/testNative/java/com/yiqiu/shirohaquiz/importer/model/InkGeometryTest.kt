package com.yiqiu.shirohaquiz.importer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM regression tests for card-relative handwriting and stroke eraser geometry. */
class InkGeometryTest {
    @Test fun hitTestFindsMiddleOfSegment() {
        val line = InkStroke(points = listOf(InkPoint(0.1f, 10f), InkPoint(0.9f, 10f)))
        assertEquals(0f, InkGeometry.distanceToStrokeDp(50f, 10f, line, 100f), 0.01f)
        assertEquals(15f, InkGeometry.distanceToStrokeDp(50f, 25f, line, 100f), 0.01f)
    }

    @Test fun singlePointAndEmptyPathsAreHandled() {
        val dot = InkStroke(points = listOf(InkPoint(0.5f, 30f)))
        assertEquals(0f, InkGeometry.distanceToStrokeDp(50f, 30f, dot, 100f), 0.01f)
        assertTrue(InkGeometry.distanceToStrokeDp(5f, 5f, dot.copy(points = emptyList()), 100f).isInfinite())
    }

    @Test fun rejectsCorruptCoordinatesAndUnsupportedTools() {
        val valid = InkStroke(tool = "marker", widthDp = 14f, points = listOf(InkPoint(0.4f, 100f)))
        assertTrue(InkGeometry.isValid(valid))
        assertFalse(InkGeometry.isValid(valid.copy(tool = "eraser")))
        assertFalse(InkGeometry.isValid(valid.copy(points = listOf(InkPoint(Float.NaN, 2f)))))
        assertFalse(InkGeometry.isValid(valid.copy(points = listOf(InkPoint(-0.1f, 2f)))))
        assertFalse(InkGeometry.isValid(valid.copy(points = emptyList())))
    }

    @Test fun questionCopyPreservesAnnotations() {
        val line = InkStroke(points = listOf(InkPoint(0.2f, 12f)))
        val original = Question(type = QuestionType.SINGLE, question = "Example", inkStrokes = listOf(line))
        assertEquals(listOf(line), original.copy(question = "Edited").inkStrokes)
    }

    @Test fun anchoredStrokeRescalesWithItsOwnRegion() {
        val optionInk = InkStroke(
            anchor = "option:B", referenceHeightDp = 40f,
            points = listOf(InkPoint(0.2f, 12f), InkPoint(0.9f, 12f))
        )
        assertTrue(InkGeometry.isValid(optionInk))
        val resized = InkGeometry.scaledForHeight(optionInk, 80f)
        assertEquals(24f, resized.points.first().yDp, 0.01f)
        assertEquals("option:B", resized.anchor)
        assertEquals(40f, optionInk.referenceHeightDp, 0.01f)
    }

    @Test fun invalidAnchorsAndMassivePointSetsAreRejected() {
        val valid = InkStroke(anchor = "scratch", referenceHeightDp = 160f,
            points = listOf(InkPoint(0.5f, 20f)))
        assertTrue(InkGeometry.isValid(valid))
        assertFalse(InkGeometry.isValid(valid.copy(anchor = "option:../../bad")))
        assertFalse(InkGeometry.isValid(valid.copy(anchor = "stem", referenceHeightDp = 0f)))
        val large = valid.copy(points = List(1200) { InkPoint(0.5f, 20f) })
        assertTrue(InkGeometry.withinLimits(List(70) { large }))
        assertFalse(InkGeometry.withinLimits(List(601) { large }))
        assertTrue(70 * 1200 > InkGeometry.MAX_NEW_DRAW_POINTS)
    }

    @Test fun legacyStrokeKeepsItsOriginalCoordinates() {
        val legacy = InkStroke(points = listOf(InkPoint(0.25f, 105f)))
        assertTrue(InkGeometry.isValid(legacy))
        assertEquals(105f, InkGeometry.scaledForHeight(legacy, 320f).points[0].yDp, 0.01f)
    }
}
