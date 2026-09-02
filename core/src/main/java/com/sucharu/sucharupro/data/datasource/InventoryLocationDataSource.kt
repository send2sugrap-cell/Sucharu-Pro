package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationActivityEvent
import kotlinx.coroutines.flow.Flow

/**
 * Reactive data source contract for storage location management (Module 07 Step 02).
 */
interface InventoryLocationDataSource {
    fun observeLocations(): Flow<List<InventoryLocation>>
    suspend fun insertLocation(location: InventoryLocation): DomainResult<InventoryLocation>
    suspend fun updateLocation(location: InventoryLocation): DomainResult<InventoryLocation>

    fun observeActivityEvents(): Flow<List<InventoryLocationActivityEvent>>
    suspend fun recordActivity(event: InventoryLocationActivityEvent): DomainResult<Unit>
}
