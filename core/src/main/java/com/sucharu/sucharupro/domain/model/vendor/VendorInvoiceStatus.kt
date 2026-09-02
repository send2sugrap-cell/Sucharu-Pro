package com.sucharu.sucharupro.domain.model.vendor

/**
 * Lifecycle status of a Vendor Invoice (Module 12 Step 07).
 */
enum class VendorInvoiceStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    MATCHED,
    APPROVED,
    POSTED,
    REJECTED,
    CANCELLED;

    val isEditable: Boolean get() = this in setOf(DRAFT, SUBMITTED)
    val isUnderReview: Boolean get() = this == UNDER_REVIEW
    val isMatched: Boolean get() = this == MATCHED
    val isApproved: Boolean get() = this == APPROVED
    val isPosted: Boolean get() = this == POSTED
    val isTerminal: Boolean get() = this in setOf(POSTED, REJECTED, CANCELLED)
    val isCancelled: Boolean get() = this == CANCELLED
    val isRejected: Boolean get() = this == REJECTED

    fun canTransitionTo(target: VendorInvoiceStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(SUBMITTED, CANCELLED)
            SUBMITTED -> target in setOf(UNDER_REVIEW, MATCHED, REJECTED, CANCELLED)
            UNDER_REVIEW -> target in setOf(MATCHED, REJECTED, CANCELLED)
            MATCHED -> target in setOf(APPROVED, REJECTED, UNDER_REVIEW, CANCELLED)
            APPROVED -> target in setOf(POSTED, CANCELLED)
            POSTED -> false // Terminal
            REJECTED -> false // Terminal
            CANCELLED -> false // Terminal
        }
    }
}

/**
 * 3-Way Match evaluation status for a Vendor Invoice.
 */
enum class VendorInvoiceMatchStatus {
    NOT_MATCHED,
    MATCHED,
    PARTIAL_MATCH,
    MISMATCH,
    EXCEPTION
}

/**
 * Structured taxonomy of 3-way matching exceptions.
 */
enum class VendorInvoiceExceptionType {
    VENDOR_MISMATCH,
    PURCHASE_ORDER_MISMATCH,
    RECEIPT_MISSING,
    QUANTITY_VARIANCE,
    PRICE_VARIANCE,
    TAX_VARIANCE,
    TOTAL_VARIANCE,
    CURRENCY_MISMATCH,
    UOM_MISMATCH,
    DUPLICATE_INVOICE,
    UNRECEIVED_QUANTITY,
    INVALID_SOURCE_REFERENCE
}
