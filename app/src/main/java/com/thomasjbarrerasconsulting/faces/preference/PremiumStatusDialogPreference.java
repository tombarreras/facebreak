/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
 */
package com.thomasjbarrerasconsulting.faces.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

public class PremiumStatusDialogPreference extends DialogPreference {
    public PremiumStatusDialogPreference(Context context) {
        super(context);
    }
    public PremiumStatusDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PremiumStatusDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}

