package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlert
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryStockLevelPolicy
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Reorder Alert & Stock Level Management data operations (Module 07 Step 08).
 */
interface InventoryReorderDataSource {

    // Stock Level Policies
    fun observePolicies(): Flow<List<InventoryStockLevelPolicy>>
    suspend fun insertPolicy(policy: InventoryStockLevelPolicy): DomainResult<InventoryStockLevelPolicy>
    suspend fun updatePolicy(policy: InventoryStockLevelPolicy): DomainResult<InventoryStockLevelPolicy>
    suspend fun deletePolicy(policyId: String): DomainResult<Unit>

    // Reorder Alerts
    fun observeAlerts(): Flow<List<InventoryReorderAlert>>
    suspend fun insertAlert(alert: InventoryReorderAlert): DomainResult<InventoryReorderAlert>
    suspend fun updateAlert(alert: InventoryReorderAlert): DomainResult<InventoryReorderAlert>
    suspend fun deleteAlert(alertId: String): DomainResult<Unit>

    // Activity Logs
    fun observeAuditEvents(): Flow<List<InventoryReorderActivityEvent>>
    suspend fun recordAuditEvent(event: InventoryReorderActivityEvent): DomainResult<InventoryReorderActivityEvent>
}
