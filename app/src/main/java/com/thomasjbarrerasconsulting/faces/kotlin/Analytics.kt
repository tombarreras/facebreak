package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.thomasjbarrerasconsulting.faces.preference.UserPreferences

class Analytics {

    companion object{
        fun setAnalyticsEnabled(context: Context, firebaseAnalytics: FirebaseAnalytics) {
            val enabled = UserPreferences.getUserPreferences(context).enableAnalytics
            firebaseAnalytics.setUserProperty(FirebaseAnalytics.UserProperty.ALLOW_AD_PERSONALIZATION_SIGNALS, if (enabled) "true" else "false");
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        }
    }

}