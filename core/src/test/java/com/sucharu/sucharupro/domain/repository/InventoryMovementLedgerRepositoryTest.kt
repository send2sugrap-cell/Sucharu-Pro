package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryMovementLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests synchronization and query logic for InventoryMovementLedgerRepository (Module 07 Step 09).
 */
class InventoryMovementLedgerRepositoryTest {

    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var transferDataSource: FakeInventoryStockTransferDataSource
    private lateinit var adjustmentDataSource: FakeInventoryStockAdjustmentDataSource
    private lateinit var traceabilityDataSource: FakeInventoryTraceabilityDataSource
    
    private lateinit var repository: InventoryMovementLedgerRepository

    @Before
    fun setup() {
        ledgerDataSource = FakeInventoryMovementLedgerDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        stockOutDataSource = FakeInventoryStockOutDataSource()
        transferDataSource = FakeInventoryStockTransferDataSource()
        adjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
        traceabilityDataSource = FakeInventoryTraceabilityDataSource()
        
        repository = InventoryMovementLedgerRepositoryImpl(
            ledgerDataSource,
            receivingDataSource,
            stockOutDataSource,
            transferDataSource,
            adjustmentDataSource,
            traceabilityDataSource
        )
    }

    @Test
    fun `synchronization is idempotent`() = runBlocking {
        val projectId = "PROJ-01"
        
        // Add a stock-in record
        receivingDataSource.insertStockInRecord(InventoryStockInRecord(
            stockInId = "SIN-01",
            receivingId = "REC-01",
            receivingLineId = "RECL-01",
            projectId = projectId,
            inventoryProductId = "P1",
            warehouseId = "WH-01",
            locationId = "L1",
            quantity = 10,
            unit = InventoryUnit.PCS,
            createdAt = "2026-08-17T10:00:00Z",
            createdBy = "user"
        ))
        
        // First sync
        val res1 = repository.synchronizeLedger(projectId)
        assertTrue(res1 is DomainResult.Success)
        
        val entries1 = (repository.getEntries(projectId) as DomainResult.Success).data
        assertEquals(1, entries1.size)
        
        // Second sync - should not add duplicate entries
        val res2 = repository.synchronizeLedger(projectId)
        assertTrue(res2 is DomainResult.Success)
        
        val entries2 = (repository.getEntries(projectId) as DomainResult.Success).data
        assertEquals(1, entries2.size)
    }

    @Test
    fun `project isolation - only syncs records for the specified project`() = runBlocking {
        receivingDataSource.insertStockInRecord(InventoryStockInRecord(
            stockInId = "SIN-01",
            receivingId = "REC-01",
            receivingLineId = "RECL-01",
            projectId = "PROJ-01",
            inventoryProductId = "P1",
            warehouseId = "WH-01",
            locationId = "L1",
            quantity = 10,
            unit = InventoryUnit.PCS,
            createdAt = "2026-08-17T10:00:00Z",
            createdBy = "user"
        ))
        
        receivingDataSource.insertStockInRecord(InventoryStockInRecord(
            stockInId = "SIN-02",
            receivingId = "REC-02",
            receivingLineId = "RECL-02",
            projectId = "PROJ-02",
            inventoryProductId = "P1",
            warehouseId = "WH-01",
            locationId = "L1",
            quantity = 20,
            unit = InventoryUnit.PCS,
            createdAt = "2026-08-17T10:00:00Z",
            createdBy = "user"
        ))
        
        repository.synchronizeLedger("PROJ-01")
        
        val entries1 = (repository.getEntries("PROJ-01") as DomainResult.Success).data
        assertEquals(1, entries1.size)
        
        val entries2 = (repository.getEntries("PROJ-02") as DomainResult.Success).data
        assertEquals(0, entries2.size)
    }
}
