package com.sucharu.sucharupro.domain.model.delivery.partial

/**
 * Line item entity for quantities assigned to a specific split dispatch (Module 08 Step 06).
 */
data class DeliverySplitDispatchLine(
    val splitDispatchLineId: String,
    val projectId: String,
    val splitDispatchId: String = "",
    val deliveryOrderLineId: String,
    val productId: String,
    val quantity: Double,
    val batchId: String? = null,
    val lotId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(splitDispatchLineId.isNotBlank()) { "Split Dispatch Line ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderLineId.isNotBlank()) { "Delivery Order Line ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(quantity > 0) { "Split quantity must be strictly positive (> 0)." }
        require(createdAt > 0) { "Created timestamp must be positive." }
    }
}
