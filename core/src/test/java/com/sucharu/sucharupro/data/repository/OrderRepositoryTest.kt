package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrderRepositoryTest {

    private lateinit var quotationDataSource: FakeQuotationDataSource
    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var orderRepository: OrderRepositoryImpl

    @Before
    fun setUp() {
        quotationDataSource = FakeQuotationDataSource()
        orderDataSource = FakeOrderDataSource()
        orderRepository = OrderRepositoryImpl(
            dataSource = orderDataSource,
            quotationDataSource = quotationDataSource
        )
    }

    @Test
    fun test01_createOrder_success() = runBlocking {
        val newOrder = Order(
            orderId = "ord-test-01",
            orderNumber = "ORD-999001",
            customerId = "cus-003",
            status = OrderStatusType.CONFIRMED,
            priority = OrderPriority.URGENT,
            items = listOf(
                OrderItem(
                    itemId = "item-01",
                    description = "Hospital Prescription Pads (100 Leaves x 50 Pads)",
                    quantity = 50,
                    unit = "Pads",
                    unitPrice = 120.toMoney()
                )
            ),
            createdAt = "2026-08-15T12:00:00Z",
            updatedAt = "2026-08-15T12:00:00Z"
        )

        val result = orderRepository.createOrder(newOrder)
        assertTrue(result.isSuccess)
        val created = (result as DomainResult.Success).data
        assertEquals("ord-test-01", created.orderId)
        assertEquals("ORD-999001", created.orderNumber)
        assertEquals("৳ 6,000", created.totalAmount.formatted())
    }

    @Test
    fun test02_duplicateOrderId_rejected() = runBlocking {
        val duplicate = Order(
            orderId = "ord-001", // already in FakeOrderDataSource
            orderNumber = "ORD-UNIQUE-123",
            customerId = "cus-001",
            items = listOf(OrderItem("it-1", "Test", quantity = 1, unitPrice = 10.toMoney())),
            createdAt = "2026-08-15T12:00:00Z",
            updatedAt = "2026-08-15T12:00:00Z"
        )

        val result = orderRepository.createOrder(duplicate)
        assertTrue(result.isError)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun test03_getOrder_success() = runBlocking {
        val order = orderRepository.getOrderById("ord-001").first()
        assertNotNull(order)
        assertEquals("ORD-000001", order?.orderNumber)
        assertEquals("cus-001", order?.customerId)
        assertEquals(OrderStatusType.CONFIRMED, order?.status)
    }

    @Test
    fun test04_getOrdersForCustomer_isolated() = runBlocking {
        val cus1Orders = orderRepository.getOrdersForCustomer("cus-001").first()
        val cus2Orders = orderRepository.getOrdersForCustomer("cus-002").first()

        assertTrue(cus1Orders.all { it.customerId == "cus-001" })
        assertTrue(cus2Orders.all { it.customerId == "cus-002" })
        assertFalse(cus1Orders.any { it.customerId == "cus-002" })
    }

    @Test
    fun test05_orderStatus_validTransitions() = runBlocking {
        // ord-001 is CONFIRMED -> transition to IN_PRODUCTION
        val transitionResult = orderRepository.updateOrderStatus("ord-001", OrderStatusType.IN_PRODUCTION)
        assertTrue(transitionResult.isSuccess)
        val updated = (transitionResult as DomainResult.Success).data
        assertEquals(OrderStatusType.IN_PRODUCTION, updated.status)

        // IN_PRODUCTION -> READY
        val readyResult = orderRepository.updateOrderStatus("ord-001", OrderStatusType.READY)
        assertTrue(readyResult.isSuccess)
        assertEquals(OrderStatusType.READY, (readyResult as DomainResult.Success).data.status)
    }

    @Test
    fun test06_orderStatus_invalidTransition_rejected() = runBlocking {
        // ord-002 is IN_PRODUCTION -> cannot jump directly to DELIVERED without READY
        val result = orderRepository.updateOrderStatus("ord-002", OrderStatusType.DELIVERED)
        assertTrue(result.isError)
        assertTrue((result as DomainResult.Error).message.contains("Invalid order status transition"))
    }

    @Test
    fun test07_cancelOrder_success() = runBlocking {
        val cancelResult = orderRepository.cancelOrder("ord-001", "Customer requested cancellation before printing.")
        assertTrue(cancelResult.isSuccess)
        val cancelled = (cancelResult as DomainResult.Success).data
        assertEquals(OrderStatusType.CANCELLED, cancelled.status)
        assertTrue(cancelled.notes?.contains("Customer requested cancellation") == true)
    }

    @Test
    fun test08_createOrderFromApprovedQuotation_success() = runBlocking {
        // qt-001 is APPROVED with approvedRevisionId = "rev-001-v2"
        val createResult = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-from-qt-01",
            orderNumber = "ORD-000099",
            quotationId = "qt-001",
            approvedRevisionId = "rev-001-v2",
            priority = OrderPriority.HIGH,
            confirmedBy = "Senior Desk Officer",
            timestamp = "2026-08-15T16:00:00Z"
        )

        assertTrue(createResult.isSuccess)
        val order = (createResult as DomainResult.Success).data
        assertEquals("ord-from-qt-01", order.orderId)
        assertEquals("ORD-000099", order.orderNumber)
        assertEquals("cus-001", order.customerId)
        assertEquals("qt-001", order.quotationId)
        assertEquals("rev-001-v2", order.approvedQuotationRevisionId)
        assertEquals(OrderStatusType.CONFIRMED, order.status)
        assertEquals(JobHandoffStatus.READY_FOR_JOB, order.jobHandoffStatus)
        assertEquals(OrderPriority.HIGH, order.priority)
        assertEquals(1, order.items.size)
        // Item: (1000 * 1.20) - 100 = 1100
        assertEquals("৳ 1,100", order.totalAmount.formatted())
    }

    @Test
    fun test09_createOrderFromApprovedQuotation_snapshotIsImmutableFromQuotationChanges() = runBlocking {
        // 1. Create order from approved quotation
        val createResult = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-snapshot-test",
            orderNumber = "ORD-SNAPSHOT-01",
            quotationId = "qt-001",
            approvedRevisionId = "rev-001-v2",
            timestamp = "2026-08-15T16:00:00Z"
        )
        assertTrue(createResult.isSuccess)
        val order = (createResult as DomainResult.Success).data
        assertEquals("৳ 1,100", order.totalAmount.formatted())

        // 2. Add a new revision V3 to the quotation with different pricing (e.g. ৳ 5,000)
        val rev3 = QuotationRevision(
            revisionId = "rev-001-v3",
            quotationId = "qt-001",
            revisionNumber = 3,
            items = listOf(
                QuotationItem(
                    itemId = "qt-item-01",
                    description = "Different Specification",
                    quantity = 5000,
                    unitPrice = 1.0.toMoney()
                )
            ),
            createdAt = "2026-08-15T18:00:00Z"
        )
        quotationDataSource.insertQuotationRevision("qt-001", rev3)

        // 3. Verify Order snapshot remains exactly ৳ 1,100 and unchanged
        val storedOrder = (orderRepository.findOrderById("ord-snapshot-test") as DomainResult.Success).data
        assertEquals("৳ 1,100", storedOrder.totalAmount.formatted())
        assertEquals(1000, storedOrder.items.first().quantity)
        assertEquals("rev-001-v2", storedOrder.approvedQuotationRevisionId)
    }

    @Test
    fun test10_createOrderFromUnapprovedQuotation_rejected() = runBlocking {
        // Attempt to create order from non-approved revision
        val result = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-invalid",
            orderNumber = "ORD-INV-01",
            quotationId = "qt-001",
            approvedRevisionId = "rev-001-v1", // rev-001-v1 is NOT the approved revision (v2 is)
            timestamp = "2026-08-15T16:00:00Z"
        )

        assertTrue(result.isError)
        assertTrue((result as DomainResult.Error).message.contains("is not the currently approved revision"))
    }
}
