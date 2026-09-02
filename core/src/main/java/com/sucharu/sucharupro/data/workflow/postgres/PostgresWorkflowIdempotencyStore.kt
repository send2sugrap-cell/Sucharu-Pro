package com.sucharu.sucharupro.data.workflow.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import java.sql.Timestamp

/**
 * Interface for workflow idempotency tracking (INFRA-04 Step 05).
 */
interface WorkflowIdempotencyStore {
    suspend fun acquireKey(key: String, workflowId: String, tenantContext: TenantContext): Boolean
    suspend fun releaseKey(key: String, tenantContext: TenantContext)
}

/**
 * PostgreSQL implementation of WorkflowIdempotencyStore using unique constraints.
 */
class PostgresWorkflowIdempotencyStore(
    private val transactionManager: TransactionManager
) : WorkflowIdempotencyStore {

    override suspend fun acquireKey(key: String, workflowId: String, tenantContext: TenantContext): Boolean {
        return transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO idempotency_records (
                    idempotency_key, project_id, resource_type, resource_id, created_at
                ) VALUES (?, ?, 'WORKFLOW', ?, ?)
                ON CONFLICT (project_id, idempotency_key) DO NOTHING;
            """.trimIndent()

            val rows = txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    key,
                    tenantContext.projectId,
                    workflowId,
                    Timestamp(System.currentTimeMillis())
                )
            )
            rows > 0
        }
    }

    override suspend fun releaseKey(key: String, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                DELETE FROM idempotency_records
                WHERE project_id = ? AND idempotency_key = ? AND resource_type = 'WORKFLOW'
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(tenantContext.projectId, key)
            )
        }
    }
}
