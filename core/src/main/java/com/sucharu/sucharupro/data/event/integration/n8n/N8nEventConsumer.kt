package com.sucharu.sucharupro.data.event.integration.n8n

import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Production-grade n8n domain event consumer bridging domain events to n8n webhook automations (INFRA-04 Step 03).
 */
class N8nEventConsumer<T : DomainEvent>(
    override val supportedEventType: DomainEventType,
    override val supportedVersion: String = supportedEventType.currentVersion,
    override val consumerId: String = "n8n.${supportedEventType.name.lowercase()}",
    private val dispatcher: N8nAutomationDispatcher
) : DomainEventConsumer<T> {

    override suspend fun consume(envelope: EventEnvelope<T>): EventConsumerResult {
        return dispatcher.dispatch(envelope)
    }
}
