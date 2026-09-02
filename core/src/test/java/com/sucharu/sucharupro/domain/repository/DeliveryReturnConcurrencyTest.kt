package com.sucharu.sucharupro.domain.repository

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReturnConcurrencyTest {

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

            val doOrder = DeliveryOrder("DO-CONCUR", "PRJ-01", "DON-CONCUR", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-CONCUR", "DO-CONCUR", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))

            val ret = DeliveryReturn("RET-CONCUR", "PRJ-01", "RN-CONCUR", "DO-CONCUR", status = DeliveryReturnStatus.INSPECTED, requestedBy = "user-1", createdAt = 1000L, updatedAt = 1000L)
            val line = DeliveryReturnLine(
                returnLineId = "RL-CONCUR",
                returnId = "RET-CONCUR",
                projectId = "PRJ-01",
                deliveryOrderLineId = "DOL-CONCUR",
                productId = "PROD-01",
                returnedQuantity = 50.0,
                receivedQuantity = 50.0,
                acceptedQuantity = 50.0,
                condition = DeliveryReturnLineCondition.GOOD,
                disposition = DeliveryReturnDisposition.RESTOCK,
                createdAt = 1000L,
                updatedAt = 1000L
            )
            returnDataSource.insertReturn(ret, listOf(line))
        }
    }

    @Test
    fun `concurrent processRestock requests execute idempotently and create exactly one stock-in record`() = runBlocking {
        val jobs = listOf(
            async(Dispatchers.IO) {
                repository.processRestock("RET-CONCUR", "RL-CONCUR", "WH-01", "LOC-01", "user-1", UserRole.WAREHOUSE)
            },
            async(Dispatchers.IO) {
                repository.processRestock("RET-CONCUR", "RL-CONCUR", "WH-01", "LOC-01", "user-2", UserRole.WAREHOUSE)
            }
        )

        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        // Check canonical Stock-In records: must be exactly 1
        val stockInRecords = receivingDataSource.observeStockInRecords().first()
        assertEquals(1, stockInRecords.size)
        assertEquals(50, stockInRecords[0].quantity)
    }
}
