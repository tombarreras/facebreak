package com.thomasjbarrerasconsulting.faces.kotlin

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AdapterView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.thomasjbarrerasconsulting.faces.kotlin.billing.Premium
import com.thomasjbarrerasconsulting.faces.kotlin.facedetector.FaceClassifierProcessor

open class ClassifierSelectedListener(private val context: Activity, private val firebaseAnalytics: FirebaseAnalytics, private val premiumAction: () -> Unit, private val nonPremiumAction: () -> Unit): AdapterView.OnItemSelectedListener {
    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        pos: Int,
        id: Long
    ) {
        if (FaceClassifierProcessor.isPremiumClassifier(pos) && !Premium.premiumIsActive()){
            premiumAction()
        } else {
            val selectedClassifier = FaceClassifierProcessor.Classifier.values()[pos]
            Settings.selectedClassifier = selectedClassifier
            FaceClassifierProcessor.classifier = selectedClassifier
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_LIST_NAME, FaceClassifierProcessor.classifierDescription(context, pos))
            }

            nonPremiumAction()
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