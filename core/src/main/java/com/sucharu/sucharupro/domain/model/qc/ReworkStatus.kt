package com.sucharu.sucharupro.domain.model.qc

/**
 * Strict lifecycle status model for QC Rework Management & Workflow (Module 06 Step 05).
 */
enum class ReworkStatus(
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

    /** Submitted rework request awaiting review or approval. */
    REQUESTED(
        defaultLabel = "Requested",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Under formal management review and feasibility assessment. */
    UNDER_REVIEW(
        defaultLabel = "Under Review",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Formally approved by management; ready for assignment. */
    APPROVED(
        defaultLabel = "Approved",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Assigned to a technician/operator; ready for execution. */
    ASSIGNED(
        defaultLabel = "Assigned",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Corrective rework action is actively being performed. */
    IN_PROGRESS(
        defaultLabel = "In Progress",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Corrective rework action finished by operator; ready for return to QC. */
    COMPLETED(
        defaultLabel = "Completed",
        isTerminal = false,
        isProtectedBoundary = false
    ),

    /** Handed off to QC for subsequent Re-QC inspection (Module 06 Step 06 boundary). */
    RETURNED_TO_QC(
        defaultLabel = "Returned to QC",
        isTerminal = false,
        isProtectedBoundary = true
    ),

    /** Rework discarded or cancelled. Terminal state. */
    CANCELLED(
        defaultLabel = "Cancelled",
        isTerminal = true,
        isProtectedBoundary = false
    ),

    /** Rework request rejected by management. Terminal state. */
    REJECTED(
        defaultLabel = "Rejected",
        isTerminal = true,
        isProtectedBoundary = false
    );

    /**
     * Enforces the valid forward state transition matrix for [ReworkStatus].
     */
    fun canTransitionTo(target: ReworkStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal || this.isProtectedBoundary) return false

        return when (this) {
            DRAFT -> target in setOf(REQUESTED, CANCELLED)
            REQUESTED -> target in setOf(UNDER_REVIEW, APPROVED, REJECTED, CANCELLED)
            UNDER_REVIEW -> target in setOf(APPROVED, REJECTED, CANCELLED)
            APPROVED -> target in setOf(ASSIGNED, CANCELLED)
            ASSIGNED -> target in setOf(IN_PROGRESS, APPROVED, CANCELLED)
            IN_PROGRESS -> target in setOf(COMPLETED, CANCELLED)
            COMPLETED -> target in setOf(RETURNED_TO_QC)
            RETURNED_TO_QC, CANCELLED, REJECTED -> false
        }
    }

    companion object {
        fun fromString(value: String?): ReworkStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
