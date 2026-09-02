package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.analytics.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository for Inventory Analytics & Governance (Module 07 Step 10).
 */
interface InventoryAnalyticsRepository {
    
    suspend fun getAnalyticsSummary(
        projectId: String, 
        period: InventoryAnalyticsPeriod,
        callerRole: UserRole? = null
    ): DomainResult<InventoryAnalyticsSummary>

    suspend fun getStockTrends(
        projectId: String, 
        period: InventoryAnalyticsPeriod,
        callerRole: UserRole? = null
    ): DomainResult<List<InventoryAnalyticsTrendPoint>>

    fun observeExceptions(projectId: String): Flow<List<InventoryException>>

    /**
     * Scans for governance issues like negative stock or reconciliation mismatches.
     * Atomic operation enforced via Mutex.
     */
    suspend fun executeGovernanceCheck(projectId: String, actorId: String): DomainResult<Unit>

    fun observeActivityEvents(projectId: String): Flow<List<InventoryAnalyticsActivityEvent>>
}
