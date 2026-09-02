package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryException
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Inventory Analytics and Governance data operations (Module 07 Step 10).
 */
interface InventoryAnalyticsDataSource {
    fun observeExceptions(projectId: String): Flow<List<InventoryException>>
    suspend fun getExceptions(projectId: String): List<InventoryException>
    suspend fun upsertExceptions(exceptions: List<InventoryException>): DomainResult<Unit>
    suspend fun deleteExceptions(exceptionIds: List<String>): DomainResult<Unit>

    fun observeActivityEvents(projectId: String): Flow<List<InventoryAnalyticsActivityEvent>>
    suspend fun recordActivityEvent(event: InventoryAnalyticsActivityEvent): DomainResult<InventoryAnalyticsActivityEvent>
}
