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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReturnMultipleReturnTest {

    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReturnRepository

    @Before
    fun setUp() {
        runBlocking {
            returnDataSource = FakeDeliveryReturnDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryReturnRepositoryImpl(returnDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-MULTI", "PRJ-01", "DON-M", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-M", "DO-MULTI", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `multiple legitimate partial returns are accepted but total return cannot exceed delivered quantity`() = runBlocking {
        // Return 1: 40 pcs -> Success
        val r1 = DeliveryReturn("RET-M1", "PRJ-01", "RN-M1", "DO-MULTI", status = DeliveryReturnStatus.DRAFT, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        val l1 = DeliveryReturnLine(returnLineId = "RL-M1", returnId = "RET-M1", projectId = "PRJ-01", deliveryOrderLineId = "DOL-M", productId = "PROD-01", returnedQuantity = 40.0, createdAt = 1000L, updatedAt = 1000L)
        val res1 = repository.createReturn(r1, listOf(l1), "u1", UserRole.STAFF)
        assertTrue(res1 is DomainResult.Success)

        // Return 2: 50 pcs -> Success (Total 40 + 50 = 90 <= 100)
        val r2 = DeliveryReturn("RET-M2", "PRJ-01", "RN-M2", "DO-MULTI", status = DeliveryReturnStatus.DRAFT, requestedBy = "u2", createdAt = 2000L, updatedAt = 2000L)
        val l2 = DeliveryReturnLine(returnLineId = "RL-M2", returnId = "RET-M2", projectId = "PRJ-01", deliveryOrderLineId = "DOL-M", productId = "PROD-01", returnedQuantity = 50.0, createdAt = 2000L, updatedAt = 2000L)
        val res2 = repository.createReturn(r2, listOf(l2), "u2", UserRole.STAFF)
        assertTrue(res2 is DomainResult.Success)

        // Return 3: 20 pcs (Total 90 + 20 = 110 > 100) -> Must be rejected!
        val r3 = DeliveryReturn("RET-M3", "PRJ-01", "RN-M3", "DO-MULTI", status = DeliveryReturnStatus.DRAFT, requestedBy = "u3", createdAt = 3000L, updatedAt = 3000L)
        val l3 = DeliveryReturnLine(returnLineId = "RL-M3", returnId = "RET-M3", projectId = "PRJ-01", deliveryOrderLineId = "DOL-M", productId = "PROD-01", returnedQuantity = 20.0, createdAt = 3000L, updatedAt = 3000L)
        val res3 = repository.createReturn(r3, listOf(l3), "u3", UserRole.STAFF)
        assertTrue(res3 is DomainResult.Error)
        assertTrue((res3 as DomainResult.Error).message.contains("exceeds max eligible returnable quantity"))

        // Remaining returnable quantity is exactly 10.0
        val remaining = repository.getEligibleReturnQuantity("DO-MULTI", "DOL-M")
        assertEquals(10.0, (remaining as DomainResult.Success).data, 0.001)
    }
}
