package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeOrderJobHandoffDataSource
import com.sucharu.sucharupro.data.repository.OrderJobHandoffRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for [OrderJobHandoffRepository] & [FakeOrderJobHandoffDataSource].
 */
class OrderJobHandoffRepositoryTest {

    private lateinit var dataSource: FakeOrderJobHandoffDataSource
    private lateinit var repository: OrderJobHandoffRepository

    private val sampleItem = OrderItem(
        itemId = "item-01",
        description = "বই প্রিন্টিং",
        quantity = 500,
        unit = "Pcs",
        unitPrice = 120.toMoney()
    )

    private val sampleOrder = Order(
        orderId = "ord-repo-01",
        orderNumber = "ORD-2026-R01",
        customerId = "cus-001",
        quotationId = "qt-001",
        approvedQuotationRevisionId = "rev-001",
        status = OrderStatusType.CONFIRMED,
        priority = OrderPriority.NORMAL,
        items = listOf(sampleItem),
        discount = Money.ZERO,
        paymentTerms = PaymentTerms.DEFAULT,
        deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
        jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeOrderJobHandoffDataSource()
        repository = OrderJobHandoffRepositoryImpl(dataSource)
    }

    @Test
    fun createHandoff_createsSealedSnapshotSuccessfully() = runBlocking {
        val result = repository.createHandoff(
            handoffId = "hnd-001",
            order = sampleOrder,
            createdBy = "Sales Executive",
            notes = "জরুরি ডেলিভারি নির্দেশনা",
            timestamp = "2026-08-16T10:30:00Z"
        )

        assertTrue(result is DomainResult.Success)
        val handoff = (result as DomainResult.Success).data
        assertEquals("hnd-001", handoff.handoffId)
        assertEquals("ord-repo-01", handoff.orderId)
        assertEquals("ORD-2026-R01", handoff.orderNumber)
        assertEquals(OrderJobHandoffStatus.READY_FOR_HANDOFF, handoff.handoffStatus)
        assertEquals(1, handoff.itemCount)
        assertEquals(500, handoff.totalQuantity)
        assertEquals("৳ 60,000", handoff.commercialTotal.formatted())
        assertEquals("Sales Executive", handoff.createdBy)
        assertEquals("জরুরি ডেলিভারি নির্দেশনা", handoff.notes)
    }

    @Test
    fun duplicateHandoffForSameOrder_isRejected() = runBlocking {
        repository.createHandoff(
            handoffId = "hnd-001",
            order = sampleOrder,
            timestamp = "2026-08-16T10:30:00Z"
        )

        val duplicateResult = repository.createHandoff(
            handoffId = "hnd-002",
            order = sampleOrder,
            timestamp = "2026-08-16T10:35:00Z"
        )

        assertTrue(duplicateResult is DomainResult.Error)
        val message = (duplicateResult as DomainResult.Error).message
        assertTrue(message.contains("already exists"))
    }

    @Test
    fun confirmHandoff_transitionsToHandedOff() = runBlocking {
        repository.createHandoff(
            handoffId = "hnd-001",
            order = sampleOrder,
            timestamp = "2026-08-16T10:30:00Z"
        )

        val confirmRes = repository.confirmHandoff(
            handoffId = "hnd-001",
            confirmedBy = "Production Manager",
            timestamp = "2026-08-16T11:00:00Z"
        )

        assertTrue(confirmRes is DomainResult.Success)
        val handoff = (confirmRes as DomainResult.Success).data
        assertEquals(OrderJobHandoffStatus.HANDED_OFF, handoff.handoffStatus)
        assertEquals("Production Manager", handoff.confirmedBy)
        assertEquals("2026-08-16T11:00:00Z", handoff.confirmedAt)
    }

    @Test
    fun markReadyForProduction_transitionsToReadyForProduction() = runBlocking {
        repository.createHandoff(
            handoffId = "hnd-001",
            order = sampleOrder,
            timestamp = "2026-08-16T10:30:00Z"
        )
        repository.confirmHandoff(
            handoffId = "hnd-001",
            confirmedBy = "Production Manager",
            timestamp = "2026-08-16T11:00:00Z"
        )

        val prodRes = repository.markReadyForProduction(
            handoffId = "hnd-001",
            timestamp = "2026-08-16T11:30:00Z"
        )

        assertTrue(prodRes is DomainResult.Success)
        val handoff = (prodRes as DomainResult.Success).data
        assertEquals(OrderJobHandoffStatus.READY_FOR_PRODUCTION, handoff.handoffStatus)
    }

    @Test
    fun cancelHandoff_recordsReasonAndCancels() = runBlocking {
        repository.createHandoff(
            handoffId = "hnd-001",
            order = sampleOrder,
            notes = "Initial instructions",
            timestamp = "2026-08-16T10:30:00Z"
        )

        val cancelRes = repository.cancelHandoff(
            handoffId = "hnd-001",
            reason = "Customer changed paper requirement"
        )

        assertTrue(cancelRes is DomainResult.Success)
        val handoff = (cancelRes as DomainResult.Success).data
        assertEquals(OrderJobHandoffStatus.CANCELLED, handoff.handoffStatus)
        assertTrue(handoff.notes?.contains("Customer changed paper requirement") == true)
        assertTrue(handoff.notes?.contains("Initial instructions") == true)
    }

    @Test
    fun reactiveObservables_emitUpdates() = runBlocking {
        repository.createHandoff(
            handoffId = "hnd-001",
            order = sampleOrder,
            timestamp = "2026-08-16T10:30:00Z"
        )

        val observed = repository.getHandoffForOrder("ord-repo-01").first()
        assertNotNull(observed)
        assertEquals("hnd-001", observed?.handoffId)
    }
}
