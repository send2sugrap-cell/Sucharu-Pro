package com.sucharu.sucharupro.data.event.integration.n8n

import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Transport interface for executing HTTP requests to n8n webhooks.
 */
interface N8nWebhookTransport {
    suspend fun postWebhook(
        url: String,
        headers: Map<String, String>,
        bodyJson: String
    ): N8nHttpResponse
}

data class N8nHttpResponse(
    val statusCode: Int,
    val responseBody: String? = null,
    val errorMessage: String? = null
)

/**
 * Production-grade n8n automation dispatcher (INFRA-04 Step 03).
 */
class N8nAutomationDispatcher(
    private val config: N8nConfig = N8nConfig(),
    private val transport: N8nWebhookTransport? = null
) {

    /**
     * Dispatches an event envelope to the configured n8n webhook endpoint.
     */
    suspend fun dispatch(envelope: EventEnvelope<*>): EventConsumerResult {
        if (!config.isEnabled) {
            return EventConsumerResult.Skipped("n8n automation dispatch is disabled by configuration.")
        }

        // Try building sanitized payload (throws IllegalArgumentException on restricted security events)
        val payloadPair = try {
            N8nPayloadBuilder.buildPayload(envelope, config.signingSecret)
        } catch (e: IllegalArgumentException) {
            return EventConsumerResult.Failure(
                reason = e.message ?: "Restricted security event cannot be dispatched to n8n.",
                classification = EventFailureClassification.SECURITY
            )
        }

        val (payload, rawJson) = payloadPair
        val url = "${config.webhookBaseUrl}/${envelope.eventType.name.lowercase()}"
        val headers = mapOf(
            "Content-Type" to "application/json",
            "X-Sucharu-Signature" to (payload.webhookSignature ?: ""),
            "X-Sucharu-Event-Id" to envelope.eventId,
            "X-Sucharu-Project-Id" to envelope.projectId,
            "X-Sucharu-Correlation-Id" to envelope.correlationId,
            "X-Sucharu-Timestamp" to envelope.occurredAt.toString()
        )

        if (transport == null) {
            // Unconfigured / simulated transport must not report genuine delivery success
            return EventConsumerResult.Skipped(
                reason = "n8n webhook transport is not configured (simulated/no-op mode)."
            )
        }

        val response = try {
            transport.postWebhook(url, headers, rawJson)
        } catch (t: Throwable) {
            return EventConsumerResult.Failure(
                reason = "n8n connection failure: ${t.message}",
                classification = EventFailureClassification.TRANSIENT,
                cause = t
            )
        }

        return when (response.statusCode) {
            in 200..299 -> {
                EventConsumerResult.Success(message = "n8n accepted event with HTTP ${response.statusCode}")
            }
            401, 403 -> {
                EventConsumerResult.Failure(
                    reason = "n8n webhook authorization rejected (HTTP ${response.statusCode})",
                    classification = EventFailureClassification.SECURITY
                )
            }
            in 400..499 -> {
                EventConsumerResult.Failure(
                    reason = "n8n rejected payload with client error (HTTP ${response.statusCode}): ${response.errorMessage ?: response.responseBody}",
                    classification = EventFailureClassification.VALIDATION
                )
            }
            else -> {
                EventConsumerResult.Failure(
                    reason = "n8n server error (HTTP ${response.statusCode}): ${response.errorMessage ?: response.responseBody}",
                    classification = EventFailureClassification.TRANSIENT
                )
            }
        }
    }
}
