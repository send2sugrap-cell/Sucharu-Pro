package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentLine
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [InventoryStockAdjustmentDataSource] (Module 07 Step 06).
 */
class FakeInventoryStockAdjustmentDataSource : InventoryStockAdjustmentDataSource {

    private val mutex = Mutex()

    private val adjustmentsFlow = MutableStateFlow<List<InventoryStockAdjustment>>(emptyList())
    private val linesFlow = MutableStateFlow<List<InventoryStockAdjustmentLine>>(emptyList())
    private val recordsFlow = MutableStateFlow<List<InventoryStockAdjustmentRecord>>(emptyList())
    private val auditFlow = MutableStateFlow<List<InventoryStockAdjustmentActivityEvent>>(emptyList())

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Headers
    // ──────────────────────────────────────────────────────────────

    override fun observeStockAdjustments(): Flow<List<InventoryStockAdjustment>> = adjustmentsFlow.asStateFlow()

    override suspend fun insertStockAdjustment(adjustment: InventoryStockAdjustment): DomainResult<InventoryStockAdjustment> = mutex.withLock {
        val current = adjustmentsFlow.value.toMutableList()
        if (current.any { it.adjustmentId == adjustment.adjustmentId }) {
            return DomainResult.Error(message = "Stock Adjustment with ID '${adjustment.adjustmentId}' already exists.")
        }
        current.add(adjustment)
        adjustmentsFlow.value = current
        DomainResult.Success(adjustment)
    }

    override suspend fun updateStockAdjustment(adjustment: InventoryStockAdjustment): DomainResult<InventoryStockAdjustment> = mutex.withLock {
        val current = adjustmentsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.adjustmentId == adjustment.adjustmentId }
        if (index == -1) {
            return DomainResult.Error(message = "Stock Adjustment with ID '${adjustment.adjustmentId}' not found.")
        }
        current[index] = adjustment
        adjustmentsFlow.value = current
        DomainResult.Success(adjustment)
    }

    override suspend fun deleteStockAdjustment(adjustmentId: String): DomainResult<Unit> = mutex.withLock {
        val current = adjustmentsFlow.value.toMutableList()
        val removed = current.removeIf { it.adjustmentId == adjustmentId }
        if (removed) {
            adjustmentsFlow.value = current
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Stock Adjustment with ID '$adjustmentId' not found.")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Lines
    // ──────────────────────────────────────────────────────────────

    override fun observeStockAdjustmentLines(): Flow<List<InventoryStockAdjustmentLine>> = linesFlow.asStateFlow()

    override suspend fun insertStockAdjustmentLine(line: InventoryStockAdjustmentLine): DomainResult<InventoryStockAdjustmentLine> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        if (current.any { it.adjustmentLineId == line.adjustmentLineId }) {
            return DomainResult.Error(message = "Stock Adjustment Line with ID '${line.adjustmentLineId}' already exists.")
        }
        current.add(line)
        linesFlow.value = current
        DomainResult.Success(line)
    }

    override suspend fun updateStockAdjustmentLine(line: InventoryStockAdjustmentLine): DomainResult<InventoryStockAdjustmentLine> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.adjustmentLineId == line.adjustmentLineId }
        if (index == -1) {
            return DomainResult.Error(message = "Stock Adjustment Line with ID '${line.adjustmentLineId}' not found.")
        }
        current[index] = line
        linesFlow.value = current
        DomainResult.Success(line)
    }

    override suspend fun deleteStockAdjustmentLine(adjustmentLineId: String): DomainResult<Unit> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        val removed = current.removeIf { it.adjustmentLineId == adjustmentLineId }
        if (removed) {
            linesFlow.value = current
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Stock Adjustment Line with ID '$adjustmentLineId' not found.")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Records (Physical Transaction Records)
    // ──────────────────────────────────────────────────────────────

    override fun observeStockAdjustmentRecords(): Flow<List<InventoryStockAdjustmentRecord>> = recordsFlow.asStateFlow()

    override suspend fun insertStockAdjustmentRecord(record: InventoryStockAdjustmentRecord): DomainResult<InventoryStockAdjustmentRecord> = mutex.withLock {
        val current = recordsFlow.value.toMutableList()
        if (current.any { it.adjustmentRecordId == record.adjustmentRecordId }) {
            return DomainResult.Error(message = "Stock Adjustment Record with ID '${record.adjustmentRecordId}' already exists.")
        }
        // One record per line for a specific adjustment
        if (current.any { it.adjustmentId == record.adjustmentId && it.adjustmentLineId == record.adjustmentLineId }) {
            return DomainResult.Error(message = "An adjustment record for adjustment '${record.adjustmentId}' line '${record.adjustmentLineId}' already exists.")
        }
        current.add(0, record)
        recordsFlow.value = current
        DomainResult.Success(record)
    }

    // ──────────────────────────────────────────────────────────────
    // Audit Trail
    // ──────────────────────────────────────────────────────────────

    override fun observeAuditEvents(): Flow<List<InventoryStockAdjustmentActivityEvent>> = auditFlow.asStateFlow()

    override suspend fun recordAuditEvent(event: InventoryStockAdjustmentActivityEvent): DomainResult<InventoryStockAdjustmentActivityEvent> = mutex.withLock {
        val current = auditFlow.value.toMutableList()
        current.add(0, event)
        auditFlow.value = current
        DomainResult.Success(event)
    }
}
