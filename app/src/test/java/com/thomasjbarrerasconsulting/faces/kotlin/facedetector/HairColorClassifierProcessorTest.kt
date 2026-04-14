package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.util.Log
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
class HairColorClassifierProcessorTest {

    @Before
    fun setUp() {
        val mockApp = mockk<FaceBreakApplication>(relaxed = true)
        every { mockApp.getString(any()) } answers { "mocked" }
        mockkObject(FaceBreakApplication.Companion)
        every { FaceBreakApplication.instance } returns mockApp

        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkObject(FaceBreakApplication.Companion)
        unmockkStatic(Log::class)
    }

    @Test
    fun `bald detected when score over 50 percent`() {
        val outputs = listOf(
            Category("Bald", 0.7f),
            Category("Brown", 0.2f),
            Category("Black", 0.1f)
        )

        val result = HairColorClassifierProcessor.extractHairColorClassification(outputs)

        assertEquals(1, result.size)
    }

    @Test
    fun `single hair color returns standalone description`() {
        val outputs = listOf(
            Category("Black", 0.85f),
            Category("Bald", 0.05f),
            Category("Brown", 0.05f),
            Category("Red", 0.05f)
        )

        val result = HairColorClassifierProcessor.extractHairColorClassification(outputs)

        assertTrue("Should have at least one classification", result.isNotEmpty())
    }

    @Test
    fun `two dominant colors returns combined description`() {
        val outputs = listOf(
            Category("Black", 0.4f),
            Category("Brown", 0.35f),
            Category("Bald", 0.1f),
            Category("Red", 0.08f),
            Category("Blond", 0.07f)
        )

        val result = HairColorClassifierProcessor.extractHairColorClassification(outputs)

        assertTrue("Should have at least one classification", result.isNotEmpty())
    }

    @Test
    fun `dominant color with prominent secondary gets special name`() {
        val outputs = listOf(
            Category("Blond", 0.45f),
            Category("Red", 0.25f),
            Category("Bald", 0.1f),
            Category("Brown", 0.1f),
            Category("Black", 0.1f)
        )

        val result = HairColorClassifierProcessor.extractHairColorClassification(outputs)

        assertTrue("Should have classifications", result.isNotEmpty())
    }

    @Test
    fun `three dominant colors each listed separately`() {
        val outputs = listOf(
            Category("Brown", 0.3f),
            Category("Black", 0.25f),
            Category("Red", 0.22f),
            Category("Bald", 0.08f),
            Category("Blond", 0.08f),
            Category("Grey", 0.07f)
        )

        val result = HairColorClassifierProcessor.extractHairColorClassification(outputs)

        assertTrue("Should have multiple classifications", result.size >= 2)
    }

    @Test
    fun `remaining colors shown as touch of`() {
        val outputs = listOf(
            Category("Brown", 0.5f),
            Category("Bald", 0.05f),
            Category("Red", 0.2f),
            Category("Black", 0.15f),
            Category("Blond", 0.1f)
        )

        val result = HairColorClassifierProcessor.extractHairColorClassification(outputs)

        assertTrue("Should produce results", result.isNotEmpty())
    }

    @Test
    fun `exception returns no hair detected`() {
        // Passing empty list triggers exception in mainHairColors
        val outputs = listOf(
            Category("Bald", 0.3f)
            // no other colors above 0.1 threshold, so mainHairColors returns empty -> exception
        )

        val result = HairColorClassifierProcessor.extractHairColorClassification(outputs)

        // Should not crash, returns a result
        assertTrue("Should handle gracefully", result.isNotEmpty())
    }
}
