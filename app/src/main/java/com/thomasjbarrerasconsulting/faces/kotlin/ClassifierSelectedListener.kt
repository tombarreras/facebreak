package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AdapterView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceClassifierProcessor

open class ClassifierSelectedListener(private val context: Context, private val firebaseAnalytics: FirebaseAnalytics): AdapterView.OnItemSelectedListener {
    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        pos: Int,
        id: Long
    ) {
        val selectedClassifier = FaceClassifierProcessor.Classifier.values()[pos]
        Settings.selectedClassifier = selectedClassifier
        FaceClassifierProcessor.classifier = selectedClassifier
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_LIST_NAME, FaceClassifierProcessor.classifierDescription(context, pos))
        }

        Log.d(TAG, "Selected classifier: ${FaceClassifierProcessor.classifierDescription(context, pos)}")
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        // Nothing to do
    }

    companion object {
        private const val TAG = "ClassifierSelectedListr"
    }
}