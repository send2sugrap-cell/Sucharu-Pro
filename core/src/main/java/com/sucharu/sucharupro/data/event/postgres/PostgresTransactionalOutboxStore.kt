package com.sucharu.sucharupro.data.event.postgres

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.event.model.DeadLetterRecord
import com.sucharu.sucharupro.data.event.model.OutboxStatus
import com.sucharu.sucharupro.data.event.model.PersistentOutboxRecord
import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.store.OutboxEventRecord
import com.sucharu.sucharupro.domain.event.store.TransactionalOutboxStore
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Production-grade PostgreSQL Transactional Outbox Store (INFRA-04 Step 02).
 *
 * Provides atomic outbox enqueueing, row-level worker claiming with FOR UPDATE SKIP LOCKED,
 * lease expiration recovery, and dead-letter quarantine.
 */
class PostgresTransactionalOutboxStore(
    private val transactionManager: TransactionManager
) : TransactionalOutboxStore {

    private fun mapRowToPersistentRecord(rs: ResultSet): PersistentOutboxRecord {
        val eventTypeStr = rs.getString("event_type")
        val eventType = DomainEventType.valueOf(eventTypeStr)
        val statusStr = rs.getString("status")
        val status = OutboxStatus.valueOf(statusStr)

        val metadataJson = rs.getString("metadata")
        val metadataMap = if (!metadataJson.isNullOrBlank()) {
            EventSerializationHelper.parseJsonObject(metadataJson)
        } else {
            emptyMap()
        }

        return PersistentOutboxRecord(
            outboxId = rs.getString("outbox_id"),
            projectId = rs.getString("project_id"),
            eventId = rs.getString("event_id"),
            eventType = eventType,
            eventVersion = rs.getString("event_version") ?: "v1",
            aggregateType = rs.getString("aggregate_type"),
            aggregateId = rs.getString("aggregate_id"),
            aggregateVersion = rs.getLong("aggregate_version"),
            status = status,
            attemptCount = rs.getInt("attempt_count"),
            claimedByWorker = rs.getString("claimed_by_worker"),
            claimedAt = rs.getTimestamp("claimed_at")?.time,
            leaseExpiresAt = rs.getTimestamp("lease_expires_at")?.time,
            availableAt = rs.getTimestamp("available_at").time,
            lastAttemptAt = rs.getTimestamp("last_attempt_at")?.time,
            nextAttemptAt = rs.getTimestamp("next_attempt_at")?.time,
            publishedAt = rs.getTimestamp("published_at")?.time,
            lastErrorCode = rs.getString("last_error_code"),
            lastErrorMessage = rs.getString("last_error_message"),
            createdAt = rs.getTimestamp("created_at").time,
            payloadJson = rs.getString("payload"),
            metadata = metadataMap,
            correlationId = rs.getString("correlation_id"),
            causationId = rs.getString("causation_id"),
            requestId = rs.getString("request_id"),
            actorType = PrincipalType.valueOf(rs.getString("actor_type") ?: "HUMAN"),
            actorId = rs.getString("actor_id"),
            principalType = PrincipalType.valueOf(rs.getString("principal_type") ?: "HUMAN"),
            source = rs.getString("source") ?: "sucharu-pro-backend"
        )
    }

    /**
     * Enqueues an Outbox record inside an ongoing active business transaction.
     */
    suspend fun enqueueInTransaction(txContext: TransactionContext, envelope: EventEnvelope<*>) {
        require(envelope.projectId == txContext.tenantContext.projectId) {
            "Cross-tenant outbox enqueue denied: envelope projectId '${envelope.projectId}' != tenant '${txContext.tenantContext.projectId}'"
        }

        val sql = """
            INSERT INTO event_outbox (
                outbox_id, project_id, event_id, event_type, event_version,
                aggregate_type, aggregate_id, aggregate_version, status, attempt_count,
                available_at, created_at, payload, metadata, correlation_id, causation_id,
                request_id, actor_type, actor_id, principal_type, source
            ) VALUES (
                ?, ?, ?, ?, ?,
                ?, ?, ?, 'PENDING', 0,
                ?, ?, ?::jsonb, ?::jsonb, ?, ?,
                ?, ?, ?, ?, ?
            )
        """.trimIndent()

        val payloadJson = EventSerializationHelper.serializePayload(envelope.payload)
        val metadataJson = EventSerializationHelper.serializeMap(envelope.metadata)
        val now = Timestamp(System.currentTimeMillis())
        val outboxId = UUID.randomUUID().toString()

        txContext.sqlExecutor.executeUpdate(
            sql = sql,
            params = listOf(
                outboxId,
                envelope.projectId,
                envelope.eventId,
                envelope.eventType.name,
                envelope.eventVersion,
                envelope.aggregateType,
                envelope.aggregateId,
                envelope.aggregateVersion,
                now,
                now,
                payloadJson,
                metadataJson,
                envelope.correlationId,
                envelope.causationId,
                envelope.requestId,
                envelope.actorType.name,
                envelope.actorId,
                envelope.principalType.name,
                envelope.source
            )
        )
    }

    suspend fun enqueue(tenantContext: TenantContext, envelope: EventEnvelope<*>) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            enqueueInTransaction(txContext, envelope)
        }
    }

    override suspend fun appendOutboxRecord(record: OutboxEventRecord, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO event_outbox (
                    outbox_id, project_id, event_id, event_type, event_version,
                    aggregate_type, aggregate_id, aggregate_version, status, attempt_count,
                    available_at, created_at, payload, metadata, correlation_id, causation_id,
                    request_id, actor_type, actor_id, principal_type, source
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, 'PENDING', ?,
                    ?, ?, ?::jsonb, ?::jsonb, ?, ?,
                    ?, 'SYSTEM', 'SYSTEM', 'SYSTEM', 'sucharu-pro-backend'
                )
            """.trimIndent()

            val now = Timestamp(record.createdAt)
            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    record.outboxId,
                    record.projectId,
                    record.eventId,
                    record.eventType.name,
                    record.eventVersion,
                    record.aggregateType,
                    record.aggregateId,
                    record.aggregateVersion,
                    record.retryCount,
                    now,
                    now,
                    record.payloadJson,
                    record.headersJson,
                    record.eventId,
                    null,
                    null
                )
            )
        }
    }

    override suspend fun getPendingRecords(limit: Int, tenantContext: TenantContext): List<OutboxEventRecord> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM event_outbox 
                WHERE project_id = ? AND status IN ('PENDING', 'RETRY_SCHEDULED') AND available_at <= NOW()
                ORDER BY aggregate_version ASC, created_at ASC
                LIMIT ?
            """.trimIndent()
            val records = txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, limit)) { rs ->
                mapRowToPersistentRecord(rs)
            }
            records.map { r ->
                OutboxEventRecord(
                    outboxId = r.outboxId,
                    eventId = r.eventId,
                    eventType = r.eventType,
                    eventVersion = r.eventVersion,
                    projectId = r.projectId,
                    aggregateType = r.aggregateType,
                    aggregateId = r.aggregateId,
                    aggregateVersion = r.aggregateVersion,
                    payloadJson = r.payloadJson,
                    status = com.sucharu.sucharupro.domain.event.store.OutboxStatus.PENDING,
                    retryCount = r.attemptCount,
                    createdAt = r.createdAt,
                    lastError = r.lastErrorMessage
                )
            }
        }
    }

    suspend fun getPending(tenantContext: TenantContext, limit: Int = 100): List<OutboxEventRecord> {
        return getPendingRecords(limit, tenantContext)
    }

    /**
     * Atomically claims eligible pending records using FOR UPDATE SKIP LOCKED with worker lease timeout.
     */
    suspend fun claimPendingRecords(
        tenantContext: TenantContext,
        workerId: String,
        limit: Int = 10,
        leaseDurationMs: Long = 30000L
    ): List<PersistentOutboxRecord> {
        return transactionManager.inTransaction(tenantContext) { txContext ->
            val now = System.currentTimeMillis()
            val leaseExpiresAt = Timestamp(now + leaseDurationMs)
            val claimedAt = Timestamp(now)

            // 1. Select eligible IDs with SKIP LOCKED
            val selectSql = """
                SELECT outbox_id FROM event_outbox
                WHERE project_id = ? 
                  AND (
                    (status IN ('PENDING', 'RETRY_SCHEDULED') AND available_at <= NOW())
                    OR (status = 'PROCESSING' AND lease_expires_at IS NOT NULL AND lease_expires_at < NOW())
                  )
                ORDER BY aggregate_version ASC, created_at ASC
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            """.trimIndent()

            val outboxIds = txContext.sqlExecutor.queryList(selectSql, listOf(tenantContext.projectId, limit)) { rs ->
                rs.getString("outbox_id")
            }

            if (outboxIds.isEmpty()) {
                return@inTransaction emptyList()
            }

            // 2. Mark claimed
            val updateSql = """
                UPDATE event_outbox
                SET status = 'PROCESSING',
                    claimed_by_worker = ?,
                    claimed_at = ?,
                    lease_expires_at = ?,
                    last_attempt_at = ?,
                    attempt_count = attempt_count + 1
                WHERE project_id = ? AND outbox_id = ?
            """.trimIndent()

            for (id in outboxIds) {
                txContext.sqlExecutor.executeUpdate(
                    updateSql,
                    listOf(workerId, claimedAt, leaseExpiresAt, claimedAt, tenantContext.projectId, id)
                )
            }

            // 3. Return updated records
            val fetchSql = """
                SELECT * FROM event_outbox
                WHERE project_id = ? AND outbox_id = ?
            """.trimIndent()

            outboxIds.mapNotNull { id ->
                txContext.sqlExecutor.querySingleOrNull(fetchSql, listOf(tenantContext.projectId, id)) { rs ->
                    mapRowToPersistentRecord(rs)
                }
            }
        }
    }

    /**
     * Recovers expired worker leases by resetting status back to RETRY_SCHEDULED or PENDING.
     */
    suspend fun recoverExpiredLeases(tenantContext: TenantContext): Int {
        return transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE event_outbox
                SET status = 'RETRY_SCHEDULED',
                    claimed_by_worker = NULL,
                    claimed_at = NULL,
                    lease_expires_at = NULL,
                    available_at = NOW()
                WHERE project_id = ? AND status = 'PROCESSING' AND lease_expires_at < NOW()
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId))
        }
    }

    suspend fun markPublished(tenantContext: TenantContext, outboxId: String) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE event_outbox
                SET status = 'PUBLISHED',
                    published_at = NOW(),
                    claimed_by_worker = NULL,
                    lease_expires_at = NULL
                WHERE project_id = ? AND outbox_id = ?
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId, outboxId))
        }
    }

    override suspend fun markPublished(outboxId: String, tenantContext: TenantContext) {
        markPublished(tenantContext, outboxId)
    }

    suspend fun scheduleRetry(
        tenantContext: TenantContext,
        outboxId: String,
        nextAttemptAt: Long,
        errorCode: String?,
        errorMessage: String?
    ) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE event_outbox
                SET status = 'RETRY_SCHEDULED',
                    available_at = ?,
                    next_attempt_at = ?,
                    last_error_code = ?,
                    last_error_message = ?,
                    claimed_by_worker = NULL,
                    lease_expires_at = NULL
                WHERE project_id = ? AND outbox_id = ?
            """.trimIndent()
            val nextTs = Timestamp(nextAttemptAt)
            txContext.sqlExecutor.executeUpdate(
                sql,
                listOf(nextTs, nextTs, errorCode, errorMessage, tenantContext.projectId, outboxId)
            )
        }
    }

    suspend fun moveToDeadLetter(
        tenantContext: TenantContext,
        outboxId: String,
        classification: EventFailureClassification,
        errorCode: String?,
        errorMessage: String?
    ) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            // 1. Fetch outbox record
            val fetchSql = "SELECT * FROM event_outbox WHERE project_id = ? AND outbox_id = ?"
            val outboxRecord = txContext.sqlExecutor.querySingleOrNull(fetchSql, listOf(tenantContext.projectId, outboxId)) { rs ->
                mapRowToPersistentRecord(rs)
            } ?: return@inTransaction

            // 2. Mark outbox status DEAD_LETTER
            val updateOutboxSql = """
                UPDATE event_outbox
                SET status = 'DEAD_LETTER',
                    last_error_code = ?,
                    last_error_message = ?,
                    claimed_by_worker = NULL,
                    lease_expires_at = NULL
                WHERE project_id = ? AND outbox_id = ?
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(updateOutboxSql, listOf(errorCode, errorMessage, tenantContext.projectId, outboxId))

            // 3. Insert into event_dead_letters
            val deadLetterSql = """
                INSERT INTO event_dead_letters (
                    dead_letter_id, project_id, outbox_id, event_id, event_type, event_version,
                    aggregate_type, aggregate_id, payload, failure_classification, error_code,
                    error_message, attempt_count, first_failure_at, final_failure_at, correlation_id,
                    causation_id, request_id
                ) VALUES (
                    ?, ?, ?, ?, ?, ?,
                    ?, ?, ?::jsonb, ?, ?,
                    ?, ?, ?, NOW(), ?,
                    ?, ?
                )
            """.trimIndent()

            val deadLetterId = UUID.randomUUID().toString()
            val firstFailure = Timestamp(outboxRecord.createdAt)

            txContext.sqlExecutor.executeUpdate(
                deadLetterSql,
                listOf(
                    deadLetterId,
                    tenantContext.projectId,
                    outboxId,
                    outboxRecord.eventId,
                    outboxRecord.eventType.name,
                    outboxRecord.eventVersion,
                    outboxRecord.aggregateType,
                    outboxRecord.aggregateId,
                    outboxRecord.payloadJson,
                    classification.name,
                    errorCode,
                    errorMessage,
                    outboxRecord.attemptCount,
                    firstFailure,
                    outboxRecord.correlationId,
                    outboxRecord.causationId,
                    outboxRecord.requestId
                )
            )
        }
    }

    suspend fun cancel(tenantContext: TenantContext, outboxId: String, reason: String? = null) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE event_outbox
                SET status = 'CANCELLED',
                    last_error_message = ?,
                    claimed_by_worker = NULL,
                    lease_expires_at = NULL
                WHERE project_id = ? AND outbox_id = ?
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(reason ?: "Cancelled by operator", tenantContext.projectId, outboxId))
        }
    }

    override suspend fun markFailed(outboxId: String, errorReason: String, tenantContext: TenantContext) {
        moveToDeadLetter(tenantContext, outboxId, EventFailureClassification.NON_RETRYABLE, "OUTBOX_FAILURE", errorReason)
    }
}
