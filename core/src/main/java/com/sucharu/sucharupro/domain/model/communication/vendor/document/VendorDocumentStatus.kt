package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Lifecycle states of a Vendor Document (Module 10 Step 06).
 */
enum class VendorDocumentStatus(val defaultLabel: String, val isTerminal: Boolean = false) {
    REQUESTED("Requested", isTerminal = false),
    SUBMITTED("Submitted", isTerminal = false),
    UNDER_REVIEW("Under Review", isTerminal = false),
    APPROVED("Approved", isTerminal = false),
    REJECTED("Rejected", isTerminal = false),
    EXPIRED("Expired", isTerminal = false),
    RENEWAL_REQUIRED("Renewal Required", isTerminal = false),
    CANCELLED("Cancelled", isTerminal = true)
}
