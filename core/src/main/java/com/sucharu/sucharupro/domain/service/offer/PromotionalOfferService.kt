package com.sucharu.sucharupro.domain.service.offer

import com.sucharu.sucharupro.data.api.model.offer.EvaluateOfferEligibilityResponseDto
import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.offer.PromotionalOffer
import com.sucharu.sucharupro.domain.model.offer.WallType
import com.sucharu.sucharupro.domain.repository.PromotionalOfferRepository

/**
 * Domain Service for Form 03 — Offer & Audience Eligibility Evaluation & Rule Enforcement.
 */
class PromotionalOfferService(
    private val repository: PromotionalOfferRepository
) {
    suspend fun createOfferConfig(offer: PromotionalOffer): PromotionalOffer {
        require(offer.offerName.isNotBlank()) { "Offer name is required." }
        require(offer.offerCode.isNotBlank()) { "Offer code is required." }

        val existing = repository.getOfferByCode(offer.offerCode)
        require(existing == null || existing.offerId == offer.offerId) {
            "Offer code '${offer.offerCode}' is already taken."
        }

        return repository.createOffer(offer)
    }

    suspend fun updateOfferConfig(offer: PromotionalOffer): PromotionalOffer {
        return repository.updateOffer(offer)
    }

    suspend fun listAllOffers(): List<PromotionalOffer> {
        return repository.getAllOffers()
    }

    suspend fun getOfferById(offerId: String): PromotionalOffer? {
        return repository.getOfferById(offerId)
    }

    suspend fun listOffersVisibleOnWall(wall: WallType): List<PromotionalOffer> {
        return repository.getOffersVisibleOnWall(wall)
    }

    suspend fun listOffersEligibleForAudience(audience: AudienceType): List<PromotionalOffer> {
        return repository.getOffersEligibleForAudience(audience)
    }

    /**
     * Evaluates REDEMPTION ELIGIBILITY for [offerId] against [audience] and order constraints.
     * (CRITICAL INVARIANT: Evaluates Offer Eligibility, NOT Wall Visibility!)
     */
    suspend fun evaluateEligibility(
        offerId: String,
        audience: AudienceType,
        orderQuantity: Int = 1,
        orderValue: Double = 0.0
    ): EvaluateOfferEligibilityResponseDto {
        val offer = repository.getOfferById(offerId)
            ?: return EvaluateOfferEligibilityResponseDto(
                isEligible = false,
                offerId = offerId,
                offerCode = "",
                audienceType = audience.name,
                reason = "Offer not found."
            )

        if (!offer.isActive) {
            return EvaluateOfferEligibilityResponseDto(
                isEligible = false,
                offerId = offerId,
                offerCode = offer.offerCode,
                audienceType = audience.name,
                reason = "Offer is administratively inactive."
            )
        }

        if (!offer.isAudienceEligible(audience)) {
            return EvaluateOfferEligibilityResponseDto(
                isEligible = false,
                offerId = offerId,
                offerCode = offer.offerCode,
                audienceType = audience.name,
                reason = "Audience '$audience' is not eligible for this offer."
            )
        }

        if (orderQuantity < offer.minOrderQuantity) {
            return EvaluateOfferEligibilityResponseDto(
                isEligible = false,
                offerId = offerId,
                offerCode = offer.offerCode,
                audienceType = audience.name,
                reason = "Order quantity ($orderQuantity) is below minimum threshold (${offer.minOrderQuantity})."
            )
        }

        if (orderValue < offer.minOrderValue) {
            return EvaluateOfferEligibilityResponseDto(
                isEligible = false,
                offerId = offerId,
                offerCode = offer.offerCode,
                audienceType = audience.name,
                reason = "Order value ($orderValue) is below minimum threshold (${offer.minOrderValue})."
            )
        }

        return EvaluateOfferEligibilityResponseDto(
            isEligible = true,
            offerId = offerId,
            offerCode = offer.offerCode,
            audienceType = audience.name,
            reason = "Audience '$audience' is fully eligible to redeem this offer."
        )
    }

    /**
     * Evaluates DISPLAY VISIBILITY for [offerId] on [wall].
     */
    suspend fun evaluateWallVisibility(offerId: String, wall: WallType): Boolean {
        val offer = repository.getOfferById(offerId) ?: return false
        return offer.isVisibleOnWall(wall)
    }
}
