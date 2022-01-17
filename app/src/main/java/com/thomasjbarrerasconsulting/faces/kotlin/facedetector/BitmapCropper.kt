/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.graphics.*
import java.lang.Exception

class BitmapCropper {
    companion object {
        fun cropBitmap(bitmap: Bitmap, rect: Rect, classificationType: FaceClassifierProcessor.Classifier): Bitmap {
            val expanded = FaceBoundingBoxExpander.expandedBoundingBox(rect, bitmap.width, bitmap.height, classificationType)

            val x = expanded.left
            val y = expanded.top
            val width = expanded.right - expanded.left
            val height = expanded.bottom - expanded.top

            return try {
                Bitmap.createBitmap(bitmap, x, y, width, height)
            } catch (e: Exception) {
                print(e.message)
                Bitmap.createBitmap(bitmap)
            }
        }
    }
}