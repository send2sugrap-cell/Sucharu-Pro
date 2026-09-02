package com.sucharu.sucharupro.domain.model.job

/**
 * High-level lifecycle status of a Production Job in Sucharu Pro Printing ERP.
 *
 * Tracks the broad state of the Job Card from draft/handoff intake through
 * execution, completion, dispatch, or cancellation.
 */
enum class ProductionJobStatus(val defaultLabel: String) {
    /** Job is in draft/initial preparation stage. */
    DRAFT("Draft"),

    /** Job has been created from a validated handoff and is ready for production intake. */
    READY_FOR_PRODUCTION("Ready for Production"),

    /** Job is currently being processed across active production stages. */
    IN_PROGRESS("In Progress"),

    /** Production is paused (e.g., waiting for client feedback or special materials). */
    ON_HOLD("On Hold"),

    /** All manufacturing, binding, and finishing stages complete; awaiting dispatch. */
    READY("Ready"),

    /** Finished products delivered or dispatched to customer. */
    DELIVERED("Delivered"),

    /** Job cancelled. */
    CANCELLED("Cancelled");

    /** Indicates whether the job has reached a terminal state where no further processing occurs. */
    val isTerminal: Boolean get() = this == DELIVERED || this == CANCELLED;

    /**
     * Determines whether transitioning from this status to [target] is valid
     * according to the canonical Production Job state machine.
     */
    fun canTransitionTo(target: ProductionJobStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal) return false

        return when (this) {
            DRAFT -> target == READY_FOR_PRODUCTION || target == CANCELLED
            READY_FOR_PRODUCTION -> target == IN_PROGRESS || target == ON_HOLD || target == CANCELLED
            IN_PROGRESS -> target == ON_HOLD || target == READY || target == CANCELLED
            ON_HOLD -> target == IN_PROGRESS || target == CANCELLED
            READY -> target == DELIVERED || target == ON_HOLD
            DELIVERED -> false
            CANCELLED -> false
        }
    }
}
