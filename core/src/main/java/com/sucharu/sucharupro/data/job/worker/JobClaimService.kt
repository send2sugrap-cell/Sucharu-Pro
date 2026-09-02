package com.sucharu.sucharupro.data.job.worker

import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobDefinition

/**
 * Service for acquiring worker leases on queued background jobs (INFRA-04 Step 04).
 */
class JobClaimService(
    private val jobRepository: JobRepository
) {
    suspend fun claimJobs(
        workerId: String,
        batchSize: Int = 10,
        leaseDurationMs: Long = 30000L,
        tenantContext: TenantContext
    ): List<JobDefinition> {
        return jobRepository.claimEligibleJobs(
            workerId = workerId,
            limit = batchSize,
            leaseDurationMs = leaseDurationMs,
            tenantContext = tenantContext
        )
    }
}
