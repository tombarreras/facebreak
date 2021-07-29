package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FaceBoundingBoxExpander {
    companion object {
        private fun intersectRects(rect1: Rect, rect2:Rect):Rect{
            val newLeft = max(rect1.left, rect2.left)
            val newTop = max(rect1.top, rect2.top)
            val newRight = min(rect1.right, rect2.right)
            val newBottom = min(rect1.bottom, rect2.bottom)

            return Rect(newLeft, newTop, newRight, newBottom)
        }

        private fun getDesiredSquare(faceRect: Rect, cropToShoulders: Boolean): Rect {
            val factor = 1.5
            val d = max(faceRect.width(), faceRect.height())
            var leftDesired = (faceRect.left + faceRect.width() / 2 - 0.5 * factor * d).toInt()
            val topDesired = (faceRect.top + faceRect.height() /2  - 0.5 * factor * d).toInt()
            var widthDesired = (factor * d).toInt()

            if (cropToShoulders){
                leftDesired = (faceRect.left + faceRect.width() / 2 - factor * d).toInt()
                widthDesired = (2 * factor * d).toInt()
            }
            return Rect(leftDesired, topDesired, leftDesired + widthDesired, topDesired + widthDesired)
        }

        fun topWithFaceCentered(faceTop: Int, faceHeight: Int, desiredTotalHeight: Int): Int {
            val desiredTop = faceTop - (desiredTotalHeight - faceHeight) / 2
            if (desiredTop < 0){
                return 0
            }
            return desiredTop
        }

        private fun squareByTrimmingBottom(rect:Rect):Rect{
            return Rect(rect.left, rect.top, rect.right, rect.top + rect.width())
        }

        private fun squareByCenteringFaceVertically(rect:Rect, faceRect:Rect):Rect{
            val newTop = topWithFaceCentered(faceRect.top, faceRect.height(), rect.width())
            return Rect(rect.left, newTop, rect.right, newTop + rect.width())
        }

        fun expandedBoundingBox(boundingBox: Rect, imageWidth: Int, imageHeight: Int, classificationType: String): Rect{
            val cropToShoulders = (classificationType == FaceClassifierProcessor.DETECT_GENDER ||
                    classificationType == FaceClassifierProcessor.DETECT_AGE ||
                    classificationType == FaceClassifierProcessor.DETECT_FEATURES ||
                    classificationType == FaceClassifierProcessor.DETECT_ANCESTRY)

            val desired = getDesiredSquare(boundingBox, cropToShoulders)
            val intersection = intersectRects(desired, Rect(0, 0, imageWidth, imageHeight))

            if (intersection.width() == intersection.height()){
                return intersection
            }

            // Taller than wide
            if (intersection.width() < intersection.height()){
                return if (cropToShoulders){
                    // Trim the bottom up to the point where the face is centered
                    if (2 * (boundingBox.top - intersection.top) + boundingBox.height() < intersection.width()){
                        squareByTrimmingBottom(intersection)
                    } else {
                        // Trim so the face is centered
                        squareByCenteringFaceVertically(intersection, boundingBox)
                    }
                } else {
                    // Trim top and bottom evenly
                    squareByCenteringFaceVertically(intersection, boundingBox)
                }
            } else {
                // Wider than tall. Try centering the face
                desired.left = boundingBox.left + boundingBox.width() / 2 - intersection.height() / 2

                // If the desired square is within the image, we're done
                if (desired.left > intersection.left && desired.left + intersection.height() < intersection.left + intersection.width()){
                    return Rect(desired.left, intersection.top, desired.left + intersection.height(), intersection.bottom)
                }

                // if the desired square is left of the image, slide right
                if (desired.left < intersection.left){
                    return Rect(intersection.left, intersection.top,intersection.left + intersection.height(), intersection.bottom)
                }

                // if the desired square is right of the image, slide left
                return Rect(intersection.left + intersection.width() - intersection.height(), intersection.top, intersection.right, intersection.bottom)
            }
        }
    }
}