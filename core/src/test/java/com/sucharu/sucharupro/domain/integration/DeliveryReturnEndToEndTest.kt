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
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnPriority
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnReason
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReturnEndToEndTest {

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

            val doOrder = DeliveryOrder("DO-E2E", "PRJ-01", "DON-E2E", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine1 = DeliveryOrderLine("DOL-E2E-1", "DO-E2E", "PRJ-01", "PROD-A", 100.0, null)
            val doLine2 = DeliveryOrderLine("DOL-E2E-2", "DO-E2E", "PRJ-01", "PROD-B", 50.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine1, doLine2))
        }
    }

    @Test
    fun `complete end to end multi-line return scenario with mixed restock and quarantine dispositions`() = runBlocking {
        // Step 1: Draft return created by Staff
        val ret = DeliveryReturn(
            returnId = "RET-E2E",
            projectId = "PRJ-01",
            returnNo = "RN-E2E-001",
            deliveryOrderId = "DO-E2E",
            returnType = DeliveryReturnType.CUSTOMER_RETURN,
            returnReason = DeliveryReturnReason.DAMAGED,
            priority = DeliveryReturnPriority.HIGH,
            status = DeliveryReturnStatus.DRAFT,
            requestedBy = "staff-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line1 = DeliveryReturnLine(returnLineId = "RL-E2E-1", returnId = "RET-E2E", projectId = "PRJ-01", deliveryOrderLineId = "DOL-E2E-1", productId = "PROD-A", returnedQuantity = 20.0, createdAt = 1000L, updatedAt = 1000L)
        val line2 = DeliveryReturnLine(returnLineId = "RL-E2E-2", returnId = "RET-E2E", projectId = "PRJ-01", deliveryOrderLineId = "DOL-E2E-2", productId = "PROD-B", returnedQuantity = 10.0, createdAt = 1000L, updatedAt = 1000L)

        val createRes = repository.createReturn(ret, listOf(line1, line2), "staff-1", UserRole.STAFF)
        assertTrue(createRes is DomainResult.Success)

        // Step 2: Submit
        val submitRes = repository.submitReturn("RET-E2E", "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)

        // Step 3: Approve
        val approveRes = repository.approveReturn("RET-E2E", "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)

        // Step 4: Receiving
        repository.startReceiving("RET-E2E", "wh-1", UserRole.WAREHOUSE)
        val receiveRes = repository.receiveReturn("RET-E2E", mapOf("RL-E2E-1" to 20.0, "RL-E2E-2" to 10.0), "wh-1", UserRole.WAREHOUSE)
        assertTrue(receiveRes is DomainResult.Success)

        // Step 5: Inspection
        repository.startInspection("RET-E2E", "qc-1", UserRole.QC_INSPECTOR)
        // Line 1: 18 good (RESTOCK), 2 damaged (SCRAP)
        repository.inspectReturnLine(
            returnId = "RET-E2E",
            returnLineId = "RL-E2E-1",
            acceptedQuantity = 18.0,
            rejectedQuantity = 2.0,
            condition = DeliveryReturnLineCondition.GOOD,
            disposition = DeliveryReturnDisposition.RESTOCK,
            inspectionNotes = "18 good units restocked, 2 damaged",
            actorId = "qc-1",
            callerRole = UserRole.QC_INSPECTOR
        )
        // Line 2: 10 damaged (QUARANTINE)
        repository.inspectReturnLine(
            returnId = "RET-E2E",
            returnLineId = "RL-E2E-2",
            acceptedQuantity = 0.0,
            rejectedQuantity = 10.0,
            condition = DeliveryReturnLineCondition.DAMAGED,
            disposition = DeliveryReturnDisposition.QUARANTINE,
            inspectionNotes = "Defective coating, quarantine for vendor claim",
            actorId = "qc-1",
            callerRole = UserRole.QC_INSPECTOR
        )
        val inspComp = repository.completeInspection("RET-E2E", "qc-1", UserRole.QC_INSPECTOR)
        assertTrue(inspComp is DomainResult.Success)

        // Step 6: Restock only Line 1
        val restockRes = repository.processRestock("RET-E2E", "RL-E2E-1", "WH-01", "LOC-01", "wh-1", UserRole.WAREHOUSE)
        assertTrue(restockRes is DomainResult.Success)

        // Verify only 1 StockIn record created for 18 units of PROD-A
        val stockIns = receivingDataSource.observeStockInRecords().first()
        assertEquals(1, stockIns.size)
        assertEquals(18, stockIns[0].quantity)
        assertEquals("PROD-A", stockIns[0].inventoryProductId)

        // Step 7: Complete Return
        val completeRes = repository.completeReturn("RET-E2E", "mgr-1", UserRole.MANAGER)
        assertTrue(completeRes is DomainResult.Success)
        assertEquals(DeliveryReturnStatus.COMPLETED, (completeRes as DomainResult.Success).data.status)
    }
}
