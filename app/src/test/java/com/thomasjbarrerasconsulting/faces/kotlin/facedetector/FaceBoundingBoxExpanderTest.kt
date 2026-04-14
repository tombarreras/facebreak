package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FaceBoundingBoxExpanderTest {

    // Standard image dimensions for tests
    private val imageWidth = 1000
    private val imageHeight = 1000

    // A face centered in the image
    private val centeredFace = Rect(400, 400, 600, 600)

    @Test
    fun `topWithFaceCentered returns 0 when face is near top`() {
        val result = FaceBoundingBoxExpander.topWithFaceCentered(10, 100, 500)
        assertEquals(0, result)
    }

    @Test
    fun `topWithFaceCentered centers face vertically`() {
        val result = FaceBoundingBoxExpander.topWithFaceCentered(400, 200, 500)
        // desiredTop = 400 - 0.6 * (500 - 200) = 400 - 180 = 220
        assertEquals(220, result)
    }

    @Test
    fun `expandedBoundingBox cropToEyes returns eye region`() {
        val face = Rect(200, 200, 400, 400) // 200x200 face
        val result = FaceBoundingBoxExpander.expandedBoundingBox(
            face, imageWidth, imageHeight,
            FaceClassifierProcessor.Classifier.DETECT_EYE_COLOR
        )

        // left = 200 + 0.15*200 = 230
        // top = 200 + 0.2*200 = 240
        // right = 400 - 0.15*200 = 370
        // bottom = 400 - 0.5*200 = 300
        assertEquals(230, result.left)
        assertEquals(240, result.top)
        assertEquals(370, result.right)
        assertEquals(300, result.bottom)
    }

    @Test
    fun `expandedBoundingBox centered face returns square`() {
        val result = FaceBoundingBoxExpander.expandedBoundingBox(
            centeredFace, imageWidth, imageHeight,
            FaceClassifierProcessor.Classifier.DETECT_FACE_SHAPE
        )

        // Result should be a square
        assertEquals(result.width(), result.height())
        // Result should be within image bounds
        assertTrue(result.left >= 0)
        assertTrue(result.top >= 0)
        assertTrue(result.right <= imageWidth)
        assertTrue(result.bottom <= imageHeight)
    }

    @Test
    fun `expandedBoundingBox cropToShoulders is wider than default`() {
        val shoulderResult = FaceBoundingBoxExpander.expandedBoundingBox(
            centeredFace, imageWidth, imageHeight,
            FaceClassifierProcessor.Classifier.DETECT_AGE
        )
        val defaultResult = FaceBoundingBoxExpander.expandedBoundingBox(
            centeredFace, imageWidth, imageHeight,
            FaceClassifierProcessor.Classifier.DETECT_FACE_SHAPE
        )

        assertTrue(shoulderResult.width() >= defaultResult.width())
    }

    @Test
    fun `expandedBoundingBox face near left edge stays in bounds`() {
        val leftFace = Rect(10, 400, 110, 500)
        val result = FaceBoundingBoxExpander.expandedBoundingBox(
            leftFace, imageWidth, imageHeight,
            FaceClassifierProcessor.Classifier.DETECT_ANCESTRY
        )

        assertTrue(result.left >= 0)
        assertTrue(result.right <= imageWidth)
        assertEquals(result.width(), result.height())
    }

    @Test
    fun `expandedBoundingBox face near right edge stays in bounds`() {
        val rightFace = Rect(890, 400, 990, 500)
        val result = FaceBoundingBoxExpander.expandedBoundingBox(
            rightFace, imageWidth, imageHeight,
            FaceClassifierProcessor.Classifier.DETECT_ANCESTRY
        )

        assertTrue(result.left >= 0)
        assertTrue(result.right <= imageWidth)
        assertEquals(result.width(), result.height())
    }

    @Test
    fun `expandedBoundingBox face near top edge stays in bounds`() {
        val topFace = Rect(400, 10, 600, 110)
        val result = FaceBoundingBoxExpander.expandedBoundingBox(
            topFace, imageWidth, imageHeight,
            FaceClassifierProcessor.Classifier.DETECT_FACE_SHAPE
        )

        assertTrue(result.top >= 0)
        assertTrue(result.bottom <= imageHeight)
    }

    @Test
    fun `expandedBoundingBox all shoulder classifier types use wider crop`() {
        val shoulderClassifiers = listOf(
            FaceClassifierProcessor.Classifier.DETECT_GENDER,
            FaceClassifierProcessor.Classifier.DETECT_AGE,
            FaceClassifierProcessor.Classifier.DETECT_FEATURES,
            FaceClassifierProcessor.Classifier.DETECT_CHARACTER,
            FaceClassifierProcessor.Classifier.DETECT_CHARACTER_FLAWS,
            FaceClassifierProcessor.Classifier.DETECT_HAIR_STYLE,
            FaceClassifierProcessor.Classifier.DETECT_HAIR_COLOR
        )

        val nonShoulderResult = FaceBoundingBoxExpander.expandedBoundingBox(
            centeredFace, imageWidth, imageHeight,
            FaceClassifierProcessor.Classifier.DETECT_FACE_SHAPE
        )

        for (classifier in shoulderClassifiers) {
            val result = FaceBoundingBoxExpander.expandedBoundingBox(
                centeredFace, imageWidth, imageHeight, classifier
            )
            assertTrue(
                "Classifier $classifier should produce wider crop",
                result.width() >= nonShoulderResult.width()
            )
        }
    }

    @Test
    fun `expandedBoundingBox result always has positive dimensions`() {
        val smallFace = Rect(500, 500, 520, 520)
        val classifiers = FaceClassifierProcessor.Classifier.values()

        for (classifier in classifiers) {
            val result = FaceBoundingBoxExpander.expandedBoundingBox(
                smallFace, imageWidth, imageHeight, classifier
            )
            assertTrue("Width should be positive for $classifier", result.width() > 0)
            assertTrue("Height should be positive for $classifier", result.height() > 0)
        }
    }
}
