package com.sucharu.sucharupro.domain.job.model

import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import java.util.UUID

/**
 * Immutable execution history record of a background job attempt (INFRA-04 Step 04).
 */
data class JobExecutionRecord(
    val executionId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val jobId: String,
    val workerId: String,
    val attemptNumber: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val durationMs: Long? = null,
    val status: JobStatus,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val failureClassification: EventFailureClassification? = null,
    val outputMetadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(executionId.isNotBlank()) { "executionId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(jobId.isNotBlank()) { "jobId cannot be blank" }
        require(workerId.isNotBlank()) { "workerId cannot be blank" }
        require(attemptNumber > 0) { "attemptNumber must be positive" }
    }
}
