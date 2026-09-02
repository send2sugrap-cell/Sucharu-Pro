package com.sucharu.sucharupro.domain.model.qc

/**
 * Concrete checklist instance assigned to an active QC Inspection session, preserving template version (Module 06 Step 03).
 */
data class QcInspectionChecklist(
    val inspectionChecklistId: String,
    val inspectionId: String,
    val checklistTemplateId: String,
    val checklistTemplateVersion: Int,
    val productionJobId: String,
    val productionQcId: String,
    val productionStageId: String? = null,
    val status: QcChecklistStatus = QcChecklistStatus.DRAFT,
    val createdAt: String,
    val completedAt: String? = null,
    val notes: String? = null
) {
    init {
        require(inspectionChecklistId.isNotBlank()) { "Inspection Checklist ID cannot be blank." }
        require(inspectionId.isNotBlank()) { "Inspection ID cannot be blank." }
        require(checklistTemplateId.isNotBlank()) { "Checklist Template ID cannot be blank." }
        require(checklistTemplateVersion >= 1) { "Checklist Template Version must be >= 1." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(productionQcId.isNotBlank()) { "Production QC ID cannot be blank." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
    }

    val isTerminal: Boolean get() = status.isTerminal
    val isEditable: Boolean get() = !isTerminal
}
