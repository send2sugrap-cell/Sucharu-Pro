package com.sucharu.sucharupro.domain.model.qc

/**
 * Strongly-typed lifecycle status of a Quality Control inspection aggregate in Sucharu Pro ERP.
 */
enum class QcStatus(val defaultLabel: String) {
    /** Initial draft inspection record. */
    DRAFT("Draft"),

    /** Ready for inspection; awaiting inspector kickoff. */
    PENDING_INSPECTION("Pending Inspection"),

    /** Inspector actively executing physical quality inspection. */
    IN_INSPECTION("In Inspection"),

    /** Inspection completed successfully; quality standards met. */
    PASSED("Passed"),

    /** Inspection completed with defects; failed quality standards. */
    FAILED("Failed"),

    /** Inspection cancelled. */
    CANCELLED("Cancelled");

    /** Indicates whether this status is a terminal state. */
    val isTerminal: Boolean get() = this == PASSED || this == FAILED || this == CANCELLED

    /** Indicates whether this inspection record is editable. */
    val isEditable: Boolean get() = !isTerminal

    /**
     * Determines whether transitioning from this status to [target] is valid.
     */
    fun canTransitionTo(target: QcStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal) return false

        return when (this) {
            DRAFT -> target == PENDING_INSPECTION || target == CANCELLED
            PENDING_INSPECTION -> target == IN_INSPECTION || target == CANCELLED
            IN_INSPECTION -> target == PASSED || target == FAILED || target == CANCELLED
            PASSED -> false
            FAILED -> false
            CANCELLED -> false
        }
    }
}
