/*
 * Copyright 2021 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.graphics.*
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

            val x = max(0.0f,  ((scaledBitmap.width - width) / 2.0f) ).toInt()
            val y = max(0.0f,  ((scaledBitmap.height - height) / 2.0f)).toInt()
            val w = min(width.toFloat(), scaledBitmap.width.toFloat() - x).toInt()
            val h = min(height.toFloat(), scaledBitmap.height.toFloat() - y).toInt()

            val croppedBitmap = Bitmap.createBitmap(scaledBitmap, x, y, w, h)

            val fullBitmap = Bitmap.createBitmap(width, height, bitmap.config)
            val canvas = Canvas(fullBitmap)
            canvas.drawBitmap(croppedBitmap, (width - croppedBitmap.width) / 2.0f, (height - croppedBitmap.height) / 2.0f, null)
            return fullBitmap
        }
    }

}