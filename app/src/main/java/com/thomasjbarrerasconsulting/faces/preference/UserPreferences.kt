/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.preference

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.thomasjbarrerasconsulting.faces.R
import java.lang.Exception

class UserPreferences {
    var faceBoxWidth: Float = FACE_BOX_STROKE_DEFAULT_WIDTH
    var faceBoxColor: Int = FACE_BOX_DEFAULT_COLOR
    var classifierTextColor: Int = DEFAULT_CLASSIFIER_TEXT_COLOR
    var classifierTextSize: Float = SIZE_MEDIUM
    var averagingSeconds: Float = DEFAULT_PREDICTION_AVERAGING_SECONDS
    var enableAnalytics = DEFAULT_ENABLE_ANALYTICS
    var enablePersonalizedAds = DEFAULT_ENABLE_PERSONALIZED_ADS
    var enableInAppReviews = DEFAULT_ENABLE_IN_APP_REVIEW
    var performanceMode = DEFAULT_PERFORMANCE_MODE

    companion object {
        private const val TAG = "UserPreferences"
        private const val SIZE_VERY_LARGE = 1.5f
        private const val SIZE_LARGE = 1.25f
        private const val SIZE_MEDIUM = 1.0f
        private const val SIZE_SMALL = 0.75f
        private const val SIZE_VERY_SMALL = 0.5f
        private const val FACE_BOX_STROKE_DEFAULT_WIDTH = 5.0f
        private const val FACE_BOX_DEFAULT_COLOR = Color.GREEN
        private const val DEFAULT_CLASSIFIER_TEXT_COLOR = Color.WHITE
        private const val DEFAULT_PREDICTION_AVERAGING_SECONDS = 5.0f
        private const val DEFAULT_ENABLE_ANALYTICS = false
        private const val DEFAULT_ENABLE_PERSONALIZED_ADS = false
        private const val DEFAULT_ENABLE_IN_APP_REVIEW = true
        private const val DEFAULT_PERFORMANCE_MODE = FaceDetectorOptions.PERFORMANCE_MODE_FAST
        const val PREFERENCE_KEY_FACE_BOX_LINE_WIDTH = "pref_key_face_box_line_width"
        const val PREFERENCE_KEY_PREDICTION_AVERAGING_SECONDS = "pref_key_live_preview_prediction_averaging_seconds"
        const val PREFERENCE_KEY_ENABLE_ANALYTICS = "pref_key_google_analytics"
        const val PREFERENCE_KEY_ENABLE_PERSONALIZED_ADS = "pref_key_personalized_ads"
        private const val PREFERENCE_KEY_ENABLE_IN_APP_REVIEWS = "pref_key_in_app_reviews"
        const val PREFERENCE_KEY_GDPR = "pref_key_gdpr"
        private const val PREFERENCE_KEY_PERFORMANCE_MODE = "lpfdpm"

        private fun readBoolean(prefKey: String, default:Boolean, context:Context):Boolean {
            try {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                return sharedPreferences.getBoolean(prefKey, default)
            }
            catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
            return default
        }

        private fun readFloat(prefKey: String, default:Float, context:Context):Float{
            try {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val prefValueString = sharedPreferences.getString(prefKey, default.toString())
                if (prefValueString != null) {
                    return prefValueString.toFloat()
                }
            }
            catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
            return default
        }

        private fun readInt(prefKey: String, default:Int, context:Context):Int{
            try {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val prefValueString = sharedPreferences.getString(prefKey, default.toString())
                if (prefValueString != null) {
                    return prefValueString.toInt()
                }
            }
            catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
            return default
        }

        private fun readSize(prefKey: String, default:Float, context:Context):Float{
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val prefValueString = sharedPreferences.getString(prefKey, default.toString())
            if (prefValueString != null) {
                when (prefValueString) {
                    context.getString(R.string.pref_entry_values_size_very_large) -> return SIZE_VERY_LARGE
                    context.getString(R.string.pref_entry_values_size_large) -> return SIZE_LARGE
                    context.getString(R.string.pref_entry_values_size_medium) -> return SIZE_MEDIUM
                    context.getString(R.string.pref_entry_values_size_small) -> return SIZE_SMALL
                    context.getString(R.string.pref_entry_values_size_very_small) -> return SIZE_VERY_SMALL
                }
            }
            return default
        }

        private fun readColor(prefKey: String, default:Int, context:Context): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val prefValueString = sharedPreferences.getString(prefKey, default.toString())
            if (prefValueString != null) {
                when (prefValueString) {
                    context.getString(R.string.pref_entry_values_color_red) -> return Color.RED
                    context.getString(R.string.pref_entry_values_color_green) -> return Color.GREEN
                    context.getString(R.string.pref_entry_values_color_blue) -> return Color.BLUE
                    context.getString(R.string.pref_entry_values_color_yellow) -> return Color.YELLOW
                    context.getString(R.string.pref_entry_values_color_black) -> return Color.BLACK
                    context.getString(R.string.pref_entry_values_color_white) -> return Color.WHITE
                    context.getString(R.string.pref_entry_values_color_dark_gray) -> return Color.DKGRAY
                    context.getString(R.string.pref_entry_values_color_light_gray) -> return Color.LTGRAY
                    context.getString(R.string.pref_entry_values_color_cyan) -> return Color.CYAN
                    context.getString(R.string.pref_entry_values_color_magenta)-> return Color.MAGENTA
                    context.getString(R.string.pref_entry_values_color_transparent) -> return Color.TRANSPARENT
                }
            }
            return default
        }

        fun getUserPreferences(context: Context):UserPreferences{
            val preferences = UserPreferences()

            preferences.faceBoxWidth = readFloat(PREFERENCE_KEY_FACE_BOX_LINE_WIDTH, FACE_BOX_STROKE_DEFAULT_WIDTH, context)
            preferences.faceBoxColor = readColor("pref_key_face_box_line_color", FACE_BOX_DEFAULT_COLOR, context)
            preferences.classifierTextColor = readColor("pref_key_classifier_text_color", DEFAULT_CLASSIFIER_TEXT_COLOR, context)
            preferences.classifierTextSize = readSize("pref_key_classifier_text_size", SIZE_MEDIUM, context)
            preferences.averagingSeconds = readFloat(PREFERENCE_KEY_PREDICTION_AVERAGING_SECONDS, DEFAULT_PREDICTION_AVERAGING_SECONDS, context)
            preferences.performanceMode = readInt(PREFERENCE_KEY_PERFORMANCE_MODE, DEFAULT_PERFORMANCE_MODE, context)
            preferences.enableAnalytics = readBoolean(PREFERENCE_KEY_ENABLE_ANALYTICS, DEFAULT_ENABLE_ANALYTICS, context)
            preferences.enablePersonalizedAds = readBoolean(PREFERENCE_KEY_ENABLE_PERSONALIZED_ADS, DEFAULT_ENABLE_PERSONALIZED_ADS, context)
            preferences.enableInAppReviews = readBoolean(PREFERENCE_KEY_ENABLE_IN_APP_REVIEWS, DEFAULT_ENABLE_IN_APP_REVIEW, context)

            return preferences
        }

        fun setUserPreferenceBoolean(context: Context, prefKey: String, value:Boolean){
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sharedPreferences.edit()
            editor.putBoolean(prefKey, value)
            editor.commit()
        }
    }
}