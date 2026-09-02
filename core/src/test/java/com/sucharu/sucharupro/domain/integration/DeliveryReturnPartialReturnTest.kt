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

class DeliveryReturnPartialReturnTest {

    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReturnRepository

    @Before
    fun setUp() {
        runBlocking {
            returnDataSource = FakeDeliveryReturnDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryReturnRepositoryImpl(returnDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-PARTIAL", "PRJ-01", "DON-P", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-P", "DO-PARTIAL", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `partial return of 30 units from 100 leaves 70 eligible return units`() = runBlocking {
        val r = DeliveryReturn("RET-P", "PRJ-01", "RN-P", "DO-PARTIAL", status = DeliveryReturnStatus.DRAFT, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        val l = DeliveryReturnLine(
            returnLineId = "RL-P",
            returnId = "RET-P",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-P",
            productId = "PROD-01",
            returnedQuantity = 30.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val createRes = repository.createReturn(r, listOf(l), "u1", UserRole.STAFF)
        assertTrue(createRes is DomainResult.Success)

        val remainingEligible = repository.getEligibleReturnQuantity("DO-PARTIAL", "DOL-P")
        assertTrue(remainingEligible is DomainResult.Success)
        assertEquals(70.0, (remainingEligible as DomainResult.Success).data, 0.001)
    }
}
