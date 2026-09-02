package com.sucharu.sucharupro.domain.model.design

/**
 * Explicit decision outcomes available to an authorized reviewer (Module 05 Step 04).
 */
enum class ApprovalDecisionType(val defaultLabel: String) {
    /** Approves the proof version for production. */
    APPROVED("Approved"),

    /** Requires modifications, triggering an integrated Step 03 revision request. */
    REVISION_REQUIRED("Revision Required"),

    /** Rejects the proof version due to fundamental non-compliance or error. */
    REJECTED("Rejected")
}
