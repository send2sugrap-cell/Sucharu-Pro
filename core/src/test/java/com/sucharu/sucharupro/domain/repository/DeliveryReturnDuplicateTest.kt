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
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReturnDuplicateTest {

    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReturnRepository

    @Before
    fun setUp() {
        runBlocking {
            returnDataSource = FakeDeliveryReturnDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryReturnRepositoryImpl(returnDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-DUP", "PRJ-01", "DON-DUP", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-DUP", "DO-DUP", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `creating return with duplicate return number in same project is rejected`() = runBlocking {
        val r1 = DeliveryReturn("RET-1", "PRJ-01", "RN-DUP", "DO-DUP", status = DeliveryReturnStatus.DRAFT, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        val l1 = DeliveryReturnLine(returnLineId = "RL-1", returnId = "RET-1", projectId = "PRJ-01", deliveryOrderLineId = "DOL-DUP", productId = "PROD-01", returnedQuantity = 10.0, createdAt = 1000L, updatedAt = 1000L)
        val res1 = repository.createReturn(r1, listOf(l1), "u1", UserRole.STAFF)
        assertTrue(res1 is DomainResult.Success)

        val r2 = DeliveryReturn("RET-2", "PRJ-01", "RN-DUP", "DO-DUP", status = DeliveryReturnStatus.DRAFT, requestedBy = "u2", createdAt = 2000L, updatedAt = 2000L)
        val l2 = DeliveryReturnLine(returnLineId = "RL-2", returnId = "RET-2", projectId = "PRJ-01", deliveryOrderLineId = "DOL-DUP", productId = "PROD-01", returnedQuantity = 10.0, createdAt = 2000L, updatedAt = 2000L)
        val res2 = repository.createReturn(r2, listOf(l2), "u2", UserRole.STAFF)
        assertTrue(res2 is DomainResult.Error)
        assertTrue((res2 as DomainResult.Error).message.contains("already exists"))
    }
}
