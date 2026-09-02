package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiptVerification
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import kotlinx.coroutines.flow.Flow

/**
 * Reactive data source contract for Stock Receiving and Stock-In operations (Module 07 Step 03).
 *
 * The data source owns raw persistence; the repository owns all validation and business logic.
 * Implementations must be thread-safe and deterministic.
 */
interface InventoryReceivingDataSource {

    // Receiving
    fun observeReceivings(): Flow<List<InventoryReceiving>>
    suspend fun insertReceiving(receiving: InventoryReceiving): DomainResult<InventoryReceiving>
    suspend fun updateReceiving(receiving: InventoryReceiving): DomainResult<InventoryReceiving>

    // Receiving Lines
    fun observeReceivingLines(): Flow<List<InventoryReceivingLine>>
    suspend fun insertReceivingLine(line: InventoryReceivingLine): DomainResult<InventoryReceivingLine>
    suspend fun updateReceivingLine(line: InventoryReceivingLine): DomainResult<InventoryReceivingLine>

    // Verifications (append-only)
    fun observeVerifications(): Flow<List<InventoryReceiptVerification>>
    suspend fun insertVerification(verification: InventoryReceiptVerification): DomainResult<InventoryReceiptVerification>

    // Stock-In Records (append-only, write-once)
    fun observeStockInRecords(): Flow<List<InventoryStockInRecord>>
    suspend fun insertStockInRecord(record: InventoryStockInRecord): DomainResult<InventoryStockInRecord>

    // Audit Events (append-only)
    fun observeAuditEvents(): Flow<List<InventoryReceivingActivityEvent>>
    suspend fun recordAuditEvent(event: InventoryReceivingActivityEvent): DomainResult<Unit>
}
