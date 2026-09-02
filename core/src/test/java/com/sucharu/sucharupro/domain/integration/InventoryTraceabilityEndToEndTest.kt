package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryTraceabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord
import com.sucharu.sucharupro.domain.model.inventory.traceability.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryTraceabilityRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * End-to-end integration tests for Batch & Lot Traceability (Module 07 Step 07).
 */
class InventoryTraceabilityEndToEndTest {

    private lateinit var repository: InventoryTraceabilityRepository
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var transferDataSource: FakeInventoryStockTransferDataSource

    @Before
    fun setup() {
        receivingDataSource = FakeInventoryReceivingDataSource()
        transferDataSource = FakeInventoryStockTransferDataSource()
        
        repository = InventoryTraceabilityRepositoryImpl(
            traceabilityDataSource = FakeInventoryTraceabilityDataSource(),
            productDataSource = FakeInventoryProductDataSource(),
            receivingDataSource = receivingDataSource,
            stockOutDataSource = FakeInventoryStockOutDataSource(),
            stockTransferDataSource = transferDataSource,
            stockAdjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
        )
    }

    @Test
    fun `full traceability flow for a production batch`() = runBlocking {
        val projectId = "PRJ-01"
        val productId = "PROD-01"
        val batchId = "BATCH-01"
        val actorId = "admin-1"
        val actorName = "Admin"

        // 1. Register Batch
        val batch = InventoryBatch(
            batchId = batchId,
            batchNo = "B-2026-001",
            projectId = projectId,
            productId = productId,
            productionReferenceId = "JOB-01",
            productionReferenceType = "JOB",
            status = InventoryTraceabilityStatus.ACTIVE,
            createdAt = "2026-08-17T10:00:00Z"
        )
        repository.createBatch(batch, actorId, actorName, UserRole.ADMIN)

        // 2. Receive stock (linked to this batch)
        val stockInId = "STK-IN-01"
        val stockInRecord = InventoryStockInRecord(
            stockInId = stockInId,
            receivingId = "RCV-01",
            receivingLineId = "RCV-L1",
            projectId = projectId,
            inventoryProductId = productId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            quantity = 500,
            unit = InventoryUnit.PCS,
            createdBy = actorId,
            createdAt = "2026-08-17T11:00:00Z"
        )
        receivingDataSource.insertStockInRecord(stockInRecord)

        // Link movement to batch
        repository.linkMovementToTraceability(
            InventoryTraceabilityRecord(
                traceRecordId = "TR-01",
                batchId = batchId,
                lotId = null,
                projectId = projectId,
                productId = productId,
                locationId = "LOC-01",
                movementRecordId = stockInId,
                movementType = InventoryMovementType.STOCK_IN,
                quantity = 500.0,
                unit = InventoryUnit.PCS,
                actorId = actorId,
                timestamp = "2026-08-17T11:05:00Z"
            ),
            UserRole.ADMIN
        )

        // 3. Transfer some stock (linked to this batch)
        val transferRecId = "TRF-REC-01"
        val transferRecord = InventoryStockTransferRecord(
            transferRecordId = transferRecId,
            transferId = "TRF-01",
            transferLineId = "TRF-L1",
            projectId = projectId,
            inventoryProductId = productId,
            fromWarehouseId = "WH-01",
            fromLocationId = "LOC-01",
            toWarehouseId = "WH-02",
            toLocationId = "LOC-02",
            quantity = 200,
            unit = InventoryUnit.PCS,
            createdBy = actorId,
            createdAt = "2026-08-17T14:00:00Z"
        )
        transferDataSource.insertStockTransferRecord(transferRecord)

        // Link movement to batch
        repository.linkMovementToTraceability(
            InventoryTraceabilityRecord(
                traceRecordId = "TR-02",
                batchId = batchId,
                lotId = null,
                projectId = projectId,
                productId = productId,
                locationId = "LOC-01", // Source location
                movementRecordId = transferRecId,
                movementType = InventoryMovementType.TRANSFER_OUT,
                quantity = 200.0,
                unit = InventoryUnit.PCS,
                actorId = actorId,
                timestamp = "2026-08-17T14:05:00Z"
            ),
            UserRole.ADMIN
        )

        // 4. Verify full Trace History
        val history = repository.getTraceHistory(batchId, "BATCH", UserRole.ADMIN)
        
        // Expected items:
        // 1. Registered event
        // 2. Stock In movement
        // 3. Transfer Out movement
        assertEquals(3, history.size)
        assertTrue(history.any { it is InventoryStockInRecord && it.stockInId == stockInId })
        assertTrue(history.any { it is InventoryStockTransferRecord && it.transferRecordId == transferRecId })
        assertTrue(history.any { it is InventoryTraceabilityActivityEvent && it.eventType == InventoryTraceabilityActivityType.REGISTERED })
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
