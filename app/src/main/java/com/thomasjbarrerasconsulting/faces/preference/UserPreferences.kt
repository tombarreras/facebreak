/*
 * Copyright 2021 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.preference

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager
import com.thomasjbarrerasconsulting.faces.R
import java.lang.Exception

class UserPreferences {
    var faceBoxWidth: Float = FACE_BOX_STROKE_DEFAULT_WIDTH
    var faceBoxColor: Int = FACE_BOX_DEFAULT_COLOR
    var classifierTextColor: Int = CLASSIFIER_TEXT_DEFAULT_COLOR
    var classifierTextSize: Float = SIZE_MEDIUM
    var averagingSeconds: Float = PREDICTION_AVERAGING_DEFAULT_SECONDS
    var enableAnalytics = false

    companion object {
        private const val SIZE_VERY_LARGE = 1.5f
        private const val SIZE_LARGE = 1.25f
        private const val SIZE_MEDIUM = 1.0f
        private const val SIZE_SMALL = 0.75f
        private const val SIZE_VERY_SMALL = 0.5f
        private const val FACE_BOX_STROKE_DEFAULT_WIDTH = 5.0f
        private const val FACE_BOX_DEFAULT_COLOR = Color.GREEN
        private const val CLASSIFIER_TEXT_DEFAULT_COLOR = Color.WHITE
        private const val PREDICTION_AVERAGING_DEFAULT_SECONDS = 5.0f
        private const val ENABLE_ANALYTICS_DEFAULT = false
        const val PREFERENCE_KEY_ENABLE_ANALYTICS = "pref_key_google_analytics"

        private fun readBoolean(prefKey: String, default:Boolean, context:Context):Boolean{
            try {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val prefValueString = sharedPreferences.getString(prefKey, default.toString())
                if (prefValueString != null) {
                    return prefValueString.toBoolean()
                }
            }
            catch (e: Exception) {
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

            preferences.faceBoxWidth = readFloat("pref_key_face_box_line_width", FACE_BOX_STROKE_DEFAULT_WIDTH, context)
            preferences.faceBoxColor = readColor("pref_key_face_box_line_color", FACE_BOX_DEFAULT_COLOR, context)
            preferences.classifierTextColor = readColor("pref_key_classifier_text_color", CLASSIFIER_TEXT_DEFAULT_COLOR, context)
            preferences.classifierTextSize = readSize("pref_key_classifier_text_size", SIZE_MEDIUM, context)
            preferences.averagingSeconds = readFloat("pref_key_live_preview_prediction_averaging_seconds", 5.0f, context)
            preferences.enableAnalytics = readBoolean(PREFERENCE_KEY_ENABLE_ANALYTICS, ENABLE_ANALYTICS_DEFAULT, context)

            return preferences
        }

    }
}