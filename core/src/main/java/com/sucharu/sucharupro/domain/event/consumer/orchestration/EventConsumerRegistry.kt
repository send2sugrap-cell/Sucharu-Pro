package com.sucharu.sucharupro.domain.event.consumer.orchestration

import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe registry mapping [DomainEventType] and schema versions to explicit [ConsumerSubscription]s.
 *
 * Rules:
 * - Unrestricted wildcard subscriptions are strictly rejected.
 * - Every consumer registration must declare valid consumerId, eventType, and version.
 * - Duplicate subscription registrations with differing configurations are rejected.
 */
class EventConsumerRegistry {

    private data class RegistrationEntry(
        val subscription: ConsumerSubscription,
        val consumer: DomainEventConsumer<*>
    )

    // Keyed by "$eventType:$version"
    private val registrationsByEvent = ConcurrentHashMap<String, CopyOnWriteArrayList<RegistrationEntry>>()
    // Keyed by consumerId
    private val subscriptionsByConsumerId = ConcurrentHashMap<String, ConsumerSubscription>()

    /**
     * Registers a typed domain event consumer with explicit subscription configuration.
     */
    fun registerConsumer(
        consumer: DomainEventConsumer<*>,
        subscription: ConsumerSubscription = ConsumerSubscription(
            consumerId = consumer.consumerId,
            supportedEventType = consumer.supportedEventType,
            supportedVersion = consumer.supportedVersion
        )
    ) {
        require(consumer.consumerId.isNotBlank()) { "consumerId cannot be blank" }
        require(consumer.consumerId == subscription.consumerId) {
            "consumerId mismatch: consumer.id '${consumer.consumerId}' != subscription.id '${subscription.consumerId}'"
        }
        require(consumer.supportedEventType == subscription.supportedEventType) {
            "eventType mismatch: consumer.type '${consumer.supportedEventType}' != subscription.type '${subscription.supportedEventType}'"
        }
        require(consumer.supportedVersion == subscription.supportedVersion) {
            "version mismatch: consumer.version '${consumer.supportedVersion}' != subscription.version '${subscription.supportedVersion}'"
        }

        val eventKey = "${subscription.supportedEventType.name}:${subscription.supportedVersion}"
        val list = registrationsByEvent.computeIfAbsent(eventKey) { CopyOnWriteArrayList() }

        // Prevent duplicate consumer registrations under the same event key
        val existing = list.firstOrNull { it.subscription.consumerId == subscription.consumerId }
        if (existing != null) {
            list.remove(existing)
        }
        list.add(RegistrationEntry(subscription, consumer))
        subscriptionsByConsumerId[subscription.consumerId] = subscription
    }

    /**
     * Unregisters a consumer by consumerId.
     */
    fun unregisterConsumer(consumerId: String) {
        subscriptionsByConsumerId.remove(consumerId)
        registrationsByEvent.values.forEach { list ->
            list.removeIf { it.subscription.consumerId == consumerId }
        }
    }

    /**
     * Retrieves all matching consumer registrations for an event envelope.
     */
    fun getConsumersForEnvelope(envelope: EventEnvelope<*>): List<Pair<ConsumerSubscription, DomainEventConsumer<*>>> {
        val eventKey = "${envelope.eventType.name}:${envelope.eventVersion}"
        val list = registrationsByEvent[eventKey] ?: return emptyList()
        return list.map { Pair(it.subscription, it.consumer) }
    }

    /**
     * Retrieves subscription metadata for a consumer.
     */
    fun getSubscription(consumerId: String): ConsumerSubscription? {
        return subscriptionsByConsumerId[consumerId]
    }

    /**
     * Clears all registrations (for testing).
     */
    fun clear() {
        registrationsByEvent.clear()
        subscriptionsByConsumerId.clear()
    }
}
