package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.tensorflow.lite.support.label.Category

@RunWith(RobolectricTestRunner::class)
class ClassificationTrackerTest {

    @Test
    fun `merge with single category returns it unchanged`() {
        val tracker = ClassificationTracker(5.0f, FaceClassifierProcessor.Classifier.DETECT_AGE)
        val categories = listOf(Category("young", 0.8f))

        val result = tracker.merge(categories)

        assertEquals(1, result.size)
        assertEquals("young", result[0].label)
        assertEquals(0.8f, result[0].score, 0.001f)
    }

    @Test
    fun `merge accumulates and averages scores over multiple calls`() {
        val tracker = ClassificationTracker(5.0f, FaceClassifierProcessor.Classifier.DETECT_AGE)

        tracker.merge(listOf(Category("young", 0.6f)))
        val result = tracker.merge(listOf(Category("young", 0.8f)))

        // Average of 0.6 and 0.8 = 0.7
        assertEquals(1, result.size)
        assertEquals("young", result[0].label)
        assertEquals(0.7f, result[0].score, 0.001f)
    }

    @Test
    fun `merge tracks multiple categories independently`() {
        val tracker = ClassificationTracker(5.0f, FaceClassifierProcessor.Classifier.DETECT_AGE)

        val result = tracker.merge(listOf(
            Category("young", 0.6f),
            Category("old", 0.4f)
        ))

        assertEquals(2, result.size)
        val young = result.find { it.label == "young" }
        val old = result.find { it.label == "old" }

        assertNotNull(young)
        assertNotNull(old)
        assertEquals(0.6f, young!!.score, 0.001f)
        assertEquals(0.4f, old!!.score, 0.001f)
    }

    @Test
    fun `merge removes expired entries`() {
        // Use 0 second timeout so everything expires immediately
        val tracker = ClassificationTracker(0.0f, FaceClassifierProcessor.Classifier.DETECT_AGE)

        tracker.merge(listOf(Category("young", 0.2f)))
        // Wait a tiny bit to ensure expiry
        Thread.sleep(50)
        val result = tracker.merge(listOf(Category("young", 0.9f)))

        // Only the latest value should remain (old one expired)
        assertEquals(1, result.size)
        assertEquals(0.9f, result[0].score, 0.001f)
    }

    @Test
    fun `merge with large timeout retains all entries`() {
        val tracker = ClassificationTracker(60.0f, FaceClassifierProcessor.Classifier.DETECT_AGE)

        tracker.merge(listOf(Category("young", 0.4f)))
        tracker.merge(listOf(Category("young", 0.6f)))
        val result = tracker.merge(listOf(Category("young", 0.8f)))

        // Average of 0.4, 0.6, 0.8 = 0.6
        assertEquals(0.6f, result[0].score, 0.001f)
    }

    @Test
    fun `merge returns mutable list`() {
        val tracker = ClassificationTracker(5.0f, FaceClassifierProcessor.Classifier.DETECT_AGE)
        val result = tracker.merge(listOf(Category("test", 0.5f)))

        // Should be able to modify the returned list
        result.add(Category("extra", 0.1f))
        assertEquals(2, result.size)
    }
}
