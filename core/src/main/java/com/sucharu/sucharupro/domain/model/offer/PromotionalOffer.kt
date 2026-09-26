package com.sucharu.sucharupro.domain.model.offer

/**
 * Form 03 — Canonical Offer & Audience Eligibility Domain Entity.
 *
 * Enforces strict separation between OFFER ELIGIBILITY (Who can REDEEM)
 * and WALL VISIBILITY (Where the offer is DISPLAYED).
 */
data class PromotionalOffer(
    val offerId: String,
    val offerName: String,
    val offerCode: String,
    val offerType: OfferType = OfferType.PROMOTIONAL_DISCOUNT,
    val description: String? = null,
    val badgeText: String? = null,
    val termsAndConditions: String? = null,

    // Offer Eligibility Flags (Who can REDEEM the offer)
    val isEveryoneEligible: Boolean = true,
    val isGuestEligible: Boolean = true,
    val isCustomerEligible: Boolean = true,
    val isAffiliateEligible: Boolean = true,

    // Offer Rules & Constraints
    val isActive: Boolean = true,
    val startAt: String? = null,
    val endAt: String? = null,
    val maxRedemption: Int = 0,
    val perCustomerLimit: Int = 0,
    val minOrderQuantity: Int = 1,
    val minOrderValue: Double = 0.0,

    // Applicable Scope (Product & Category Linkages)
    val applicableProductIds: List<String> = emptyList(),
    val applicableCategoryIds: List<String> = emptyList(),

    // Offer Presentation & Form 02 Design Linkage
    val visualDesignId: String? = null,
    val promotionalText: String? = null,
    val ctaText: String = "অর্ডার করুন",
    val displayPriority: Int = 0,

    // Wall Visibility Flags (Where the offer is DISPLAYED)
    val publicGuestWallVisible: Boolean = true,
    val customerWallVisible: Boolean = true,
    val affiliateWallVisible: Boolean = true,

    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(offerId.isNotBlank()) { "Offer ID cannot be blank." }
        require(offerName.isNotBlank()) { "Offer Name cannot be blank." }
        require(offerCode.isNotBlank()) { "Offer Code cannot be blank." }
        require(minOrderQuantity > 0) { "Minimum Order Quantity must be greater than zero." }
        require(minOrderValue >= 0.0) { "Minimum Order Value cannot be negative." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
    }

    /**
     * Evaluates whether [audience] is REDEMPTION ELIGIBLE for this offer.
     */
    fun isAudienceEligible(audience: AudienceType): Boolean {
        if (!isActive) return false
        if (isEveryoneEligible) return true
        return when (audience) {
            AudienceType.GUEST -> isGuestEligible
            AudienceType.CUSTOMER -> isCustomerEligible
            AudienceType.AFFILIATE -> isAffiliateEligible
        }
    }

    /**
     * Evaluates whether this offer is DISPLAY VISIBLE on [wall].
     * (CRITICAL INVARIANT: Wall Visibility is independent of Offer Eligibility!)
     */
    fun isVisibleOnWall(wall: WallType): Boolean {
        if (!isActive) return false
        return when (wall) {
            WallType.PUBLIC_GUEST_WALL -> publicGuestWallVisible
            WallType.CUSTOMER_WALL -> customerWallVisible
            WallType.AFFILIATE_WALL -> affiliateWallVisible
        }
    }
}
