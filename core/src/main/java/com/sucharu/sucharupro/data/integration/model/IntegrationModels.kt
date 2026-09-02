package com.sucharu.sucharupro.data.integration.model

import com.sucharu.sucharupro.domain.event.consumer.orchestration.IntegrationType
import java.util.UUID

/**
 * Lifecycle state of an external provider integration (INFRA-05 Step 05).
 */
enum class IntegrationStatus {
    PENDING,
    ACTIVE,
    PAUSED,
    FAILED,
    DISABLED;

    val isExecutable: Boolean get() = this == ACTIVE
}

/**
 * Processing status of an inbound webhook event.
 */
enum class WebhookEventStatus {
    RECEIVED,
    VERIFIED,
    ENQUEUED,
    PROCESSED,
    FAILED,
    DUPLICATE,
    REJECTED;

    val isTerminal: Boolean get() = this == PROCESSED || this == DUPLICATE || this == REJECTED
}

/**
 * Direction of an integration network interaction.
 */
enum class IntegrationDirection {
    INBOUND,
    OUTBOUND
}

/**
 * Canonical model for a tenant's external provider integration.
 */
data class ExternalIntegration(
    val integrationId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val provider: String,
    val integrationType: IntegrationType,
    val status: IntegrationStatus = IntegrationStatus.ACTIVE,
    val baseUrl: String,
    val configurationReference: String? = null,
    val allowedEventTypes: Set<String> = emptySet(),
    val version: String = "v1",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSuccessfulAt: Long? = null,
    val lastFailureAt: Long? = null
) {
    init {
        require(integrationId.isNotBlank()) { "integrationId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(provider.isNotBlank()) { "provider cannot be blank" }
        require(baseUrl.isNotBlank()) { "baseUrl cannot be blank" }
    }
}

/**
 * Outbound integration request representation.
 */
data class IntegrationRequest(
    val integrationId: String,
    val projectId: String,
    val provider: String,
    val method: String = "POST",
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val correlationId: String = UUID.randomUUID().toString(),
    val causationId: String? = null,
    val idempotencyKey: String? = null,
    val timeoutMs: Long = 10000L
)

/**
 * Outbound integration response representation.
 */
data class IntegrationResponse(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val durationMs: Long = 0L,
    val isSuccess: Boolean = statusCode in 200..299,
    val sanitizedError: String? = null
)

/**
 * Durable model for an inbound webhook event.
 */
data class WebhookEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val provider: String,
    val integrationId: String,
    val externalEventId: String? = null,
    val eventType: String,
    val payload: String,
    val payloadHash: String,
    val headers: Map<String, String> = emptyMap(),
    val receivedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null,
    val status: WebhookEventStatus = WebhookEventStatus.RECEIVED,
    val attemptCount: Int = 0,
    val processedAt: Long? = null,
    val correlationId: String = UUID.randomUUID().toString(),
    val causationId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Audit record for external integration interactions.
 */
data class IntegrationAuditRecord(
    val auditId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val integrationId: String,
    val provider: String,
    val operationType: String,
    val direction: IntegrationDirection,
    val status: String,
    val sanitizedError: String? = null,
    val durationMs: Long = 0L,
    val correlationId: String,
    val jobId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
