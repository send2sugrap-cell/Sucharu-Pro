package com.sucharu.sucharupro.data.event.integration.n8n

import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.domain.event.boundary.N8nIntegrationBoundary
import com.sucharu.sucharupro.domain.event.boundary.N8nWebhookPayload
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Production-grade payload builder and HMAC signature generator for n8n webhooks (INFRA-04 Step 03).
 */
object N8nPayloadBuilder {

    /**
     * Computes HMAC-SHA256 signature hex string over the provided payload string using a secret.
     */
    fun computeHmacSha256(data: String, secret: String): String {
        val algorithm = "HmacSHA256"
        val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secretKey)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Builds a sanitized n8n webhook payload with signature.
     */
    fun buildPayload(envelope: EventEnvelope<*>, secret: String): Pair<N8nWebhookPayload, String> {
        val base = N8nIntegrationBoundary.toSanitizedWebhookPayload(envelope)

        val jsonMap = mutableMapOf<String, Any?>(
            "eventId" to base.eventId,
            "eventType" to base.eventType,
            "eventVersion" to base.eventVersion,
            "projectId" to base.projectId,
            "aggregateType" to base.aggregateType,
            "aggregateId" to base.aggregateId,
            "correlationId" to base.correlationId,
            "causationId" to base.causationId,
            "occurredAt" to base.occurredAt,
            "payloadSummary" to base.payloadSummary
        )
        val rawJson = EventSerializationHelper.serializeMap(jsonMap)
        val signature = computeHmacSha256(rawJson, secret)

        val signedPayload = base.copy(webhookSignature = signature)
        return Pair(signedPayload, rawJson)
    }
}
