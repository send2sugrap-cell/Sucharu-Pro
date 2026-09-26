package com.sucharu.sucharupro.domain.service.offer

import com.sucharu.sucharupro.data.repository.PromotionalOfferRepositoryImpl
import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.offer.OfferType
import com.sucharu.sucharupro.domain.model.offer.PromotionalOffer
import com.sucharu.sucharupro.domain.model.offer.WallType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PromotionalOfferServiceTest {

    private lateinit var repository: PromotionalOfferRepositoryImpl
    private lateinit var service: PromotionalOfferService

    @Before
    fun setUp() {
        repository = PromotionalOfferRepositoryImpl()
        service = PromotionalOfferService(repository)
    }

    @Test
    fun `criticalInvariant_offerEligibilityIsNotEqualWallVisibility`() = runBlocking {
        // CASE A: Affiliate Eligibility = OFF, Affiliate Wall Visibility = ON (Teaser on Affiliate Wall)
        val teaserOffer = PromotionalOffer(
            offerId = "OFFER-TEST-TEASER",
            offerName = "Teaser Offer for Affiliate Wall",
            offerCode = "TEASER-01",
            offerType = OfferType.PROMOTIONAL_DISCOUNT,
            isEveryoneEligible = false,
            isGuestEligible = false,
            isCustomerEligible = true,
            isAffiliateEligible = false, // NOT ELIGIBLE for Affiliate
            publicGuestWallVisible = false,
            customerWallVisible = true,
            affiliateWallVisible = true, // VISIBLE on Affiliate Wall!
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "ADMIN-001"
        )

        service.createOfferConfig(teaserOffer)

        // Verify Wall Visibility is TRUE on Affiliate Wall
        val isVisibleOnAffiliateWall = service.evaluateWallVisibility("OFFER-TEST-TEASER", WallType.AFFILIATE_WALL)
        assertTrue("Offer must be VISIBLE on Affiliate Wall as teaser!", isVisibleOnAffiliateWall)

        // Verify Redemption Eligibility is FALSE for Affiliate
        val evalResult = service.evaluateEligibility("OFFER-TEST-TEASER", AudienceType.AFFILIATE, orderQuantity = 1, orderValue = 500.0)
        assertFalse("Affiliate MUST NOT be eligible to redeem this teaser offer!", evalResult.isEligible)
    }

    @Test
    fun `criticalInvariant_eligibleCustomerCanRedeemEvenIfHiddenFromCustomerWall`() = runBlocking {
        // CASE B: Customer Eligibility = ON, Customer Wall Visibility = OFF (Direct link deal)
        val hiddenOffer = PromotionalOffer(
            offerId = "OFFER-TEST-HIDDEN",
            offerName = "Direct Link VIP Deal",
            offerCode = "VIP-DIRECT",
            offerType = OfferType.CUSTOM_DEAL,
            isEveryoneEligible = false,
            isCustomerEligible = true, // ELIGIBLE for Customer
            customerWallVisible = false, // HIDDEN from Customer Wall!
            minOrderQuantity = 10,
            minOrderValue = 1000.0,
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "ADMIN-001"
        )

        service.createOfferConfig(hiddenOffer)

        // Verify Wall Visibility is FALSE on Customer Wall
        val isVisibleOnCustomerWall = service.evaluateWallVisibility("OFFER-TEST-HIDDEN", WallType.CUSTOMER_WALL)
        assertFalse("Offer MUST be hidden from Customer Wall!", isVisibleOnCustomerWall)

        // Verify Redemption Eligibility is TRUE when meeting order quantity and value thresholds
        val evalResult = service.evaluateEligibility("OFFER-TEST-HIDDEN", AudienceType.CUSTOMER, orderQuantity = 10, orderValue = 1000.0)
        assertTrue("Customer MUST be eligible to redeem via direct flow!", evalResult.isEligible)
    }

    @Test
    fun `evaluateEligibility_failsWhenOrderQuantityBelowThreshold`() = runBlocking {
        val bulkOffer = PromotionalOffer(
            offerId = "OFFER-TEST-BULK",
            offerName = "Bulk Printing Special",
            offerCode = "BULK-500",
            minOrderQuantity = 500,
            minOrderValue = 2000.0,
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "ADMIN-001"
        )

        service.createOfferConfig(bulkOffer)

        val failEval = service.evaluateEligibility("OFFER-TEST-BULK", AudienceType.CUSTOMER, orderQuantity = 100, orderValue = 2500.0)
        assertFalse("Redemption MUST fail when order quantity is below 500!", failEval.isEligible)
    }
}
