package com.sucharu.sucharupro.domain.job.operations

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobStatus

/**
 * Interface for operational background job control (INFRA-04 Step 04).
 */
interface JobOperationsService {
    suspend fun getJob(jobId: String, tenantContext: TenantContext): JobDefinition?
    suspend fun listQueuedJobs(tenantContext: TenantContext, limit: Int = 50): List<JobDefinition>
    suspend fun cancelJob(jobId: String, reason: String, principal: AuthenticatedPrincipal): Boolean
    suspend fun retryDeadLetterJob(deadLetterId: String, principal: AuthenticatedPrincipal): String
    suspend fun replayJob(jobId: String, principal: AuthenticatedPrincipal): String
    suspend fun pauseSchedule(scheduleId: String, principal: AuthenticatedPrincipal): Boolean
    suspend fun resumeSchedule(scheduleId: String, principal: AuthenticatedPrincipal): Boolean
}
