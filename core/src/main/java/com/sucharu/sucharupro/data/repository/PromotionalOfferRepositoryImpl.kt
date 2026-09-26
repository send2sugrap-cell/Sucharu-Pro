package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.offer.OfferType
import com.sucharu.sucharupro.domain.model.offer.PromotionalOffer
import com.sucharu.sucharupro.domain.model.offer.WallType
import com.sucharu.sucharupro.domain.repository.PromotionalOfferRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe Repository Implementation for Form 03 Promotional Offers & Audience Eligibility.
 */
class PromotionalOfferRepositoryImpl : PromotionalOfferRepository {
    private val store = ConcurrentHashMap<String, PromotionalOffer>()

    init {
        // Default Sample Offers demonstrating Offer Eligibility vs Wall Visibility Separation
        val o1 = PromotionalOffer(
            offerId = "OFFER-2026-EID",
            offerName = "Eid Special Bulk Visiting Card Offer",
            offerCode = "EID-2026-VC",
            offerType = OfferType.PROMOTIONAL_DISCOUNT,
            description = "১০\n১০০০ মেট ফিনিশ ভিজিটিং কার্ড বিশেষ ছাড়ে।",
            badgeText = "১৫% ছাড়",
            isEveryoneEligible = true,
            isGuestEligible = true,
            isCustomerEligible = true,
            isAffiliateEligible = true,
            publicGuestWallVisible = true,
            customerWallVisible = true,
            affiliateWallVisible = true,
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "SYSTEM"
        )
        val o2 = PromotionalOffer(
            offerId = "OFFER-2026-AFFILIATE-EXCLUSIVE",
            offerName = "Affiliate Network Exclusive Cashback Deal",
            offerCode = "AFF-EXCL-2026",
            offerType = OfferType.AFFILIATE_EXCLUSIVE,
            description = "শুধু মাত্র রেজিস্টার্ড এ্যাফিলিয়েট পার্টনারদের জন্য বিশেষ ডিল।",
            badgeText = "AFFILIATE ONLY",
            isEveryoneEligible = false,
            isGuestEligible = false,
            isCustomerEligible = false,
            isAffiliateEligible = true, // ELIGIBLE for Affiliate ONLY
            publicGuestWallVisible = true, // VISIBLE on Public Wall as Teaser! (CRITICAL INVARIANT PROOF)
            customerWallVisible = true,
            affiliateWallVisible = true,
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "SYSTEM"
        )
        store[o1.offerId] = o1
        store[o2.offerId] = o2
    }

    override suspend fun createOffer(offer: PromotionalOffer): PromotionalOffer {
        store[offer.offerId] = offer
        return offer
    }

    override suspend fun updateOffer(offer: PromotionalOffer): PromotionalOffer {
        store[offer.offerId] = offer
        return offer
    }

    override suspend fun getOfferById(offerId: String): PromotionalOffer? {
        return store[offerId]
    }

    override suspend fun getOfferByCode(offerCode: String): PromotionalOffer? {
        return store.values.firstOrNull { it.offerCode.equals(offerCode, ignoreCase = true) }
    }

    override suspend fun getAllOffers(): List<PromotionalOffer> {
        return store.values.sortedBy { it.displayPriority }
    }

    override suspend fun getOffersVisibleOnWall(wall: WallType): List<PromotionalOffer> {
        return store.values.filter { it.isVisibleOnWall(wall) }.sortedBy { it.displayPriority }
    }

    override suspend fun getOffersEligibleForAudience(audience: AudienceType): List<PromotionalOffer> {
        return store.values.filter { it.isAudienceEligible(audience) }.sortedBy { it.displayPriority }
    }

    override suspend fun deleteOffer(offerId: String): Boolean {
        return store.remove(offerId) != null
    }
}
