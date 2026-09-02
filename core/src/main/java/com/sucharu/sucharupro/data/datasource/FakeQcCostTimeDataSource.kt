package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeSnapshot
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory implementation of [QcCostTimeDataSource] (Module 06 Step 08).
 */
class FakeQcCostTimeDataSource : QcCostTimeDataSource {

    private val mutex = Mutex()

    private val costEntriesFlow = MutableStateFlow<List<QcCostEntry>>(emptyList())
    private val timeEntriesFlow = MutableStateFlow<List<QcTimeEntry>>(emptyList())
    private val reconciliationsFlow = MutableStateFlow<List<QcCostTimeReconciliation>>(emptyList())
    private val snapshotsFlow = MutableStateFlow<List<QcCostTimeSnapshot>>(emptyList())
    private val activityEventsFlow = MutableStateFlow<List<QcCostTimeActivityEvent>>(emptyList())

    override fun observeCostEntries(): Flow<List<QcCostEntry>> = costEntriesFlow.asStateFlow()

    override fun observeCostEntriesForJob(productionJobId: String): Flow<List<QcCostEntry>> =
        costEntriesFlow.map { list -> list.filter { it.productionJobId == productionJobId } }

    override suspend fun findCostEntryById(id: String): QcCostEntry? = mutex.withLock {
        costEntriesFlow.value.find { it.id == id }
    }

    override suspend fun insertCostEntry(entry: QcCostEntry): DomainResult<QcCostEntry> = mutex.withLock {
        if (costEntriesFlow.value.any { it.id == entry.id }) {
            return DomainResult.Error(message = "Cost entry with ID '${entry.id}' already exists.")
        }
        costEntriesFlow.update { it + entry }
        DomainResult.Success(entry)
    }

    override suspend fun updateCostEntry(entry: QcCostEntry): DomainResult<QcCostEntry> = mutex.withLock {
        val index = costEntriesFlow.value.indexOfFirst { it.id == entry.id }
        if (index == -1) {
            return DomainResult.Error(message = "Cost entry with ID '${entry.id}' not found.")
        }
        costEntriesFlow.update { list ->
            list.map { if (it.id == entry.id) entry else it }
        }
        DomainResult.Success(entry)
    }

    override fun observeTimeEntries(): Flow<List<QcTimeEntry>> = timeEntriesFlow.asStateFlow()

    override fun observeTimeEntriesForJob(productionJobId: String): Flow<List<QcTimeEntry>> =
        timeEntriesFlow.map { list -> list.filter { it.productionJobId == productionJobId } }

    override suspend fun findTimeEntryById(id: String): QcTimeEntry? = mutex.withLock {
        timeEntriesFlow.value.find { it.id == id }
    }

    override suspend fun insertTimeEntry(entry: QcTimeEntry): DomainResult<QcTimeEntry> = mutex.withLock {
        if (timeEntriesFlow.value.any { it.id == entry.id }) {
            return DomainResult.Error(message = "Time entry with ID '${entry.id}' already exists.")
        }
        timeEntriesFlow.update { it + entry }
        DomainResult.Success(entry)
    }

    override suspend fun updateTimeEntry(entry: QcTimeEntry): DomainResult<QcTimeEntry> = mutex.withLock {
        val index = timeEntriesFlow.value.indexOfFirst { it.id == entry.id }
        if (index == -1) {
            return DomainResult.Error(message = "Time entry with ID '${entry.id}' not found.")
        }
        timeEntriesFlow.update { list ->
            list.map { if (it.id == entry.id) entry else it }
        }
        DomainResult.Success(entry)
    }

    override fun observeReconciliations(): Flow<List<QcCostTimeReconciliation>> = reconciliationsFlow.asStateFlow()

    override fun observeReconciliationForJob(productionJobId: String): Flow<QcCostTimeReconciliation?> =
        reconciliationsFlow.map { list -> list.find { it.productionJobId == productionJobId } }

    override suspend fun findReconciliationById(id: String): QcCostTimeReconciliation? = mutex.withLock {
        reconciliationsFlow.value.find { it.id == id }
    }

    override suspend fun findReconciliationByJob(productionJobId: String): QcCostTimeReconciliation? = mutex.withLock {
        reconciliationsFlow.value.find { it.productionJobId == productionJobId }
    }

    override suspend fun insertReconciliation(reconciliation: QcCostTimeReconciliation): DomainResult<QcCostTimeReconciliation> = mutex.withLock {
        if (reconciliationsFlow.value.any { it.id == reconciliation.id }) {
            return DomainResult.Error(message = "Reconciliation with ID '${reconciliation.id}' already exists.")
        }
        reconciliationsFlow.update { it + reconciliation }
        DomainResult.Success(reconciliation)
    }

    override suspend fun updateReconciliation(reconciliation: QcCostTimeReconciliation): DomainResult<QcCostTimeReconciliation> = mutex.withLock {
        val index = reconciliationsFlow.value.indexOfFirst { it.id == reconciliation.id }
        if (index == -1) {
            return DomainResult.Error(message = "Reconciliation with ID '${reconciliation.id}' not found.")
        }
        reconciliationsFlow.update { list ->
            list.map { if (it.id == reconciliation.id) reconciliation else it }
        }
        DomainResult.Success(reconciliation)
    }

    override fun observeSnapshots(): Flow<List<QcCostTimeSnapshot>> = snapshotsFlow.asStateFlow()

    override fun observeSnapshotForJob(productionJobId: String): Flow<QcCostTimeSnapshot?> =
        snapshotsFlow.map { list -> list.find { it.productionJobId == productionJobId } }

    override suspend fun findSnapshotById(snapshotId: String): QcCostTimeSnapshot? = mutex.withLock {
        snapshotsFlow.value.find { it.snapshotId == snapshotId }
    }

    override suspend fun findSnapshotByJob(productionJobId: String): QcCostTimeSnapshot? = mutex.withLock {
        snapshotsFlow.value.find { it.productionJobId == productionJobId }
    }

    override suspend fun insertSnapshot(snapshot: QcCostTimeSnapshot): DomainResult<QcCostTimeSnapshot> = mutex.withLock {
        if (snapshotsFlow.value.any { it.snapshotId == snapshot.snapshotId }) {
            return DomainResult.Error(message = "Snapshot with ID '${snapshot.snapshotId}' already exists.")
        }
        snapshotsFlow.update { it + snapshot }
        DomainResult.Success(snapshot)
    }

    override fun observeActivityEvents(productionJobId: String): Flow<List<QcCostTimeActivityEvent>> =
        activityEventsFlow.map { list -> list.filter { it.productionJobId == productionJobId } }

    override suspend fun insertActivityEvent(event: QcCostTimeActivityEvent): DomainResult<QcCostTimeActivityEvent> = mutex.withLock {
        activityEventsFlow.update { it + event }
        DomainResult.Success(event)
    }

    suspend fun clear() = mutex.withLock {
        costEntriesFlow.value = emptyList()
        timeEntriesFlow.value = emptyList()
        reconciliationsFlow.value = emptyList()
        snapshotsFlow.value = emptyList()
        activityEventsFlow.value = emptyList()
    }
}
