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

import android.R.attr
import android.graphics.*
import com.thomasjbarrerasconsulting.faces.GraphicOverlay
import com.thomasjbarrerasconsulting.faces.GraphicOverlay.Graphic
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import android.R.attr.lineHeight
import java.util.*
import android.R.attr.x
import android.R.attr.y








/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
class FaceGraphic constructor(overlay: GraphicOverlay?, private val face: Face, private val faceClassifications: List<String>, private val classificationType: String) : Graphic(overlay) {
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
    // Decide color based on face ID
    val colorID = if (face.trackingId == null) 0 else abs(face.trackingId!! % NUM_COLORS)

    // Draws the bounding box.
    val faceBoundingBox = RectF(face.boundingBox)
    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
    val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
//    val eyesBoundingBox = if (leftEye == null || rightEye == null) null else RectF(leftEye.position.x, leftEye.position.y, rightEye.position.x + rightEye.position.length(), rightEye.position.y + rightEye.position.length() / 2)

    val x0 = translateX(faceBoundingBox.left)
    val x1 = translateX(faceBoundingBox.right)
    faceBoundingBox.left = min(x0, x1)
    faceBoundingBox.right = max(x0, x1)
    faceBoundingBox.top = translateY(faceBoundingBox.top)
    faceBoundingBox.bottom = translateY(faceBoundingBox.bottom)

//    val faceHeight = faceBoundingBox.bottom - faceBoundingBox.top
//    val eyesBoundingBox = RectF(faceBoundingBox.left, faceBoundingBox.top + faceHeight / 4, faceBoundingBox.right, faceBoundingBox.bottom - faceHeight / 2)
    val expandedRect = FaceBoundingBoxExpander.expandedBoundingBox(Rect(faceBoundingBox.left.roundToInt(), faceBoundingBox.top.roundToInt(), faceBoundingBox.right.roundToInt(), faceBoundingBox.bottom.roundToInt()),
      canvas.width, canvas.height, classificationType)

    val expandedRectF = RectF(expandedRect)

    canvas.drawRect(expandedRectF, boxPaints[colorID])
//    canvas.drawRect(RectF(eyesBoundingBox), boxPaints[colorID])
//    val left = x - scale(face.boundingBox.width() / 2.0f)
//    val top = y - scale(face.boundingBox.height() / 2.0f)
//    val lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH
//    val yLabelOffset: Float = if (face.trackingId == null) 0.0f else -lineHeight
//    if (face.leftEyeOpenProbability != null) {
//      canvas.drawText(
//        "Left eye open: " + String.format(Locale.US, "%.2f", face.leftEyeOpenProbability),
//        left,
//        top + yLabelOffset,
//        idPaints[colorID]);
//    }
//    if (leftEye != null) {
//      val leftEyeLeft =
//        translateX(leftEye.position.x) - idPaints[colorID].measureText("Left Eye") / 2.0f
//      canvas.drawRect(
//        leftEyeLeft - BOX_STROKE_WIDTH,
//        translateY(leftEye.position.y) + ID_Y_OFFSET - ID_TEXT_SIZE,
//        leftEyeLeft + idPaints[colorID].measureText("Left Eye") + BOX_STROKE_WIDTH,
//        translateY(leftEye.position.y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
//        labelPaints[colorID]
//      )
//      canvas.drawText(
//        "Left Eye",
//        leftEyeLeft,
//        translateY(leftEye.position.y) + ID_Y_OFFSET,
//        idPaints[colorID]
//      )
//    }

//    if (rightEye != null) {
//      val rightEyeLeft =
//        translateX(rightEye.position.x) - idPaints[colorID].measureText("Right Eye") / 2.0f
//      canvas.drawRect(
//        rightEyeLeft - BOX_STROKE_WIDTH,
//        translateY(rightEye.position.y) + ID_Y_OFFSET - ID_TEXT_SIZE,
//        rightEyeLeft + idPaints[colorID].measureText("Right Eye") + BOX_STROKE_WIDTH,
//        translateY(rightEye.position.y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
//        labelPaints[colorID]
//      )
//      canvas.drawText(
//        "Right Eye",
//        rightEyeLeft,
//        translateY(rightEye.position.y) + ID_Y_OFFSET,
//        idPaints[colorID]
//      )
//    }

//    canvas.drawRect(rect, boxPaints[colorID])

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
