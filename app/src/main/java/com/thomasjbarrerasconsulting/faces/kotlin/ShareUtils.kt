/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceGraphic
import java.io.File

class ShareUtils {
    companion object {
        fun createShareIntent(context: Context): Intent{
            val imagePath: File = File(context.cacheDir, "images")
            val newFile = File(imagePath, "${StillImageActivity.SHARED_IMAGE_NAME}.jpg")
            val contentUri: Uri = FileProvider.getUriForFile(context, "com.thomasjbarrerasconsulting.faces.fileprovider", newFile)

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, context.contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            return shareIntent
        }

        fun drawClassifierAndLogo(context:Context, canvas:Canvas, classifier:String) {
            val textPaint = Paint()
            FaceGraphic.configureClassificationTextPaint(context, textPaint)
            val positionX = 0.5f * textPaint.textSize
            var positionY = -0.25f * textPaint.textSize
            val message = classifier + " " + context.getString(R.string.brought_to_you_by) + " "
            val logo = "FaceBreak"
            val splitText = DrawingUtils.splitText(message + logo, textPaint, canvas.width)
            for (text in splitText) {
                positionY += textPaint.textSize * 1.5f
                canvas.drawText(text.replace(logo, ""), positionX, positionY, textPaint)
            }
            val logoX = positionX + textPaint.measureText(splitText.last().replace(logo, ""))
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText(logo, logoX, positionY, textPaint)
        }
    }
}