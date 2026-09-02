package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Status lifecycle of a Vendor Document Request (Module 10 Step 06).
 */
enum class VendorDocumentRequestStatus(val defaultLabel: String, val isTerminal: Boolean = false) {
    OPEN("Open", isTerminal = false),
    SUBMITTED("Submitted", isTerminal = false),
    UNDER_REVIEW("Under Review", isTerminal = false),
    COMPLETED("Completed", isTerminal = true),
    OVERDUE("Overdue", isTerminal = false),
    CANCELLED("Cancelled", isTerminal = true)
}
