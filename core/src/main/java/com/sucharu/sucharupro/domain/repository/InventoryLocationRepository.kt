package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for storage location management and hierarchy tree operations (Module 07 Step 02).
 */
interface InventoryLocationRepository {

    // Queries
    fun observeLocations(projectId: String): Flow<List<InventoryLocation>>
    fun observeLocationsByWarehouse(warehouseId: String, projectId: String): Flow<List<InventoryLocation>>
    fun observeActiveLocations(warehouseId: String, projectId: String): Flow<List<InventoryLocation>>
    fun observeChildLocations(parentLocationId: String, projectId: String): Flow<List<InventoryLocation>>
    suspend fun getLocationById(locationId: String, callerRole: UserRole? = null): DomainResult<InventoryLocation>
    suspend fun getLocationByCode(code: String, warehouseId: String, projectId: String, callerRole: UserRole? = null): DomainResult<InventoryLocation>

    // Mutations
    suspend fun createLocation(
        location: InventoryLocation,
        callerRole: UserRole? = null
    ): DomainResult<InventoryLocation>

    suspend fun updateLocationMetadata(
        locationId: String,
        name: String,
        description: String?,
        type: InventoryLocationType,
        capacity: Double?,
        capacityUnit: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryLocation>

    suspend fun changeParentLocation(
        locationId: String,
        newParentLocationId: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryLocation>

    suspend fun activateLocation(
        locationId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryLocation>

    suspend fun deactivateLocation(
        locationId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryLocation>

    suspend fun archiveLocation(
        locationId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryLocation>

    // Audit Trail
    fun observeActivityEvents(projectId: String): Flow<List<InventoryLocationActivityEvent>>
}
