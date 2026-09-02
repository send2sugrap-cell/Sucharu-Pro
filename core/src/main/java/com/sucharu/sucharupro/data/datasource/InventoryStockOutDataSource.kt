package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutLine
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Stock Out & Issue Management data operations (Module 07 Step 04).
 */
interface InventoryStockOutDataSource {

    // Stock Out Headers
    fun observeStockOuts(): Flow<List<InventoryStockOut>>
    suspend fun insertStockOut(stockOut: InventoryStockOut): DomainResult<InventoryStockOut>
    suspend fun updateStockOut(stockOut: InventoryStockOut): DomainResult<InventoryStockOut>
    suspend fun deleteStockOut(stockOutId: String): DomainResult<Unit>

    // Stock Out Lines
    fun observeStockOutLines(): Flow<List<InventoryStockOutLine>>
    suspend fun insertStockOutLine(line: InventoryStockOutLine): DomainResult<InventoryStockOutLine>
    suspend fun updateStockOutLine(line: InventoryStockOutLine): DomainResult<InventoryStockOutLine>
    suspend fun deleteStockOutLine(stockOutLineId: String): DomainResult<Unit>

    // Stock Out Records (Physical Transaction Records)
    fun observeStockOutRecords(): Flow<List<InventoryStockOutRecord>>
    suspend fun insertStockOutRecord(record: InventoryStockOutRecord): DomainResult<InventoryStockOutRecord>

    // Audit Trail
    fun observeAuditEvents(): Flow<List<InventoryStockOutActivityEvent>>
    suspend fun recordAuditEvent(event: InventoryStockOutActivityEvent): DomainResult<InventoryStockOutActivityEvent>
}
