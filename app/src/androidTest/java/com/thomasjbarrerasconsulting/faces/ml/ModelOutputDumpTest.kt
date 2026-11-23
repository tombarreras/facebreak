package com.thomasjbarrerasconsulting.faces.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random

/**
 * Runs every bundled TFLite model on a fixed set of inputs and writes all output probabilities
 * plus inference timings to files/model-dump.json in the app's private storage.
 *
 * Used to verify that upgrading the TFLite/LiteRT runtime does not change model results:
 * run on the old and new runtime, pull both dumps, and compare.
 *
 *   adb shell am instrument -w -e class com.thomasjbarrerasconsulting.faces.ml.ModelOutputDumpTest \
 *       com.thomasjbarrerasconsulting.faces.test/androidx.test.runner.AndroidJUnitRunner
 *   adb exec-out run-as com.thomasjbarrerasconsulting.faces cat files/model-dump.json > dump.json
 */
@RunWith(AndroidJUnit4::class)
class ModelOutputDumpTest {

    private class ModelUnderTest(
        val name: String,
        val run: (TensorImage) -> List<Category>,
        val runRaw: (TensorBuffer) -> List<Category>,
        val close: () -> Unit
    )

    private val options: Model.Options = Model.Options.Builder()
        .setDevice(Model.Device.CPU)
        .setNumThreads(2) // Same as TfliteModels in the app
        .build()

