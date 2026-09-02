package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.inventory.traceability.*
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Batches, Lots, TraceRecords, and ActivityEvents (Module 07 Step 07).
 */
interface InventoryTraceabilityDataSource {
    fun observeBatches(projectId: String): Flow<List<InventoryBatch>>
    fun observeLots(projectId: String): Flow<List<InventoryLot>>
    fun observeTraceRecords(projectId: String): Flow<List<InventoryTraceabilityRecord>>
    fun observeActivityEvents(projectId: String): Flow<List<InventoryTraceabilityActivityEvent>>

    suspend fun saveBatch(batch: InventoryBatch)
    suspend fun saveLot(lot: InventoryLot)
    suspend fun saveTraceRecord(record: InventoryTraceabilityRecord)
    suspend fun saveActivityEvent(event: InventoryTraceabilityActivityEvent)

    suspend fun getBatchById(batchId: String): InventoryBatch?
    suspend fun getLotById(lotId: String): InventoryLot?
    
    suspend fun deleteBatch(batchId: String)
    suspend fun deleteLot(lotId: String)
}
