package com.sucharu.sucharupro.data.job.lease

import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext

/**
 * Service for recovering abandoned worker leases and crash recovery (INFRA-04 Step 04).
 */
class JobLeaseRecoveryService(
    private val jobRepository: JobRepository
) {
    /**
     * Scans and reclaims jobs with expired leases for the tenant.
     * @return count of reclaimed jobs.
     */
    suspend fun recoverStaleLeases(tenantContext: TenantContext): Int {
        return jobRepository.recoverExpiredLeases(tenantContext)
    }
}
