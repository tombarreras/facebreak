package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapScalerTest {

    private fun createBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun `scaleBitmap outputs correct dimensions`() {
        val bitmap = createBitmap(200, 200)
        val result = BitmapScaler.scaleBitmap(bitmap, 1.0f, 100, 100)

        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `scaleBitmap scales up small bitmap`() {
        val bitmap = createBitmap(50, 50)
        val result = BitmapScaler.scaleBitmap(bitmap, 1.0f, 200, 200)

        assertEquals(200, result.width)
        assertEquals(200, result.height)
    }

    @Test
    fun `scaleBitmap with scale factor applies correctly`() {
        val bitmap = createBitmap(100, 100)
        val result = BitmapScaler.scaleBitmap(bitmap, 2.0f, 100, 100)

        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `scaleBitmap with different aspect ratio maintains target dimensions`() {
        val bitmap = createBitmap(400, 200) // wide bitmap
        val result = BitmapScaler.scaleBitmap(bitmap, 1.0f, 100, 100) // square target

        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `scaleBitmap with tall bitmap maintains target dimensions`() {
        val bitmap = createBitmap(200, 400) // tall bitmap
        val result = BitmapScaler.scaleBitmap(bitmap, 1.0f, 100, 100) // square target

        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `scaleBitmap preserves bitmap config`() {
        val bitmap = createBitmap(100, 100)
        val result = BitmapScaler.scaleBitmap(bitmap, 1.0f, 50, 50)

        assertEquals(Bitmap.Config.ARGB_8888, result.config)
    }
}
