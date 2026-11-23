/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector
import org.tensorflow.lite.support.label.Category
import java.util.*
import kotlin.math.roundToInt

class ClassificationTracker(private val timeOutSeconds: Float, val classifier: FaceClassifierProcessor.Classifier) {
    private val categories: MutableMap<String, MutableList<ClassificationProbability>> = mutableMapOf()

    fun merge(newCategories: List<Category>): MutableList<Category>{
        val categoryList = mutableListOf<Category>()
        val now = Date()
        // Compare plain millisecond timestamps: this runs for every label on every frame, and
        // allocating Calendar objects per stored score was a measurable cost.
        val oldestValidMs = now.time - timeOutSeconds.roundToInt() * 1000L

        for (category in newCategories){
            val history = categories.getOrPut(category.label) { mutableListOf() }

            history.removeAll { it.timeStamp.time < oldestValidMs }
            history.add(ClassificationProbability(now, category.score))

            var score = 0.0f
            for (classificationProbability in history){
                score += classificationProbability.probability
            }
            score /= history.size

            categoryList.add(Category(category.label, score))
        }

        return categoryList
    }
}