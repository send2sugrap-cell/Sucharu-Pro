package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureRecord
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory implementation of [ProductionReQcDataSource] protected with [Mutex] (Module 06 Step 06).
 */
class FakeProductionReQcDataSource(
    initialReQcs: List<ReQcInspection> = emptyList(),
    initialFailureRecords: List<ReQcFailureRecord> = emptyList(),
    initialActivityEvents: List<ReQcActivityEvent> = emptyList()
) : ProductionReQcDataSource {

    private val mutex = Mutex()
    private val _reQcList = MutableStateFlow<List<ReQcInspection>>(initialReQcs)
    private val _failureRecords = MutableStateFlow<List<ReQcFailureRecord>>(initialFailureRecords)
    private val _activityEvents = MutableStateFlow<List<ReQcActivityEvent>>(initialActivityEvents)

    override fun observeReQcList(): Flow<List<ReQcInspection>> = _reQcList.asStateFlow()

    override suspend fun fetchReQcById(reQcId: String): DomainResult<ReQcInspection> = mutex.withLock {
        val found = _reQcList.value.find { it.reQcId == reQcId }
        return if (found != null) {
            DomainResult.Success(found)
        } else {
            DomainResult.Error(message = "Re-QC record not found with ID: $reQcId")
        }
    }

    override suspend fun insertReQc(reQc: ReQcInspection): DomainResult<ReQcInspection> = mutex.withLock {
        if (_reQcList.value.any { it.reQcId == reQc.reQcId }) {
            return DomainResult.Error(message = "Re-QC with ID '${reQc.reQcId}' already exists.")
        }
        _reQcList.value = _reQcList.value + reQc
        DomainResult.Success(reQc)
    }

    override suspend fun updateReQc(reQc: ReQcInspection): DomainResult<ReQcInspection> = mutex.withLock {
        val index = _reQcList.value.indexOfFirst { it.reQcId == reQc.reQcId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent Re-QC: ${reQc.reQcId}")
        }
        val currentList = _reQcList.value.toMutableList()
        currentList[index] = reQc
        _reQcList.value = currentList.toList()
        DomainResult.Success(reQc)
    }

    override fun observeFailureRecords(): Flow<List<ReQcFailureRecord>> = _failureRecords.asStateFlow()

    override suspend fun fetchFailureRecordById(recordId: String): DomainResult<ReQcFailureRecord> = mutex.withLock {
        val found = _failureRecords.value.find { it.failureRecordId == recordId }
        return if (found != null) {
            DomainResult.Success(found)
        } else {
            DomainResult.Error(message = "Failure record not found with ID: $recordId")
        }
    }

    override suspend fun insertFailureRecord(record: ReQcFailureRecord): DomainResult<ReQcFailureRecord> = mutex.withLock {
        if (_failureRecords.value.any { it.failureRecordId == record.failureRecordId }) {
            return DomainResult.Error(message = "Failure record with ID '${record.failureRecordId}' already exists.")
        }
        _failureRecords.value = _failureRecords.value + record
        DomainResult.Success(record)
    }

    override fun observeActivityEvents(): Flow<List<ReQcActivityEvent>> = _activityEvents.asStateFlow()

    override suspend fun insertActivityEvent(event: ReQcActivityEvent): DomainResult<ReQcActivityEvent> = mutex.withLock {
        if (_activityEvents.value.any { it.eventId == event.eventId }) {
            return DomainResult.Error(message = "Activity event with ID '${event.eventId}' already exists.")
        }
        _activityEvents.value = listOf(event) + _activityEvents.value
        DomainResult.Success(event)
    }
}
