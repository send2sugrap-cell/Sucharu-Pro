package com.sucharu.sucharupro.data.api.model.wall

import kotlinx.serialization.Serializable

@Serializable
data class WallSectionItemDto(
    val itemId: String,
    val sectionId: String,
    val contentFoundationId: String? = null,
    val productId: String? = null,
    val offerId: String? = null,
    val visualDesignId: String? = null,
    val displayOrder: Int = 0,
    val isPinned: Boolean = false
)

@Serializable
data class WallSectionConfigDto(
    val sectionId: String,
    val wallId: String,
    val sectionType: String = "PRODUCT_GALLERY",
    val sectionName: String,
    val sectionTitle: String? = null,
    val sectionSubtitle: String? = null,
    val layoutType: String = "GRID",
    val visualDesignId: String? = null,
    val displayOrder: Int = 0,
    val priorityLevel: Int = 0,
    val isVisible: Boolean = true,
    val isFeatured: Boolean = false,
    val isPinned: Boolean = false,
    val items: List<WallSectionItemDto> = emptyList()
)

@Serializable
data class ServerDrivenWallConfigDto(
    val wallId: String,
    val wallType: String = "PUBLIC",
    val wallTitle: String,
    val versionNumber: Int = 1,
    val status: String = "DRAFT",
    val isActivePublished: Boolean = false,

    val publicVisibility: Boolean = true,
    val guestVisibility: Boolean = true,
    val customerVisibility: Boolean = true,
    val affiliateVisibility: Boolean = true,

    val scheduledStartAt: String? = null,
    val scheduledEndAt: String? = null,

    val sections: List<WallSectionConfigDto> = emptyList(),

    val createdAt: String = "",
    val updatedAt: String = "",
    val createdBy: String = ""
)

@Serializable
data class ResolvedServerDrivenWallResponseDto(
    val wallId: String,
    val wallType: String,
    val wallTitle: String,
    val audienceType: String,
    val sections: List<WallSectionConfigDto> = emptyList(),
    val resolvedAt: String
)
