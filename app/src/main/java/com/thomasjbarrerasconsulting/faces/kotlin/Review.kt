/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.ReviewManager
import com.thomasjbarrerasconsulting.faces.kotlin.PackageManagerExtensions.Companion.getPackageInfoCompat
import com.thomasjbarrerasconsulting.faces.preference.UserPreferences
import java.util.*

class Review {

    companion object {
        private const val TAG = "Review"
        private const val SIXTY_MINUTES_IN_MS = 3600000
        private const val FIVE_MINUTES_IN_MS =  300000
//        private const val SIXTY_MINUTES_IN_MS = 60000
//        private const val FIVE_MINUTES_IN_MS =  60000
        private const val MIN_LAUNCH_CHECK_COUNT = 6
        private lateinit var reviewManager: ReviewManager
        private var firstInstallTime: Long = 0
        private var triggerTime: Long = 0
        private var launchInAppReviewCount = 0

        fun resetLaunchInAppReviewCount(){
            launchInAppReviewCount = 0
        }

        private fun updateTriggerTime(){
            triggerTime = if (triggerTime == 0L){
                System.currentTimeMillis() + FIVE_MINUTES_IN_MS
            } else {
                System.currentTimeMillis() + SIXTY_MINUTES_IN_MS
            }

            if (triggerTime < firstInstallTime + SIXTY_MINUTES_IN_MS){
                triggerTime = firstInstallTime + SIXTY_MINUTES_IN_MS
            }
        }

        fun initialize(context: Context) {
            reviewManager = ReviewManagerFactory.create(context)
            firstInstallTime = context.packageManager.getPackageInfoCompat(context.packageName, 0).firstInstallTime
            updateTriggerTime()
        }

        private fun reviewTriggered(context: Context): Boolean{
            if (launchInAppReviewCount < MIN_LAUNCH_CHECK_COUNT){
                launchInAppReviewCount += 1
            }
            val currentTime = System.currentTimeMillis()
            val triggered = (launchInAppReviewCount >= MIN_LAUNCH_CHECK_COUNT) && (currentTime >= triggerTime) && UserPreferences.getUserPreferences(context).enableInAppReviews
            if (triggered){
                resetLaunchInAppReviewCount()
                updateTriggerTime()
            }
            return triggered
        }

        fun checkLaunchInAppReview(activity:Activity){
            if (reviewTriggered(activity)) {
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        val request = reviewManager.requestReviewFlow()
                        request.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // We got the ReviewInfo object
                                val reviewInfo = task.result

                                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                                flow.addOnCompleteListener { _ ->
                                    // The flow has finished. The API does not indicate whether the user
                                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                                    // matter the result, we continue our app flow.
                                }
                            } else {
                                Log.e(TAG, "${task.exception}")
                            }
                        }
                    }
                }, 1000)
            }
        }
    }
}