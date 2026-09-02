package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Overall compliance evaluation state of a Vendor (Module 10 Step 06).
 */
enum class VendorComplianceStatus(val defaultLabel: String) {
    COMPLIANT("Compliant"),
    PARTIALLY_COMPLIANT("Partially Compliant"),
    NON_COMPLIANT("Non-Compliant"),
    UNDER_REVIEW("Under Review"),
    EXPIRING("Expiring Soon"),
    UNKNOWN("Unknown / Not Evaluated")
}
