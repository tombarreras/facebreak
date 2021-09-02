package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.thomasjbarrerasconsulting.faces.BitmapUtils
import com.thomasjbarrerasconsulting.faces.FrameMetadata
import com.google.mlkit.vision.face.Face
import com.thomasjbarrerasconsulting.faces.ml.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat
import kotlin.math.round

class FaceClassifierProcessor(private val context: Context) {
    private var classificationTracker: ClassificationTracker = ClassificationTracker(10, "")

    fun getFaceClassifications(face: Face, image: InputImage): FaceWithClassifications {

        if (image.byteBuffer == null){
            return FaceWithClassifications(face, mutableListOf(), classifier)
        }
        val currentClassifier = classifier

        val bitmap: Bitmap =
            BitmapUtils.getBitmap(image.byteBuffer, FrameMetadata.Builder().setHeight(image.height).setWidth(image.width).setRotation(image.rotationDegrees).build())
                ?: return FaceWithClassifications(face, mutableListOf(), currentClassifier)

        val croppedBitmap = BitmapCropper.cropBitmap(bitmap, face.boundingBox, currentClassifier)

        try {
            val classifications: MutableList<String> = mutableListOf()
            val tensorImage = TensorImage.fromBitmap(croppedBitmap)

            if (currentClassifier != classificationTracker.classifier){
                resetClassificationTracker(currentClassifier)
            }

            when (currentClassifier) {
                DETECT_AGE -> {
                    val ageModel = AgesModel10500.newInstance(context)
                    classifications.addAll(extractAgeClassification(classificationTracker.merge(ageModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    ageModel.close()
                }
                DETECT_EMOTIONS -> {
                    val emotionsModel = EmotionsModel1600.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(emotionsModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(6))))
                    emotionsModel.close()
                }
                DETECT_GENDER -> {
                    val genderModel = GenderModel9000.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(genderModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(2))))
                    genderModel.close()
                }
                DETECT_FACE_SHAPE -> {
                    val faceShapeModel = FaceShapeModel1000d.newInstance(context)
                    classifications.addAll(extractFaceShapeClassifications(classificationTracker.merge(faceShapeModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    faceShapeModel.close()
                }
                DETECT_EYE_COLOR -> {
                    val model = EyeColorModel3.newInstance(context)
                    classifications.addAll(extractEyeColorClassification(classificationTracker.merge(model.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    model.close()
                }
                DETECT_FEATURES -> {
                    val featuresModel = FeaturesModel2000.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(featuresModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.take(6)))
                    featuresModel.close()
                }
                DETECT_CHARACTER -> {
                    val characterModel = CharacterModel1600.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(characterModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.take(6).filter { it.score >= 0.05 }))
                    characterModel.close()
                }
                DETECT_ANCESTRY -> {
                    val ancestryModel = AncestryModel5500.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(ancestryModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.take(6).filter { it.score >= 0.05 }))
                    ancestryModel.close()
                }
            }

            return FaceWithClassifications(face, classifications, currentClassifier)
        }
        finally{
            bitmap.recycle()
            croppedBitmap.recycle()
        }
    }

    private fun extractFaceShapeClassifications(outputs: List<Category?>): MutableList<String>{
        val interimOutputs: MutableList<Category?> = mutableListOf()

        var scaleFactor = 0.0f

        val adjustment = mapOf("Diamond" to 0.8f, "Square" to 0.9f, "Heart" to 0.95f, "Oval" to 0.95f, "Oblong" to 0.95f, "Pear" to 1.15f, "Round" to 1.0f)

        for (output in outputs){
            val adjustedValue = adjustment[output?.label]!! * output?.score!!
            interimOutputs.add(Category(output.label, adjustedValue))
            scaleFactor += adjustedValue
        }

        val adjustedOutputs: MutableList<Category?> = mutableListOf()
        for (interimOutput in interimOutputs){
            adjustedOutputs.add(Category(interimOutput?.label, (interimOutput?.score)?.div(scaleFactor)!!
            ))
        }
        val classifications: MutableList<String> = mutableListOf()
        val percentFormat: NumberFormat = NumberFormat.getPercentInstance()

        val sortedOutputs = adjustedOutputs.apply { sortByDescending { it?.score } }
        val veryStrongCategories: List<Category?> = sortedOutputs.filter {  it?.score!! >= 0.4  }
        val strongCategories: List<Category?> = sortedOutputs.filter { it?.score!! >= 0.3 && it.score < 0.4}
        val regularCategories: List<Category?> = sortedOutputs.filter { it?.score!! >= 0.2 && it.score < 0.3}
        val slightlyCategories: List<Category?> = sortedOutputs.filter {it?.score!! >= 0.14 && it.score < 0.2}
        val weakCategories: List<Category?> = sortedOutputs.filter { it?.score!! >= 0.07 && it.score < 0.14}
        val veryWeakCategories: List<Category?> = sortedOutputs.filter { it?.score!! < 0.07 }

        for (shape in veryStrongCategories){
            classifications.add("Very Strong ${shape?.label} (${percentFormat.format(shape?.score)})")
        }
        for (shape in strongCategories){
            classifications.add("Strong ${shape?.label} (${percentFormat.format(shape?.score)})")
        }
        for (shape in regularCategories){
            classifications.add("${shape?.label} (${percentFormat.format(shape?.score)})")
        }
        val allClassificationsAreWeak = !classifications.any()
        if (allClassificationsAreWeak){
            for (shape in slightlyCategories) {
                classifications.add("${shape?.label} (${percentFormat.format(shape?.score)})")
            }
        }

        classifications.add("")

        if (!allClassificationsAreWeak and slightlyCategories.any()){
            classifications.add("Slightly: ${slightlyCategories.map{ it?.label }.joinToString(separator = ", ")} (${slightlyCategories.map{ percentFormat.format(it?.score) }.joinToString(separator = "/")})")
        }
        if (weakCategories.any()){
            classifications.add("Unlike: ${weakCategories.map{ it?.label }.joinToString(separator = ", ")} (${weakCategories.map{ percentFormat.format(it?.score) }.joinToString(separator = "/")})")
        }
        if (veryWeakCategories.any()){
            classifications.add("Very Unlike: ${veryWeakCategories.map{ it?.label }.joinToString(separator = ", ")} (${veryWeakCategories.map{ percentFormat.format(it?.score) }.joinToString(separator = "/")})")
        }
        return classifications
    }

    private fun extractEyeColorClassification(outputs: List<Category?>): MutableList<String>{
        val classifications: MutableList<String> = mutableListOf()
        val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
        if (outputs[0]?.label == "Closed Eyes" && outputs[0]?.score!! > 0.5){
            classifications.add("Eyes are closed")
        } else {
            val eyesOpenProbability = 1.0 - outputs.find{ o -> o?.label == "Closed Eyes"}?.score!!
            if ((outputs.filterNot{ o -> o?.label == "Closed Eyes"}[0]?.score)?.div(eyesOpenProbability)!! > 0.85){
                val eyeColor = outputs.filterNot{ o -> o?.label == "Closed Eyes"}[0]?.label?.replace(" Eyes", "")
                val colorProbability = percentFormat.format((outputs.filterNot{ o -> o?.label == "Closed Eyes"}[0]?.score)?.div(eyesOpenProbability)!!)
                classifications.add("Pure $eyeColor ($colorProbability)")
            } else {
                val eyeColorOutputs = outputs.filterNot{ o -> o?.label == "Closed Eyes"}
                val eyeColorLabel1 = eyeColorOutputs[0]?.label?.replace(" Eyes", "")
                val eyeColorLabel2 = eyeColorOutputs[1]?.label?.replace(" Eyes", "")
                val eyeColorScore1 = eyeColorOutputs[0]?.score?.div(eyesOpenProbability)
                val eyeColorScore2 = eyeColorOutputs[1]?.score?.div(eyesOpenProbability)
                if ((eyeColorLabel1 == "Brown" && eyeColorLabel2 == "Blue") || (eyeColorLabel1 == "Blue" && eyeColorLabel2 == "Brown")){
                    classifications.add("$eyeColorLabel1 (${percentFormat.format(eyeColorScore1)})")
                    classifications.add("Trace $eyeColorLabel2 (${percentFormat.format(eyeColorScore2)})")
                } else {
                    classifications.add("$eyeColorLabel1-$eyeColorLabel2 (${percentFormat.format(eyeColorScore1)}/${percentFormat.format(eyeColorScore2)})")
                }
                if (eyeColorScore1?.plus(eyeColorScore2!!)!! < 0.85){
                    val eyeColorLabel3 = eyeColorOutputs[2]?.label?.replace(" Eyes", "")
                    val eyeColorScore3 = eyeColorOutputs[2]?.score?.div(eyesOpenProbability)
                    classifications.add("Trace $eyeColorLabel3 (${percentFormat.format(eyeColorScore3)})")

                    if (eyeColorScore1.plus(eyeColorScore2!!).plus(eyeColorScore3!!) < 0.85){
                        val eyeColorLabel4 = eyeColorOutputs[3]?.label?.replace(" Eyes", "")
                        val eyeColorScore4 = eyeColorOutputs[3]?.score?.div(eyesOpenProbability)
                        classifications.add("Trace $eyeColorLabel4 (${percentFormat.format(eyeColorScore4)})")
                    }
                }
            }
        }
        return classifications
    }

    private fun extractAgeClassification(outputs: List<Category?>): MutableList<String> {
        val classifications: MutableList<String> = mutableListOf()

        var minAge = 100
        var maxAge = 0
        var totalProbability = 0.0f
        var weightedAverage = 0.0f

        for (output in outputs){
            var age = 0
            val probability = output?.score ?: 0.0f
            totalProbability += probability
            age = if (output?.label == "Infant"){
                0
            } else {
                output?.label?.toInt() ?: 0
            }

            weightedAverage += probability * age
            if (minAge > age){
                minAge = age
            }
            if (maxAge < age){
                maxAge = age
            }
            if (totalProbability > 0.66){
                break
            }
        }

        val averageAge = round(minAge + 0.5f * (maxAge - minAge))
        val weightedAverageAge = round(weightedAverage / totalProbability)
        classifications.add("$weightedAverageAge ($minAge - $maxAge) $averageAge")

        return classifications
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

    private fun extractClassifications(outputs: List<Category?>): MutableList<String> {
        val classifications: MutableList<String> = mutableListOf()
        val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
        for (output in outputs) {
            val score: String = percentFormat.format(output?.score)
            val label: String = output?.label ?: "Unknown"
            classifications.add("$label ($score)")
        }
        return classifications
    }

    fun resetClassificationTracker(currentClassifier: String) {
        classificationTracker = ClassificationTracker(2, currentClassifier)
    }

    companion object {
        const val DETECT_AGE = "Detect Age"
        const val DETECT_EMOTIONS = "Detect Emotions"
        const val DETECT_GENDER = "Detect Gender"
        const val DETECT_FACE_SHAPE = "Detect Face Shape"
        const val DETECT_FEATURES = "Detect Physical Features"
        const val DETECT_EYE_COLOR = "Detect Eye Color"
        const val DETECT_CHARACTER = "Detect Character"
        const val DETECT_ANCESTRY = "Detect Ancestry"
//        @get:Synchronized @set:Synchronized
        var classifier = DETECT_AGE
    }
}