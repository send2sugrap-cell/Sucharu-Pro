package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DashboardDataSource
import com.sucharu.sucharupro.data.datasource.FakeDashboardDataSource
import com.sucharu.sucharupro.domain.repository.DashboardRepository

/**
 * ⚠️ Development/Mock Repository for Sucharu Pro Dashboard.
 *
 * Delegates all repository operations to [DashboardRepositoryImpl] using the in-memory
 * [FakeDashboardDataSource].
 *
 * This provides 100% backward compatibility with existing usages (e.g. in [DashboardViewModel])
 * while establishing a clean separation between repository and data-source layers.
 *
 * To swap data sources in the future (e.g., Room database or Network API):
 * Simply construct [DashboardRepositoryImpl] with the real data source implementation.
 */
class FakeDashboardRepository(
    dataSource: DashboardDataSource = FakeDashboardDataSource()
) : DashboardRepository by DashboardRepositoryImpl(dataSource)
