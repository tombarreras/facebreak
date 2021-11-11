package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.HairStyleClassifierProcessor.Companion.getHairStyleDescription
import com.thomasjbarrerasconsulting.faces.ml.*
import org.tensorflow.lite.support.image.TensorImage
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

            val headwearModel = HeadwearModel2.newInstance(context)
            val headwearOutputs = classificationTracker.merge(headwearModel.process(tensorImage).probabilityAsCategoryList)
            val significantHeadwearOutputs = headwearOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter{ it.score >= 0.2 }
            headwearModel.close()

            val eyebrowsModel = EyebrowsModel.newInstance(context)
            val eyebrowsOutputs = classificationTracker.merge(eyebrowsModel.process(tensorImage).probabilityAsCategoryList)
            val significantEyebrowsOutputs = eyebrowsOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter{ it.score >= 0.25}
            eyebrowsModel.close()

            val jawModel = JawModel4.newInstance(context)
            val jawOutputs = classificationTracker.merge(jawModel.process(tensorImage).probabilityAsCategoryList)
            val significantJawOutputs = jawOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter{ it.score >= 0.08}
            jawModel.close()

            val faceFeaturesFaceModel = FeaturesFaceModel5.newInstance(context)
            val faceFeaturesOutputs = classificationTracker.merge(faceFeaturesFaceModel.process(tensorImage).probabilityAsCategoryList)
            val significantFaceFeaturesOutputs = faceFeaturesOutputs.apply{sortByDescending { it.score }}.filter { it.label != "Clear" }.filter{ it.score >= 0.25}
            faceFeaturesFaceModel.close()

            for (output in significantHeadwearOutputs){
                val label = output.label
                classifications.add("$label (${percentFormat.format(output.score)})")
            }

            for (output in significantEyebrowsOutputs){
                val label = output.label
                classifications.add("$label (${percentFormat.format(output.score)})")
            }

            for (output in significantJawOutputs){
                val label = output.label
                classifications.add("$label (${percentFormat.format(output.score)})")
            }

            for (output in significantFaceFeaturesOutputs){
                val label = output.label
                classifications.add("$label (${percentFormat.format(output.score)})")
            }

            if (classifications.count() == 0){
                classifications.add("No physical features detected")
            }
            return classifications.toList()
        }
    }
}