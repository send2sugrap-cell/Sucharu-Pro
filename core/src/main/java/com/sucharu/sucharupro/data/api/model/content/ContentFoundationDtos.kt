package com.sucharu.sucharupro.data.api.model.content

import kotlinx.serialization.Serializable

@Serializable
data class ContentMediaReferenceDto(
    val mediaId: String,
    val contentId: String,
    val mediaType: String = "MAIN_IMAGE",
    val mediaUri: String,
    val altText: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

@Serializable
data class ContentFoundationDto(
    val contentId: String,
    val contentType: String = "FINISHED_PRODUCT",
    val productId: String? = null,
    val productName: String,
    val productCode: String,
    val categoryId: String? = null,
    val categoryName: String,
    val subCategoryName: String? = null,
    val templateCode: String,
    val internalReference: String? = null,

    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val badgeText: String? = null,
    val specificationList: List<String> = emptyList(),
    val featuresList: List<String> = emptyList(),
    val tagsList: List<String> = emptyList(),
    val searchKeywords: List<String> = emptyList(),

    val mediaReferences: List<ContentMediaReferenceDto> = emptyList(),

    val unit: String = "PCS",
    val minimumQuantity: Int = 1,
    val availableQuantity: Int = 0,
    val isActive: Boolean = true,

    val publicationStatus: String = "DRAFT",
    val scheduledStartAt: String? = null,
    val scheduledEndAt: String? = null,

    val createdAt: String = "",
    val updatedAt: String = "",
    val createdBy: String = ""
)

@Serializable
data class CreateContentFoundationRequestDto(
    val contentType: String = "FINISHED_PRODUCT",
    val productId: String? = null,
    val productName: String,
    val productCode: String,
    val categoryId: String? = null,
    val categoryName: String,
    val subCategoryName: String? = null,
    val templateCode: String,
    val internalReference: String? = null,

    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val badgeText: String? = null,
    val specificationList: List<String> = emptyList(),
    val featuresList: List<String> = emptyList(),
    val tagsList: List<String> = emptyList(),
    val searchKeywords: List<String> = emptyList(),

    val mediaReferences: List<ContentMediaReferenceDto> = emptyList(),

    val unit: String = "PCS",
    val minimumQuantity: Int = 1,
    val availableQuantity: Int = 0,
    val publicationStatus: String = "DRAFT",
    val scheduledStartAt: String? = null,
    val scheduledEndAt: String? = null
)
