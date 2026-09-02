package com.sucharu.sucharupro.data.event.integration.notification

import com.sucharu.sucharupro.data.notification.security.NotificationPayloadSanitizer
import com.sucharu.sucharupro.data.notification.security.NotificationSecurityPolicy
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.notification.security.NotificationDataClassification
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityContext
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityDecision
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Production-grade notification dispatch service for Sucharu Pro (INFRA-04 Step 03 / Step 07).
 *
 * Step 07 integration: when a [securityPolicy] is provided, every dispatch is evaluated
 * through the full security policy chain before reaching channel providers.
 * Backward compatible: [securityPolicy] defaults to null (no-op) to preserve existing tests.
 */
class NotificationDispatchService(
    private val recipientLookup: (projectId: String, recipientId: String) -> NotificationRecipient? = { p, r ->
        NotificationRecipient(recipientId = r, projectId = p, displayName = "Customer $r")
    },
    private val preferenceLookup: (projectId: String, recipientId: String) -> NotificationPreferences? = { p, r ->
        NotificationPreferences(recipientId = r, projectId = p)
    },
    /** Optional Step 07 security policy. Null = permissive (pre-Step-07 behavior). */
    private val securityPolicy: NotificationSecurityPolicy? = null
) {

    private val providersByChannel = ConcurrentHashMap<NotificationChannel, CopyOnWriteArrayList<NotificationProvider>>()

    fun registerProvider(provider: NotificationProvider) {
        providersByChannel.computeIfAbsent(provider.channel) { CopyOnWriteArrayList() }.add(provider)
    }

    fun clearProviders() {
        providersByChannel.clear()
    }

    /**
     * Dispatches a notification intent across all authorized, preferred channels.
     * If a [securityPolicy] is configured, it is evaluated first.
     */
    suspend fun dispatchIntent(intent: NotificationIntent): EventConsumerResult {
        if (securityPolicy != null) {
            val secContext = NotificationSecurityContext(
                principal = null,
                projectId = intent.projectId,
                intent = intent,
                classification = NotificationDataClassification.PUBLIC,
                correlationId = intent.correlationId,
                requestId = UUID.randomUUID().toString()
            )
            return when (val decision = securityPolicy.evaluateDispatch(secContext)) {
                is NotificationSecurityDecision.Deny -> EventConsumerResult.Failure(
                    reason = "Notification security denied: ${decision.reason.name} — ${decision.message}",
                    classification = EventFailureClassification.NON_RETRYABLE
                )
                is NotificationSecurityDecision.Suppress -> EventConsumerResult.Skipped(
                    reason = "Notification suppressed: ${decision.reason}"
                )
                is NotificationSecurityDecision.RateLimit -> EventConsumerResult.Failure(
                    reason = "Notification rate limited on '${decision.dimension}'. Retry after ${decision.retryAfterMs}ms.",
                    classification = EventFailureClassification.TRANSIENT
                )
                is NotificationSecurityDecision.Allow -> dispatchAllowed(decision.sanitizedIntent, decision.effectiveChannels)
                is NotificationSecurityDecision.RequireConfirmation -> EventConsumerResult.Failure(
                    reason = "Notification requires human confirmation: ${decision.reason}",
                    classification = EventFailureClassification.NON_RETRYABLE
                )
            }
        }

        return dispatchAllowed(intent, intent.targetChannels)
    }

    private suspend fun dispatchAllowed(
        intent: NotificationIntent,
        channels: Set<NotificationChannel>
    ): EventConsumerResult {
        val recipient = recipientLookup(intent.projectId, intent.targetRecipientId)
            ?: return EventConsumerResult.Failure(
                reason = "Recipient '${intent.targetRecipientId}' not found in project '${intent.projectId}'",
                classification = EventFailureClassification.NON_RETRYABLE
            )

        val preferences = preferenceLookup(intent.projectId, intent.targetRecipientId)
            ?: NotificationPreferences(recipientId = intent.targetRecipientId, projectId = intent.projectId)

        val deliveryResults = mutableListOf<NotificationDeliveryResult>()
        var successfulDeliveries = 0

        for (channel in channels) {
            if (!preferences.isChannelAllowed(channel)) continue

            val providers = providersByChannel[channel] ?: emptyList()
            if (providers.isEmpty()) continue

            val sanitized = NotificationPayloadSanitizer.sanitize(intent, channel)
            val idempotencyKey = "${intent.projectId}:${intent.eventId}:${intent.targetRecipientId}:${channel.name}"

            for (provider in providers) {
                try {
                    val result = provider.deliver(
                        recipient = recipient,
                        title = sanitized.title,
                        body = sanitized.body,
                        metadata = mapOf("correlationId" to intent.correlationId),
                        idempotencyKey = idempotencyKey
                    )
                    deliveryResults.add(result)
                    if (result.isSuccess) successfulDeliveries++
                } catch (t: Throwable) {
                    deliveryResults.add(
                        NotificationDeliveryResult(
                            channel = channel,
                            isSuccess = false,
                            errorMessage = t.message,
                            failureClassification = EventFailureClassification.TRANSIENT
                        )
                    )
                }
            }
        }

        val failures = deliveryResults.filter { !it.isSuccess }
        return if (failures.isEmpty() || successfulDeliveries > 0) {
            EventConsumerResult.Success(message = "Notification delivered to $successfulDeliveries channel(s)")
        } else {
            val firstFailure = failures.first()
            EventConsumerResult.Failure(
                reason = firstFailure.errorMessage ?: "All notification providers failed",
                classification = firstFailure.failureClassification ?: EventFailureClassification.TRANSIENT
            )
        }
    }
}
