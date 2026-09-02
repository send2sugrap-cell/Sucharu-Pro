package com.sucharu.sucharupro.data.job.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.job.idempotency.JobIdempotencyStore

/**
 * PostgreSQL implementation of [JobIdempotencyStore] with multi-tenant RLS (INFRA-04 Step 04).
 */
class PostgresJobIdempotencyStore(
    private val transactionManager: TransactionManager
) : JobIdempotencyStore {

    override suspend fun isIdempotencyKeyClaimed(projectId: String, idempotencyKey: String): Boolean {
        val tenantContext = TenantContext(projectId)
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT 1 FROM background_jobs WHERE project_id = ? AND idempotency_key = ?"
            val exists = txContext.sqlExecutor.querySingleOrNull(sql, listOf(projectId, idempotencyKey)) { 1 }
            exists != null
        }
    }

    override suspend fun getJobIdByIdempotencyKey(projectId: String, idempotencyKey: String): String? {
        val tenantContext = TenantContext(projectId)
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT job_id FROM background_jobs WHERE project_id = ? AND idempotency_key = ?"
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(projectId, idempotencyKey)) { rs ->
                rs.getString("job_id")
            }
        }
    }

    override suspend fun recordIdempotencyKey(projectId: String, idempotencyKey: String, jobId: String) {
        // Idempotency keys are atomically persisted with the job definition in background_jobs table.
    }
}
