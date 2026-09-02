package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutLine
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [InventoryStockOutDataSource] (Module 07 Step 04).
 */
class FakeInventoryStockOutDataSource : InventoryStockOutDataSource {

    private val mutex = Mutex()

    private val stockOutsFlow = MutableStateFlow<List<InventoryStockOut>>(emptyList())
    private val linesFlow = MutableStateFlow<List<InventoryStockOutLine>>(emptyList())
    private val recordsFlow = MutableStateFlow<List<InventoryStockOutRecord>>(emptyList())
    private val auditFlow = MutableStateFlow<List<InventoryStockOutActivityEvent>>(emptyList())

    // ──────────────────────────────────────────────────────────────
    // Stock Out Headers
    // ──────────────────────────────────────────────────────────────

    override fun observeStockOuts(): Flow<List<InventoryStockOut>> = stockOutsFlow.asStateFlow()

    override suspend fun insertStockOut(stockOut: InventoryStockOut): DomainResult<InventoryStockOut> = mutex.withLock {
        val current = stockOutsFlow.value.toMutableList()
        if (current.any { it.stockOutId == stockOut.stockOutId }) {
            return DomainResult.Error(message = "Stock Out with ID '${stockOut.stockOutId}' already exists.")
        }
        current.add(stockOut)
        stockOutsFlow.value = current
        DomainResult.Success(stockOut)
    }

    override suspend fun updateStockOut(stockOut: InventoryStockOut): DomainResult<InventoryStockOut> = mutex.withLock {
        val current = stockOutsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.stockOutId == stockOut.stockOutId }
        if (index == -1) {
            return DomainResult.Error(message = "Stock Out with ID '${stockOut.stockOutId}' not found.")
        }
        current[index] = stockOut
        stockOutsFlow.value = current
        DomainResult.Success(stockOut)
    }

    override suspend fun deleteStockOut(stockOutId: String): DomainResult<Unit> = mutex.withLock {
        val current = stockOutsFlow.value.toMutableList()
        val removed = current.removeIf { it.stockOutId == stockOutId }
        if (removed) {
            stockOutsFlow.value = current
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Stock Out with ID '$stockOutId' not found.")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Out Lines
    // ──────────────────────────────────────────────────────────────

    override fun observeStockOutLines(): Flow<List<InventoryStockOutLine>> = linesFlow.asStateFlow()

    override suspend fun insertStockOutLine(line: InventoryStockOutLine): DomainResult<InventoryStockOutLine> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        if (current.any { it.stockOutLineId == line.stockOutLineId }) {
            return DomainResult.Error(message = "Stock Out Line with ID '${line.stockOutLineId}' already exists.")
        }
        current.add(line)
        linesFlow.value = current
        DomainResult.Success(line)
    }

    override suspend fun updateStockOutLine(line: InventoryStockOutLine): DomainResult<InventoryStockOutLine> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.stockOutLineId == line.stockOutLineId }
        if (index == -1) {
            return DomainResult.Error(message = "Stock Out Line with ID '${line.stockOutLineId}' not found.")
        }
        current[index] = line
        linesFlow.value = current
        DomainResult.Success(line)
    }

    override suspend fun deleteStockOutLine(stockOutLineId: String): DomainResult<Unit> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        val removed = current.removeIf { it.stockOutLineId == stockOutLineId }
        if (removed) {
            linesFlow.value = current
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Stock Out Line with ID '$stockOutLineId' not found.")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Out Records (Physical Transaction Records)
    // ──────────────────────────────────────────────────────────────

    override fun observeStockOutRecords(): Flow<List<InventoryStockOutRecord>> = recordsFlow.asStateFlow()

    override suspend fun insertStockOutRecord(record: InventoryStockOutRecord): DomainResult<InventoryStockOutRecord> = mutex.withLock {
        val current = recordsFlow.value.toMutableList()
        if (current.any { it.stockOutRecordId == record.stockOutRecordId }) {
            return DomainResult.Error(message = "Stock Out Record with ID '${record.stockOutRecordId}' already exists.")
        }
        // One record per line for a specific stock out
        if (current.any { it.stockOutId == record.stockOutId && it.stockOutLineId == record.stockOutLineId }) {
            return DomainResult.Error(message = "A stock out record for stock out '${record.stockOutId}' line '${record.stockOutLineId}' already exists.")
        }
        current.add(0, record)
        recordsFlow.value = current
        DomainResult.Success(record)
    }

    // ──────────────────────────────────────────────────────────────
    // Audit Trail
    // ──────────────────────────────────────────────────────────────

    override fun observeAuditEvents(): Flow<List<InventoryStockOutActivityEvent>> = auditFlow.asStateFlow()

    override suspend fun recordAuditEvent(event: InventoryStockOutActivityEvent): DomainResult<InventoryStockOutActivityEvent> = mutex.withLock {
        val current = auditFlow.value.toMutableList()
        current.add(0, event)
        auditFlow.value = current
        DomainResult.Success(event)
    }
}
