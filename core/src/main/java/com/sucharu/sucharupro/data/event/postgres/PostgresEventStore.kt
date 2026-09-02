package com.sucharu.sucharupro.data.event.postgres

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.store.EventStore
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Production-grade PostgreSQL Event Store implementation (INFRA-04 Step 02).
 *
 * Enforces multi-tenant RLS scoping, immutable append-only persistence,
 * and deterministic aggregate stream retrieval.
 */
class PostgresEventStore(
    private val transactionManager: TransactionManager
) : EventStore {

    private fun mapRowToEnvelope(rs: ResultSet): EventEnvelope<DomainEvent> {
        val eventTypeStr = rs.getString("event_type")
        val eventType = DomainEventType.valueOf(eventTypeStr)
        val payloadJson = rs.getString("payload")
        val payload = EventSerializationHelper.deserializePayload(eventType, payloadJson)

        val metadataJson = rs.getString("metadata")
        val metadataMap = if (!metadataJson.isNullOrBlank()) {
            EventSerializationHelper.parseJsonObject(metadataJson)
        } else {
            emptyMap()
        }

        val actorType = PrincipalType.valueOf(rs.getString("actor_type") ?: "HUMAN")
        val principalType = PrincipalType.valueOf(rs.getString("principal_type") ?: "HUMAN")

        return EventEnvelope(
            eventId = rs.getString("event_id"),
            eventType = eventType,
            eventVersion = rs.getString("event_version") ?: "v1",
            occurredAt = rs.getTimestamp("occurred_at").time,
            publishedAt = rs.getTimestamp("published_at").time,
            projectId = rs.getString("project_id"),
            aggregateType = rs.getString("aggregate_type"),
            aggregateId = rs.getString("aggregate_id"),
            aggregateVersion = rs.getLong("aggregate_version"),
            actorType = actorType,
            actorId = rs.getString("actor_id"),
            principalType = principalType,
            correlationId = rs.getString("correlation_id"),
            causationId = rs.getString("causation_id"),
            requestId = rs.getString("request_id"),
            source = rs.getString("source") ?: "sucharu-pro-backend",
            payload = payload,
            metadata = metadataMap
        )
    }

    suspend fun appendInTransaction(txContext: TransactionContext, envelope: EventEnvelope<*>) {
        require(envelope.projectId == txContext.tenantContext.projectId) {
            "Cross-tenant event append denied: envelope projectId '${envelope.projectId}' != tenant '${txContext.tenantContext.projectId}'"
        }

        val sql = """
            INSERT INTO event_store (
                project_id, event_id, event_type, event_version, occurred_at, published_at,
                aggregate_type, aggregate_id, aggregate_version, actor_type, actor_id,
                principal_type, correlation_id, causation_id, request_id, source, payload, metadata
            ) VALUES (
                ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb
            )
        """.trimIndent()

        val payloadJson = EventSerializationHelper.serializePayload(envelope.payload)
        val metadataJson = EventSerializationHelper.serializeMap(envelope.metadata)

        txContext.sqlExecutor.executeUpdate(
            sql = sql,
            params = listOf(
                envelope.projectId,
                envelope.eventId,
                envelope.eventType.name,
                envelope.eventVersion,
                Timestamp(envelope.occurredAt),
                Timestamp(envelope.publishedAt),
                envelope.aggregateType,
                envelope.aggregateId,
                envelope.aggregateVersion,
                envelope.actorType.name,
                envelope.actorId,
                envelope.principalType.name,
                envelope.correlationId,
                envelope.causationId,
                envelope.requestId,
                envelope.source,
                payloadJson,
                metadataJson
            )
        )
    }

    override suspend fun append(envelope: EventEnvelope<*>, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            appendInTransaction(txContext, envelope)
        }
    }

    override suspend fun appendAll(envelopes: List<EventEnvelope<*>>, tenantContext: TenantContext) {
        if (envelopes.isEmpty()) return
        transactionManager.inTransaction(tenantContext) { txContext ->
            for (envelope in envelopes) {
                appendInTransaction(txContext, envelope)
            }
        }
    }

    override suspend fun getById(eventId: String, tenantContext: TenantContext): EventEnvelope<*>? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM event_store WHERE project_id = ? AND event_id = ?"
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(tenantContext.projectId, eventId)) { rs ->
                mapRowToEnvelope(rs)
            }
        }
    }

    override suspend fun getByAggregate(
        aggregateType: String,
        aggregateId: String,
        tenantContext: TenantContext
    ): List<EventEnvelope<*>> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM event_store 
                WHERE project_id = ? AND aggregate_type = ? AND aggregate_id = ?
                ORDER BY aggregate_version ASC, occurred_at ASC, event_id ASC
            """.trimIndent()
            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, aggregateType, aggregateId)) { rs ->
                mapRowToEnvelope(rs)
            }
        }
    }

    override suspend fun getByCorrelationId(
        correlationId: String,
        tenantContext: TenantContext
    ): List<EventEnvelope<*>> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM event_store 
                WHERE project_id = ? AND correlation_id = ?
                ORDER BY occurred_at ASC, aggregate_version ASC
            """.trimIndent()
            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, correlationId)) { rs ->
                mapRowToEnvelope(rs)
            }
        }
    }

    suspend fun getByCausationId(
        causationId: String,
        tenantContext: TenantContext
    ): List<EventEnvelope<*>> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM event_store 
                WHERE project_id = ? AND causation_id = ?
                ORDER BY occurred_at ASC
            """.trimIndent()
            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, causationId)) { rs ->
                mapRowToEnvelope(rs)
            }
        }
    }

    suspend fun getByEventType(
        eventType: DomainEventType,
        tenantContext: TenantContext
    ): List<EventEnvelope<*>> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM event_store 
                WHERE project_id = ? AND event_type = ?
                ORDER BY occurred_at ASC
            """.trimIndent()
            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, eventType.name)) { rs ->
                mapRowToEnvelope(rs)
            }
        }
    }

    suspend fun getStream(
        aggregateType: String,
        aggregateId: String,
        tenantContext: TenantContext,
        fromVersion: Long = 1L,
        limit: Int = 100
    ): List<EventEnvelope<*>> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM event_store 
                WHERE project_id = ? AND aggregate_type = ? AND aggregate_id = ? AND aggregate_version >= ?
                ORDER BY aggregate_version ASC, occurred_at ASC
                LIMIT ?
            """.trimIndent()
            txContext.sqlExecutor.queryList(
                sql,
                listOf(tenantContext.projectId, aggregateType, aggregateId, fromVersion, limit)
            ) { rs ->
                mapRowToEnvelope(rs)
            }
        }
    }
}
