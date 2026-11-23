/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.FaceBreakApplication
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.ClassifierText.Companion.get
import com.thomasjbarrerasconsulting.faces.ml.*
import com.thomasjbarrerasconsulting.faces.preference.UserPreferences
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class FaceClassifierProcessor(private val context: Context, private val processingDelayMilliseconds: Long) {
    private var classificationTracker = ClassificationTracker(UserPreferences.getUserPreferences(context).averagingSeconds, Classifier.DETECT_AGE)

    fun getFaceClassifications(face: Face, bitmap: Bitmap?): FaceWithClassifications {
        val currentClassifier = classifier
        if (bitmap == null){
            return FaceWithClassifications(face, mutableListOf(), currentClassifier)
        }

        val croppedBitmap = BitmapCropper.cropBitmap(bitmap, face.boundingBox, currentClassifier)

        if (nextProcessTime > System.currentTimeMillis()){
            return FaceWithClassifications(face, currentClassifications, currentClassifier)
        }

        try { synchronized(TfliteModels.lock) {
            val classifications: MutableList<String> = mutableListOf()
            val tensorImage = TensorImage.fromBitmap(croppedBitmap)
            val genderClassifier = GenderClassifier(context, tensorImage)

            if (currentClassifier != classificationTracker.classifier){
                resetClassificationTracker(currentClassifier)
            }

            when (currentClassifier) {
                Classifier.DETECT_AGE -> {
                    val ageModel = TfliteModels.get(context, AgeModel5::class.java, AgeModel5::newInstance)
                    classifications.addAll(AgeClassifierProcessor.extractAgeClassification(classificationTracker.merge(ageModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                }
                Classifier.DETECT_EMOTIONS -> {
                    val emotionsModel = TfliteModels.get(context, EmotionsModel1600::class.java, EmotionsModel1600::newInstance)
                    classifications.addAll(extractClassifications(classificationTracker.merge(emotionsModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(2)), genderClassifier))
                }
                Classifier.DETECT_GENDER -> {
                    val genderModel = TfliteModels.get(context, GenderModel2::class.java, GenderModel2::newInstance)
                    classifications.addAll(extractClassifications(classificationTracker.merge(genderModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }), genderClassifier))
                }
                Classifier.DETECT_FACE_SHAPE -> {
                    val faceShapeModel = TfliteModels.get(context, FaceShapeModel1000d::class.java, FaceShapeModel1000d::newInstance)
                    classifications.addAll(FaceShapeClassifierProcessor.extractFaceShapeClassifications(classificationTracker.merge(faceShapeModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }, genderClassifier))
                }
                Classifier.DETECT_EYE_COLOR -> {
                    val model = TfliteModels.get(context, EyeColorModel3::class.java, EyeColorModel3::newInstance)
                    classifications.addAll(EyeColorClassifierProcessor.extractEyeColorClassification(classificationTracker.merge(model.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }, genderClassifier))
                }
                Classifier.DETECT_HAIR_COLOR -> {
                    val model = TfliteModels.get(context, HairColorModel8::class.java, HairColorModel8::newInstance)
                    classifications.addAll(HairColorClassifierProcessor.extractHairColorClassification(classificationTracker.merge(model.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                }
                Classifier.DETECT_HAIR_STYLE -> {
                    val model = TfliteModels.get(context, HairStyleModel4::class.java, HairStyleModel4::newInstance)
                    classifications.addAll(HairStyleClassifierProcessor.extractHairStyleClassification(classificationTracker.merge(model.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.filter {it.score >= 0.05}))
                }
                Classifier.DETECT_FEATURES -> {
                    classifications.addAll(PhysicalFeatureClassifierProcessor.extractPhysicalFeatureClassifications(tensorImage, context, classificationTracker))
                }
                Classifier.DETECT_CHARACTER -> {
                    val characterModel = TfliteModels.get(context, CharacterModel4::class.java, CharacterModel4::newInstance)
                    classifications.addAll(CharacterClassifierProcessor.extractCharacterClassification(classificationTracker.merge(characterModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.filter { it.score >= 0.01 }, genderClassifier))
                }
                Classifier.DETECT_CHARACTER_FLAWS -> {
                    val characterModel = TfliteModels.get(context, CharacterFlawsModel3::class.java, CharacterFlawsModel3::newInstance)
                    classifications.addAll(CharacterFlawsClassifierProcessor.extractCharacterFlawsClassification(classificationTracker.merge(characterModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }.filter { it.score >= 0.01 }, genderClassifier))
                }
                Classifier.DETECT_ANCESTRY -> {
                    val ancestryModel = TfliteModels.get(context, AncestryModel9::class.java, AncestryModel9::newInstance)
                    classifications.addAll(AncestryClassifierProcessor.extractAncestryClassifications(classificationTracker.merge(ancestryModel.process(tensorImage).probabilityAsCategoryList).apply { sortByDescending { it.score } }))
                }
            }

            currentClassifications = classifications.toMutableList()
            nextProcessTime = System.currentTimeMillis() + processingDelayMilliseconds
            return FaceWithClassifications(face, classifications, currentClassifier)
        } }
        finally{
            croppedBitmap.recycle()
        }
    }

    fun resetClassificationTracker() {
        classificationTracker = ClassificationTracker(UserPreferences.getUserPreferences(context).averagingSeconds, classifier)
    }

    fun resetClassificationTracker(classifier: Classifier) {
        classificationTracker = ClassificationTracker(UserPreferences.getUserPreferences(context).averagingSeconds, classifier)
    }

    private fun extractClassifications(outputs: List<Category?>, genderClassifier: GenderClassifier? = null): MutableList<String> {
        val classifications: MutableList<String> = mutableListOf()
        val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
        val totalScore = outputs.map { it?.score!! }.sum()

        for (output in outputs) {
            val score: String = percentFormat.format(output?.score!!.div(totalScore))
            val label: String = get(output.label ?: context.getString(R.string.classification_unknown), genderClassifier)
            classifications.add("$label ($score)")
        }
        return classifications
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
        var nextProcessTime = System.currentTimeMillis()

        @get:Synchronized @set:Synchronized
        var classifier = Classifier.DETECT_AGE

        @get:Synchronized @set:Synchronized
        var currentClassifications: MutableList<String> = mutableListOf()

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