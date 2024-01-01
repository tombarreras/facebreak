/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.FaceBreakApplication
import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat
import kotlin.math.max

class HairStyleClassifierProcessor {
    companion object{
        private fun getSingleHairStyleDescriptionWithPercentage(output: Category?):String {
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            val description =  ClassifierText.get(output!!.label)
            return "$description (${percentFormat.format(output.score)})"
        }

        private fun getTwoHairStylesDescription(first:Category?, second:Category?):MutableList<String> {
            val context = FaceBreakApplication.instance
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            val classifications: MutableList<String> = mutableListOf()
            val firstDescription = getSingleHairStyleDescriptionWithPercentage(first)
            val secondDescription = getSingleHairStyleDescriptionWithPercentage(second)
            var description = ""

            when (first!!.label) {
                "Afro" -> {
                    description = when (second!!.label) {
                        "Hairless" -> context.getString(R.string.hairstyle_thin_afro)
                        "Bob" -> context.getString(R.string.hairstyle_afro_bob)
                        "Buzz Cut" -> context.getString(R.string.hairstyle_short_afro)
                        "Curly" -> context.getString(R.string.hairstyle_loosely_curled_afro)
                        "Men's Medium" -> context.getString(R.string.hairstyle_medium_length_afro)
                        "Mohawk" -> context.getString(R.string.hairstyle_afro_mohawk)
                        "Long" -> context.getString(R.string.hairstyle_long_afro)
                        "Pixie" -> context.getString(R.string.hairstyle_afro_pixie)
                        else -> description
                    }
                }
                "Hairless" -> {
                    description = when (second!!.label) {
                        "Afro" -> context.getString(R.string.hairstyle_thin_afro)
                        "Buzz Cut" -> context.getString(R.string.hairstyle_shaved_or_balding)
                        "Curly" -> context.getString(R.string.hairstyle_thin_curly_hair)
                        "Men's Medium" -> context.getString(R.string.hairstyle_thin_medium_length_hair)
                        "Long" -> context.getString(R.string.hairstyle_long_thin_hair)
                        "Women's Medium" -> context.getString(R.string.hairstyle_thin_medium_length_hair)
                        "Women's Short" -> context.getString(R.string.hairstyle_short_thin_hair)
                        else -> description
                    }
                }
                "Bob" -> {
                    description = when (second!!.label) {
                        "Afro" -> context.getString(R.string.hairstyle_afro_bob)
                        "Curly" -> context.getString(R.string.hairstyle_bob_with_curl)
                        "Long" -> context.getString(R.string.hairstyle_long_bob)
                        "Men's Medium" -> context.getString(R.string.hairstyle_tousled_bob)
                        "Pixie" -> context.getString(R.string.hairstyle_pixie_bob)
                        "Women's Medium" -> context.getString(R.string.hairstyle_medium_bob)
                        "Women's Short" -> context.getString(R.string.hairstyle_short_bob)
                        else -> description
                    }
                }
                "Braids" -> {
                    description = when (second!!.label) {
                        "Curly" -> context.getString(R.string.hairstyle_thick_braids)
                        else -> description
                    }
                }
                "Buzz Cut" -> {
                    description = when (second!!.label) {
                        "Afro" -> context.getString(R.string.hairstyle_short_afro)
                        "Hairless" -> context.getString(R.string.hairstyle_very_short_hair)
                        "Men's Medium" -> context.getString(R.string.hairstyle_short_hair)
                        else -> description
                    }
                }
                "Curly" -> {
                    description = when (second!!.label) {
                        "Afro" -> context.getString(R.string.hairstyle_tightly_curled)
                        "Hairless" -> context.getString(R.string.hairstyle_thin_curly_hair)
                        "Bob" -> context.getString(R.string.hairstyle_curly_bob)
                        "Braids" -> context.getString(R.string.hairstyle_thick_braids)
                        "Long" -> context.getString(R.string.hairstyle_long_curly_hair)
                        "Men's Medium" -> context.getString(R.string.hairstyle_medium_length_curly_hair)
                        "Mohawk" -> context.getString(R.string.hairstyle_curly_mohawk)
                        "Mullet" -> context.getString(R.string.hairstyle_curly_mullet)
                        "Pig Tails" -> context.getString(R.string.hairstyle_loosely_curled_hair)
                        "Pixie" -> context.getString(R.string.hairstyle_curly_pixie)
                        "Women's Medium" -> context.getString(R.string.hairstyle_medium_length_curly_hair)
                        "Women's Short" -> context.getString(R.string.hairstyle_short_curly_hair)
                        else -> description
                    }
                }
                "Long" -> {
                    description = when (second!!.label) {
                        "Hairless" -> context.getString(R.string.hairstyle_long_thin_hair)
                        "Bob" -> context.getString(R.string.hairstyle_long_bob)
                        "Curly" -> context.getString(R.string.hairstyle_long_hair_with_curl)
                        "Mullet" -> context.getString(R.string.hairstyle_long_mullet)
                        "Women's Medium" -> context.getString(R.string.hairstyle_medium_long_hair)
                        "Men's Medium" -> context.getString(R.string.hairstyle_medium_long_hair)
                        else -> description
                    }
                }
                "Men's Medium" -> {
                    description = when (second!!.label) {
                        "Afro" -> context.getString(R.string.hairstyle_medium_length_afro)
                        "Hairless" -> context.getString(R.string.hairstyle_thin_medium_length_hair)
                        "Bob" -> context.getString(R.string.hairstyle_medium_bob)
                        "Buzz Cut" -> context.getString(R.string.hairstyle_short_hair)
                        "Curly" -> context.getString(R.string.hairstyle_medium_length_curly_hair)
                        "Long" -> context.getString(R.string.hairstyle_medium_long_hair)
                        "Mullet" -> context.getString(R.string.hairstyle_medium_length_mullet)
                        "Pixie" -> context.getString(R.string.hairstyle_tousled_pixie)
                        "Women's Short" -> context.getString(R.string.hairstyle_short_hair)
                        else -> description
                    }
                }
                "Mohawk" -> {
                    description = when (second!!.label) {
                        "Afro" -> context.getString(R.string.hairstyle_afro_mohawk)
                        "Curly" -> context.getString(R.string.hairstyle_curly_mohawk)
                        "Pixie" -> context.getString(R.string.hairstyle_pixie_mohawk)
                        else -> description
                    }
                }
                "Mullet" -> {
                    description = when (second!!.label) {
                        "Curly" -> context.getString(R.string.hairstyle_curly_mullet)
                        "Long" -> context.getString(R.string.hairstyle_long_mullet)
                        "Men's Medium" -> context.getString(R.string.hairstyle_medium_length_mullet)
                        "Pixie" -> context.getString(R.string.hairstyle_pixie_mullet)
                        "Women's Medium" -> context.getString(R.string.hairstyle_medium_length_mullet)
                        "Women's Short" -> context.getString(R.string.hairstyle_short_mullet)
                        else -> description
                    }
                }
                "Pixie" -> {
                    description = when (second!!.label) {
                        "Afro" -> context.getString(R.string.hairstyle_afro_pixie)
                        "Bob" -> context.getString(R.string.hairstyle_bob_pixie)
                        "Curly" -> context.getString(R.string.hairstyle_curly_pixie)
                        "Men's Medium" -> context.getString(R.string.hairstyle_tousled_pixie)
                        "Mohawk" -> context.getString(R.string.hairstyle_pixie_mohawk)
                        "Mullet" -> context.getString(R.string.hairstyle_pixie_mullet)
                        else -> description
                    }
                }
                "Women's Medium" -> {
                    description = when (second!!.label) {
                        "Hairless" -> context.getString(R.string.hairstyle_thin_medium_length_hair)
                        "Bob" -> context.getString(R.string.hairstyle_medium_bob)
                        "Curly" -> context.getString(R.string.hairstyle_medium_length_curly_hair)
                        "Long" -> context.getString(R.string.hairstyle_medium_long_hair)
                        "Mullet" -> context.getString(R.string.hairstyle_medium_length_mullet)
                        else -> description
                    }
                }
                "Women's Short" -> {
                    description = when (second!!.label) {
                        "Hairless" -> context.getString(R.string.hairstyle_thin_short_hair)
                        "Bob" -> context.getString(R.string.hairstyle_short_bob)
                        "Curly" -> context.getString(R.string.hairstyle_short_curly_hair)
                        "Men's Medium" -> context.getString(R.string.hairstyle_short_hair)
                        "Mullet" -> context.getString(R.string.hairstyle_short_mullet)
                        else -> description
                    }
                }
            }

            if (description == ""){
                classifications.add(firstDescription)
                classifications.add(secondDescription)
            } else {
                classifications.add("$description (${percentFormat.format(first.score)} ${ClassifierText.get(first.label)} / ${percentFormat.format(second!!.score)} ${ClassifierText.get(second.label)})")
            }

            return classifications
        }

        private fun adjustOutputs(outputs:List<Category?>):List<Category?>{
            var totalProbability = 0.0f
            for (c in outputs) {
                totalProbability += c!!.score
            }
            return outputs.map { Category(if (it?.label == "Bald") "Hairless" else it?.label, it?.score!! / totalProbability) }
        }

        private fun getHairStyleDescription(outputs:List<Category?>):MutableList<String>{
            val classifications: MutableList<String> = mutableListOf()

            if (outputs.isEmpty()){
                return classifications
            }

            if (outputs.size == 1){
                classifications.add(getSingleHairStyleDescriptionWithPercentage(outputs.first()))
            } else {
                classifications.addAll(getTwoHairStylesDescription(outputs.first(), outputs.take(2).last()))

                for (output in outputs.takeLast(outputs.size - 2)){
                    classifications.add(getSingleHairStyleDescriptionWithPercentage(output))
                }
            }

            return classifications
        }

        fun extractHairStyleClassification(outputs: List<Category?>): MutableList<String> {
            val adjustedOutputs = adjustOutputs(outputs)
            val mainOutputsCount = max(outputs.filter{it!!.score >= 0.1}.count(), 1)

            val mainAdjustedOutputs = adjustedOutputs.take(mainOutputsCount)
            val additionalAdjustedOutputs = adjustedOutputs.takeLast(adjustedOutputs.count() - mainAdjustedOutputs.count())

            val classifications = getHairStyleDescription(mainAdjustedOutputs)

            if (additionalAdjustedOutputs.count() > 0){
                classifications.add("")
                classifications.add(FaceBreakApplication.instance.getString(R.string.haircolor_hint_of))
                classifications.addAll(getHairStyleDescription(additionalAdjustedOutputs))
            }
            return classifications
        }
    }
}