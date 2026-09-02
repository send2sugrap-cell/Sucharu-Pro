package com.sucharu.sucharupro.data.job.postgres

import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Dead-letter quarantine record for background jobs (INFRA-04 Step 04).
 */
data class JobDeadLetterRecord(
    val deadLetterId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val jobId: String,
    val jobType: String,
    val payloadJson: String,
    val metadata: Map<String, String> = emptyMap(),
    val attemptCount: Int,
    val failureClassification: EventFailureClassification,
    val errorCode: String?,
    val errorMessage: String?,
    val firstFailureAt: Long,
    val finalFailureAt: Long = System.currentTimeMillis(),
    val correlationId: String,
    val causationId: String?,
    val requestId: String?,
    val isResolved: Boolean = false,
    val replayedAt: Long? = null,
    val replayedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Interface for job dead-letter quarantine persistence.
 */
interface JobDeadLetterRepository {
    suspend fun quarantineJob(record: JobDeadLetterRecord, tenantContext: TenantContext)
    suspend fun getDeadLetterById(deadLetterId: String, tenantContext: TenantContext): JobDeadLetterRecord?
    suspend fun listUnresolvedDeadLetters(limit: Int = 50, tenantContext: TenantContext): List<JobDeadLetterRecord>
    suspend fun markReplayed(deadLetterId: String, replayedBy: String, tenantContext: TenantContext)
    suspend fun markResolved(deadLetterId: String, tenantContext: TenantContext)
}

/**
 * PostgreSQL implementation of [JobDeadLetterRepository] with multi-tenant RLS.
 */
class PostgresJobDeadLetterRepository(
    private val transactionManager: TransactionManager
) : JobDeadLetterRepository {

    private fun mapRowToDeadLetter(rs: ResultSet): JobDeadLetterRecord {
        val metadataJson = rs.getString("metadata") ?: "{}"
        val metadata = EventSerializationHelper.parseJsonObject(metadataJson)

        return JobDeadLetterRecord(
            deadLetterId = rs.getString("dead_letter_id"),
            projectId = rs.getString("project_id"),
            jobId = rs.getString("job_id"),
            jobType = rs.getString("job_type"),
            payloadJson = rs.getString("payload"),
            metadata = metadata,
            attemptCount = rs.getInt("attempt_count"),
            failureClassification = EventFailureClassification.valueOf(rs.getString("failure_classification")),
            errorCode = rs.getString("error_code"),
            errorMessage = rs.getString("error_message"),
            firstFailureAt = rs.getTimestamp("first_failure_at").time,
            finalFailureAt = rs.getTimestamp("final_failure_at").time,
            correlationId = rs.getString("correlation_id"),
            causationId = rs.getString("causation_id"),
            requestId = rs.getString("request_id"),
            isResolved = rs.getBoolean("is_resolved"),
            replayedAt = rs.getTimestamp("replayed_at")?.time,
            replayedBy = rs.getString("replayed_by"),
            createdAt = rs.getTimestamp("created_at").time
        )
    }

    override suspend fun quarantineJob(record: JobDeadLetterRecord, tenantContext: TenantContext) {
        require(record.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: record projectId '${record.projectId}' != tenant '${tenantContext.projectId}'"
        }

        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO job_dead_letters (
                    dead_letter_id, project_id, job_id, job_type, payload,
                    metadata, attempt_count, failure_classification, error_code, error_message,
                    first_failure_at, final_failure_at, correlation_id, causation_id, request_id,
                    is_resolved, replayed_at, replayed_by, created_at
                ) VALUES (
                    ?, ?, ?, ?, ?::jsonb,
                    ?::jsonb, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?
                )
            """.trimIndent()

            val metadataJson = EventSerializationHelper.serializeMap(record.metadata)

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    record.deadLetterId,
                    tenantContext.projectId,
                    record.jobId,
                    record.jobType,
                    record.payloadJson,
                    metadataJson,
                    record.attemptCount,
                    record.failureClassification.name,
                    record.errorCode,
                    record.errorMessage,
                    Timestamp(record.firstFailureAt),
                    Timestamp(record.finalFailureAt),
                    record.correlationId,
                    record.causationId,
                    record.requestId,
                    record.isResolved,
                    record.replayedAt?.let { Timestamp(it) },
                    record.replayedBy,
                    Timestamp(record.createdAt)
                )
            )
        }
    }

    override suspend fun getDeadLetterById(deadLetterId: String, tenantContext: TenantContext): JobDeadLetterRecord? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM job_dead_letters WHERE project_id = ? AND dead_letter_id = ?"
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(tenantContext.projectId, deadLetterId)) { rs ->
                mapRowToDeadLetter(rs)
            }
        }
    }

    override suspend fun listUnresolvedDeadLetters(limit: Int, tenantContext: TenantContext): List<JobDeadLetterRecord> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM job_dead_letters
                WHERE project_id = ? AND is_resolved = FALSE
                ORDER BY final_failure_at DESC
                LIMIT ?
            """.trimIndent()

            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, limit)) { rs ->
                mapRowToDeadLetter(rs)
            }
        }
    }

    override suspend fun markReplayed(deadLetterId: String, replayedBy: String, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE job_dead_letters
                SET is_resolved = TRUE,
                    replayed_at = NOW(),
                    replayed_by = ?
                WHERE project_id = ? AND dead_letter_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(sql, listOf(replayedBy, tenantContext.projectId, deadLetterId))
        }
    }

    override suspend fun markResolved(deadLetterId: String, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE job_dead_letters
                SET is_resolved = TRUE
                WHERE project_id = ? AND dead_letter_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId, deadLetterId))
        }
    }
}
