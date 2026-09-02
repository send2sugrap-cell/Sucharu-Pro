package com.sucharu.sucharupro.domain.model.returns

/**
 * Inspection decision outcomes for Return Requests (Module 11 Step 03).
 */
enum class ReturnDecision(val defaultLabel: String) {
    APPROVE("Approve Return"),
    REJECT("Reject Return");

    val isApproved: Boolean
        get() = this == APPROVE

    val isRejected: Boolean
        get() = this == REJECT

    val displayName: String
        get() = defaultLabel
}
