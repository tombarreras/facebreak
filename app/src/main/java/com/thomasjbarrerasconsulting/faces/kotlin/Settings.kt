package com.thomasjbarrerasconsulting.faces.kotlin

import android.net.Uri
import com.thomasjbarrerasconsulting.faces.CameraSource

class Settings {
    companion object {
        var cameraFacing: Int = CameraSource.CAMERA_FACING_FRONT
        var selectedClassifier:Int = 0
    }
}