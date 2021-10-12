package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat
import kotlin.math.ln
import kotlin.math.log

class AgeClassifierProcessor {
    companion object{
        fun extractAgeClassification(outputs: List<Category?>): MutableList<String> {
            val classifications: MutableList<String> = mutableListOf()
//            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
//            val likelyAgeConfidence = outputs.first()?.score
//            val nextLikelyAgeConfidence = outputs.take(2).last()?.score

            val likelyAgeString = outputs.first()?.label
            val likelyAge = if (likelyAgeString == "Infant") 0 else likelyAgeString?.toInt()

//            val (minAge66, maxAge66) = ageRange(outputs, 0.66f)
//            val (minAge50, maxAge50) = ageRange(outputs, 0.5f)
            val (minAge33, maxAge33) = ageRange(outputs, 0.33f)
//            val (minAge25, maxAge25) = ageRange(outputs, 0.25f)

//            val confidence = when(true){
//                ln(likelyAgeConfidence!!) - ln(nextLikelyAgeConfidence!!) > 0.4 -> "High"
//                maxAge25 - minAge25 < 3 -> "High"
//                (minAge25 > likelyAge!! * 0.9) && (maxAge25 < likelyAge * 1.1) -> "High"
//                (minAge25 > likelyAge * 0.8) && (maxAge25 < likelyAge * 1.2) -> "Medium"
//                else -> "Low"
//            }

            if (minAge33 == maxAge33){
                classifications.add("Apparent age: $likelyAge years old")
            } else {
                classifications.add("Apparent age: $likelyAge years old ($minAge33 to $maxAge33)")
//                classifications.add("Apparent age: $minAge33 to $maxAge33 years old")
            }

//            classifications.add("Confidence: $confidence")
//            classifications.add("${percentFormat.format(likelyAgeConfidence)} ${likelyAge}, ${percentFormat.format(nextLikelyAgeConfidence)} ${outputs.take(2).last()?.label}")
//            classifications.add("$minAge25 to $maxAge25 years old")
//            classifications.add("Probably: $minAge50 to $maxAge50 years old")
//            classifications.add("Very Probably: $minAge66 to $maxAge66 years old")

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


//    private fun getAgeClassifications(
//        ageModel: AgesModel10000,
//        tensorImage: TensorImage
//    ): MutableList<String> {
//        val categories = ageModel.process(tensorImage).probabilityAsCategoryList
//
//        val categoryMap = mutableMapOf("Infant" to Category("Infant", 0.0f))
//
//        for (category in categories){
//            categoryMap[category.label] = category
//        }
//
//        var category = categoryMap["Infant"]
//        var probability = category?.score
//        var n = 0
//
//        while (probability!! < 0.5){
//            n += 1
//            category = categoryMap[n.toString()]
//            probability += category?.score ?: 0.0f
//        }
//
//        return extractClassifications(listOf(category))
//    }
}