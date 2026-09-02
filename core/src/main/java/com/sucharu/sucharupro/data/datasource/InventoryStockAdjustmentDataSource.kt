package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentLine
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentRecord
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Stock Adjustment Management data operations (Module 07 Step 06).
 */
interface InventoryStockAdjustmentDataSource {

    // Stock Adjustment Headers
    fun observeStockAdjustments(): Flow<List<InventoryStockAdjustment>>
    suspend fun insertStockAdjustment(adjustment: InventoryStockAdjustment): DomainResult<InventoryStockAdjustment>
    suspend fun updateStockAdjustment(adjustment: InventoryStockAdjustment): DomainResult<InventoryStockAdjustment>
    suspend fun deleteStockAdjustment(adjustmentId: String): DomainResult<Unit>

    // Stock Adjustment Lines
    fun observeStockAdjustmentLines(): Flow<List<InventoryStockAdjustmentLine>>
    suspend fun insertStockAdjustmentLine(line: InventoryStockAdjustmentLine): DomainResult<InventoryStockAdjustmentLine>
    suspend fun updateStockAdjustmentLine(line: InventoryStockAdjustmentLine): DomainResult<InventoryStockAdjustmentLine>
    suspend fun deleteStockAdjustmentLine(adjustmentLineId: String): DomainResult<Unit>

    // Stock Adjustment Records (Physical Transaction Records)
    fun observeStockAdjustmentRecords(): Flow<List<InventoryStockAdjustmentRecord>>
    suspend fun insertStockAdjustmentRecord(record: InventoryStockAdjustmentRecord): DomainResult<InventoryStockAdjustmentRecord>

    // Audit Trail
    fun observeAuditEvents(): Flow<List<InventoryStockAdjustmentActivityEvent>>
    suspend fun recordAuditEvent(event: InventoryStockAdjustmentActivityEvent): DomainResult<InventoryStockAdjustmentActivityEvent>
}
