package com.sucharu.sucharupro.domain.workflow.model

import com.sucharu.sucharupro.data.api.model.PrincipalType
import java.util.UUID

/**
 * Strict workflow lifecycle states (INFRA-04 Step 05).
 */
enum class WorkflowStatus(val isTerminal: Boolean, val canResume: Boolean) {
    DRAFT(isTerminal = false, canResume = false),
    ACTIVE(isTerminal = false, canResume = true),
    RUNNING(isTerminal = false, canResume = false),
    WAITING(isTerminal = false, canResume = true),
    WAITING_APPROVAL(isTerminal = false, canResume = true),
    PAUSED(isTerminal = false, canResume = true),
    COMPENSATING(isTerminal = false, canResume = false),
    COMPLETED(isTerminal = true, canResume = false),
    FAILED(isTerminal = true, canResume = false),
    CANCELLED(isTerminal = true, canResume = false),
    TIMED_OUT(isTerminal = true, canResume = false),
    DEAD_LETTER(isTerminal = true, canResume = true); // Can be manually replayed/resumed by admin
}

/**
 * Supported workflow step types (INFRA-04 Step 05).
 */
enum class WorkflowStepType {
    ACTION,
    EVENT_WAIT,
    JOB,
    CONDITION,
    DELAY,
    APPROVAL,
    NOTIFICATION,
    WEBHOOK,
    COMPENSATION,
    END
}

/**
 * Execution status for individual workflow steps.
 */
enum class StepExecutionStatus(val isTerminal: Boolean) {
    PENDING(isTerminal = false),
    RUNNING(isTerminal = false),
    WAITING(isTerminal = false),
    SUCCEEDED(isTerminal = true),
    FAILED(isTerminal = true),
    SKIPPED(isTerminal = true),
    COMPENSATED(isTerminal = true)
}

/**
 * Workflow definition template.
 */
data class WorkflowDefinition(
    val definitionId: String,
    val projectId: String,
    val workflowName: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    init {
        require(definitionId.isNotBlank()) { "definitionId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(workflowName.isNotBlank()) { "workflowName cannot be blank" }
        require(createdBy.isNotBlank()) { "createdBy cannot be blank" }
    }
}

/**
 * Specific versioned specification of a workflow.
 */
data class WorkflowVersion(
    val definitionId: String,
    val projectId: String,
    val versionId: String,
    val steps: List<WorkflowStepDefinition>,
    val definitionJson: String = "{}",
    val isActive: Boolean = true,
    val publishedAt: Long = System.currentTimeMillis(),
    val publishedBy: String
) {
    init {
        require(definitionId.isNotBlank()) { "definitionId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(versionId.isNotBlank()) { "versionId cannot be blank" }
        require(publishedBy.isNotBlank()) { "publishedBy cannot be blank" }
    }
}

/**
 * Declarative definition of an individual step in a workflow version.
 */
data class WorkflowStepDefinition(
    val stepId: String,
    val definitionId: String,
    val versionId: String,
    val projectId: String,
    val stepName: String,
    val stepType: WorkflowStepType,
    val sequenceOrder: Int,
    val config: Map<String, String> = emptyMap(),
    val retryPolicy: StepRetryPolicy? = null,
    val timeoutMs: Long = 60000L,
    val compensationStepId: String? = null,
    val requiredCapability: String? = null
) {
    init {
        require(stepId.isNotBlank()) { "stepId cannot be blank" }
        require(stepName.isNotBlank()) { "stepName cannot be blank" }
        require(sequenceOrder >= 0) { "sequenceOrder cannot be negative" }
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
    }
}

/**
 * Retry policy for a workflow step.
 */
data class StepRetryPolicy(
    val maxAttempts: Int = 3,
    val initialBackoffMs: Long = 1000L,
    val maxBackoffMs: Long = 30000L,
    val multiplier: Double = 2.0
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(initialBackoffMs > 0) { "initialBackoffMs must be positive" }
        require(maxBackoffMs >= initialBackoffMs) { "maxBackoffMs must be >= initialBackoffMs" }
        require(multiplier >= 1.0) { "multiplier must be >= 1.0" }
    }
}

/**
 * Concrete running instance of a workflow.
 */
data class WorkflowInstance(
    val workflowId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val definitionId: String,
    val versionId: String,
    val executionId: String = UUID.randomUUID().toString(),
    val status: WorkflowStatus = WorkflowStatus.DRAFT,
    val currentStepId: String? = null,
    val context: Map<String, String> = emptyMap(),
    val correlationId: String = UUID.randomUUID().toString(),
    val causationId: String? = null,
    val requestId: String? = null,
    val actorType: PrincipalType = PrincipalType.HUMAN,
    val actorId: String,
    val principalType: PrincipalType = actorType,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val completedAt: Long? = null,
    val failedAt: Long? = null,
    val errorMessage: String? = null
) {
    init {
        require(workflowId.isNotBlank()) { "workflowId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(definitionId.isNotBlank()) { "definitionId cannot be blank" }
        require(versionId.isNotBlank()) { "versionId cannot be blank" }
        require(executionId.isNotBlank()) { "executionId cannot be blank" }
        require(actorId.isNotBlank()) { "actorId cannot be blank" }
        require(correlationId.isNotBlank()) { "correlationId cannot be blank" }
    }
}

/**
 * Execution record for a specific step invocation.
 */
data class WorkflowStepExecution(
    val stepExecutionId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val workflowId: String,
    val executionId: String,
    val stepId: String,
    val stepName: String,
    val stepType: WorkflowStepType,
    val status: StepExecutionStatus = StepExecutionStatus.PENDING,
    val attemptNumber: Int = 1,
    val inputJson: String? = null,
    val outputJson: String? = null,
    val errorMessage: String? = null,
    val failureClassification: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Immutable state transition audit record.
 */
data class WorkflowTransition(
    val transitionId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val workflowId: String,
    val executionId: String,
    val fromStatus: WorkflowStatus,
    val toStatus: WorkflowStatus,
    val triggerType: String,
    val actorType: PrincipalType = PrincipalType.HUMAN,
    val actorId: String,
    val principalType: PrincipalType = actorType,
    val metadata: Map<String, String> = emptyMap(),
    val transitionedAt: Long = System.currentTimeMillis()
)
