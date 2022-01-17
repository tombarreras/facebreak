/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
 */

package com.thomasjbarrerasconsulting.faces.preference

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.thomasjbarrerasconsulting.faces.R

class PreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is OpenSourceDialogPreference){
            // When the user selects an option to see the licenses:
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        } else if (preference is PremiumStatusDialogPreference) {
            // User selects the option to view premium status

        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}