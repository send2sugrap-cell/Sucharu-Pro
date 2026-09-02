package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Expiry categorization status for documents (Module 10 Step 06).
 */
enum class VendorDocumentExpiryStatus(val defaultLabel: String) {
    VALID("Valid"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired"),
    NO_EXPIRY("No Expiry Date")
}
