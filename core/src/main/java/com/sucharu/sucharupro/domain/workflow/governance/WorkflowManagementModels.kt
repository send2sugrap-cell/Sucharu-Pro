package com.sucharu.sucharupro.domain.workflow.governance

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.workflow.model.*

/**
 * High-level operational summary of the Workflow Engine for a tenant (INFRA-04 Step 06).
 */
data class WorkflowManagementSummary(
    val projectId: String,
    val totalDefinitions: Int,
    val totalActiveVersions: Int,
    val totalRunningInstances: Int,
    val totalWaitingApprovals: Int,
    val totalFailedInstances: Int,
    val totalDeadLetterInstances: Int,
    val totalCompletedToday: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Summary representation of a WorkflowDefinition for API and UI layers.
 */
data class WorkflowDefinitionSummary(
    val definitionId: String,
    val projectId: String,
    val name: String,
    val description: String?,
    val category: String,
    val isEnabled: Boolean,
    val latestVersion: Int,
    val activeVersionId: String?,
    val totalInstances: Int,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Summary of a specific WorkflowVersion.
 */
data class WorkflowVersionSummary(
    val versionId: String,
    val definitionId: String,
    val versionNumber: Int,
    val isPublished: Boolean,
    val isDeprecated: Boolean,
    val isArchived: Boolean,
    val stepCount: Int,
    val description: String?,
    val publishedAt: Long?,
    val publishedBy: String?,
    val createdAt: Long
)

/**
 * Summary representation of a WorkflowInstance.
 */
data class WorkflowInstanceSummary(
    val workflowId: String,
    val projectId: String,
    val definitionId: String,
    val definitionName: String,
    val versionId: String,
    val versionNumber: Int,
    val status: WorkflowStatus,
    val currentStepId: String?,
    val currentStepName: String?,
    val progressPercent: Int,
    val actorType: PrincipalType,
    val actorId: String,
    val startedAt: Long,
    val completedAt: Long?,
    val updatedAt: Long
)

/**
 * Step execution summary within an instance.
 */
data class WorkflowStepExecutionSummary(
    val executionId: String,
    val workflowId: String,
    val stepId: String,
    val stepName: String,
    val stepType: WorkflowStepType,
    val status: StepExecutionStatus,
    val attempt: Int,
    val durationMs: Long?,
    val errorDetails: String?,
    val startedAt: Long,
    val completedAt: Long?
)

/**
 * Timeline event type classification for execution traceability.
 */
enum class TimelineEventType {
    DOMAIN_EVENT_RECEIVED,
    OUTBOX_RECORDED,
    OUTBOX_DISPATCHED,
    WORKFLOW_TRIGGERED,
    WORKFLOW_STARTED,
    STEP_STARTED,
    STEP_COMPLETED,
    STEP_FAILED,
    RETRY_SCHEDULED,
    BACKGROUND_JOB_ENQUEUED,
    BACKGROUND_JOB_COMPLETED,
    APPROVAL_REQUESTED,
    APPROVAL_DECIDED,
    APPROVAL_ESCALATED,
    COMPENSATION_TRIGGERED,
    COMPENSATION_COMPLETED,
    WORKFLOW_PAUSED,
    WORKFLOW_RESUMED,
    WORKFLOW_COMPLETED,
    WORKFLOW_FAILED,
    WORKFLOW_CANCELLED,
    DEAD_LETTER_QUARANTINED,
    ADMIN_REPLAYED
}

/**
 * Discrete chronological event along a workflow's execution timeline.
 */
data class WorkflowTimelineEvent(
    val eventId: String,
    val eventType: TimelineEventType,
    val title: String,
    val description: String,
    val stepId: String? = null,
    val stepName: String? = null,
    val actorId: String? = null,
    val actorRole: UserRole? = null,
    val durationMs: Long? = null,
    val correlationId: String? = null,
    val causationId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * End-to-end trace timeline of a workflow instance.
 */
data class WorkflowExecutionTimeline(
    val workflowId: String,
    val projectId: String,
    val definitionId: String,
    val definitionName: String,
    val status: WorkflowStatus,
    val totalDurationMs: Long?,
    val events: List<WorkflowTimelineEvent>
)

/**
 * Failure summary for diagnosing stopped/dead-letter workflows.
 */
data class WorkflowFailureSummary(
    val workflowId: String,
    val failedStepId: String?,
    val failedStepName: String?,
    val failureClassification: String,
    val errorMessage: String,
    val retryCount: Int,
    val maxRetries: Int,
    val isCompensated: Boolean,
    val canReplay: Boolean,
    val failedAt: Long
)

/**
 * Summary of a pending or decided approval request.
 */
data class WorkflowApprovalSummary(
    val approvalId: String,
    val workflowId: String,
    val stepId: String,
    val policyId: String,
    val policyName: String,
    val requiredRole: UserRole,
    val status: ApprovalStatus,
    val requesterId: String,
    val requesterRole: UserRole,
    val approvalsReceived: Int,
    val approvalsRequired: Int,
    val allowSelfApproval: Boolean,
    val isEscalated: Boolean,
    val timeoutAt: Long?,
    val createdAt: Long
)

/**
 * Compensation summary record for rolled back sagas.
 */
data class WorkflowCompensationSummary(
    val compensationId: String,
    val workflowId: String,
    val stepId: String,
    val compensationStepId: String,
    val status: CompensationStatus,
    val failureReason: String?,
    val completedAt: Long?
)

/**
 * Background job linkage summary.
 */
data class WorkflowJobSummary(
    val jobId: String,
    val workflowId: String,
    val stepId: String,
    val jobType: String,
    val status: String,
    val retryCount: Int,
    val durationMs: Long?
)

/**
 * Tenant-scoped operational telemetry metrics.
 */
data class WorkflowOperationalMetrics(
    val projectId: String,
    val activeWorkflows: Int,
    val completedWorkflows: Int,
    val failedWorkflows: Int,
    val cancelledWorkflows: Int,
    val pausedWorkflows: Int,
    val pendingApprovals: Int,
    val compensatedSagas: Int,
    val deadLetterCount: Int,
    val averageWorkflowDurationMs: Double,
    val averageStepDurationMs: Double,
    val failureRatePercent: Double,
    val throughputPerMinute: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Immutable audit log entry for governance actions.
 */
data class WorkflowAuditEntry(
    val auditId: String,
    val projectId: String,
    val actorId: String,
    val actorRole: UserRole,
    val principalType: PrincipalType,
    val operation: String,
    val targetType: String,
    val targetId: String,
    val previousState: String?,
    val newState: String?,
    val details: String?,
    val clientIp: String,
    val correlationId: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Recovery operation description.
 */
data class WorkflowRecoveryOperation(
    val operationId: String,
    val workflowId: String,
    val operationType: String,
    val initiatedBy: String,
    val status: String,
    val message: String?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Filter criteria for querying workflow instances.
 */
data class WorkflowFilterCriteria(
    val status: WorkflowStatus? = null,
    val definitionId: String? = null,
    val versionId: String? = null,
    val actorId: String? = null,
    val correlationId: String? = null,
    val fromTimestamp: Long? = null,
    val toTimestamp: Long? = null,
    val searchQuery: String? = null,
    val page: Int = 1,
    val pageSize: Int = 20
)

/**
 * Generic paginated response container.
 */
data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int
)

// Request DTOs
data class WorkflowPauseRequest(val reason: String? = null)
data class WorkflowResumeRequest(val contextUpdates: Map<String, String> = emptyMap())
data class WorkflowCancelRequest(val reason: String)
data class WorkflowRetryRequest(val targetStepId: String? = null)
data class WorkflowReplayRequest(val checkpointStepId: String? = null, val contextOverrides: Map<String, String> = emptyMap())
data class WorkflowCompensationRequest(val reason: String)

data class CreateWorkflowDefinitionRequest(
    val name: String,
    val description: String?,
    val category: String,
    val initialSteps: List<WorkflowStepDefinition> = emptyList()
)

data class UpdateWorkflowDefinitionRequest(
    val name: String?,
    val description: String?,
    val isEnabled: Boolean?
)

data class CreateWorkflowVersionRequest(
    val description: String?,
    val steps: List<WorkflowStepDefinition>
)

data class WorkflowApprovalDecisionDto(
    val decision: ApprovalDecisionType,
    val notes: String? = null,
    val humanConfirmation: HumanConfirmationMetadata? = null
)

data class WorkflowApprovalEscalationDto(
    val targetRole: UserRole,
    val reason: String
)
