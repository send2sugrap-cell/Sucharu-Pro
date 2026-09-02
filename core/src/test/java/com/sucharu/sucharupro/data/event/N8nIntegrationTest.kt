package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.integration.n8n.*
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationFailedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class N8nIntegrationTest {

    private class FakeN8nTransport(
        var simulatedResponse: N8nHttpResponse = N8nHttpResponse(statusCode = 200, responseBody = "{\"ok\":true}")
    ) : N8nWebhookTransport {
        var lastPostedUrl: String? = null
        var lastHeaders: Map<String, String>? = null
        var lastBody: String? = null

        override suspend fun postWebhook(
            url: String,
            headers: Map<String, String>,
            bodyJson: String
        ): N8nHttpResponse {
            lastPostedUrl = url
            lastHeaders = headers
            lastBody = bodyJson
            return simulatedResponse
        }
    }

    private lateinit var fakeTransport: FakeN8nTransport
    private lateinit var config: N8nConfig
    private lateinit var dispatcher: N8nAutomationDispatcher

    @Before
    fun setUp() {
        config = N8nConfig(signingSecret = "test_n8n_secret")
        fakeTransport = FakeN8nTransport()
        dispatcher = N8nAutomationDispatcher(config, fakeTransport)
    }

    @Test
    fun test01_payloadSigning_computesValidHmacSha256() {
        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
            projectId = "sucharu_main",
            actor = EventActor.human("U-1")
        )

        val (payload, rawJson) = N8nPayloadBuilder.buildPayload(envelope, "test_secret")
        assertNotNull(payload.webhookSignature)
        assertEquals(64, payload.webhookSignature?.length) // 256-bit hex string

        // Verify HMAC matches independent calculation
        val expectedSig = N8nPayloadBuilder.computeHmacSha256(rawJson, "test_secret")
        assertEquals(expectedSig, payload.webhookSignature)
    }

    @Test
    fun test02_successfulDispatch_includesTraceHeadersAndSignature() {
        runBlocking {
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            val consumer = N8nEventConsumer<OrderCreatedEvent>(
                supportedEventType = DomainEventType.ORDER_CREATED,
                dispatcher = dispatcher
            )

            val result = consumer.consume(envelope)
            assertTrue(result.isSuccess)
            assertEquals("https://automation.sucharu.internal/webhook/order_created", fakeTransport.lastPostedUrl)
            assertNotNull(fakeTransport.lastHeaders?.get("X-Sucharu-Signature"))
            assertEquals(envelope.eventId, fakeTransport.lastHeaders?.get("X-Sucharu-Event-Id"))
        }
    }

    @Test
    fun test03_securityEvent_isBlockedFromN8n() {
        runBlocking {
            val securityEnvelope = EventEnvelope.create(
                payload = AuthenticationFailedEvent("***", "INVALID_PASSWORD", "10.0.0.1"),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            val consumer = N8nEventConsumer<AuthenticationFailedEvent>(
                supportedEventType = DomainEventType.AUTH_FAILED,
                dispatcher = dispatcher
            )

            val result = consumer.consume(securityEnvelope)
            assertTrue(result.isFailure)
            val failure = result as EventConsumerResult.Failure
            assertEquals(EventFailureClassification.SECURITY, failure.classification)
            assertNull(fakeTransport.lastPostedUrl) // Nothing sent
        }
    }

    @Test
    fun test04_httpErrorClassification_maps5xxToTransientAnd4xxToValidation() {
        runBlocking {
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            // Test 503 Server Error -> TRANSIENT (retryable)
            fakeTransport.simulatedResponse = N8nHttpResponse(statusCode = 503, errorMessage = "Service Unavailable")
            val res503 = dispatcher.dispatch(envelope) as EventConsumerResult.Failure
            assertEquals(EventFailureClassification.TRANSIENT, res503.classification)
            assertTrue(res503.isRetryable)

            // Test 400 Bad Request -> VALIDATION (non-retryable)
            fakeTransport.simulatedResponse = N8nHttpResponse(statusCode = 400, errorMessage = "Invalid schema")
            val res400 = dispatcher.dispatch(envelope) as EventConsumerResult.Failure
            assertEquals(EventFailureClassification.VALIDATION, res400.classification)
            assertFalse(res400.isRetryable)
        }
    }
}
