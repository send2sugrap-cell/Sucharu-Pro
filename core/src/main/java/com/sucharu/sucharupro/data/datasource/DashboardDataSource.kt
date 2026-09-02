package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.dashboard.DashboardInventoryAlert
import com.sucharu.sucharupro.domain.model.dashboard.DashboardJobSummary
import com.sucharu.sucharupro.domain.model.dashboard.DashboardKpis
import com.sucharu.sucharupro.domain.model.dashboard.DashboardOperationalAlerts
import com.sucharu.sucharupro.domain.model.dashboard.DashboardSummary
import com.sucharu.sucharupro.domain.model.dashboard.StageCount

/**
 * Data source interface for the Sucharu Pro Dashboard.
 *
 * This interface defines the DATA LAYER contract for fetching raw dashboard data.
 * It sits between the [DashboardRepository] (domain interface) and the actual
 * data implementations (in-memory fake, Room database, remote API, etc.).
 *
 * Architecture position:
 * ```
 * DashboardViewModel
 *        ↓ (uses domain interface)
 * DashboardRepository          ← domain layer interface
 *        ↓ (implemented by)
 * DashboardRepositoryImpl      ← data layer implementation
 *        ↓ (delegates to)
 * DashboardDataSource          ← THIS INTERFACE (data layer)
 *        ↓ (implemented by)
 * FakeDashboardDataSource      ← in-memory fake (current)
 * RoomDashboardDataSource      ← future Room/SQLite implementation
 * RemoteDashboardDataSource    ← future API implementation
 * ```
 *
 * Design principles:
 *  - All methods are `suspend` — prepared for async data sources.
 *  - Returns [DomainResult] for structured success/error propagation.
 *  - No Flow here — reactive streaming is the repository's responsibility.
 *  - No Android/Compose imports — data source is platform-aware but not UI-aware.
 *
 * Future implementations:
 *  - [FakeDashboardDataSource] — in-memory sample data for development/prototyping
 *  - Room-based implementation — after database module is established
 *  - Remote API implementation — after backend integration module
 */
interface DashboardDataSource {

    /**
     * Fetches the complete dashboard summary aggregate in a single call.
     *
     * Preferred for initial dashboard load where all sections are needed at once.
     * For targeted section refresh, use the granular fetch methods below.
     *
     * @return [DomainResult.Success] with [DashboardSummary], or [DomainResult.Error].
     */
    suspend fun fetchDashboardSummary(): DomainResult<DashboardSummary>

    /**
     * Fetches executive KPI metrics only (sales, counts, financials).
     *
     * Use for targeted KPI section refresh without reloading the full dashboard.
     *
     * @return [DomainResult.Success] with [DashboardKpis], or [DomainResult.Error].
     */
    suspend fun fetchKpis(): DomainResult<DashboardKpis>

    /**
     * Fetches production pipeline stage counts.
     *
     * Uses canonical [com.sucharu.sucharupro.domain.model.production.ProductionStageType]
     * (13-stage workflow). Do NOT use OrderStatusType for stage counts.
     *
     * @return [DomainResult.Success] with all 13 [StageCount] entries, or [DomainResult.Error].
     */
    suspend fun fetchStageCounts(): DomainResult<List<StageCount>>

    /**
     * Fetches recent orders/jobs for the dashboard list.
     *
     * Each record maintains both:
     *  - `jobStatus: OrderStatusType` — commercial lifecycle state
     *  - `currentProductionStage: ProductionStageType?` — production pipeline position
     *
     * These two fields must NEVER be collapsed into one status.
     *
     * @param limit Maximum number of records to return.
     * @return [DomainResult.Success] with up to [limit] [DashboardJobSummary] entries.
     */
    suspend fun fetchRecentOrders(limit: Int = 10): DomainResult<List<DashboardJobSummary>>

    /**
     * Fetches finished product stock alert items.
     *
     * ⚠️ Returns FINISHED/SALEABLE PRODUCT alerts ONLY.
     * Do NOT return raw material alerts (paper, ink, plates, chemicals).
     *
     * Uses canonical [com.sucharu.sucharupro.domain.model.inventory.StockStatusType]
     * for stock status.
     *
     * @return [DomainResult.Success] with [DashboardInventoryAlert] list (LOW_STOCK / OUT_OF_STOCK items).
     */
    suspend fun fetchInventoryAlerts(): DomainResult<List<DashboardInventoryAlert>>

    /**
     * Fetches operational alert counts for all dashboard alert categories.
     *
     * Alert categories:
     *  - Pending Approval
     *  - QC Pending
     *  - Delivery Pending
     *  - Low Stock
     *  - Outstanding Payment
     *  - Vendor Due
     *  - Replacement Pending
     *  - Delayed Jobs
     *
     * @return [DomainResult.Success] with [DashboardOperationalAlerts], or [DomainResult.Error].
     */
    suspend fun fetchOperationalAlerts(): DomainResult<DashboardOperationalAlerts>
}
