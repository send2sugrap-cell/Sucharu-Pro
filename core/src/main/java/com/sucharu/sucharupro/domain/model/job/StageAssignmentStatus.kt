package com.sucharu.sucharupro.domain.model.job

/**
 * Lifecycle status of an individual operator assignment to a production stage.
 */
enum class StageAssignmentStatus(val defaultLabel: String) {
    /** Operator is currently actively assigned to the stage. */
    ASSIGNED("Assigned"),

    /** Previous assignment superseded by a reassignment to another operator. */
    REASSIGNED("Reassigned"),

    /** Operator assignment was removed without a replacement. */
    UNASSIGNED("Unassigned"),

    /** The assigned stage was completed successfully. */
    COMPLETED("Completed"),

    /** The associated Job was cancelled. */
    CANCELLED("Cancelled")
}
