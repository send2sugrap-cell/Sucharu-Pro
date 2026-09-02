package com.sucharu.sucharupro.domain.model.design

/**
 * Status of an individual proof revision request.
 */
enum class RevisionRequestStatus(val defaultLabel: String) {
    /** Request created, waiting for designer to start work. */
    OPEN("Open"),

    /** Designer has started revision work. */
    IN_PROGRESS("In Progress"),

    /** Revisions completed and new proof version resubmitted. (NOT an approval). */
    RESOLVED("Resolved"),

    /** Revision request cancelled or superseded. */
    CANCELLED("Cancelled");

    val isPending: Boolean get() = this == OPEN || this == IN_PROGRESS
    val isResolved: Boolean get() = this == RESOLVED
}
