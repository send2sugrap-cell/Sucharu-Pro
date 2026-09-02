package com.sucharu.sucharupro.domain.model.design

/**
 * Strongly-typed lifecycle status of an Approval in Sucharu Pro ERP (Module 05 Step 04).
 */
enum class ApprovalStatus(val defaultLabel: String) {
    /** Approval request draft. */
    DRAFT("Draft"),

    /** Submitted and pending reviewer pick-up. */
    PENDING_REVIEW("Pending Review"),

    /** Actively being reviewed by authorized approver. */
    UNDER_REVIEW("Under Review"),

    /** Changes requested by reviewer; triggers Step 03 revision cycle. */
    REVISION_REQUIRED("Revision Required"),

    /** Proof permanently rejected. */
    REJECTED("Rejected"),

    /** Revised proof resubmitted and returning to review. */
    RESUBMITTED("Resubmitted"),

    /** Approved by authorized reviewer. */
    APPROVED("Approved"),

    /** Final lock applied; immutable decision reference for prepress/production. */
    FINAL_LOCKED("Final Locked");

    val isPending: Boolean get() = this == PENDING_REVIEW || this == UNDER_REVIEW
    val isApproved: Boolean get() = this == APPROVED || this == FINAL_LOCKED
    val isLocked: Boolean get() = this == FINAL_LOCKED
    val isTerminal: Boolean get() = this == FINAL_LOCKED || this == REJECTED

    /**
     * Determines whether transitioning from this status to [target] is valid.
     */
    fun canTransitionTo(target: ApprovalStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal) return false

        return when (this) {
            DRAFT -> target == PENDING_REVIEW || target == REJECTED
            PENDING_REVIEW -> target == UNDER_REVIEW || target == REVISION_REQUIRED || target == REJECTED || target == APPROVED
            UNDER_REVIEW -> target == APPROVED || target == REVISION_REQUIRED || target == REJECTED
            REVISION_REQUIRED -> target == RESUBMITTED || target == REJECTED
            RESUBMITTED -> target == PENDING_REVIEW || target == UNDER_REVIEW || target == REJECTED
            APPROVED -> target == FINAL_LOCKED
            FINAL_LOCKED -> false
            REJECTED -> false
        }
    }
}
