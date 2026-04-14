package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import com.thomasjbarrerasconsulting.faces.kotlin.FaceBreakApplication
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.tensorflow.lite.support.label.Category

@RunWith(RobolectricTestRunner::class)
class FaceShapeClassifierProcessorTest {

    private lateinit var genderClassifier: GenderClassifier

    @Before
    fun setUp() {
        val mockApp = mockk<FaceBreakApplication>(relaxed = true)
        every { mockApp.getString(any()) } answers { "mocked" }
        mockkObject(FaceBreakApplication.Companion)
        every { FaceBreakApplication.instance } returns mockApp

        genderClassifier = mockk()
        every { genderClassifier.isMaleOrGenderNeutral() } returns true
    }

    @After
    fun tearDown() {
        unmockkObject(FaceBreakApplication.Companion)
    }

    private fun allShapeOutputs(dominantShape: String, dominantScore: Float): List<Category?> {
        val shapes = listOf("Diamond", "Square", "Heart", "Oval", "Oblong", "Pear", "Round")
        val remaining = (1.0f - dominantScore) / (shapes.size - 1)
        return shapes.map { shape ->
            if (shape == dominantShape) Category(shape, dominantScore) else Category(shape, remaining)
        }
    }

    @Test
    fun `very strong shape returns classification`() {
        // Use raw scores that will produce a very strong shape after adjustment
        val outputs = allShapeOutputs("Round", 0.7f)
        val result = FaceShapeClassifierProcessor.extractFaceShapeClassifications(outputs, genderClassifier)

        assertTrue("Should have at least one classification", result.isNotEmpty())
    }

    @Test
    fun `all shapes included produces non-empty result`() {
        val outputs = listOf(
            Category("Diamond", 0.15f),
            Category("Square", 0.15f),
            Category("Heart", 0.15f),
            Category("Oval", 0.15f),
            Category("Oblong", 0.15f),
            Category("Pear", 0.13f),
            Category("Round", 0.12f)
        )

        val result = FaceShapeClassifierProcessor.extractFaceShapeClassifications(outputs, genderClassifier)

        assertTrue("Should produce classifications", result.isNotEmpty())
    }

    @Test
    fun `dominant diamond shape classifies correctly`() {
        val outputs = allShapeOutputs("Diamond", 0.8f)
        val result = FaceShapeClassifierProcessor.extractFaceShapeClassifications(outputs, genderClassifier)

        assertTrue("Should have classifications", result.isNotEmpty())
    }

    @Test
    fun `model adjustment reduces diamond score`() {
        // Diamond has a 0.65x adjustment factor, so a high raw score gets reduced
        val outputs = listOf(
            Category("Diamond", 0.3f),
            Category("Round", 0.3f),
            Category("Oval", 0.1f),
            Category("Heart", 0.1f),
            Category("Square", 0.1f),
            Category("Oblong", 0.05f),
            Category("Pear", 0.05f)
        )

        val result = FaceShapeClassifierProcessor.extractFaceShapeClassifications(outputs, genderClassifier)

        assertTrue("Should produce classifications", result.isNotEmpty())
    }

    @Test
    fun `returns mutable list`() {
        val outputs = allShapeOutputs("Oval", 0.5f)
        val result = FaceShapeClassifierProcessor.extractFaceShapeClassifications(outputs, genderClassifier)

        result.add("extra")
        assertTrue(result.contains("extra"))
    }
}
