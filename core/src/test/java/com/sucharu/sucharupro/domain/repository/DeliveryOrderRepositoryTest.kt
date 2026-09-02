package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.repository.DeliveryOrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryOrderRepositoryTest {

    private lateinit var dataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryOrderRepository

    @Before
    fun setUp() {
        dataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryOrderRepositoryImpl(dataSource)
    }

    private fun sampleOrder(
        orderId: String = "DO-100",
        projectId: String = "PRJ-01",
        orderNo: String = "DEL-001",
        status: DeliveryOrderStatus = DeliveryOrderStatus.DRAFT
    ): DeliveryOrder {
        return DeliveryOrder(
            deliveryOrderId = orderId,
            projectId = projectId,
            deliveryOrderNo = orderNo,
            customerId = "CUST-01",
            sourceReferenceId = "SO-01",
            sourceReferenceType = "ORDER",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.HIGH,
            status = status,
            requestedDeliveryDate = 20000L,
            notes = "Handle carefully",
            createdBy = "admin-1",
            createdAt = 10000L,
            updatedAt = 10000L
        )
    }

    private fun sampleLine(
        lineId: String = "LINE-100",
        orderId: String = "DO-100",
        projectId: String = "PRJ-01",
        productId: String = "PROD-1",
        qty: Double = 5.0
    ): DeliveryOrderLine {
        return DeliveryOrderLine(
            lineId = lineId,
            deliveryOrderId = orderId,
            projectId = projectId,
            productId = productId,
            requestedQuantity = qty,
            notes = null
        )
    }

    @Test
    fun `createDeliveryOrder successfully persists order and lines`() = runBlocking {
        val order = sampleOrder()
        val lines = listOf(sampleLine())

        val result = repository.createDeliveryOrder(order, lines, UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)

        val fetched = repository.getDeliveryOrder(order.deliveryOrderId, UserRole.ADMIN)
        assertTrue(fetched is DomainResult.Success)
        assertEquals(order.deliveryOrderNo, (fetched as DomainResult.Success).data.deliveryOrderNo)

        val fetchedLines = repository.getDeliveryOrderLines(order.deliveryOrderId, UserRole.ADMIN)
        assertTrue(fetchedLines is DomainResult.Success)
        assertEquals(1, (fetchedLines as DomainResult.Success).data.size)
    }

    @Test
    fun `createDeliveryOrder rejects duplicate order number in same project`() = runBlocking {
        val order1 = sampleOrder(orderId = "DO-1", orderNo = "DEL-DUP")
        val order2 = sampleOrder(orderId = "DO-2", orderNo = "DEL-DUP")
        val lines1 = listOf(sampleLine(lineId = "L-1", orderId = "DO-1"))
        val lines2 = listOf(sampleLine(lineId = "L-2", orderId = "DO-2"))

        val res1 = repository.createDeliveryOrder(order1, lines1, UserRole.ADMIN)
        assertTrue(res1 is DomainResult.Success)

        val res2 = repository.createDeliveryOrder(order2, lines2, UserRole.ADMIN)
        assertTrue(res2 is DomainResult.Error)
        assertTrue((res2 as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun `submit and approve delivery order advances lifecycle`() = runBlocking {
        val order = sampleOrder()
        val lines = listOf(sampleLine())
        repository.createDeliveryOrder(order, lines, UserRole.ADMIN)

        // Submit
        val submitRes = repository.submitDeliveryOrder(order.deliveryOrderId, "admin-1", UserRole.ADMIN)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(DeliveryOrderStatus.PENDING, (submitRes as DomainResult.Success).data.status)

        // Approve
        val approveRes = repository.approveDeliveryOrder(order.deliveryOrderId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(DeliveryOrderStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        // Mark Ready For Dispatch
        val readyRes = repository.markReadyForDispatch(order.deliveryOrderId, "warehouse-1", UserRole.WAREHOUSE)
        assertTrue(readyRes is DomainResult.Success)
        assertEquals(DeliveryOrderStatus.READY_FOR_DISPATCH, (readyRes as DomainResult.Success).data.status)
    }

    @Test
    fun `updateDraftDeliveryOrder modifies draft and rejects editing non-draft orders`() = runBlocking {
        val order = sampleOrder()
        val lines = listOf(sampleLine())
        repository.createDeliveryOrder(order, lines, UserRole.ADMIN)

        val updatedLines = listOf(sampleLine(qty = 20.0))
        val updateRes = repository.updateDraftDeliveryOrder(
            deliveryOrderId = order.deliveryOrderId,
            deliveryType = DeliveryOrderType.INTERNAL_TRANSFER,
            priority = DeliveryPriority.URGENT,
            requestedDeliveryDate = 25000L,
            notes = "Updated notes",
            lines = updatedLines,
            actorId = "admin-1",
            callerRole = UserRole.ADMIN
        )
        assertTrue(updateRes is DomainResult.Success)
        assertEquals(DeliveryOrderType.INTERNAL_TRANSFER, (updateRes as DomainResult.Success).data.deliveryType)

        // Now submit order to PENDING
        repository.submitDeliveryOrder(order.deliveryOrderId, "admin-1", UserRole.ADMIN)

        // Attempting to update non-draft must fail
        val invalidUpdate = repository.updateDraftDeliveryOrder(
            deliveryOrderId = order.deliveryOrderId,
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.LOW,
            requestedDeliveryDate = 30000L,
            notes = null,
            lines = updatedLines,
            actorId = "admin-1",
            callerRole = UserRole.ADMIN
        )
        assertTrue(invalidUpdate is DomainResult.Error)
        assertTrue((invalidUpdate as DomainResult.Error).message.contains("Only DRAFT delivery orders"))
    }

    @Test
    fun `cancelDeliveryOrder transitions order to CANCELLED`() = runBlocking {
        val order = sampleOrder()
        val lines = listOf(sampleLine())
        repository.createDeliveryOrder(order, lines, UserRole.ADMIN)

        val cancelRes = repository.cancelDeliveryOrder(
            deliveryOrderId = order.deliveryOrderId,
            actorId = "admin-1",
            reason = "Customer cancelled",
            callerRole = UserRole.ADMIN
        )
        assertTrue(cancelRes is DomainResult.Success)
        assertEquals(DeliveryOrderStatus.CANCELLED, (cancelRes as DomainResult.Success).data.status)
    }
}
