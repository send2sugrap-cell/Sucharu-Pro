package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.InventoryWarehouseDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseActivityType
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryWarehouseRepository
import com.sucharu.sucharupro.domain.validation.InventoryWarehouseValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe production-grade repository implementation for Warehouse Management (Module 07 Step 02).
 */
class InventoryWarehouseRepositoryImpl(
    private val dataSource: InventoryWarehouseDataSource
) : InventoryWarehouseRepository {

    private val repositoryMutex = Mutex()

    // ==========================================
    // 1. Queries
    // ==========================================

    override fun observeWarehouses(projectId: String): Flow<List<InventoryWarehouse>> {
        return dataSource.observeWarehouses().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeActiveWarehouses(projectId: String): Flow<List<InventoryWarehouse>> {
        return dataSource.observeWarehouses().map { list ->
            list.filter { it.projectId == projectId && it.status == InventoryWarehouseStatus.ACTIVE }
        }
    }

    override suspend fun getWarehouseById(
        warehouseId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryWarehouse> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbacResult = InventoryWarehouseValidator.validateWarehouseViewPermission(callerRole)
            if (rbacResult is DomainResult.Error) return rbacResult
        }
        val warehouses = dataSource.observeWarehouses().first()
        val warehouse = warehouses.find { it.id == warehouseId }
            ?: return DomainResult.Error(message = "Warehouse with ID '$warehouseId' not found.")
        DomainResult.Success(warehouse)
    }

    override suspend fun getWarehouseByCode(
        code: String,
        projectId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryWarehouse> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbacResult = InventoryWarehouseValidator.validateWarehouseViewPermission(callerRole)
            if (rbacResult is DomainResult.Error) return rbacResult
        }
        val normalized = code.trim().uppercase()
        val warehouses = dataSource.observeWarehouses().first()
        val warehouse = warehouses.find { it.projectId == projectId && it.normalizedCode == normalized }
            ?: return DomainResult.Error(message = "Warehouse with code '$code' not found in project '$projectId'.")
        DomainResult.Success(warehouse)
    }

    // ==========================================
    // 2. Mutations
    // ==========================================

    override suspend fun createWarehouse(
        warehouse: InventoryWarehouse,
        callerRole: UserRole?
    ): DomainResult<InventoryWarehouse> = repositoryMutex.withLock {
        val rbacResult = InventoryWarehouseValidator.validateWarehouseAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val valResult = InventoryWarehouseValidator.validateWarehouse(warehouse)
        if (valResult is DomainResult.Error) return valResult

        val existing = dataSource.observeWarehouses().first()
        val uniqueness = InventoryWarehouseValidator.validateWarehouseCodeUniqueness(
            code = warehouse.code,
            warehouseId = warehouse.id,
            projectId = warehouse.projectId,
            existingWarehouses = existing
        )
        if (uniqueness is DomainResult.Error) return uniqueness

        val insertResult = dataSource.insertWarehouse(warehouse)
        if (insertResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = warehouse.projectId,
                warehouseId = warehouse.id,
                eventType = InventoryWarehouseActivityType.WAREHOUSE_CREATED,
                actorId = warehouse.createdBy,
                description = "Created warehouse '${warehouse.name}' (Code: ${warehouse.code})",
                timestamp = warehouse.createdAt
            )
        }
        return insertResult
    }

    override suspend fun updateWarehouseMetadata(
        warehouseId: String,
        name: String,
        description: String?,
        type: InventoryWarehouseType,
        address: String?,
        contactPerson: String?,
        contactPhone: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryWarehouse> = repositoryMutex.withLock {
        val rbacResult = InventoryWarehouseValidator.validateWarehouseAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val warehouses = dataSource.observeWarehouses().first()
        val current = warehouses.find { it.id == warehouseId }
            ?: return DomainResult.Error(message = "Warehouse with ID '$warehouseId' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Cannot update metadata of an ARCHIVED warehouse (ID: '$warehouseId').")
        }

        val updated = current.copy(
            name = name,
            description = description,
            type = type,
            address = address,
            contactPerson = contactPerson,
            contactPhone = contactPhone,
            notes = notes,
            updatedAt = timestamp
        )

        val valResult = InventoryWarehouseValidator.validateWarehouse(updated)
        if (valResult is DomainResult.Error) return valResult

        val updateResult = dataSource.updateWarehouse(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                warehouseId = updated.id,
                eventType = InventoryWarehouseActivityType.WAREHOUSE_UPDATED,
                actorId = "SYSTEM",
                description = "Updated metadata for warehouse '${updated.name}' (Code: ${updated.code})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun activateWarehouse(
        warehouseId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryWarehouse> = repositoryMutex.withLock {
        val rbacResult = InventoryWarehouseValidator.validateWarehouseAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val warehouses = dataSource.observeWarehouses().first()
        val current = warehouses.find { it.id == warehouseId }
            ?: return DomainResult.Error(message = "Warehouse with ID '$warehouseId' not found.")

        val transition = InventoryWarehouseValidator.validateWarehouseTransition(current.status, InventoryWarehouseStatus.ACTIVE)
        if (transition is DomainResult.Error) return transition

        if (current.status == InventoryWarehouseStatus.ACTIVE) return DomainResult.Success(current)

        val updated = current.copy(
            status = InventoryWarehouseStatus.ACTIVE,
            updatedAt = timestamp
        )

        val updateResult = dataSource.updateWarehouse(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                warehouseId = updated.id,
                eventType = InventoryWarehouseActivityType.WAREHOUSE_ACTIVATED,
                actorId = actorId,
                description = "Activated warehouse '${updated.name}' (Code: ${updated.code})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun deactivateWarehouse(
        warehouseId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryWarehouse> = repositoryMutex.withLock {
        val rbacResult = InventoryWarehouseValidator.validateWarehouseAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val warehouses = dataSource.observeWarehouses().first()
        val current = warehouses.find { it.id == warehouseId }
            ?: return DomainResult.Error(message = "Warehouse with ID '$warehouseId' not found.")

        val transition = InventoryWarehouseValidator.validateWarehouseTransition(current.status, InventoryWarehouseStatus.INACTIVE)
        if (transition is DomainResult.Error) return transition

        if (current.status == InventoryWarehouseStatus.INACTIVE) return DomainResult.Success(current)

        val updated = current.copy(
            status = InventoryWarehouseStatus.INACTIVE,
            updatedAt = timestamp
        )

        val updateResult = dataSource.updateWarehouse(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                warehouseId = updated.id,
                eventType = InventoryWarehouseActivityType.WAREHOUSE_DEACTIVATED,
                actorId = actorId,
                description = "Deactivated warehouse '${updated.name}' (Code: ${updated.code})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun archiveWarehouse(
        warehouseId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryWarehouse> = repositoryMutex.withLock {
        val rbacResult = InventoryWarehouseValidator.validateWarehouseAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val warehouses = dataSource.observeWarehouses().first()
        val current = warehouses.find { it.id == warehouseId }
            ?: return DomainResult.Error(message = "Warehouse with ID '$warehouseId' not found.")

        val transition = InventoryWarehouseValidator.validateWarehouseTransition(current.status, InventoryWarehouseStatus.ARCHIVED)
        if (transition is DomainResult.Error) return transition

        if (current.status == InventoryWarehouseStatus.ARCHIVED) return DomainResult.Success(current)

        val updated = current.copy(
            status = InventoryWarehouseStatus.ARCHIVED,
            archivedAt = timestamp,
            updatedAt = timestamp
        )

        val updateResult = dataSource.updateWarehouse(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                projectId = updated.projectId,
                warehouseId = updated.id,
                eventType = InventoryWarehouseActivityType.WAREHOUSE_ARCHIVED,
                actorId = actorId,
                description = "Archived warehouse '${updated.name}' (Code: ${updated.code})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    // ==========================================
    // 3. Audit Trail
    // ==========================================

    override fun observeActivityEvents(projectId: String): Flow<List<InventoryWarehouseActivityEvent>> {
        return dataSource.observeActivityEvents().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    private suspend fun recordActivityInternal(
        projectId: String,
        warehouseId: String,
        eventType: InventoryWarehouseActivityType,
        actorId: String,
        actorName: String? = null,
        description: String,
        timestamp: String
    ) {
        val event = InventoryWarehouseActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            warehouseId = warehouseId,
            eventType = eventType,
            actorId = actorId,
            actorName = actorName,
            description = description,
            timestamp = timestamp
        )
        dataSource.recordActivity(event)
    }
}
