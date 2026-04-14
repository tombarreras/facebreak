package com.thomasjbarrerasconsulting.faces.kotlin.billing

import com.android.billingclient.api.Purchase
import com.thomasjbarrerasconsulting.faces.kotlin.ObservableList
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PremiumTest {

    private lateinit var mockPurchaseList: ObservableList<Purchase>

    @Before
    fun setUp() {
        mockPurchaseList = mockk()
        mockkObject(BillingHandler.Companion)
        every { BillingHandler.purchases } returns mockPurchaseList
    }

    @After
    fun tearDown() {
        unmockkObject(BillingHandler.Companion)
    }

    @Test
    fun `status returns NONE when no purchases`() {
        every { mockPurchaseList.items() } returns emptyList()

        assertEquals(Premium.Status.PREMIUM_STATUS_NONE, Premium.status())
    }

    @Test
    fun `status returns PENDING when purchase is pending`() {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PENDING
        every { mockPurchaseList.items() } returns listOf(purchase)

        assertEquals(Premium.Status.PREMIUM_STATUS_PENDING, Premium.status())
    }

    @Test
    fun `status returns ENABLED when purchase is purchased`() {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { mockPurchaseList.items() } returns listOf(purchase)

        assertEquals(Premium.Status.PREMIUM_STATUS_ENABLED, Premium.status())
    }

    @Test
    fun `status returns NONE for unspecified purchase state`() {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.UNSPECIFIED_STATE
        every { mockPurchaseList.items() } returns listOf(purchase)

        assertEquals(Premium.Status.PREMIUM_STATUS_NONE, Premium.status())
    }

    @Test
    fun `premiumIsActive returns true when purchased`() {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { mockPurchaseList.items() } returns listOf(purchase)

        assertTrue(Premium.premiumIsActive())
    }

    @Test
    fun `premiumIsActive returns false when pending`() {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PENDING
        every { mockPurchaseList.items() } returns listOf(purchase)

        assertFalse(Premium.premiumIsActive())
    }

    @Test
    fun `premiumIsActive returns false when no purchases`() {
        every { mockPurchaseList.items() } returns emptyList()

        assertFalse(Premium.premiumIsActive())
    }

    @Test
    fun `premiumIsPending returns true when pending`() {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PENDING
        every { mockPurchaseList.items() } returns listOf(purchase)

        assertTrue(Premium.premiumIsPending())
    }

    @Test
    fun `premiumIsPending returns false when purchased`() {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { mockPurchaseList.items() } returns listOf(purchase)

        assertFalse(Premium.premiumIsPending())
    }
}
