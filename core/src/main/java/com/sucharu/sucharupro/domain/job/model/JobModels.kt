package com.sucharu.sucharupro.domain.job.model

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import java.util.UUID

/**
 * Status lifecycle of a background job (INFRA-04 Step 04).
 */
enum class JobStatus {
    QUEUED,
    CLAIMED,
    RUNNING,
    WAITING,            // Waiting for dependencies to be satisfied
    RETRY_SCHEDULED,
    SUCCEEDED,
    FAILED,
    DEAD_LETTER,
    CANCELLED,
    EXPIRED;

    val isTerminal: Boolean get() = this == SUCCEEDED || this == DEAD_LETTER || this == CANCELLED || this == EXPIRED
    val isPendingOrRunning: Boolean get() = this == QUEUED || this == CLAIMED || this == RUNNING || this == RETRY_SCHEDULED
}

/**
 * Priority classification with deterministic execution weight.
 */
enum class JobPriority(val weight: Int) {
    CRITICAL(1),
    HIGH(2),
    NORMAL(3),
    LOW(4)
}

/**
 * Mechanism that triggered the background job.
 */
enum class JobTriggerType {
    EVENT,
    SCHEDULE,
    DELAY,
    MANUAL,
    API,
    N8N,
    AI_AGENT,
    WORKFLOW,
    SYSTEM
}

/**
 * Worker lease details on an actively executing job.
 */
data class JobLease(
    val leaseId: String = UUID.randomUUID().toString(),
    val workerId: String,
    val acquiredAt: Long = System.currentTimeMillis(),
    val expiresAt: Long
)

/**
 * Result of executing a job handler.
 */
sealed class JobResult {
    data class Success(
        val message: String = "Job completed successfully",
        val outputMetadata: Map<String, String> = emptyMap()
    ) : JobResult()

    data class Failure(
        val reason: String,
        val classification: EventFailureClassification = EventFailureClassification.TRANSIENT,
        val retryAfterMs: Long? = null,
        val cause: Throwable? = null
    ) : JobResult() {
        val isRetryable: Boolean get() = classification.isRetryable
    }

    data class Cancelled(
        val reason: String = "Job cancelled by operator or system"
    ) : JobResult()
}

/**
 * Canonical server-authoritative Background Job definition (INFRA-04 Step 04).
 */
data class JobDefinition(
    val jobId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val jobType: String,
    val jobVersion: String = "v1",
    val triggerType: JobTriggerType = JobTriggerType.SYSTEM,
    val priority: JobPriority = JobPriority.NORMAL,
    val status: JobStatus = JobStatus.QUEUED,
    val attemptCount: Int = 0,
    val maxAttempts: Int = 3,
    val scheduledAt: Long = System.currentTimeMillis(),
    val availableAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val nextAttemptAt: Long? = null,
    val claimedByWorker: String? = null,
    val claimedAt: Long? = null,
    val leaseExpiresAt: Long? = null,
    val payloadJson: String = "{}",
    val metadata: Map<String, String> = emptyMap(),
    val correlationId: String = UUID.randomUUID().toString(),
    val causationId: String? = null,
    val requestId: String? = null,
    val actorType: PrincipalType = PrincipalType.SYSTEM,
    val actorId: String = "SYSTEM",
    val principalType: PrincipalType = PrincipalType.SYSTEM,
    val source: String = "sucharu-pro-backend",
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val failureClassification: EventFailureClassification? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(jobId.isNotBlank()) { "jobId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(jobType.isNotBlank()) { "jobType cannot be blank" }
        require(jobVersion.isNotBlank()) { "jobVersion cannot be blank" }
        require(correlationId.isNotBlank()) { "correlationId cannot be blank" }
        require(maxAttempts > 0) { "maxAttempts must be greater than 0" }
        require(attemptCount >= 0) { "attemptCount cannot be negative" }
    }
}
