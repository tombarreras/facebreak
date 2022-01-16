package com.thomasjbarrerasconsulting.faces.kotlin

import android.os.Handler
import android.os.Looper
import android.widget.Toast

class Toaster {
    companion object {
        fun toast(message:String){
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    FaceBreakApplication.instance,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}