package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import androidx.core.os.ConfigurationCompat
import kotlinx.coroutines.sync.Mutex

class GenderClassifier (val context: Context, private val modelInput: ModelInput) {

    private var isMaleOrGenderNeutral: Boolean? = null
    fun isMaleOrGenderNeutral(): Boolean {

        if (isMaleOrGenderNeutral == null){
            synchronized(modelInput){
                val currentLocale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
                isMaleOrGenderNeutral = if (currentLocale?.language == "es") {
                    val genderModel = TfliteModels.get(context, TfliteModels.GENDER)
                    val isMale = genderModel.classify(modelInput).apply { sortByDescending { it.score } }.first().label == "Male"
                    isMale
                } else {
                    true
                }
            }
        }

        return isMaleOrGenderNeutral!!
    }
}