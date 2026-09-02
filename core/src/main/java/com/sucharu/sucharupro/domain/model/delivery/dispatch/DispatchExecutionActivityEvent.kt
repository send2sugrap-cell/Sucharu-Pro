package com.sucharu.sucharupro.domain.model.delivery.dispatch

/**
 * Immutable audit log event for Dispatch Executions (Module 08 Step 03).
 */
data class DispatchExecutionActivityEvent(
    val activityId: String,
    val projectId: String,
    val dispatchExecutionId: String,
    val activityType: DispatchExecutionActivityType,
    val performedBy: String,
    val performedAt: Long,
    val details: String? = null,
    val referenceId: String? = null,
    val previousStatus: String? = null,
    val newStatus: String? = null
) {
    init {
        require(activityId.isNotBlank()) { "Activity ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(dispatchExecutionId.isNotBlank()) { "Dispatch Execution ID cannot be blank." }
        require(performedBy.isNotBlank()) { "Performed By actor cannot be blank." }
        require(performedAt > 0) { "Performed At timestamp must be positive." }
    }
}
