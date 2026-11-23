/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.FaceBreakApplication
import com.thomasjbarrerasconsulting.faces.ml.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class PhysicalFeatureClassifierProcessor {
    companion object{
        fun extractPhysicalFeatureClassifications(
            tensorImage: TensorImage,
            context: Context,
            classificationTracker: ClassificationTracker
        ): List<String> {
            val classifications = mutableListOf<String>()
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()

            val headwearModel = TfliteModels.get(context, HeadwearModel3::class.java, HeadwearModel3::newInstance)
            val headwearOutputs = classificationTracker.merge(headwearModel.process(tensorImage).probabilityAsCategoryList)
            val significantHeadwearOutputs = headwearOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter{ it.score >= 0.2 }

            val eyebrowsModel = TfliteModels.get(context, EyebrowsModel::class.java, EyebrowsModel::newInstance)
            val eyebrowsOutputs = classificationTracker.merge(eyebrowsModel.process(tensorImage).probabilityAsCategoryList)
            val significantEyebrowsOutputs = eyebrowsOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter { it.label != "Medium Eyebrows" }.filter{ it.label != "No Eyebrows" }.filter { it.score >= 0.35}.take(1)

            val jawModel = TfliteModels.get(context, JawModel5::class.java, JawModel5::newInstance)
            val jawOutputs = classificationTracker.merge(jawModel.process(tensorImage).probabilityAsCategoryList)
            val significantJawOutputs = jawOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter{ it.score >= 0.08}

            val faceFeaturesFaceModel = TfliteModels.get(context, FeaturesFaceModel5::class.java, FeaturesFaceModel5::newInstance)
            val faceFeaturesOutputs = classificationTracker.merge(faceFeaturesFaceModel.process(tensorImage).probabilityAsCategoryList)
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