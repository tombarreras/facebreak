package com.thomasjbarrerasconsulting.faces

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageUtils {
    companion object {
        private fun getImagePath(context: Context, imageName:String, format:Bitmap.CompressFormat):String {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()

            val extension = when (format) {
                Bitmap.CompressFormat.JPEG -> "jpg"
                Bitmap.CompressFormat.PNG -> "png"
                else -> "webp"
            }
            return "$cachePath/$imageName.$extension"
        }

        fun saveImageToCache(context: Context, imageBitmap:Bitmap, imageName:String, format:Bitmap.CompressFormat):Boolean {
            var success = false
            try {
                val stream = FileOutputStream(getImagePath(context, imageName, format)) // overwrites this image every time

                imageBitmap.compress(format, 100, stream)
                stream.close()
                success = true

            } catch (e: IOException) {
                e.printStackTrace()
            }
            return success
        }

        fun getImageFromCache(context:Context, imageName:String, format:Bitmap.CompressFormat):Bitmap? {
            return BitmapFactory.decodeFile(getImagePath(context, imageName, format))
        }

        fun deleteImageFromCache(context:Context, imageName:String, format:Bitmap.CompressFormat):Boolean {
            var success = false
            try {
                File(getImagePath(context, imageName, format)).delete()
                success = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return success
        }
    }
}