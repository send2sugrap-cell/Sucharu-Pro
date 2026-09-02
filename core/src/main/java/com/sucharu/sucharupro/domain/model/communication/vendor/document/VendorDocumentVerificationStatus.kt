package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Verification assessment status for vendor compliance records (Module 10 Step 06).
 */
enum class VendorDocumentVerificationStatus(val defaultLabel: String) {
    NOT_REVIEWED("Not Reviewed"),
    PENDING_REVIEW("Pending Review"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    EXPIRED("Expired")
}
