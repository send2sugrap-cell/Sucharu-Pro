package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinanceGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialAnalyticsSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for immutable analytics snapshots and governance audit trail (Module 09 Step 10).
 */
interface FinanceAnalyticsDataSource {

    suspend fun saveSnapshot(snapshot: FinancialAnalyticsSnapshot)

    suspend fun getSnapshotById(snapshotId: String): FinancialAnalyticsSnapshot?

    suspend fun getSnapshotByRequestId(projectId: String, snapshotRequestId: String): FinancialAnalyticsSnapshot?

    fun observeSnapshots(projectId: String): Flow<List<FinancialAnalyticsSnapshot>>

    suspend fun recordActivityEvent(event: FinanceGovernanceActivityEvent)

    fun observeActivityEvents(projectId: String): Flow<List<FinanceGovernanceActivityEvent>>
}
