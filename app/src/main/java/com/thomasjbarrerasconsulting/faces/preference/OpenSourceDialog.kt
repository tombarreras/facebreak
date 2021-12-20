/*
 * Copyright 2021 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.preference

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.thomasjbarrerasconsulting.faces.R

class OpenSourceDialog : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.preference_open_source_dialog, null)

        val licenceTextControl:EditText = view.findViewById(R.id.licenseText)
        licenceTextControl.setText(this.javaClass.getResource("/res/raw/apache_2_license")?.readText())

        val urlControl:TextView = view.findViewById(R.id.licenseUrl)
        urlControl.movementMethod = LinkMovementMethod()

        return view
    }
}