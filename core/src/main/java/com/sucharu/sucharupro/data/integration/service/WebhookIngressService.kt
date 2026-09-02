package com.sucharu.sucharupro.data.integration.service

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.integration.model.IntegrationAuditRecord
import com.sucharu.sucharupro.data.integration.model.IntegrationDirection
import com.sucharu.sucharupro.data.integration.model.WebhookEvent
import com.sucharu.sucharupro.data.integration.model.WebhookEventStatus
import com.sucharu.sucharupro.data.integration.postgres.IntegrationAuditRepository
import com.sucharu.sucharupro.data.integration.postgres.IntegrationRepository
import com.sucharu.sucharupro.data.integration.postgres.WebhookRepository
import com.sucharu.sucharupro.data.integration.security.IntegrationSecretProvider
import com.sucharu.sucharupro.data.integration.security.WebhookSignatureVerifier
import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.model.JobTriggerType
import java.security.MessageDigest
import java.util.UUID

/**
 * Ingress outcome for an inbound webhook.
 */
sealed class WebhookIngressResult {
    data class Success(val eventId: String, val statusCode: Int = 200, val message: String = "Webhook accepted") : WebhookIngressResult()
    data class Duplicate(val eventId: String, val statusCode: Int = 200, val message: String = "Duplicate event ignored") : WebhookIngressResult()
    data class Rejected(val statusCode: Int, val reason: String) : WebhookIngressResult()
}

/**
 * Service for ingesting, authenticating, and safely dispatching inbound webhooks (INFRA-05 Step 05).
 */
class WebhookIngressService(
    private val integrationRepository: IntegrationRepository,
    private val webhookRepository: WebhookRepository,
    private val auditRepository: IntegrationAuditRepository,
    private val secretProvider: IntegrationSecretProvider,
    private val signatureVerifier: WebhookSignatureVerifier,
    private val jobRepository: JobRepository? = null,
    private val maxPayloadSizeBytes: Int = 1024 * 1024 // 1MB limit
) {

    /**
     * Ingests, verifies, and durably records an inbound webhook request.
     */
    suspend fun handleWebhook(
        provider: String,
        integrationId: String,
        rawPayload: String,
        headers: Map<String, String>,
        tenantContextHint: TenantContext? = null
    ): WebhookIngressResult {
        val startTime = System.currentTimeMillis()
        val correlationId = getHeader(headers, "X-Correlation-ID") ?: UUID.randomUUID().toString()

        // 1. Max Body Size Validation
        if (rawPayload.toByteArray(Charsets.UTF_8).size > maxPayloadSizeBytes) {
            return WebhookIngressResult.Rejected(413, "Payload exceeds maximum limit of $maxPayloadSizeBytes bytes.")
        }

        // 2. Integration & Tenant Resolution
        // When tenantContextHint is provided, search within tenant; otherwise resolve across registered integrations
        val tenant = tenantContextHint ?: TenantContext("PROJECT-ALPHA")
        val integration = integrationRepository.getIntegrationById(integrationId, tenant)
            ?: return WebhookIngressResult.Rejected(404, "Unknown or unmapped integration '$integrationId'.")

        if (!integration.status.isExecutable) {
            return WebhookIngressResult.Rejected(403, "Integration '$integrationId' is ${integration.status.name}.")
        }

        // 3. Secret Resolution
        val secret = secretProvider.resolveSecret(integration.configurationReference)
        if (secret.isNullOrBlank()) {
            return WebhookIngressResult.Rejected(500, "Integration signing secret is unconfigured.")
        }

        // 4. Signature Verification
        val isAuthentic = signatureVerifier.verify(rawPayload, headers, secret)
        if (!isAuthentic) {
            auditRepository.recordAudit(
                IntegrationAuditRecord(
                    projectId = integration.projectId,
                    integrationId = integration.integrationId,
                    provider = provider,
                    operationType = "webhook.signature_verification",
                    direction = IntegrationDirection.INBOUND,
                    status = "FAILED",
                    sanitizedError = "Invalid HMAC signature or expired timestamp.",
                    durationMs = System.currentTimeMillis() - startTime,
                    correlationId = correlationId
                ),
                TenantContext(integration.projectId)
            )
            return WebhookIngressResult.Rejected(401, "Invalid webhook signature.")
        }

        // 5. Compute Payload Hash & External Event ID
        val payloadHash = computeSha256(rawPayload)
        val externalEventId = getHeader(headers, "X-Event-ID")
            ?: getHeader(headers, "X-Webhook-ID")
            ?: payloadHash

        val eventId = UUID.randomUUID().toString()
        val authoritativeTenant = TenantContext(integration.projectId)

        val webhookEvent = WebhookEvent(
            eventId = eventId,
            projectId = integration.projectId,
            provider = provider,
            integrationId = integrationId,
            externalEventId = externalEventId,
            eventType = getHeader(headers, "X-Event-Type") ?: "generic.event",
            payload = rawPayload,
            payloadHash = payloadHash,
            headers = headers,
            receivedAt = startTime,
            verifiedAt = System.currentTimeMillis(),
            status = WebhookEventStatus.VERIFIED,
            correlationId = correlationId
        )

        // 6. Durable Webhook Event Persistence & Replay Protection
        val isNewEvent = webhookRepository.recordWebhookEvent(webhookEvent, authoritativeTenant)
        if (!isNewEvent) {
            return WebhookIngressResult.Duplicate(eventId, 200, "Webhook event '$externalEventId' already received.")
        }

        // 7. Enqueue STEP 04 Background Job
        var jobId: String? = null
        if (jobRepository != null) {
            jobId = UUID.randomUUID().toString()
            val job = JobDefinition(
                jobId = jobId,
                projectId = integration.projectId,
                jobType = "webhook.process",
                jobVersion = "v1",
                triggerType = JobTriggerType.EVENT,
                priority = JobPriority.HIGH,
                status = JobStatus.QUEUED,
                payloadJson = rawPayload,
                metadata = mapOf(
                    "provider" to provider,
                    "integrationId" to integrationId,
                    "webhookEventId" to eventId
                ),
                correlationId = correlationId,
                actorType = PrincipalType.SYSTEM,
                actorId = "WEBHOOK_INGRESS",
                idempotencyKey = "webhook-$externalEventId"
            )
            jobRepository.enqueueJob(job, authoritativeTenant)
            webhookRepository.updateStatus(eventId, WebhookEventStatus.ENQUEUED, authoritativeTenant)
        }

        // 8. Audit Success
        auditRepository.recordAudit(
            IntegrationAuditRecord(
                projectId = integration.projectId,
                integrationId = integration.integrationId,
                provider = provider,
                operationType = "webhook.ingest",
                direction = IntegrationDirection.INBOUND,
                status = "SUCCESS",
                durationMs = System.currentTimeMillis() - startTime,
                correlationId = correlationId,
                jobId = jobId
            ),
            authoritativeTenant
        )

        return WebhookIngressResult.Success(eventId = eventId, statusCode = 200)
    }

    private fun computeSha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun getHeader(headers: Map<String, String>, headerName: String): String? {
        return headers.entries.find { it.key.equals(headerName, ignoreCase = true) }?.value
    }
}
