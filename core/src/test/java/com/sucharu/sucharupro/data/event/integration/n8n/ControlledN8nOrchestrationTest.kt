package com.sucharu.sucharupro.data.event.integration.n8n

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.job.integration.n8n.N8nJobTriggerAdapter
import com.sucharu.sucharupro.data.job.integration.n8n.N8nJobTriggerResult
import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.N8nIntegrationBoundary
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationFailedEvent
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ControlledN8nOrchestrationTest {

    private lateinit var config: N8nConfig
    private lateinit var mockJobRepo: JobRepository
    private lateinit var triggerAdapter: N8nJobTriggerAdapter

    @Before
    fun setUp() {
        config = N8nConfig(
            webhookBaseUrl = "https://n8n.sucharupro.com/webhook/test",
            signingSecret = "test_n8n_secret_key_32_characters_long"
        )

        mockJobRepo = object : JobRepository {
            override suspend fun enqueueJob(job: JobDefinition, tenantContext: TenantContext): Boolean = true
            override suspend fun claimEligibleJobs(workerId: String, limit: Int, leaseDurationMs: Long, tenantContext: TenantContext): List<JobDefinition> = emptyList()
            override suspend fun markSucceeded(jobId: String, tenantContext: TenantContext) {}
            override suspend fun markFailed(jobId: String, errorCode: String?, errorMessage: String?, classification: EventFailureClassification, nextAttemptAt: Long?, tenantContext: TenantContext) {}
            override suspend fun markDeadLetter(jobId: String, errorCode: String?, errorMessage: String?, classification: EventFailureClassification, tenantContext: TenantContext) {}
            override suspend fun markCancelled(jobId: String, reason: String, tenantContext: TenantContext) {}
            override suspend fun getJobById(jobId: String, tenantContext: TenantContext): JobDefinition? = null
            override suspend fun listQueuedJobs(tenantContext: TenantContext, limit: Int): List<JobDefinition> = emptyList()
            override suspend fun recoverExpiredLeases(tenantContext: TenantContext): Int = 0
        }

        triggerAdapter = N8nJobTriggerAdapter(config, mockJobRepo)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `n8nIntegrationBoundary_blocksRestrictedSecurityEventsFromExport`() {
        val authFailedPayload = AuthenticationFailedEvent(
            attemptedIdentifierMasked = "user@sucharu.com",
            failureReason = "INVALID_CREDENTIALS",
            clientIpMasked = "192.168.1.1"
        )

        val secEnvelope = EventEnvelope(
            eventId = "EVT-SEC-01",
            eventType = DomainEventType.AUTH_FAILED,
            eventVersion = "v1",
            occurredAt = System.currentTimeMillis(),
            publishedAt = System.currentTimeMillis(),
            projectId = "TENANT-001",
            aggregateType = "SECURITY",
            aggregateId = "USR-1001",
            aggregateVersion = 1L,
            actorType = PrincipalType.HUMAN,
            actorId = "USR-1001",
            correlationId = "CORR-01",
            causationId = null,
            source = "auth-service",
            payload = authFailedPayload
        )

        // Must throw IllegalArgumentException
        N8nIntegrationBoundary.toSanitizedWebhookPayload(secEnvelope)
    }

    @Test
    fun `n8nWebhookTrigger_verifiesHmacSignatureAndRejectsTamperedPayload`() = runBlocking {
        val payload = "{\"orderId\":\"ORD-1001\",\"action\":\"TRIGGER_WORKFLOW\"}"
        val now = System.currentTimeMillis().toString()
        val validSig = computeHmac(payload, config.signingSecret)

        // 1. Valid Signature & Timestamp -> ACCEPTED
        val validResult = triggerAdapter.triggerJobFromN8n(
            payloadJson = payload,
            signatureHeader = validSig,
            timestampHeader = now,
            jobType = "order.workflow.trigger",
            idempotencyKey = "IDEM-N8N-001",
            tenantContext = TenantContext("TENANT-001")
        )

        assertTrue(validResult is N8nJobTriggerResult.Accepted)
        assertNotNull((validResult as N8nJobTriggerResult.Accepted).jobId)

        // 2. Tampered Signature -> REJECTED
        val tamperedResult = triggerAdapter.triggerJobFromN8n(
            payloadJson = payload,
            signatureHeader = "invalid_tampered_signature_12345",
            timestampHeader = now,
            jobType = "order.workflow.trigger",
            idempotencyKey = "IDEM-N8N-002",
            tenantContext = TenantContext("TENANT-001")
        )

        assertTrue(tamperedResult is N8nJobTriggerResult.Rejected)
        val rejected = tamperedResult as N8nJobTriggerResult.Rejected
        assertTrue(rejected.isSecurityViolation)
        assertEquals("Invalid HMAC signature verification", rejected.reason)
    }

    @Test
    fun `n8nWebhookTrigger_rejectsSecurityJobTypes`() = runBlocking {
        val payload = "{\"action\":\"ELEVATE_PERMISSIONS\"}"
        val now = System.currentTimeMillis().toString()
        val validSig = computeHmac(payload, config.signingSecret)

        val secResult = triggerAdapter.triggerJobFromN8n(
            payloadJson = payload,
            signatureHeader = validSig,
            timestampHeader = now,
            jobType = "security.privilege_escalation",
            idempotencyKey = "IDEM-N8N-003",
            tenantContext = TenantContext("TENANT-001")
        )

        assertTrue(secResult is N8nJobTriggerResult.Rejected)
        val rejected = secResult as N8nJobTriggerResult.Rejected
        assertTrue(rejected.isSecurityViolation)
        assertTrue(rejected.reason.contains("security jobType"))
    }

    private fun computeHmac(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
