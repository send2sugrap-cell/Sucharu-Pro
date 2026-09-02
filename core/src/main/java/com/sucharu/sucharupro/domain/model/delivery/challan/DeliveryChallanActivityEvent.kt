package com.sucharu.sucharupro.domain.model.delivery.challan

/**
 * Immutable audit event record for Delivery Challan activities (Module 08 Step 02).
 */
data class DeliveryChallanActivityEvent(
    val activityId: String,
    val projectId: String,
    val challanId: String,
    val activityType: DeliveryChallanActivityType,
    val performedBy: String,
    val performedAt: Long,
    val details: String? = null,
    val previousStatus: String? = null,
    val newStatus: String? = null
) {
    init {
        require(activityId.isNotBlank()) { "Activity ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(challanId.isNotBlank()) { "Challan ID cannot be blank." }
        require(performedBy.isNotBlank()) { "Performed By cannot be blank." }
        require(performedAt > 0) { "Performed At timestamp must be positive." }
    }
}
