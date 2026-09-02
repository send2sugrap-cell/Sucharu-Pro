package com.sucharu.sucharupro.domain.model.qc

/**
 * Lifecycle status for individual QC operational cost entries (Module 06 Step 08).
 */
enum class QcCostStatus(
    val defaultLabel: String,
    val isTerminal: Boolean = false,
    val isLocked: Boolean = false
) {
    /** Initial draft cost entry before submission or confirmation. */
    DRAFT("Draft", isTerminal = false, isLocked = false),

    /** Formally recorded and active QC cost entry. */
    RECORDED("Recorded", isTerminal = false, isLocked = false),

    /** Included in an active reconciliation calculation. */
    RECONCILED("Reconciled", isTerminal = false, isLocked = false),

    /** Adjusted prior to final lock with auditable justification. */
    ADJUSTED("Adjusted", isTerminal = false, isLocked = false),

    /** Permanently sealed and immutable after reconciliation lock. */
    LOCKED("Locked", isTerminal = true, isLocked = true),

    /** Discarded or voided cost entry. */
    CANCELLED("Cancelled", isTerminal = true, isLocked = false);

    /**
     * Determines whether transitioning to [target] is valid.
     */
    fun canTransitionTo(target: QcCostStatus): Boolean {
        if (this == target) return true
        if (this.isLocked || this.isTerminal) return false

        return when (this) {
            DRAFT -> target in setOf(RECORDED, CANCELLED)
            RECORDED -> target in setOf(RECONCILED, ADJUSTED, LOCKED, CANCELLED)
            RECONCILED -> target in setOf(ADJUSTED, LOCKED, CANCELLED)
            ADJUSTED -> target in setOf(RECONCILED, LOCKED, CANCELLED)
            LOCKED, CANCELLED -> false
        }
    }

    companion object {
        fun fromString(value: String?): QcCostStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
