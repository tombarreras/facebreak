package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.thomasjbarrerasconsulting.faces.ml.GenderModel2
import kotlinx.coroutines.sync.Mutex
import org.tensorflow.lite.support.image.TensorImage

class GenderClassifier (val context: Context, private val tensorImage: TensorImage) {

    private var isMaleOrGenderNeutral: Boolean? = null
    fun isMaleOrGenderNeutral(): Boolean {

        if (isMaleOrGenderNeutral == null){
            synchronized(tensorImage){
                val currentLocale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
                isMaleOrGenderNeutral = if (currentLocale?.language == "es") {
                    val genderModel = TfliteModels.get(context, GenderModel2::class.java, GenderModel2::newInstance)
                    val isMale = genderModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.first().label == "Male"
                    isMale
                } else {
                    true
                }
            }
        }

        return isMaleOrGenderNeutral!!
    }
}