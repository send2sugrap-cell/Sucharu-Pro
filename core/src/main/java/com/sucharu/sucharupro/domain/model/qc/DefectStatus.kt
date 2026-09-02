package com.sucharu.sucharupro.domain.model.qc

/**
 * Strict lifecycle status model for QC defects and failures (Module 06 Step 04).
 */
enum class DefectStatus(
    val defaultLabel: String,
    val isTerminal: Boolean = false
) {
    /** Initial state when defect is first logged. */
    OPEN(
        defaultLabel = "Open",
        isTerminal = false
    ),

    /** Defect has been reviewed and acknowledged by responsible QC/Manager. */
    ACKNOWLEDGED(
        defaultLabel = "Acknowledged",
        isTerminal = false
    ),

    /** Root cause analysis and active investigation in progress. */
    UNDER_INVESTIGATION(
        defaultLabel = "Under Investigation",
        isTerminal = false
    ),

    /** Immediate containment measures applied to stop propagation. */
    CONTAINED(
        defaultLabel = "Contained",
        isTerminal = false
    ),

    /** Corrective action formulated; awaiting resolution confirmation. */
    RESOLUTION_PENDING(
        defaultLabel = "Resolution Pending",
        isTerminal = false
    ),

    /** Resolution actions executed and validated by QC. */
    RESOLVED(
        defaultLabel = "Resolved",
        isTerminal = false
    ),

    /** Final terminal closure after sign-off. Cannot be reopened. */
    CLOSED(
        defaultLabel = "Closed",
        isTerminal = true
    ),

    /** Defect discarded or logged in error. Terminal state. */
    CANCELLED(
        defaultLabel = "Cancelled",
        isTerminal = true
    );

    /**
     * Enforces the valid forward state transition matrix for [DefectStatus].
     */
    fun canTransitionTo(target: DefectStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal) return false

        return when (this) {
            OPEN -> target in setOf(ACKNOWLEDGED, UNDER_INVESTIGATION, CANCELLED)
            ACKNOWLEDGED -> target in setOf(UNDER_INVESTIGATION, CONTAINED, RESOLUTION_PENDING, CANCELLED)
            UNDER_INVESTIGATION -> target in setOf(CONTAINED, RESOLUTION_PENDING, RESOLVED, CANCELLED)
            CONTAINED -> target in setOf(RESOLUTION_PENDING, RESOLVED, UNDER_INVESTIGATION, CANCELLED)
            RESOLUTION_PENDING -> target in setOf(RESOLVED, UNDER_INVESTIGATION, CANCELLED)
            RESOLVED -> target in setOf(CLOSED, UNDER_INVESTIGATION)
            CLOSED, CANCELLED -> false
        }
    }

    companion object {
        fun fromString(value: String?): DefectStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
