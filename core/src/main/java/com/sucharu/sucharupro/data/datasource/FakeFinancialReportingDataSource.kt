package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinancialReportActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReportExportRequest
import com.sucharu.sucharupro.domain.model.finance.FinancialReportSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake data source for Financial Reporting (Module 09 Step 09).
 */
class FakeFinancialReportingDataSource : FinancialReportingDataSource {

    private val mutex = Mutex()
    private val snapshotsFlow = MutableStateFlow<List<FinancialReportSnapshot>>(emptyList())
    private val activityEventsFlow = MutableStateFlow<List<FinancialReportActivityEvent>>(emptyList())
    private val exportRequests = mutableListOf<FinancialReportExportRequest>()

    override suspend fun saveSnapshot(snapshot: FinancialReportSnapshot) {
        mutex.withLock {
            val current = snapshotsFlow.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.snapshotId == snapshot.snapshotId }
            if (existingIndex >= 0) {
                current[existingIndex] = snapshot
            } else {
                current.add(snapshot)
            }
            snapshotsFlow.value = current
        }
    }

    override suspend fun getSnapshotById(snapshotId: String): FinancialReportSnapshot? {
        return mutex.withLock {
            snapshotsFlow.value.find { it.snapshotId == snapshotId }
        }
    }

    override suspend fun getSnapshotByRequestId(projectId: String, snapshotRequestId: String): FinancialReportSnapshot? {
        return mutex.withLock {
            snapshotsFlow.value.find { it.projectId == projectId && it.snapshotRequestId == snapshotRequestId }
        }
    }

    override fun observeSnapshots(projectId: String): Flow<List<FinancialReportSnapshot>> {
        return snapshotsFlow.asStateFlow().map { list ->
            list.filter { it.projectId == projectId }
                .sortedByDescending { it.generatedAt }
        }
    }

    override suspend fun insertActivityEvent(event: FinancialReportActivityEvent) {
        mutex.withLock {
            val current = activityEventsFlow.value.toMutableList()
            current.add(event)
            activityEventsFlow.value = current
        }
    }

    override fun observeActivityEvents(projectId: String): Flow<List<FinancialReportActivityEvent>> {
        return activityEventsFlow.asStateFlow().map { list ->
            list.filter { it.projectId == projectId }
                .sortedByDescending { it.timestamp }
        }
    }

    override suspend fun recordExportRequest(request: FinancialReportExportRequest) {
        mutex.withLock {
            exportRequests.add(request)
        }
    }
}
