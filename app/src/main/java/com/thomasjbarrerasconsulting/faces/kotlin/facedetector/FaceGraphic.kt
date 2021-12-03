/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import android.graphics.*
import com.thomasjbarrerasconsulting.faces.GraphicOverlay
import com.thomasjbarrerasconsulting.faces.GraphicOverlay.Graphic
import com.google.mlkit.vision.face.Face
import com.thomasjbarrerasconsulting.faces.kotlin.DrawingUtils
import com.thomasjbarrerasconsulting.faces.preference.DisplayPreferences
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
class FaceGraphic constructor(context: Context, overlay: GraphicOverlay?, private val face: Face, private val faceClassifications: List<String>, private val classificationType: String) : Graphic(overlay) {
  private val classificationTextPaint: Paint = Paint()
  private val boxPaint: Paint = Paint()
  private val displayPreferences: DisplayPreferences = DisplayPreferences.getDisplayPreferences(context)

  init {
    configureClassificationTextPaint(context, classificationTextPaint)

    boxPaint.color = displayPreferences.faceBoxColor
    boxPaint.style = Paint.Style.STROKE
    boxPaint.strokeWidth = displayPreferences.faceBoxWidth
    boxPaint.pathEffect = CornerPathEffect(10.0f)
    boxPaint.setShadowLayer(5.0f, -5.0f, 5.0f, Color.BLACK)
  }

  /** Draws the face annotations for position on the supplied canvas.  */
  override fun draw(canvas: Canvas) {
    // Draws the bounding box.
    val faceBoundingBox = RectF(face.boundingBox)
    val x0 = translateX(faceBoundingBox.left)
    val x1 = translateX(faceBoundingBox.right)
    faceBoundingBox.left = min(x0, x1)
    faceBoundingBox.right = max(x0, x1)
    faceBoundingBox.top = translateY(faceBoundingBox.top)
    faceBoundingBox.bottom = translateY(faceBoundingBox.bottom)
    val expandedRect = FaceBoundingBoxExpander.expandedBoundingBox(Rect(faceBoundingBox.left.roundToInt(), faceBoundingBox.top.roundToInt(), faceBoundingBox.right.roundToInt(), faceBoundingBox.bottom.roundToInt()),
      canvas.width, canvas.height, classificationType)

    val expandedRectF = RectF(expandedRect)

    canvas.drawRect(expandedRectF, boxPaint)
    if (3.0f * FACE_CLASSIFICATION_TEXT_SIZE_LARGE * faceClassifications.count().toFloat() < canvas.height.toFloat()){
      classificationTextPaint.textSize = FACE_CLASSIFICATION_TEXT_SIZE_LARGE * displayPreferences.classifierTextSize
    } else {
      classificationTextPaint.textSize = FACE_CLASSIFICATION_TEXT_SIZE_SMALL * displayPreferences.classifierTextSize
    }
    val sentences = mutableListOf<String>()
    for (classification in faceClassifications) {
       sentences.addAll(DrawingUtils.splitText(classification, classificationTextPaint, canvas.width))
    }

    var position = 0
    val classificationX = 0.5f * classificationTextPaint.textSize
    for (sentence in sentences){
      val classificationY = canvas.height - (classificationTextPaint.textSize * 1.5f * (sentences.size - position++).toFloat())
      canvas.drawText(
        sentence,
        classificationX,
        classificationY,
        classificationTextPaint
      )
    }
  }

  companion object {
    private const val FACE_CLASSIFICATION_TEXT_SIZE_LARGE = 60.0f
    private const val FACE_CLASSIFICATION_TEXT_SIZE_SMALL = 50.0f

    fun configureClassificationTextPaint (context: Context, paint: Paint){
      paint.color = DisplayPreferences.getDisplayPreferences(context).classifierTextColor
      paint.textSize = FACE_CLASSIFICATION_TEXT_SIZE_LARGE
      paint.setShadowLayer(5.0f, -5.0f, 5.0f, Color.BLACK)
    }
  }
}
