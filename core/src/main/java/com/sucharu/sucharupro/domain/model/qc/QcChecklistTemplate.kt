package com.sucharu.sucharupro.domain.model.qc

/**
 * Reusable QC Checklist Template definition supporting deterministic versioning (Module 06 Step 03).
 */
data class QcChecklistTemplate(
    val checklistTemplateId: String,
    val name: String,
    val description: String? = null,
    val qcType: QcType,
    val applicableStageType: String? = null,
    val version: Int = 1,
    val isActive: Boolean = true,
    val createdBy: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(checklistTemplateId.isNotBlank()) { "Checklist Template ID cannot be blank." }
        require(name.isNotBlank()) { "Checklist Template Name cannot be blank." }
        require(version >= 1) { "Checklist Template version must be >= 1." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Update timestamp cannot be blank." }
    }
}
