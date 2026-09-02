package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Summary projection for Delivery Return metrics and dashboards (Module 08 Step 07).
 */
data class DeliveryReturnSummary(
    val returnId: String,
    val projectId: String,
    val returnNo: String,
    val deliveryOrderId: String,
    val customerId: String?,
    val returnType: DeliveryReturnType,
    val returnReason: DeliveryReturnReason,
    val status: DeliveryReturnStatus,
    val priority: DeliveryReturnPriority,
    val totalReturnedQuantity: Double,
    val totalReceivedQuantity: Double,
    val totalAcceptedQuantity: Double,
    val totalRejectedQuantity: Double,
    val totalRestockedQuantity: Double,
    val isFullyRestocked: Boolean,
    val lineCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
