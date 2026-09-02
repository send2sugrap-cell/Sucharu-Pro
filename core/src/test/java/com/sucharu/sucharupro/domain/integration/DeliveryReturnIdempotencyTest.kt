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

class DeliveryReturnIdempotencyTest {

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

            val doOrder = DeliveryOrder("DO-IDEM", "PRJ-01", "DON-IDEM", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-IDEM", "DO-IDEM", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))

            val r = DeliveryReturn("RET-IDEM", "PRJ-01", "RN-IDEM", "DO-IDEM", status = DeliveryReturnStatus.INSPECTED, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
            val l = DeliveryReturnLine(
                returnLineId = "RL-IDEM",
                returnId = "RET-IDEM",
                projectId = "PRJ-01",
                deliveryOrderLineId = "DOL-IDEM",
                productId = "PROD-01",
                returnedQuantity = 20.0,
                receivedQuantity = 20.0,
                acceptedQuantity = 20.0,
                condition = DeliveryReturnLineCondition.GOOD,
                disposition = DeliveryReturnDisposition.RESTOCK,
                createdAt = 1000L,
                updatedAt = 1000L
            )
            returnDataSource.insertReturn(r, listOf(l))
        }
    }

    @Test
    fun `multiple identical restock operations do not duplicate Stock-In records`() = runBlocking {
        val firstRes = repository.processRestock("RET-IDEM", "RL-IDEM", "WH-01", "LOC-01", "wh", UserRole.WAREHOUSE)
        assertTrue(firstRes is DomainResult.Success)

        val secondRes = repository.processRestock("RET-IDEM", "RL-IDEM", "WH-01", "LOC-01", "wh", UserRole.WAREHOUSE)
        assertTrue(secondRes is DomainResult.Success)

        val stockIns = receivingDataSource.observeStockInRecords().first()
        assertEquals(1, stockIns.size)
    }
}
