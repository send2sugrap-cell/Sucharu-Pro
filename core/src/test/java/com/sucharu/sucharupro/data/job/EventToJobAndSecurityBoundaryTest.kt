package com.sucharu.sucharupro.data.job

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.event.integration.n8n.N8nConfig
import com.sucharu.sucharupro.data.job.integration.EventToJobDispatcher
import com.sucharu.sucharupro.data.job.integration.aiagent.AiAgentJobAuthResult
import com.sucharu.sucharupro.data.job.integration.aiagent.AiAgentJobSecurityBoundary
import com.sucharu.sucharupro.data.job.integration.n8n.N8nJobTriggerAdapter
import com.sucharu.sucharupro.data.job.integration.n8n.N8nJobTriggerResult
import com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.model.JobTriggerType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class EventToJobAndSecurityBoundaryTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var jobRepo: PostgresJobRepository
    private lateinit var eventToJobDispatcher: EventToJobDispatcher
    private lateinit var n8nConfig: N8nConfig
    private lateinit var n8nAdapter: N8nJobTriggerAdapter

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        jobRepo = PostgresJobRepository(mockDb)
        eventToJobDispatcher = EventToJobDispatcher(jobRepo)
        n8nConfig = N8nConfig(
            webhookBaseUrl = "https://n8n.sucharu.internal/webhook/job",
            signingSecret = "test-n8n-signing-secret-sucharu-12345"
        )
        n8nAdapter = N8nJobTriggerAdapter(n8nConfig, jobRepo)
    }

    private fun computeHmac(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testEventToJobDispatcherCreatesQueuedJob() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent(
                    orderId = "ord-1",
                    customerId = "cust-1",
                    totalAmount = BigDecimal("5000.00"),
                    itemCount = 2
                ),
                projectId = "tenant_alpha",
                actor = EventActor.human("USER-1")
            )

            val jobId = eventToJobDispatcher.dispatchEventToJob(
                envelope = envelope,
                targetJobType = "order.generate_production_sheet",
                priority = JobPriority.HIGH,
                tenantContext = tenant
            )

            assertNotNull(jobId)

            val job = jobRepo.getJobById(jobId, tenant)
            assertNotNull(job)
            assertEquals("order.generate_production_sheet", job?.jobType)
            assertEquals(JobPriority.HIGH, job?.priority)
            assertEquals(JobTriggerType.EVENT, job?.triggerType)
            assertEquals(JobStatus.QUEUED, job?.status)
            assertEquals(envelope.correlationId, job?.correlationId)
            assertEquals("event:${envelope.eventId}:order.generate_production_sheet", job?.idempotencyKey)
        }
    }

    @Test
    fun testAiAgentJobSecurityBoundaryGrantsPermittedJobs() {
        val tenant = TenantContext("tenant_alpha")
        val aiPrincipal = AuthenticatedPrincipal(
            userId = "agent-1",
            username = "sucharu-agent",
            role = UserRole.AI_AGENT,
            projectId = "tenant_alpha",
            principalType = PrincipalType.AI_AGENT
        )

        val authResult = AiAgentJobSecurityBoundary.evaluateJobSubmission(
            principal = aiPrincipal,
            jobType = "order.analyze_print_specifications",
            metadata = emptyMap(),
            tenantContext = tenant
        )

        assertEquals(AiAgentJobAuthResult.Authorized, authResult)
    }

    @Test
    fun testAiAgentJobSecurityBoundaryDeniesCrossTenant() {
        val tenantTarget = TenantContext("tenant_beta")
        val aiPrincipal = AuthenticatedPrincipal(
            userId = "agent-1",
            username = "sucharu-agent",
            role = UserRole.AI_AGENT,
            projectId = "tenant_alpha",
            principalType = PrincipalType.AI_AGENT
        )

        val authResult = AiAgentJobSecurityBoundary.evaluateJobSubmission(
            principal = aiPrincipal,
            jobType = "order.analyze_print_specifications",
            metadata = emptyMap(),
            tenantContext = tenantTarget
        )

        assertTrue(authResult is AiAgentJobAuthResult.Denied)
    }

    @Test
    fun testAiAgentJobSecurityBoundaryRequiresHumanConfirmationForHighImpact() {
        val tenant = TenantContext("tenant_alpha")
        val aiPrincipal = AuthenticatedPrincipal(
            userId = "agent-1",
            username = "sucharu-agent",
            role = UserRole.AI_AGENT,
            projectId = "tenant_alpha",
            principalType = PrincipalType.AI_AGENT
        )

        // Without human confirmation metadata
        val authResult = AiAgentJobSecurityBoundary.evaluateJobSubmission(
            principal = aiPrincipal,
            jobType = "finance.execute_bulk_payout",
            metadata = emptyMap(),
            tenantContext = tenant
        )
        assertTrue(authResult is AiAgentJobAuthResult.RequiresConfirmation)

        // With human approval metadata
        val approvedResult = AiAgentJobSecurityBoundary.evaluateJobSubmission(
            principal = aiPrincipal,
            jobType = "finance.execute_bulk_payout",
            metadata = mapOf(
                "requiresConfirmation" to "true",
                "confirmationId" to "CONF-999",
                "approvedByHumanId" to "manager-alice"
            ),
            tenantContext = tenant
        )
        assertEquals(AiAgentJobAuthResult.Authorized, approvedResult)
    }

    @Test
    fun testN8nJobTriggerAdapterValidatesHmacAndEnqueuesJob() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val payload = "{\"workflowId\":\"wf-print-1\",\"action\":\"sync\"}"
            val timestamp = System.currentTimeMillis().toString()
            val validSignature = computeHmac(payload, n8nConfig.signingSecret)

            val result = n8nAdapter.triggerJobFromN8n(
                payloadJson = payload,
                signatureHeader = validSignature,
                timestampHeader = timestamp,
                jobType = "n8n.sync_catalogs",
                idempotencyKey = "n8n-sync-1",
                tenantContext = tenant
            )

            assertTrue(result is N8nJobTriggerResult.Accepted)
            val jobId = (result as N8nJobTriggerResult.Accepted).jobId

            val job = jobRepo.getJobById(jobId, tenant)
            assertNotNull(job)
            assertEquals("n8n.sync_catalogs", job?.jobType)
            assertEquals(JobTriggerType.N8N, job?.triggerType)
        }
    }

    @Test
    fun testN8nJobTriggerAdapterRejectsTamperedSignature() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val payload = "{\"workflowId\":\"wf-print-1\",\"action\":\"sync\"}"
            val timestamp = System.currentTimeMillis().toString()

            val result = n8nAdapter.triggerJobFromN8n(
                payloadJson = payload,
                signatureHeader = "invalid-tampered-signature",
                timestampHeader = timestamp,
                jobType = "n8n.sync_catalogs",
                idempotencyKey = "n8n-sync-2",
                tenantContext = tenant
            )

            assertTrue(result is N8nJobTriggerResult.Rejected)
            assertTrue((result as N8nJobTriggerResult.Rejected).isSecurityViolation)
        }
    }
}
