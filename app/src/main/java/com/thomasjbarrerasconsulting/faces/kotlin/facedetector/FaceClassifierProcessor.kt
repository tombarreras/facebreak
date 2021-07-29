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

class FaceClassifierProcessor(private val context: Context) {
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

            when (currentClassifier) {
                DETECT_AGE -> {
                    val ageModel = AgesModel10000.newInstance(context)
                    classifications.addAll(getAgeClassifications(ageModel, tensorImage))
                    ageModel.close()
                }
                DETECT_EMOTIONS -> {
                    val emotionsModel = EmotionsModel.newInstance(context)
                    classifications.addAll(extractClassifications(emotionsModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(2)))
                    emotionsModel.close()
                }
                DETECT_GENDER -> {
                    val genderModel = GenderModel9000.newInstance(context)
                    classifications.addAll(extractClassifications(genderModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(2)))
                    genderModel.close()
                }
                DETECT_FACE_SHAPE -> {
                    val faceShapeModel = FaceShapeModel2000c.newInstance(context)
                    classifications.addAll(extractClassifications(faceShapeModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }))
                    faceShapeModel.close()
                }
                DETECT_FEATURES -> {
                    val featuresModel = FeaturesModel2000b.newInstance(context)
                    classifications.addAll(extractClassifications(featuresModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(6).filter { it.score >= 0.05 }))
                    featuresModel.close()
                }
                DETECT_ANCESTRY -> {
                    val ancestryModel = AncestryModel12000.newInstance(context)
                    classifications.addAll(extractClassifications(ancestryModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(6).filter { it.score >= 0.05 }))
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

    private fun getAgeClassifications(
        ageModel: AgesModel10000,
        tensorImage: TensorImage
    ): MutableList<String> {
        val categories = ageModel.process(tensorImage).probabilityAsCategoryList

        val categoryMap = mutableMapOf("Infant" to Category("Infant", 0.0f))

        for (category in categories){
            categoryMap[category.label] = category
        }

        var category = categoryMap["Infant"]
        var probability = category?.score
        var n = 0

        while (probability!! < 0.5){
            n += 1
            category = categoryMap[n.toString()]
            probability += category?.score ?: 0.0f
        }

        return extractClassifications(listOf(category))
    }

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

    companion object {
        const val DETECT_AGE = "Detect Age"
        const val DETECT_EMOTIONS = "Detect Emotions"
        const val DETECT_GENDER = "Detect Gender"
        const val DETECT_FACE_SHAPE = "Detect Face Shape"
        const val DETECT_FEATURES = "Detect Physical Features"
        const val DETECT_ANCESTRY = "Detect Ancestry"
//        @get:Synchronized @set:Synchronized
        var classifier = DETECT_AGE
    }
}