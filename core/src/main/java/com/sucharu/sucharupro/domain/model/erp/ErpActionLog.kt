package com.sucharu.sucharupro.domain.model.erp

/**
 * Action log entry for ERP workflow transitions.
 */
data class ErpActionLog(
    val logId: String,
    val orchestrationId: String,
    val fromStatus: ErpWorkflowStatus,
    val toStatus: ErpWorkflowStatus,
    val actionType: String,
    val performedBy: String,
    val actionNotes: String? = null,
    val occurredAt: String
) {
    init {
        require(logId.isNotBlank()) { "Log ID cannot be blank." }
        require(orchestrationId.isNotBlank()) { "Orchestration ID cannot be blank." }
        require(performedBy.isNotBlank()) { "performedBy actor cannot be blank." }
        require(occurredAt.isNotBlank()) { "occurredAt timestamp cannot be blank." }
    }
}
