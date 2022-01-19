package com.thomasjbarrerasconsulting.faces.kotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.Purchase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.thomasjbarrerasconsulting.faces.R
import com.thomasjbarrerasconsulting.faces.databinding.ActivityPremiumStatusBinding
import com.thomasjbarrerasconsulting.faces.kotlin.billing.BillingHandler
import com.thomasjbarrerasconsulting.faces.kotlin.billing.Premium
import java.lang.Exception

class PremiumStatusActivity: AppCompatActivity() {

    private lateinit var binding: ActivityPremiumStatusBinding
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val purchasesListener = object: ObservableList.ListUpdatedListener<Purchase> {
        override fun listUpdated(list: List<Purchase>) {
            updatePremiumStatusText()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeBillingAndPurchases()
        inflateUI()
        initializeAnalytics()
        updateProductsText()
        updatePremiumStatusText()
        binding.purchasePremiumButton.setOnClickListener { onPurchasePremiumClick() }
        binding.backButton.setOnClickListener { onBackPressed() }
    }

    override fun onDestroy() {
        super.onDestroy()
        BillingHandler.removePurchasesListener(purchasesListener)
    }

    private fun initializeBillingAndPurchases() {
        BillingHandler.addPurchasesListener(purchasesListener)
        BillingHandler.refreshInAppPurchases()
    }

    private fun updatePremiumStatusText() {
        when {
            Premium.premiumIsActive() -> {
                binding.premiumStatusTextView.text = getString(R.string.premium_status_you_are_using_the_premium_version)
                binding.premiumStatusDescriptionTextView.text = getString(R.string.premium_status_description_premium_customer)
                binding.premiumStatusImageView.setBackgroundResource(R.drawable.ic_premium)
                binding.purchasePremiumButton.isEnabled = false
            }
            Premium.premiumIsPending() -> {
                binding.premiumStatusTextView.text = getString(R.string.premium_status_your_premium_payment_is_pending)
                binding.premiumStatusDescriptionTextView.text = getString(R.string.premium_status_description_premium_pending)
                binding.premiumStatusImageView.setBackgroundResource(R.drawable.ic_premium_pending)
                binding.purchasePremiumButton.isEnabled = false
            }
            else -> {
                binding.premiumStatusTextView.text = getString(R.string.premium_status_you_are_using_the_free_version)
                binding.premiumStatusDescriptionTextView.text = getString(R.string.premium_status_description_upgrade_to_unlock_capabilities)
                binding.premiumStatusImageView.setBackgroundResource(R.drawable.ic_free)
                binding.purchasePremiumButton.isEnabled = true
            }
        }
    }

    private fun inflateUI(){
        binding = ActivityPremiumStatusBinding.inflate(layoutInflater)

        setContentView(binding.root)
    }

    private fun initializeAnalytics() {
        firebaseAnalytics = Firebase.analytics
        Analytics.setAnalyticsEnabled(this, firebaseAnalytics)
    }

    private fun onPurchasePremiumClick(){
        try {
            BillingHandler.startPurchaseFlow(BillingHandler.skus.items().first(), this)
        } catch (e: Exception) {
            ExceptionHandler.alert(this, "Failed to show purchase screen.", TAG, e)
        }
    }

    private fun updateProductsText(){
        // TODO: Handle more than one product
        val premiumSku = BillingHandler.skus.items().firstOrNull()
        val defaultText = getString(R.string.product_info_unavailable)
        if (premiumSku == null) {
            binding.productTitleTextView.text = defaultText
            binding.productPriceTextView.text = defaultText
            binding.productDescriptionTextView.text = defaultText
            binding.purchasePremiumButton.isEnabled = false
        } else {
            binding.productTitleTextView.text = premiumSku.title
            binding.productPriceTextView.text = premiumSku.price
            binding.productDescriptionTextView.text = premiumSku.description
            binding.purchasePremiumButton.isEnabled = true
        }
    }

    companion object{
        private const val TAG = "PremiumStatusActivity"
    }
}