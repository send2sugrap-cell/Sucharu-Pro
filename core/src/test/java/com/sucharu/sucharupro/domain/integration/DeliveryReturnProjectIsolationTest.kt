package com.sucharu.sucharupro.domain.integration

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
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReturnProjectIsolationTest {

    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReturnRepository

    @Before
    fun setUp() {
        runBlocking {
            returnDataSource = FakeDeliveryReturnDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryReturnRepositoryImpl(returnDataSource, doDataSource)

            val doA = DeliveryOrder("DO-A", "PRJ-A", "DON-A", "CUST-A", "SO-A", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val dlA = DeliveryOrderLine("DLA", "DO-A", "PRJ-A", "PROD-A", 100.0, null)
            doDataSource.insertDeliveryOrder(doA, listOf(dlA))

            val doB = DeliveryOrder("DO-B", "PRJ-B", "DON-B", "CUST-B", "SO-B", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-2", 1000L, 1000L)
            val dlB = DeliveryOrderLine("DLB", "DO-B", "PRJ-B", "PROD-B", 200.0, null)
            doDataSource.insertDeliveryOrder(doB, listOf(dlB))
        }
    }

    @Test
    fun `observeReturns strictly filters by project boundary`() = runBlocking {
        val rA = DeliveryReturn("RET-A", "PRJ-A", "RN-A", "DO-A", status = DeliveryReturnStatus.DRAFT, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        val lA = DeliveryReturnLine(returnLineId = "RL-A", returnId = "RET-A", projectId = "PRJ-A", deliveryOrderLineId = "DLA", productId = "PROD-A", returnedQuantity = 10.0, createdAt = 1000L, updatedAt = 1000L)
        repository.createReturn(rA, listOf(lA), "u1", UserRole.ADMIN, "PRJ-A")

        val rB = DeliveryReturn("RET-B", "PRJ-B", "RN-B", "DO-B", status = DeliveryReturnStatus.DRAFT, requestedBy = "u2", createdAt = 2000L, updatedAt = 2000L)
        val lB = DeliveryReturnLine(returnLineId = "RL-B", returnId = "RET-B", projectId = "PRJ-B", deliveryOrderLineId = "DLB", productId = "PROD-B", returnedQuantity = 20.0, createdAt = 2000L, updatedAt = 2000L)
        repository.createReturn(rB, listOf(lB), "u2", UserRole.ADMIN, "PRJ-B")

        val listA = repository.observeReturns("PRJ-A").first()
        val listB = repository.observeReturns("PRJ-B").first()

        assertEquals(1, listA.size)
        assertEquals("RET-A", listA[0].returnId)

        assertEquals(1, listB.size)
        assertEquals("RET-B", listB[0].returnId)
    }

    @Test
    fun `cross project getReturn is denied`() = runBlocking {
        val rA = DeliveryReturn("RET-A", "PRJ-A", "RN-A", "DO-A", status = DeliveryReturnStatus.DRAFT, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        val lA = DeliveryReturnLine(returnLineId = "RL-A", returnId = "RET-A", projectId = "PRJ-A", deliveryOrderLineId = "DLA", productId = "PROD-A", returnedQuantity = 10.0, createdAt = 1000L, updatedAt = 1000L)
        repository.createReturn(rA, listOf(lA), "u1", UserRole.ADMIN, "PRJ-A")

        val result = repository.getReturn("RET-A", UserRole.ADMIN, "PRJ-B")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Access denied"))
    }
}
