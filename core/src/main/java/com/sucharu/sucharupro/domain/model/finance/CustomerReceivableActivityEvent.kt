package com.sucharu.sucharupro.domain.model.finance

/**
 * Event classification for Customer Receivable audit tracking (Module 09 Step 02).
 */
enum class CustomerReceivableActivityType(val defaultLabel: String) {
    RECEIVABLE_CREATED("Receivable Created"),
    RECEIVABLE_UPDATED("Receivable Updated"),
    RECEIVABLE_MARKED_OVERDUE("Receivable Marked Overdue"),
    RECEIVABLE_SETTLEMENT_RECORDED("Receivable Settlement Recorded"),
    RECEIVABLE_CANCELLED("Receivable Cancelled")
}

/**
 * Immutable audit trail event for customer receivables (Module 09 Step 02).
 */
data class CustomerReceivableActivityEvent(
    val eventId: String,
    val receivableId: String,
    val projectId: String,
    val activityType: CustomerReceivableActivityType,
    val actorId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(receivableId.isNotBlank()) { "Receivable ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(details.isNotBlank()) { "Details cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
