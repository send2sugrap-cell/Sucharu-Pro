package com.sucharu.sucharupro.data.event.postgres

import com.sucharu.sucharupro.data.event.model.DeadLetterRecord
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Repository interface and PostgreSQL implementation for managing dead-letter event records (INFRA-04 Step 02).
 */
interface DeadLetterRepository {
    suspend fun listDeadLetters(tenantContext: TenantContext, limit: Int = 50): List<DeadLetterRecord>
    suspend fun getByDeadLetterId(tenantContext: TenantContext, deadLetterId: String): DeadLetterRecord?
    suspend fun markReplayed(tenantContext: TenantContext, deadLetterId: String, replayedBy: String)
    suspend fun resolve(tenantContext: TenantContext, deadLetterId: String)
}

class PostgresDeadLetterRepository(
    private val transactionManager: TransactionManager
) : DeadLetterRepository {

    private fun mapRowToDeadLetterRecord(rs: ResultSet): DeadLetterRecord {
        return DeadLetterRecord(
            deadLetterId = rs.getString("dead_letter_id"),
            projectId = rs.getString("project_id"),
            outboxId = rs.getString("outbox_id"),
            eventId = rs.getString("event_id"),
            eventType = DomainEventType.valueOf(rs.getString("event_type")),
            eventVersion = rs.getString("event_version") ?: "v1",
            aggregateType = rs.getString("aggregate_type"),
            aggregateId = rs.getString("aggregate_id"),
            payloadJson = rs.getString("payload"),
            failureClassification = EventFailureClassification.valueOf(rs.getString("failure_classification")),
            errorCode = rs.getString("error_code"),
            errorMessage = rs.getString("error_message"),
            attemptCount = rs.getInt("attempt_count"),
            firstFailureAt = rs.getTimestamp("first_failure_at").time,
            finalFailureAt = rs.getTimestamp("final_failure_at").time,
            correlationId = rs.getString("correlation_id"),
            causationId = rs.getString("causation_id"),
            requestId = rs.getString("request_id"),
            replayedAt = rs.getTimestamp("replayed_at")?.time,
            replayedBy = rs.getString("replayed_by"),
            isResolved = rs.getBoolean("is_resolved"),
            createdAt = rs.getTimestamp("created_at").time
        )
    }

    override suspend fun listDeadLetters(tenantContext: TenantContext, limit: Int): List<DeadLetterRecord> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM event_dead_letters
                WHERE project_id = ? AND is_resolved = FALSE
                ORDER BY final_failure_at DESC
                LIMIT ?
            """.trimIndent()
            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, limit)) { rs ->
                mapRowToDeadLetterRecord(rs)
            }
        }
    }

    override suspend fun getByDeadLetterId(tenantContext: TenantContext, deadLetterId: String): DeadLetterRecord? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM event_dead_letters WHERE project_id = ? AND dead_letter_id = ?"
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(tenantContext.projectId, deadLetterId)) { rs ->
                mapRowToDeadLetterRecord(rs)
            }
        }
    }

    override suspend fun markReplayed(tenantContext: TenantContext, deadLetterId: String, replayedBy: String) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE event_dead_letters
                SET replayed_at = NOW(),
                    replayed_by = ?,
                    is_resolved = TRUE
                WHERE project_id = ? AND dead_letter_id = ?
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(replayedBy, tenantContext.projectId, deadLetterId))
        }
    }

    override suspend fun resolve(tenantContext: TenantContext, deadLetterId: String) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE event_dead_letters
                SET is_resolved = TRUE
                WHERE project_id = ? AND dead_letter_id = ?
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId, deadLetterId))
        }
    }
}
