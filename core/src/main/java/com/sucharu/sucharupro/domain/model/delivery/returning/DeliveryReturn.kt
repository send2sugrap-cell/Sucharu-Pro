package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Aggregate root representing a Delivery Return transaction in Sucharu Pro (Module 08 Step 07).
 */
data class DeliveryReturn(
    val returnId: String,
    val projectId: String,
    val returnNo: String,
    val deliveryOrderId: String,
    val deliveryChallanId: String? = null,
    val dispatchExecutionId: String? = null,
    val shipmentId: String? = null,
    val verificationId: String? = null,
    val customerId: String? = null,
    val returnType: DeliveryReturnType = DeliveryReturnType.CUSTOMER_RETURN,
    val returnReason: DeliveryReturnReason = DeliveryReturnReason.CUSTOMER_REQUEST,
    val status: DeliveryReturnStatus = DeliveryReturnStatus.DRAFT,
    val priority: DeliveryReturnPriority = DeliveryReturnPriority.NORMAL,
    val requestedBy: String,
    val approvedBy: String? = null,
    val receivedBy: String? = null,
    val inspectedBy: String? = null,
    val completedBy: String? = null,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val replacementRequired: Boolean = false,
    val replacementReferenceId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val approvedAt: Long? = null,
    val receivedAt: Long? = null,
    val inspectedAt: Long? = null,
    val completedAt: Long? = null,
    val cancelledAt: Long? = null
) {
    init {
        require(returnId.isNotBlank()) { "Return ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(returnNo.isNotBlank()) { "Return Number cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(requestedBy.isNotBlank()) { "Requested By cannot be blank." }
        require(createdAt > 0) { "Created timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation." }
    }
}
