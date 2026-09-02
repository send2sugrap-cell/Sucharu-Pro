package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryMovementLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryValuationMethod
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import com.sucharu.sucharupro.domain.repository.InventoryMovementLedgerRepository
import com.sucharu.sucharupro.domain.service.inventory.InventoryBalanceCalculator
import com.sucharu.sucharupro.domain.service.inventory.InventoryValuationCalculator
import com.sucharu.sucharupro.domain.service.inventory.ValuationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive integration test for the Movement Ledger & Inventory Valuation flow (Module 07 Step 09).
 */
class InventoryMovementLedgerEndToEndTest {

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
    fun `comprehensive flow - Movements to Synchronization to Balance and Valuation`() = runBlocking {
        val projectId = "PROJ-E2E"
        
        // 1. Record source movements
        receivingDataSource.insertStockInRecord(InventoryStockInRecord(
            stockInId = "SIN-01", 
            receivingId = "R1", 
            receivingLineId = "RL1",
            projectId = projectId,
            inventoryProductId = "P1", 
            warehouseId = "WH1",
            locationId = "L1", 
            quantity = 100,
            unit = InventoryUnit.PCS,
            createdAt = "2026-08-17T10:00:00Z", 
            createdBy = "user"
        ))
        
        stockOutDataSource.insertStockOutRecord(InventoryStockOutRecord(
            stockOutRecordId = "SOUT-01", 
            stockOutId = "SO-1", 
            stockOutLineId = "SOL1",
            projectId = projectId,
            inventoryProductId = "P1", 
            warehouseId = "WH1",
            locationId = "L1", 
            quantity = 30,
            unit = InventoryUnit.PCS,
            createdAt = "2026-08-17T12:00:00Z", 
            createdBy = "user"
        ))
        
        // 2. Synchronize Ledger
        repository.synchronizeLedger(projectId)
        
        val entries = (repository.getEntries(projectId) as DomainResult.Success).data
        assertEquals(2, entries.size)
        
        // 3. Verify Balance
        val balance = InventoryBalanceCalculator.calculateBalance(entries)
        assertEquals(70.0, balance, 0.0)
        
        // 4. Verify Valuation (injecting costs for test)
        val entriesWithCost = entries.map { 
            if (it.quantity > 0) it.copy(unitCost = 100.0, totalCost = it.quantity * 100.0)
            else it
        }
        
        val valuation = InventoryValuationCalculator.calculateValuation(entriesWithCost, InventoryValuationMethod.FIFO)
        assertTrue(valuation is ValuationResult.Success)
        assertEquals(7000.0, (valuation as ValuationResult.Success).totalValue, 0.0)

        // 5. Verify Reconciliation (Directly calculating expected from datasource)
        val directBalance = 100.0 - 30.0
        assertEquals(directBalance, balance, 0.0)
    }
}
