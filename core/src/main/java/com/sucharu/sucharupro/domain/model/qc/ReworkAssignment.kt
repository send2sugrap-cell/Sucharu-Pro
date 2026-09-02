package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable assignment record tracking rework ownership and execution responsibility (Module 06 Step 05).
 */
data class ReworkAssignment(
    val assignmentId: String,
    val reworkId: String,
    val assignedTo: String,
    val assignedToName: String,
    val assignedBy: String,
    val assignedByName: String? = null,
    val assignedAt: String,
    val unassignedAt: String? = null,
    val active: Boolean = true,
    val notes: String? = null
) {
    init {
        require(assignmentId.isNotBlank()) { "Assignment ID cannot be blank." }
        require(reworkId.isNotBlank()) { "Rework ID cannot be blank." }
        require(assignedTo.isNotBlank()) { "AssignedTo ID cannot be blank." }
        require(assignedToName.isNotBlank()) { "AssignedTo Name cannot be blank." }
        require(assignedBy.isNotBlank()) { "AssignedBy cannot be blank." }
        require(assignedAt.isNotBlank()) { "AssignedAt timestamp cannot be blank." }
    }
}
