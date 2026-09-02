package com.sucharu.sucharupro.domain.event.boundary

import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Standard channels for notification delivery.
 */
enum class NotificationChannel {
    IN_APP,
    EMAIL,
    SMS,
    PUSH
}

/**
 * Decoupled notification message intent extracted from a domain event envelope.
 */
data class NotificationIntent(
    val eventId: String,
    val eventType: DomainEventType,
    val projectId: String,
    val targetRecipientId: String,
    val targetChannels: Set<NotificationChannel>,
    val title: String,
    val body: String,
    val correlationId: String,
    val deepLinkUrl: String? = null
)

/**
 * Decoupled Notification boundary preparing domain events for notification consumers (INFRA-04 Step 01).
 *
 * Remains completely provider-agnostic (no Firebase, SMTP, Twilio, or SMS SDK imports).
 */
object NotificationEventBoundary {

    /**
     * Determines default eligible notification channels for a domain event type.
     */
    fun resolveChannelsForEventType(eventType: DomainEventType): Set<NotificationChannel> {
        return when (eventType) {
            DomainEventType.ORDER_CREATED,
            DomainEventType.DELIVERY_DISPATCHED,
            DomainEventType.DELIVERY_DELIVERED,
            DomainEventType.PAYMENT_RECEIVED -> setOf(
                NotificationChannel.IN_APP,
                NotificationChannel.EMAIL,
                NotificationChannel.SMS,
                NotificationChannel.PUSH
            )

            DomainEventType.PRODUCTION_STARTED,
            DomainEventType.PRODUCTION_COMPLETED,
            DomainEventType.QC_PASSED,
            DomainEventType.QC_FAILED,
            DomainEventType.STOCK_RECEIVED,
            DomainEventType.STOCK_ISSUED -> setOf(
                NotificationChannel.IN_APP,
                NotificationChannel.PUSH
            )

            DomainEventType.AUTH_FAILED,
            DomainEventType.SESSION_REVOKED,
            DomainEventType.ACCOUNT_LOCKED,
            DomainEventType.PASSWORD_CHANGED -> setOf(
                NotificationChannel.IN_APP,
                NotificationChannel.EMAIL,
                NotificationChannel.SMS
            )

            else -> setOf(NotificationChannel.IN_APP)
        }
    }

    /**
     * Formats a generic notification intent from an envelope.
     */
    fun createNotificationIntent(
        envelope: EventEnvelope<*>,
        targetRecipientId: String,
        title: String,
        body: String,
        channels: Set<NotificationChannel> = resolveChannelsForEventType(envelope.eventType),
        deepLinkUrl: String? = null
    ): NotificationIntent {
        return NotificationIntent(
            eventId = envelope.eventId,
            eventType = envelope.eventType,
            projectId = envelope.projectId,
            targetRecipientId = targetRecipientId,
            targetChannels = channels,
            title = title,
            body = body,
            correlationId = envelope.correlationId,
            deepLinkUrl = deepLinkUrl
        )
    }
}
