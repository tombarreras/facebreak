/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.preference

import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.databinding.ActivitySettingsBinding
import com.thomasjbarrerasconsulting.faces.preference.UserPreferences.Companion.PREFERENCE_KEY_FACE_BOX_LINE_WIDTH
import com.thomasjbarrerasconsulting.faces.preference.UserPreferences.Companion.PREFERENCE_KEY_PREDICTION_AVERAGING_SECONDS

private var binding: ActivitySettingsBinding? = null
private var preferencesFragment: PreferencesFragment? = null

class PreferencesActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = binding!!.root
        setContentView(view)

        preferencesFragment = PreferencesFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, preferencesFragment!!)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        val faceBoxLineWidth: EditTextPreference? = preferencesFragment?.preferenceManager?.findPreference(PREFERENCE_KEY_FACE_BOX_LINE_WIDTH)
        faceBoxLineWidth?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val averagingTime: EditTextPreference? = preferencesFragment?.preferenceManager?.findPreference(PREFERENCE_KEY_PREDICTION_AVERAGING_SECONDS)
        averagingTime?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
    }
}