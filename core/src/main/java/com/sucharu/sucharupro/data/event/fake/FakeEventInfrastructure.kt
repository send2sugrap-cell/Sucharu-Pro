package com.sucharu.sucharupro.data.event.fake

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.dispatcher.DomainEventDispatcher
import com.sucharu.sucharupro.domain.event.idempotency.EventIdempotencyStore
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingRecord
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingStatus
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.publisher.DomainEventPublisher
import com.sucharu.sucharupro.domain.event.publisher.PublishResult
import com.sucharu.sucharupro.domain.event.store.EventStore
import com.sucharu.sucharupro.domain.event.store.OutboxEventRecord
import com.sucharu.sucharupro.domain.event.store.OutboxStatus
import com.sucharu.sucharupro.domain.event.store.TransactionalOutboxStore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory thread-safe [DomainEventPublisher] for unit testing and offline development.
 */
class FakeDomainEventPublisher(
    private val dispatcher: DomainEventDispatcher? = null
) : DomainEventPublisher {

    private val _publishedEvents = CopyOnWriteArrayList<EventEnvelope<*>>()
    val publishedEvents: List<EventEnvelope<*>> get() = _publishedEvents.toList()

    override suspend fun <T : DomainEvent> publish(envelope: EventEnvelope<T>): PublishResult {
        _publishedEvents.add(envelope)
        if (dispatcher != null) {
            dispatcher.dispatch(envelope)
        }
        return PublishResult.Success(eventId = envelope.eventId, publishedAt = envelope.publishedAt)
    }

    override suspend fun publishAll(envelopes: List<EventEnvelope<*>>): List<PublishResult> {
        val results = mutableListOf<PublishResult>()
        for (envelope in envelopes) {
            @Suppress("UNCHECKED_CAST")
            val typedEnvelope = envelope as EventEnvelope<DomainEvent>
            results.add(publish(typedEnvelope))
        }
        return results
    }

    fun clear() {
        _publishedEvents.clear()
    }
}

/**
 * In-memory thread-safe [EventStore] enforcing strict [TenantContext] isolation.
 */
class FakeEventStore : EventStore {

    // Keyed by "$projectId:$eventId"
    private val eventsByProjectAndId = ConcurrentHashMap<String, EventEnvelope<*>>()
    private val allEvents = CopyOnWriteArrayList<EventEnvelope<*>>()

    override suspend fun append(envelope: EventEnvelope<*>, tenantContext: TenantContext) {
        require(envelope.projectId == tenantContext.projectId) {
            "Tenant isolation violation: Envelope projectId '${envelope.projectId}' does not match context projectId '${tenantContext.projectId}'."
        }
        val key = "${tenantContext.projectId}:${envelope.eventId}"
        if (eventsByProjectAndId.putIfAbsent(key, envelope) != null) {
            throw IllegalArgumentException("Duplicate eventId '${envelope.eventId}' already exists in project '${tenantContext.projectId}'.")
        }
        allEvents.add(envelope)
    }

    override suspend fun appendAll(envelopes: List<EventEnvelope<*>>, tenantContext: TenantContext) {
        for (env in envelopes) {
            append(env, tenantContext)
        }
    }

    override suspend fun getById(eventId: String, tenantContext: TenantContext): EventEnvelope<*>? {
        val key = "${tenantContext.projectId}:$eventId"
        return eventsByProjectAndId[key]
    }

    override suspend fun getByAggregate(
        aggregateType: String,
        aggregateId: String,
        tenantContext: TenantContext
    ): List<EventEnvelope<*>> {
        return allEvents.filter {
            it.projectId == tenantContext.projectId &&
                    it.aggregateType == aggregateType &&
                    it.aggregateId == aggregateId
        }.sortedBy { it.aggregateVersion }
    }

