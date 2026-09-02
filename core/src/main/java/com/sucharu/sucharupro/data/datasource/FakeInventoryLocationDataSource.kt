package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationActivityEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [InventoryLocationDataSource] (Module 07 Step 02).
 */
class FakeInventoryLocationDataSource : InventoryLocationDataSource {

    private val mutex = Mutex()

    private val locationsFlow = MutableStateFlow<List<InventoryLocation>>(emptyList())
    private val eventsFlow = MutableStateFlow<List<InventoryLocationActivityEvent>>(emptyList())

    override fun observeLocations(): Flow<List<InventoryLocation>> = locationsFlow.asStateFlow()

    override suspend fun insertLocation(location: InventoryLocation): DomainResult<InventoryLocation> = mutex.withLock {
        val current = locationsFlow.value.toMutableList()
        if (current.any { it.id == location.id }) {
            return DomainResult.Error(message = "Location with ID '${location.id}' already exists.")
        }
        if (current.any { it.warehouseId == location.warehouseId && it.normalizedCode == location.normalizedCode }) {
            return DomainResult.Error(
                message = "Location with code '${location.code}' already exists in warehouse '${location.warehouseId}'."
            )
        }
        current.add(location)
        locationsFlow.value = current
        DomainResult.Success(location)
    }

    override suspend fun updateLocation(location: InventoryLocation): DomainResult<InventoryLocation> = mutex.withLock {
        val current = locationsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == location.id }
        if (index == -1) {
            return DomainResult.Error(message = "Location with ID '${location.id}' not found.")
        }
        if (current.any { it.warehouseId == location.warehouseId && it.normalizedCode == location.normalizedCode && it.id != location.id }) {
            return DomainResult.Error(
                message = "Location with code '${location.code}' already exists on another location in warehouse '${location.warehouseId}'."
            )
        }
        current[index] = location
        locationsFlow.value = current
        DomainResult.Success(location)
    }

    override fun observeActivityEvents(): Flow<List<InventoryLocationActivityEvent>> = eventsFlow.asStateFlow()

    override suspend fun recordActivity(event: InventoryLocationActivityEvent): DomainResult<Unit> = mutex.withLock {
        val current = eventsFlow.value.toMutableList()
        current.add(0, event)
        eventsFlow.value = current
        DomainResult.Success(Unit)
    }
}
