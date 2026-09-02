package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.integration.realtime.*
import com.sucharu.sucharupro.domain.event.boundary.RealTimeEventFrame
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationSucceededEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList

class RealTimeIntegrationTest {

    private class FakeTransportSender : RealTimeTransportSender {
        val sentFrames = CopyOnWriteArrayList<Pair<String, RealTimeEventFrame>>()

        override suspend fun sendFrame(session: RealTimeClientSession, frame: RealTimeEventFrame): Boolean {
            sentFrames.add(Pair(session.sessionId, frame))
            return true
        }
    }

    private lateinit var registry: RealTimeSubscriptionRegistry
    private lateinit var transportSender: FakeTransportSender
    private lateinit var deliveryService: RealTimeDeliveryService

    @Before
    fun setUp() {
        registry = RealTimeSubscriptionRegistry()
        transportSender = FakeTransportSender()
        deliveryService = RealTimeDeliveryService(registry, transportSender)
    }

    @Test
    fun test01_realTimeSubscription_andBroadcasting() {
        runBlocking {
            val session = RealTimeClientSession(
                sessionId = "SESS-01",
                userId = "USER-100",
                projectId = "tenant_alpha"
            )
            registry.registerSession(session)

            val topic = "tenant.tenant_alpha.order.ORD-101"
            val subscribed = registry.subscribe("SESS-01", topic)
            assertTrue(subscribed)

            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-101", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "tenant_alpha",
                actor = EventActor.human("USER-100")
            )

            val consumer = RealTimeEventConsumer<OrderCreatedEvent>(
                supportedEventType = DomainEventType.ORDER_CREATED,
                deliveryService = deliveryService
            )

            val result = consumer.consume(envelope)
            assertTrue(result.isSuccess)

            assertEquals(1, transportSender.sentFrames.size)
            val (sessionId, frame) = transportSender.sentFrames[0]
            assertEquals("SESS-01", sessionId)
            assertEquals("tenant.tenant_alpha.order.ORD-101", frame.topic)
            assertEquals("OrderCreated", frame.eventType)
        }
    }

    @Test
    fun test02_crossTenantSubscription_isRejected() {
        val sessionAlpha = RealTimeClientSession(
            sessionId = "SESS-ALPHA",
            userId = "USER-ALPHA",
            projectId = "tenant_alpha"
        )
        registry.registerSession(sessionAlpha)

        // Attempt to subscribe to Tenant Beta topic
        val subscribed = registry.subscribe("SESS-ALPHA", "tenant.tenant_beta.order.ORD-999")
        assertFalse(subscribed)
    }

    @Test
    fun test03_securityEvent_isBlockedFromRealTimeStreaming() {
        runBlocking {
            val session = RealTimeClientSession(
                sessionId = "SESS-01",
                userId = "USER-100",
                projectId = "tenant_alpha"
            )
            registry.registerSession(session)
            registry.subscribe("SESS-01", "tenant.tenant_alpha.auth.U-100")

            val securityEnvelope = EventEnvelope.create(
                payload = AuthenticationSucceededEvent("U-100", "admin", "192.168.1.1", "Chrome"),
                projectId = "tenant_alpha",
                actor = EventActor.human("U-100")
            )

            val consumer = RealTimeEventConsumer<AuthenticationSucceededEvent>(
                supportedEventType = DomainEventType.AUTH_SUCCEEDED,
                deliveryService = deliveryService
            )

            val result = consumer.consume(securityEnvelope)
            assertTrue(result.isSkipped)
            assertEquals(0, transportSender.sentFrames.size)
        }
    }

    @Test
    fun test04_clientDisconnect_unregistersAndPreventsDanglingFrames() {
        runBlocking {
            val session = RealTimeClientSession(
                sessionId = "SESS-01",
                userId = "USER-100",
                projectId = "tenant_alpha"
            )
            registry.registerSession(session)
            registry.subscribe("SESS-01", "tenant.tenant_alpha.order.ORD-101")

            // Client disconnects
            registry.unregisterSession("SESS-01")

            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-101", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "tenant_alpha",
                actor = EventActor.human("USER-100")
            )

            val consumer = RealTimeEventConsumer<OrderCreatedEvent>(
                supportedEventType = DomainEventType.ORDER_CREATED,
                deliveryService = deliveryService
            )

            val result = consumer.consume(envelope)
            assertTrue(result.isSuccess)
            assertEquals(0, transportSender.sentFrames.size) // Zero frames dispatched after disconnect
        }
    }
}
