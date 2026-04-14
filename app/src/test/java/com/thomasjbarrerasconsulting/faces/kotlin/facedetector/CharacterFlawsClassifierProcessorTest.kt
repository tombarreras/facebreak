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
class CharacterFlawsClassifierProcessorTest {

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
            Category("Lazy", 0.5f),
            Category("Grumpy", 0.3f),
            Category("Vain", 0.03f),
            Category("Bossy", 0.01f)
        )

        val result = CharacterFlawsClassifierProcessor.extractCharacterFlawsClassification(outputs, genderClassifier)

        assertEquals(2, result.size)
    }

    @Test
    fun `includes all categories above threshold`() {
        val outputs = listOf(
            Category("Lazy", 0.25f),
            Category("Grumpy", 0.25f),
            Category("Vain", 0.25f),
            Category("Bossy", 0.25f)
        )

        val result = CharacterFlawsClassifierProcessor.extractCharacterFlawsClassification(outputs, genderClassifier)

        assertEquals(4, result.size)
    }

    @Test
    fun `results contain percentages`() {
        val outputs = listOf(
            Category("Lazy", 0.6f),
            Category("Grumpy", 0.4f)
        )

        val result = CharacterFlawsClassifierProcessor.extractCharacterFlawsClassification(outputs, genderClassifier)

        for (classification in result) {
            assertTrue("Should contain percentage", classification.contains("("))
        }
    }
}
