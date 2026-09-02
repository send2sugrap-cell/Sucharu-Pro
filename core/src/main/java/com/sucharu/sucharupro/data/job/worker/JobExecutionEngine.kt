package com.sucharu.sucharupro.data.job.worker

import com.sucharu.sucharupro.data.job.postgres.JobDeadLetterRecord
import com.sucharu.sucharupro.data.job.postgres.JobDeadLetterRepository
import com.sucharu.sucharupro.data.job.postgres.JobDependencyRepository
import com.sucharu.sucharupro.data.job.postgres.JobExecutionRepository
import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.job.retry.JobRetryEngine
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.job.model.DependencyRequirement
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobExecutionRecord
import com.sucharu.sucharupro.domain.job.model.JobResult
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.worker.JobExecutionContext
import com.sucharu.sucharupro.domain.job.worker.JobHandlerRegistry
import java.util.UUID

/**
 * Engine responsible for executing a single claimed job safely and idempotently (INFRA-04 Step 04).
 */
class JobExecutionEngine(
    private val handlerRegistry: JobHandlerRegistry,
    private val jobRepository: JobRepository,
    private val executionRepository: JobExecutionRepository,
    private val deadLetterRepository: JobDeadLetterRepository,
    private val dependencyRepository: JobDependencyRepository,
    private val retryEngine: JobRetryEngine = JobRetryEngine()
) {

    /**
     * Executes a claimed job definition.
     */
    suspend fun executeJob(job: JobDefinition, workerId: String, tenantContext: TenantContext): JobResult {
        val startTime = System.currentTimeMillis()
        val executionId = UUID.randomUUID().toString()
        val attemptNumber = job.attemptCount // already incremented when claimed

        val handler = handlerRegistry.getHandler(job.jobType, job.jobVersion)
        if (handler == null) {
            val failure = JobResult.Failure(
                reason = "No registered handler found for jobType '${job.jobType}' version '${job.jobVersion}'",
                classification = EventFailureClassification.NON_RETRYABLE
            )
            handleFailure(job, executionId, workerId, attemptNumber, startTime, failure, tenantContext)
            return failure
        }

        val context = JobExecutionContext.fromDefinition(job, workerId)
        val result = try {
            handler.execute(context)
        } catch (t: Throwable) {
            JobResult.Failure(
                reason = t.message ?: "Unhandled job execution exception",
                classification = EventFailureClassification.TRANSIENT,
                cause = t
            )
        }

        val endTime = System.currentTimeMillis()
        val durationMs = endTime - startTime

        when (result) {
            is JobResult.Success -> {
                handleSuccess(job, executionId, workerId, attemptNumber, startTime, durationMs, result, tenantContext)
            }
            is JobResult.Failure -> {
                handleFailure(job, executionId, workerId, attemptNumber, startTime, result, tenantContext)
            }
            is JobResult.Cancelled -> {
                handleCancelled(job, executionId, workerId, attemptNumber, startTime, durationMs, result, tenantContext)
            }
        }

        return result
    }

    private suspend fun handleSuccess(
        job: JobDefinition,
        executionId: String,
        workerId: String,
        attemptNumber: Int,
        startTime: Long,
        durationMs: Long,
        result: JobResult.Success,
        tenantContext: TenantContext
    ) {
        // 1. Record execution
        executionRepository.recordExecution(
            JobExecutionRecord(
                executionId = executionId,
                projectId = tenantContext.projectId,
                jobId = job.jobId,
                workerId = workerId,
                attemptNumber = attemptNumber,
                startedAt = startTime,
                completedAt = System.currentTimeMillis(),
                durationMs = durationMs,
                status = JobStatus.SUCCEEDED,
                outputMetadata = result.outputMetadata
            ),
            tenantContext
        )

        // 2. Mark succeeded in jobs table
        jobRepository.markSucceeded(job.jobId, tenantContext)

        // 3. Evaluate downstream dependencies
        notifyDependents(job.jobId, JobStatus.SUCCEEDED, tenantContext)
    }

    private suspend fun handleFailure(
        job: JobDefinition,
        executionId: String,
        workerId: String,
        attemptNumber: Int,
        startTime: Long,
        result: JobResult.Failure,
        tenantContext: TenantContext
    ) {
        val durationMs = System.currentTimeMillis() - startTime
        val isRetryable = result.isRetryable && retryEngine.canRetry(attemptNumber, job.maxAttempts)

        // 1. Record execution
        executionRepository.recordExecution(
            JobExecutionRecord(
                executionId = executionId,
                projectId = tenantContext.projectId,
                jobId = job.jobId,
                workerId = workerId,
                attemptNumber = attemptNumber,
                startedAt = startTime,
                completedAt = System.currentTimeMillis(),
                durationMs = durationMs,
                status = if (isRetryable) JobStatus.RETRY_SCHEDULED else JobStatus.DEAD_LETTER,
                errorCode = result.classification.name,
                errorMessage = result.reason,
                failureClassification = result.classification
            ),
            tenantContext
        )

        if (isRetryable) {
            val delayMs = retryEngine.calculateNextAttemptDelay(attemptNumber, result.retryAfterMs)
            val nextAttemptAt = System.currentTimeMillis() + delayMs

            jobRepository.markFailed(
                jobId = job.jobId,
                errorCode = result.classification.name,
                errorMessage = result.reason,
                classification = result.classification,
                nextAttemptAt = nextAttemptAt,
                tenantContext = tenantContext
            )
        } else {
            // Quarantine to Dead-Letter
            jobRepository.markDeadLetter(
                jobId = job.jobId,
                errorCode = result.classification.name,
                errorMessage = result.reason,
                classification = result.classification,
                tenantContext = tenantContext
            )

            deadLetterRepository.quarantineJob(
                JobDeadLetterRecord(
                    projectId = tenantContext.projectId,
                    jobId = job.jobId,
                    jobType = job.jobType,
                    payloadJson = job.payloadJson,
                    metadata = job.metadata,
                    attemptCount = attemptNumber,
                    failureClassification = result.classification,
                    errorCode = result.classification.name,
                    errorMessage = result.reason,
                    firstFailureAt = job.startedAt ?: startTime,
                    finalFailureAt = System.currentTimeMillis(),
                    correlationId = job.correlationId,
                    causationId = job.causationId,
                    requestId = job.requestId
                ),
                tenantContext
            )

            // Notify downstream dependents of failure
            notifyDependents(job.jobId, JobStatus.DEAD_LETTER, tenantContext)
        }
    }

    private suspend fun handleCancelled(
        job: JobDefinition,
        executionId: String,
        workerId: String,
        attemptNumber: Int,
        startTime: Long,
        durationMs: Long,
        result: JobResult.Cancelled,
        tenantContext: TenantContext
    ) {
        executionRepository.recordExecution(
            JobExecutionRecord(
                executionId = executionId,
                projectId = tenantContext.projectId,
                jobId = job.jobId,
                workerId = workerId,
                attemptNumber = attemptNumber,
                startedAt = startTime,
                completedAt = System.currentTimeMillis(),
                durationMs = durationMs,
                status = JobStatus.CANCELLED,
                errorMessage = result.reason
            ),
            tenantContext
        )

        jobRepository.markCancelled(job.jobId, result.reason, tenantContext)
    }

    private suspend fun notifyDependents(parentJobId: String, status: JobStatus, tenantContext: TenantContext) {
        val dependents = dependencyRepository.getDependentsOfJob(parentJobId, tenantContext)
        for (dep in dependents) {
            val isSatisfied = when (dep.requirement) {
                DependencyRequirement.ON_SUCCESS -> status == JobStatus.SUCCEEDED
                DependencyRequirement.ON_FAILURE -> status == JobStatus.DEAD_LETTER || status == JobStatus.FAILED
                DependencyRequirement.ON_COMPLETION -> status.isTerminal
            }
            if (isSatisfied) {
                dependencyRepository.markDependencySatisfied(dep.dependencyId, tenantContext)
            }
        }
    }
}
