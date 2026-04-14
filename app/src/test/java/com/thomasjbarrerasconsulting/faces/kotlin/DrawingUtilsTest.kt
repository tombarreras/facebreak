package com.thomasjbarrerasconsulting.faces.kotlin

import android.graphics.Paint
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DrawingUtilsTest {

    private val paint = Paint().apply { textSize = 20f }

    @Test
    fun `single word returns one line`() {
        val result = DrawingUtils.splitText("Hello", paint, 1000)
        assertEquals(1, result.size)
        assertEquals("Hello", result[0])
    }

    @Test
    fun `text that fits on one line stays on one line`() {
        val result = DrawingUtils.splitText("Hello World", paint, 1000)
        assertEquals(1, result.size)
        assertEquals("Hello World", result[0])
    }

    @Test
    fun `text wraps to multiple lines when exceeding width`() {
        // Use a very narrow width to force wrapping
        val result = DrawingUtils.splitText("Hello World Test", paint, 1)

        assertTrue("Should wrap to multiple lines", result.size > 1)
    }

    @Test
    fun `continuation lines are indented`() {
        // Use a very narrow width to force wrapping
        val result = DrawingUtils.splitText("Hello World Test", paint, 1)

        // Second and subsequent lines should be indented with 4 spaces
        for (i in 1 until result.size) {
            assertTrue(
                "Line $i should be indented: '${result[i]}'",
                result[i].startsWith("    ")
            )
        }
    }

    @Test
    fun `first line is not indented`() {
        val result = DrawingUtils.splitText("Hello World Test", paint, 1)

        assertFalse(result[0].startsWith("    "))
    }
}
