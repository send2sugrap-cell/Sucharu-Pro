package com.sucharu.sucharupro.domain.job.worker

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobResult
import com.sucharu.sucharupro.domain.job.model.JobTriggerType

/**
 * Authoritative runtime context passed to a [JobHandler] during background execution.
 */
data class JobExecutionContext(
    val jobId: String,
    val projectId: String,
    val jobType: String,
    val jobVersion: String,
    val triggerType: JobTriggerType,
    val priority: JobPriority,
    val attemptNumber: Int,
    val maxAttempts: Int,
    val workerId: String,
    val correlationId: String,
    val causationId: String?,
    val requestId: String?,
    val actorType: PrincipalType,
    val actorId: String,
    val principalType: PrincipalType,
    val source: String,
    val payloadJson: String,
    val metadata: Map<String, String>,
    val executionStartTime: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDefinition(job: JobDefinition, workerId: String): JobExecutionContext {
            return JobExecutionContext(
                jobId = job.jobId,
                projectId = job.projectId,
                jobType = job.jobType,
                jobVersion = job.jobVersion,
                triggerType = job.triggerType,
                priority = job.priority,
                attemptNumber = job.attemptCount + 1,
                maxAttempts = job.maxAttempts,
                workerId = workerId,
                correlationId = job.correlationId,
                causationId = job.causationId,
                requestId = job.requestId,
                actorType = job.actorType,
                actorId = job.actorId,
                principalType = job.principalType,
                source = job.source,
                payloadJson = job.payloadJson,
                metadata = job.metadata
            )
        }
    }
}

/**
 * Interface for typed background job handlers (INFRA-04 Step 04).
 */
interface JobHandler {
    val supportedJobType: String
    val supportedVersion: String get() = "v1"

    suspend fun execute(context: JobExecutionContext): JobResult
}
