package com.sucharu.sucharupro.domain.model.qc

/**
 * Root domain entity representing a Quality Control inspection record in Sucharu Pro ERP (Module 06).
 *
 * References an existing [com.sucharu.sucharupro.domain.model.job.ProductionJob] and optional stage
 * without duplicating Customer, Order, or Job aggregates.
 */
data class ProductionQc(
    val qcId: String,
    val productionJobId: String,
    val productionStageId: String? = null,
    val qcType: QcType,
    val status: QcStatus = QcStatus.DRAFT,
    val decision: QcDecision = QcDecision.PENDING,
    val assignedInspectorId: String? = null,
    val assignedInspectorName: String? = null,
    val createdBy: String? = null,
    val createdAt: String,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val notes: String? = null,
    val updatedAt: String,
    val updatedBy: String? = null
) {
    init {
        require(qcId.isNotBlank()) { "QC ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Update timestamp cannot be blank." }
    }

    /** Convenience flag indicating whether an inspector is assigned. */
    val isAssigned: Boolean get() = !assignedInspectorId.isNullOrBlank()

    /** Convenience flag indicating whether inspection is terminal. */
    val isTerminal: Boolean get() = status.isTerminal

    /** Convenience flag indicating whether inspection is editable. */
    val isEditable: Boolean get() = !isTerminal

    /** Convenience flag indicating whether inspection is active. */
    val isInInspection: Boolean get() = status == QcStatus.IN_INSPECTION
}
