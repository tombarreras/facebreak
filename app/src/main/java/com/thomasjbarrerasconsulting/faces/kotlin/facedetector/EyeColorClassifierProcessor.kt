/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
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
                classifications.add(ClassifierText.get("Closed Eyes"))
            } else {
                val eyesOpenProbability = outputs.filterNot { o -> o?.label == "Closed Eyes" }.map { it?.score!! }.sum()
                val allColorsProbability = getAllColorsProbability(outputs, eyesOpenProbability)
                if ((outputs.filterNot{ o -> o?.label == "Closed Eyes"}[0]?.score)?.div(eyesOpenProbability)!! > 0.85){
                    val eyeColor = outputs.filterNot{ o -> o?.label == "Closed Eyes"}[0]?.label
                    val colorProbability = percentFormat.format((outputs.filterNot{ o -> o?.label == "Closed Eyes"}[0]?.score)?.div(allColorsProbability)!!)
                    val pureEyeColor = ClassifierText.get("Pure $eyeColor")
                    classifications.add("$pureEyeColor ($colorProbability)")
                } else {
                    val eyeColorOutputs = outputs.filterNot{ o -> o?.label == "Closed Eyes"}
                    val eyeColorLabel1 = ClassifierText.get(eyeColorOutputs[0]?.label!!)
                    val eyeColorLabel2 = ClassifierText.get(eyeColorOutputs[1]?.label!!)
                    val eyeColorScore1 = eyeColorOutputs[0]?.score?.div(allColorsProbability)
                    val eyeColorScore2 = eyeColorOutputs[1]?.score?.div(allColorsProbability)
                    if ((eyeColorOutputs[0]?.label!! == "Brown Eyes" && eyeColorOutputs[1]?.label!! == "Blue Eyes") || (eyeColorOutputs[0]?.label!! == "Blue Eyes" && eyeColorOutputs[1]?.label!! == "Brown Eyes")){
                        classifications.add("$eyeColorLabel1 (${percentFormat.format(eyeColorScore1)})")
                        val traceColor = ClassifierText.get("Trace ${eyeColorOutputs[1]?.label!!}")
                        classifications.add("$traceColor (${percentFormat.format(eyeColorScore2)})")
                    } else {
                        classifications.add("$eyeColorLabel1-$eyeColorLabel2 (${percentFormat.format(eyeColorScore1)}/${percentFormat.format(eyeColorScore2)})")
                    }
                    if (eyeColorScore1?.plus(eyeColorScore2!!)!! < 0.85){
                        val eyeColorLabel3 = ClassifierText.get("Trace ${eyeColorOutputs[2]?.label!!}")
                        val eyeColorScore3 = eyeColorOutputs[2]?.score?.div(allColorsProbability)
                        classifications.add("$eyeColorLabel3 (${percentFormat.format(eyeColorScore3)})")

                        if (eyeColorScore1.plus(eyeColorScore2!!).plus(eyeColorScore3!!) < 0.85){
                            val eyeColorLabel4 = ClassifierText.get("Trace ${eyeColorOutputs[3]?.label!!}")
                            val eyeColorScore4 = eyeColorOutputs[3]?.score?.div(allColorsProbability)
                            classifications.add("$eyeColorLabel4 (${percentFormat.format(eyeColorScore4)})")
                        }
                    }
                }
            }
            return classifications
        }

        private fun getAllColorsProbability(outputs: List<Category?>, eyesOpenProbability: Float): Double {
            var probability = 0.0
            var colorCount = 0

            for (category in outputs){
                if (category?.label != "Closed Eyes"){
                    probability += category?.score!!
                    colorCount += 1

                    if (probability / eyesOpenProbability >= 0.85 || colorCount >= 4){
                        break
                    }
                }
            }
            return probability
        }
    }
}