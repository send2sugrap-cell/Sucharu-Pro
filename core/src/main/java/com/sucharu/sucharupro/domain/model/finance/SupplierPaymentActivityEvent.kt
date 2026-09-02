package com.sucharu.sucharupro.domain.model.finance

/**
 * Activity event types for Supplier Payment lifecycle tracking (Module 09 Step 05).
 */
enum class SupplierPaymentActivityType(val defaultLabel: String) {
    PAYMENT_CREATED("Supplier Payment Created"),
    PAYMENT_UPDATED("Supplier Payment Updated"),
    PAYMENT_SUBMITTED("Supplier Payment Submitted"),
    PAYMENT_APPROVED("Supplier Payment Approved"),
    PAYMENT_POSTED("Supplier Payment Posted to Ledger"),
    PAYMENT_REJECTED("Supplier Payment Rejected"),
    PAYMENT_CANCELLED("Supplier Payment Cancelled"),
    PAYMENT_SETTLEMENT_RECORDED("Payable Settlement Recorded")
}

/**
 * Immutable audit trail event for Supplier Payment operations (Module 09 Step 05).
 */
data class SupplierPaymentActivityEvent(
    val eventId: String,
    val paymentId: String,
    val projectId: String,
    val activityType: SupplierPaymentActivityType,
    val actorId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(paymentId.isNotBlank()) { "Payment ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(details.isNotBlank()) { "Details cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
