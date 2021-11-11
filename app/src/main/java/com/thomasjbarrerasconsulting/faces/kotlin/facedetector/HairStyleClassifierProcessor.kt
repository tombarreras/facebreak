package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class HairStyleClassifierProcessor {
    companion object{

        private fun getSingleHairStyleDescription(label:String):String{
            return label.replace("Men's Medium", "Medium-Length Hair")
                .replace("Women's Medium", "Medium-Length Hair")
                .replace("Long", "Long Hair")
        }

        private fun getSingleHairStyleDescriptionWithPercentage(output: Category?):String {
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            val description =  getSingleHairStyleDescription(output!!.label)
            return "$description (${percentFormat.format(output.score)})"
        }

        private fun getTwoHairStylesDescription(first:Category?, second:Category?):MutableList<String> {
            val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
            val classifications: MutableList<String> = mutableListOf()
            val firstDescription = getSingleHairStyleDescriptionWithPercentage(first)
            val secondDescription = getSingleHairStyleDescriptionWithPercentage(second)
            var description = ""

            when (first!!.label) {
                "Afro" -> {
                    description = when (second!!.label) {
                        "Bald" -> "Thin Afro"
                        "Bob" -> "Afro Bob"
                        "Buzz Cut" -> "Short Afro"
                        "Curly" -> "Loosely-Curled Afro"
                        "Men's Medium" -> "Medium-Length Afro"
                        "Mohawk" -> "Afro Mohawk"
                        "Pixie" -> "Afro Pixie"
                        else -> description
                    }
                }
                "Bald" -> {
                    description = when (second!!.label) {
                        "Afro" -> "Thin Afro"
                        "Buzz Cut" -> "Shaved Head"
                        "Curly" -> "Thin, Curly Hair"
                        "Men's Medium" -> "Thin, Medium-Length Hair"
                        "Long" -> "Long, Thin Hair"
                        "Women's Medium" -> "Thin, Medium-Length Hair"
                        else -> description
                    }
                }
                "Bob" -> {
                    description = when (second!!.label) {
                        "Afro" -> "Afro Bob"
                        "Curly" -> "Bob with Curl"
                        "Long" -> "Long Bob"
                        "Women's Medium" -> "Medium Bob"
                        "Men's Medium" -> "Touseled Bob"
                        "Pixie" -> "Bob Pixie"
                        else -> description
                    }
                }
                "Braids" -> {
                    description = when (second!!.label) {
                        "Curly" -> "Thick Braids"
                        else -> description
                    }
                }
                "Buzz Cut" -> {
                    description = when (second!!.label) {
                        "Afro" -> "Short Afro"
                        "Bald" -> "Very Short Hair"
                        "Men's Medium" -> "Short Hair"
                        else -> description
                    }
                }
                "Curly" -> {
                    description = when (second!!.label) {
                        "Afro" -> "Tightly-Curled"
                        "Bald" -> "Thin, Curly Hair"
                        "Bob" -> "Curly Bob"
                        "Braids" -> "Thick Braids"
                        "Long" -> "Long, Curly Hair"
                        "Men's Medium" -> "Medium-Length, Curly Hair"
                        "Mohawk" -> "Curly Mohawk"
                        "Mullet" -> "Curly Mullet"
                        "Pig Tails" -> "Loosely-Curled Hair"
                        "Women's Medium" -> "Medium-Length, Curly Hair"
                        else -> description
                    }
                }
                "Long" -> {
                    description = when (second!!.label) {
                        "Bald" -> "Long, Thin Hair"
                        "Bob" -> "Long Bob"
                        "Curly" -> "Long Hair with Curl"
                        "Mullet" -> "Long Mullet"
                        "Women's Medium" -> "Medium-Long Hair"
                        "Men's Medium" -> "Medium-Long Hair"
                        else -> description
                    }
                }
                "Men's Medium" -> {
                    description = when (second!!.label) {
                        "Afro" -> "Medium-Length Afro"
                        "Bald" -> "Thin, Medium-Length Hair"
                        "Buzz Cut" -> "Short Hair"
                        "Curly" -> "Medium-Length, Curly Hair"
                        "Long" -> "Medium-Long Hair"
                        "Mullet" -> "Medium-Length Mullet"
                        "Pixie" -> "Tousled Pixie"
                        else -> description
                    }
                }
                "Mohawk" -> {
                    description = when (second!!.label) {
                        "Afro" -> "Afro Mohawk"
                        "Curly" -> "Curly Mohawk"
                        "Pixie" -> "Pixie Mohawk"
                        else -> description
                    }
                }
                "Mullet" -> {
                    description = when (second!!.label) {
                        "Curly" -> "Curly Mullet"
                        "Long" -> "Long Mullet"
                        "Men's Medium" -> "Medium-Length Mullet"
                        "Women's Medium" -> "Medium-Length Mullet"
                        else -> description
                    }
                }
                "Pixie" -> {
                    description = when (second!!.label) {
                        "Afro" -> "Afro Pixie"
                        "Bob" -> "Pixie Bob"
                        "Men's Medium" -> "Tousled Pixie"
                        "Mohawk" -> "Pixie Mohawk"
                        else -> description
                    }
                }
                "Women's Medium" -> {
                    description = when (second!!.label) {
                        "Bald" -> "Thin, Medium-Length Hair"
                        "Bob" -> "Medium Bob"
                        "Curly" -> "Medium-Length, Curly Hair"
                        "Long" -> "Medium-Long Hair"
                        "Mullet" -> "Medium-Length Mullet"
                        else -> description
                    }
                }
            }

            if (description == ""){
                classifications.add(firstDescription)
                classifications.add(secondDescription)
            } else {
                classifications.add("$description (${percentFormat.format(first.score)} ${getSingleHairStyleDescription(first.label)} / ${percentFormat.format(second!!.score)} ${getSingleHairStyleDescription(second.label)})")
            }

            return classifications
        }

        private fun adjustOutputs(outputs:List<Category?>):List<Category?>{
            var totalProbability = 0.0f
            for (c in outputs) {
                totalProbability += c!!.score
            }
            return outputs.map { Category(it?.label, it?.score!! / totalProbability) }
        }

        fun getHairStyleDescription(outputs:List<Category?>):MutableList<String>{
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
            return getHairStyleDescription(adjustOutputs(outputs))
        }
    }
}