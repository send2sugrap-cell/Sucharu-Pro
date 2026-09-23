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
 * Response DTO containing active Public Wall Feed Banners and Category Images.
 */
@Serializable
data class PublicWallCmsFeedResponseDto(
    val banners: List<CmsBannerDto>,
    val categories: List<CmsCategoryDto>
)
