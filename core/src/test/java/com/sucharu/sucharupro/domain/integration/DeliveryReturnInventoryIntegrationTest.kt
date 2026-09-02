package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReturnDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.repository.DeliveryReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLineCondition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReturnInventoryIntegrationTest {

    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var repository: DeliveryReturnRepository

    @Before
    fun setUp() {
        runBlocking {
            returnDataSource = FakeDeliveryReturnDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            repository = DeliveryReturnRepositoryImpl(returnDataSource, doDataSource, receivingDataSource)

            val doOrder = DeliveryOrder("DO-INT", "PRJ-01", "DON-INT", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-INT", "DO-INT", "PRJ-01", "PROD-INT", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))

            val r = DeliveryReturn("RET-INT", "PRJ-01", "RN-INT", "DO-INT", status = DeliveryReturnStatus.INSPECTED, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
            val l = DeliveryReturnLine(
                returnLineId = "RL-INT",
                returnId = "RET-INT",
                projectId = "PRJ-01",
                deliveryOrderLineId = "DOL-INT",
                productId = "PROD-INT",
                returnedQuantity = 50.0,
                receivedQuantity = 50.0,
                acceptedQuantity = 50.0,
                condition = DeliveryReturnLineCondition.GOOD,
                disposition = DeliveryReturnDisposition.RESTOCK,
                createdAt = 1000L,
                updatedAt = 1000L
            )
            returnDataSource.insertReturn(r, listOf(l))
        }
    }

    @Test
    fun `processAllRestock integrates with Module 07 Stock-In and sets restocked flag`() = runBlocking {
        val result = repository.processAllRestock("RET-INT", "WH-01", "LOC-01", "wh", UserRole.WAREHOUSE)
        assertTrue(result is DomainResult.Success)

        val lines = (repository.getReturnLines("RET-INT", UserRole.ADMIN) as DomainResult.Success).data
        assertTrue(lines[0].isRestocked)
        assertEquals(50.0, lines[0].restockedQuantity, 0.001)

        val stockIns = receivingDataSource.observeStockInRecords().first()
        assertEquals(1, stockIns.size)
        assertEquals(50, stockIns[0].quantity)
        assertEquals("DELIVERY_RETURN:RN-INT", stockIns[0].sourceReference)
    }
}
