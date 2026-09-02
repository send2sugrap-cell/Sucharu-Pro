package com.sucharu.sucharupro.domain.model.returns

/**
 * Lifecycle states for Return Inspection (Module 11 Step 03).
 */
enum class ReturnInspectionStatus(val defaultLabel: String) {
    PENDING("Pending Inspection"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Inspection Completed");

    val isTerminal: Boolean
        get() = this == COMPLETED

    val displayName: String
        get() = defaultLabel
}
