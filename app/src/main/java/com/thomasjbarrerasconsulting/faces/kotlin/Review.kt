/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.impl.Schedulers.schedule
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.ReviewManager
import java.util.*
import kotlin.math.min

class Review {

    companion object {
        private const val TAG = "Review"
//        private const val TWELVE_HOURS_IN_MS = 43200000
//        private const val FIVE_MINUTES_IN_MS = 300000
//        private const val SIXTY_MINUTES_IN_MS = 3600000
        private const val TWELVE_HOURS_IN_MS = 10
        private const val FIVE_MINUTES_IN_MS = 10
        private const val SIXTY_MINUTES_IN_MS = 10
        private const val MIN_LAUNCH_CHECK_COUNT = 5
        private lateinit var reviewManager: ReviewManager
        private var firstInstallTime: Long = 0
        private var triggerTime: Long = 0
        private var launchCheckCount = 0

        fun resetReviewTrigger(){
            launchCheckCount = 0
        }

        private fun updateTriggerTime(){
            val currentTime = System.currentTimeMillis()
            triggerTime = if (triggerTime == 0L){
                currentTime + FIVE_MINUTES_IN_MS
            } else {
                currentTime + SIXTY_MINUTES_IN_MS
            }
            if (triggerTime < firstInstallTime + TWELVE_HOURS_IN_MS){
                triggerTime = firstInstallTime + TWELVE_HOURS_IN_MS
            }
        }

        fun initialize(context: Context) {
            reviewManager = ReviewManagerFactory.create(context)
            firstInstallTime = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            updateTriggerTime()
        }

        private fun reviewTriggered(): Boolean{
            if (launchCheckCount < MIN_LAUNCH_CHECK_COUNT){
                launchCheckCount += 1
            }
            val currentTime = System.currentTimeMillis()
            val triggered = (launchCheckCount >= MIN_LAUNCH_CHECK_COUNT) && (currentTime >= triggerTime)
            if (triggered){
                resetReviewTrigger()
                updateTriggerTime()
            }
            return triggered
        }

        fun checkLaunchInAppReview(activity:Activity){
            if (reviewTriggered()) {
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
                }, 2000)
            }
        }
    }
}