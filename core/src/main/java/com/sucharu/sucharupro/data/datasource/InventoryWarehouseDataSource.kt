package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseActivityEvent
import kotlinx.coroutines.flow.Flow

/**
 * Reactive data source contract for physical warehouse management (Module 07 Step 02).
 */
interface InventoryWarehouseDataSource {
    fun observeWarehouses(): Flow<List<InventoryWarehouse>>
    suspend fun insertWarehouse(warehouse: InventoryWarehouse): DomainResult<InventoryWarehouse>
    suspend fun updateWarehouse(warehouse: InventoryWarehouse): DomainResult<InventoryWarehouse>

    fun observeActivityEvents(): Flow<List<InventoryWarehouseActivityEvent>>
    suspend fun recordActivity(event: InventoryWarehouseActivityEvent): DomainResult<Unit>
}
