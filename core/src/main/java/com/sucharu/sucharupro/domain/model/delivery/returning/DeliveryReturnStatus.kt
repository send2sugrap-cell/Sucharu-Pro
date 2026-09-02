package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Strict lifecycle states for Delivery Returns (Module 08 Step 07).
 */
enum class DeliveryReturnStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    RECEIVING("Receiving"),
    RECEIVED("Received"),
    INSPECTING("Inspecting"),
    INSPECTED("Inspected"),
    DISPOSITION_PENDING("Disposition Pending"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected");

    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED || this == REJECTED

    val canBeEdited: Boolean
        get() = this == DRAFT

    val canReceive: Boolean
        get() = this == APPROVED || this == RECEIVING

    val canInspect: Boolean
        get() = this == RECEIVED || this == INSPECTING

    val canSetDisposition: Boolean
        get() = this == INSPECTED || this == DISPOSITION_PENDING

    val canProcessInventory: Boolean
        get() = this == DISPOSITION_PENDING || this == PROCESSING
}
