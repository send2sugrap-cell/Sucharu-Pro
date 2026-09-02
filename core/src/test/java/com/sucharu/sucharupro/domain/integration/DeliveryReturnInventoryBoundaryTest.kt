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

class DeliveryReturnInventoryBoundaryTest {

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

            val doOrder = DeliveryOrder("DO-BOUND", "PRJ-BOUND", "DON-BOUND", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-BOUND", "DO-BOUND", "PRJ-BOUND", "PROD-BOUND", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `inventory is untouched throughout return creation, approval, receipt, and inspection, mutating ONLY at explicit restock processing`() = runBlocking {
        // 1. Initial StockIn count = 0
        assertEquals(0, receivingDataSource.observeStockInRecords().first().size)

        // 2. Create Return -> StockIn count = 0
        val r = DeliveryReturn("RET-B1", "PRJ-BOUND", "RN-B1", "DO-BOUND", status = DeliveryReturnStatus.DRAFT, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        val l = DeliveryReturnLine(
            returnLineId = "RL-B1",
            returnId = "RET-B1",
            projectId = "PRJ-BOUND",
            deliveryOrderLineId = "DOL-BOUND",
            productId = "PROD-BOUND",
            returnedQuantity = 40.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        repository.createReturn(r, listOf(l), "u1", UserRole.STAFF)
        assertEquals(0, receivingDataSource.observeStockInRecords().first().size)

        // 3. Submit & Approve -> StockIn count = 0
        repository.submitReturn("RET-B1", "u1", UserRole.STAFF)
        repository.approveReturn("RET-B1", "mgr", UserRole.MANAGER)
        assertEquals(0, receivingDataSource.observeStockInRecords().first().size)

        // 4. Receive -> StockIn count = 0
        repository.startReceiving("RET-B1", "wh", UserRole.WAREHOUSE)
        repository.receiveReturn("RET-B1", mapOf("RL-B1" to 40.0), "wh", UserRole.WAREHOUSE)
        assertEquals(0, receivingDataSource.observeStockInRecords().first().size)

        // 5. Inspect -> StockIn count = 0
        repository.startInspection("RET-B1", "wh", UserRole.WAREHOUSE)
        repository.inspectReturnLine("RET-B1", "RL-B1", acceptedQuantity = 35.0, rejectedQuantity = 5.0, condition = DeliveryReturnLineCondition.GOOD, disposition = DeliveryReturnDisposition.RESTOCK, actorId = "wh", callerRole = UserRole.WAREHOUSE)
        repository.completeInspection("RET-B1", "wh", UserRole.WAREHOUSE)
        assertEquals(0, receivingDataSource.observeStockInRecords().first().size)

        // 6. Explicit RESTOCK processing -> Creates exactly ONE Stock-In record for accepted quantity
        val restockRes = repository.processRestock("RET-B1", "RL-B1", "WH-01", "LOC-01", "wh", UserRole.WAREHOUSE)
        assertTrue(restockRes is DomainResult.Success)

        val stockInRecords = receivingDataSource.observeStockInRecords().first()
        assertEquals(1, stockInRecords.size)
        assertEquals(35, stockInRecords[0].quantity)
        assertEquals("PROD-BOUND", stockInRecords[0].inventoryProductId)
        assertEquals("WH-01", stockInRecords[0].warehouseId)
        assertEquals("LOC-01", stockInRecords[0].locationId)

        // 7. Duplicate restock attempt -> Idempotent, zero duplicate stock in
        val dupRestock = repository.processRestock("RET-B1", "RL-B1", "WH-01", "LOC-01", "wh", UserRole.WAREHOUSE)
        assertTrue(dupRestock is DomainResult.Success)
        assertEquals(1, receivingDataSource.observeStockInRecords().first().size)
    }
}
