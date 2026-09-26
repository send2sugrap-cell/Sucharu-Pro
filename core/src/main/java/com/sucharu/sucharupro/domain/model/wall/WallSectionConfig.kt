package com.sucharu.sucharupro.domain.model.wall

/**
 * Section configuration for Form 06 Server-Driven Wall Engine.
 */
data class WallSectionConfig(
    val sectionId: String,
    val wallId: String,
    val sectionType: SectionType = SectionType.PRODUCT_GALLERY,
    val sectionName: String,
    val sectionTitle: String? = null,
    val sectionSubtitle: String? = null,
    val layoutType: SectionLayoutType = SectionLayoutType.GRID,
    val visualDesignId: String? = null,
    val displayOrder: Int = 0,
    val priorityLevel: Int = 0,
    val isVisible: Boolean = true,
    val isFeatured: Boolean = false,
    val isPinned: Boolean = false,
    val items: List<WallSectionItem> = emptyList(),
    val createdAt: String
) {
    init {
        require(sectionId.isNotBlank()) { "Section ID cannot be blank." }
        require(wallId.isNotBlank()) { "Wall ID cannot be blank." }
        require(sectionName.isNotBlank()) { "Section Name cannot be blank." }
    }
}
