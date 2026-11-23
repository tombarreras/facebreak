package com.thomasjbarrerasconsulting.faces.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.ModelInput
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.TfliteModels
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.ops.QuantizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.Random

/**
 * Checks that [ModelInput] produces exactly the same model input bytes as the preprocessing in
 * the generated model classes (copied from e.g. AgeModel5.java), and that the app's runner
 * reads the same labels as the generated classes.
 */
@RunWith(AndroidJUnit4::class)
class ModelInputEquivalenceTest {

    private val generatedPreprocessing = ImageProcessor.Builder()
        .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
        .add(NormalizeOp(floatArrayOf(0.0f), floatArrayOf(255.0f)))
        .add(QuantizeOp(0f, 0.003921569f))
        .add(CastOp(DataType.UINT8))
        .build()

    private fun bytesOf(buffer: java.nio.ByteBuffer): ByteArray {
        buffer.rewind()
        return ByteArray(buffer.remaining()).also { buffer.get(it); buffer.rewind() }
    }

    private fun testBitmaps(): List<Pair<String, Bitmap>> {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val result = mutableListOf<Pair<String, Bitmap>>()
        for (file in testContext.assets.list("model-inputs")!!.sorted()) {
            val source = testContext.assets.open("model-inputs/$file").use { BitmapFactory.decodeStream(it) }
            result += file to source
            result += "$file:360" to Bitmap.createScaledBitmap(source, 360, 360, true)
            result += "$file:small" to Bitmap.createBitmap(source, 0, 0, 150, 150)
        }
        // Every channel value 0-255 appears in this one
        val random = Random(7)
        val noise = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        for (x in 0 until 224) for (y in 0 until 224) {
            noise.setPixel(x, y, Color.rgb((x + y) % 256, random.nextInt(256), (x * 7 + y * 3) % 256))
        }
        result += "noise" to noise
        return result
    }

    @Test
    fun modelInputMatchesGeneratedPreprocessing() {
        for ((name, bitmap) in testBitmaps()) {
            val expected = bytesOf(generatedPreprocessing.process(TensorImage.fromBitmap(bitmap)).buffer)
            val actual = bytesOf(ModelInput(bitmap).buffer)
            assertArrayEquals("model input bytes differ for $name", expected, actual)
        }
    }

    @Test
    fun runnerLabelsMatchGeneratedClasses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = TensorImage.fromBitmap(Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888))
        val generatedLabels = mapOf(
            TfliteModels.AGE to AgeModel5.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.ANCESTRY to AncestryModel9.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.CHARACTER to CharacterModel4.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.CHARACTER_FLAWS to CharacterFlawsModel3.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.EMOTIONS to EmotionsModel1600.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.EYEBROWS to EyebrowsModel.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.EYE_COLOR to EyeColorModel3.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.FACE_FEATURES to FeaturesFaceModel5.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.FACE_SHAPE to FaceShapeModel1000d.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.GENDER to GenderModel2.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.HAIR_COLOR to HairColorModel8.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.HAIR_STYLE to HairStyleModel4.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.HEADWEAR to HeadwearModel3.newInstance(context).process(image).probabilityAsCategoryList,
            TfliteModels.JAW to JawModel5.newInstance(context).process(image).probabilityAsCategoryList,
        ).mapValues { (_, categories) -> categories.map { it.label } }

        assertEquals(TfliteModels.ALL.toSet(), generatedLabels.keys)
        for (name in TfliteModels.ALL) {
            val model = TfliteModels.load(context, name, 1)
            assertEquals("labels differ for $name", generatedLabels[name], model.labels)
            model.close()
        }
    }
}
