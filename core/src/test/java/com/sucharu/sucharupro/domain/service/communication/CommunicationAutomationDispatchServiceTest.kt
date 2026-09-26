package com.sucharu.sucharupro.domain.service.communication

import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationDeliveryStatus
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommunicationAutomationDispatchServiceTest {

    private lateinit var service: CommunicationAutomationDispatchService

    @Before
    fun setUp() {
        service = CommunicationAutomationDispatchService()
    }

    @Test
    fun `processAndDispatchEvent_preventsDuplicateMessagesViaIdempotencyDeduplication`() = runBlocking {
        val key = "IDEM-TEST-12345"

        // First dispatch call
        val firstDispatch = service.processAndDispatchEvent(
            eventId = "EVT-TEST-001",
            recipientId = "CUST-1001",
            title = "Order Confirmed",
            message = "Your order #ORD-1001 has been confirmed.",
            channel = NotificationChannel.IN_APP,
            idempotencyKey = key
        )

        assertNotNull(firstDispatch)
        assertEquals("CUST-1001", firstDispatch.recipientId)

        // Second dispatch call with the EXACT SAME idempotencyKey
        val duplicateDispatch = service.processAndDispatchEvent(
            eventId = "EVT-TEST-001",
            recipientId = "CUST-1001",
            title = "Order Confirmed",
            message = "Your order #ORD-1001 has been confirmed.",
            channel = NotificationChannel.IN_APP,
            idempotencyKey = key
        )

        // Verify IDEMPOTENCY DEDUPLICATION: Returns the exact same dispatch record
        assertEquals(firstDispatch.dispatchId, duplicateDispatch.dispatchId)

        // Verify Summary
        val summary = service.buildAutomationDispatchSummary()
        assertTrue("Outbox Decoupled Delivery MUST be enforced!", summary.isOutboxDecoupledDeliveryEnforced)
    }
}
