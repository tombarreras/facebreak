package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import android.view.View
import android.widget.AdapterView
import com.google.firebase.analytics.FirebaseAnalytics

class StillImageActivityClassifierSelectedListener(private val stillImageActivity:StillImageActivity, context: Context, firebaseAnalytics: FirebaseAnalytics) :
    ClassifierSelectedListener(context, firebaseAnalytics, {}) {
    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        pos: Int,
        id: Long
    ){
        super.onItemSelected(parent, view, pos, id)

        stillImageActivity.createImageProcessor()
        stillImageActivity.classifyDisplayedImage()
    }
}