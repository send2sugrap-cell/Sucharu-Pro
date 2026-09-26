package com.sucharu.sucharupro.domain.model.wall

/**
 * Section item reference mapping canonical Content (Form 01), Product (Module 07), Offer (Form 03) or Visual Design (Form 02).
 */
data class WallSectionItem(
    val itemId: String,
    val sectionId: String,
    val contentFoundationId: String? = null,
    val productId: String? = null,
    val offerId: String? = null,
    val visualDesignId: String? = null,
    val displayOrder: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: String
) {
    init {
        require(itemId.isNotBlank()) { "Item ID cannot be blank." }
        require(sectionId.isNotBlank()) { "Section ID cannot be blank." }
    }
}
