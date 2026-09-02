package com.sucharu.sucharupro.domain.model.finance

/**
 * Audit event types for Financial Adjustments, Credit Notes, Debit Notes, and Refunds (Module 09 Step 07).
 */
enum class FinancialAdjustmentActivityType(val defaultLabel: String) {
    ADJUSTMENT_CREATED("Financial Adjustment Created"),
    ADJUSTMENT_UPDATED("Financial Adjustment Updated"),
    ADJUSTMENT_SUBMITTED("Financial Adjustment Submitted"),
    ADJUSTMENT_APPROVED("Financial Adjustment Approved"),
    ADJUSTMENT_POSTED("Financial Adjustment Posted to Ledger"),
    ADJUSTMENT_REJECTED("Financial Adjustment Rejected"),
    ADJUSTMENT_CANCELLED("Financial Adjustment Cancelled"),

    CREDIT_NOTE_ISSUED("Customer Credit Note Issued"),
    DEBIT_NOTE_ISSUED("Vendor Debit Note Issued"),

    REFUND_CREATED("Customer Refund Created"),
    REFUND_UPDATED("Customer Refund Updated"),
    REFUND_SUBMITTED("Customer Refund Submitted"),
    REFUND_APPROVED("Customer Refund Approved"),
    REFUND_POSTED("Customer Refund Posted to Ledger"),
    REFUND_REJECTED("Customer Refund Rejected"),
    REFUND_CANCELLED("Customer Refund Cancelled"),

    RECEIVABLE_ADJUSTED("Customer Receivable Balance Adjusted"),
    PAYABLE_ADJUSTED("Vendor Payable Balance Adjusted")
}

/**
 * Immutable audit trail event for financial adjustment operations (Module 09 Step 07).
 */
data class FinancialAdjustmentActivityEvent(
    val eventId: String,
    val entityId: String,
    val projectId: String,
    val activityType: FinancialAdjustmentActivityType,
    val actorId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(entityId.isNotBlank()) { "Entity ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(details.isNotBlank()) { "Details cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
