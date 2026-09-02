package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.repository.DeliveryChallanRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanActivityType
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryChallanAuditTest {

    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanRepository: DeliveryChallanRepository

    @Before
    fun setUp() {
        runBlocking {
            challanDataSource = FakeDeliveryChallanDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            challanRepository = DeliveryChallanRepositoryImpl(challanDataSource, doDataSource)

            val doOrder = DeliveryOrder(
                deliveryOrderId = "DO-AUDIT-1",
                projectId = "PRJ-01",
                deliveryOrderNo = "DEL-AUDIT",
                customerId = null,
                sourceReferenceId = null,
                sourceReferenceType = null,
                deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
                priority = DeliveryPriority.NORMAL,
                status = DeliveryOrderStatus.APPROVED,
                requestedDeliveryDate = 20000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            val line = DeliveryOrderLine(
                lineId = "DOLINE-AUDIT",
                deliveryOrderId = "DO-AUDIT-1",
                projectId = "PRJ-01",
                productId = "PROD-1",
                requestedQuantity = 50.0,
                notes = null
            )
            doDataSource.insertDeliveryOrder(doOrder, listOf(line))
        }
    }

    @Test
    fun `full lifecycle generates immutable audit events`() = runBlocking {
        val challan = DeliveryChallan(
            challanId = "CH-AUDIT",
            projectId = "PRJ-01",
            challanNo = "CH-AUDIT-01",
            deliveryOrderId = "DO-AUDIT-1",
            customerId = null,
            sourceReferenceId = null,
            sourceReferenceType = null,
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = 1000L,
            notes = null,
            createdBy = "user-creator",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryChallanLine(
            lineId = "LINE-AUDIT",
            challanId = "CH-AUDIT",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOLINE-AUDIT",
            productId = "PROD-1",
            quantity = 25.0
        )

        // 1. Create
        challanRepository.createChallan(challan, listOf(line), UserRole.ADMIN)

        // 2. Submit
        challanRepository.submitChallan("CH-AUDIT", "user-submitter", UserRole.ADMIN)

        // 3. Approve
        challanRepository.approveChallan("CH-AUDIT", "user-approver", UserRole.MANAGER)

        // 4. Ready for dispatch
        challanRepository.markReadyForDispatch("CH-AUDIT", "user-warehouse", UserRole.WAREHOUSE)

        // 5. Cancel
        challanRepository.cancelChallan("CH-AUDIT", "user-canceller", "Customer request", UserRole.MANAGER)

        val eventsResult = challanRepository.getActivityEvents("CH-AUDIT", UserRole.ADMIN)
        assertTrue(eventsResult is DomainResult.Success)
        val events = (eventsResult as DomainResult.Success).data

        assertEquals(5, events.size)
        // Descending order of performedAt:
        val types = events.map { it.activityType }
        assertTrue(types.contains(DeliveryChallanActivityType.CREATED))
        assertTrue(types.contains(DeliveryChallanActivityType.SUBMITTED))
        assertTrue(types.contains(DeliveryChallanActivityType.APPROVED))
        assertTrue(types.contains(DeliveryChallanActivityType.READY_FOR_DISPATCH))
        assertTrue(types.contains(DeliveryChallanActivityType.CANCELLED))
    }
}
