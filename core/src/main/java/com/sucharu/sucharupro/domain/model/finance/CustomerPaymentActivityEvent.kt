package com.sucharu.sucharupro.domain.model.finance

/**
 * Activity classification for Customer Payment audit tracking (Module 09 Step 03).
 */
enum class CustomerPaymentActivityType(val defaultLabel: String) {
    PAYMENT_CREATED("Payment Created"),
    PAYMENT_UPDATED("Payment Updated"),
    PAYMENT_SUBMITTED("Payment Submitted for Posting"),
    PAYMENT_POSTED("Payment Posted & Settled"),
    PAYMENT_REJECTED("Payment Rejected"),
    PAYMENT_CANCELLED("Payment Cancelled"),
    RECEIPT_ISSUED("Receipt Issued")
}

/**
 * Immutable audit trail event for Customer Payment and Receipt operations (Module 09 Step 03).
 */
data class CustomerPaymentActivityEvent(
    val eventId: String,
    val paymentId: String,
    val projectId: String,
    val activityType: CustomerPaymentActivityType,
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
