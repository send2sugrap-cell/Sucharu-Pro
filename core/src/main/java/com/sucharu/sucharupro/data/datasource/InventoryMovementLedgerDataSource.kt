package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryLedgerActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Inventory Movement Ledger and Valuation data operations (Module 07 Step 09).
 */
interface InventoryMovementLedgerDataSource {
    fun observeEntries(projectId: String): Flow<List<InventoryMovementLedgerEntry>>
    suspend fun getEntries(projectId: String): List<InventoryMovementLedgerEntry>
    suspend fun insertEntries(entries: List<InventoryMovementLedgerEntry>): DomainResult<List<InventoryMovementLedgerEntry>>

    fun observeActivityEvents(projectId: String): Flow<List<InventoryLedgerActivityEvent>>
    suspend fun recordActivityEvent(event: InventoryLedgerActivityEvent): DomainResult<InventoryLedgerActivityEvent>
}
