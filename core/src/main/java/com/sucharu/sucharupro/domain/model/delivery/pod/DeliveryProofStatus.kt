package com.sucharu.sucharupro.domain.model.delivery.pod

/**
 * Controlled lifecycle statuses for a Proof of Delivery (Module 08 Step 08).
 */
enum class DeliveryProofStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING_REVIEW("Pending Review"),
    SUBMITTED("Submitted"),
    VERIFIED("Verified"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == ACCEPTED || this == CANCELLED

    val canEdit: Boolean
        get() = this == DRAFT || this == REJECTED

    val canAddEvidence: Boolean
        get() = this == DRAFT || this == PENDING_REVIEW || this == SUBMITTED || this == REJECTED

    val canSubmit: Boolean
        get() = this == DRAFT || this == PENDING_REVIEW || this == REJECTED

    val canReview: Boolean
        get() = this == SUBMITTED || this == PENDING_REVIEW

    val canVerify: Boolean
        get() = this == SUBMITTED || this == PENDING_REVIEW

    val canAccept: Boolean
        get() = this == VERIFIED || this == SUBMITTED

    val canReject: Boolean
        get() = this == PENDING_REVIEW || this == SUBMITTED || this == VERIFIED

    val canCancel: Boolean
        get() = this == DRAFT || this == PENDING_REVIEW || this == REJECTED
}
