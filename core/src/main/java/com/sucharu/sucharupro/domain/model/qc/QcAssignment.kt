package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable inspector assignment record for a [ProductionQc], preserving complete assignment history.
 */
data class QcAssignment(
    val assignmentId: String,
    val qcId: String,
    val inspectorId: String,
    val inspectorName: String,
    val assignedBy: String? = null,
    val assignedAt: String,
    val unassignedAt: String? = null,
    val isActive: Boolean = true,
    val reason: String? = null
) {
    init {
        require(assignmentId.isNotBlank()) { "Assignment ID cannot be blank." }
        require(qcId.isNotBlank()) { "QC ID cannot be blank." }
        require(inspectorId.isNotBlank()) { "Inspector ID cannot be blank." }
        require(inspectorName.isNotBlank()) { "Inspector Name cannot be blank." }
        require(assignedAt.isNotBlank()) { "Assigned timestamp cannot be blank." }
    }
}
