package com.sucharu.sucharupro.domain.model.design

/**
 * Strongly-typed lifecycle status of a Design Project in Sucharu Pro Printing ERP.
 *
 * Tracks the progression from initial assignment through creative drafting,
 * proofs, customer review, revisions, approvals, and final production handoff.
 */
enum class DesignStatus(val defaultLabel: String) {
    /** Design project has been initialized for a Production Job but not yet assigned to a designer. */
    NOT_STARTED("Not Started"),

    /** Designer has been assigned; awaiting creative execution kickoff. */
    ASSIGNED("Assigned"),

    /** Designer is actively working on artwork drafts and specifications. */
    IN_DESIGN("In Design"),

    /** Initial or revised artwork proof generated; pending review dispatch. */
    PROOF_PENDING("Proof Pending"),

    /** Proof shared with customer; awaiting client feedback or approval. */
    CUSTOMER_REVIEW("Customer Review"),

    /** Customer requested changes/corrections; returned to designer queue. */
    REVISION_REQUIRED("Revision Required"),

    /** Revisions finalized; awaiting final formal sign-off. */
    APPROVAL_PENDING("Approval Pending"),

    /** Proof approved by client/manager; ready for final prepress artwork generation. */
    APPROVED("Approved"),

    /** High-resolution print-ready artwork files and separations finalized. */
    FINALIZED("Finalized"),

    /** Completed artwork handed off to production floor / prepress workflow. */
    HANDED_OFF_TO_PRODUCTION("Handed Off to Production"),

    /** Design project cancelled or superseded. */
    CANCELLED("Cancelled");

    /** Indicates whether the design project is in an unalterable terminal state. */
    val isTerminal: Boolean get() = this == HANDED_OFF_TO_PRODUCTION || this == CANCELLED

    /** Indicates whether the design project is active and editable. */
    val isEditable: Boolean get() = !isTerminal

    /**
     * Determines whether transitioning from this status to [target] is valid
     * according to the Design state machine.
     */
    fun canTransitionTo(target: DesignStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal) return false

        return when (this) {
            NOT_STARTED -> target == ASSIGNED || target == CANCELLED
            ASSIGNED -> target == IN_DESIGN || target == NOT_STARTED || target == CANCELLED
            IN_DESIGN -> target == PROOF_PENDING || target == ASSIGNED || target == CANCELLED
            PROOF_PENDING -> target == CUSTOMER_REVIEW || target == IN_DESIGN || target == CANCELLED
            CUSTOMER_REVIEW -> target == REVISION_REQUIRED || target == APPROVAL_PENDING || target == APPROVED || target == CANCELLED
            REVISION_REQUIRED -> target == IN_DESIGN || target == PROOF_PENDING || target == CANCELLED
            APPROVAL_PENDING -> target == APPROVED || target == REVISION_REQUIRED || target == CANCELLED
            APPROVED -> target == FINALIZED || target == REVISION_REQUIRED || target == CANCELLED
            FINALIZED -> target == HANDED_OFF_TO_PRODUCTION || target == CANCELLED
            HANDED_OFF_TO_PRODUCTION -> false
            CANCELLED -> false
        }
    }
}
