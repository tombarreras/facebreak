package com.thomasjbarrerasconsulting.faces.preference

import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager

class DisplayPreferences {
    var faceBoxWidth: Float = FACE_BOX_STROKE_DEFAULT_WIDTH
    var faceBoxColor: Int = FACE_BOX_DEFAULT_COLOR
    var classifierTextColor: Int = CLASSIFIER_TEXT_DEFAULT_COLOR
    var classifierTextSize: Float = SIZE_MEDIUM
    var averagingSeconds: Float = PREDICTION_AVERAGING_DEFAULT_SECONDS

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

        private fun readFloat(prefKey: String, default:Float, context:Context):Float{
            // TODO: Take care of this
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val prefValueString = sharedPreferences.getString(prefKey, default.toString())
            if (prefValueString != null) {
                return prefValueString.toFloat()
            }
            return default
        }

        private fun readSize(prefKey: String, default:Float, context:Context):Float{
            // TODO: Take care of this
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val prefValueString = sharedPreferences.getString(prefKey, default.toString())
            if (prefValueString != null) {
                when (prefValueString) {
                    "Very Large" -> return SIZE_VERY_LARGE
                    "Large" -> return SIZE_LARGE
                    "Medium" -> return SIZE_MEDIUM
                    "Small" -> return SIZE_SMALL
                    "Very Small" -> return SIZE_VERY_SMALL
                }
            }
            return default
        }

        private fun readColor(prefKey: String, default:Int, context:Context): Int {
            // TODO: Take care of this
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val prefValueString = sharedPreferences.getString(prefKey, default.toString())
            if (prefValueString != null) {
                when (prefValueString) {
                    "Red" -> return Color.RED
                    "Green" -> return Color.GREEN
                    "Blue" -> return Color.BLUE
                    "Yellow" -> return Color.YELLOW
                    "Black" -> return Color.BLACK
                    "White" -> return Color.WHITE
                    "Dark Gray" -> return Color.DKGRAY
                    "Light Gray" -> return Color.LTGRAY
                    "Cyan" -> return Color.CYAN
                    "Magenta" -> return Color.MAGENTA
                    "Transparent" -> return Color.TRANSPARENT
                }
            }
            return default
        }

        fun getDisplayPreferences(context: Context):DisplayPreferences{
            val preferences = DisplayPreferences()

            preferences.faceBoxWidth = readFloat("pref_key_face_box_line_width", FACE_BOX_STROKE_DEFAULT_WIDTH, context)
            preferences.faceBoxColor = readColor("pref_key_face_box_line_color", FACE_BOX_DEFAULT_COLOR, context)
            preferences.classifierTextColor = readColor("pref_key_classifier_text_color", CLASSIFIER_TEXT_DEFAULT_COLOR, context)
            preferences.classifierTextSize = readSize("pref_key_classifier_text_size", SIZE_MEDIUM, context)
            preferences.averagingSeconds = readFloat("pref_key_live_preview_prediction_averaging_seconds", 5.0f, context)

            return preferences
        }

    }
}