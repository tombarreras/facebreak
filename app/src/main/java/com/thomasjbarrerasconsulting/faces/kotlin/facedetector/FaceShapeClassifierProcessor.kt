/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class FaceShapeClassifierProcessor {
    enum class FaceShapeStrength {
        VeryStrong,
        Strong,
        Regular,
        Slight,
        Weak,
        VeryWeak
    }
    companion object {
        fun extractFaceShapeClassifications(outputs: List<Category?>): MutableList<String>{
            val outputsByStrength: Map<FaceShapeStrength, List<Category?>> = splitOutputsByScoreStrength(adjustModelOutput(outputs))

            val classifications: MutableList<String> = mutableListOf()
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()

            for (shape in outputsByStrength[FaceShapeStrength.VeryStrong]!!){
                classifications.add("Very Strong ${shape?.label} (${percentFormat.format(shape?.score)})")
            }
            for (shape in outputsByStrength[FaceShapeStrength.Strong]!!){
                classifications.add("Strong ${shape?.label} (${percentFormat.format(shape?.score)})")
            }
            for (shape in outputsByStrength[FaceShapeStrength.Regular]!!){
                classifications.add("${shape?.label} (${percentFormat.format(shape?.score)})")
            }
            val allClassificationsAreWeak = !classifications.any()
            if (allClassificationsAreWeak){
                for (shape in outputsByStrength[FaceShapeStrength.Slight]!!) {
                    classifications.add("${shape?.label} (${percentFormat.format(shape?.score)})")
                }
            }
            if (!allClassificationsAreWeak and outputsByStrength[FaceShapeStrength.Slight]!!.any()){
                classifications.add("Slightly: ${outputsByStrength[FaceShapeStrength.Slight]!!.map{ it?.label }.joinToString(separator = ", ")} (${outputsByStrength[FaceShapeStrength.Slight]!!.map{ percentFormat.format(it?.score) }.joinToString(separator = "/")})")
            }
            if (outputsByStrength[FaceShapeStrength.Weak]!!.any() or outputsByStrength[FaceShapeStrength.VeryWeak]!!.any())
                classifications.add("")
            if (outputsByStrength[FaceShapeStrength.Weak]!!.any()){
                classifications.add("Unlike: ${outputsByStrength[FaceShapeStrength.Weak]!!.map{ it?.label }.joinToString(separator = ", ")} (${outputsByStrength[FaceShapeStrength.Weak]!!.map{ percentFormat.format(it?.score) }.joinToString(separator = "/")})")
            }
            if (outputsByStrength[FaceShapeStrength.VeryWeak]!!.any()){
                classifications.add("Very Unlike: ${outputsByStrength[FaceShapeStrength.VeryWeak]!!.map{ it?.label }.joinToString(separator = ", ")} (${outputsByStrength[FaceShapeStrength.VeryWeak]!!.map{ percentFormat.format(it?.score) }.joinToString(separator = "/")})")
            }
            return classifications
        }

        private fun splitOutputsByScoreStrength(adjustedOutputs: MutableList<Category?>): Map<FaceShapeStrength, List<Category?>> {
            val sortedOutputs = adjustedOutputs.apply { sortByDescending { it?.score } }

            val splitOutputs = mapOf(
                FaceShapeStrength.VeryStrong to sortedOutputs.filter { it?.score!! >= 0.4 },
                FaceShapeStrength.Strong to sortedOutputs.filter { it?.score!! >= 0.3 && it.score < 0.4 },
                FaceShapeStrength.Regular to sortedOutputs.filter { it?.score!! >= 0.2 && it.score < 0.3 },
                FaceShapeStrength.Slight to sortedOutputs.filter { it?.score!! >= 0.14 && it.score < 0.2 },
                FaceShapeStrength.Weak to sortedOutputs.filter { it?.score!! >= 0.07 && it.score < 0.14 },
                FaceShapeStrength.VeryWeak to sortedOutputs.filter { it?.score!! < 0.07 }
            )

            val reSplitOutputs = mapOf(
                FaceShapeStrength.VeryStrong to splitOutputs[FaceShapeStrength.VeryStrong]?.toMutableList(),
                FaceShapeStrength.Strong to mutableListOf(),
                FaceShapeStrength.Regular to mutableListOf(),
                FaceShapeStrength.Slight to mutableListOf(),
                FaceShapeStrength.Weak to mutableListOf(),
                FaceShapeStrength.VeryWeak to mutableListOf()
            )

            for (strength in listOf(FaceShapeStrength.Strong, FaceShapeStrength.Regular, FaceShapeStrength.Slight, FaceShapeStrength.Weak, FaceShapeStrength.VeryWeak)) {
                for (output in splitOutputs[strength]!!){
                    // If this score is w/i 2% of the lowest-scoring output in the previous set
                    var assignedTo = strength
                    if (reSplitOutputs[nextStrength(strength)]?.any()!!) {
                        if (reSplitOutputs[nextStrength(strength)]?.last()?.score?.minus(output!!.score)!! < 0.03){
                            assignedTo = nextStrength((strength))
                        }
                    }
                    reSplitOutputs[assignedTo]?.add(output)
                }
            }

            return mapOf(
                FaceShapeStrength.VeryStrong to reSplitOutputs[FaceShapeStrength.VeryStrong]!!.toList(),
                FaceShapeStrength.Strong to reSplitOutputs[FaceShapeStrength.Strong]!!.toList(),
                FaceShapeStrength.Regular to reSplitOutputs[FaceShapeStrength.Regular]!!.toList(),
                FaceShapeStrength.Slight to reSplitOutputs[FaceShapeStrength.Slight]!!.toList(),
                FaceShapeStrength.Weak to reSplitOutputs[FaceShapeStrength.Weak]!!.toList(),
                FaceShapeStrength.VeryWeak to reSplitOutputs[FaceShapeStrength.VeryWeak]!!.toList())
        }

        private fun nextStrength(strength: FaceShapeStrength): FaceShapeStrength{
            if (strength == FaceShapeStrength.VeryWeak){
                return FaceShapeStrength.Weak
            }
            if (strength == FaceShapeStrength.Weak){
                return FaceShapeStrength.Slight
            }
            if (strength == FaceShapeStrength.Slight){
                return FaceShapeStrength.Regular
            }
            if (strength == FaceShapeStrength.Regular){
                return FaceShapeStrength.Strong
            }
            return FaceShapeStrength.VeryStrong
        }

        private fun adjustModelOutput(outputs: List<Category?>): MutableList<Category?> {
            val interimOutputs: MutableList<Category?> = mutableListOf()

            var scaleFactor = 0.0f

            val adjustment = mapOf(
                "Diamond" to 0.65f,
                "Square" to 0.9f,
                "Heart" to 0.95f,
                "Oval" to 0.95f,
                "Oblong" to 1.0f,
                "Pear" to 1.15f,
                "Round" to 1.0f
            )

            for (output in outputs) {
                val adjustedValue = adjustment[output?.label]!! * output?.score!!
                interimOutputs.add(Category(output.label, adjustedValue))
                scaleFactor += adjustedValue
            }

            val adjustedOutputs: MutableList<Category?> = mutableListOf()
            for (interimOutput in interimOutputs) {
                adjustedOutputs.add(
                    Category(
                        interimOutput?.label, (interimOutput?.score)?.div(scaleFactor)!!
                    )
                )
            }
            return adjustedOutputs
        }
    }
}