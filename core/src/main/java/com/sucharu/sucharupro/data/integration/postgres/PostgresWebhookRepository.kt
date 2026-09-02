package com.sucharu.sucharupro.data.integration.postgres

import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.data.integration.model.WebhookEvent
import com.sucharu.sucharupro.data.integration.model.WebhookEventStatus
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for inbound webhook events persistence and replay protection (INFRA-05 Step 05).
 */
interface WebhookRepository {
    suspend fun recordWebhookEvent(event: WebhookEvent, tenantContext: TenantContext): Boolean
    suspend fun getEventById(eventId: String, tenantContext: TenantContext): WebhookEvent?
    suspend fun getByProviderEventId(provider: String, externalEventId: String, tenantContext: TenantContext): WebhookEvent?
    suspend fun updateStatus(eventId: String, status: WebhookEventStatus, tenantContext: TenantContext)
    suspend fun markProcessed(eventId: String, tenantContext: TenantContext)
}

/**
 * PostgreSQL persistent implementation with Row-Level Security (RLS).
 */
class PostgresWebhookRepository(
    private val transactionManager: TransactionManager
) : WebhookRepository {

    private fun mapRowToWebhook(rs: ResultSet): WebhookEvent {
        val headersJson = rs.getString("headers") ?: "{}"
        val headers = EventSerializationHelper.parseJsonObject(headersJson)

        return WebhookEvent(
            eventId = rs.getString("event_id"),
            projectId = rs.getString("project_id"),
            provider = rs.getString("provider"),
            integrationId = rs.getString("integration_id"),
            externalEventId = rs.getString("external_event_id"),
            eventType = rs.getString("event_type"),
            payload = rs.getString("payload"),
            payloadHash = rs.getString("payload_hash"),
            headers = headers,
            receivedAt = rs.getTimestamp("received_at").time,
            verifiedAt = rs.getTimestamp("verified_at")?.time,
            status = WebhookEventStatus.valueOf(rs.getString("status")),
            attemptCount = rs.getInt("attempt_count"),
            processedAt = rs.getTimestamp("processed_at")?.time,
            correlationId = rs.getString("correlation_id"),
            causationId = rs.getString("causation_id"),
            createdAt = rs.getTimestamp("created_at").time
        )
    }

    override suspend fun recordWebhookEvent(event: WebhookEvent, tenantContext: TenantContext): Boolean {
        require(event.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: webhook projectId '${event.projectId}' != tenant '${tenantContext.projectId}'"
        }

        return transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO webhook_events (
                    event_id, project_id, provider, integration_id, external_event_id,
                    event_type, payload, payload_hash, headers, received_at,
                    verified_at, status, attempt_count, correlation_id, causation_id, created_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?::jsonb, ?,
                    ?, ?, ?, ?, ?, ?
                )
                ON CONFLICT (project_id, provider, external_event_id) WHERE external_event_id IS NOT NULL DO NOTHING
            """.trimIndent()

            val headersJson = EventSerializationHelper.serializeMap(event.headers)
            val recvTs = Timestamp(event.receivedAt)
            val verTs = event.verifiedAt?.let { Timestamp(it) }
            val createdTs = Timestamp(event.createdAt)

            val rows = txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    event.eventId,
                    tenantContext.projectId,
                    event.provider,
                    event.integrationId,
                    event.externalEventId,
                    event.eventType,
                    event.payload,
                    event.payloadHash,
                    headersJson,
                    recvTs,
                    verTs,
                    event.status.name,
                    event.attemptCount,
                    event.correlationId,
                    event.causationId,
                    createdTs
                )
            )
            rows > 0
        }
    }

    override suspend fun getEventById(eventId: String, tenantContext: TenantContext): WebhookEvent? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM webhook_events WHERE project_id = ? AND event_id = ?"
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(tenantContext.projectId, eventId)) { rs ->
                mapRowToWebhook(rs)
            }
        }
    }

    override suspend fun getByProviderEventId(
        provider: String,
        externalEventId: String,
        tenantContext: TenantContext
    ): WebhookEvent? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM webhook_events WHERE project_id = ? AND provider = ? AND external_event_id = ?"
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(tenantContext.projectId, provider, externalEventId)) { rs ->
                mapRowToWebhook(rs)
            }
        }
    }

    override suspend fun updateStatus(eventId: String, status: WebhookEventStatus, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = "UPDATE webhook_events SET status = ? WHERE project_id = ? AND event_id = ?"
            txContext.sqlExecutor.executeUpdate(sql, listOf(status.name, tenantContext.projectId, eventId))
        }
    }

    override suspend fun markProcessed(eventId: String, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE webhook_events
                SET status = 'PROCESSED', processed_at = NOW()
                WHERE project_id = ? AND event_id = ?
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId, eventId))
        }
    }
}
