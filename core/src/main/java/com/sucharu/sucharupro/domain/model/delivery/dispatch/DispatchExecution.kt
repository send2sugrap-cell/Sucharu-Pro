package com.sucharu.sucharupro.domain.model.delivery.dispatch

/**
 * Aggregate root representing an operational Dispatch Execution in Sucharu Pro (Module 08 Step 03).
 *
 * Bridges document-level Delivery Challan authorization with physical stock withdrawal and fulfillment.
 */
data class DispatchExecution(
    val dispatchExecutionId: String,
    val projectId: String,
    val dispatchNo: String,
    val deliveryOrderId: String,
    val deliveryChallanId: String,
    val customerId: String?,
    val sourceWarehouseId: String,
    val sourceLocationId: String,
    val dispatchType: DispatchExecutionType,
    val status: DispatchExecutionStatus,
    val stockOutId: String? = null,
    val dispatchDate: Long,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val dispatchedAt: Long? = null,
    val dispatchedBy: String? = null,
    val cancelledAt: Long? = null,
    val cancelledBy: String? = null
) {
    init {
        require(dispatchExecutionId.isNotBlank()) { "Dispatch Execution ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(dispatchNo.isNotBlank()) { "Dispatch Number cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(deliveryChallanId.isNotBlank()) { "Delivery Challan ID cannot be blank." }
        require(sourceWarehouseId.isNotBlank()) { "Source Warehouse ID cannot be blank." }
        require(sourceLocationId.isNotBlank()) { "Source Location ID cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(dispatchDate > 0) { "Dispatch Date must be positive." }
        require(createdAt > 0) { "Created At must be positive." }
        require(updatedAt >= createdAt) { "Updated At cannot precede Created At." }
        if (status == DispatchExecutionStatus.DISPATCHED) {
            require(dispatchedAt != null && dispatchedAt > 0) { "dispatchedAt timestamp is required for DISPATCHED state." }
            require(!dispatchedBy.isNullOrBlank()) { "dispatchedBy actor is required for DISPATCHED state." }
            require(!stockOutId.isNullOrBlank()) { "stockOutId is required for DISPATCHED state." }
        }
        if (status == DispatchExecutionStatus.CANCELLED) {
            require(cancelledAt != null && cancelledAt > 0) { "cancelledAt timestamp is required for CANCELLED state." }
            require(!cancelledBy.isNullOrBlank()) { "cancelledBy actor is required for CANCELLED state." }
        }
    }
}
