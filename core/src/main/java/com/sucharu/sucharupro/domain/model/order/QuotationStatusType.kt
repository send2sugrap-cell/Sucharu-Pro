package com.sucharu.sucharupro.domain.model.order

/**
 * Commercial lifecycle status of a Quotation in Sucharu Pro.
 */
enum class QuotationStatusType(val defaultLabel: String) {
    /** Quotation is in draft estimation stage. */
    DRAFT("Draft"),

    /** Quotation has been submitted/sent to the customer. */
    SENT("Sent"),

    /** Commercial terms or specifications are under active negotiation. */
    NEGOTIATION("Negotiation"),

    /** Quotation has been accepted and approved by customer. */
    APPROVED("Approved"),

    /** Quotation was declined/rejected by customer. */
    REJECTED("Rejected"),

    /** Quotation validity window has expired. */
    EXPIRED("Expired"),

    /** Quotation was cancelled before approval. */
    CANCELLED("Cancelled");

    /**
     * Checks whether transitioning from this quotation status to [target] is permitted.
     */
    fun canTransitionTo(target: QuotationStatusType): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(SENT, CANCELLED)
            SENT -> target in setOf(NEGOTIATION, APPROVED, REJECTED, EXPIRED, CANCELLED)
            NEGOTIATION -> target in setOf(SENT, APPROVED, REJECTED, CANCELLED)
            APPROVED -> false // Terminal approved state for that revision
            REJECTED -> target == NEGOTIATION // Reopened for renegotiation
            EXPIRED -> target in setOf(DRAFT, SENT) // Re-quoted or extended
            CANCELLED -> false // Terminal cancelled
        }
    }
}
