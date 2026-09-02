package com.sucharu.sucharupro.domain.workflow.model

import java.util.UUID

/**
 * Compensation lifecycle status for multi-step saga rollbacks (INFRA-04 Step 05).
 */
enum class CompensationStatus(val isTerminal: Boolean) {
    COMPENSATION_PENDING(isTerminal = false),
    COMPENSATING(isTerminal = false),
    COMPENSATED(isTerminal = true),
    COMPENSATION_FAILED(isTerminal = true)
}

/**
 * Immutable record of a step compensation execution.
 */
data class WorkflowCompensationRecord(
    val compensationId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val workflowId: String,
    val stepId: String,
    val stepExecutionId: String,
    val status: CompensationStatus = CompensationStatus.COMPENSATION_PENDING,
    val attemptNumber: Int = 1,
    val payloadJson: String? = null,
    val resultMessage: String? = null,
    val errorMessage: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Structured workflow failure record.
 */
data class WorkflowFailureRecord(
    val failureId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val workflowId: String,
    val stepId: String,
    val stepExecutionId: String,
    val errorMessage: String,
    val failureClassification: String,
    val requiresCompensation: Boolean = true,
    val occurredAt: Long = System.currentTimeMillis()
)
