package com.sucharu.sucharupro.domain.model.qc

/**
 * Lifecycle status of a Final QC inspection record in Sucharu Pro ERP (Module 06 Step 07).
 */
enum class FinalQcStatus(
    val defaultLabel: String
) {
    /** Initial draft status before being queued for inspection. */
    DRAFT("Draft"),

    /** Queued and awaiting inspector assignment or start. */
    PENDING("Pending"),

    /** Inspector assigned to the Final QC inspection. */
    ASSIGNED("Assigned"),

    /** Final QC inspection actively in progress. */
    IN_INSPECTION("In Inspection"),

    /** Inspection passed all quality checks. */
    PASSED("Passed"),

    /** Inspection failed one or more quality checks. */
    FAILED("Failed"),

    /** Blocked from completion or release by unresolved upstream quality issues. */
    BLOCKED("Blocked"),

    /** Formally cancelled before completion. */
    CANCELLED("Cancelled"),

    /** Production release authorized and formally released. */
    RELEASED("Released");

    /**
     * Determines whether a transition to [target] status is permitted by the state machine.
     */
    fun canTransitionTo(target: FinalQcStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(PENDING, ASSIGNED, IN_INSPECTION, CANCELLED)
            PENDING -> target in setOf(ASSIGNED, IN_INSPECTION, CANCELLED)
            ASSIGNED -> target in setOf(IN_INSPECTION, PENDING, CANCELLED)
            IN_INSPECTION -> target in setOf(PASSED, FAILED, BLOCKED, CANCELLED)
            PASSED -> target in setOf(RELEASED, BLOCKED, FAILED)
            FAILED -> target in setOf(BLOCKED, IN_INSPECTION)
            BLOCKED -> target in setOf(IN_INSPECTION, PENDING, CANCELLED)
            CANCELLED -> false
            RELEASED -> false
        }
    }

    /**
     * Flag indicating whether this is a terminal, immutable status.
     */
    val isTerminal: Boolean
        get() = this in setOf(CANCELLED, RELEASED)

    companion object {
        fun fromString(value: String?): FinalQcStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
