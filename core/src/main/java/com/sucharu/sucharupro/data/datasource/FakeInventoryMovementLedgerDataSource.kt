package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryLedgerActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeInventoryMovementLedgerDataSource : InventoryMovementLedgerDataSource {
    private val entries = MutableStateFlow<Map<String, InventoryMovementLedgerEntry>>(emptyMap())
    private val events = MutableStateFlow<List<InventoryLedgerActivityEvent>>(emptyList())

    override fun observeEntries(projectId: String): Flow<List<InventoryMovementLedgerEntry>> {
        return entries.map { it.values.filter { entry -> entry.projectId == projectId } }
    }

    override suspend fun getEntries(projectId: String): List<InventoryMovementLedgerEntry> {
        return entries.value.values.filter { it.projectId == projectId }
    }

    override suspend fun insertEntries(entries: List<InventoryMovementLedgerEntry>): DomainResult<List<InventoryMovementLedgerEntry>> {
        this.entries.update { current ->
            current + entries.associateBy { it.ledgerEntryId }
        }
        return DomainResult.Success(entries)
    }

    override fun observeActivityEvents(projectId: String): Flow<List<InventoryLedgerActivityEvent>> {
        return events.map { it.filter { event -> event.projectId == projectId } }
    }

    override suspend fun recordActivityEvent(event: InventoryLedgerActivityEvent): DomainResult<InventoryLedgerActivityEvent> {
        events.update { it + event }
        return DomainResult.Success(event)
    }
}
