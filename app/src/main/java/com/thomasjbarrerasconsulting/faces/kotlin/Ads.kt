package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.thomasjbarrerasconsulting.faces.preference.UserPreferences

class Ads {
    companion object{
        fun initialize(context: Context, adView: AdView) {
            MobileAds.initialize(context) {}
            loadAds(context, adView)
        }

        fun loadAds(context: Context, adView: AdView) {
            val personalizeAdsEnabled = UserPreferences.getUserPreferences(context).enablePersonalizedAds

            val extras = Bundle()
            extras.putString("npa", if (personalizeAdsEnabled) "1" else "0")
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()

            adView.loadAd(adRequest)
        }
    }
}