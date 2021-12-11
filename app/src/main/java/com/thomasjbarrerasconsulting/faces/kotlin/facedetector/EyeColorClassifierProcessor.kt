/*
 * Copyright 2021 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class EyeColorClassifierProcessor {
    companion object{
        fun extractEyeColorClassification(outputs: List<Category?>): MutableList<String>{
            val classifications: MutableList<String> = mutableListOf()
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            if (outputs[0]?.label == "Closed Eyes" && outputs[0]?.score!! > 0.5){
                classifications.add("No eyes detected")
            } else {
                val eyesOpenProbability = 1.0 - outputs.find{ o -> o?.label == "Closed Eyes"}?.score!!
                if ((outputs.filterNot{ o -> o?.label == "Closed Eyes"}[0]?.score)?.div(eyesOpenProbability)!! > 0.85){
                    val eyeColor = outputs.filterNot{ o -> o?.label == "Closed Eyes"}[0]?.label?.replace(" Eyes", "")
                    val colorProbability = percentFormat.format((outputs.filterNot{ o -> o?.label == "Closed Eyes"}[0]?.score)?.div(eyesOpenProbability)!!)
                    classifications.add("Pure $eyeColor ($colorProbability)")
                } else {
                    val eyeColorOutputs = outputs.filterNot{ o -> o?.label == "Closed Eyes"}
                    val eyeColorLabel1 = eyeColorOutputs[0]?.label?.replace(" Eyes", "")
                    val eyeColorLabel2 = eyeColorOutputs[1]?.label?.replace(" Eyes", "")
                    val eyeColorScore1 = eyeColorOutputs[0]?.score?.div(eyesOpenProbability)
                    val eyeColorScore2 = eyeColorOutputs[1]?.score?.div(eyesOpenProbability)
                    if ((eyeColorLabel1 == "Brown" && eyeColorLabel2 == "Blue") || (eyeColorLabel1 == "Blue" && eyeColorLabel2 == "Brown")){
                        classifications.add("$eyeColorLabel1 (${percentFormat.format(eyeColorScore1)})")
                        classifications.add("Trace $eyeColorLabel2 (${percentFormat.format(eyeColorScore2)})")
                    } else {
                        classifications.add("$eyeColorLabel1-$eyeColorLabel2 (${percentFormat.format(eyeColorScore1)}/${percentFormat.format(eyeColorScore2)})")
                    }
                    if (eyeColorScore1?.plus(eyeColorScore2!!)!! < 0.85){
                        val eyeColorLabel3 = eyeColorOutputs[2]?.label?.replace(" Eyes", "")
                        val eyeColorScore3 = eyeColorOutputs[2]?.score?.div(eyesOpenProbability)
                        classifications.add("Trace $eyeColorLabel3 (${percentFormat.format(eyeColorScore3)})")

                        if (eyeColorScore1.plus(eyeColorScore2!!).plus(eyeColorScore3!!) < 0.85){
                            val eyeColorLabel4 = eyeColorOutputs[3]?.label?.replace(" Eyes", "")
                            val eyeColorScore4 = eyeColorOutputs[3]?.score?.div(eyesOpenProbability)
                            classifications.add("Trace $eyeColorLabel4 (${percentFormat.format(eyeColorScore4)})")
                        }
                    }
                }
            }
            return classifications
        }
    }
}