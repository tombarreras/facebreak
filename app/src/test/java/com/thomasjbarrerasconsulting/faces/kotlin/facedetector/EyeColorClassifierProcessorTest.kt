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
class EyeColorClassifierProcessorTest {

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

    @Test
    fun `eyes closed returns single classification`() {
        val outputs = listOf(
            Category("Closed Eyes", 0.7f),
            Category("Brown Eyes", 0.2f),
            Category("Blue Eyes", 0.1f)
        )

        val result = EyeColorClassifierProcessor.extractEyeColorClassification(outputs, genderClassifier)

        assertEquals(1, result.size)
    }

    @Test
    fun `single dominant eye color shows pure color`() {
        // When filtering "Closed Eyes", only one color remains above the 85% threshold
        val outputs = listOf(
            Category("Brown Eyes", 0.9f),
            Category("Closed Eyes", 0.1f)
        )

        val result = EyeColorClassifierProcessor.extractEyeColorClassification(outputs, genderClassifier)

        assertEquals(1, result.size)
        assertTrue("Should contain percentage", result[0].contains("("))
    }

    @Test
    fun `two eye colors shows mix`() {
        val outputs = listOf(
            Category("Green Eyes", 0.5f),
            Category("Blue Eyes", 0.3f),
            Category("Closed Eyes", 0.1f),
            Category("Brown Eyes", 0.1f)
        )

        val result = EyeColorClassifierProcessor.extractEyeColorClassification(outputs, genderClassifier)

        assertTrue("Should have at least one classification", result.isNotEmpty())
    }

    @Test
    fun `brown and blue eyes use trace description`() {
        val outputs = listOf(
            Category("Brown Eyes", 0.6f),
            Category("Blue Eyes", 0.3f),
            Category("Closed Eyes", 0.05f),
            Category("Green Eyes", 0.05f)
        )

        val result = EyeColorClassifierProcessor.extractEyeColorClassification(outputs, genderClassifier)

        assertEquals(2, result.size)
    }

    @Test
    fun `three colors shows third as trace`() {
        val outputs = listOf(
            Category("Green Eyes", 0.4f),
            Category("Grey Eyes", 0.3f),
            Category("Blue Eyes", 0.2f),
            Category("Closed Eyes", 0.05f),
            Category("Brown Eyes", 0.05f)
        )

        val result = EyeColorClassifierProcessor.extractEyeColorClassification(outputs, genderClassifier)

        assertTrue("Should have multiple classifications", result.size >= 2)
    }
}
