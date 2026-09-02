package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.dashboard.DashboardInventoryAlert
import com.sucharu.sucharupro.domain.model.dashboard.DashboardJobSummary
import com.sucharu.sucharupro.domain.model.dashboard.DashboardKpis
import com.sucharu.sucharupro.domain.model.dashboard.DashboardOperationalAlerts
import com.sucharu.sucharupro.domain.model.dashboard.DashboardSummary
import com.sucharu.sucharupro.domain.model.dashboard.StageCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository interface contract for the Sucharu Pro operational dashboard.
 *
 * Provides two levels of access:
 *  1. [getDashboardSummary] — full aggregate (all-in-one, for initial load)
 *  2. Granular query methods — for targeted refresh of individual dashboard sections
 *
 * All methods return [Flow] for reactive UI updates.
 * Implementations may use local cache, remote API, or in-memory fake data.
 *
 * Architecture boundary:
 *  - This interface is in the DOMAIN layer — no Android/Compose imports allowed here.
 *  - Implementations live in the DATA layer ([FakeDashboardRepository], future real repository).
 *  - ViewModels depend on this interface, never on implementations directly.
 */
interface DashboardRepository {

    // =========================================================================
    // Full Aggregate — Primary Dashboard Load
    // =========================================================================

    /**
     * Reactive stream of the complete dashboard summary.
     * Use for initial dashboard load or full-screen refresh.
     * Contains: KPIs, stage counts, payment breakdown, workload, recent orders,
     * inventory alerts, and operational alert counts.
     */
    fun getDashboardSummary(): Flow<DashboardSummary>

    /**
     * Triggers a server/data refresh of dashboard metrics.
     * Returns [Result.success] when complete or [Result.failure] with the error.
     */
    suspend fun refreshDashboardSummary(): Result<Unit>

    // =========================================================================
    // Granular Queries — Section-Level Data Access
    //
    // Default implementations derive from [getDashboardSummary] so that
    // FakeDashboardRepository does not need to override each one individually.
    // Real repository implementations may override these for optimized queries.
    // =========================================================================

    /**
     * Reactive stream of executive KPI metrics only.
     * Used for targeted KPI card refresh without reloading the full dashboard.
     */
    fun getDashboardKpis(): Flow<DashboardKpis> =
        getDashboardSummary().map { it.kpis }

    /**
     * Reactive stream of production stage counts.
     * Uses canonical [com.sucharu.sucharupro.domain.model.production.ProductionStageType].
     * Used for targeted pipeline section refresh.
     */
    fun getStageCounts(): Flow<List<StageCount>> =
        getDashboardSummary().map { it.stageCounts }

    /**
     * Reactive stream of operational alert counts.
     * Used for badge/counter updates (pending approval, QC, delivery, payments, etc.)
     * without reloading the full dashboard.
     */
    fun getOperationalAlerts(): Flow<DashboardOperationalAlerts> =
        getDashboardSummary().map { it.operationalAlerts }

    /**
     * Reactive stream of recent jobs/orders for the dashboard list.
     *
     * @param limit Maximum number of jobs to return. Defaults to 10.
     *   Use a lower value for dashboard preview cards.
     *   Full list is accessed from the Orders module.
     */
    fun getRecentOrders(limit: Int = 10): Flow<List<DashboardJobSummary>> =
        getDashboardSummary().map { it.recentOrders.take(limit) }

    /**
     * Reactive stream of finished product stock alert items.
     * Returns only [com.sucharu.sucharupro.domain.model.inventory.StockStatusType.LOW_STOCK]
     * and [com.sucharu.sucharupro.domain.model.inventory.StockStatusType.OUT_OF_STOCK] items.
     *
     * ⚠️ Finished products only. No raw material alerts.
     */
    fun getInventoryAlerts(): Flow<List<DashboardInventoryAlert>> =
        getDashboardSummary().map { it.inventoryAlerts }
}
