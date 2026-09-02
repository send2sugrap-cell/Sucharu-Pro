package com.sucharu.sucharupro.domain.model.delivery.partial

/**
 * Aggregate root representing the partial delivery and settlement state of a DeliveryOrder (Module 08 Step 06).
 */
data class DeliveryPartialSettlement(
    val settlementId: String,
    val projectId: String,
    val deliveryOrderId: String,
    val customerId: String? = null,
    val status: DeliverySettlementStatus = DeliverySettlementStatus.OPEN,
    val totalOrderedQuantity: Double = 0.0,
    val totalAllocatedQuantity: Double = 0.0,
    val totalDispatchedQuantity: Double = 0.0,
    val totalDeliveredQuantity: Double = 0.0,
    val totalShortQuantity: Double = 0.0,
    val totalExcessQuantity: Double = 0.0,
    val totalReturnedQuantity: Double = 0.0,
    val totalReplacementQuantity: Double = 0.0,
    val totalPendingQuantity: Double = totalOrderedQuantity,
    val settlementVersion: Int = 1,
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String? = null,
    val updatedAt: Long
) {
    init {
        require(settlementId.isNotBlank()) { "Settlement ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(totalOrderedQuantity >= 0) { "Total ordered quantity cannot be negative." }
        require(totalAllocatedQuantity >= 0) { "Total allocated quantity cannot be negative." }
        require(totalDispatchedQuantity >= 0) { "Total dispatched quantity cannot be negative." }
        require(totalDeliveredQuantity >= 0) { "Total delivered quantity cannot be negative." }
        require(totalShortQuantity >= 0) { "Total short quantity cannot be negative." }
        require(totalExcessQuantity >= 0) { "Total excess quantity cannot be negative." }
        require(totalReturnedQuantity >= 0) { "Total returned quantity cannot be negative." }
        require(totalReplacementQuantity >= 0) { "Total replacement quantity cannot be negative." }
        require(totalPendingQuantity >= 0) { "Total pending quantity cannot be negative." }
        require(settlementVersion >= 1) { "Settlement version must be >= 1." }
        require(createdBy.isNotBlank()) { "Created by cannot be blank." }
        require(createdAt > 0) { "Created timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp must be >= created timestamp." }
    }

    val completionPercentage: Double
        get() = if (totalOrderedQuantity > 0.0) {
            ((totalDeliveredQuantity / totalOrderedQuantity) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

    val isFullySettled: Boolean
        get() = status == DeliverySettlementStatus.SETTLED || (totalDeliveredQuantity >= totalOrderedQuantity && totalPendingQuantity <= 0.0)
}
