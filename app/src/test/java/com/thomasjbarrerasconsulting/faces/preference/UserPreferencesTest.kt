package com.thomasjbarrerasconsulting.faces.preference

import android.graphics.Color
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import androidx.preference.PreferenceManager

@RunWith(RobolectricTestRunner::class)
class UserPreferencesTest {

    @Test
    fun `constructor has sensible defaults`() {
        val prefs = UserPreferences()

        assertEquals(5.0f, prefs.faceBoxWidth, 0.001f)
        assertEquals(Color.GREEN, prefs.faceBoxColor)
        assertEquals(Color.WHITE, prefs.classifierTextColor)
        assertEquals(1.0f, prefs.classifierTextSize, 0.001f)
        assertEquals(5.0f, prefs.averagingSeconds, 0.001f)
        assertFalse(prefs.enableAnalytics)
        assertFalse(prefs.enablePersonalizedAds)
        assertTrue(prefs.enableInAppReviews)
        assertEquals(FaceDetectorOptions.PERFORMANCE_MODE_FAST, prefs.performanceMode)
    }

    @Test
    fun `setUserPreferenceBoolean writes true`() {
        val context = RuntimeEnvironment.getApplication()

        UserPreferences.setUserPreferenceBoolean(
            context,
            UserPreferences.PREFERENCE_KEY_ENABLE_ANALYTICS,
            true
        )

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        assertTrue(sharedPrefs.getBoolean(UserPreferences.PREFERENCE_KEY_ENABLE_ANALYTICS, false))
    }

    @Test
    fun `setUserPreferenceBoolean writes false`() {
        val context = RuntimeEnvironment.getApplication()

        UserPreferences.setUserPreferenceBoolean(
            context,
            UserPreferences.PREFERENCE_KEY_ENABLE_ANALYTICS,
            false
        )

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        assertFalse(sharedPrefs.getBoolean(UserPreferences.PREFERENCE_KEY_ENABLE_ANALYTICS, true))
    }

    @Test
    fun `setUserPreferenceBoolean can toggle value`() {
        val context = RuntimeEnvironment.getApplication()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        UserPreferences.setUserPreferenceBoolean(
            context,
            UserPreferences.PREFERENCE_KEY_ENABLE_PERSONALIZED_ADS,
            true
        )
        assertTrue(sharedPrefs.getBoolean(UserPreferences.PREFERENCE_KEY_ENABLE_PERSONALIZED_ADS, false))

        UserPreferences.setUserPreferenceBoolean(
            context,
            UserPreferences.PREFERENCE_KEY_ENABLE_PERSONALIZED_ADS,
            false
        )
        assertFalse(sharedPrefs.getBoolean(UserPreferences.PREFERENCE_KEY_ENABLE_PERSONALIZED_ADS, true))
    }

    @Test
    fun `properties are mutable`() {
        val prefs = UserPreferences()

        prefs.faceBoxWidth = 10.0f
        prefs.faceBoxColor = Color.RED
        prefs.enableAnalytics = true

        assertEquals(10.0f, prefs.faceBoxWidth, 0.001f)
        assertEquals(Color.RED, prefs.faceBoxColor)
        assertTrue(prefs.enableAnalytics)
    }
}
