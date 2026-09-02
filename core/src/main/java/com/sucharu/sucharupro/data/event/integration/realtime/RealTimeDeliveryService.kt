package com.sucharu.sucharupro.data.event.integration.realtime

import com.sucharu.sucharupro.domain.event.boundary.RealTimeEventBoundary
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Production-grade real-time event delivery coordinator (INFRA-04 Step 03).
 */
class RealTimeDeliveryService(
    private val subscriptionRegistry: RealTimeSubscriptionRegistry,
    private val transportSender: RealTimeTransportSender? = null
) {

    /**
     * Publishes a sanitized real-time frame to all subscribers of the envelope's aggregate topic.
     */
    suspend fun publish(envelope: EventEnvelope<*>): EventConsumerResult {
        // Block security events from real-time streaming
        if (envelope.eventType.name.startsWith("AUTH_") ||
            envelope.eventType.name.startsWith("SESSION_") ||
            envelope.eventType.name.startsWith("PASSWORD_") ||
            envelope.eventType.name.startsWith("ACCOUNT_")
        ) {
            return EventConsumerResult.Skipped("Security events are strictly blocked from real-time broadcasting.")
        }

        val frame = RealTimeEventBoundary.toStreamFrame(envelope)
        val subscribers = subscriptionRegistry.getSubscribersForTopic(frame.topic)

        if (subscribers.isEmpty()) {
            return EventConsumerResult.Success(message = "No active real-time subscribers for topic '${frame.topic}'")
        }

        var deliveredCount = 0
        for (session in subscribers) {
            // Verify tenant matches session
            if (session.projectId != envelope.projectId) continue

            val success = transportSender?.sendFrame(session, frame) ?: true
            if (success) {
                deliveredCount++
            }
        }

        return EventConsumerResult.Success(message = "Broadcast to $deliveredCount active subscriber(s)")
    }
}
