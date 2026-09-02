package com.sucharu.sucharupro.domain.model.delivery.returning

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Event types for Delivery Return audit logging (Module 08 Step 07).
 */
enum class DeliveryReturnActivityType(val defaultLabel: String) {
    CREATED("Return Created"),
    UPDATED("Return Updated"),
    SUBMITTED("Return Submitted for Approval"),
    APPROVED("Return Approved"),
    RECEIVING_STARTED("Receiving Started"),
    RECEIVED("Items Received"),
    INSPECTION_STARTED("Inspection Started"),
    INSPECTED("Inspection Completed"),
    DISPOSITION_SET("Disposition Determined"),
    PROCESSING_STARTED("Processing Started"),
    INVENTORY_RESTOCKED("Inventory Restocked"),
    INVENTORY_QUARANTINED("Inventory Quarantined"),
    REVERSE_SHIPMENT_CREATED("Reverse Shipment Created"),
    REVERSE_SHIPMENT_UPDATED("Reverse Shipment Updated"),
    COMPLETED("Return Completed"),
    CANCELLED("Return Cancelled"),
    REJECTED("Return Rejected"),
    NOTE_ADDED("Note Added")
}

/**
 * Append-only immutable audit record for return lifecycle events (Module 08 Step 07).
 */
data class DeliveryReturnActivityEvent(
    val eventId: String,
    val projectId: String,
    val returnId: String,
    val activityType: DeliveryReturnActivityType,
    val actorId: String,
    val actorRole: UserRole? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val previousStatus: DeliveryReturnStatus? = null,
    val newStatus: DeliveryReturnStatus? = null,
    val metadata: Map<String, String> = emptyMap(),
    val notes: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(returnId.isNotBlank()) { "Return ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
