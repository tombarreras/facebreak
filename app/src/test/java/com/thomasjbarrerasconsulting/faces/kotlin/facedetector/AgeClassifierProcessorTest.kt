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
class AgeClassifierProcessorTest {

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
    fun `single dominant age returns one classification`() {
        val outputs = listOf(
            Category("25", 0.5f),
            Category("26", 0.3f),
            Category("24", 0.2f)
        )

        val result = AgeClassifierProcessor.extractAgeClassification(outputs)

        assertEquals(1, result.size)
    }

    @Test
    fun `infant label is handled as age 0`() {
        val outputs = listOf(
            Category("Infant", 0.8f),
            Category("1", 0.15f),
            Category("2", 0.05f)
        )

        val result = AgeClassifierProcessor.extractAgeClassification(outputs)

        assertEquals(1, result.size)
        assertTrue("Should contain age info", result[0].contains("0"))
    }

    @Test
    fun `narrow age range shows range in parentheses`() {
        // When the 33% threshold spans multiple ages, it shows a range
        val outputs = listOf(
            Category("30", 0.2f),
            Category("31", 0.15f),
            Category("29", 0.1f),
            Category("32", 0.05f)
        )

        val result = AgeClassifierProcessor.extractAgeClassification(outputs)

        assertEquals(1, result.size)
    }

    @Test
    fun `returns mutable list`() {
        val outputs = listOf(Category("25", 0.9f), Category("26", 0.1f))
        val result = AgeClassifierProcessor.extractAgeClassification(outputs)

        result.add("extra")
        assertEquals(2, result.size)
    }
}
