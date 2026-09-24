package com.sucharu.sucharupro.data.api.model.cms

import kotlinx.serialization.Serializable

/**
 * DTO representing a Public Wall Banner / Special Offer item in Sucharu Pro CMS.
 */
@Serializable
data class CmsBannerDto(
    val bannerId: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val discountTag: String,
    val targetDestination: String = "AppDestination.Public.Offers",
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val createdAt: Long = 0L
)

/**
 * DTO representing a Printing Product Category item with custom image in Sucharu Pro CMS.
 */
@Serializable
data class CmsCategoryDto(
    val categoryId: String,
    val categoryName: String,
    val imageUrl: String,
    val description: String,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

/**
 * DTO representing a Design Template item under Services and Products in Sucharu Pro CMS.
 */
@Serializable
data class CmsDesignTemplateDto(
    val templateId: String,
    val templateCode: String,
    val categoryName: String,
    val title: String,
    val colorMode: String = "৪ কালার (CMYK)",
    val paperStock: String = "৩০০ GSM আর্ট কার্ড",
    val printSize: String = "স্ট্যান্ডার্ড সাইজ",
    val finishing: String = "গ্লস ল্যামিনেশন",
    val suitabilityDescription: String = "উচ্চমানের কমার্শিয়াল কোয়ালিটি",
    val priceText: String = "৳ ১,২০০ / ১,০০০ পিস",
    val perUnitRate: String = "(৳ ১.২০ / পিস)",
    val imageUrl: String = "",
    val badgeLabel: String? = null,
    val isActive: Boolean = true
)

/**
 * Request DTO for creating / uploading a new CMS Banner.
 */
@Serializable
data class CreateCmsBannerRequestDto(
    val title: String,
    val description: String,
    val imageUrl: String,
    val discountTag: String,
    val targetDestination: String = "AppDestination.Public.Offers",
    val displayOrder: Int = 0
)

/**
 * Request DTO for creating / uploading a new CMS Category.
 */
@Serializable
data class CreateCmsCategoryRequestDto(
    val categoryName: String,
    val imageUrl: String,
    val description: String,
    val displayOrder: Int = 0
)

/**
 * Request DTO for creating / uploading a new Design Template.
 */
@Serializable
data class CreateCmsDesignTemplateRequestDto(
    val categoryName: String,
    val title: String,
    val colorMode: String = "৪ কালার (CMYK)",
    val paperStock: String = "৩০০ GSM আর্ট কার্ড",
    val printSize: String = "স্ট্যান্ডার্ড সাইজ",
    val finishing: String = "গ্লস ল্যামিনেশন",
    val suitabilityDescription: String = "উচ্চমানের কমার্শিয়াল কোয়ালিটি",
    val priceText: String = "৳ ১,২০০ / ১,০০০ পিস",
    val perUnitRate: String = "(৳ ১.২০ / পিস)",
    val imageUrl: String = "",
    val badgeLabel: String? = null
)

/**
 * Response DTO containing active Public Wall Feed Banners and Category Images.
 */
@Serializable
data class PublicWallCmsFeedResponseDto(
    val banners: List<CmsBannerDto>,
    val categories: List<CmsCategoryDto>,
    val templates: List<CmsDesignTemplateDto> = emptyList()
)

/**
 * DTO representing Admin Panel CMS Configuration for Order Checkout Form titles, labels & charges.
 */
@Serializable
data class CmsOrderFormConfigDto(
    val formTitle: String = "🎉 বিশেষ অফার",
    val quickCheckoutBadge: String = "QUICK CHECKOUT",
    val formSubtitle: String = "বুকিং করতে নিচের ফরমটি পূরণ করুন।",
    val customerSectionTitle: String = "১. গ্রাহকের তথ্য (Customer Info)",
    val designSectionTitle: String = "২. ডিজাইন অপশন (Design Choice)",
    val instructionSectionTitle: String = "৩. বিশেষ নির্দেশনা (Special Instructions)",
    val instructionPlaceholder: String = "বিস্তারিত লিখতে আপনার ব্যবসা বা প্রতিষ্ঠানের ধরন বলে সুচারু এ আই এর সহযোগিতা নিন",
    val confirmButtonText: String = "অর্ডার কনফার্ম করুন",
    val insideDhakaDeliveryCharge: Double = 60.0,
    val outsideDhakaDeliveryCharge: Double = 120.0
)
