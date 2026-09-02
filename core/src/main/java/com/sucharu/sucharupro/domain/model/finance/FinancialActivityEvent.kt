package com.sucharu.sucharupro.domain.model.finance

/**
 * Immutable activity log entry for financial audit trails (Module 09 Step 01).
 */
data class FinancialActivityEvent(
    val eventId: String,
    val transactionId: String,
    val projectId: String,
    val activityType: FinancialActivityType,
    val actorId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(transactionId.isNotBlank()) { "Transaction ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(details.isNotBlank()) { "Details cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
