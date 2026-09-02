package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.inventory.traceability.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Batch & Lot Traceability Management (Module 07 Step 07).
 */
interface InventoryTraceabilityRepository {
    fun observeBatches(projectId: String): Flow<List<InventoryBatch>>
    fun observeLots(projectId: String): Flow<List<InventoryLot>>
    fun observeTraceRecords(projectId: String): Flow<List<InventoryTraceabilityRecord>>
    fun observeActivityEvents(projectId: String): Flow<List<InventoryTraceabilityActivityEvent>>
    
    suspend fun createBatch(batch: InventoryBatch, actorId: String, actorName: String?, callerRole: UserRole? = null)
    suspend fun createLot(lot: InventoryLot, actorId: String, actorName: String?, callerRole: UserRole? = null)
    
    suspend fun updateBatchStatus(batchId: String, status: InventoryTraceabilityStatus, actorId: String, actorName: String?, callerRole: UserRole? = null)
    suspend fun updateLotStatus(lotId: String, status: InventoryTraceabilityStatus, actorId: String, actorName: String?, callerRole: UserRole? = null)
    
    suspend fun getBatchDetails(batchId: String): InventoryBatch?
    suspend fun getLotDetails(lotId: String): InventoryLot?
    
    suspend fun linkMovementToTraceability(record: InventoryTraceabilityRecord, callerRole: UserRole? = null)
    
    suspend fun getTraceHistory(targetId: String, targetType: String, callerRole: UserRole? = null): List<Any>
    
    fun searchBatches(projectId: String, query: String): Flow<List<InventoryBatch>>
    fun searchLots(projectId: String, query: String): Flow<List<InventoryLot>>
}
