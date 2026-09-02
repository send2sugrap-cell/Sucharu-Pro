package com.sucharu.sucharupro.data.job.operations

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.job.observability.JobAuditLogger
import com.sucharu.sucharupro.data.job.postgres.JobDeadLetterRepository
import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.job.postgres.JobScheduleRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.model.JobTriggerType
import com.sucharu.sucharupro.domain.job.operations.JobOperationsService
import java.util.UUID

/**
 * Production-grade implementation of [JobOperationsService] with RBAC capability checks and audit logging (INFRA-04 Step 04).
 */
class DefaultJobOperationsService(
    private val jobRepository: JobRepository,
    private val scheduleRepository: JobScheduleRepository,
    private val deadLetterRepository: JobDeadLetterRepository,
    private val auditLogger: JobAuditLogger = JobAuditLogger()
) : JobOperationsService {

    private fun checkAdminOrManager(principal: AuthenticatedPrincipal) {
        val isPermitted = principal.role == UserRole.ADMIN || principal.role == UserRole.MANAGER
        require(isPermitted) { "Access denied: Principal '${principal.userId}' lacks ADMIN or MANAGER role" }
    }

    override suspend fun getJob(jobId: String, tenantContext: TenantContext): JobDefinition? {
        return jobRepository.getJobById(jobId, tenantContext)
    }

    override suspend fun listQueuedJobs(tenantContext: TenantContext, limit: Int): List<JobDefinition> {
        return jobRepository.listQueuedJobs(tenantContext, limit)
    }

    override suspend fun cancelJob(jobId: String, reason: String, principal: AuthenticatedPrincipal): Boolean {
        checkAdminOrManager(principal)
        val tenantContext = TenantContext(principal.projectId)
        val job = jobRepository.getJobById(jobId, tenantContext) ?: return false

        jobRepository.markCancelled(jobId, reason, tenantContext)
        auditLogger.logAction("JOB_CANCELLED", job, principal, mapOf("reason" to reason))
        return true
    }

    override suspend fun retryDeadLetterJob(deadLetterId: String, principal: AuthenticatedPrincipal): String {
        checkAdminOrManager(principal)
        val tenantContext = TenantContext(principal.projectId)
        val deadLetter = deadLetterRepository.getDeadLetterById(deadLetterId, tenantContext)
            ?: throw IllegalArgumentException("Dead-letter record '$deadLetterId' not found in tenant '${principal.projectId}'")

        val newJobId = UUID.randomUUID().toString()
        val replayedJob = JobDefinition(
            jobId = newJobId,
            projectId = tenantContext.projectId,
            jobType = deadLetter.jobType,
            triggerType = JobTriggerType.MANUAL,
            priority = JobPriority.HIGH,
            status = JobStatus.QUEUED,
            payloadJson = deadLetter.payloadJson,
            metadata = deadLetter.metadata + mapOf("replayedFromDeadLetter" to deadLetterId),
            correlationId = deadLetter.correlationId,
            causationId = deadLetter.deadLetterId,
            actorType = principal.principalType,
            actorId = principal.userId,
            principalType = principal.principalType,
            source = "admin:replay"
        )

        jobRepository.enqueueJob(replayedJob, tenantContext)
        deadLetterRepository.markReplayed(deadLetterId, principal.userId, tenantContext)
        auditLogger.logAction("DEAD_LETTER_REPLAYED", replayedJob, principal, mapOf("deadLetterId" to deadLetterId))
        return newJobId
    }

    override suspend fun replayJob(jobId: String, principal: AuthenticatedPrincipal): String {
        checkAdminOrManager(principal)
        val tenantContext = TenantContext(principal.projectId)
        val originalJob = jobRepository.getJobById(jobId, tenantContext)
            ?: throw IllegalArgumentException("Job '$jobId' not found in tenant '${principal.projectId}'")

        val newJobId = UUID.randomUUID().toString()
        val newJob = JobDefinition(
            jobId = newJobId,
            projectId = tenantContext.projectId,
            jobType = originalJob.jobType,
            jobVersion = originalJob.jobVersion,
            triggerType = JobTriggerType.MANUAL,
            priority = originalJob.priority,
            status = JobStatus.QUEUED,
            payloadJson = originalJob.payloadJson,
            metadata = originalJob.metadata + mapOf("replayedFromJob" to jobId),
            correlationId = originalJob.correlationId,
            causationId = originalJob.jobId,
            actorType = principal.principalType,
            actorId = principal.userId,
            principalType = principal.principalType,
            source = "admin:job_replay"
        )

        jobRepository.enqueueJob(newJob, tenantContext)
        auditLogger.logAction("JOB_REPLAYED", newJob, principal, mapOf("originalJobId" to jobId))
        return newJobId
    }

    override suspend fun pauseSchedule(scheduleId: String, principal: AuthenticatedPrincipal): Boolean {
        checkAdminOrManager(principal)
        val tenantContext = TenantContext(principal.projectId)
        val schedule = scheduleRepository.getScheduleById(scheduleId, tenantContext) ?: return false

        scheduleRepository.setScheduleEnabled(scheduleId, false, tenantContext)
        return true
    }

    override suspend fun resumeSchedule(scheduleId: String, principal: AuthenticatedPrincipal): Boolean {
        checkAdminOrManager(principal)
        val tenantContext = TenantContext(principal.projectId)
        val schedule = scheduleRepository.getScheduleById(scheduleId, tenantContext) ?: return false

        scheduleRepository.setScheduleEnabled(scheduleId, true, tenantContext)
        return true
    }
}
