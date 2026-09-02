package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiptVerification
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [InventoryReceivingDataSource] (Module 07 Step 03).
 *
 * All state is protected by a single [Mutex] to prevent race conditions.
 * Reactive state is exposed via [MutableStateFlow] for deterministic observation.
 *
 * Duplicate protection:
 * - Receiving IDs must be unique.
 * - Receiving line IDs must be unique.
 * - Stock-in records enforce (receivingId + receivingLineId) uniqueness to prevent
 *   duplicate stock-in for the same line.
 * - Verification IDs must be unique.
 * - Audit event IDs must be unique.
 *
 * Stock-in records and audit events are append-only (prepended to preserve recency order).
 */
class FakeInventoryReceivingDataSource : InventoryReceivingDataSource {

    private val mutex = Mutex()

    private val receivingsFlow = MutableStateFlow<List<InventoryReceiving>>(emptyList())
    private val linesFlow = MutableStateFlow<List<InventoryReceivingLine>>(emptyList())
    private val verificationsFlow = MutableStateFlow<List<InventoryReceiptVerification>>(emptyList())
    private val stockInFlow = MutableStateFlow<List<InventoryStockInRecord>>(emptyList())
    private val auditFlow = MutableStateFlow<List<InventoryReceivingActivityEvent>>(emptyList())

    // ──────────────────────────────────────────────────────────────
    // Receiving
    // ──────────────────────────────────────────────────────────────

    override fun observeReceivings(): Flow<List<InventoryReceiving>> = receivingsFlow.asStateFlow()

    override suspend fun insertReceiving(receiving: InventoryReceiving): DomainResult<InventoryReceiving> = mutex.withLock {
        val current = receivingsFlow.value.toMutableList()
        if (current.any { it.receivingId == receiving.receivingId }) {
            return DomainResult.Error(message = "Receiving with ID '${receiving.receivingId}' already exists.")
        }
        current.add(receiving)
        receivingsFlow.value = current
        DomainResult.Success(receiving)
    }

    override suspend fun updateReceiving(receiving: InventoryReceiving): DomainResult<InventoryReceiving> = mutex.withLock {
        val current = receivingsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.receivingId == receiving.receivingId }
        if (index == -1) {
            return DomainResult.Error(message = "Receiving with ID '${receiving.receivingId}' not found.")
        }
        current[index] = receiving
        receivingsFlow.value = current
        DomainResult.Success(receiving)
    }

    // ──────────────────────────────────────────────────────────────
    // Receiving Lines
    // ──────────────────────────────────────────────────────────────

    override fun observeReceivingLines(): Flow<List<InventoryReceivingLine>> = linesFlow.asStateFlow()

    override suspend fun insertReceivingLine(line: InventoryReceivingLine): DomainResult<InventoryReceivingLine> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        if (current.any { it.receivingLineId == line.receivingLineId }) {
            return DomainResult.Error(message = "Receiving line with ID '${line.receivingLineId}' already exists.")
        }
        current.add(line)
        linesFlow.value = current
        DomainResult.Success(line)
    }

    override suspend fun updateReceivingLine(line: InventoryReceivingLine): DomainResult<InventoryReceivingLine> = mutex.withLock {
        val current = linesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.receivingLineId == line.receivingLineId }
        if (index == -1) {
            return DomainResult.Error(message = "Receiving line with ID '${line.receivingLineId}' not found.")
        }
        current[index] = line
        linesFlow.value = current
        DomainResult.Success(line)
    }

    // ──────────────────────────────────────────────────────────────
    // Verifications (append-only)
    // ──────────────────────────────────────────────────────────────

    override fun observeVerifications(): Flow<List<InventoryReceiptVerification>> = verificationsFlow.asStateFlow()

    override suspend fun insertVerification(verification: InventoryReceiptVerification): DomainResult<InventoryReceiptVerification> = mutex.withLock {
        val current = verificationsFlow.value.toMutableList()
        if (current.any { it.verificationId == verification.verificationId }) {
            return DomainResult.Error(message = "Verification with ID '${verification.verificationId}' already exists.")
        }
        // Also check that we don't have a verification for the same line (one verification per line)
        if (current.any { it.receivingLineId == verification.receivingLineId }) {
            return DomainResult.Error(
                message = "A verification for receiving line '${verification.receivingLineId}' already exists."
            )
        }
        current.add(0, verification)
        verificationsFlow.value = current
        DomainResult.Success(verification)
    }

    // ──────────────────────────────────────────────────────────────
    // Stock-In Records (write-once, append-only)
    // ──────────────────────────────────────────────────────────────

    override fun observeStockInRecords(): Flow<List<InventoryStockInRecord>> = stockInFlow.asStateFlow()

    override suspend fun insertStockInRecord(record: InventoryStockInRecord): DomainResult<InventoryStockInRecord> = mutex.withLock {
        val current = stockInFlow.value.toMutableList()
        // Duplicate stockInId protection
        if (current.any { it.stockInId == record.stockInId }) {
            return DomainResult.Error(message = "Stock-in record with ID '${record.stockInId}' already exists.")
        }
        // Duplicate per-line protection: exactly ONE stock-in per receiving line
        if (current.any { it.receivingId == record.receivingId && it.receivingLineId == record.receivingLineId }) {
            return DomainResult.Error(
                message = "A stock-in record for receiving '${record.receivingId}' line '${record.receivingLineId}' already exists. " +
                    "Duplicate stock-in is forbidden."
            )
        }
        current.add(0, record)
        stockInFlow.value = current
        DomainResult.Success(record)
    }

    // ──────────────────────────────────────────────────────────────
    // Audit Events (append-only)
    // ──────────────────────────────────────────────────────────────

    override fun observeAuditEvents(): Flow<List<InventoryReceivingActivityEvent>> = auditFlow.asStateFlow()

    override suspend fun recordAuditEvent(event: InventoryReceivingActivityEvent): DomainResult<Unit> = mutex.withLock {
        val current = auditFlow.value.toMutableList()
        current.add(0, event)
        auditFlow.value = current
        DomainResult.Success(Unit)
    }
}
