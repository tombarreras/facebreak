/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.FaceBreakApplication
import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class PhysicalFeatureClassifierProcessor {
    companion object{
        fun extractPhysicalFeatureClassifications(
            modelInput: ModelInput,
            context: Context,
            classificationTracker: ClassificationTracker
        ): List<String> {
            val classifications = mutableListOf<String>()
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()

            val headwearModel = TfliteModels.get(context, TfliteModels.HEADWEAR)
            val headwearOutputs = classificationTracker.merge(headwearModel.classify(modelInput))
            val significantHeadwearOutputs = headwearOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter{ it.score >= 0.2 }

            val eyebrowsModel = TfliteModels.get(context, TfliteModels.EYEBROWS)
            val eyebrowsOutputs = classificationTracker.merge(eyebrowsModel.classify(modelInput))
            val significantEyebrowsOutputs = eyebrowsOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter { it.label != "Medium Eyebrows" }.filter{ it.label != "No Eyebrows" }.filter { it.score >= 0.35}.take(1)

            val jawModel = TfliteModels.get(context, TfliteModels.JAW)
            val jawOutputs = classificationTracker.merge(jawModel.classify(modelInput))
            val significantJawOutputs = jawOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter{ it.score >= 0.08}

            val faceFeaturesFaceModel = TfliteModels.get(context, TfliteModels.FACE_FEATURES)
            val faceFeaturesOutputs = classificationTracker.merge(faceFeaturesFaceModel.classify(modelInput))
            val significantFaceFeaturesOutputs = faceFeaturesOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter{ it.score >= 0.25}

            val categories = mutableListOf<Category>()
            categories.addAll(significantHeadwearOutputs)
            categories.addAll(significantEyebrowsOutputs)
            categories.addAll(significantJawOutputs)
            categories.addAll(significantFaceFeaturesOutputs)

            for (output in categories.apply { sortByDescending { it.score } }){
                val label = ClassifierText.get(output.label)
                classifications.add("$label (${percentFormat.format(output.score)})")
            }

            if (classifications.count() == 0){
                classifications.add(FaceBreakApplication.instance.getString(R.string.physicalfeature_no_physical_features_detected))
            }
            return classifications.toList()
        }
    }
}