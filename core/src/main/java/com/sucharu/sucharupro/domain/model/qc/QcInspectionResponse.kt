package com.sucharu.sucharupro.domain.model.qc

/**
 * Domain entity capturing an inspector's recorded response to a specific checklist item (Module 06 Step 03).
 */
data class QcInspectionResponse(
    val responseId: String,
    val inspectionId: String,
    val checklistItemId: String,
    val status: QcResponseStatus = QcResponseStatus.PENDING,
    val value: String? = null,
    val numericValue: Double? = null,
    val selectedValue: String? = null,
    val remarks: String? = null,
    val respondedBy: String,
    val respondedByName: String? = null,
    val respondedAt: String
) {
    init {
        require(responseId.isNotBlank()) { "Response ID cannot be blank." }
        require(inspectionId.isNotBlank()) { "Inspection ID cannot be blank." }
        require(checklistItemId.isNotBlank()) { "Checklist Item ID cannot be blank." }
        require(respondedBy.isNotBlank()) { "RespondedBy cannot be blank." }
        require(respondedAt.isNotBlank()) { "RespondedAt timestamp cannot be blank." }
    }

    /** Indicates whether this response is evaluated. */
    val isEvaluated: Boolean get() = status != QcResponseStatus.PENDING
}
