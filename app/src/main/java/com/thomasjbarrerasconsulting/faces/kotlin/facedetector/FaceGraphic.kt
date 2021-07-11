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

import android.graphics.*
import com.thomasjbarrerasconsulting.faces.GraphicOverlay
import com.thomasjbarrerasconsulting.faces.GraphicOverlay.Graphic
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
class FaceGraphic constructor(overlay: GraphicOverlay?, private val face: Face, private val faceClassifications: List<String>) : Graphic(overlay) {
  private val facePositionPaint: Paint
  private val numColors = COLORS.size
  private val idPaints = Array(numColors) { Paint() }
  private val boxPaints = Array(numColors) { Paint() }
  private val labelPaints = Array(numColors) { Paint() }
  private val classificationTextPaint: Paint = Paint()

  init {
    classificationTextPaint.color = Color.WHITE
    classificationTextPaint.textSize = FACE_CLASSIFICATION_TEXT_SIZE
    classificationTextPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK)

    val selectedColor = Color.WHITE
    facePositionPaint = Paint()
    facePositionPaint.color = selectedColor
    for (i in 0 until numColors) {
      idPaints[i] = Paint()
      idPaints[i].color = COLORS[i][1]
      idPaints[i].textSize = ID_TEXT_SIZE
      boxPaints[i] = Paint()
      boxPaints[i].color = COLORS[i][1]
      boxPaints[i].style = Paint.Style.STROKE
      boxPaints[i].strokeWidth = BOX_STROKE_WIDTH
      labelPaints[i] = Paint()
      labelPaints[i].color = COLORS[i][1]
      labelPaints[i].style = Paint.Style.FILL
    }
  }

  /** Draws the face annotations for position on the supplied canvas.  */
  override fun draw(canvas: Canvas) {
//    val x = translateX(face.boundingBox.centerX().toFloat())
//    val y = translateY(face.boundingBox.centerY().toFloat())

    // Calculate positions.
//    val left = x - scale(face.boundingBox.width() / 2.0f)
//    val top = y - scale(face.boundingBox.height() / 2.0f)
//    val right = x + scale(face.boundingBox.width() / 2.0f)
//    val bottom = y + scale(face.boundingBox.height() / 2.0f)
//    val lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH
//    var yLabelOffset: Float = if (face.trackingId == null) 0f else -lineHeight

    // Decide color based on face ID
    val colorID = if (face.trackingId == null) 0 else abs(face.trackingId!! % NUM_COLORS)

    // Draws the bounding box.
    val rect = RectF(face.boundingBox)
    val x0 = translateX(rect.left)
    val x1 = translateX(rect.right)
    rect.left = min(x0, x1)
    rect.right = max(x0, x1)
    rect.top = translateY(rect.top)
    rect.bottom = translateY(rect.bottom)
//    canvas.drawRect(FaceBoundingBoxExpander.expandedBoundingBox(Rect(rect.left.roundToInt(), rect.top.roundToInt(), rect.right.roundToInt(), rect.bottom.roundToInt()),
//      canvas.width, canvas.height), boxPaints[colorID])
    canvas.drawRect(rect, boxPaints[colorID])

    // Draw face classification text.
//    val classificationX = rect.left
    val classificationX = FACE_CLASSIFICATION_TEXT_SIZE * 0.5f
//    var classificationY = rect.bottom

    for (i in faceClassifications.indices) {
//      classificationY += (FACE_CLASSIFICATION_TEXT_SIZE * 1.5f)
      val classificationY = canvas.height - (
              FACE_CLASSIFICATION_TEXT_SIZE * 1.5f * (faceClassifications.size - i).toFloat()
              )
      canvas.drawText(
        faceClassifications[i],
        classificationX,
        classificationY,
        classificationTextPaint
      )
    }
  }

  private fun drawFaceLandmark(canvas: Canvas, @LandmarkType landmarkType: Int) {
    val faceLandmark = face.getLandmark(landmarkType)
    if (faceLandmark != null) {
      canvas.drawCircle(
        translateX(faceLandmark.position.x),
        translateY(faceLandmark.position.y),
        FACE_POSITION_RADIUS,
        facePositionPaint
      )
    }
  }

  companion object {
    private const val FACE_CLASSIFICATION_TEXT_SIZE = 60.0f
    private const val FACE_POSITION_RADIUS = 8.0f
    private const val ID_TEXT_SIZE = 30.0f
    private const val ID_Y_OFFSET = 40.0f
    private const val BOX_STROKE_WIDTH = 5.0f
    private const val NUM_COLORS = 10
    private val COLORS =
      arrayOf(
        intArrayOf(Color.BLACK, Color.WHITE),
        intArrayOf(Color.WHITE, Color.MAGENTA),
        intArrayOf(Color.BLACK, Color.LTGRAY),
        intArrayOf(Color.WHITE, Color.RED),
        intArrayOf(Color.WHITE, Color.BLUE),
        intArrayOf(Color.WHITE, Color.DKGRAY),
        intArrayOf(Color.BLACK, Color.CYAN),
        intArrayOf(Color.BLACK, Color.YELLOW),
        intArrayOf(Color.WHITE, Color.BLACK),
        intArrayOf(Color.BLACK, Color.GREEN)
      )
  }
}
