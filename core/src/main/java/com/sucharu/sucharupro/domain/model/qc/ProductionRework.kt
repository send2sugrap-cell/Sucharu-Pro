package com.sucharu.sucharupro.domain.model.qc

import com.sucharu.sucharupro.domain.model.common.FileReference

/**
 * Primary aggregate root for Rework Management & Workflow (Module 06 Step 05).
 *
 * Tracks the complete lifecycle of corrective rework originating from detected defects
 * or failed QC inspections, enforcing strict stage, quantity, ownership, and audit controls.
 */
data class ProductionRework(
    val reworkId: String,
    val projectId: String,
    val productionJobId: String,
    val productionStageId: String? = null,
    val qcId: String? = null,
    val inspectionChecklistId: String? = null,
    val defectId: String? = null,
    val reworkType: ReworkType,
    val reason: ReworkReason,
    val status: ReworkStatus = ReworkStatus.REQUESTED,
    val affectedQuantity: Int,
    val quantityUnit: String = "pcs",
    val description: String,
    val correctiveAction: String? = null,
    val requestedBy: String,
    val requestedByName: String? = null,
    val requestedAt: String,
    val reviewedBy: String? = null,
    val reviewedByName: String? = null,
    val reviewedAt: String? = null,
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val assignedAt: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val returnedToQcAt: String? = null,
    val actualReworkedQuantity: Int? = null,
    val evidenceReferences: List<FileReference> = emptyList(),
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    /** Whether the rework is in a terminal state (CANCELLED or REJECTED). */
    val isTerminal: Boolean get() = status.isTerminal

    /** Whether an operator/technician is currently assigned to this rework. */
    val isAssigned: Boolean get() = !assignedTo.isNullOrBlank()

    /** Whether the rework has reached or passed the completed milestone. */
    val isCompleted: Boolean get() = status == ReworkStatus.COMPLETED || status == ReworkStatus.RETURNED_TO_QC

    /** Whether the rework has been handed off for Re-QC inspection. */
    val isReturnedToQc: Boolean get() = status == ReworkStatus.RETURNED_TO_QC

    /** Whether the rework is active and editable. */
    val isEditable: Boolean get() = !isTerminal && status != ReworkStatus.RETURNED_TO_QC
}
