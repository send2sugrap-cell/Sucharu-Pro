package com.sucharu.sucharupro.domain.model.returns

/**
 * Domain entity representing the quality/physical inspection performed on a Return Request.
 * Part of Module 11 Step 03 – Return Inspection & Decision Management.
 */
data class ReturnInspection(
    val inspectionId: String,
    val returnId: String,
    val projectId: String,
    val inspectorId: String,
    val status: ReturnInspectionStatus = ReturnInspectionStatus.PENDING,
    val checklist: List<InspectionChecklistItem> = emptyList(),
    val findings: String? = null,
    val decision: ReturnDecision? = null,
    val decisionReason: String? = null,
    val inspectedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
) {
    init {
        require(inspectionId.isNotBlank()) { "Inspection ID cannot be blank." }
        require(returnId.isNotBlank()) { "Return ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(inspectorId.isNotBlank()) { "Inspector ID cannot be blank." }
        require(inspectedAt > 0) { "Inspected At timestamp must be positive." }
        require(createdAt > 0) { "Created At timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated At timestamp cannot precede Created At." }
        require(version > 0) { "Version must be strictly positive." }

        if (status == ReturnInspectionStatus.COMPLETED) {
            require(decision != null) { "A decision (APPROVE or REJECT) is required when completing an inspection." }
            if (decision == ReturnDecision.REJECT) {
                require(!decisionReason.isNullOrBlank()) { "Decision reason is required when rejecting a return." }
            }
        }
    }
}
