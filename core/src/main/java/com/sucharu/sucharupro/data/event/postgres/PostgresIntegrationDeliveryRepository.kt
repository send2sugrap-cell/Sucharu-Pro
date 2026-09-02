package com.sucharu.sucharupro.data.event.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.consumer.orchestration.IntegrationDeliveryStatus
import com.sucharu.sucharupro.domain.event.consumer.orchestration.IntegrationType
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Model representing a persistent integration delivery attempt record.
 */
data class IntegrationDeliveryRecord(
    val deliveryId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val eventId: String,
    val consumerId: String,
    val integrationType: IntegrationType,
    val destination: String,
    val status: IntegrationDeliveryStatus = IntegrationDeliveryStatus.PENDING,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val nextAttemptAt: Long? = null,
    val deliveredAt: Long? = null,
    val failureClassification: EventFailureClassification? = null,
    val sanitizedError: String? = null,
    val correlationId: String,
    val requestId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Interface for managing persistent integration delivery records (INFRA-04 Step 03).
 */
interface IntegrationDeliveryRepository {
    suspend fun recordDeliveryAttempt(record: IntegrationDeliveryRecord, tenantContext: TenantContext)
    suspend fun markDelivered(deliveryId: String, tenantContext: TenantContext)
    suspend fun markFailed(
        deliveryId: String,
        classification: EventFailureClassification,
        error: String,
        nextAttemptAt: Long?,
        tenantContext: TenantContext
    )
    suspend fun getByConsumerAndEvent(
        consumerId: String,
        eventId: String,
        tenantContext: TenantContext
    ): IntegrationDeliveryRecord?
}

/**
 * PostgreSQL persistent implementation with Row-Level Security (RLS) and multi-tenant scoping.
 */
class PostgresIntegrationDeliveryRepository(
    private val transactionManager: TransactionManager
) : IntegrationDeliveryRepository {

    private fun mapRowToRecord(rs: ResultSet): IntegrationDeliveryRecord {
        val classificationStr = rs.getString("failure_classification")
        val classification = if (!classificationStr.isNullOrBlank()) {
            EventFailureClassification.valueOf(classificationStr)
        } else null

        return IntegrationDeliveryRecord(
            deliveryId = rs.getString("delivery_id"),
            projectId = rs.getString("project_id"),
            eventId = rs.getString("event_id"),
            consumerId = rs.getString("consumer_id"),
            integrationType = IntegrationType.valueOf(rs.getString("integration_type")),
            destination = rs.getString("destination"),
            status = IntegrationDeliveryStatus.valueOf(rs.getString("status")),
            attemptCount = rs.getInt("attempt_count"),
            lastAttemptAt = rs.getTimestamp("last_attempt_at")?.time,
            nextAttemptAt = rs.getTimestamp("next_attempt_at")?.time,
            deliveredAt = rs.getTimestamp("delivered_at")?.time,
            failureClassification = classification,
            sanitizedError = rs.getString("sanitized_error"),
            correlationId = rs.getString("correlation_id"),
            requestId = rs.getString("request_id"),
            createdAt = rs.getTimestamp("created_at").time
        )
    }

    override suspend fun recordDeliveryAttempt(record: IntegrationDeliveryRecord, tenantContext: TenantContext) {
        require(record.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: record projectId '${record.projectId}' != tenant '${tenantContext.projectId}'"
        }

        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO integration_delivery_records (
                    delivery_id, project_id, event_id, consumer_id, integration_type,
                    destination, status, attempt_count, last_attempt_at, next_attempt_at,
                    delivered_at, failure_classification, sanitized_error, correlation_id,
                    request_id, created_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?
                )
                ON CONFLICT (project_id, consumer_id, event_id) DO UPDATE
                SET status = EXCLUDED.status,
                    attempt_count = EXCLUDED.attempt_count,
                    last_attempt_at = EXCLUDED.last_attempt_at,
                    next_attempt_at = EXCLUDED.next_attempt_at,
                    delivered_at = EXCLUDED.delivered_at,
                    failure_classification = EXCLUDED.failure_classification,
                    sanitized_error = EXCLUDED.sanitized_error
            """.trimIndent()

            val lastAttempt = record.lastAttemptAt?.let { Timestamp(it) }
            val nextAttempt = record.nextAttemptAt?.let { Timestamp(it) }
            val delivered = record.deliveredAt?.let { Timestamp(it) }
            val created = Timestamp(record.createdAt)

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    record.deliveryId,
                    tenantContext.projectId,
                    record.eventId,
                    record.consumerId,
                    record.integrationType.name,
                    record.destination,
                    record.status.name,
                    record.attemptCount,
                    lastAttempt,
                    nextAttempt,
                    delivered,
                    record.failureClassification?.name,
                    record.sanitizedError,
                    record.correlationId,
                    record.requestId,
                    created
                )
            )
        }
    }

    override suspend fun markDelivered(deliveryId: String, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE integration_delivery_records
                SET status = 'DELIVERED',
                    delivered_at = NOW()
                WHERE project_id = ? AND delivery_id = ?
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId, deliveryId))
        }
    }

    override suspend fun markFailed(
        deliveryId: String,
        classification: EventFailureClassification,
        error: String,
        nextAttemptAt: Long?,
        tenantContext: TenantContext
    ) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val status = if (classification.isRetryable && nextAttemptAt != null) {
                IntegrationDeliveryStatus.RETRY_SCHEDULED
            } else {
                IntegrationDeliveryStatus.FAILED
            }
            val nextTs = nextAttemptAt?.let { Timestamp(it) }

            val sql = """
                UPDATE integration_delivery_records
                SET status = ?,
                    failure_classification = ?,
                    sanitized_error = ?,
                    next_attempt_at = ?
                WHERE project_id = ? AND delivery_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    status.name,
                    classification.name,
                    error,
                    nextTs,
                    tenantContext.projectId,
                    deliveryId
                )
            )
        }
    }

    override suspend fun getByConsumerAndEvent(
        consumerId: String,
        eventId: String,
        tenantContext: TenantContext
    ): IntegrationDeliveryRecord? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM integration_delivery_records
                WHERE project_id = ? AND consumer_id = ? AND event_id = ?
            """.trimIndent()

            txContext.sqlExecutor.querySingleOrNull(sql, listOf(tenantContext.projectId, consumerId, eventId)) { rs ->
                mapRowToRecord(rs)
            }
        }
    }
}
