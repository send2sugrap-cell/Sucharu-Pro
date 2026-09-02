package com.sucharu.sucharupro.domain.model.qc

/**
 * Lifecycle status for individual QC time tracking entries (Module 06 Step 08).
 */
enum class QcTimeStatus(
    val defaultLabel: String,
    val isTerminal: Boolean = false,
    val isLocked: Boolean = false
) {
    /** Active/in-progress timer or open time tracking record. */
    OPEN("Open", isTerminal = false, isLocked = false),

    /** Time tracking completed and duration recorded. */
    RECORDED("Recorded", isTerminal = false, isLocked = false),

    /** Included in an active reconciliation calculation. */
    RECONCILED("Reconciled", isTerminal = false, isLocked = false),

    /** Permanently sealed and immutable after reconciliation lock. */
    LOCKED("Locked", isTerminal = true, isLocked = true),

    /** Cancelled or voided time entry. */
    CANCELLED("Cancelled", isTerminal = true, isLocked = false);

    /**
     * Determines whether transitioning to [target] is valid.
     */
    fun canTransitionTo(target: QcTimeStatus): Boolean {
        if (this == target) return true
        if (this.isLocked || this.isTerminal) return false

        return when (this) {
            OPEN -> target in setOf(RECORDED, CANCELLED)
            RECORDED -> target in setOf(RECONCILED, LOCKED, CANCELLED)
            RECONCILED -> target in setOf(RECORDED, LOCKED, CANCELLED)
            LOCKED, CANCELLED -> false
        }
    }

    companion object {
        fun fromString(value: String?): QcTimeStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
