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
