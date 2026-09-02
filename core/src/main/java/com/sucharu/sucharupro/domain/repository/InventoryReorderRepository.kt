package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlert
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryStockLevelPolicy
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository for Reorder Alert & Stock Level Management (Module 07 Step 08).
 */
interface InventoryReorderRepository {

    // Policies
    fun observePolicies(projectId: String): Flow<List<InventoryStockLevelPolicy>>
    suspend fun getPolicy(policyId: String, callerRole: UserRole? = null): DomainResult<InventoryStockLevelPolicy>
    suspend fun createPolicy(policy: InventoryStockLevelPolicy, callerRole: UserRole? = null): DomainResult<InventoryStockLevelPolicy>
    suspend fun updatePolicy(policy: InventoryStockLevelPolicy, callerRole: UserRole? = null): DomainResult<InventoryStockLevelPolicy>
    suspend fun deletePolicy(policyId: String, callerRole: UserRole? = null): DomainResult<Unit>

    // Alerts
    fun observeAlerts(projectId: String): Flow<List<InventoryReorderAlert>>
    suspend fun getAlert(alertId: String, callerRole: UserRole? = null): DomainResult<InventoryReorderAlert>
    suspend fun acknowledgeAlert(alertId: String, userId: String, callerRole: UserRole? = null): DomainResult<InventoryReorderAlert>
    suspend fun resolveAlert(alertId: String, userId: String, callerRole: UserRole? = null): DomainResult<InventoryReorderAlert>

    // Stock Level Evaluation
    suspend fun evaluatePolicies(projectId: String): DomainResult<Unit>
}
