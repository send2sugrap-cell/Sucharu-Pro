package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for physical warehouse management (Module 07 Step 02).
 */
interface InventoryWarehouseRepository {

    // Queries
    fun observeWarehouses(projectId: String): Flow<List<InventoryWarehouse>>
    fun observeActiveWarehouses(projectId: String): Flow<List<InventoryWarehouse>>
    suspend fun getWarehouseById(warehouseId: String, callerRole: UserRole? = null): DomainResult<InventoryWarehouse>
    suspend fun getWarehouseByCode(code: String, projectId: String, callerRole: UserRole? = null): DomainResult<InventoryWarehouse>

    // Mutations
    suspend fun createWarehouse(
        warehouse: InventoryWarehouse,
        callerRole: UserRole? = null
    ): DomainResult<InventoryWarehouse>

    suspend fun updateWarehouseMetadata(
        warehouseId: String,
        name: String,
        description: String?,
        type: InventoryWarehouseType,
        address: String?,
        contactPerson: String?,
        contactPhone: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryWarehouse>

    suspend fun activateWarehouse(
        warehouseId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryWarehouse>

    suspend fun deactivateWarehouse(
        warehouseId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryWarehouse>

    suspend fun archiveWarehouse(
        warehouseId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryWarehouse>

    // Audit Trail
    fun observeActivityEvents(projectId: String): Flow<List<InventoryWarehouseActivityEvent>>
}
