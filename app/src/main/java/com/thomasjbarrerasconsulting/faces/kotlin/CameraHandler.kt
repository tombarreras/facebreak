package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import android.content.pm.PackageManager
import com.thomasjbarrerasconsulting.faces.kotlin.PermissionsHandler.Companion.CAMERA_PERMISSION

class CameraHandler {
    companion object {
        fun cameraExists(context: Context): Boolean{
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        }

        fun frontFacingCameraExists(context: Context): Boolean{
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        }

        fun cameraPermissionExists(context: Context, permissionsHandler: PermissionsHandler): Boolean{
            return permissionsHandler.isPermissionGranted(context, CAMERA_PERMISSION)
        }

        fun cameraAndPermissionExists(context: Context, permissionsHandler: PermissionsHandler): Boolean{
            return cameraExists(context) && cameraPermissionExists(context, permissionsHandler)
        }
    }
}