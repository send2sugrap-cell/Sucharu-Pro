package com.sucharu.sucharupro.data.integration.postgres

import com.sucharu.sucharupro.data.integration.model.IntegrationAuditRecord
import com.sucharu.sucharupro.data.integration.model.IntegrationDirection
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for integration audit logging (INFRA-05 Step 05).
 */
interface IntegrationAuditRepository {
    suspend fun recordAudit(record: IntegrationAuditRecord, tenantContext: TenantContext)
    suspend fun listAuditLogs(integrationId: String, limit: Int = 50, tenantContext: TenantContext): List<IntegrationAuditRecord>
}

/**
 * PostgreSQL persistent implementation with Row-Level Security (RLS).
 */
class PostgresIntegrationAuditRepository(
    private val transactionManager: TransactionManager
) : IntegrationAuditRepository {

    private fun mapRowToAudit(rs: ResultSet): IntegrationAuditRecord {
        return IntegrationAuditRecord(
            auditId = rs.getString("audit_id"),
            projectId = rs.getString("project_id"),
            integrationId = rs.getString("integration_id"),
            provider = rs.getString("provider"),
            operationType = rs.getString("operation_type"),
            direction = IntegrationDirection.valueOf(rs.getString("direction")),
            status = rs.getString("status"),
            sanitizedError = rs.getString("sanitized_error"),
            durationMs = rs.getLong("duration_ms"),
            correlationId = rs.getString("correlation_id"),
            jobId = rs.getString("job_id"),
            createdAt = rs.getTimestamp("created_at").time
        )
    }

    override suspend fun recordAudit(record: IntegrationAuditRecord, tenantContext: TenantContext) {
        require(record.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: audit projectId '${record.projectId}' != tenant '${tenantContext.projectId}'"
        }

        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO integration_audit_log (
                    audit_id, project_id, integration_id, provider, operation_type,
                    direction, status, sanitized_error, duration_ms, correlation_id,
                    job_id, created_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?
                )
            """.trimIndent()

            val createdTs = Timestamp(record.createdAt)

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    record.auditId,
                    tenantContext.projectId,
                    record.integrationId,
                    record.provider,
                    record.operationType,
                    record.direction.name,
                    record.status,
                    record.sanitizedError,
                    record.durationMs,
                    record.correlationId,
                    record.jobId,
                    createdTs
                )
            )
        }
    }

    override suspend fun listAuditLogs(
        integrationId: String,
        limit: Int,
        tenantContext: TenantContext
    ): List<IntegrationAuditRecord> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM integration_audit_log
                WHERE project_id = ? AND integration_id = ?
                ORDER BY created_at DESC
                LIMIT ?
            """.trimIndent()

            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, integrationId, limit)) { rs ->
                mapRowToAudit(rs)
            }
        }
    }
}
