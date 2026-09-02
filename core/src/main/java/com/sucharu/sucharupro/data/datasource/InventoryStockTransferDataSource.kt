package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferLine
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Stock Transfer Management data operations (Module 07 Step 05).
 */
interface InventoryStockTransferDataSource {

    // Stock Transfer Headers
    fun observeStockTransfers(): Flow<List<InventoryStockTransfer>>
    suspend fun insertStockTransfer(transfer: InventoryStockTransfer): DomainResult<InventoryStockTransfer>
    suspend fun updateStockTransfer(transfer: InventoryStockTransfer): DomainResult<InventoryStockTransfer>
    suspend fun deleteStockTransfer(transferId: String): DomainResult<Unit>

    // Stock Transfer Lines
    fun observeStockTransferLines(): Flow<List<InventoryStockTransferLine>>
    suspend fun insertStockTransferLine(line: InventoryStockTransferLine): DomainResult<InventoryStockTransferLine>
    suspend fun updateStockTransferLine(line: InventoryStockTransferLine): DomainResult<InventoryStockTransferLine>
    suspend fun deleteStockTransferLine(transferLineId: String): DomainResult<Unit>

    // Stock Transfer Records (Physical Transaction Records)
    fun observeStockTransferRecords(): Flow<List<InventoryStockTransferRecord>>
    suspend fun insertStockTransferRecord(record: InventoryStockTransferRecord): DomainResult<InventoryStockTransferRecord>

    // Audit Trail
    fun observeAuditEvents(): Flow<List<InventoryStockTransferActivityEvent>>
    suspend fun recordAuditEvent(event: InventoryStockTransferActivityEvent): DomainResult<InventoryStockTransferActivityEvent>
}
