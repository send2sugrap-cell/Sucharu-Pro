package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DashboardDataSource
import com.sucharu.sucharupro.data.datasource.FakeDashboardDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.dashboard.DashboardInventoryAlert
import com.sucharu.sucharupro.domain.model.dashboard.DashboardJobSummary
import com.sucharu.sucharupro.domain.model.dashboard.DashboardKpis
import com.sucharu.sucharupro.domain.model.dashboard.DashboardOperationalAlerts
import com.sucharu.sucharupro.domain.model.dashboard.DashboardSummary
import com.sucharu.sucharupro.domain.model.dashboard.StageCount
import com.sucharu.sucharupro.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Production-ready implementation of [DashboardRepository].
 *
 * Delegates all data fetching to a [DashboardDataSource], keeping repository logic
 * (reactive streams, error mapping, caching) separate from data-fetching logic.
 *
 * Architecture position:
 * ```
 * DashboardViewModel
 *        ↓ (uses interface)
 * DashboardRepository          ← domain interface
 *        ↓ (implemented by THIS CLASS)
 * DashboardRepositoryImpl      ← YOU ARE HERE (data layer)
 *        ↓ (delegates to)
 * DashboardDataSource          ← data layer interface
 *        ↓
 * FakeDashboardDataSource      ← current in-memory implementation
 * RoomDashboardDataSource      ← future Room/SQLite implementation
 * RemoteDashboardDataSource    ← future API implementation
 * ```
 *
 * Design principles:
 *  - ViewModel never sees [DomainResult] or [DashboardDataSource] directly.
 *  - All reactive streaming is done here via [Flow]/[MutableStateFlow].
 *  - Error propagation: data source [DomainResult.Error] → repository propagates error
 *    through the flow, allowing ViewModel to catch and show UI error state.
 *  - Refresh: [refreshDashboardSummary] re-fetches from data source and updates the
 *    shared state flow, which [getDashboardSummary] observes.
 *
 * Swapping data sources:
 * Inject a different [DashboardDataSource] to switch between fake, Room, or remote
 * without changing ViewModel, domain model, or UI code.
 */
class DashboardRepositoryImpl(
    private val dataSource: DashboardDataSource = FakeDashboardDataSource()
) : DashboardRepository {

    /**
     * Internal hot state flow holding the latest dashboard summary snapshot.
     */
    private val _dashboardFlow = MutableStateFlow<DashboardSummary?>(null)

    /**
     * Reactive stream of [DashboardSummary].
     *
     * On initial collection, triggers an eager fetch from the data source if no cached
     * value exists. Emits the initial and subsequent refreshed summaries.
     */
    override fun getDashboardSummary(): Flow<DashboardSummary> = flow {
        if (_dashboardFlow.value == null) {
            when (val result = dataSource.fetchDashboardSummary()) {
                is DomainResult.Success -> {
                    _dashboardFlow.value = result.data
                }
                is DomainResult.Error -> {
                    throw result.exception ?: Exception(result.message)
                }
                is DomainResult.Loading -> {
                    // Handled upstream in UI layer
                }
            }
        }
        _dashboardFlow.collect { summary ->
            if (summary != null) {
                emit(summary)
            }
        }
    }

    /**
     * Fetches fresh data from [DashboardDataSource] and updates the shared flow.
     *
     * Called by [DashboardViewModel.refresh] and on pull-to-refresh actions.
     *
     * @return [Result.success] if fetch succeeded, [Result.failure] with the error.
     */
    override suspend fun refreshDashboardSummary(): Result<Unit> {
        return when (val result = dataSource.fetchDashboardSummary()) {
            is DomainResult.Success -> {
                _dashboardFlow.value = result.data
                Result.success(Unit)
            }
            is DomainResult.Error -> {
                Result.failure(
                    result.exception ?: Exception(result.message)
                )
            }
            is DomainResult.Loading -> {
                Result.success(Unit)
            }
        }
    }

    // =========================================================================
    // Granular Query Overrides
    //
    // These methods provide targeted reactive streams for specific dashboard
    // sections, backed by the shared summary stream.
    // =========================================================================

    override fun getDashboardKpis(): Flow<DashboardKpis> =
        getDashboardSummary().map { it.kpis }

    override fun getStageCounts(): Flow<List<StageCount>> =
        getDashboardSummary().map { it.stageCounts }

    override fun getOperationalAlerts(): Flow<DashboardOperationalAlerts> =
        getDashboardSummary().map { it.operationalAlerts }

    override fun getRecentOrders(limit: Int): Flow<List<DashboardJobSummary>> =
        getDashboardSummary().map { it.recentOrders.take(limit) }

    override fun getInventoryAlerts(): Flow<List<DashboardInventoryAlert>> =
        getDashboardSummary().map { it.inventoryAlerts }
}
