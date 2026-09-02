package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodClosingSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe fake data source for Financial Closing Snapshot persistence (Module 09 Step 08).
 */
class FakeFinancialClosingSnapshotDataSource : FinancialClosingSnapshotDataSource {

    private val mutex = Mutex()
    private val snapshotsState = MutableStateFlow<Map<String, FinancialPeriodClosingSnapshot>>(emptyMap())
    private val counters = mutableMapOf<String, Int>()

    override suspend fun insertSnapshot(snapshot: FinancialPeriodClosingSnapshot): Boolean = mutex.withLock {
        if (snapshotsState.value.containsKey(snapshot.snapshotId)) return@withLock false
        snapshotsState.value = snapshotsState.value + (snapshot.snapshotId to snapshot)
        true
    }

    override suspend fun getSnapshotById(snapshotId: String): FinancialPeriodClosingSnapshot? = mutex.withLock {
        snapshotsState.value[snapshotId]
    }

    override suspend fun getSnapshotByPeriod(periodId: String): FinancialPeriodClosingSnapshot? = mutex.withLock {
        snapshotsState.value.values.firstOrNull { it.periodId == periodId }
    }

    override suspend fun getSnapshotsByProject(projectId: String): List<FinancialPeriodClosingSnapshot> = mutex.withLock {
        snapshotsState.value.values.filter { it.projectId == projectId }.sortedByDescending { it.generatedAt }
    }

    override fun observeSnapshots(projectId: String): Flow<List<FinancialPeriodClosingSnapshot>> {
        return snapshotsState.asStateFlow().map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.generatedAt }
        }
    }

    override suspend fun generateNextSnapshotNo(projectId: String): String = mutex.withLock {
        val count = (counters[projectId] ?: 0) + 1
        counters[projectId] = count
        "SNAP-2026-${count.toString().padStart(4, '0')}"
    }
}