    override suspend fun getByCorrelationId(
        correlationId: String,
        tenantContext: TenantContext
    ): List<EventEnvelope<*>> {
        return allEvents.filter {
            it.projectId == tenantContext.projectId && it.correlationId == correlationId
        }.sortedBy { it.occurredAt }
    }

    fun clear() {
        eventsByProjectAndId.clear()
        allEvents.clear()
    }
}

/**
 * In-memory thread-safe [EventIdempotencyStore].
 */
class FakeIdempotencyStore : EventIdempotencyStore {

    // Keyed by "$projectId:$consumerId:$eventId"
    private val records = ConcurrentHashMap<String, EventProcessingRecord>()

    override suspend fun isProcessed(eventId: String, consumerId: String, projectId: String): Boolean {
        val key = "$projectId:$consumerId:$eventId"
        val record = records[key] ?: return false
        return record.status == EventProcessingStatus.PROCESSED
    }

    override suspend fun recordProcessing(record: EventProcessingRecord) {
        val key = "${record.projectId}:${record.consumerId}:${record.eventId}"
        records[key] = record
    }

    override suspend fun getRecord(
        eventId: String,
        consumerId: String,
        projectId: String
    ): EventProcessingRecord? {
        val key = "$projectId:$consumerId:$eventId"
        return records[key]
    }

    fun clear() {
        records.clear()
    }
}

/**
 * In-memory thread-safe [TransactionalOutboxStore].
 */
class FakeTransactionalOutboxStore : TransactionalOutboxStore {

    private val outboxRecords = ConcurrentHashMap<String, OutboxEventRecord>()

    override suspend fun appendOutboxRecord(record: OutboxEventRecord, tenantContext: TenantContext) {
        require(record.projectId == tenantContext.projectId) {
            "Tenant isolation violation: Outbox record projectId '${record.projectId}' does not match context '${tenantContext.projectId}'."
        }
        outboxRecords[record.outboxId] = record
    }

    override suspend fun getPendingRecords(
        limit: Int,
        tenantContext: TenantContext
    ): List<OutboxEventRecord> {
        return outboxRecords.values
            .filter { it.projectId == tenantContext.projectId && it.status == OutboxStatus.PENDING }
            .sortedBy { it.createdAt }
            .take(limit)
    }

    override suspend fun markPublished(outboxId: String, tenantContext: TenantContext) {
        val current = outboxRecords[outboxId] ?: return
        require(current.projectId == tenantContext.projectId) { "Tenant isolation mismatch." }
        outboxRecords[outboxId] = current.copy(
            status = OutboxStatus.PUBLISHED,
            publishedAt = System.currentTimeMillis()
        )
    }

    override suspend fun markFailed(
        outboxId: String,
        errorReason: String,
        tenantContext: TenantContext
    ) {
        val current = outboxRecords[outboxId] ?: return
        require(current.projectId == tenantContext.projectId) { "Tenant isolation mismatch." }
        outboxRecords[outboxId] = current.copy(
            status = OutboxStatus.FAILED,
            retryCount = current.retryCount + 1,
            lastError = errorReason
        )
    }

    fun clear() {
        outboxRecords.clear()
    }
}

/**
 * Configurable test consumer for unit testing event dispatch and failure handling.
 */
class FakeDomainEventConsumer<T : DomainEvent>(
    override val consumerId: String,
    override val supportedEventType: DomainEventType,
    override val supportedVersion: String = supportedEventType.currentVersion,
    var configuredResult: EventConsumerResult = EventConsumerResult.Success()
) : DomainEventConsumer<T> {

    private val _consumedEnvelopes = CopyOnWriteArrayList<EventEnvelope<T>>()
    val consumedEnvelopes: List<EventEnvelope<T>> get() = _consumedEnvelopes.toList()

    override suspend fun consume(envelope: EventEnvelope<T>): EventConsumerResult {
        _consumedEnvelopes.add(envelope)
        return configuredResult
    }

    fun clear() {
        _consumedEnvelopes.clear()
    }
}
