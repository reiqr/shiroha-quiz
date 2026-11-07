package com.yiqiu.shirohaquiz.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionInkControllerTest {
    @Test
    fun anotherRegionCannotTakeOverAnActiveStroke() {
        val controller = QuestionInkController(emptyList()) { true }
        controller.changeEnabled(true)

        assertTrue(controller.begin(Offset(10f, 10f), 100, 100, 1f, false, "stem"))
        assertFalse(controller.begin(Offset(20f, 20f), 100, 100, 1f, false, "scratch"))
        controller.move(Offset(30f, 30f), 100, 100, 1f, "scratch")
        controller.finish("scratch")
        assertTrue(controller.strokes.isEmpty())

        controller.move(Offset(40f, 40f), 100, 100, 1f, "stem")
        controller.finish("stem")
        assertEquals(1, controller.strokes.size)
        assertEquals("stem", controller.strokes.single().anchor)
        assertEquals(2, controller.strokes.single().points.size)
    }
}
