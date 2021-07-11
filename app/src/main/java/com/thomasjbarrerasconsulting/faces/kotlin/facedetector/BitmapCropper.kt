package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.graphics.*
import java.lang.Exception

class BitmapCropper {
    companion object {
        fun cropBitmap(bitmap: Bitmap, rect: Rect, cropToShoulders: Boolean = true): Bitmap {
            val expanded = FaceBoundingBoxExpander.expandedBoundingBox(rect, bitmap.width, bitmap.height, cropToShoulders)

            val x = expanded.left
            val y = expanded.top
            val width = expanded.right - expanded.left
            val height = expanded.bottom - expanded.top

            try{
                val cropped: Bitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
                return cropped
            }
            catch (e: Exception){
                print(e.message)
                return Bitmap.createBitmap(bitmap)
            }
        }
    }
}