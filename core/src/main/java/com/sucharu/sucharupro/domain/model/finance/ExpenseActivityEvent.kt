package com.sucharu.sucharupro.domain.model.finance

/**
 * Activity event types for Expense lifecycle tracking (Module 09 Step 06).
 */
enum class ExpenseActivityType(val defaultLabel: String) {
    EXPENSE_CREATED("Expense Created"),
    EXPENSE_UPDATED("Expense Updated"),
    EXPENSE_SUBMITTED("Expense Submitted for Approval"),
    EXPENSE_APPROVED("Expense Approved"),
    EXPENSE_POSTED("Expense Posted to Financial Ledger"),
    EXPENSE_REJECTED("Expense Rejected"),
    EXPENSE_CANCELLED("Expense Cancelled")
}

/**
 * Immutable audit trail event for operational expense operations (Module 09 Step 06).
 */
data class ExpenseActivityEvent(
    val eventId: String,
    val expenseId: String,
    val projectId: String,
    val activityType: ExpenseActivityType,
    val actorId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(expenseId.isNotBlank()) { "Expense ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(details.isNotBlank()) { "Details cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
