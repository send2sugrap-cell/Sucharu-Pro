package com.sucharu.sucharupro.domain.model.qc

/**
 * Primary aggregate root for Re-QC & Failure Loops (Module 06 Step 06).
 *
 * Represents ONE specific immutable Re-QC inspection cycle.
 * Maintains full traceability to original QC, defect, rework, checklist, and previous Re-QC cycle.
 */
data class ReQcInspection(
    val reQcId: String,
    val productionJobId: String,
    val projectId: String,
    val productionReworkId: String,
    val originalQcId: String? = null,
    val originalDefectId: String? = null,
    val checklistId: String? = null,
    val previousReQcId: String? = null,
    val cycleNumber: Int,
    val cycleType: ReQcCycleType = ReQcCycleType.POST_REWORK,
    val status: ReQcStatus = ReQcStatus.PENDING,
    val decision: ReQcDecision = ReQcDecision.PENDING,
    val assignedInspectorId: String? = null,
    val assignedInspectorName: String? = null,
    val assignedAt: String? = null,
    val createdAt: String,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val failureReason: ReQcFailureReason? = null,
    val failureNotes: String? = null,
    val passNotes: String? = null,
    val returnedToReworkAt: String? = null,
    val createdBy: String,
    val createdByName: String? = null,
    val affectedQuantity: Int? = null,
    val quantityUnit: String = "pcs",
    val notes: String? = null,
    val updatedAt: String
) {
    /** Whether this cycle is in a terminal state (PASSED or CANCELLED). */
    val isTerminal: Boolean get() = status.isTerminal

    /** Whether an inspector is assigned to this Re-QC cycle. */
    val isAssigned: Boolean get() = !assignedInspectorId.isNullOrBlank()

    /** Whether this cycle has completed inspection. */
    val isCompleted: Boolean get() = status == ReQcStatus.PASSED || status == ReQcStatus.FAILED || status == ReQcStatus.RETURNED_TO_REWORK

    /** Whether this cycle resulted in a PASS. */
    val isPassed: Boolean get() = status == ReQcStatus.PASSED && decision == ReQcDecision.PASS

    /** Whether this cycle resulted in a FAIL. */
    val isFailed: Boolean get() = status == ReQcStatus.FAILED || status == ReQcStatus.RETURNED_TO_REWORK || decision == ReQcDecision.FAIL

    /** Whether the cycle is active and editable. */
    val isEditable: Boolean get() = !isTerminal && status != ReQcStatus.RETURNED_TO_REWORK && status != ReQcStatus.FAILED
}
