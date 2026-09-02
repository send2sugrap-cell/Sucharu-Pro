package com.sucharu.sucharupro.domain.model.returns

/**
 * Lifecycle states for Return Governance Exceptions (Module 11 Step 06).
 */
enum class ReturnExceptionStatus(val defaultLabel: String) {
    OPEN("Open"),
    ACKNOWLEDGED("Acknowledged"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed");

    val isTerminal: Boolean
        get() = this == RESOLVED || this == DISMISSED

    val displayName: String
        get() = defaultLabel
}
