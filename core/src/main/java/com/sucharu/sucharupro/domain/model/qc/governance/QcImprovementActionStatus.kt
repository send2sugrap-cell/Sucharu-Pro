package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * State machine for Quality Improvement Actions (Module 06 Step 10).
 */
enum class QcImprovementActionStatus(
    val defaultLabel: String,
    val isTerminal: Boolean
) {
    PROPOSED("Proposed", false),
    APPROVED("Approved", false),
    ASSIGNED("Assigned", false),
    IN_PROGRESS("In Progress", false),
    COMPLETED("Completed", false),
    VERIFIED("Verified (Effective)", true),
    REJECTED("Rejected", true),
    CANCELLED("Cancelled", true);

    val canTransition: Boolean get() = !isTerminal

    companion object {
        fun fromString(value: String?): QcImprovementActionStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
