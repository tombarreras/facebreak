package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.thomasjbarrerasconsulting.faces.BitmapUtils
import com.thomasjbarrerasconsulting.faces.FrameMetadata
import com.google.mlkit.vision.face.Face
import com.thomasjbarrerasconsulting.faces.kotlin.LivePreviewActivity
import com.thomasjbarrerasconsulting.faces.ml.AgesModel5000
import com.thomasjbarrerasconsulting.faces.ml.EmotionsModel
import com.thomasjbarrerasconsulting.faces.ml.GenderModel9000
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import java.text.NumberFormat

class FaceClassifierProcessor(private val context: Context) {
    fun getFaceClassifications(face: Face, image: InputImage): FaceWithClassifications {

        if (image.byteBuffer == null){
            return FaceWithClassifications(face, mutableListOf())
        }

        val bitmap: Bitmap =
            BitmapUtils.getBitmap(image.byteBuffer, FrameMetadata.Builder().setHeight(image.height).setWidth(image.width).setRotation(image.rotationDegrees).build())
                ?: return FaceWithClassifications(face, mutableListOf())

        val croppedBitmap = BitmapCropper.cropBitmap(bitmap, face.boundingBox)

        try {
            val classifications: MutableList<String> = mutableListOf()
            val tensorImage = TensorImage.fromBitmap(croppedBitmap)

            when (classifier) {
                DETECT_AGE -> {
                    val genderModel = AgesModel5000.newInstance(context)
                    classifications.addAll(extractClassifications(genderModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(3)))
                    genderModel.close()
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
            }

//            val emotionsModel = EmotionsModel.newInstance(context)
//            classifications.addAll(extractClassifications(emotionsModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(1)))
//            emotionsModel.close()
//
//            val agesModel = AgesModel.newInstance(context)
//            classifications.addAll(extractClassifications(agesModel.process(tensorImage).probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(1)))
//            agesModel.close()

            return FaceWithClassifications(face, classifications)
        }
        finally{
            bitmap.recycle()
            croppedBitmap.recycle()
        }
    }

    private fun extractClassifications(outputs: List<Category>): MutableList<String> {
        val classifications: MutableList<String> = mutableListOf()
        val percentFormat: NumberFormat = NumberFormat.getPercentInstance()
        for (output in outputs) {
            val score: String = percentFormat.format(output.score)
            val label: String = output.label
            classifications.add("$label ($score)")
        }
        return classifications
    }

    companion object {
        const val DETECT_AGE = "Detect Age"
        const val DETECT_EMOTIONS = "Detect Emotions"
        const val DETECT_GENDER = "Detect Gender"
        @get:Synchronized @set:Synchronized
        var classifier = DETECT_AGE
    }
}