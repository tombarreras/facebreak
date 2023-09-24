package com.thomasjbarrerasconsulting.faces.kotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.ProductDetails
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

    private val productDetailsResponseListener = object: ObservableList.ListUpdatedListener<ProductDetails>{
        override fun listUpdated(list: List<ProductDetails>) {
            updateProductDetailsText()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeBillingAndPurchases()
        inflateUI()
        initializeAnalytics()
        updateProductDetailsText()
        updatePremiumStatusText()
        binding.purchasePremiumButton.setOnClickListener { onPurchasePremiumClick() }
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onDestroy() {
        super.onDestroy()
        BillingHandler.removePurchasesListener(purchasesListener)
        BillingHandler.removeProductDetailsListener(productDetailsResponseListener)
    }

    private fun initializeBillingAndPurchases() {
        BillingHandler.addPurchasesListener(purchasesListener)
        BillingHandler.addProductDetailsListener(productDetailsResponseListener)
        BillingHandler.refreshInAppPurchases()
        BillingHandler.refreshInAppProductDetails()
    }

    private fun updatePremiumStatusText() {
        runOnUiThread{
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
            binding.premiumStatusImageView.invalidate()
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
            BillingHandler.startPurchaseFlow(BillingHandler.productDetails.items().first(), this)
        } catch (e: Exception) {
            ExceptionHandler.alert(this, "Failed to show purchase screen.", TAG, e)
        }
    }

    private fun updateProductDetailsText(){
        runOnUiThread{
            // TODO: Handle more than one product
            val premiumProductDetails = BillingHandler.productDetails.items().firstOrNull()
            val defaultText = getString(R.string.product_info_unavailable)
            if (premiumProductDetails == null) {
                binding.productTitleTextView.text = defaultText
                binding.productPriceTextView.text = defaultText
                binding.productDescriptionTextView.text = defaultText
                binding.purchasePremiumButton.isEnabled = false
            } else {
                binding.productTitleTextView.text = premiumProductDetails.title
                binding.productPriceTextView.text = "${premiumProductDetails.oneTimePurchaseOfferDetails?.formattedPrice} (${premiumProductDetails.oneTimePurchaseOfferDetails?.priceCurrencyCode})"
                binding.productDescriptionTextView.text = premiumProductDetails.description
                binding.purchasePremiumButton.isEnabled = true
            }
        }
    }

    companion object{
        private const val TAG = "PremiumStatusActivity"
    }
}