    private fun models(context: Context): List<ModelUnderTest> = listOf(
        AgeModel5.newInstance(context, options).let { m -> ModelUnderTest("AgeModel5", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        AncestryModel9.newInstance(context, options).let { m -> ModelUnderTest("AncestryModel9", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        CharacterFlawsModel3.newInstance(context, options).let { m -> ModelUnderTest("CharacterFlawsModel3", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        CharacterModel4.newInstance(context, options).let { m -> ModelUnderTest("CharacterModel4", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        EmotionsModel1600.newInstance(context, options).let { m -> ModelUnderTest("EmotionsModel1600", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        EyeColorModel3.newInstance(context, options).let { m -> ModelUnderTest("EyeColorModel3", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        EyebrowsModel.newInstance(context, options).let { m -> ModelUnderTest("EyebrowsModel", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        FaceShapeModel1000d.newInstance(context, options).let { m -> ModelUnderTest("FaceShapeModel1000d", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        FeaturesFaceModel5.newInstance(context, options).let { m -> ModelUnderTest("FeaturesFaceModel5", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        GenderModel2.newInstance(context, options).let { m -> ModelUnderTest("GenderModel2", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        HairColorModel8.newInstance(context, options).let { m -> ModelUnderTest("HairColorModel8", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        HairStyleModel4.newInstance(context, options).let { m -> ModelUnderTest("HairStyleModel4", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        HeadwearModel3.newInstance(context, options).let { m -> ModelUnderTest("HeadwearModel3", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
        JawModel5.newInstance(context, options).let { m -> ModelUnderTest("JawModel5", { m.process(it).probabilityAsCategoryList }, { m.process(it).probabilityAsCategoryList }, m::close) },
    )

    /** Fixed, deterministic inputs: crops of the bundled screenshots plus seeded synthetic images. */
    private fun inputs(): List<Pair<String, Bitmap>> {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val result = mutableListOf<Pair<String, Bitmap>>()
        for (file in testContext.assets.list("model-inputs")!!.sorted()) {
            val source = testContext.assets.open("model-inputs/$file").use { BitmapFactory.decodeStream(it) }
            val side = minOf(source.width, source.height)
            result += "$file:full" to source
            result += "$file:center" to Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            result += "$file:top" to Bitmap.createBitmap(source, (source.width - side) / 2, 0, side, side)
            result += "$file:small" to Bitmap.createBitmap(source, source.width / 4, source.height / 4, side / 2, side / 2)
        }
        for (seed in 1L..3L) {
            val random = Random(seed)
            val noise = Bitmap.createBitmap(360, 360, Bitmap.Config.ARGB_8888)
            for (x in 0 until 360) for (y in 0 until 360) {
                noise.setPixel(x, y, Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
            }
            result += "noise-$seed" to noise
        }
        val gradient = Bitmap.createBitmap(360, 360, Bitmap.Config.ARGB_8888)
        for (x in 0 until 360) for (y in 0 until 360) {
            gradient.setPixel(x, y, Color.rgb(x * 255 / 359, y * 255 / 359, 128))
        }
        result += "gradient" to gradient
        return result
    }

    @Test
    fun dumpAllModelOutputs() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputs = inputs()
        val dump = JSONObject()
        dump.put("runtimeVersion", TensorFlowLite.runtimeVersion())
        dump.put("device", android.os.Build.MODEL)

        // Identical pre-built model inputs, bypassing the support library's image preprocessing,
        // so differences in these outputs come from the TFLite runtime alone.
        val rawInputs = inputs.map { (name, bitmap) -> "$name:raw" to rawInput(bitmap) }

        // What the app actually classifies: a face crop around 360x360
        val faceCrop = Bitmap.createScaledBitmap(inputs.first { it.first.endsWith(":top") }.second, 360, 360, true)

        val outputs = JSONArray()
        val timings = JSONObject()
        val inferenceTimings = JSONObject()
        for (model in models(context)) {
            try {
                for ((inputName, bitmap) in inputs) {
                    outputs.put(output(model.name, inputName, model.run(TensorImage.fromBitmap(bitmap))))
                }
                for ((inputName, buffer) in rawInputs) {
                    outputs.put(output(model.name, inputName, model.runRaw(buffer)))
                }

                // App path: preprocessing + inference on a 360x360 face crop
                timings.put(model.name, time { model.run(TensorImage.fromBitmap(faceCrop)) })
                // Inference only
                val raw = rawInputs.first().second
                inferenceTimings.put(model.name, time { model.runRaw(raw) })
                Log.i(TAG, "${model.name}: app p50=${timings.getJSONObject(model.name).getDouble("p50")} ms, " +
                        "inference p50=${inferenceTimings.getJSONObject(model.name).getDouble("p50")} ms")
            } finally {
                model.close()
            }
        }
        dump.put("outputs", outputs)
        dump.put("timingsMs", timings)
        dump.put("inferenceTimingsMs", inferenceTimings)

        File(context.filesDir, "model-dump.json").writeText(dump.toString(1))
        Log.i(TAG, "Wrote ${outputs.length()} outputs, runtime ${TensorFlowLite.runtimeVersion()}")
    }

    private fun output(model: String, input: String, categories: List<Category>): JSONObject {
        val scores = JSONObject()
        for (category in categories) scores.put(category.label, category.score.toDouble())
        return JSONObject().put("model", model).put("input", input).put("scores", scores)
    }

    /** Warm up, then time 40 runs. */
    private fun time(block: () -> Unit): JSONObject {
        repeat(5) { block() }
        val times = (1..40).map {
            val start = SystemClock.elapsedRealtimeNanos()
            block()
            (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
        }.sorted()
        return JSONObject().put("p10", times[4]).put("p50", times[20]).put("p90", times[36]).put("max", times.last())
    }

    /** A [1, 224, 224, 3] uint8 buffer: the same thing the models' own preprocessing produces. */
    private fun rawInput(bitmap: Bitmap): TensorBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        val bytes = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3).order(ByteOrder.nativeOrder())
        for (pixel in pixels) {
            bytes.put(Color.red(pixel).toByte())
            bytes.put(Color.green(pixel).toByte())
            bytes.put(Color.blue(pixel).toByte())
        }
        bytes.rewind()
        val buffer = TensorBuffer.createFixedSize(intArrayOf(1, INPUT_SIZE, INPUT_SIZE, 3), DataType.UINT8)
        buffer.loadBuffer(bytes)
        return buffer
    }

    companion object {
        private const val TAG = "ModelOutputDump"
        private const val INPUT_SIZE = 224
    }
}
