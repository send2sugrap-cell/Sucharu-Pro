package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationEventBoundary
import com.sucharu.sucharupro.domain.event.boundary.RealTimeEventBoundary
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class NotificationReadinessTest {

    @Test
    fun test01_notificationIntent_creation_resolvesCorrectChannels() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-999",
            customerId = "CUST-888",
            totalAmount = BigDecimal("1200"),
            itemCount = 1
        )
        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        val intent = NotificationEventBoundary.createNotificationIntent(
            envelope = envelope,
            targetRecipientId = "CUST-888",
            title = "Order Placed",
            body = "Your order ORD-999 has been successfully confirmed."
        )

        assertEquals("sucharu_main", intent.projectId)
        assertEquals("CUST-888", intent.targetRecipientId)
        assertEquals(DomainEventType.ORDER_CREATED, intent.eventType)
        assertTrue(intent.targetChannels.contains(NotificationChannel.IN_APP))
        assertTrue(intent.targetChannels.contains(NotificationChannel.EMAIL))
        assertTrue(intent.targetChannels.contains(NotificationChannel.SMS))
        assertTrue(intent.targetChannels.contains(NotificationChannel.PUSH))
    }

    @Test
    fun test02_realTimeEventFrame_generatesCanonicalTopicAndMetadata() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-999",
            customerId = "CUST-888",
            totalAmount = BigDecimal("1200"),
            itemCount = 1
        )
        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        val frame = RealTimeEventBoundary.toStreamFrame(envelope)
        assertEquals("tenant.sucharu_main.order.ORD-999", frame.topic)
        assertEquals("OrderCreated", frame.eventType)
        assertEquals("v1", frame.eventVersion)
        assertEquals(envelope.eventId, frame.eventId)
    }
}
