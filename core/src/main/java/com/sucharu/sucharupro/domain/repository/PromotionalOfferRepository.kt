package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.offer.PromotionalOffer
import com.sucharu.sucharupro.domain.model.offer.WallType

/**
 * Domain Repository Contract for Form 03 — Offer & Audience Eligibility persistence.
 */
interface PromotionalOfferRepository {
    suspend fun createOffer(offer: PromotionalOffer): PromotionalOffer
    suspend fun updateOffer(offer: PromotionalOffer): PromotionalOffer
    suspend fun getOfferById(offerId: String): PromotionalOffer?
    suspend fun getOfferByCode(offerCode: String): PromotionalOffer?
    suspend fun getAllOffers(): List<PromotionalOffer>
    suspend fun getOffersVisibleOnWall(wall: WallType): List<PromotionalOffer>
    suspend fun getOffersEligibleForAudience(audience: AudienceType): List<PromotionalOffer>
    suspend fun deleteOffer(offerId: String): Boolean
}
