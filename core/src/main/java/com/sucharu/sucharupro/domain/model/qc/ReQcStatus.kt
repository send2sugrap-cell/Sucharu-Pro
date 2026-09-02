package com.sucharu.sucharupro.domain.model.qc

/**
 * Strict lifecycle status model for Re-QC & Failure Loops (Module 06 Step 06).
 */
enum class ReQcStatus(
    val defaultLabel: String,
    val isTerminal: Boolean = false,
    val isProtectedBoundary: Boolean = false
) {
    /** Initial draft state before submission. */
    DRAFT(
        defaultLabel = "Draft",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Submitted Re-QC request awaiting assignment or inspection. */
    PENDING(
        defaultLabel = "Pending",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Assigned to a QC inspector; ready for inspection execution. */
    ASSIGNED(
        defaultLabel = "Assigned",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Re-QC inspection is actively being performed. */
    IN_INSPECTION(
        defaultLabel = "In Inspection",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Re-QC passed successfully. Terminal state; eligible for next boundary (Final QC). */
    PASSED(
        defaultLabel = "Passed",
        isTerminal = true,
        isProtectedBoundary = true
    ),

    /** Re-QC failed. Non-terminal in the aggregate lifecycle, triggers failure record and return to rework. */
    FAILED(
        defaultLabel = "Failed",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Handoff to Rework workflow for subsequent corrective cycle. Protected boundary state. */
    RETURNED_TO_REWORK(
        defaultLabel = "Returned to Rework",
        isTerminal = false,
        isProtectedBoundary = true
    ),

    /** Re-QC request cancelled or discarded. Terminal state. */
    CANCELLED(
        defaultLabel = "Cancelled",
        isTerminal = true,
        isProtectedBoundary = false
    );

    /**
     * Enforces the valid forward state transition matrix for [ReQcStatus].
     */
    fun canTransitionTo(target: ReQcStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal || this.isProtectedBoundary) return false

        return when (this) {
            DRAFT -> target in setOf(PENDING, CANCELLED)
            PENDING -> target in setOf(ASSIGNED, IN_INSPECTION, CANCELLED)
            ASSIGNED -> target in setOf(IN_INSPECTION, PENDING, CANCELLED)
            IN_INSPECTION -> target in setOf(PASSED, FAILED, CANCELLED)
            FAILED -> target in setOf(RETURNED_TO_REWORK)
            RETURNED_TO_REWORK, PASSED, CANCELLED -> false
        }
    }

    companion object {
        fun fromString(value: String?): ReQcStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
