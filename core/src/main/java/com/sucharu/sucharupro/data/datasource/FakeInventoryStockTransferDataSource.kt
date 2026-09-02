package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferLine
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [InventoryStockTransferDataSource] (Module 07 Step 05).
 */
class FakeInventoryStockTransferDataSource : InventoryStockTransferDataSource {

    private val mutex = Mutex()

    private val transfersFlow = MutableStateFlow<List<InventoryStockTransfer>>(emptyList())
    private val linesFlow = MutableStateFlow<List<InventoryStockTransferLine>>(emptyList())
    private val recordsFlow = MutableStateFlow<List<InventoryStockTransferRecord>>(emptyList())
    private val auditFlow = MutableStateFlow<List<InventoryStockTransferActivityEvent>>(emptyList())

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Headers
    // ──────────────────────────────────────────────────────────────

    override fun observeStockTransfers(): Flow<List<InventoryStockTransfer>> = transfersFlow.asStateFlow()

    override suspend fun insertStockTransfer(transfer: InventoryStockTransfer): DomainResult<InventoryStockTransfer> = mutex.withLock {
        val current = transfersFlow.value.toMutableList()
        if (current.any { it.transferId == transfer.transferId }) {
            return DomainResult.Error(message = "Stock Transfer with ID '${transfer.transferId}' already exists.")
        }
        current.add(transfer)
        transfersFlow.value = current
        DomainResult.Success(transfer)
    }

    override suspend fun updateStockTransfer(transfer: InventoryStockTransfer): DomainResult<InventoryStockTransfer> = mutex.withLock {
        val current = transfersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.transferId == transfer.transferId }
        if (index == -1) {
            return DomainResult.Error(message = "Stock Transfer with ID '${transfer.transferId}' not found.")
        }
        current[index] = transfer
        transfersFlow.value = current
        DomainResult.Success(transfer)
    }

    override suspend fun deleteStockTransfer(transferId: String): DomainResult<Unit> = mutex.withLock {
        val current = transfersFlow.value.toMutableList()
        val removed = current.removeIf { it.transferId == transferId }
        if (removed) {
            transfersFlow.value = current
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Stock Transfer with ID '$transferId' not found.")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Lines
    // ──────────────────────────────────────────────────────────────

    override fun observeStockTransferLines(): Flow<List<InventoryStockTransferLine>> = linesFlow.asStateFlow()

    override suspend fun insertStockTransferLine(line: InventoryStockTransferLine): DomainResult<InventoryStockTransferLine> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        if (current.any { it.transferLineId == line.transferLineId }) {
            return DomainResult.Error(message = "Stock Transfer Line with ID '${line.transferLineId}' already exists.")
        }
        current.add(line)
        linesFlow.value = current
        DomainResult.Success(line)
    }

    override suspend fun updateStockTransferLine(line: InventoryStockTransferLine): DomainResult<InventoryStockTransferLine> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.transferLineId == line.transferLineId }
        if (index == -1) {
            return DomainResult.Error(message = "Stock Transfer Line with ID '${line.transferLineId}' not found.")
        }
        current[index] = line
        linesFlow.value = current
        DomainResult.Success(line)
    }

    override suspend fun deleteStockTransferLine(transferLineId: String): DomainResult<Unit> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        val removed = current.removeIf { it.transferLineId == transferLineId }
        if (removed) {
            linesFlow.value = current
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Stock Transfer Line with ID '$transferLineId' not found.")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Records (Physical Transaction Records)
    // ──────────────────────────────────────────────────────────────

    override fun observeStockTransferRecords(): Flow<List<InventoryStockTransferRecord>> = recordsFlow.asStateFlow()

    override suspend fun insertStockTransferRecord(record: InventoryStockTransferRecord): DomainResult<InventoryStockTransferRecord> = mutex.withLock {
        val current = recordsFlow.value.toMutableList()
        if (current.any { it.transferRecordId == record.transferRecordId }) {
            return DomainResult.Error(message = "Stock Transfer Record with ID '${record.transferRecordId}' already exists.")
        }
        // One record per line for a specific transfer
        if (current.any { it.transferId == record.transferId && it.transferLineId == record.transferLineId }) {
            return DomainResult.Error(message = "A transfer record for transfer '${record.transferId}' line '${record.transferLineId}' already exists.")
        }
        current.add(0, record)
        recordsFlow.value = current
        DomainResult.Success(record)
    }

    // ──────────────────────────────────────────────────────────────
    // Audit Trail
    // ──────────────────────────────────────────────────────────────

    override fun observeAuditEvents(): Flow<List<InventoryStockTransferActivityEvent>> = auditFlow.asStateFlow()

    override suspend fun recordAuditEvent(event: InventoryStockTransferActivityEvent): DomainResult<InventoryStockTransferActivityEvent> = mutex.withLock {
        val current = auditFlow.value.toMutableList()
        current.add(0, event)
        auditFlow.value = current
        DomainResult.Success(event)
    }
}
