package com.sucharu.sucharupro.domain.model.order

/**
 * Lifecycle states for an initial customer inquiry in Sucharu Pro.
 *
 * Represents the status of the customer requirement before or during quotation preparation:
 *  - NEW: Newly captured customer inquiry.
 *  - IN_PROGRESS: Sales or estimator is actively reviewing specifications and estimating costs.
 *  - QUOTED: Formal quotation has been created and issued for this inquiry.
 *  - CONVERTED: Successfully converted into a confirmed commercial order.
 *  - CLOSED: Inquiry completed or archived without quotation.
 *  - CANCELLED: Customer withdrew or cancelled the inquiry.
 */
enum class InquiryStatusType(val defaultLabel: String) {
    /** Newly captured customer inquiry. */
    NEW("New"),

    /** Actively reviewing specifications and estimating cost. */
    IN_PROGRESS("In Progress"),

    /** Quotation generated and presented to customer. */
    QUOTED("Quoted"),

    /** Successfully converted into a confirmed order. */
    CONVERTED("Converted"),

    /** Closed or archived without conversion. */
    CLOSED("Closed"),

    /** Cancelled or withdrawn. */
    CANCELLED("Cancelled");

    /**
     * Checks if transitioning from this status to [target] is valid.
     */
    fun canTransitionTo(target: InquiryStatusType): Boolean {
        if (this == target) return true
        return when (this) {
            NEW -> target in setOf(IN_PROGRESS, QUOTED, CANCELLED)
            IN_PROGRESS -> target in setOf(QUOTED, CLOSED, CANCELLED)
            QUOTED -> target in setOf(CONVERTED, CLOSED, CANCELLED)
            CONVERTED -> false // Terminal success state
            CLOSED -> target == IN_PROGRESS // Can reopen
            CANCELLED -> target == NEW // Can reopen as new
        }
    }
}
