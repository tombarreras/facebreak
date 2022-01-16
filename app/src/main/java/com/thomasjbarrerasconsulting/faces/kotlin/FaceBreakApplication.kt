package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.multidex.MultiDexApplication

class FaceBreakApplication: MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FaceBreakApplication
    }
}
