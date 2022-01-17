/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.billing

import com.android.billingclient.api.Purchase

class Premium {
    enum class Status {
        PREMIUM_STATUS_NONE,
        PREMIUM_STATUS_PENDING,
        PREMIUM_STATUS_ENABLED
    }

    companion object{
        private const val TAG = "PremiumStatus"

        fun status(): Status{
            // TODO - Handle more than one product
            val purchase = BillingHandler.purchases.items().firstOrNull()
            if (purchase == null) return Status.PREMIUM_STATUS_NONE

            return when (purchase.purchaseState) {
                Purchase.PurchaseState.PENDING -> Status.PREMIUM_STATUS_PENDING
                Purchase.PurchaseState.PURCHASED -> Status.PREMIUM_STATUS_ENABLED
                else -> Status.PREMIUM_STATUS_NONE
            }
        }
    }
}