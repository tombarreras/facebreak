/*
 * Copyright 2021 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector
import org.tensorflow.lite.support.label.Category
import java.util.*
import kotlin.math.roundToInt

class ClassificationTracker(val timeOutSeconds: Float, val classifier: String) {
    private val categories: MutableMap<String, MutableList<ClassificationProbability>> = mutableMapOf()

    fun merge(newCategories: List<Category>): MutableList<Category>{
        val categoryList = mutableListOf<Category>()
        val now = Date()
        val calendarNow = Calendar.getInstance()
        calendarNow.time = now

        for (category in newCategories){
            if (categories[category.label] == null) {
                categories[category.label] = mutableListOf()
            }

            val expiredClassificationProbabilities = mutableListOf<ClassificationProbability>()
            for (classificationProbability in categories[category.label]!!){
                val expiresOn = Calendar.getInstance()
                expiresOn.time = classificationProbability.timeStamp
                expiresOn.add(Calendar.SECOND, timeOutSeconds.roundToInt())

                if (calendarNow > expiresOn) {
                    expiredClassificationProbabilities.add(classificationProbability)
                }
            }

            for (expiredClassificationProbability in expiredClassificationProbabilities){
                categories[category.label]?.remove(expiredClassificationProbability)
            }
            categories[category.label]?.add(ClassificationProbability(now, category.score))

            var score = 0.0f
            var scoresCount = 0
            for (classificationProbability in categories[category.label]!!){
                score += classificationProbability.probability
                scoresCount++
            }
            score /= scoresCount

            categoryList.add(Category(category.label, score))
        }

        return categoryList
    }
}