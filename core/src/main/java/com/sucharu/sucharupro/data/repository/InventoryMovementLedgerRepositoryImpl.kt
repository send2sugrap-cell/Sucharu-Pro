package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentType
import com.sucharu.sucharupro.domain.model.inventory.ledger.*
import com.sucharu.sucharupro.domain.repository.InventoryMovementLedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.time.ZonedDateTime

class InventoryMovementLedgerRepositoryImpl(
    private val ledgerDataSource: InventoryMovementLedgerDataSource,
    private val receivingDataSource: InventoryReceivingDataSource,
    private val stockOutDataSource: InventoryStockOutDataSource,
    private val transferDataSource: InventoryStockTransferDataSource,
    private val adjustmentDataSource: InventoryStockAdjustmentDataSource,
    private val traceabilityDataSource: InventoryTraceabilityDataSource
) : InventoryMovementLedgerRepository {

    private val mutex = Mutex()

    override fun observeEntries(projectId: String): Flow<List<InventoryMovementLedgerEntry>> =
        ledgerDataSource.observeEntries(projectId)

    override suspend fun getEntries(projectId: String): DomainResult<List<InventoryMovementLedgerEntry>> {
        return DomainResult.Success(ledgerDataSource.getEntries(projectId))
    }

    override suspend fun getBalance(projectId: String, productId: String, locationId: String): Double {
        return ledgerDataSource.getEntries(projectId)
            .filter { it.productId == productId && it.locationId == locationId }
            .sumOf { it.quantity }
    }

    override suspend fun synchronizeLedger(projectId: String): DomainResult<Unit> = mutex.withLock {
        try {
            val existingEntries = ledgerDataSource.getEntries(projectId)
            val existingSourceIds = existingEntries.map { it.sourceMovementId to it.movementType }.toSet()
            
            val traceRecords = traceabilityDataSource.observeTraceRecords(projectId).first()
            val newEntries = mutableListOf<InventoryMovementLedgerEntry>()
            val now = ZonedDateTime.now().toString()

            // 1. Stock In Records
            val stockInRecords = receivingDataSource.observeStockInRecords().first().filter { it.projectId == projectId }
            stockInRecords.forEach { record ->
                if (!existingSourceIds.contains(record.stockInId to InventoryMovementLedgerType.STOCK_IN)) {
                    val trace = traceRecords.find { it.movementRecordId == record.stockInId }
                    newEntries.add(InventoryMovementLedgerEntry(
                        ledgerEntryId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        productId = record.inventoryProductId,
                        locationId = record.locationId,
                        batchId = trace?.batchId,
                        lotId = trace?.lotId,
                        movementType = InventoryMovementLedgerType.STOCK_IN,
                        direction = InventoryMovementDirection.IN,
                        quantity = record.quantity.toDouble(),
                        referenceId = record.receivingId,
                        referenceType = "RECEIVING",
                        movementAt = record.createdAt,
                        sourceMovementId = record.stockInId,
                        createdAt = now
                    ))
                }
            }

            // 2. Stock Out Records
            val stockOutRecords = stockOutDataSource.observeStockOutRecords().first().filter { it.projectId == projectId }
            stockOutRecords.forEach { record ->
                if (!existingSourceIds.contains(record.stockOutRecordId to InventoryMovementLedgerType.STOCK_OUT)) {
                    val trace = traceRecords.find { it.movementRecordId == record.stockOutRecordId }
                    newEntries.add(InventoryMovementLedgerEntry(
                        ledgerEntryId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        productId = record.inventoryProductId,
                        locationId = record.locationId,
                        batchId = trace?.batchId,
                        lotId = trace?.lotId,
                        movementType = InventoryMovementLedgerType.STOCK_OUT,
                        direction = InventoryMovementDirection.OUT,
                        quantity = -record.quantity.toDouble(),
                        referenceId = record.stockOutId,
                        referenceType = "STOCK_OUT",
                        movementAt = record.createdAt,
                        sourceMovementId = record.stockOutRecordId,
                        createdAt = now
                    ))
                }
            }

            // 3. Transfer Records
            val transferRecords = transferDataSource.observeStockTransferRecords().first().filter { it.projectId == projectId }
            transferRecords.forEach { record ->
                // TRANSFER_OUT
                if (!existingSourceIds.contains(record.transferRecordId to InventoryMovementLedgerType.TRANSFER_OUT)) {
                    val trace = traceRecords.find { it.movementRecordId == record.transferRecordId }
                    newEntries.add(InventoryMovementLedgerEntry(
                        ledgerEntryId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        productId = record.inventoryProductId,
                        locationId = record.fromLocationId,
                        batchId = trace?.batchId,
                        lotId = trace?.lotId,
                        movementType = InventoryMovementLedgerType.TRANSFER_OUT,
                        direction = InventoryMovementDirection.OUT,
                        quantity = -record.quantity.toDouble(),
                        referenceId = record.transferId,
                        referenceType = "TRANSFER",
                        movementAt = record.createdAt,
                        sourceMovementId = record.transferRecordId,
                        createdAt = now
                    ))
                }
                // TRANSFER_IN
                if (!existingSourceIds.contains(record.transferRecordId to InventoryMovementLedgerType.TRANSFER_IN)) {
                    val trace = traceRecords.find { it.movementRecordId == record.transferRecordId }
                    newEntries.add(InventoryMovementLedgerEntry(
                        ledgerEntryId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        productId = record.inventoryProductId,
                        locationId = record.toLocationId,
                        batchId = trace?.batchId,
                        lotId = trace?.lotId,
                        movementType = InventoryMovementLedgerType.TRANSFER_IN,
                        direction = InventoryMovementDirection.IN,
                        quantity = record.quantity.toDouble(),
                        referenceId = record.transferId,
                        referenceType = "TRANSFER",
                        movementAt = record.createdAt,
                        sourceMovementId = record.transferRecordId,
                        createdAt = now
                    ))
                }
            }

            // 4. Adjustment Records
            val adjustmentRecords = adjustmentDataSource.observeStockAdjustmentRecords().first().filter { it.projectId == projectId }
            adjustmentRecords.forEach { record ->
                val (type, direction) = when (record.adjustmentType) {
                    InventoryAdjustmentType.INCREASE -> InventoryMovementLedgerType.ADJUSTMENT_IN to InventoryMovementDirection.IN
                    InventoryAdjustmentType.DECREASE -> InventoryMovementLedgerType.ADJUSTMENT_OUT to InventoryMovementDirection.OUT
                }
                
                if (!existingSourceIds.contains(record.adjustmentRecordId to type)) {
                    val trace = traceRecords.find { it.movementRecordId == record.adjustmentRecordId }
                    newEntries.add(InventoryMovementLedgerEntry(
                        ledgerEntryId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        productId = record.inventoryProductId,
                        locationId = record.locationId,
                        batchId = trace?.batchId,
                        lotId = trace?.lotId,
                        movementType = type,
                        direction = direction,
                        quantity = if (direction == InventoryMovementDirection.IN) record.quantity.toDouble() else -record.quantity.toDouble(),
                        referenceId = record.adjustmentId,
                        referenceType = "ADJUSTMENT",
                        movementAt = record.createdAt,
                        sourceMovementId = record.adjustmentRecordId,
                        createdAt = now
                    ))
                }
            }

            if (newEntries.isNotEmpty()) {
                ledgerDataSource.insertEntries(newEntries)
                ledgerDataSource.recordActivityEvent(InventoryLedgerActivityEvent(
                    eventId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    activityType = InventoryLedgerActivityType.LEDGER_BUILT,
                    performedBy = "SYSTEM",
                    occurredAt = now,
                    description = "Synchronized ${newEntries.size} new entries into the ledger."
                ))
            }

            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override fun observeActivityEvents(projectId: String): Flow<List<InventoryLedgerActivityEvent>> =
        ledgerDataSource.observeActivityEvents(projectId)
}
