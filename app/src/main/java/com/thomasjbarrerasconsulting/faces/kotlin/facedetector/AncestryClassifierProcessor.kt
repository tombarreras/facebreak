/*
 * Copyright 2021 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class AncestryClassifierProcessor {
    companion object {
        fun extractAncestryClassifications(outputs: List<Category?>): MutableList<String> {
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            val classifications: MutableList<String> = mutableListOf()

            val significantOutputs = outputs.filter{ it!!.score >= 0.0625 }
            val totalScore = significantOutputs.map { it!!.score }.sum()

            for (output in significantOutputs){
                classifications.add("${output!!.label} (${percentFormat.format(output.score / totalScore)})")
            }

            return classifications
        }
    }
}
