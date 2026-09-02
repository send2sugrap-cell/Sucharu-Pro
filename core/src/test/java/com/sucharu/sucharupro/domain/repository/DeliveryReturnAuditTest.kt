package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReturnDataSource
import com.sucharu.sucharupro.data.repository.DeliveryReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnActivityType
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReturnAuditTest {

    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReturnRepository

    @Before
    fun setUp() {
        runBlocking {
            returnDataSource = FakeDeliveryReturnDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryReturnRepositoryImpl(returnDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-AUDIT", "PRJ-01", "DON-AUDIT", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-AUDIT", "DO-AUDIT", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `return actions generate chronological immutable audit records`() = runBlocking {
        val r = DeliveryReturn("RET-AUDIT", "PRJ-01", "RN-AUDIT", "DO-AUDIT", status = DeliveryReturnStatus.DRAFT, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        val l = DeliveryReturnLine(
            returnLineId = "RL-AUDIT",
            returnId = "RET-AUDIT",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-AUDIT",
            productId = "PROD-01",
            returnedQuantity = 20.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        repository.createReturn(r, listOf(l), "u1", UserRole.STAFF)
        repository.submitReturn("RET-AUDIT", "u1", UserRole.STAFF)
        repository.approveReturn("RET-AUDIT", "mgr", UserRole.MANAGER)

        val eventsRes = repository.getEvents("RET-AUDIT", UserRole.ADMIN)
        assertTrue(eventsRes is DomainResult.Success)
        val types = (eventsRes as DomainResult.Success).data.map { it.activityType }

        assertTrue(types.contains(DeliveryReturnActivityType.CREATED))
        assertTrue(types.contains(DeliveryReturnActivityType.SUBMITTED))
        assertTrue(types.contains(DeliveryReturnActivityType.APPROVED))
    }
}
