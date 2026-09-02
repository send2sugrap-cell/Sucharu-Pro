package com.sucharu.sucharupro.domain.event.store

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import java.util.UUID

/**
 * Status lifecycle of a transactional outbox message.
 */
enum class OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}

/**
 * Canonical record stored in the transactional outbox table within the same database transaction
 * as the domain business mutation.
 */
data class OutboxEventRecord(
    val outboxId: String = UUID.randomUUID().toString(),
    val eventId: String,
    val eventType: DomainEventType,
    val eventVersion: String = eventType.currentVersion,
    val projectId: String,
    val aggregateType: String,
    val aggregateId: String,
    val aggregateVersion: Long,
    val payloadJson: String,
    val headersJson: String = "{}",
    val status: OutboxStatus = OutboxStatus.PENDING,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null,
    val lastError: String? = null
) {
    init {
        require(outboxId.isNotBlank()) { "outboxId cannot be blank" }
        require(eventId.isNotBlank()) { "eventId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(aggregateType.isNotBlank()) { "aggregateType cannot be blank" }
        require(aggregateId.isNotBlank()) { "aggregateId cannot be blank" }
    }
}

/**
 * Transactional Outbox Store contract for guaranteed at-least-once event publication.
 */
interface TransactionalOutboxStore {
    /**
     * Appends an outbox record inside the active business transaction.
     */
    suspend fun appendOutboxRecord(record: OutboxEventRecord, tenantContext: TenantContext)

    /**
     * Queries pending outbox records awaiting publication dispatch.
     */
    suspend fun getPendingRecords(limit: Int = 100, tenantContext: TenantContext): List<OutboxEventRecord>

    /**
     * Marks an outbox record as successfully published.
     */
    suspend fun markPublished(outboxId: String, tenantContext: TenantContext)

    /**
     * Marks an outbox record as failed with recorded error information.
     */
    suspend fun markFailed(outboxId: String, errorReason: String, tenantContext: TenantContext)
}
