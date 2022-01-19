/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.kotlin.ExceptionHandler
import com.thomasjbarrerasconsulting.faces.kotlin.FaceBreakApplication
import com.thomasjbarrerasconsulting.faces.kotlin.ObservableList
import com.thomasjbarrerasconsulting.faces.kotlin.Toaster.Companion.toast
import com.thomasjbarrerasconsulting.faces.kotlin.tasks.TaskLauncher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BillingHandler() {
    companion object {
        private const val TAG = "BillingHandler"
        private const val SKU_PREMIUM = "premium"
        private const val BILLING_TASK_REFRESH_IN_APP_PURCHASES = "refreshInAppPurchases"
        private const val BILLING_TASK_REFRESH_IN_APP_SKUS = "refreshInAppSkus"
        private const val BILLING_TASK_START_PAYMENT = "startPayment"

        private var billingClient: BillingClient
        private val tasks = TaskLauncher()

        private var billingServiceIsConnecting: Boolean = false
        private var billingServiceIsConnected: Boolean = false
        val purchases = ObservableList<Purchase>()
        val skus = ObservableList<SkuDetails>()
        private val purchasesUpdatedListener = MyPurchaseUpdatedListener()

        init {
            billingClient = BillingClient.newBuilder(FaceBreakApplication.instance)
                .enablePendingPurchases()
                .setListener(purchasesUpdatedListener)
                .build()

            refreshInAppPurchases()
            refreshInAppSkuDetails(listOf(SKU_PREMIUM))

            log("Billing client initialized")
        }

        private fun runBillingTask(taskType: String, task: () -> Unit){
            try {
                tasks.update(taskType, task)

                if (billingServiceIsConnected) {
                    tasks.launchAll()
                } else {
                    startServiceConnection()
                }

            } catch (e: Exception){
                ExceptionHandler.alert(FaceBreakApplication.instance, "Failed to complete task of type $taskType", TAG, e)
            }
        }

         @Synchronized
         private fun startServiceConnection() {
             if (billingServiceIsConnecting){
                 return
             }

            if (billingServiceIsConnected) {
                billingServiceIsConnecting = false
            } else {
                billingServiceIsConnecting = true
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        log("Billing setup finished: billingResult=${billingResult.responseCode}: ${billingResult.debugMessage}")
                        billingServiceIsConnecting = false
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            billingServiceIsConnected = true
                            tasks.launchAll()
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        billingServiceIsConnecting = false
                        billingServiceIsConnected = false

                        log("Billing setup disconnected")
                    }
                })
            }
        }

        fun refreshInAppPurchases() {
            log("Refreshing in-app purchases")

            runBillingTask(BILLING_TASK_REFRESH_IN_APP_PURCHASES) {
                runBlocking {
                    launch {
                        purchases.merge(billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP).purchasesList)
                    }
                }
            }
        }

        private fun refreshInAppSkuDetails(skus: List<String>) {
            log( "Refreshing in-app SKU Details")

            runBillingTask (BILLING_TASK_REFRESH_IN_APP_SKUS) {
                val params = SkuDetailsParams.newBuilder().setSkusList(skus).setType(BillingClient.SkuType.INAPP)
                billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
//                    toast("Billing Result: ${billingResult.responseCode}: ${billingResult.debugMessage}")
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                        this.skus.merge(skuDetailsList)
                    } else {
                        val message = "Failed to get SKU details: ${billingResult.responseCode}. ${billingResult.debugMessage}"
                        log(message)
                        ExceptionHandler.alert(FaceBreakApplication.instance, "Failed to get SKU details", TAG, Exception(message))
                    }
                }
            }
        }

        fun startPurchaseFlow(sku: SkuDetails, activity: Activity) {
            log( "Purchasing SKU $skus")
            runBillingTask (BILLING_TASK_START_PAYMENT + sku.description) {
                val flowParams = BillingFlowParams.newBuilder().setSkuDetails(sku).build()
                val billingResult = billingClient.launchBillingFlow(activity, flowParams)
                log("Purchase result: $billingResult")
            }
        }

        internal class MyPurchaseUpdatedListener : PurchasesUpdatedListener{
            override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> processPurchases(purchases)
                    BillingClient.BillingResponseCode.USER_CANCELED -> log("Purchase canceled for: $purchases")
                    else -> log("Purchases updated with response code ${billingResult.responseCode}")
                }
            }

            private fun processPurchases(purchases: MutableList<Purchase>?) {
                for (purchase in purchases!!) {
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> processPurchase(purchase)
                        Purchase.PurchaseState.PENDING -> processPendingPurchase(purchase)
                        Purchase.PurchaseState.UNSPECIFIED_STATE -> processCanceledPurchase(purchase)
                    }
                }
                refreshInAppPurchases()
                log("Purchase updated: $purchases")
            }

            private fun processPurchase(purchase: Purchase){
                ensurePurchaseAcknowledged(purchase)
                toast(FaceBreakApplication.instance.getString(R.string.message_premium_purchased) + purchase.orderId)

            }

            private fun processCanceledPurchase(purchase: Purchase){
                toast(FaceBreakApplication.instance.getString(R.string.message_purchase_canceled) + purchase.orderId)
            }

            private fun processPendingPurchase(purchase: Purchase){
                toast(FaceBreakApplication.instance.getString(R.string.message_purchase_pending) + purchase.orderId)
            }

            private fun ensurePurchaseAcknowledged(purchase: Purchase) {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        log("Purchase $purchase acknowledged: billingResult=$billingResult")
                    }
                }
            }
        }

        fun addPurchasesListener(listener: ObservableList.ListUpdatedListener<Purchase>){
            purchases.addListener(listener)
        }

        fun removePurchasesListener(listener: ObservableList.ListUpdatedListener<Purchase>){
            purchases.removeListener(listener)
        }

        fun addSkusListener(listener: ObservableList.ListUpdatedListener<SkuDetails>){
            skus.addListener(listener)
        }

        fun removeSkusListener(listener: ObservableList.ListUpdatedListener<SkuDetails>){
            skus.removeListener(listener)
        }

        private fun log(message: String){
            try{
                Log.d(TAG, message)
//                toast(message)
            }
            catch (e: Exception){
                Log.e(TAG, e.message.toString())
            }
        }

//        fun consumePurchase() {
//            val purchase = purchases.items().first()
//            val params = ConsumeParams.newBuilder()
//                .setPurchaseToken(purchase.purchaseToken)
//                .build()
//
//            billingClient.consumeAsync(params){ _, _ -> toast("consumed")}
//        }
    }
}