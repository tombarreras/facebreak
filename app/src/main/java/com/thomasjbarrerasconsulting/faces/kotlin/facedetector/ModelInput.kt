package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The input tensor for the face classification models, built once per face and shared by every
 * model that classifies it.
 *
 * All the bundled models take the same [1, 224, 224, 3] uint8 RGB input. This reproduces the
 * preprocessing in the generated model classes exactly, so results are unchanged:
 *   ResizeOp(224, 224, NEAREST_NEIGHBOR) -> NormalizeOp(0, 255) -> QuantizeOp(0, 0.003921569) -> CastOp(UINT8)
 * Note the quantize scale is slightly larger than 1/255 and the cast truncates, so most channel
 * values come out one lower than the original pixel value. [CHANNEL_LUT] reproduces that with the
 * same float arithmetic.
 */
class ModelInput(bitmap: Bitmap) {
    val buffer: ByteBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3).order(ByteOrder.nativeOrder())

    init {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        // Fill a plain array and copy it in one go: per-byte puts into a direct buffer are slow
        val lut = CHANNEL_LUT
        val bytes = ByteArray(pixels.size * 3)
        var i = 0
        for (pixel in pixels) {
            bytes[i++] = lut[(pixel shr 16) and 0xFF]
            bytes[i++] = lut[(pixel shr 8) and 0xFF]
            bytes[i++] = lut[pixel and 0xFF]
        }
        buffer.put(bytes)
        buffer.rewind()
    }

    companion object {
        const val INPUT_SIZE = 224
        private const val NORMALIZE_STDDEV = 255.0f
        private const val QUANTIZE_SCALE = 0.003921569f

        /** Channel value (0-255) -> model input byte, using the generated classes' float math. */
        internal val CHANNEL_LUT = ByteArray(256) { value ->
            val normalized = (value.toFloat() - 0f) / NORMALIZE_STDDEV
            val quantized = (normalized - 0f) / QUANTIZE_SCALE
            quantized.coerceIn(0f, 255f).toInt().toByte()
        }
    }
}
