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
class CharacterClassifierProcessorTest {

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
    fun `filters categories below 4 percent`() {
        val outputs = listOf(
            Category("Kind", 0.5f),
            Category("Strong", 0.3f),
            Category("Wise", 0.03f),     // below 4%
            Category("Creative", 0.02f)   // below 4%
        )

        val result = CharacterClassifierProcessor.extractCharacterClassification(outputs, genderClassifier)

        assertEquals(2, result.size)
    }

    @Test
    fun `includes all categories above threshold`() {
        val outputs = listOf(
            Category("Kind", 0.3f),
            Category("Strong", 0.3f),
            Category("Wise", 0.2f),
            Category("Creative", 0.2f)
        )

        val result = CharacterClassifierProcessor.extractCharacterClassification(outputs, genderClassifier)

        assertEquals(4, result.size)
    }

    @Test
    fun `results contain percentages`() {
        val outputs = listOf(
            Category("Kind", 0.6f),
            Category("Strong", 0.4f)
        )

        val result = CharacterClassifierProcessor.extractCharacterClassification(outputs, genderClassifier)

        for (classification in result) {
            assertTrue("Should contain percentage: $classification",
                classification.contains("(") && classification.contains(")"))
        }
    }
}
