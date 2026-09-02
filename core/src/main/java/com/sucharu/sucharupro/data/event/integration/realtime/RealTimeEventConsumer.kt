package com.sucharu.sucharupro.data.event.integration.realtime

import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Production-grade real-time domain event consumer bridging domain events to WebSocket / SSE subscribers.
 */
class RealTimeEventConsumer<T : DomainEvent>(
    override val supportedEventType: DomainEventType,
    override val supportedVersion: String = supportedEventType.currentVersion,
    override val consumerId: String = "realtime.${supportedEventType.name.lowercase()}",
    private val deliveryService: RealTimeDeliveryService
) : DomainEventConsumer<T> {

    override suspend fun consume(envelope: EventEnvelope<T>): EventConsumerResult {
        return deliveryService.publish(envelope)
    }
}
