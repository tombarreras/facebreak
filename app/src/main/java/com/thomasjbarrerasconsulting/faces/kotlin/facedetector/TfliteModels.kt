package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.metadata.MetadataExtractor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One bundled classification model, run directly on a TFLite [Interpreter]. */
class FaceModel internal constructor(val name: String, private val interpreter: Interpreter, val labels: List<String>) {
    private val outputScale: Float
    private val outputZeroPoint: Int
    private val output: ByteBuffer

    init {
        val outputTensor = interpreter.getOutputTensor(0)
        check(outputTensor.dataType() == DataType.UINT8) { "$name: expected uint8 output, got ${outputTensor.dataType()}" }
        check(outputTensor.numElements() == labels.size) { "$name: ${outputTensor.numElements()} outputs but ${labels.size} labels" }
        outputScale = outputTensor.quantizationParams().scale
        outputZeroPoint = outputTensor.quantizationParams().zeroPoint
        output = ByteBuffer.allocateDirect(outputTensor.numBytes()).order(ByteOrder.nativeOrder())
    }

    /** Category scores in label order, the same as the generated classes' probabilityAsCategoryList. */
    fun classify(input: ModelInput): MutableList<Category> {
        input.buffer.rewind()
        output.rewind()
        interpreter.run(input.buffer, output)
        output.rewind()
        return MutableList(labels.size) { i ->
            Category(labels[i], ((output.get(i).toInt() and 0xFF) - outputZeroPoint) * outputScale)
        }
    }

    fun close() {
        interpreter.close()
    }
}

/**
 * Keeps one instance of each TFLite model for the life of the app, running directly on the
 * TFLite interpreter rather than through the generated model classes, which re-run the same
 * image preprocessing for every model (~6 ms each). Results are bit-identical to the generated
 * classes (verified by ModelInputEquivalenceTest and ModelOutputDumpTest).
 *
 * XNNPACK stays off: on newer TFLite versions it is the default, and for these 8-bit models it
 * ignores the thread count and is slower.
 *
 * Model instances are not thread safe: callers must hold [lock] while creating or running them.
 */
object TfliteModels {
    val lock = Any()

    // Measured on a Galaxy A54 (ModelOutputDumpTest, input + classify per model): 16.8 ms with
    // 1 thread, 10.2 ms with 2, 7.3 ms with 4. 4 threads caused 150-300 ms spikes in the live
    // camera preview, where the models compete with ML Kit and the camera for CPU cores.
    private const val NUM_THREADS = 2

    private val models = HashMap<String, FaceModel>()

    fun get(context: Context, name: String): FaceModel {
        synchronized(lock) {
            return models.getOrPut(name) { load(context.applicationContext, name, NUM_THREADS) }
        }
    }

    /** Loads `<name>.tflite` from the app's assets. Visible for on-device tests. */
    fun load(context: Context, name: String, numThreads: Int): FaceModel {
        val modelBuffer = FileUtil.loadMappedFile(context, "$name.tflite")
        val labels = FileUtil.loadLabels(MetadataExtractor(modelBuffer).getAssociatedFile("labels.txt"))
        val options = Interpreter.Options()
            .setNumThreads(numThreads)
            .setUseXNNPACK(false)
        return FaceModel(name, Interpreter(modelBuffer, options), labels)
    }

    const val AGE = "AgeModel5"
    const val ANCESTRY = "AncestryModel9"
    const val CHARACTER = "CharacterModel4"
    const val CHARACTER_FLAWS = "CharacterFlawsModel3"
    const val EMOTIONS = "EmotionsModel1600"
    const val EYEBROWS = "EyebrowsModel"
    const val EYE_COLOR = "EyeColorModel3"
    const val FACE_FEATURES = "FeaturesFaceModel5"
    const val FACE_SHAPE = "FaceShapeModel1000d"
    const val GENDER = "GenderModel2"
    const val HAIR_COLOR = "HairColorModel8"
    const val HAIR_STYLE = "HairStyleModel4"
    const val HEADWEAR = "HeadwearModel3"
    const val JAW = "JawModel5"

    val ALL = listOf(AGE, ANCESTRY, CHARACTER, CHARACTER_FLAWS, EMOTIONS, EYEBROWS, EYE_COLOR,
        FACE_FEATURES, FACE_SHAPE, GENDER, HAIR_COLOR, HAIR_STYLE, HEADWEAR, JAW)
}
