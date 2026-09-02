package com.sucharu.sucharupro.domain.model.qc

/**
 * Inspection session record capturing inspector execution and final decision on a [ProductionQc].
 */
data class QcInspection(
    val inspectionId: String,
    val qcId: String,
    val inspectorId: String,
    val inspectorName: String? = null,
    val startedAt: String,
    val completedAt: String? = null,
    val decision: QcDecision = QcDecision.PENDING,
    val notes: String? = null
) {
    init {
        require(inspectionId.isNotBlank()) { "Inspection ID cannot be blank." }
        require(qcId.isNotBlank()) { "QC ID cannot be blank." }
        require(inspectorId.isNotBlank()) { "Inspector ID cannot be blank." }
        require(startedAt.isNotBlank()) { "Started timestamp cannot be blank." }
    }

    /** Indicates whether this inspection session has completed. */
    val isCompleted: Boolean get() = !completedAt.isNullOrBlank() && decision != QcDecision.PENDING
}
