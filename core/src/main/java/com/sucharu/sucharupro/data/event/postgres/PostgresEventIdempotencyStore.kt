package com.sucharu.sucharupro.data.event.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.event.idempotency.EventIdempotencyStore
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingRecord
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingStatus
import java.sql.Timestamp
import java.util.UUID

/**
 * Production-grade PostgreSQL Event Idempotency Store (INFRA-04 Step 02).
 *
 * Guarantees persistent at-least-once deduplication keyed by `(project_id, consumer_id, event_id)`.
 */
class PostgresEventIdempotencyStore(
    private val transactionManager: TransactionManager
) : EventIdempotencyStore {

    override suspend fun isProcessed(eventId: String, consumerId: String, projectId: String): Boolean {
        val tenantContext = TenantContext(projectId = projectId)
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT 1 FROM event_processing_records
                WHERE project_id = ? AND consumer_id = ? AND event_id = ? AND status = 'PROCESSED'
            """.trimIndent()
            val exists = txContext.sqlExecutor.querySingleOrNull(sql, listOf(projectId, consumerId, eventId)) { rs ->
                rs.getInt(1)
            }
            exists != null
        }
    }

    override suspend fun recordProcessing(record: EventProcessingRecord) {
        val tenantContext = TenantContext(projectId = record.projectId)
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO event_processing_records (
                    processing_id, project_id, event_id, consumer_id, status,
                    failure_reason, execution_duration_ms, processed_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?
                )
                ON CONFLICT (project_id, consumer_id, event_id)
                DO UPDATE SET
                    status = EXCLUDED.status,
                    failure_reason = EXCLUDED.failure_reason,
                    execution_duration_ms = EXCLUDED.execution_duration_ms,
                    processed_at = EXCLUDED.processed_at
            """.trimIndent()

            val processingId = UUID.randomUUID().toString()
            val processedAt = Timestamp(record.processedAt)

            txContext.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    processingId,
                    record.projectId,
                    record.eventId,
                    record.consumerId,
                    record.status.name,
                    record.failureReason,
                    record.executionDurationMs,
                    processedAt
                )
            )
        }
    }

    override suspend fun getRecord(
        eventId: String,
        consumerId: String,
        projectId: String
    ): EventProcessingRecord? {
        val tenantContext = TenantContext(projectId = projectId)
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM event_processing_records
                WHERE project_id = ? AND consumer_id = ? AND event_id = ?
            """.trimIndent()
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(projectId, consumerId, eventId)) { rs ->
                EventProcessingRecord(
                    eventId = rs.getString("event_id"),
                    consumerId = rs.getString("consumer_id"),
                    projectId = rs.getString("project_id"),
                    processedAt = rs.getTimestamp("processed_at").time,
                    status = EventProcessingStatus.valueOf(rs.getString("status")),
                    failureReason = rs.getString("failure_reason"),
                    executionDurationMs = rs.getLong("execution_duration_ms")
                )
            }
        }
    }
}
