/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/

package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.FaceBreakApplication
import org.tensorflow.lite.support.label.Category

class AgeClassifierProcessor {
    companion object{
        fun extractAgeClassification(outputs: List<Category?>): MutableList<String> {
            val context = FaceBreakApplication.instance
            val classifications: MutableList<String> = mutableListOf()

            val likelyAgeString = outputs.first()?.label
            val likelyAge = if (likelyAgeString == "Infant") 0 else likelyAgeString?.toInt()

            val (minAge33, maxAge33) = ageRange(outputs, 0.33f)

            if (minAge33 == maxAge33){
                classifications.add(context.getString(R.string.apparent_age) + likelyAge)
            } else {
                classifications.add(context.getString(R.string.apparent_age) + " " + likelyAge + " " + context.getString(R.string.years) +
                        " (" + minAge33 + " " + context.getString(R.string.to) + " " + maxAge33 + ")")
            }

            return classifications
        }

        private fun ageRange(outputs: List<Category?>, threshold: Float): Pair<Int, Int> {
            var minAge = 100
            var maxAge = 0

            var totalProbability = 0.0f
            for (output in outputs) {
                var age = 0
                val probability = output?.score ?: 0.0f
                totalProbability += probability
                age = if (output?.label == "Infant") {
                    0
                } else {
                    output?.label?.toInt() ?: 0
                }

                if (minAge > age) {
                    minAge = age
                }
                if (maxAge < age) {
                    maxAge = age
                }
                if (totalProbability > threshold) {
                    return Pair(minAge, maxAge)
                }
            }
            return Pair(minAge, maxAge)
        }
    }
}