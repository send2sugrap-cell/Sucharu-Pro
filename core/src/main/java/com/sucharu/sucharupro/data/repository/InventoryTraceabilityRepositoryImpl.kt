package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.traceability.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryTraceabilityRepository
import com.sucharu.sucharupro.domain.validation.InventoryTraceabilityAuthorizationValidator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe implementation of InventoryTraceabilityRepository (Module 07 Step 07).
 */
class InventoryTraceabilityRepositoryImpl(
    private val traceabilityDataSource: InventoryTraceabilityDataSource,
    private val productDataSource: InventoryProductDataSource,
    private val receivingDataSource: InventoryReceivingDataSource,
    private val stockOutDataSource: InventoryStockOutDataSource,
    private val stockTransferDataSource: InventoryStockTransferDataSource,
    private val stockAdjustmentDataSource: InventoryStockAdjustmentDataSource
) : InventoryTraceabilityRepository {

    private val repositoryMutex = Mutex()

    override fun observeBatches(projectId: String): Flow<List<InventoryBatch>> =
        traceabilityDataSource.observeBatches(projectId)

    override fun observeLots(projectId: String): Flow<List<InventoryLot>> =
        traceabilityDataSource.observeLots(projectId)

    override fun observeTraceRecords(projectId: String): Flow<List<InventoryTraceabilityRecord>> =
        traceabilityDataSource.observeTraceRecords(projectId)

    override fun observeActivityEvents(projectId: String): Flow<List<InventoryTraceabilityActivityEvent>> =
        traceabilityDataSource.observeActivityEvents(projectId)

    override suspend fun createBatch(batch: InventoryBatch, actorId: String, actorName: String?, callerRole: UserRole?) = repositoryMutex.withLock {
        if (callerRole != null) {
            val authResult = InventoryTraceabilityAuthorizationValidator.validateRegisterPermission(callerRole)
            if (authResult is DomainResult.Error) return@withLock
        }

        traceabilityDataSource.saveBatch(batch)
        traceabilityDataSource.saveActivityEvent(
            InventoryTraceabilityActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = batch.projectId,
                eventType = InventoryTraceabilityActivityType.REGISTERED,
                targetId = batch.batchId,
                targetType = "BATCH",
                actorId = actorId,
                actorName = actorName,
                description = "Batch ${batch.batchNo} created.",
                timestamp = batch.createdAt
            )
        )
    }

    override suspend fun createLot(lot: InventoryLot, actorId: String, actorName: String?, callerRole: UserRole?) = repositoryMutex.withLock {
        if (callerRole != null) {
            val authResult = InventoryTraceabilityAuthorizationValidator.validateRegisterPermission(callerRole)
            if (authResult is DomainResult.Error) return@withLock
        }

        traceabilityDataSource.saveLot(lot)
        traceabilityDataSource.saveActivityEvent(
            InventoryTraceabilityActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = lot.projectId,
                eventType = InventoryTraceabilityActivityType.REGISTERED,
                targetId = lot.lotId,
                targetType = "LOT",
                actorId = actorId,
                actorName = actorName,
                description = "Lot ${lot.lotNo} created.",
                timestamp = lot.createdAt
            )
        )
    }

    override suspend fun updateBatchStatus(
        batchId: String,
        status: InventoryTraceabilityStatus,
        actorId: String,
        actorName: String?,
        callerRole: UserRole?
    ) = repositoryMutex.withLock {
        if (callerRole != null) {
            val authResult = InventoryTraceabilityAuthorizationValidator.validateStatusChangePermission(callerRole)
            if (authResult is DomainResult.Error) return@withLock
        }

        val existing = traceabilityDataSource.getBatchById(batchId) ?: return@withLock
        val updated = existing.copy(status = status)
        traceabilityDataSource.saveBatch(updated)
        traceabilityDataSource.saveActivityEvent(
            InventoryTraceabilityActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = existing.projectId,
                eventType = InventoryTraceabilityActivityType.STATUS_CHANGED,
                targetId = batchId,
                targetType = "BATCH",
                actorId = actorId,
                actorName = actorName,
                description = "Batch status changed to ${status.defaultLabel}.",
                timestamp = "2026-08-17T16:00:00Z" // Consistent mock timestamp for now
            )
        )
    }

    override suspend fun updateLotStatus(
        lotId: String,
        status: InventoryTraceabilityStatus,
        actorId: String,
        actorName: String?,
        callerRole: UserRole?
    ) = repositoryMutex.withLock {
        if (callerRole != null) {
            val authResult = InventoryTraceabilityAuthorizationValidator.validateStatusChangePermission(callerRole)
            if (authResult is DomainResult.Error) return@withLock
        }

        val existing = traceabilityDataSource.getLotById(lotId) ?: return@withLock
        val updated = existing.copy(status = status)
        traceabilityDataSource.saveLot(updated)
        traceabilityDataSource.saveActivityEvent(
            InventoryTraceabilityActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = existing.projectId,
                eventType = InventoryTraceabilityActivityType.STATUS_CHANGED,
                targetId = lotId,
                targetType = "LOT",
                actorId = actorId,
                actorName = actorName,
                description = "Lot status changed to ${status.defaultLabel}.",
                timestamp = "2026-08-17T16:00:00Z"
            )
        )
    }

    override suspend fun getBatchDetails(batchId: String): InventoryBatch? =
        traceabilityDataSource.getBatchById(batchId)

    override suspend fun getLotDetails(lotId: String): InventoryLot? =
        traceabilityDataSource.getLotById(lotId)

    override suspend fun linkMovementToTraceability(record: InventoryTraceabilityRecord, callerRole: UserRole?) = repositoryMutex.withLock {
        if (callerRole != null) {
            val authResult = InventoryTraceabilityAuthorizationValidator.validateRegisterPermission(callerRole)
            if (authResult is DomainResult.Error) return@withLock
        }

        traceabilityDataSource.saveTraceRecord(record)
    }

    override suspend fun getTraceHistory(targetId: String, targetType: String, callerRole: UserRole?): List<Any> {
        if (callerRole != null) {
            val authResult = InventoryTraceabilityAuthorizationValidator.validateViewPermission(callerRole)
            if (authResult is DomainResult.Error) return emptyList()
        }

        val projectId = if (targetType == "BATCH") {
            traceabilityDataSource.getBatchById(targetId)?.projectId
        } else {
            traceabilityDataSource.getLotById(targetId)?.projectId
        } ?: return emptyList()

        val allTraceRecords = traceabilityDataSource.observeTraceRecords(projectId).first()
        val relevantRecords = allTraceRecords.filter { 
            if (targetType == "BATCH") it.batchId == targetId else it.lotId == targetId 
        }

        val history = mutableListOf<Any>()
        
        relevantRecords.forEach { trace ->
            val movement = when (trace.movementType) {
                InventoryMovementType.STOCK_IN -> 
                    receivingDataSource.observeStockInRecords().first().find { it.stockInId == trace.movementRecordId }
                InventoryMovementType.STOCK_OUT -> 
                    stockOutDataSource.observeStockOutRecords().first().find { it.stockOutRecordId == trace.movementRecordId }
                InventoryMovementType.TRANSFER_IN, InventoryMovementType.TRANSFER_OUT -> 
                    stockTransferDataSource.observeStockTransferRecords().first().find { it.transferRecordId == trace.movementRecordId }
                InventoryMovementType.ADJUSTMENT_IN, InventoryMovementType.ADJUSTMENT_OUT -> 
                    stockAdjustmentDataSource.observeStockAdjustmentRecords().first().find { it.adjustmentRecordId == trace.movementRecordId }
            }
            movement?.let { history.add(it) }
        }
        
        // Add activity events to history
        val events = traceabilityDataSource.observeActivityEvents(projectId).first()
            .filter { it.targetId == targetId && it.targetType == targetType }
        history.addAll(events)
        
        return history // In production, we'd sort this by timestamp
    }

    override fun searchBatches(projectId: String, query: String): Flow<List<InventoryBatch>> =
        traceabilityDataSource.observeBatches(projectId).map { list ->
            list.filter { it.batchNo.contains(query, ignoreCase = true) }
        }

    override fun searchLots(projectId: String, query: String): Flow<List<InventoryLot>> =
        traceabilityDataSource.observeLots(projectId).map { list ->
            list.filter { it.lotNo.contains(query, ignoreCase = true) }
        }
}
