package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcItem
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcSnapshot
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcAssignment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [ProductionQcDataSource] with [Mutex] atomicity.
 */
class FakeProductionQcDataSource(
    initialQcList: List<ProductionQc> = emptyList(),
    initialAssignments: List<QcAssignment> = emptyList(),
    initialActivities: List<QcActivityEvent> = emptyList(),
    initialItems: List<PreProductionQcItem> = emptyList(),
    initialSnapshots: List<PreProductionQcSnapshot> = emptyList()
) : ProductionQcDataSource {

    private val mutex = Mutex()
    private val _qcList = MutableStateFlow<List<ProductionQc>>(initialQcList)
    private val _assignments = MutableStateFlow<List<QcAssignment>>(initialAssignments)
    private val _activityEvents = MutableStateFlow<List<QcActivityEvent>>(initialActivities)
    private val _preProductionItems = MutableStateFlow<List<PreProductionQcItem>>(initialItems)
    private val _snapshots = MutableStateFlow<List<PreProductionQcSnapshot>>(initialSnapshots)

    override fun observeQcList(): Flow<List<ProductionQc>> = _qcList.asStateFlow()

    override suspend fun fetchQcById(qcId: String): DomainResult<ProductionQc> = mutex.withLock {
        val qc = _qcList.value.find { it.qcId == qcId }
        return if (qc != null) {
            DomainResult.Success(qc)
        } else {
            DomainResult.Error(message = "QC record not found with ID: $qcId")
        }
    }

    override suspend fun insertQc(qc: ProductionQc): DomainResult<ProductionQc> = mutex.withLock {
        if (_qcList.value.any { it.qcId == qc.qcId }) {
            return DomainResult.Error(message = "QC record with ID '${qc.qcId}' already exists.")
        }
        _qcList.value = _qcList.value + qc
        DomainResult.Success(qc)
    }

    override suspend fun updateQc(qc: ProductionQc): DomainResult<ProductionQc> = mutex.withLock {
        val index = _qcList.value.indexOfFirst { it.qcId == qc.qcId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent QC record: ${qc.qcId}")
        }

        val currentList = _qcList.value.toMutableList()
        currentList[index] = qc
        _qcList.value = currentList.toList()
        DomainResult.Success(qc)
    }

    override fun observeAssignments(): Flow<List<QcAssignment>> = _assignments.asStateFlow()

    override suspend fun insertAssignment(assignment: QcAssignment): DomainResult<QcAssignment> = mutex.withLock {
        if (_assignments.value.any { it.assignmentId == assignment.assignmentId }) {
            return DomainResult.Error(message = "Assignment with ID '${assignment.assignmentId}' already exists.")
        }
        _assignments.value = _assignments.value + assignment
        DomainResult.Success(assignment)
    }

    override suspend fun updateAssignment(assignment: QcAssignment): DomainResult<QcAssignment> = mutex.withLock {
        val index = _assignments.value.indexOfFirst { it.assignmentId == assignment.assignmentId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent assignment: ${assignment.assignmentId}")
        }

        val currentList = _assignments.value.toMutableList()
        currentList[index] = assignment
        _assignments.value = currentList.toList()
        DomainResult.Success(assignment)
    }

    override fun observeActivityEvents(): Flow<List<QcActivityEvent>> = _activityEvents.asStateFlow()

    override suspend fun insertActivityEvent(event: QcActivityEvent): DomainResult<QcActivityEvent> = mutex.withLock {
        if (_activityEvents.value.any { it.eventId == event.eventId }) {
            return DomainResult.Error(message = "Activity event with ID '${event.eventId}' already exists.")
        }
        _activityEvents.value = listOf(event) + _activityEvents.value
        DomainResult.Success(event)
    }

    override fun observePreProductionItems(): Flow<List<PreProductionQcItem>> = _preProductionItems.asStateFlow()

    override suspend fun insertPreProductionItems(items: List<PreProductionQcItem>): DomainResult<List<PreProductionQcItem>> = mutex.withLock {
        val existingIds = _preProductionItems.value.map { it.itemId }.toSet()
        val newItems = items.filter { it.itemId !in existingIds }
        _preProductionItems.value = _preProductionItems.value + newItems
        DomainResult.Success(items)
    }

    override suspend fun updatePreProductionItem(item: PreProductionQcItem): DomainResult<PreProductionQcItem> = mutex.withLock {
        val index = _preProductionItems.value.indexOfFirst { it.itemId == item.itemId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent check item: ${item.itemId}")
        }

        val currentList = _preProductionItems.value.toMutableList()
        currentList[index] = item
        _preProductionItems.value = currentList.toList()
        DomainResult.Success(item)
    }

    override fun observeSnapshots(): Flow<List<PreProductionQcSnapshot>> = _snapshots.asStateFlow()

    override suspend fun insertSnapshot(snapshot: PreProductionQcSnapshot): DomainResult<PreProductionQcSnapshot> = mutex.withLock {
        if (_snapshots.value.any { it.snapshotId == snapshot.snapshotId }) {
            return DomainResult.Error(message = "Snapshot with ID '${snapshot.snapshotId}' already exists.")
        }
        _snapshots.value = _snapshots.value + snapshot
        DomainResult.Success(snapshot)
    }
}
