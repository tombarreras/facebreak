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
                    classifications.addAll(AgeClassifierProcessor.extractAgeClassification(classificationTracker.merge(ageModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    ageModel.close()
                }
                DETECT_EMOTIONS -> {
                    val emotionsModel = EmotionsModel1600.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(emotionsModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(2))))
                    emotionsModel.close()
                }
                DETECT_GENDER -> {
                    val genderModel = GenderModel9000.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(genderModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(2))))
                    genderModel.close()
                }
                DETECT_FACE_SHAPE -> {
                    val faceShapeModel = FaceShapeModel1000d.newInstance(context)
                    classifications.addAll(FaceShapeClassifierProcessor.extractFaceShapeClassifications(classificationTracker.merge(faceShapeModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    faceShapeModel.close()
                }
                DETECT_EYE_COLOR -> {
                    val model = EyeColorModel3.newInstance(context)
                    classifications.addAll(EyeColorClassifierProcessor.extractEyeColorClassification(classificationTracker.merge(model.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    model.close()
                }
                DETECT_HAIR_COLOR -> {
                    val model = HairColorModel6.newInstance(context)
                    classifications.addAll(HairColorClassifierProcessor.extractHairColorClassification(classificationTracker.merge(model.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    model.close()
                }
                DETECT_FEATURES -> {
                    val featuresModel = FeaturesFaceModel3.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(featuresModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.filter{it.label != "Clear"}.filter { it.score >= 0.1 }))
                    featuresModel.close()
                }
                DETECT_CHARACTER -> {
                    val characterModel = CharacterModel2.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(characterModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.filter { it.score >= 0.01 }))
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
        val timeOutSeconds = if (currentClassifier == DETECT_FACE_SHAPE || currentClassifier == DETECT_HAIR_COLOR) 6 else 4

        classificationTracker = ClassificationTracker(timeOutSeconds, currentClassifier)
    }

    companion object {
        const val DETECT_AGE = "Age"
        const val DETECT_EMOTIONS = "Emotions"
        const val DETECT_GENDER = "Gender"
        const val DETECT_FACE_SHAPE = "Face Shape"
        const val DETECT_FEATURES = "Physical Features"
        const val DETECT_EYE_COLOR = "Eye Color"
        const val DETECT_HAIR_COLOR = "Hair Color"
        const val DETECT_CHARACTER = "Character"
        const val DETECT_ANCESTRY = "Ancestry"
//        @get:Synchronized @set:Synchronized
        var classifier = DETECT_AGE
    }
}