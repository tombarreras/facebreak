/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin

import android.app.Activity
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.ump.*
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.preference.UserPreferences

class Privacy {
    companion object{
        private const val TAG = "Privacy"
        private const val STORE_AND_OR_ACCESS_INFORMATION_ON_A_DEVICE = 1
        private const val SELECT_BASIC_ADS = 2
        private const val CREATE_A_PERSONALIZED_ADS_PROFILE = 3
        private const val SELECT_PERSONALIZED_ADS = 4
        private const val CREATE_A_PERSONALIZED_CONTENT_PROFILE = 5
        private const val SELECT_PERSONALIZED_CONTENT = 6
        private const val MEASURE_AD_PERFORMANCE = 7
        private const val MEASURE_CONTENT_PERFORMANCE = 8
        private const val APPLY_MARKET_RESEARCH_TO_GENERATE_AUDIENCE_INSIGHTS = 9
        private const val DEVELOP_AND_IMPROVE_PRODUCTS = 10
        private const val GOOGLE_VENDOR_ID = 755
        
        private lateinit var consentInformation: ConsentInformation
        private lateinit var consentForm: ConsentForm

        @Suppress("UNUSED_PARAMETER")
        private fun consentRequestParameters(activity: Activity): ConsentRequestParameters? {
            // For debugging
//            consentInformation.reset()
//            val debugSettings = ConsentDebugSettings.Builder(activity)
//                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
//                .addTestDeviceHashedId("7223D4C474A5F2F45B8F904C5974A412")
//                .build()
//
//            return ConsentRequestParameters.Builder()
//                .setConsentDebugSettings(debugSettings)
//                .setTagForUnderAgeOfConsent(false)
//                .build()

            // For production
            return ConsentRequestParameters.Builder()
              .setTagForUnderAgeOfConsent(false)
              .build()
        }

        private fun loadConsentForm(activity: Activity, onConsentObtained: () -> Unit) {
            UserMessagingPlatform.loadConsentForm(
                activity,
                { consentForm ->
                    this.consentForm = consentForm
                    if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                        consentForm.show(
                            activity
                        ) { // Handle dismissal by reloading form.
                            loadConsentForm(activity, onConsentObtained)
                        }
                    }
                    if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED){

                        UserPreferences.setUserPreferenceBoolean(activity,
                            UserPreferences.PREFERENCE_KEY_ENABLE_ANALYTICS, canCollectAnalytics())
                        UserPreferences.setUserPreferenceBoolean(activity,
                            UserPreferences.PREFERENCE_KEY_ENABLE_PERSONALIZED_ADS, canShowPersonalizedAds())

                        onConsentObtained()
                    }
                }
            ) {
                /// Handle Error.
                Log.e(TAG,  activity.getString(R.string.failed_to_load_consent_form) + " ${it.errorCode} ${it.message}")
            }
        }
        
        fun obtainConsent(activity: Activity, onConsentObtained: () -> Unit) {
            consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            // Set tag for underage of consent. false means users are not underage.
            val params = consentRequestParameters(activity)

            if (params != null) {
                consentInformation.requestConsentInfoUpdate(
                    activity,
                    params,
                    {
                        // The consent information state was updated.
                        // You are now ready to check if a form is available.
                        if (consentInformation.isConsentFormAvailable) {
                            loadConsentForm(activity, onConsentObtained)
                        }
                    },
                    {
                        Log.e(TAG,activity.getString(R.string.failed_to_obtain_consent) + " ${it.errorCode} ${it.message}")
                    })
            }
        }
        
        fun isGdpr(): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(FaceBreakApplication.instance)
            val gdpr = prefs.getInt("IABTCF_gdprApplies", 0)
            return gdpr == 1
        }

        // https://stackoverflow.com/questions/69307205/mandatory-consent-for-admob-user-messaging-platform
        private fun canShowPersonalizedAds(): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(FaceBreakApplication.instance)

            //https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#in-app-details
            //https://support.google.com/admob/answer/9760862?hl=en&ref_topic=9756841

            val purposeConsent = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
            val vendorConsent = prefs.getString("IABTCF_VendorConsents","") ?: ""
            val vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests","") ?: ""
            val purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests","") ?: ""

            val hasGoogleVendorConsent = hasAttribute(vendorConsent, index= GOOGLE_VENDOR_ID)
            val hasGoogleVendorLI = hasAttribute(vendorLI, index= GOOGLE_VENDOR_ID)

            return hasConsentFor(listOf(
                STORE_AND_OR_ACCESS_INFORMATION_ON_A_DEVICE,
                CREATE_A_PERSONALIZED_ADS_PROFILE,
                SELECT_PERSONALIZED_ADS), purposeConsent, hasGoogleVendorConsent)
                    && hasConsentOrLegitimateInterestFor(listOf(
                SELECT_BASIC_ADS,
                MEASURE_AD_PERFORMANCE,
                APPLY_MARKET_RESEARCH_TO_GENERATE_AUDIENCE_INSIGHTS,
                DEVELOP_AND_IMPROVE_PRODUCTS), purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI)
        }

        private fun canCollectAnalytics(): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(FaceBreakApplication.instance)
            val purposeConsent = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
            val vendorConsent = prefs.getString("IABTCF_VendorConsents","") ?: ""
            val hasGoogleVendorConsent = hasAttribute(vendorConsent, index= GOOGLE_VENDOR_ID)

            return hasConsentFor(listOf(
                STORE_AND_OR_ACCESS_INFORMATION_ON_A_DEVICE,
                CREATE_A_PERSONALIZED_CONTENT_PROFILE,
                MEASURE_AD_PERFORMANCE,
                MEASURE_CONTENT_PERFORMANCE,
                APPLY_MARKET_RESEARCH_TO_GENERATE_AUDIENCE_INSIGHTS,
                DEVELOP_AND_IMPROVE_PRODUCTS), purposeConsent, hasGoogleVendorConsent)
        }

        // Check if a binary string has a "1" at position "index" (1-based)
        private fun hasAttribute(input: String, index: Int): Boolean {
            return input.length >= index && input[index-1] == '1'
        }

        // Check if consent is given for a list of purposes
        private fun hasConsentFor(purposes: List<Int>, purposeConsent: String, hasVendorConsent: Boolean): Boolean {
            return purposes.all { p -> hasAttribute(purposeConsent, p) } && hasVendorConsent
        }

        // Check if a vendor either has consent or legitimate interest for a list of purposes
        private fun hasConsentOrLegitimateInterestFor(purposes: List<Int>, purposeConsent: String, purposeLI: String, hasVendorConsent: Boolean, hasVendorLI: Boolean): Boolean {
            return purposes.all { p ->
                (hasAttribute(purposeLI, p) && hasVendorLI) ||
                        (hasAttribute(purposeConsent, p) && hasVendorConsent)
            }
        }
    }
}