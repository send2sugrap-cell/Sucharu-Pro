package com.sucharu.sucharupro.domain.model.returns

/**
 * Lifecycle states for Return Requests (Module 11 Step 01).
 */
enum class ReturnStatus(val defaultLabel: String) {
    REQUESTED("Requested"),
    UNDER_INSPECTION("Under Inspection"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    RETURN_RECEIVED("Return Received"),
    PROCESSED("Processed"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == PROCESSED || this == CANCELLED || this == REJECTED

    val displayName: String
        get() = defaultLabel
}
