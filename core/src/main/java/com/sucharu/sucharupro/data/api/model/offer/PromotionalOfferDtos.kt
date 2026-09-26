package com.sucharu.sucharupro.data.api.model.offer

import kotlinx.serialization.Serializable

@Serializable
data class PromotionalOfferDto(
    val offerId: String,
    val offerName: String,
    val offerCode: String,
    val offerType: String = "PROMOTIONAL_DISCOUNT",
    val description: String? = null,
    val badgeText: String? = null,
    val termsAndConditions: String? = null,

    // Eligibility
    val isEveryoneEligible: Boolean = true,
    val isGuestEligible: Boolean = true,
    val isCustomerEligible: Boolean = true,
    val isAffiliateEligible: Boolean = true,

    // Rules
    val isActive: Boolean = true,
    val startAt: String? = null,
    val endAt: String? = null,
    val maxRedemption: Int = 0,
    val perCustomerLimit: Int = 0,
    val minOrderQuantity: Int = 1,
    val minOrderValue: Double = 0.0,

    // Scope
    val applicableProductIds: List<String> = emptyList(),
    val applicableCategoryIds: List<String> = emptyList(),

    // Presentation
    val visualDesignId: String? = null,
    val promotionalText: String? = null,
    val ctaText: String = "অর্ডার করুন",
    val displayPriority: Int = 0,

    // Wall Visibility
    val publicGuestWallVisible: Boolean = true,
    val customerWallVisible: Boolean = true,
    val affiliateWallVisible: Boolean = true,

    val createdAt: String = "",
    val updatedAt: String = "",
    val createdBy: String = ""
)

@Serializable
data class CreatePromotionalOfferRequestDto(
    val offerName: String,
    val offerCode: String,
    val offerType: String = "PROMOTIONAL_DISCOUNT",
    val description: String? = null,
    val badgeText: String? = null,

    val isEveryoneEligible: Boolean = true,
    val isGuestEligible: Boolean = true,
    val isCustomerEligible: Boolean = true,
    val isAffiliateEligible: Boolean = true,

    val maxRedemption: Int = 0,
    val perCustomerLimit: Int = 0,
    val minOrderQuantity: Int = 1,
    val minOrderValue: Double = 0.0,

    val publicGuestWallVisible: Boolean = true,
    val customerWallVisible: Boolean = true,
    val affiliateWallVisible: Boolean = true
)

@Serializable
data class EvaluateOfferEligibilityRequestDto(
    val audienceType: String = "CUSTOMER", // GUEST, CUSTOMER, AFFILIATE
    val orderQuantity: Int = 1,
    val orderValue: Double = 0.0
)

@Serializable
data class EvaluateOfferEligibilityResponseDto(
    val isEligible: Boolean,
    val offerId: String,
    val offerCode: String,
    val audienceType: String,
    val reason: String
)
