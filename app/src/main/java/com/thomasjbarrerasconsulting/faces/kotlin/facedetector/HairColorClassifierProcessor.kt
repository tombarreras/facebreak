/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.util.Log
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.FaceBreakApplication
import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class HairColorClassifierProcessor {
    companion object {
        private const val TAG = "HairColorClassifierPrcr"
        private fun isOneDominantColor(hairColors: List<Category?>) = (hairColors.count() == 1) or (hairColors.first()?.score!! > (hairColors.take(2)).last()?.score!! * 1.5)
        private fun areTwoDominantColors(hairColors: List<Category?>) = (hairColors.count() == 2) or (hairColors.take(2).first()?.score!! > (hairColors.take(3)).last()?.score!! * 1.5)
        private fun areThreeDominantColors(hairColors: List<Category?>) = (hairColors.count() == 3) or (hairColors.take(3).first()?.score!! > (hairColors.take(4)).last()?.score!! * 1.5)
        private fun secondColorIsProminent(hairColors: List<Category?>) = (hairColors.count() > 1) and isOneDominantColor ( hairColors.takeLast(hairColors.count() - 1)) and (hairColors.first()?.score!! < (hairColors.take(2)).last()?.score!! * 2)

        private fun mainHairColors(outputs: List<Category?>): List<Category> {
            val primaryHairColors = outputs.filter { o -> o?.label != "Bald" }.filter { o -> o?.score!! > 0.1 }

            var totalProbability = 0.0f
            for (c in primaryHairColors) {
                totalProbability += c!!.score
            }
            return primaryHairColors.map { Category(it?.label, it?.score!! / totalProbability) }
        }

        private fun descriptionOfAStandaloneColor(color:Category): String{
            val context = FaceBreakApplication.instance
            val oneHundredPercent = NumberFormat.getPercentInstance().format(1.0f)

            return when (color.label) {
                "Black" -> context.getString(R.string.haircolor_pure_black) + " (${oneHundredPercent})"
                "Blond" -> context.getString(R.string.haircolor_true_blond) + " (${oneHundredPercent})"
                "Brown" -> context.getString(R.string.eyecolor_pure_brown) + " (${oneHundredPercent})"
                "Red" -> context.getString(R.string.haircolor_true_red) + " (${oneHundredPercent})"
                else -> "${ClassifierText.get(color.label)} (${oneHundredPercent})"
            }
        }

        private fun descriptionOfAPrimaryColorAndProminentSecondaryColor(primaryColor: Category, secondaryColor: Category):String{
            val context = FaceBreakApplication.instance
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            val percentages = descriptionOfPrimaryPercentSecondaryPercent(percentFormat, primaryColor, secondaryColor)
            when (primaryColor.label) {
                "Black" -> {
                    return when (secondaryColor.label) {
                        "Blue" -> context.getString(R.string.haircolor_blue_black) + " (${percentages})"
                        "Purple" -> context.getString(R.string.haircolor_violet_black) + " (${percentages})"
                        "Brown" -> context.getString(R.string.haircolor_deep_brown_black) + " (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Blond" -> {
                    return when (secondaryColor.label){
                        "White" -> context.getString(R.string.haircolor_light_blond) + " (${percentages})"
                        "Brown" -> context.getString(R.string.haircolor_medium_blond) + " (${percentages})"
                        "Red" -> context.getString(R.string.haircolor_strawberry_blond) + " (${percentages})"
                        "Yellow" -> context.getString(R.string.haircolor_light_yellow_blond) + " (${percentages})"
                        "Orange" -> context.getString(R.string.haircolor_light_golden_blond) + " (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Yellow" -> {
                    return when (secondaryColor.label){
                        "White" -> context.getString(R.string.haircolor_light_yellow) + " (${percentages})"
                        "Brown" -> context.getString(R.string.haircolor_honey_yellow) + " (${percentages})"
                        "Blond" -> context.getString(R.string.haircolor_yellow_blond) + " (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Brown" -> {
                    return when (secondaryColor.label){
                        "Black" -> "Dark Brown (${percentages})"
                        "Red" -> "Red Brown (${percentages})"
                        "Blond" -> "Light Brown (${percentages})"
                        "Purple" -> "Plum Brown (${percentages})"
                        "Yellow" -> "Dark Honey Brown (${percentages})"
                        "Orange" -> "Dark Golden Brown (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Red" -> {
                    return when (secondaryColor.label){
                        "Blond" -> context.getString(R.string.haircolor_natural_red) + " (${percentages})"
                        "Black" -> context.getString(R.string.haircolor_dark_red) + " (${percentages})"
                        "Brown" -> context.getString(R.string.haircolor_auburn_red) + " (${percentages})"
                        "Pink" -> context.getString(R.string.haircolor_raspberry_red) + " (${percentages})"
                        "Purple" -> context.getString(R.string.haircolor_crimson_red) + " (${percentages})"
                        "Orange" -> context.getString(R.string.haircolor_red_orange) + " (${percentages})"
                        "Neon Red" -> context.getString(R.string.haircolor_brilliant_red) + " (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Neon Red" -> {
                    return when (secondaryColor.label){
                        "Orange" -> context.getString(R.string.haircolor_flaming_red) + " (${percentages})"
                        "Red" -> context.getString(R.string.haircolor_brilliant_red) + " (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Orange" -> {
                    return when (secondaryColor.label){
                        "Neon Red" -> context.getString(R.string.haircolor_brilliant_red_orange) + " (${percentages})"
                        "Red" -> context.getString(R.string.haircolor_brilliant_strawberry_blond) + " (${percentages})"
                        "Blond" -> context.getString(R.string.haircolor_bright_golden_blond) + " (${percentages})"
                        "Brown" -> context.getString(R.string.haircolor_golden_brown) + " (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Grey" -> {
                    return when (secondaryColor.label){
                        "White" -> context.getString(R.string.haircolor_light_grey) + " (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                else -> return descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
            }
        }

        private fun descriptionOfTwoDominantColors(primaryColor: Category, secondaryColor: Category):String{
            val context = FaceBreakApplication.instance
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            val hairColors = listOf(primaryColor, secondaryColor)
            val percentages = descriptionOfPrimaryPercentSecondaryPercent(percentFormat, primaryColor, secondaryColor)

            when (true) {
                hairColors.filter { it.label == "Black" }.any() -> {
                    return when (true) {
                        hairColors.filter { it.label == "Blue" }.any() -> context.getString(R.string.haircolor_midnight_blue) + " (${percentages})"
                        hairColors.filter { it.label == "Purple" }.any() -> context.getString(R.string.haircolor_dark_purple) + " (${percentages})"
                        hairColors.filter { it.label == "White" }.any() or hairColors.filter { it.label == "Grey" }.any() -> context.getString(R.string.haircolor_salt_and_pepper) + " (${percentages})"
                        hairColors.filter { it.label == "Brown" }.any() -> context.getString(R.string.haircolor_deep_brown) + " (${percentages})"
                        hairColors.filter { it.label == "Red" }.any() -> context.getString(R.string.haircolor_deep_red) + " (${percentages})"
                        hairColors.filter { it.label == "Orange" }.any() -> context.getString(R.string.haircolor_deep_orange) + " (${percentages})"
                        hairColors.filter { it.label == "Neon Red" }.any() -> context.getString(R.string.haircolor_dark_neon_red) + " (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                hairColors.filter { it.label == "Blond" }.any() -> {
                    return when (true){
                        hairColors.filter { it.label == "White" }.any() -> context.getString(R.string.haircolor_platinum_blond) + " (${percentages})"
                        hairColors.filter { it.label == "Brown" }.any() -> context.getString(R.string.haircolor_dark_blond) + " (${percentages})"
                        hairColors.filter { it.label == "Red" }.any() -> context.getString(R.string.haircolor_light_red) + " (${percentages})"
                        hairColors.filter { it.label == "Yellow" }.any() -> context.getString(R.string.haircolor_bright_blond) + " (${percentages})"
                        hairColors.filter { it.label == "Orange" }.any() -> context.getString(R.string.haircolor_golden_blond) + " (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                hairColors.filter { it.label == "Brown" }.any() -> {
                    return when (true){
                        hairColors.filter { it.label == "Red" }.any() -> context.getString(R.string.haircolor_cinnamon) + " (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                hairColors.filter { it.label == "Red" }.any() -> {
                    return when (true){
                        hairColors.filter { it.label == "Pink" }.any() -> context.getString(R.string.haircolor_dark_pink) + " (${percentages})"
                        hairColors.filter { it.label == "Purple" }.any() -> context.getString(R.string.haircolor_maroon_red) + " (${percentages})"
                        hairColors.filter { it.label == "Neon Red" }.any() -> context.getString(R.string.haircolor_bright_red) + " (${percentages})"
                        hairColors.filter { it.label == "Orange" }.any() -> context.getString(R.string.haircolor_bright_red_orange) + " (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                hairColors.filter { it.label == "Grey" }.any() -> {
                    return when (true){
                        hairColors.filter { it.label == "White"}.any() -> context.getString(R.string.haircolor_grayish_white) + " (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                else -> return descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
            }
        }

        private fun descriptionOfPrimaryPercentSecondaryPercent(percentFormat: NumberFormat, primaryColor: Category, secondaryColor: Category) =
            "${percentFormat.format(primaryColor.score)} ${ClassifierText.get(primaryColor.label)} / ${percentFormat.format(secondaryColor.score)} ${ClassifierText.get(secondaryColor.label)}"

        private fun descriptionOfPrimaryWithAHintOfSecondary(percentFormat: NumberFormat, primaryColor: Category, secondaryColor: Category) =
            ClassifierText.get(primaryColor.label) + " " + FaceBreakApplication.instance.getString(R.string.haircolor_with_a_hint_of) + " " + ClassifierText.get(secondaryColor.label) +
                    " (${descriptionOfPrimaryPercentSecondaryPercent(percentFormat, primaryColor, secondaryColor)})"

        private fun descriptionOfPrimaryAndSecondary(percentFormat: NumberFormat, primaryColor: Category, secondaryColor: Category) =
            ClassifierText.get(primaryColor.label) + " " + FaceBreakApplication.instance.getString(R.string.haircolor_and) + " " +
                    ClassifierText.get(secondaryColor.label) + " (${descriptionOfPrimaryPercentSecondaryPercent(percentFormat, primaryColor, secondaryColor)})"

        fun extractHairColorClassification(outputs: List<Category?>): MutableList<String> {
            try{
                val context = FaceBreakApplication.instance
                val classifications: MutableList<String> = mutableListOf()

                if (outputs[0]?.label == "Bald" && outputs[0]?.score!! > 0.5) {
                    classifications.add(context.getString(R.string.haircolor_no_hair_detected))
                } else {
                    val hairColors = mainHairColors(outputs)

                    when {
                        hairColors.count() == 1 -> classifications.add(descriptionOfAStandaloneColor(hairColors.first()))
                        isOneDominantColor(hairColors) -> addClassificationForADominantColorDetected(hairColors, classifications)
                        areTwoDominantColors(hairColors) -> addClassificationForTwoDominantColorsDetected(hairColors, classifications)
                        areThreeDominantColors(hairColors) -> addClassificationsForThreeDominantHairColors(hairColors, classifications)
                        else -> addClassificationsForMixedHairColors(hairColors, classifications)
                    }
                }
                return classifications
            }
            catch (e: Exception){
                Log.e(TAG, e.toString())
                return mutableListOf(FaceBreakApplication.instance.getString(R.string.haircolor_no_hair_detected))
            }
        }

        private fun addClassificationForADominantColorDetected(hairColors: List<Category>, classifications: MutableList<String>) {
            if (!secondColorIsProminent(hairColors)){
                classifications.add("${ClassifierText.get(hairColors.first().label)} (${NumberFormat.getPercentInstance().format(hairColors.first().score)})")
                addClassificationForRemainingColors(hairColors.takeLast(hairColors.count() - 1), classifications)
            } else {
                classifications.add(descriptionOfAPrimaryColorAndProminentSecondaryColor(hairColors.first(), hairColors.take(2).last()))
                addClassificationForRemainingColors(hairColors.takeLast(hairColors.count() - 2), classifications)
            }
        }

        private fun addClassificationForTwoDominantColorsDetected(hairColors: List<Category>, classifications: MutableList<String>) {
            classifications.add(descriptionOfTwoDominantColors(hairColors.first(), hairColors.take(2).last()))
            addClassificationForRemainingColors(hairColors.takeLast(hairColors.count() - 2), classifications)
        }

        private fun addClassificationsForThreeDominantHairColors(hairColors: List<Category>, classifications: MutableList<String>) {
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()

            classifications.add("${ClassifierText.get(hairColors.first().label)} (${percentFormat.format(hairColors.first().score)})")
            classifications.add("${ClassifierText.get(hairColors.take(2).last().label)} (${percentFormat.format(hairColors.take(2).last().score)})")
            classifications.add("${ClassifierText.get(hairColors.take(3).last().label)} (${percentFormat.format(hairColors.take(3).last().score)})")

            addClassificationForRemainingColors(hairColors.takeLast(hairColors.count() - 3), classifications)
        }

        private fun addClassificationsForMixedHairColors(hairColors: List<Category>, classifications: MutableList<String>, prefix:String = "") {
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            for (color in hairColors){
                classifications.add("${prefix}${ClassifierText.get(color.label)} (${percentFormat.format(color.score)})")
            }
        }

        private fun addClassificationForRemainingColors(remainingColors: List<Category>, classifications: MutableList<String>, prefix:String = "Touch of ") {
            val context = FaceBreakApplication.instance
            if (remainingColors.count() == 0){
                return
            }

            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()

            when {
                remainingColors.count() == 1 -> classifications.add("${prefix}${ClassifierText.get(remainingColors.first().label)} (${percentFormat.format(remainingColors.first().score)})")
                isOneDominantColor(remainingColors) -> {
                    if (!secondColorIsProminent(remainingColors)){
                        classifications.add("${prefix}${ClassifierText.get(remainingColors.first().label)} (${NumberFormat.getPercentInstance().format(remainingColors.first().score)})")
                        addClassificationForRemainingColors(remainingColors.takeLast(remainingColors.count() - 1), classifications, context.getString(R.string.haircolor_trace_of) + " ")
                    } else {
                        classifications.add("${prefix}${descriptionOfAPrimaryColorAndProminentSecondaryColor(remainingColors.first(), remainingColors.take(2).last())}")
                        addClassificationForRemainingColors(remainingColors.takeLast(remainingColors.count() - 2), classifications, context.getString(R.string.haircolor_trace_of) + " ")
                    }
                }
                areTwoDominantColors(remainingColors) -> {
                    classifications.add("${prefix}${descriptionOfTwoDominantColors(remainingColors.first(), remainingColors.take(2).last())}")
                    addClassificationForRemainingColors(remainingColors.takeLast(remainingColors.count() - 2), classifications, context.getString(R.string.haircolor_trace_of) + " ")
                }
                areThreeDominantColors(remainingColors) -> {
                    classifications.add("${prefix}${ClassifierText.get(remainingColors.first().label)} (${percentFormat.format(remainingColors.first().score)})")
                    classifications.add("${prefix}${ClassifierText.get(remainingColors.take(2).last().label)} (${percentFormat.format(remainingColors.take(2).last().score)})")
                    classifications.add("${prefix}${ClassifierText.get(remainingColors.take(3).last().label)} (${percentFormat.format(remainingColors.take(3).last().score)})")

                    addClassificationForRemainingColors(remainingColors.takeLast(remainingColors.count() - 3), classifications, context.getString(R.string.haircolor_trace_of) + " ")
                }
                else -> addClassificationsForMixedHairColors(remainingColors, classifications, prefix)
            }
        }
    }
}