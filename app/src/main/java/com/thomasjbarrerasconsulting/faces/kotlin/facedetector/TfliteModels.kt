package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.content.Context
import org.tensorflow.lite.support.model.Model

/**
 * Keeps one instance of each TFLite model for the life of the app instead of creating and
 * closing a model on every classification.
 *
 * Model instances are not thread safe: callers must hold [lock] while creating or running them.
 */
object TfliteModels {
    val lock = Any()

    // Measured on a Galaxy A54 (4 big + 4 little cores): 4 threads gave the best best-case but
    // 2x worse p90 latency with 150-300 ms spikes, as threads competed with ML Kit and the camera.
    private const val NUM_THREADS = 2

    private val options: Model.Options = Model.Options.Builder()
        .setDevice(Model.Device.CPU)
        .setNumThreads(NUM_THREADS)
        .build()

    private val models = HashMap<Class<*>, Any>()

    fun <T : Any> get(context: Context, modelClass: Class<T>, create: (Context, Model.Options) -> T): T {
        synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            return models.getOrPut(modelClass) { create(context.applicationContext, options) } as T
        }
    }
}
