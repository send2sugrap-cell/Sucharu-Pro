package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable assignment record tracking defect ownership and responsibility (Module 06 Step 04).
 */
data class DefectAssignment(
    val assignmentId: String,
    val defectId: String,
    val assigneeId: String,
    val assigneeName: String,
    val assignedBy: String,
    val assignedAt: String,
    val active: Boolean = true,
    val reason: String? = null
) {
    init {
        require(assignmentId.isNotBlank()) { "Assignment ID cannot be blank." }
        require(defectId.isNotBlank()) { "Defect ID cannot be blank." }
        require(assigneeId.isNotBlank()) { "Assignee ID cannot be blank." }
        require(assigneeName.isNotBlank()) { "Assignee Name cannot be blank." }
        require(assignedBy.isNotBlank()) { "AssignedBy cannot be blank." }
        require(assignedAt.isNotBlank()) { "AssignedAt timestamp cannot be blank." }
    }
}
