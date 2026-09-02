package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.InventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.InventoryWarehouseDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationActivityType
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryLocationRepository
import com.sucharu.sucharupro.domain.validation.InventoryLocationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe production-grade repository implementation for Storage Location Management (Module 07 Step 02).
 */
class InventoryLocationRepositoryImpl(
    private val locationDataSource: InventoryLocationDataSource,
    private val warehouseDataSource: InventoryWarehouseDataSource? = null
) : InventoryLocationRepository {

    private val repositoryMutex = Mutex()

    // ==========================================
    // 1. Queries
    // ==========================================

    override fun observeLocations(projectId: String): Flow<List<InventoryLocation>> {
        return locationDataSource.observeLocations().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeLocationsByWarehouse(
        warehouseId: String,
        projectId: String
    ): Flow<List<InventoryLocation>> {
        return locationDataSource.observeLocations().map { list ->
            list.filter { it.projectId == projectId && it.warehouseId == warehouseId }
        }
    }

    override fun observeActiveLocations(
        warehouseId: String,
        projectId: String
    ): Flow<List<InventoryLocation>> {
        return locationDataSource.observeLocations().map { list ->
            list.filter { it.projectId == projectId && it.warehouseId == warehouseId && it.status == InventoryLocationStatus.ACTIVE }
        }
    }

    override fun observeChildLocations(
        parentLocationId: String,
        projectId: String
    ): Flow<List<InventoryLocation>> {
        return locationDataSource.observeLocations().map { list ->
            list.filter { it.projectId == projectId && it.parentLocationId == parentLocationId }
        }
    }

    override suspend fun getLocationById(
        locationId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryLocation> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbacResult = InventoryLocationValidator.validateLocationViewPermission(callerRole)
            if (rbacResult is DomainResult.Error) return rbacResult
        }
        val locations = locationDataSource.observeLocations().first()
        val location = locations.find { it.id == locationId }
            ?: return DomainResult.Error(message = "Location with ID '$locationId' not found.")
        DomainResult.Success(location)
    }

    override suspend fun getLocationByCode(
        code: String,
        warehouseId: String,
        projectId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryLocation> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbacResult = InventoryLocationValidator.validateLocationViewPermission(callerRole)
            if (rbacResult is DomainResult.Error) return rbacResult
        }
        val normalized = code.trim().uppercase()
        val locations = locationDataSource.observeLocations().first()
        val location = locations.find {
            it.projectId == projectId && it.warehouseId == warehouseId && it.normalizedCode == normalized
        } ?: return DomainResult.Error(message = "Location with code '$code' not found in warehouse '$warehouseId'.")
        DomainResult.Success(location)
    }

    // ==========================================
    // 2. Mutations
    // ==========================================

    override suspend fun createLocation(
        location: InventoryLocation,
        callerRole: UserRole?
    ): DomainResult<InventoryLocation> = repositoryMutex.withLock {
        val rbacResult = InventoryLocationValidator.validateLocationAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val valResult = InventoryLocationValidator.validateLocation(location)
        if (valResult is DomainResult.Error) return valResult

        if (warehouseDataSource != null) {
            val warehouses = warehouseDataSource.observeWarehouses().first()
            val targetWh = warehouses.find { it.id == location.warehouseId }
            val whResult = InventoryLocationValidator.validateWarehouseAssociation(location, targetWh)
            if (whResult is DomainResult.Error) return whResult
        }

        val allLocations = locationDataSource.observeLocations().first()

        val uniqueness = InventoryLocationValidator.validateLocationCodeUniqueness(
            code = location.code,
            locationId = location.id,
            warehouseId = location.warehouseId,
            existingLocations = allLocations
        )
        if (uniqueness is DomainResult.Error) return uniqueness

        val hierarchyResult = InventoryLocationValidator.validateParentHierarchy(
            locationId = location.id,
            parentLocationId = location.parentLocationId,
            warehouseId = location.warehouseId,
            projectId = location.projectId,
            allLocations = allLocations
        )
        if (hierarchyResult is DomainResult.Error) return hierarchyResult

        val insertResult = locationDataSource.insertLocation(location)
        if (insertResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = location.projectId,
                warehouseId = location.warehouseId,
                locationId = location.id,
                eventType = InventoryLocationActivityType.LOCATION_CREATED,
                actorId = location.createdBy,
                description = "Created location '${location.name}' (Code: ${location.code}, Warehouse: ${location.warehouseId})",
                timestamp = location.createdAt
            )
        }
        return insertResult
    }

    override suspend fun updateLocationMetadata(
        locationId: String,
        name: String,
        description: String?,
        type: InventoryLocationType,
        capacity: Double?,
        capacityUnit: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryLocation> = repositoryMutex.withLock {
        val rbacResult = InventoryLocationValidator.validateLocationAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val locations = locationDataSource.observeLocations().first()
        val current = locations.find { it.id == locationId }
            ?: return DomainResult.Error(message = "Location with ID '$locationId' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Cannot update metadata of an ARCHIVED location (ID: '$locationId').")
        }

        val updated = current.copy(
            name = name,
            description = description,
            type = type,
            capacity = capacity,
            capacityUnit = capacityUnit,
            notes = notes,
            updatedAt = timestamp
        )

        val valResult = InventoryLocationValidator.validateLocation(updated)
        if (valResult is DomainResult.Error) return valResult

        val updateResult = locationDataSource.updateLocation(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                warehouseId = updated.warehouseId,
                locationId = updated.id,
                eventType = InventoryLocationActivityType.LOCATION_UPDATED,
                actorId = "SYSTEM",
                description = "Updated metadata for location '${updated.name}' (Code: ${updated.code})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun changeParentLocation(
        locationId: String,
        newParentLocationId: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryLocation> = repositoryMutex.withLock {
        val rbacResult = InventoryLocationValidator.validateLocationAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val locations = locationDataSource.observeLocations().first()
        val current = locations.find { it.id == locationId }
            ?: return DomainResult.Error(message = "Location with ID '$locationId' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Cannot modify parent of an ARCHIVED location (ID: '$locationId').")
        }

        val hierarchyResult = InventoryLocationValidator.validateParentHierarchy(
            locationId = current.id,
            parentLocationId = newParentLocationId,
            warehouseId = current.warehouseId,
            projectId = current.projectId,
            allLocations = locations
        )
        if (hierarchyResult is DomainResult.Error) return hierarchyResult

        val updated = current.copy(
            parentLocationId = newParentLocationId,
            updatedAt = timestamp
        )

        val updateResult = locationDataSource.updateLocation(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                warehouseId = updated.warehouseId,
                locationId = updated.id,
                eventType = InventoryLocationActivityType.LOCATION_PARENT_CHANGED,
                actorId = "SYSTEM",
                description = "Changed parent of location '${updated.name}' to '${newParentLocationId ?: "ROOT"}'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun activateLocation(
        locationId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryLocation> = repositoryMutex.withLock {
        val rbacResult = InventoryLocationValidator.validateLocationAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val locations = locationDataSource.observeLocations().first()
        val current = locations.find { it.id == locationId }
            ?: return DomainResult.Error(message = "Location with ID '$locationId' not found.")

        val transition = InventoryLocationValidator.validateLocationTransition(current.status, InventoryLocationStatus.ACTIVE)
        if (transition is DomainResult.Error) return transition

        if (current.status == InventoryLocationStatus.ACTIVE) return DomainResult.Success(current)

        val updated = current.copy(
            status = InventoryLocationStatus.ACTIVE,
            updatedAt = timestamp
        )

        val updateResult = locationDataSource.updateLocation(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                warehouseId = updated.warehouseId,
                locationId = updated.id,
                eventType = InventoryLocationActivityType.LOCATION_ACTIVATED,
                actorId = actorId,
                description = "Activated location '${updated.name}' (Code: ${updated.code})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun deactivateLocation(
        locationId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryLocation> = repositoryMutex.withLock {
        val rbacResult = InventoryLocationValidator.validateLocationAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val locations = locationDataSource.observeLocations().first()
        val current = locations.find { it.id == locationId }
            ?: return DomainResult.Error(message = "Location with ID '$locationId' not found.")

        val transition = InventoryLocationValidator.validateLocationTransition(current.status, InventoryLocationStatus.INACTIVE)
        if (transition is DomainResult.Error) return transition

        if (current.status == InventoryLocationStatus.INACTIVE) return DomainResult.Success(current)

        val updated = current.copy(
            status = InventoryLocationStatus.INACTIVE,
            updatedAt = timestamp
        )

        val updateResult = locationDataSource.updateLocation(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                warehouseId = updated.warehouseId,
                locationId = updated.id,
                eventType = InventoryLocationActivityType.LOCATION_DEACTIVATED,
                actorId = actorId,
                description = "Deactivated location '${updated.name}' (Code: ${updated.code})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun archiveLocation(
        locationId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryLocation> = repositoryMutex.withLock {
        val rbacResult = InventoryLocationValidator.validateLocationAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val locations = locationDataSource.observeLocations().first()
        val current = locations.find { it.id == locationId }
            ?: return DomainResult.Error(message = "Location with ID '$locationId' not found.")

        val transition = InventoryLocationValidator.validateLocationTransition(current.status, InventoryLocationStatus.ARCHIVED)
        if (transition is DomainResult.Error) return transition

        if (current.status == InventoryLocationStatus.ARCHIVED) return DomainResult.Success(current)

        val updated = current.copy(
            status = InventoryLocationStatus.ARCHIVED,
            archivedAt = timestamp,
            updatedAt = timestamp
        )

        val updateResult = locationDataSource.updateLocation(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                warehouseId = updated.warehouseId,
                locationId = updated.id,
                eventType = InventoryLocationActivityType.LOCATION_ARCHIVED,
                actorId = actorId,
                description = "Archived location '${updated.name}' (Code: ${updated.code})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    // ==========================================
    // 3. Audit Trail
    // ==========================================

    override fun observeActivityEvents(projectId: String): Flow<List<InventoryLocationActivityEvent>> {
        return locationDataSource.observeActivityEvents().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    private suspend fun recordActivityInternal(
        projectId: String,
        warehouseId: String,
        locationId: String,
        eventType: InventoryLocationActivityType,
        actorId: String,
        actorName: String? = null,
        description: String,
        timestamp: String
    ) {
        val event = InventoryLocationActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            warehouseId = warehouseId,
            locationId = locationId,
            eventType = eventType,
            actorId = actorId,
            actorName = actorName,
            description = description,
            timestamp = timestamp
        )
        locationDataSource.recordActivity(event)
    }
}
