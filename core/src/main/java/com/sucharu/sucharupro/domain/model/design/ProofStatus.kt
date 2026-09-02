package com.sucharu.sucharupro.domain.model.design

/**
 * Strongly-typed lifecycle status of a Proof in Sucharu Pro ERP (Module 05 Step 03).
 *
 * NOTE: Does NOT include approval or production handoff states, which belong to later steps.
 */
enum class ProofStatus(val defaultLabel: String) {
    /** Initial proof draft. */
    DRAFT("Draft"),

    /** Proof generated and ready for internal/editorial review. */
    READY_FOR_REVIEW("Ready for Review"),

    /** Changes requested; waiting for designer to pick up revision. */
    REVISION_REQUESTED("Revision Requested"),

    /** Designer actively performing revisions. */
    REVISING("Revising"),

    /** Revised proof version generated and resubmitted for review. */
    RESUBMITTED("Resubmitted"),

    /** Proof superseded or archived. */
    ARCHIVED("Archived");

    val isTerminal: Boolean get() = this == ARCHIVED
    val isArchived: Boolean get() = this == ARCHIVED

    /**
     * Determines whether transitioning from this status to [target] is valid.
     */
    fun canTransitionTo(target: ProofStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal) return false

        return when (this) {
            DRAFT -> target == READY_FOR_REVIEW || target == ARCHIVED
            READY_FOR_REVIEW -> target == REVISION_REQUESTED || target == ARCHIVED
            REVISION_REQUESTED -> target == REVISING || target == ARCHIVED
            REVISING -> target == RESUBMITTED || target == ARCHIVED
            RESUBMITTED -> target == REVISION_REQUESTED || target == ARCHIVED
            ARCHIVED -> false
        }
    }
}
