package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.repository.DeliveryOrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryActivityType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.DispatchRequestStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryOrderAuditTest {

    private lateinit var dataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryOrderRepository

    @Before
    fun setUp() {
        dataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryOrderRepositoryImpl(dataSource)
    }

    @Test
    fun `lifecycle actions produce append-only audit events`() = runBlocking {
        val order = DeliveryOrder(
            deliveryOrderId = "DO-AUDIT-1",
            projectId = "PRJ-01",
            deliveryOrderNo = "DEL-AUDIT-01",
            customerId = "CUST-1",
            sourceReferenceId = null,
            sourceReferenceType = null,
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.HIGH,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = 20000L,
            notes = null,
            createdBy = "creator-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryOrderLine(
            lineId = "LINE-AUDIT-1",
            deliveryOrderId = "DO-AUDIT-1",
            projectId = "PRJ-01",
            productId = "PROD-1",
            requestedQuantity = 10.0,
            notes = null
        )

        // 1. Create -> CREATED event
        repository.createDeliveryOrder(order, listOf(line), UserRole.ADMIN)

        // 2. Submit -> STATUS_CHANGED
        repository.submitDeliveryOrder(order.deliveryOrderId, "creator-1", UserRole.ADMIN)

        // 3. Approve -> STATUS_CHANGED
        repository.approveDeliveryOrder(order.deliveryOrderId, "mgr-1", UserRole.MANAGER)

        // 4. Dispatch Request -> DISPATCH_REQUESTED
        val dispatchReq = DeliveryDispatchRequest(
            dispatchRequestId = "DISP-1",
            projectId = "PRJ-01",
            deliveryOrderId = order.deliveryOrderId,
            requestedBy = "wh-1",
            requestedAt = 3000L,
            priority = DeliveryPriority.HIGH,
            status = DispatchRequestStatus.REQUESTED,
            notes = null
        )
        repository.createDispatchRequest(dispatchReq, UserRole.WAREHOUSE)

        val eventsResult = repository.getActivityEvents(order.deliveryOrderId, UserRole.ADMIN)
        assertTrue(eventsResult is DomainResult.Success)
        val events = (eventsResult as DomainResult.Success).data

        assertEquals(4, events.size)
        val eventTypes = events.map { it.activityType }
        assertTrue(eventTypes.contains(DeliveryActivityType.CREATED))
        assertTrue(eventTypes.contains(DeliveryActivityType.STATUS_CHANGED))
        assertTrue(eventTypes.contains(DeliveryActivityType.DISPATCH_REQUESTED))
    }
}
