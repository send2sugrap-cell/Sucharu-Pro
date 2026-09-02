package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.inventory.traceability.*
import kotlinx.coroutines.flow.*

/**
 * Thread-safe in-memory implementation of InventoryTraceabilityDataSource (Module 07 Step 07).
 */
class FakeInventoryTraceabilityDataSource : InventoryTraceabilityDataSource {
    private val _batches = MutableStateFlow<List<InventoryBatch>>(emptyList())
    private val _lots = MutableStateFlow<List<InventoryLot>>(emptyList())
    private val _traceRecords = MutableStateFlow<List<InventoryTraceabilityRecord>>(emptyList())
    private val _activityEvents = MutableStateFlow<List<InventoryTraceabilityActivityEvent>>(emptyList())

    override fun observeBatches(projectId: String): Flow<List<InventoryBatch>> = 
        _batches.map { list -> list.filter { it.projectId == projectId } }

    override fun observeLots(projectId: String): Flow<List<InventoryLot>> = 
        _lots.map { list -> list.filter { it.projectId == projectId } }

    override fun observeTraceRecords(projectId: String): Flow<List<InventoryTraceabilityRecord>> = 
        _traceRecords.map { list -> list.filter { it.projectId == projectId } }

    override fun observeActivityEvents(projectId: String): Flow<List<InventoryTraceabilityActivityEvent>> = 
        _activityEvents.map { list -> list.filter { it.projectId == projectId } }

    override suspend fun saveBatch(batch: InventoryBatch) {
        _batches.update { current ->
            val index = current.indexOfFirst { it.batchId == batch.batchId }
            if (index != -1) current.toMutableList().apply { set(index, batch) }
            else current + batch
        }
    }

    override suspend fun saveLot(lot: InventoryLot) {
        _lots.update { current ->
            val index = current.indexOfFirst { it.lotId == lot.lotId }
            if (index != -1) current.toMutableList().apply { set(index, lot) }
            else current + lot
        }
    }

    override suspend fun saveTraceRecord(record: InventoryTraceabilityRecord) {
        _traceRecords.update { it + record }
    }

    override suspend fun saveActivityEvent(event: InventoryTraceabilityActivityEvent) {
        _activityEvents.update { it + event }
    }

    override suspend fun getBatchById(batchId: String): InventoryBatch? = 
        _batches.value.find { it.batchId == batchId }

    override suspend fun getLotById(lotId: String): InventoryLot? = 
        _lots.value.find { it.lotId == lotId }

    override suspend fun deleteBatch(batchId: String) {
        _batches.update { it.filterNot { b -> b.batchId == batchId } }
    }

    override suspend fun deleteLot(lotId: String) {
        _lots.update { it.filterNot { l -> l.lotId == lotId } }
    }
}
