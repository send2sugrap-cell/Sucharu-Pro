package com.sucharu.sucharupro.domain.model.design

/**
 * Status indicator for an individual designer assignment record.
 */
enum class DesignAssignmentStatus(val defaultLabel: String) {
    ACTIVE("Active"),
    REASSIGNED("Reassigned"),
    UNASSIGNED("Unassigned")
}

/**
 * Immutable domain model recording designer assignment history for a Design Project.
 */
data class DesignAssignment(
    val assignmentId: String,
    val projectId: String,
    val designerId: String,
    val designerName: String,
    val assignedAt: String,
    val assignedBy: String? = null,
    val reassignedAt: String? = null,
    val reassignedBy: String? = null,
    val unassignedAt: String? = null,
    val unassignedBy: String? = null,
    val status: DesignAssignmentStatus = DesignAssignmentStatus.ACTIVE,
    val notes: String? = null
) {
    init {
        require(assignmentId.isNotBlank()) { "Assignment ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(designerId.isNotBlank()) { "Designer ID cannot be blank." }
        require(designerName.isNotBlank()) { "Designer Name cannot be blank." }
        require(assignedAt.isNotBlank()) { "Assigned timestamp cannot be blank." }
    }

    val isActive: Boolean get() = status == DesignAssignmentStatus.ACTIVE
}
