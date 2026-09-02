package com.sucharu.sucharupro.domain.model.delivery.dispatch

/**
 * Line item within a Dispatch Execution operation (Module 08 Step 03).
 */
data class DispatchExecutionLine(
    val dispatchExecutionLineId: String,
    val projectId: String,
    val dispatchExecutionId: String,
    val deliveryChallanLineId: String,
    val deliveryOrderLineId: String,
    val productId: String,
    val requestedQuantity: Double,
    val dispatchQuantity: Double,
    val batchId: String? = null,
    val lotId: String? = null,
    val sourceLocationId: String,
    val createdAt: Long
) {
    init {
        require(dispatchExecutionLineId.isNotBlank()) { "Dispatch Execution Line ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(dispatchExecutionId.isNotBlank()) { "Dispatch Execution ID cannot be blank." }
        require(deliveryChallanLineId.isNotBlank()) { "Delivery Challan Line ID cannot be blank." }
        require(deliveryOrderLineId.isNotBlank()) { "Delivery Order Line ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(sourceLocationId.isNotBlank()) { "Source Location ID cannot be blank." }
        require(requestedQuantity > 0) { "Requested Quantity must be greater than zero." }
        require(dispatchQuantity > 0) { "Dispatch Quantity must be greater than zero." }
        require(dispatchQuantity <= requestedQuantity) { "Dispatch Quantity ($dispatchQuantity) cannot exceed Requested Quantity ($requestedQuantity)." }
        require(createdAt > 0) { "Created At timestamp must be positive." }
    }
}
