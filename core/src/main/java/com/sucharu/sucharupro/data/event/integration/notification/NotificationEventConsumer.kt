package com.sucharu.sucharupro.data.event.integration.notification

import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Production-grade notification domain event consumer (INFRA-04 Step 03).
 */
class NotificationEventConsumer<T : DomainEvent>(
    override val supportedEventType: DomainEventType,
    override val supportedVersion: String = supportedEventType.currentVersion,
    override val consumerId: String = "notification.${supportedEventType.name.lowercase()}",
    private val dispatchService: NotificationDispatchService
) : DomainEventConsumer<T> {

    override suspend fun consume(envelope: EventEnvelope<T>): EventConsumerResult {
        val intent = NotificationIntentResolver.resolve(envelope)
            ?: return EventConsumerResult.Skipped(
                reason = "Event type '${envelope.eventType}' has no notification intent configured."
            )

        return dispatchService.dispatchIntent(intent)
    }
}
