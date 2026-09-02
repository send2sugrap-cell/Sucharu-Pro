package com.sucharu.sucharupro.domain.model.finance

/**
 * Activity classification for Vendor Payable audit tracking (Module 09 Step 04).
 */
enum class VendorPayableActivityType(val defaultLabel: String) {
    PAYABLE_CREATED("Payable Created"),
    PAYABLE_UPDATED("Payable Updated"),
    PAYABLE_SUBMITTED("Payable Submitted for Approval"),
    PAYABLE_APPROVED("Payable Approved"),
    PAYABLE_MARKED_OVERDUE("Payable Marked Overdue"),
    PAYABLE_SETTLEMENT_RECORDED("Payable Settlement Recorded"),
    PAYABLE_CANCELLED("Payable Cancelled")
}

/**
 * Immutable audit trail event for Vendor Payable operations (Module 09 Step 04).
 */
data class VendorPayableActivityEvent(
    val eventId: String,
    val payableId: String,
    val projectId: String,
    val activityType: VendorPayableActivityType,
    val actorId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(payableId.isNotBlank()) { "Payable ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(details.isNotBlank()) { "Details cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
