package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryLedgerActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository for querying normalized movement ledger and calculating inventory valuation.
 */
interface InventoryMovementLedgerRepository {
    fun observeEntries(projectId: String): Flow<List<InventoryMovementLedgerEntry>>
    suspend fun getEntries(projectId: String): DomainResult<List<InventoryMovementLedgerEntry>>
    
    /**
     * Calculates the current balance for a specific product and location.
     */
    suspend fun getBalance(projectId: String, productId: String, locationId: String): Double

    /**
     * Idempotent operation to synchronize ledger entries from all source movement records.
     */
    suspend fun synchronizeLedger(projectId: String): DomainResult<Unit>
    
    fun observeActivityEvents(projectId: String): Flow<List<InventoryLedgerActivityEvent>>
}
