package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinanceGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialAnalyticsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe fake data source for Finance Analytics & Governance (Module 09 Step 10).
 */
class FakeFinanceAnalyticsDataSource : FinanceAnalyticsDataSource {

    private val mutex = Mutex()
    private val snapshots = MutableStateFlow<Map<String, FinancialAnalyticsSnapshot>>(emptyMap())
    private val activityEvents = MutableStateFlow<List<FinanceGovernanceActivityEvent>>(emptyList())

    override suspend fun saveSnapshot(snapshot: FinancialAnalyticsSnapshot) = mutex.withLock {
        snapshots.update { current ->
            current + (snapshot.snapshotId to snapshot)
        }
    }

    override suspend fun getSnapshotById(snapshotId: String): FinancialAnalyticsSnapshot? = mutex.withLock {
        snapshots.value[snapshotId]
    }

    override suspend fun getSnapshotByRequestId(
        projectId: String,
        snapshotRequestId: String
    ): FinancialAnalyticsSnapshot? = mutex.withLock {
        snapshots.value.values.firstOrNull { it.projectId == projectId && it.snapshotRequestId == snapshotRequestId }
    }

    override fun observeSnapshots(projectId: String): Flow<List<FinancialAnalyticsSnapshot>> {
        return snapshots.asStateFlow().map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.generatedAt }
        }
    }

    override suspend fun recordActivityEvent(event: FinanceGovernanceActivityEvent) = mutex.withLock {
        activityEvents.update { current ->
            current + event
        }
    }

    override fun observeActivityEvents(projectId: String): Flow<List<FinanceGovernanceActivityEvent>> {
        return activityEvents.asStateFlow().map { list ->
            list.filter { it.projectId == projectId }.sortedByDescending { it.timestamp }
        }
    }
}
