package com.sucharu.sucharupro.domain.model.qc

/**
 * Entity representing an individual operational QC cost entry (Module 06 Step 08).
 *
 * Captures specific quality-related expenses (labor, materials, testing tools, rework cost)
 * with strict provenance linking back to originating QC entities.
 */
data class QcCostEntry(
    val id: String,
    val productionJobId: String,
    val projectId: String,
    val qcId: String? = null,
    val inspectionChecklistId: String? = null,
    val productionDefectId: String? = null,
    val productionReworkId: String? = null,
    val reQcId: String? = null,
    val finalQcId: String? = null,
    val costType: QcCostType,
    val description: String,
    val quantity: Double,
    val unitCost: Double,
    val totalCost: Double = quantity * unitCost,
    val currency: String = "BDT",
    val status: QcCostStatus = QcCostStatus.RECORDED,
    val recordedBy: String,
    val recordedByName: String? = null,
    val recordedAt: String,
    val reconciledBy: String? = null,
    val reconciledAt: String? = null,
    val adjustmentReason: String? = null,
    val sourceReferenceId: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    /** Whether this cost entry is immutable and permanently locked. */
    val isLocked: Boolean get() = status.isLocked

    /** Whether this cost entry is in a terminal state. */
    val isTerminal: Boolean get() = status.isTerminal

    /** Whether this cost entry is active and included in reconciliation. */
    val isActive: Boolean get() = status == QcCostStatus.RECORDED || status == QcCostStatus.RECONCILED || status == QcCostStatus.ADJUSTED || status == QcCostStatus.LOCKED
}
