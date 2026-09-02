package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Immutable domain model recording operator assignment history for a production stage.
 */
data class ProductionStageAssignment(
    val assignmentId: String,
    val jobId: String,
    val stageId: String,
    val stageType: ProductionStageType,
    val operatorId: String,
    val operatorName: String,
    val assignedAt: String,
    val assignedBy: String? = null,
    val reassignedAt: String? = null,
    val reassignedBy: String? = null,
    val status: StageAssignmentStatus = StageAssignmentStatus.ASSIGNED,
    val notes: String? = null
) {
    init {
        require(assignmentId.isNotBlank()) { "Assignment ID cannot be blank." }
        require(jobId.isNotBlank()) { "Job ID cannot be blank." }
        require(stageId.isNotBlank()) { "Stage ID cannot be blank." }
        require(operatorId.isNotBlank()) { "Operator ID cannot be blank." }
        require(operatorName.isNotBlank()) { "Operator Name cannot be blank." }
        require(assignedAt.isNotBlank()) { "Assigned timestamp cannot be blank." }
    }

    val isActive: Boolean get() = status == StageAssignmentStatus.ASSIGNED
}
