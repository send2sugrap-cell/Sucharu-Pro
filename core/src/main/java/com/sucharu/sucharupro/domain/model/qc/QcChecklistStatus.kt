package com.sucharu.sucharupro.domain.model.qc

/**
 * Lifecycle status of a concrete QC Inspection Checklist instance (Module 06 Step 03).
 */
enum class QcChecklistStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    READY("Ready for Inspection"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean get() = this == COMPLETED || this == CANCELLED
    val isEditable: Boolean get() = !isTerminal

    fun canTransitionTo(target: QcChecklistStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal) return false

        return when (this) {
            DRAFT -> target == READY || target == CANCELLED
            READY -> target == IN_PROGRESS || target == CANCELLED
            IN_PROGRESS -> target == COMPLETED || target == CANCELLED
            COMPLETED -> false
            CANCELLED -> false
        }
    }
}
