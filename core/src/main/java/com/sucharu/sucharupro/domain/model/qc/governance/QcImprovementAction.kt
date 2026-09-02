package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Quality Improvement Action (CAPA / continuous quality improvement) entity (Module 06 Step 10).
 */
data class QcImprovementAction(
    val actionId: String,
    val projectId: String,
    val sourceAlertId: String? = null,
    val sourceReviewId: String? = null,
    val relatedJobId: String? = null,
    val relatedDefectId: String? = null,
    val ownerId: String? = null,
    val ownerName: String? = null,
    val proposedBy: String,
    val proposedByName: String? = null,
    val approvedBy: String? = null,
    val approvedByName: String? = null,
    val actionType: QcImprovementActionType,
    val priority: QcImprovementPriority = QcImprovementPriority.MEDIUM,
    val status: QcImprovementActionStatus = QcImprovementActionStatus.PROPOSED,
    val title: String,
    val description: String,
    val expectedOutcome: String? = null,
    val dueDate: String? = null,
    val completedAt: String? = null,
    val completionNotes: String? = null,
    val verifiedAt: String? = null,
    val verifiedBy: String? = null,
    val verifiedByName: String? = null,
    val verificationNotes: String? = null,
    val effectiveness: QcImprovementEffectiveness = QcImprovementEffectiveness.NOT_EVALUATED,
    val baselineKpiValue: Double? = null,
    val postImprovementKpiValue: Double? = null,
    val createdAt: String,
    val updatedAt: String
) {
    val isTerminal: Boolean get() = status.isTerminal

    init {
        require(actionId.isNotBlank()) { "Action ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(proposedBy.isNotBlank()) { "ProposedBy cannot be blank" }
        require(title.isNotBlank()) { "Action title cannot be blank" }
        require(description.isNotBlank()) { "Action description cannot be blank" }
        require(createdAt.isNotBlank()) { "CreatedAt timestamp cannot be blank" }
    }
}
