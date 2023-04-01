package com.thomasjbarrerasconsulting.faces.kotlin

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.thomasjbarrerasconsulting.faces.R

class AdLayoutChangedListener (private val premiumStatusImageView: View, private val settingsImageView: View, private val parentLayout: ConstraintLayout): View.OnLayoutChangeListener {
    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        if (v != null && (left < premiumStatusImageView.right || right > settingsImageView.left)){
            val constraintSet = ConstraintSet()
            constraintSet.clone(parentLayout)

            constraintSet.connect(settingsImageView.id, ConstraintSet.END, parentLayout.id, ConstraintSet.END,1 )
            constraintSet.connect(settingsImageView.id, ConstraintSet.TOP, v.id, ConstraintSet.BOTTOM,1 )

            constraintSet.applyTo(parentLayout)
        } else {
            val constraintSet = ConstraintSet()
            constraintSet.clone(parentLayout)

            constraintSet.connect(settingsImageView.id, ConstraintSet.END, parentLayout.id, ConstraintSet.END,1 )
            constraintSet.connect(settingsImageView.id, ConstraintSet.TOP, parentLayout.id, ConstraintSet.TOP,1 )

            constraintSet.applyTo(parentLayout)
        }
    }
}