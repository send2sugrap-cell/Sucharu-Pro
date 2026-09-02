package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventCategory
import com.sucharu.sucharupro.domain.event.model.events.OrderCancelledEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderUpdatedEvent
import com.sucharu.sucharupro.domain.event.model.events.PaymentReceivedEvent
import com.sucharu.sucharupro.domain.event.model.events.ProductionCompletedEvent
import com.sucharu.sucharupro.domain.event.model.events.ProductionStartedEvent
import com.sucharu.sucharupro.domain.event.model.events.QcFailedEvent
import com.sucharu.sucharupro.domain.event.model.events.QcPassedEvent
import com.sucharu.sucharupro.domain.event.model.events.StockReceivedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DomainEventModelTest {

    @Test
    fun test01_orderCreatedEvent_initializesWithValidFields() {
        val event = OrderCreatedEvent(
            orderId = "ORD-1001",
            customerId = "CUST-001",
            totalAmount = BigDecimal("1500.50"),
            currency = "BDT",
            itemCount = 3,
            aggregateVersion = 1L
        )

        assertEquals("ORD-1001", event.aggregateId)
        assertEquals("ORDER", event.aggregateType)
        assertEquals(DomainEventType.ORDER_CREATED, event.eventType)
        assertEquals("v1", event.eventVersion)
        assertEquals(EventCategory.ORDER, event.eventType.category)
        assertEquals(1L, event.aggregateVersion)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test02_orderCreatedEvent_rejectsBlankOrderId() {
        OrderCreatedEvent(
            orderId = "  ",
            customerId = "CUST-001",
            totalAmount = BigDecimal("100"),
            itemCount = 1
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun test03_orderCreatedEvent_rejectsNegativeAmount() {
        OrderCreatedEvent(
            orderId = "ORD-1",
            customerId = "CUST-1",
            totalAmount = BigDecimal("-10"),
            itemCount = 1
        )
    }

    @Test
    fun test04_orderUpdatedAndCancelledEvents_holdAggregateContext() {
        val updated = OrderUpdatedEvent(
            orderId = "ORD-1001",
            customerId = "CUST-001",
            updatedTotalAmount = BigDecimal("2000.00"),
            updateReason = "Added extra lamination",
            aggregateVersion = 2L
        )
        val cancelled = OrderCancelledEvent(
            orderId = "ORD-1001",
            customerId = "CUST-001",
            cancellationReason = "Customer request",
            aggregateVersion = 3L
        )

        assertEquals(2L, updated.aggregateVersion)
        assertEquals(DomainEventType.ORDER_UPDATED, updated.eventType)
        assertEquals(3L, cancelled.aggregateVersion)
        assertEquals(DomainEventType.ORDER_CANCELLED, cancelled.eventType)
    }

    @Test
    fun test05_productionAndQcEvents_validateInvariants() {
        val prodStart = ProductionStartedEvent(
            jobId = "JOB-500",
            orderId = "ORD-1001",
            workstationId = "PRESS-01",
            operatorId = "STAFF-99"
        )
        val prodDone = ProductionCompletedEvent(
            jobId = "JOB-500",
            orderId = "ORD-1001",
            completedQuantity = 1000,
            wastageQuantity = 20,
            aggregateVersion = 2L
        )
        val qcPass = QcPassedEvent(
            inspectionId = "QC-101",
            jobId = "JOB-500",
            inspectedBy = "QC-INSP-1",
            sampleSize = 50,
            passRatePercentage = 98.5
        )
        val qcFail = QcFailedEvent(
            inspectionId = "QC-102",
            jobId = "JOB-500",
            inspectedBy = "QC-INSP-1",
            defectReason = "Color registration off by 2mm",
            rejectedQuantity = 500
        )

        assertEquals("PRODUCTION", prodStart.aggregateType)
        assertEquals(1000, prodDone.completedQuantity)
        assertEquals("QC", qcPass.aggregateType)
        assertEquals("QC", qcFail.aggregateType)
    }

    @Test
    fun test06_inventoryAndPaymentEvents_validatePrecision() {
        val stock = StockReceivedEvent(
            movementId = "MOV-1",
            productId = "PROD-100",
            sku = "PAPER-ART-300GSM",
            quantity = BigDecimal("5000"),
            warehouseId = "WH-MAIN"
        )
        val payment = PaymentReceivedEvent(
            paymentId = "PAY-1",
            invoiceId = "INV-1",
            orderId = "ORD-1",
            customerId = "CUST-1",
            amount = BigDecimal("1500.50"),
            paymentMethod = "BKASH",
            transactionRef = "TRX998877"
        )

        assertEquals("INVENTORY", stock.aggregateType)
        assertEquals("FINANCE", payment.aggregateType)
        assertEquals(BigDecimal("1500.50"), payment.amount)
    }
}
