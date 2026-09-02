package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Thread-safe in-memory implementation of InventoryAnalyticsDataSource (Module 07 Step 10).
 */
class FakeInventoryAnalyticsDataSource : InventoryAnalyticsDataSource {
    private val exceptions = MutableStateFlow<Map<String, InventoryException>>(emptyMap())
    private val events = MutableStateFlow<List<InventoryAnalyticsActivityEvent>>(emptyList())

    override fun observeExceptions(projectId: String): Flow<List<InventoryException>> {
        return exceptions.map { it.values.filter { ex -> ex.projectId == projectId } }
    }

    override suspend fun getExceptions(projectId: String): List<InventoryException> {
        return exceptions.value.values.filter { it.projectId == projectId }
    }

    override suspend fun upsertExceptions(exceptions: List<InventoryException>): DomainResult<Unit> {
        this.exceptions.update { current ->
            current + exceptions.associateBy { it.exceptionId }
        }
        return DomainResult.Success(Unit)
    }

    override suspend fun deleteExceptions(exceptionIds: List<String>): DomainResult<Unit> {
        this.exceptions.update { current ->
            current.filterKeys { it !in exceptionIds }
        }
        return DomainResult.Success(Unit)
    }

    override fun observeActivityEvents(projectId: String): Flow<List<InventoryAnalyticsActivityEvent>> {
        return events.map { it.filter { event -> event.projectId == projectId } }
    }

    override suspend fun recordActivityEvent(event: InventoryAnalyticsActivityEvent): DomainResult<InventoryAnalyticsActivityEvent> {
        events.update { it + event }
        return DomainResult.Success(event)
    }
}
