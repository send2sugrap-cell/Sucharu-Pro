package com.sucharu.sucharupro.domain.service

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentReason
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentType
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentRecord
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord
import com.sucharu.sucharupro.domain.service.inventory.InventoryMovementLedgerBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies mapping of all source records to ledger entries (Module 07 Step 09).
 */
class InventoryMovementLedgerNormalizationTest {

    @Test
    fun `mapping StockIn to ledger entry`() {
        val record = InventoryStockInRecord(
            stockInId = "SIN-01",
            receivingId = "REC-01",
            receivingLineId = "RECL-01",
            projectId = "PROJ-01",
            inventoryProductId = "PROD-01",
            warehouseId = "WH-01",
            locationId = "LOC-01",
            quantity = 100,
            unit = InventoryUnit.PCS,
            createdAt = "2026-08-17T10:00:00Z",
            createdBy = "user-01"
        )
        
        val entry = InventoryMovementLedgerBuilder.buildFromStockIn(record, unitCost = 10.0)
        
        assertEquals("PROJ-01", entry.projectId)
        assertEquals("PROD-01", entry.productId)
        assertEquals(100.0, entry.quantity, 0.0)
        assertEquals(InventoryMovementDirection.IN, entry.direction)
        assertEquals(InventoryMovementLedgerType.STOCK_IN, entry.movementType)
        assertEquals(10.0, entry.unitCost!!, 0.0)
        assertEquals(1000.0, entry.totalCost!!, 0.0)
    }

    @Test
    fun `mapping StockOut to ledger entry`() {
        val record = InventoryStockOutRecord(
            stockOutRecordId = "SOUT-01",
            stockOutId = "SO-01",
            stockOutLineId = "SOL-01",
            projectId = "PROJ-01",
            inventoryProductId = "PROD-01",
            warehouseId = "WH-01",
            locationId = "LOC-01",
            quantity = 20,
            unit = InventoryUnit.PCS,
            createdAt = "2026-08-17T12:00:00Z",
            createdBy = "user-01"
        )
        
        val entry = InventoryMovementLedgerBuilder.buildFromStockOut(record, unitCost = 12.0)
        
        assertEquals(-20.0, entry.quantity, 0.0)
        assertEquals(InventoryMovementDirection.OUT, entry.direction)
        assertEquals(InventoryMovementLedgerType.STOCK_OUT, entry.movementType)
    }

    @Test
    fun `mapping StockTransfer to dual ledger entries`() {
        val record = InventoryStockTransferRecord(
            transferRecordId = "TR-01",
            transferId = "T-01",
            transferLineId = "TL-01",
            projectId = "PROJ-01",
            inventoryProductId = "PROD-01",
            fromWarehouseId = "WH-01",
            fromLocationId = "LOC-01",
            toWarehouseId = "WH-01",
            toLocationId = "LOC-02",
            quantity = 50,
            unit = InventoryUnit.PCS,
            createdAt = "2026-08-17T14:00:00Z",
            createdBy = "user-01"
        )
        
        val entries = InventoryMovementLedgerBuilder.buildFromStockTransfer(record, unitCost = 15.0)
        
        assertEquals(2, entries.size)
        
        val outEntry = entries.find { it.direction == InventoryMovementDirection.OUT }!!
        val inEntry = entries.find { it.direction == InventoryMovementDirection.IN }!!
        
        assertEquals("LOC-01", outEntry.locationId)
        assertEquals(-50.0, outEntry.quantity, 0.0)
        
        assertEquals("LOC-02", inEntry.locationId)
        assertEquals(50.0, inEntry.quantity, 0.0)
    }

    @Test
    fun `mapping StockAdjustment to ledger entry`() {
        val record = InventoryStockAdjustmentRecord(
            adjustmentRecordId = "ADJR-01",
            adjustmentId = "ADJ-01",
            adjustmentLineId = "ADJL-01",
            projectId = "PROJ-01",
            inventoryProductId = "PROD-01",
            warehouseId = "WH-01",
            locationId = "LOC-01",
            adjustmentType = InventoryAdjustmentType.INCREASE,
            adjustmentReason = InventoryAdjustmentReason.FOUND,
            quantity = 5,
            unit = InventoryUnit.PCS,
            createdAt = "2026-08-17T16:00:00Z",
            createdBy = "user-01"
        )
        
        val entry = InventoryMovementLedgerBuilder.buildFromStockAdjustment(record, unitCost = 20.0)
        
        assertEquals(5.0, entry.quantity, 0.0)
        assertEquals(InventoryMovementDirection.IN, entry.direction)
        assertEquals(InventoryMovementLedgerType.ADJUSTMENT_IN, entry.movementType)
    }
}
