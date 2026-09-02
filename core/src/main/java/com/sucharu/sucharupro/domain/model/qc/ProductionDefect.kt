package com.sucharu.sucharupro.domain.model.qc

/**
 * Primary aggregate root for QC Defect & Failure Management (Module 06 Step 04).
 *
 * Captures comprehensive failure detection, categorization, severity, affected quantity,
 * ownership assignment, containment, resolution, and immutable lifecycle history.
 */
data class ProductionDefect(
    val defectId: String,
    val productionJobId: String,
    val productionStageId: String? = null,
    val qcId: String? = null,
    val inspectionChecklistId: String? = null,
    val checklistItemId: String? = null,
    val category: DefectCategory,
    val severity: DefectSeverity,
    val source: DefectSource,
    val status: DefectStatus = DefectStatus.OPEN,
    val title: String,
    val description: String,
    val affectedQuantity: Int,
    val affectedUnit: String = "pcs",
    val detectedAt: String,
    val detectedBy: String,
    val detectedByName: String? = null,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: String? = null,
    val containmentNotes: String? = null,
    val resolutionNotes: String? = null,
    val resolvedBy: String? = null,
    val resolvedAt: String? = null,
    val closedBy: String? = null,
    val closedAt: String? = null,
    val assignedToId: String? = null,
    val assignedToName: String? = null,
    val evidenceList: List<DefectEvidence> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val notes: String? = null
) {
    /** Whether the defect is in a terminal state (CLOSED or CANCELLED). */
    val isTerminal: Boolean get() = status.isTerminal

    /** Whether the defect has been resolved or closed. */
    val isResolved: Boolean get() = status == DefectStatus.RESOLVED || status == DefectStatus.CLOSED

    /** Whether this is a critical severity failure. */
    val isCritical: Boolean get() = severity == DefectSeverity.CRITICAL

    /** Whether an inspector or technician is currently assigned to this defect. */
    val isAssigned: Boolean get() = !assignedToId.isNullOrBlank()
}
