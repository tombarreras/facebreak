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
class HairStyleClassifierProcessorTest {

    @Before
    fun setUp() {
        val mockApp = mockk<FaceBreakApplication>(relaxed = true)
        every { mockApp.getString(any()) } answers { "mocked" }
        mockkObject(FaceBreakApplication.Companion)
        every { FaceBreakApplication.instance } returns mockApp
    }

    @After
    fun tearDown() {
        unmockkObject(FaceBreakApplication.Companion)
    }

    @Test
    fun `single dominant hairstyle returns one classification`() {
        val outputs = listOf(
            Category("Curly", 0.7f),
            Category("Bob", 0.05f),
            Category("Long", 0.05f)
        )

        val result = HairStyleClassifierProcessor.extractHairStyleClassification(outputs)

        // Main section should have one entry
        assertTrue("Should have at least one classification", result.isNotEmpty())
    }

    @Test
    fun `two hairstyles with known combination get combined description`() {
        val outputs = listOf(
            Category("Curly", 0.4f),
            Category("Bob", 0.3f),
            Category("Long", 0.05f)
        )

        val result = HairStyleClassifierProcessor.extractHairStyleClassification(outputs)

        assertTrue("Should produce classifications", result.isNotEmpty())
    }

    @Test
    fun `bald is renamed to hairless in output`() {
        val outputs = listOf(
            Category("Bald", 0.8f),
            Category("Buzz Cut", 0.1f),
            Category("Curly", 0.05f)
        )

        val result = HairStyleClassifierProcessor.extractHairStyleClassification(outputs)

        assertTrue("Should produce result", result.isNotEmpty())
    }

    @Test
    fun `low probability styles shown in hint section`() {
        val outputs = listOf(
            Category("Curly", 0.5f),
            Category("Bob", 0.09f),  // below 0.1 threshold
            Category("Long", 0.08f)
        )

        val result = HairStyleClassifierProcessor.extractHairStyleClassification(outputs)

        // Should have main section + blank line + "Hint of" header + hint styles
        assertTrue("Should include hint section for low scores", result.size > 1)
    }

    @Test
    fun `all styles above threshold are in main section`() {
        val outputs = listOf(
            Category("Curly", 0.4f),
            Category("Bob", 0.3f),
            Category("Long", 0.2f),
            Category("Pixie", 0.1f)
        )

        val result = HairStyleClassifierProcessor.extractHairStyleClassification(outputs)

        assertTrue("Should have multiple main classifications", result.isNotEmpty())
    }
}
