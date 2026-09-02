package com.sucharu.sucharupro.domain.event.model.events

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType

/**
 * Emitted when a notification dispatch or replay is denied by the security policy (INFRA-04 Step 07).
 */
data class NotificationAuthorizationDeniedEvent(
    val notificationId: String?,
    val recipientId: String,
    val denialReason: String,
    val attemptedChannel: String? = null,
    val actorId: String? = null,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.NOTIFICATION_AUTHORIZATION_DENIED
    override val aggregateId: String get() = notificationId ?: recipientId
    override val aggregateType: String get() = "NOTIFICATION_SECURITY"

    init {
        require(recipientId.isNotBlank()) { "recipientId cannot be blank" }
        require(denialReason.isNotBlank()) { "denialReason cannot be blank" }
    }
}

/**
 * Emitted when a notification is suppressed (INFRA-04 Step 07).
 */
data class NotificationSuppressedEvent(
    val recipientId: String,
    val channel: String?,
    val suppressionReason: String,
    val suppressionType: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.NOTIFICATION_SUPPRESSED
    override val aggregateId: String get() = recipientId
    override val aggregateType: String get() = "NOTIFICATION_SECURITY"

    init {
        require(recipientId.isNotBlank()) { "recipientId cannot be blank" }
        require(suppressionReason.isNotBlank()) { "suppressionReason cannot be blank" }
    }
}

/**
 * Emitted when a notification rate limit is exceeded (INFRA-04 Step 07).
 */
data class NotificationRateLimitTriggeredEvent(
    val dimensionKey: String,
    val recipientId: String?,
    val channel: String?,
    val retryAfterMs: Long,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.NOTIFICATION_RATE_LIMIT_TRIGGERED
    override val aggregateId: String get() = dimensionKey
    override val aggregateType: String get() = "NOTIFICATION_SECURITY"

    init {
        require(dimensionKey.isNotBlank()) { "dimensionKey cannot be blank" }
    }
}

/**
 * Emitted when an abuse signal or pattern is detected (INFRA-04 Step 07).
 */
data class NotificationAbuseDetectedEvent(
    val signalType: String,
    val description: String,
    val severity: String,
    val recipientId: String?,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.NOTIFICATION_ABUSE_DETECTED
    override val aggregateId: String get() = recipientId ?: signalType
    override val aggregateType: String get() = "NOTIFICATION_SECURITY"

    init {
        require(signalType.isNotBlank()) { "signalType cannot be blank" }
        require(description.isNotBlank()) { "description cannot be blank" }
    }
}

/**
 * Emitted when a notification replay request is denied (INFRA-04 Step 07).
 */
data class NotificationReplayDeniedEvent(
    val originalEventId: String,
    val actorId: String?,
    val denialReason: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.NOTIFICATION_REPLAY_DENIED
    override val aggregateId: String get() = originalEventId
    override val aggregateType: String get() = "NOTIFICATION_SECURITY"

    init {
        require(originalEventId.isNotBlank()) { "originalEventId cannot be blank" }
        require(denialReason.isNotBlank()) { "denialReason cannot be blank" }
    }
}

/**
 * Emitted when a notification provider returns an invalid callback signature or authentication failure (INFRA-04 Step 07).
 */
data class NotificationProviderSecurityFailureEvent(
    val providerName: String,
    val failureType: String,
    val sanitizedDetails: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.NOTIFICATION_PROVIDER_SECURITY_FAILURE
    override val aggregateId: String get() = providerName
    override val aggregateType: String get() = "NOTIFICATION_SECURITY"

    init {
        require(providerName.isNotBlank()) { "providerName cannot be blank" }
        require(failureType.isNotBlank()) { "failureType cannot be blank" }
    }
}
