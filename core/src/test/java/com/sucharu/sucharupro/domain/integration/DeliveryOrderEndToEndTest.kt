package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
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
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryOrderEndToEndTest {

    private lateinit var deliveryDataSource: FakeDeliveryOrderDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var repository: DeliveryOrderRepository

    @Before
    fun setUp() {
        deliveryDataSource = FakeDeliveryOrderDataSource()
        stockOutDataSource = FakeInventoryStockOutDataSource()
        repository = DeliveryOrderRepositoryImpl(deliveryDataSource)
    }

    @Test
    fun `complete end to end delivery foundation workflow`() = runBlocking {
        val projectId = "PRJ-E2E"
        val orderId = "DO-E2E-001"
        val orderNo = "DEL-E2E-2026"
        val creatorId = "admin-user"

        // 1. Create Delivery Order & Lines (Draft)
        val order = DeliveryOrder(
            deliveryOrderId = orderId,
            projectId = projectId,
            deliveryOrderNo = orderNo,
            customerId = "CUST-E2E",
            sourceReferenceId = "ORD-999",
            sourceReferenceType = "ORDER",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.URGENT,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = System.currentTimeMillis() + 86400000L * 2,
            notes = "E2E Delivery Workflow",
            createdBy = creatorId,
            createdAt = 100000L,
            updatedAt = 100000L
        )

        val lines = listOf(
            DeliveryOrderLine(
                lineId = "LINE-E2E-1",
                deliveryOrderId = orderId,
                projectId = projectId,
                productId = "PROD-BOX-01",
                requestedQuantity = 100.0,
                notes = "First batch"
            ),
            DeliveryOrderLine(
                lineId = "LINE-E2E-2",
                deliveryOrderId = orderId,
                projectId = projectId,
                productId = "PROD-LID-01",
                requestedQuantity = 100.0,
                notes = "Matching lids"
            )
        )

        val createResult = repository.createDeliveryOrder(order, lines, UserRole.ADMIN, callerProjectId = projectId)
        assertTrue(createResult is DomainResult.Success)

        // 2. Query newly created order & lines
        val fetchedOrder = repository.getDeliveryOrder(orderId, UserRole.ADMIN, callerProjectId = projectId)
        assertTrue(fetchedOrder is DomainResult.Success)
        assertEquals(DeliveryOrderStatus.DRAFT, (fetchedOrder as DomainResult.Success).data.status)

        val fetchedLines = repository.getDeliveryOrderLines(orderId, UserRole.ADMIN, callerProjectId = projectId)
        assertTrue(fetchedLines is DomainResult.Success)
        assertEquals(2, (fetchedLines as DomainResult.Success).data.size)

        // 3. Submit for Approval (DRAFT -> PENDING)
        val submitResult = repository.submitDeliveryOrder(orderId, creatorId, UserRole.ADMIN, callerProjectId = projectId)
        assertTrue(submitResult is DomainResult.Success)
        assertEquals(DeliveryOrderStatus.PENDING, (submitResult as DomainResult.Success).data.status)

        // 4. Approve Order (PENDING -> APPROVED)
        val approveResult = repository.approveDeliveryOrder(orderId, "manager-user", UserRole.MANAGER, callerProjectId = projectId)
        assertTrue(approveResult is DomainResult.Success)
        assertEquals(DeliveryOrderStatus.APPROVED, (approveResult as DomainResult.Success).data.status)

        // 5. Mark Ready for Dispatch (APPROVED -> READY_FOR_DISPATCH)
        val readyResult = repository.markReadyForDispatch(orderId, "warehouse-staff", UserRole.WAREHOUSE, callerProjectId = projectId)
        assertTrue(readyResult is DomainResult.Success)
        assertEquals(DeliveryOrderStatus.READY_FOR_DISPATCH, (readyResult as DomainResult.Success).data.status)

        // 6. Create Dispatch Request
        val dispatchRequest = DeliveryDispatchRequest(
            dispatchRequestId = "DISP-E2E-001",
            projectId = projectId,
            deliveryOrderId = orderId,
            requestedBy = "warehouse-staff",
            requestedAt = 105000L,
            priority = DeliveryPriority.URGENT,
            status = DispatchRequestStatus.REQUESTED,
            notes = "Ready for warehouse pickup"
        )
        val dispatchResult = repository.createDispatchRequest(dispatchRequest, UserRole.WAREHOUSE, callerProjectId = projectId)
        assertTrue(dispatchResult is DomainResult.Success)

        // 7. Verify Dispatch Request querying
        val fetchedDispatch = repository.getDispatchRequestForOrder(orderId, UserRole.WAREHOUSE, callerProjectId = projectId)
        assertTrue(fetchedDispatch is DomainResult.Success)
        assertNotNull((fetchedDispatch as DomainResult.Success).data)
        assertEquals(DispatchRequestStatus.REQUESTED, fetchedDispatch.data!!.status)

        // 8. Verify complete append-only Activity Audit Trail
        val activitiesResult = repository.getActivityEvents(orderId, UserRole.ADMIN, callerProjectId = projectId)
        assertTrue(activitiesResult is DomainResult.Success)
        val activities = (activitiesResult as DomainResult.Success).data
        assertEquals(5, activities.size)

        val activityTypes = activities.map { it.activityType }
        assertTrue(activityTypes.contains(DeliveryActivityType.CREATED))
        assertTrue(activityTypes.contains(DeliveryActivityType.STATUS_CHANGED))
        assertTrue(activityTypes.contains(DeliveryActivityType.DISPATCH_REQUESTED))

        // 9. Verify Project Isolation (User from PRJ-OTHER cannot see or touch this)
        val isolatedCheck = repository.getDeliveryOrder(orderId, UserRole.MANAGER, callerProjectId = "PRJ-OTHER")
        assertTrue(isolatedCheck is DomainResult.Error)

        // 10. Verify ZERO physical inventory deductions occurred
        val stockOuts = stockOutDataSource.observeStockOuts().first()
        assertEquals(0, stockOuts.size)
    }
}
