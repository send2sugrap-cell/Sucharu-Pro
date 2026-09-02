package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.EventTraceContext
import com.sucharu.sucharupro.domain.event.model.events.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class EventSerializationTest {

    @Test
    fun test01_orderCreatedEvent_roundtrip() {
        val event = OrderCreatedEvent("ORD-101", "CUST-55", BigDecimal("45000.50"), 3, "BDT", 1L)
        val json = EventSerializationHelper.serializePayload(event)
        assertTrue(json.contains("\"orderId\":\"ORD-101\""))
        assertTrue(json.contains("\"totalAmount\":\"45000.50\""))

        val deserialized = EventSerializationHelper.deserializePayload(DomainEventType.ORDER_CREATED, json) as OrderCreatedEvent
        assertEquals("ORD-101", deserialized.orderId)
        assertEquals("CUST-55", deserialized.customerId)
        assertEquals(BigDecimal("45000.50"), deserialized.totalAmount)
        assertEquals(3, deserialized.itemCount)
        assertEquals("BDT", deserialized.currency)
    }

    @Test
    fun test02_allDomainEventTypes_serializeWithoutErrors() {
        val events = listOf(
            OrderCreatedEvent("ORD-1", "C-1", BigDecimal("100"), 1),
            OrderUpdatedEvent("ORD-1", "C-1", BigDecimal("200"), "Add item"),
            OrderCancelledEvent("ORD-1", "C-1", "Customer request", 1L),
            ProductionStartedEvent("JOB-1", "ORD-1", "PRESS-01", "OP-1", 1L),
            ProductionCompletedEvent("JOB-1", "ORD-1", 500, 10, 1L),
            QcPassedEvent("QC-1", "JOB-1", "INSP-1", 500, 100.0, 1L),
            QcFailedEvent("QC-2", "JOB-1", "INSP-1", "Color bleed", 50, 1L),
            StockReceivedEvent("MOV-1", "MAT-1", "SKU-1", BigDecimal("1000.00"), "WH-A", "PO-1", 1L),
            StockIssuedEvent("MOV-2", "MAT-1", "SKU-1", BigDecimal("250.00"), "WH-A", "PRODUCTION", "JOB-1", 1L),
            StockAdjustedEvent("ADJ-1", "MAT-1", "WH-A", BigDecimal("-50.00"), "Spoilage", "MGR-1", 1L),
            DeliveryCreatedEvent("CHAL-1", "ORD-1", "C-1", "123 Main St", 5, 1L),
            DeliveryDispatchedEvent("CHAL-1", "ORD-1", "Sundarban Courier", "TRK-999", 1L),
            DeliveryDeliveredEvent("CHAL-1", "ORD-1", "John Doe", 1700000000L, 1L),
            ReturnRequestedEvent("RET-1", "ORD-1", "C-1", "Damaged goods", 2, 1L),
            ReturnInspectedEvent("RET-1", "INSP-1", "DAMAGED", 0, 2, 1L),
            ReturnApprovedEvent("RET-1", "ORD-1", "MGR-1", true, 1L),
            ReturnRejectedEvent("RET-1", "ORD-1", "MGR-1", "Policy exceeded", 1L),
            InvoiceCreatedEvent("INV-1", "ORD-1", "C-1", "INV-2026-001", BigDecimal("45000.00"), "BDT", 1700000000L),
            PaymentReceivedEvent("PAY-1", "INV-1", "ORD-1", "C-1", BigDecimal("20000.00"), "BDT", "BKASH", "TRX-777"),
            PaymentRefundedEvent("REF-1", "PAY-1", "C-1", BigDecimal("5000.00"), "BDT", "Defect refund"),
            CustomerRegisteredEvent("CUST-1", "C-001", "Acme Corp", "01700000000", "cust@example.com"),
            CustomerVerifiedEvent("CUST-1", "EMAIL", 1700000000L, 1L),
            AffiliateReferralCreatedEvent("REF-1", "AFF-1", "CUST-1", "REF-CODE-1"),
            AffiliateCommissionGeneratedEvent("COM-1", "AFF-1", "ORD-1", BigDecimal("1000.00"), "BDT", 5.0),
            AuthenticationSucceededEvent("U-1", "admin_user", "192.168.1.1"),
            AuthenticationFailedEvent("u***@sucharu.com", "BAD_PASSWORD", "192.168.1.1"),
            SessionCreatedEvent("SESS-1", "U-1", 1700000000L, "Android"),
            SessionRevokedEvent("SESS-1", "U-1", "LOGOUT", "U-1"),
            AuthorizationDeniedEvent("U-1", "READ", "FINANCE", "PERMISSION_DENIED"),
            AccountLockedEvent("U-1", "BRUTE_FORCE", 1800000000L),
            PasswordChangedEvent("U-1", "USER_RESET", true),
            SystemMaintenanceScheduledEvent("MAINT-1", 1700000000L, 1700003600L, "DB upgrade"),
            SystemAlertEvent("ALT-1", "CRITICAL", "High memory alert", "INFRA")
        )

        for (evt in events) {
            val json = EventSerializationHelper.serializePayload(evt)
            assertNotNull(json)
            val deserialized = EventSerializationHelper.deserializePayload(evt.eventType, json)
            assertEquals(evt.eventType, deserialized.eventType)
            assertEquals(evt.aggregateId, deserialized.aggregateId)
        }
    }

    @Test
    fun test03_envelope_serializationAndDeserialization_roundtrip() {
        val payload = OrderCreatedEvent("ORD-999", "CUST-88", BigDecimal("12000.00"), 5, "BDT", 1L)
        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("USER-123"),
            traceContext = EventTraceContext(correlationId = "CORR-777", causationId = "PARENT-1", requestId = "REQ-HTTP-1"),
            metadata = mapOf("client_app" to "Android", "ip" to "10.0.0.1")
        )

        val json = EventSerializationHelper.serializeEnvelope(envelope)
        val deserialized = EventSerializationHelper.deserializeEnvelope(json)

        assertEquals(envelope.eventId, deserialized.eventId)
        assertEquals(envelope.eventType, deserialized.eventType)
        assertEquals(envelope.projectId, deserialized.projectId)
        assertEquals(envelope.aggregateType, deserialized.aggregateType)
        assertEquals(envelope.aggregateId, deserialized.aggregateId)
        assertEquals(envelope.aggregateVersion, deserialized.aggregateVersion)
        assertEquals(envelope.actor.actorId, deserialized.actor.actorId)
        assertEquals(envelope.correlationId, deserialized.correlationId)
        assertEquals(envelope.causationId, deserialized.causationId)
        assertEquals(envelope.requestId, deserialized.requestId)
        assertEquals(envelope.metadata["client_app"], deserialized.metadata["client_app"])

        val payloadTyped = deserialized.payload as OrderCreatedEvent
        assertEquals(payload.orderId, payloadTyped.orderId)
        assertEquals(payload.totalAmount, payloadTyped.totalAmount)
    }

    @Test
    fun test04_zeroSecretsInSerializedOutput() {
        val authEvent = AuthenticationFailedEvent(
            attemptedIdentifierMasked = "ad***@sucharu.com",
            failureReason = "INVALID_CREDENTIALS"
        )
        val json = EventSerializationHelper.serializePayload(authEvent)
        assertFalse(json.contains("password"))
        assertFalse(json.contains("secret"))
        assertFalse(json.contains("bearer"))
    }
}
