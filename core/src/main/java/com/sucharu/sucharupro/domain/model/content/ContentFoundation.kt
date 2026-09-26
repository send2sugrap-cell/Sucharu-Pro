package com.sucharu.sucharupro.domain.model.content

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.product.ProductType

/**
 * Form 01 — Content & Product Foundation Canonical Domain Entity.
 *
 * Serves as the authoritative metadata foundation for visual design studios, gallery cards,
 * hero banners, public wall feeds, and product catalog linkages without duplicating Module 07 Product Master.
 */
data class ContentFoundation(
    val contentId: String,
    val contentType: ProductType = ProductType.FINISHED_PRODUCT,
    val productId: String? = null,
    val productName: String,
    val productCode: String,
    val categoryId: String? = null,
    val categoryName: String,
    val subCategoryName: String? = null,
    val templateCode: String,
    val internalReference: String? = null,

    // Content Text Fields
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val badgeText: String? = null,
    val specificationList: List<String> = emptyList(),
    val featuresList: List<String> = emptyList(),
    val tagsList: List<String> = emptyList(),
    val searchKeywords: List<String> = emptyList(),

    // Media References
    val mediaReferences: List<ContentMediaReference> = emptyList(),

    // Product Info & Availability
    val unit: InventoryUnit = InventoryUnit.PCS,
    val minimumQuantity: Int = 1,
    val availableQuantity: Int = 0,
    val isActive: Boolean = true,

    // Publication Lifecycle
    val publicationStatus: PublicationStatus = PublicationStatus.DRAFT,
    val scheduledStartAt: String? = null,
    val scheduledEndAt: String? = null,

    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(contentId.isNotBlank()) { "Content ID cannot be blank." }
        require(productName.isNotBlank()) { "Product Name cannot be blank." }
        require(productCode.isNotBlank()) { "Product Code cannot be blank." }
        require(categoryName.isNotBlank()) { "Category Name cannot be blank." }
        require(templateCode.isNotBlank()) { "Template Code cannot be blank." }
        require(title.isNotBlank()) { "Title cannot be blank." }
        require(minimumQuantity > 0) { "Minimum Quantity must be greater than zero." }
        require(availableQuantity >= 0) { "Available Quantity cannot be negative." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
    }

    /**
     * Evaluates whether the content foundation is currently publishable at [currentTimestamp].
     */
    fun isCurrentlyPublishable(currentTimestamp: String = createdAt): Boolean {
        if (!isActive) return false
        return when (publicationStatus) {
            PublicationStatus.PUBLISHED -> {
                val startOk = scheduledStartAt == null || currentTimestamp >= scheduledStartAt
                val endOk = scheduledEndAt == null || currentTimestamp <= scheduledEndAt
                startOk && endOk
            }
            PublicationStatus.SCHEDULED -> {
                val startOk = scheduledStartAt != null && currentTimestamp >= scheduledStartAt
                val endOk = scheduledEndAt == null || currentTimestamp <= scheduledEndAt
                startOk && endOk
            }
            else -> false
        }
    }
}
