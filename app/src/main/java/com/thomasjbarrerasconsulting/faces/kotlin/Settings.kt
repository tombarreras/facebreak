package com.thomasjbarrerasconsulting.faces.kotlin

import android.net.Uri
import com.thomasjbarrerasconsulting.faces.CameraSource

class Settings {
    companion object {
        private const val DEFAULT_STILL_IMAGE_X = 0.0f
        private const val DEFAULT_STILL_IMAGE_Y = 0.0f
        private const val DEFAULT_STILL_IMAGE_SCALE_FACTOR = 1.0f

        var stillImageExists:Boolean = false
            set(value){
                field = value
                if (!value) {
                    stillImageX = DEFAULT_STILL_IMAGE_X
                    stillImageY = DEFAULT_STILL_IMAGE_Y
                    stillImageScaleFactor = DEFAULT_STILL_IMAGE_SCALE_FACTOR
                }
            }
        var stillImageScaleFactor:Float = DEFAULT_STILL_IMAGE_SCALE_FACTOR
        var stillImageX:Float = DEFAULT_STILL_IMAGE_X
        var stillImageY:Float = DEFAULT_STILL_IMAGE_Y

        var cameraFacing: Int = CameraSource.CAMERA_FACING_FRONT
        var selectedClassifier:Int = 0
    }
}