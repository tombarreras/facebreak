package com.thomasjbarrerasconsulting.faces.kotlin

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.preference.UserPreferences
import java.util.*

class Ads {
    companion object{
        fun initialize(context: Context, adView: AdView, premiumStatusImageView: View, settingsImageView: View, parentLayout: ConstraintLayout) {

            // Comment out in production
            setTestIdsIfDebugging()

            adView.addOnLayoutChangeListener (AdLayoutChangedListener(premiumStatusImageView, settingsImageView, parentLayout))
            MobileAds.initialize(context) {}
            loadAds(context, adView)
        }

        private fun setTestIdsIfDebugging() {
            val testDeviceIds = listOf("7223D4C474A5F2F45B8F904C5974A412")
            val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
            MobileAds.setRequestConfiguration(configuration)
        }

        fun loadAds(context: Context, adView: AdView) {
            val personalizeAdsEnabled = UserPreferences.getUserPreferences(context).enablePersonalizedAds

            val extras = Bundle()
            extras.putString("npa", if (personalizeAdsEnabled) "0" else "1")
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()
            adView.loadAd(adRequest)
        }

        fun removeAds(adView: AdView){
            adView.isEnabled = false
            adView.visibility = View.GONE
        }
    }
}