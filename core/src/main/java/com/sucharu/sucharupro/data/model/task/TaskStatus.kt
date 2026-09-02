package com.sucharu.sucharupro.data.model.task

/**
 * Task lifecycle status states for Sucharu Pro ERP.
 */
enum class TaskStatus(
    val defaultLabel: String,
    val isTerminal: Boolean = false,
    val isActive: Boolean = true
) {
    DRAFT("Draft", isTerminal = false, isActive = false),
    ASSIGNED("Assigned", isTerminal = false, isActive = true),
    ACKNOWLEDGED("Acknowledged", isTerminal = false, isActive = true),
    IN_PROGRESS("In Progress", isTerminal = false, isActive = true),
    BLOCKED("Blocked", isTerminal = false, isActive = true),
    ON_HOLD("On Hold", isTerminal = false, isActive = true),
    COMPLETED("Completed", isTerminal = false, isActive = false),
    VERIFIED("Verified", isTerminal = false, isActive = false),
    CLOSED("Closed", isTerminal = true, isActive = false),
    CANCELLED("Cancelled", isTerminal = true, isActive = false),
    REJECTED("Rejected", isTerminal = true, isActive = false);

    companion object {
        fun fromString(status: String?): TaskStatus {
            if (status.isNull_or_blank()) return DRAFT
            return entries.firstOrNull { it.name.equals(status, ignoreCase = true) } ?: DRAFT
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
