/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
 */

package com.thomasjbarrerasconsulting.faces.preference

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.PremiumStatusActivity
import com.thomasjbarrerasconsulting.faces.kotlin.Privacy

class PreferencesFragment (private val activity: Activity) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        val gdprPreference = preferenceScreen.findPreference<GdprPrivacyDialogPreference>(UserPreferences.PREFERENCE_KEY_GDPR)
        val enableAnalyticsPreference = preferenceScreen.findPreference<CheckBoxPreference>(UserPreferences.PREFERENCE_KEY_ENABLE_ANALYTICS)
        val enablePersonalizedAdsPreference = preferenceScreen.findPreference<CheckBoxPreference>(UserPreferences.PREFERENCE_KEY_ENABLE_PERSONALIZED_ADS)

        if (Privacy.isGdpr()){
            enableAnalyticsPreference?.isVisible = false
            enablePersonalizedAdsPreference?.isVisible = false
            updateGdprSummary()
        } else {
            gdprPreference?.isVisible = false
        }
    }

    private fun updateGdprSummary(){
        val gdprPreference = preferenceScreen.findPreference<GdprPrivacyDialogPreference>(UserPreferences.PREFERENCE_KEY_GDPR)
        val enableAnalyticsPreference = if (UserPreferences.getUserPreferences(activity).enableAnalytics) getString(
                    R.string.message_usage_data_collection_enabled) else getString(R.string.message_usage_data_collection_disabled)
        val enablePersonalizedAdsPreference = if (UserPreferences.getUserPreferences(activity).enableAnalytics) getString(
                    R.string.message_personalized_ads_enabled) else getString(R.string.message_personalized_ads_disabled)

        gdprPreference?.summary = activity.getString(R.string.pref_gdpr_summary) + System.lineSeparator() + System.lineSeparator() + getString(
                    R.string.message_current_settings) + System.lineSeparator() +
                 enableAnalyticsPreference + System.lineSeparator() + enablePersonalizedAdsPreference
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is OpenSourceDialogPreference -> {
                // When the user selects an option to see the licenses:
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            }
            is PremiumStatusDialogPreference -> {
                // User selects the option to view premium status
                startActivity((Intent(context, PremiumStatusActivity::class.java)))
            }
            is GdprPrivacyDialogPreference -> {
                Privacy.obtainConsent(activity) {updateGdprSummary()}
            }
            else -> {
                super.onDisplayPreferenceDialog(preference)
            }
        }
    }
}