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
import java.util.*


class BillingHandler() {
    companion object {
        private const val TAG = "BillingHandler"
        private const val PREMIUM_PRODUCT_ID = "premium"
        private const val BILLING_TASK_REFRESH_IN_APP_PURCHASES = "refreshInAppPurchases"
        private const val BILLING_TASK_REFRESH_IN_APP_PURCHASE_PRODUCT_DETAILS = "refreshInAppPurchaseProductDetails"
        private const val BILLING_TASK_START_PAYMENT = "startPayment"
        private val tasks = TaskLauncher()
        val purchases = ObservableList<Purchase>()
        val productDetails = ObservableList<ProductDetails>()
        private val purchasesUpdatedListener = MyPurchaseUpdatedListener()
        private lateinit var billingClient: BillingClient

        private fun createBillingClient(){
            billingClient = BillingClient.newBuilder(FaceBreakApplication.instance)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build()
        }

        fun initialize() {
            createBillingClient()

            refreshInAppPurchases()
            refreshInAppProductDetails()

            log("Billing client initialized")
        }

        private fun runBillingTask(taskType: String, task: () -> Unit){
            try {
                tasks.update(taskType, task)

                createBillingClientIfNecessary()

                if (billingClient.isReady) {
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
            if (!billingClient.isReady) {
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        log("Billing setup finished: billingResult=${billingResult.responseCode}: ${billingResult.debugMessage}")
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            tasks.launchAll()
                        }
                    }

                    override fun onBillingServiceDisconnected() {
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
                        billingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                        ) { billingResult, purchaseList ->
                            // Process the result
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                val filteredList = purchaseList.filter {
                                    Security.verifyPurchase(
                                        it.originalJson,
                                        it.signature
                                    )
                                }
                                handlePendingPurchases(filteredList)

                                // Don't remove purchases
                                if (!(filteredList.isEmpty() && purchases.list.any { it.purchaseState == Purchase.PurchaseState.PURCHASED })){
                                    purchases.updateIfDifferent(filteredList)
                                }
                            }

                            retryRefreshInAppPurchasesIfNecessary(billingResult)
                        }
                    }
                }
            }
        }

        private fun handlePendingPurchases(filteredList: List<Purchase>) {
            if (filteredList.isEmpty() && purchases.list.any { it.purchaseState == Purchase.PurchaseState.PENDING }){
                toast(FaceBreakApplication.instance.getString(R.string.message_payment_declined) + purchases.list.first { it.purchaseState == Purchase.PurchaseState.PENDING }.orderId)
            }
        }

        private fun retryRefreshInAppPurchasesIfNecessary(billingResult: BillingResult) {
            var retry = false
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK){
                log("Failed to refresh InApp purchases: ${billingResult.responseCode}. ${billingResult.debugMessage}. Retrying in 60 seconds")
                retry = true
            }

            if (purchases.list.any { it.purchaseState == Purchase.PurchaseState.PENDING }){
                log("Pending purchases. Retrying in 60 seconds")
                retry = true
            }

            if (retry){
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        refreshInAppPurchases()
                    }
                }, 60000)
            }
        }

        fun refreshInAppProductDetails() {
            log( "Refreshing in-app Product Details")

            runBillingTask (BILLING_TASK_REFRESH_IN_APP_PURCHASE_PRODUCT_DETAILS) {
                val productList =
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PREMIUM_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )

                val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

                billingClient.queryProductDetailsAsync(params.build()) {
                        billingResult,
                        productDetailsList ->
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                this.productDetails.updateIfDifferent(productDetailsList)
                            } else {
                                log("Failed to get product details: ${billingResult.responseCode}. ${billingResult.debugMessage}. Retrying in 60 seconds")
                                Timer().schedule(object : TimerTask() {
                                    override fun run() {
                                        refreshInAppProductDetails()
                                    }
                                }, 60000)
                            }
                }
            }
        }

        fun startPurchaseFlow(details:ProductDetails, activity: Activity) {
            log( "Purchasing product $details")

            // Launch the billing flow
            runBillingTask(BILLING_TASK_START_PAYMENT + details.description){
                val productDetailsParamsList =
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build()
                    )
                val billingFlowParams =
                    BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
                log("Purchase result: $billingResult")
            }
        }

        internal class MyPurchaseUpdatedListener : PurchasesUpdatedListener{
            override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> processPurchases(purchases)
                    BillingClient.BillingResponseCode.USER_CANCELED -> log("Purchase canceled for: $purchases")
                    else -> {
                        log("Purchases updated with response code ${billingResult.responseCode}")
                    }
                }
            }

            private fun processPurchases(purchases: MutableList<Purchase>?) {
                for (purchase in purchases!!) {

                    if (!Security.verifyPurchase(purchase.originalJson, purchase.signature)) {
                        toast(FaceBreakApplication.instance.getString(R.string.message_purchase_invalid) + purchase.orderId)
                    }
                    else
                    {
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> processPurchase(purchase)
                            Purchase.PurchaseState.PENDING -> processPendingPurchase(purchase)
                            Purchase.PurchaseState.UNSPECIFIED_STATE -> processCanceledPurchase(purchase)
                    }
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

        fun addProductDetailsListener(listener: ObservableList.ListUpdatedListener<ProductDetails>){
            productDetails.addListener(listener)
        }

        fun removeProductDetailsListener(listener: ObservableList.ListUpdatedListener<ProductDetails>){
            productDetails.removeListener(listener)
        }

        private fun log(message: String){
            try{
                Log.d(TAG, message)
            }
            catch (e: Exception){
                Log.e(TAG, e.message.toString())
            }
        }

        @Synchronized
        private fun createBillingClientIfNecessary(){
            if (! this::billingClient.isInitialized){
                createBillingClient()
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