package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

class BitmapScaler {
    companion object {
        fun scaleBitmap(bitmap: Bitmap, scale: Float, width: Int, height: Int): Bitmap {
            // Determine how much to scale the image.
            val fullScaleFactor = min(
                scale * width.toFloat() / bitmap.width.toFloat(),
                scale * height.toFloat() / bitmap.height.toFloat()
            )

            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (fullScaleFactor * bitmap.width).toInt(),
                (fullScaleFactor * bitmap.height).toInt(),
                true
            )
            val x = max(0, (scaledBitmap.width - width) / 2)
            val y = max(0, (scaledBitmap.height - height) / 2)
            val w = min(width, scaledBitmap.width - x)
            val h = min(height, scaledBitmap.height - y)

            return Bitmap.createBitmap(scaledBitmap, x, y, w, h)
        }
    }

}