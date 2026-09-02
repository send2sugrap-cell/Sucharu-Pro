package com.sucharu.sucharupro.domain.model.delivery.partial

/**
 * Entity representing a logical split fulfillment against a Delivery Order (Module 08 Step 06).
 */
data class DeliverySplitDispatch(
    val splitDispatchId: String,
    val projectId: String,
    val deliveryOrderId: String,
    val deliveryChallanId: String? = null,
    val dispatchExecutionId: String? = null,
    val shipmentId: String? = null,
    val splitSequence: Int,
    val status: DeliverySplitDispatchStatus = DeliverySplitDispatchStatus.DRAFT,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String? = null,
    val updatedAt: Long,
    val completedAt: Long? = null
) {
    init {
        require(splitDispatchId.isNotBlank()) { "Split Dispatch ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(splitSequence >= 1) { "Split sequence must be >= 1." }
        require(createdBy.isNotBlank()) { "Created by cannot be blank." }
        require(createdAt > 0) { "Created timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp must be >= created timestamp." }
    }
}
