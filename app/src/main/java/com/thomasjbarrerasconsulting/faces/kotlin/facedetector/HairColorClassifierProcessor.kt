/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class HairColorClassifierProcessor {
    companion object {
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
            val oneHundredPercent = NumberFormat.getPercentInstance().format(1.0f)

            return when (color.label) {
                "Black" -> "Pure Black (${oneHundredPercent})"
                "Blond" -> "True Blond (${oneHundredPercent})"
                "Brown" -> "Pure Brown (${oneHundredPercent})"
                "Red" -> "True Red (${oneHundredPercent})"
                else -> "${color.label} (${oneHundredPercent})"
            }
        }

        private fun descriptionOfAPrimaryColorAndProminentSecondaryColor(primaryColor: Category, secondaryColor: Category):String{
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            val percentages = descriptionOfPrimaryPercentSecondaryPercent(percentFormat, primaryColor, secondaryColor)
            when (primaryColor.label) {
                "Black" -> {
                    return when (secondaryColor.label) {
                        "Blue" -> "Blue-Black (${percentages})"
                        "Purple" -> "Violet Black (${percentages})"
                        "Brown" -> "Deep Brown-Black (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Blond" -> {
                    return when (secondaryColor.label){
                        "White" -> "Light Blond (${percentages})"
                        "Brown" -> "Medium Blond (${percentages})"
                        "Red" -> "Strawberry Blond (${percentages})"
                        "Yellow" -> "Light Yellow Blond (${percentages})"
                        "Orange" -> "Light Golden Blond (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Yellow" -> {
                    return when (secondaryColor.label){
                        "White" -> "Light Yellow (${percentages})"
                        "Brown" -> "Honey Yellow (${percentages})"
                        "Blond" -> "Yellow Blond (${percentages})"
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
                        "Blond" -> "Natural Red (${percentages})"
                        "Black" -> "Dark Red (${percentages})"
                        "Brown" -> "Auburn Red (${percentages})"
                        "Pink" -> "Raspberry Red (${percentages})"
                        "Purple" -> "Crimson Red (${percentages})"
                        "Orange" -> "Red-Orange (${percentages})"
                        "Neon Red" -> "Brilliant Red (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Neon Red" -> {
                    return when (secondaryColor.label){
                        "Orange" -> "Flaming Red (${percentages})"
                        "Red" -> "Brilliant Red (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Orange" -> {
                    return when (secondaryColor.label){
                        "Neon Red" -> "Brilliant Red-Orange (${percentages})"
                        "Red" -> "Brilliant Strawberry Blond (${percentages})"
                        "Blond" -> "Bright Golden Blond (${percentages})"
                        "Brown" -> "Golden Brown (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                "Grey" -> {
                    return when (secondaryColor.label){
                        "White" -> "Light Grey (${percentages})"
                        else -> descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                else -> return descriptionOfPrimaryWithAHintOfSecondary(percentFormat, primaryColor, secondaryColor)
            }
        }

        private fun descriptionOfTwoDominantColors(primaryColor: Category, secondaryColor: Category):String{
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            val hairColors = listOf(primaryColor, secondaryColor)
            val percentages = descriptionOfPrimaryPercentSecondaryPercent(percentFormat, primaryColor, secondaryColor)

            when (true) {
                hairColors.filter { it.label == "Black" }.any() -> {
                    return when (true) {
                        hairColors.filter { it.label == "Blue" }.any() -> "Midnight Blue (${percentages})"
                        hairColors.filter { it.label == "Purple" }.any() -> "Dark Purple (${percentages})"
                        hairColors.filter { it.label == "White" }.any() or hairColors.filter { it.label == "Grey" }.any() -> "Salt and Pepper (${percentages})"
                        hairColors.filter { it.label == "Brown" }.any() -> "Deep Brown (${percentages})"
                        hairColors.filter { it.label == "Red" }.any() -> "Deep Red (${percentages})"
                        hairColors.filter { it.label == "Orange" }.any() -> "Deep Orange (${percentages})"
                        hairColors.filter { it.label == "Neon Red" }.any() -> "Dark Neon Red (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                hairColors.filter { it.label == "Blond" }.any() -> {
                    return when (true){
                        hairColors.filter { it.label == "White" }.any() -> "Platinum Blond (${percentages})"
                        hairColors.filter { it.label == "Brown" }.any() -> "Dark Blond (${percentages})"
                        hairColors.filter { it.label == "Red" }.any() -> "Light Red (${percentages})"
                        hairColors.filter { it.label == "Yellow" }.any() -> "Bright Blond (${percentages})"
                        hairColors.filter { it.label == "Orange" }.any() -> "Golden Blond (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                hairColors.filter { it.label == "Brown" }.any() -> {
                    return when (true){
                        hairColors.filter { it.label == "Red" }.any() -> "Cinnamon (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                hairColors.filter { it.label == "Red" }.any() -> {
                    return when (true){
                        hairColors.filter { it.label == "Pink" }.any() -> "Dark Pink (${percentages})"
                        hairColors.filter { it.label == "Purple" }.any() -> "Maroon Red (${percentages})"
                        hairColors.filter { it.label == "Neon Red" }.any() -> "Bright Red (${percentages})"
                        hairColors.filter { it.label == "Orange" }.any() -> "Bright Red-Orange (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                hairColors.filter { it.label == "Grey" }.any() -> {
                    return when (true){
                        hairColors.filter { it.label == "White"}.any() -> "Greyish White (${percentages})"
                        else -> descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
                    }
                }
                else -> return descriptionOfPrimaryAndSecondary(percentFormat, primaryColor, secondaryColor)
            }
        }

        private fun descriptionOfPrimaryPercentSecondaryPercent(percentFormat: NumberFormat, primaryColor: Category, secondaryColor: Category) =
            "${percentFormat.format(primaryColor.score)} ${primaryColor.label} / ${percentFormat.format(secondaryColor.score)} ${secondaryColor.label}"

        private fun descriptionOfPrimaryWithAHintOfSecondary(percentFormat: NumberFormat, primaryColor: Category, secondaryColor: Category) =
            "${primaryColor.label} with a hint of ${secondaryColor.label} (${descriptionOfPrimaryPercentSecondaryPercent(percentFormat, primaryColor, secondaryColor)})"

        private fun descriptionOfPrimaryAndSecondary(percentFormat: NumberFormat, primaryColor: Category, secondaryColor: Category) =
            "${primaryColor.label} and ${secondaryColor.label} (${descriptionOfPrimaryPercentSecondaryPercent(percentFormat, primaryColor, secondaryColor)})"

        fun extractHairColorClassification(outputs: List<Category?>): MutableList<String> {
            val classifications: MutableList<String> = mutableListOf()

            if (outputs[0]?.label == "Bald" && outputs[0]?.score!! > 0.5) {
                classifications.add("No hair detected")
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

        private fun addClassificationForADominantColorDetected(hairColors: List<Category>, classifications: MutableList<String>) {
            if (!secondColorIsProminent(hairColors)){
                classifications.add("${hairColors.first().label} (${NumberFormat.getPercentInstance().format(hairColors.first().score)})")
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

            classifications.add("${hairColors.first().label} (${percentFormat.format(hairColors.first().score)})")
            classifications.add("${hairColors.take(2).last().label} (${percentFormat.format(hairColors.take(2).last().score)})")
            classifications.add("${hairColors.take(3).last().label} (${percentFormat.format(hairColors.take(3).last().score)})")

            addClassificationForRemainingColors(hairColors.takeLast(hairColors.count() - 3), classifications)
        }

        private fun addClassificationsForMixedHairColors(hairColors: List<Category>, classifications: MutableList<String>, prefix:String = "") {
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            for (color in hairColors){
                classifications.add("${prefix}${color.label} (${percentFormat.format(color.score)})")
            }
        }

        private fun addClassificationForRemainingColors(remainingColors: List<Category>, classifications: MutableList<String>, prefix:String = "Touch of ") {
            if (remainingColors.count() == 0){
                return
            }

            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()

            when {
                remainingColors.count() == 1 -> classifications.add("${prefix}${remainingColors.first().label} (${percentFormat.format(remainingColors.first().score)})")
                isOneDominantColor(remainingColors) -> {
                    if (!secondColorIsProminent(remainingColors)){
                        classifications.add("${prefix}${remainingColors.first().label} (${NumberFormat.getPercentInstance().format(remainingColors.first().score)})")
                        addClassificationForRemainingColors(remainingColors.takeLast(remainingColors.count() - 1), classifications, "Trace of ")
                    } else {
                        classifications.add("${prefix}${descriptionOfAPrimaryColorAndProminentSecondaryColor(remainingColors.first(), remainingColors.take(2).last())}")
                        addClassificationForRemainingColors(remainingColors.takeLast(remainingColors.count() - 2), classifications, "Trace of ")
                    }
                }
                areTwoDominantColors(remainingColors) -> {
                    classifications.add("${prefix}${descriptionOfTwoDominantColors(remainingColors.first(), remainingColors.take(2).last())}")
                    addClassificationForRemainingColors(remainingColors.takeLast(remainingColors.count() - 2), classifications, "Trace of ")
                }
                areThreeDominantColors(remainingColors) -> {
                    classifications.add("${prefix}${remainingColors.first().label} (${percentFormat.format(remainingColors.first().score)})")
                    classifications.add("${prefix}${remainingColors.take(2).last().label} (${percentFormat.format(remainingColors.take(2).last().score)})")
                    classifications.add("${prefix}${remainingColors.take(3).last().label} (${percentFormat.format(remainingColors.take(3).last().score)})")

                    addClassificationForRemainingColors(remainingColors.takeLast(remainingColors.count() - 3), classifications, "Trace of ")
                }
                else -> addClassificationsForMixedHairColors(remainingColors, classifications, prefix)
            }
        }
    }
}