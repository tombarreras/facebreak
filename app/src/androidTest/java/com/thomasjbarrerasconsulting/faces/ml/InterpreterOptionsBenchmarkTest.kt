package com.thomasjbarrerasconsulting.faces.ml

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random

/**
 * Times raw interpreter inference for one model under different thread counts, with the
 * XNNPACK CPU engine on and off. Writes files/interpreter-benchmark.json in app storage.
 */
@RunWith(AndroidJUnit4::class)
class InterpreterOptionsBenchmarkTest {

    @Test
    fun benchmarkInterpreterOptions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelBuffer = FileUtil.loadMappedFile(context, "AgeModel5.tflite")

        val input = ByteBuffer.allocateDirect(224 * 224 * 3).order(ByteOrder.nativeOrder())
        val random = Random(42)
        repeat(224 * 224 * 3) { input.put(random.nextInt(256).toByte()) }

        val results = JSONArray()
        for (xnnpack in listOf(false, true)) {
            for (threads in listOf(1, 2, 4)) {
                val options = Interpreter.Options().setNumThreads(threads).setUseXNNPACK(xnnpack)
                val interpreter = Interpreter(modelBuffer, options)
                try {
                    val output = ByteBuffer.allocateDirect(interpreter.getOutputTensor(0).numBytes()).order(ByteOrder.nativeOrder())
                    val run = {
                        input.rewind()
                        output.rewind()
                        interpreter.run(input, output)
                    }
                    repeat(10) { run() }
                    val times = (1..60).map {
                        val start = SystemClock.elapsedRealtimeNanos()
                        run()
                        (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                    }.sorted()
                    val result = JSONObject().put("xnnpack", xnnpack).put("threads", threads)
                        .put("p10", times[6]).put("p50", times[30]).put("p90", times[54])
                    results.put(result)
                    Log.i(TAG, result.toString())
                } finally {
                    interpreter.close()
                }
            }
        }

        val dump = JSONObject().put("runtimeVersion", TensorFlowLite.runtimeVersion()).put("results", results)
        File(context.filesDir, "interpreter-benchmark.json").writeText(dump.toString(1))
    }

    companion object {
        private const val TAG = "InterpreterBenchmark"
    }
}
