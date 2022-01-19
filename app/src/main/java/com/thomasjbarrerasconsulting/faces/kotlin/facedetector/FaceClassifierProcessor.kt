/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.ml.*
import com.thomasjbarrerasconsulting.faces.preference.UserPreferences
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class FaceClassifierProcessor(private val context: Context) {
    private var classificationTracker = ClassificationTracker(UserPreferences.getUserPreferences(context).averagingSeconds, Classifier.DETECT_AGE)

    fun getFaceClassifications(face: Face, bitmap: Bitmap?): FaceWithClassifications {
        val currentClassifier = classifier
        if (bitmap == null){
            return FaceWithClassifications(face, mutableListOf(), currentClassifier)
        }

        val croppedBitmap = BitmapCropper.cropBitmap(bitmap, face.boundingBox, currentClassifier)

        try {
            val classifications: MutableList<String> = mutableListOf()
            val tensorImage = TensorImage.fromBitmap(croppedBitmap)

            if (currentClassifier != classificationTracker.classifier){
                resetClassificationTracker(currentClassifier)
            }

            when (currentClassifier) {
                Classifier.DETECT_AGE -> {
                    val ageModel = AgeModel5.newInstance(context)
                    classifications.addAll(AgeClassifierProcessor.extractAgeClassification(classificationTracker.merge(ageModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    ageModel.close()
                }
                Classifier.DETECT_EMOTIONS -> {
                    val emotionsModel = EmotionsModel1600.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(emotionsModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(2))))
                    emotionsModel.close()
                }
                Classifier.DETECT_GENDER -> {
                    val genderModel = GenderModel2.newInstance(context)
                    classifications.addAll(extractClassifications(classificationTracker.merge(genderModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } })))
                    genderModel.close()
                }
                Classifier.DETECT_FACE_SHAPE -> {
                    val faceShapeModel = FaceShapeModel1000d.newInstance(context)
                    classifications.addAll(FaceShapeClassifierProcessor.extractFaceShapeClassifications(classificationTracker.merge(faceShapeModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    faceShapeModel.close()
                }
                Classifier.DETECT_EYE_COLOR -> {
                    val model = EyeColorModel3.newInstance(context)
                    classifications.addAll(EyeColorClassifierProcessor.extractEyeColorClassification(classificationTracker.merge(model.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    model.close()
                }
                Classifier.DETECT_HAIR_COLOR -> {
                    val model = HairColorModel8.newInstance(context)
                    classifications.addAll(HairColorClassifierProcessor.extractHairColorClassification(classificationTracker.merge(model.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    model.close()
                }
                Classifier.DETECT_HAIR_STYLE -> {
                    val model = HairStyleModel4.newInstance(context)
                    classifications.addAll(HairStyleClassifierProcessor.extractHairStyleClassification(classificationTracker.merge(model.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.filter {it.score >= 0.05}))
                    model.close()
                }
                Classifier.DETECT_FEATURES -> {
                    classifications.addAll(PhysicalFeatureClassifierProcessor.extractPhysicalFeatureClassifications(tensorImage, context, classificationTracker))
                }
                Classifier.DETECT_CHARACTER -> {
                    val characterModel = CharacterModel4.newInstance(context)
                    classifications.addAll(CharacterClassifierProcessor.extractCharacterClassification(classificationTracker.merge(characterModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.filter { it.score >= 0.01 }))
                    characterModel.close()
                }
                Classifier.DETECT_CHARACTER_FLAWS -> {
                    val characterModel = CharacterFlawsModel3.newInstance(context)
                    classifications.addAll(CharacterFlawsClassifierProcessor.extractCharacterFlawsClassification(classificationTracker.merge(characterModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.filter { it.score >= 0.01 }))
                    characterModel.close()
                }
                Classifier.DETECT_ANCESTRY -> {
                    val ancestryModel = AncestryModel9.newInstance(context)
                    classifications.addAll(AncestryClassifierProcessor.extractAncestryClassifications(classificationTracker.merge(ancestryModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                    ancestryModel.close()
                }
            }

            return FaceWithClassifications(face, classifications, currentClassifier)
        }
        finally{
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

    fun resetClassificationTracker(currentClassifier: Classifier) {
        classificationTracker = ClassificationTracker(UserPreferences.getUserPreferences(context).averagingSeconds, currentClassifier)
    }

    enum class Classifier {
        DETECT_ANCESTRY,
        DETECT_AGE,
        DETECT_CHARACTER,
        DETECT_CHARACTER_FLAWS,
        DETECT_EMOTIONS,
        DETECT_EYE_COLOR,
        DETECT_FACE_SHAPE,
        DETECT_GENDER,
        DETECT_HAIR_COLOR,
        DETECT_HAIR_STYLE,
        DETECT_FEATURES
    }

    companion object {
        @get:Synchronized @set:Synchronized
        var classifier = Classifier.DETECT_AGE

        private lateinit var allClassificationDescriptionsTranslated:List<String>
        private lateinit var allClassificationDescriptionsFreeTranslated:List<String>

        fun allClassificationDescriptions(context: Context):List<String>{
            if (! ::allClassificationDescriptionsTranslated.isInitialized ){
                allClassificationDescriptionsTranslated = listOf(
                    context.getString(R.string.classifier_detect_ancestry),
                    context.getString(R.string.classifier_detect_age),
                    context.getString(R.string.classifier_character),
                    context.getString(R.string.classifier_character_flaws),
                    context.getString(R.string.classifier_emotions),
                    context.getString(R.string.classifier_eye_color),
                    context.getString(R.string.classifier_face_shape),
                    context.getString(R.string.classifier_gender),
                    context.getString(R.string.classifier_hair_color),
                    context.getString(R.string.classifier_hair_style),
                    context.getString(R.string.classifier_physical_features)
                )
            }
            return allClassificationDescriptionsTranslated
        }
        fun allClassificationDescriptionsFree(context: Context):List<String>{
            if (! ::allClassificationDescriptionsFreeTranslated.isInitialized ){
                allClassificationDescriptionsFreeTranslated = listOf(
                    "* " + context.getString(R.string.classifier_detect_ancestry),
                    context.getString(R.string.classifier_detect_age),
                    context.getString(R.string.classifier_character),
                    "* " + context.getString(R.string.classifier_character_flaws),
                    context.getString(R.string.classifier_emotions),
                    context.getString(R.string.classifier_eye_color),
                    context.getString(R.string.classifier_face_shape),
                    context.getString(R.string.classifier_gender),
                    context.getString(R.string.classifier_hair_color),
                    context.getString(R.string.classifier_hair_style),
                    context.getString(R.string.classifier_physical_features)
                )
            }
            return allClassificationDescriptionsFreeTranslated
        }

        private val allClassificationDescriptionsEnglish = listOf(
            "Ancestry",
            "Age",
            "Character",
            "Character Flaws",
            "Emotions",
            "Eye Color",
            "Face Shape",
            "Gender",
            "Hair Color",
            "Hair Style",
            "Physical Features"
        )

        fun isPremiumClassifier(position: Int): Boolean{
            return position == 0 || position == 3
        }

        fun classifierDescriptionEnglish(classifier:Classifier):String{
            return allClassificationDescriptionsEnglish[Classifier.values().indexOf(classifier)]
        }

        fun classifierDescription(context:Context, position:Int):String{
            return allClassificationDescriptions(context)[position]
        }
    }

}