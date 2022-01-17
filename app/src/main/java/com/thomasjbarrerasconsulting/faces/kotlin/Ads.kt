package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.thomasjbarrerasconsulting.faces.R

class Ads {
    companion object{
        fun initialize(context: Context, adView: AdView) {
            MobileAds.initialize(context) {}
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        }
    }
}