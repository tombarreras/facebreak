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
class AncestryClassifierProcessorTest {

    @Before
    fun setUp() {
        val mockApp = mockk<FaceBreakApplication>(relaxed = true)
        every { mockApp.getString(any()) } answers { "mocked_string" }
        mockkObject(FaceBreakApplication.Companion)
        every { FaceBreakApplication.instance } returns mockApp
    }

    @After
    fun tearDown() {
        unmockkObject(FaceBreakApplication.Companion)
    }

    @Test
    fun `filters out categories below 6_25 percent threshold`() {
        val outputs = listOf(
            Category("England", 0.8f),
            Category("Ireland, Scotland and Wales", 0.15f),
            Category("Scandinavia", 0.04f), // below 6.25%
            Category("China", 0.01f)         // below 6.25%
        )

        val result = AncestryClassifierProcessor.extractAncestryClassifications(outputs)

        assertEquals(2, result.size)
    }

    @Test
    fun `includes all categories above threshold`() {
        val outputs = listOf(
            Category("England", 0.4f),
            Category("Ireland, Scotland and Wales", 0.3f),
            Category("Scandinavia", 0.2f),
            Category("China", 0.1f)
        )

        val result = AncestryClassifierProcessor.extractAncestryClassifications(outputs)

        assertEquals(4, result.size)
    }

    @Test
    fun `single dominant ancestry returns one result`() {
        val outputs = listOf(
            Category("England", 0.9f),
            Category("China", 0.05f),
            Category("Scandinavia", 0.05f)
        )

        val result = AncestryClassifierProcessor.extractAncestryClassifications(outputs)

        assertEquals(1, result.size)
    }

    @Test
    fun `result contains percentage`() {
        val outputs = listOf(
            Category("England", 0.5f),
            Category("Scandinavia", 0.5f)
        )

        val result = AncestryClassifierProcessor.extractAncestryClassifications(outputs)

        for (classification in result) {
            assertTrue("Should contain parenthesized percentage: $classification",
                classification.contains("(") && classification.contains(")"))
        }
    }
}
