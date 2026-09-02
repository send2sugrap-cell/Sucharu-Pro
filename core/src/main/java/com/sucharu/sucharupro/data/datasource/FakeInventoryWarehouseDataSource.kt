package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseActivityEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [InventoryWarehouseDataSource] (Module 07 Step 02).
 */
class FakeInventoryWarehouseDataSource : InventoryWarehouseDataSource {

    private val mutex = Mutex()

    private val warehousesFlow = MutableStateFlow<List<InventoryWarehouse>>(emptyList())
    private val eventsFlow = MutableStateFlow<List<InventoryWarehouseActivityEvent>>(emptyList())

    override fun observeWarehouses(): Flow<List<InventoryWarehouse>> = warehousesFlow.asStateFlow()

    override suspend fun insertWarehouse(warehouse: InventoryWarehouse): DomainResult<InventoryWarehouse> = mutex.withLock {
        val current = warehousesFlow.value.toMutableList()
        if (current.any { it.id == warehouse.id }) {
            return DomainResult.Error(message = "Warehouse with ID '${warehouse.id}' already exists.")
        }
        if (current.any { it.projectId == warehouse.projectId && it.normalizedCode == warehouse.normalizedCode }) {
            return DomainResult.Error(
                message = "Warehouse with code '${warehouse.code}' already exists in project '${warehouse.projectId}'."
            )
        }
        current.add(warehouse)
        warehousesFlow.value = current
        DomainResult.Success(warehouse)
    }

    override suspend fun updateWarehouse(warehouse: InventoryWarehouse): DomainResult<InventoryWarehouse> = mutex.withLock {
        val current = warehousesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == warehouse.id }
        if (index == -1) {
            return DomainResult.Error(message = "Warehouse with ID '${warehouse.id}' not found.")
        }
        if (current.any { it.projectId == warehouse.projectId && it.normalizedCode == warehouse.normalizedCode && it.id != warehouse.id }) {
            return DomainResult.Error(
                message = "Warehouse with code '${warehouse.code}' already exists on another warehouse in project '${warehouse.projectId}'."
            )
        }
        current[index] = warehouse
        warehousesFlow.value = current
        DomainResult.Success(warehouse)
    }

    override fun observeActivityEvents(): Flow<List<InventoryWarehouseActivityEvent>> = eventsFlow.asStateFlow()

    override suspend fun recordActivity(event: InventoryWarehouseActivityEvent): DomainResult<Unit> = mutex.withLock {
        val current = eventsFlow.value.toMutableList()
        current.add(0, event)
        eventsFlow.value = current
        DomainResult.Success(Unit)
    }
}
