package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReworkAssignment
import com.sucharu.sucharupro.domain.model.qc.ReworkEvidence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory implementation of [ProductionReworkDataSource] protected with [Mutex] (Module 06 Step 05).
 */
class FakeProductionReworkDataSource(
    initialReworks: List<ProductionRework> = emptyList(),
    initialAssignments: List<ReworkAssignment> = emptyList(),
    initialActivities: List<ReworkActivityEvent> = emptyList(),
    initialEvidence: List<ReworkEvidence> = emptyList()
) : ProductionReworkDataSource {

    private val mutex = Mutex()
    private val _reworks = MutableStateFlow<List<ProductionRework>>(initialReworks)
    private val _assignments = MutableStateFlow<List<ReworkAssignment>>(initialAssignments)
    private val _activityEvents = MutableStateFlow<List<ReworkActivityEvent>>(initialActivities)
    private val _evidenceList = MutableStateFlow<List<ReworkEvidence>>(initialEvidence)

    override fun observeReworks(): Flow<List<ProductionRework>> = _reworks.asStateFlow()

    override suspend fun fetchReworkById(reworkId: String): DomainResult<ProductionRework> = mutex.withLock {
        val rework = _reworks.value.find { it.reworkId == reworkId }
        return if (rework != null) {
            DomainResult.Success(rework)
        } else {
            DomainResult.Error(message = "Rework record not found with ID: $reworkId")
        }
    }

    override suspend fun insertRework(rework: ProductionRework): DomainResult<ProductionRework> = mutex.withLock {
        if (_reworks.value.any { it.reworkId == rework.reworkId }) {
            return DomainResult.Error(message = "Rework with ID '${rework.reworkId}' already exists.")
        }
        _reworks.value = _reworks.value + rework
        DomainResult.Success(rework)
    }

    override suspend fun updateRework(rework: ProductionRework): DomainResult<ProductionRework> = mutex.withLock {
        val index = _reworks.value.indexOfFirst { it.reworkId == rework.reworkId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent rework: ${rework.reworkId}")
        }
        val currentList = _reworks.value.toMutableList()
        currentList[index] = rework
        _reworks.value = currentList.toList()
        DomainResult.Success(rework)
    }

    override fun observeAssignments(): Flow<List<ReworkAssignment>> = _assignments.asStateFlow()

    override suspend fun insertAssignment(assignment: ReworkAssignment): DomainResult<ReworkAssignment> = mutex.withLock {
        if (_assignments.value.any { it.assignmentId == assignment.assignmentId }) {
            return DomainResult.Error(message = "Assignment with ID '${assignment.assignmentId}' already exists.")
        }
        _assignments.value = _assignments.value + assignment
        DomainResult.Success(assignment)
    }

    override suspend fun updateAssignment(assignment: ReworkAssignment): DomainResult<ReworkAssignment> = mutex.withLock {
        val index = _assignments.value.indexOfFirst { it.assignmentId == assignment.assignmentId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent assignment: ${assignment.assignmentId}")
        }
        val currentList = _assignments.value.toMutableList()
        currentList[index] = assignment
        _assignments.value = currentList.toList()
        DomainResult.Success(assignment)
    }

    override fun observeActivityEvents(): Flow<List<ReworkActivityEvent>> = _activityEvents.asStateFlow()

    override suspend fun insertActivityEvent(event: ReworkActivityEvent): DomainResult<ReworkActivityEvent> = mutex.withLock {
        if (_activityEvents.value.any { it.eventId == event.eventId }) {
            return DomainResult.Error(message = "Activity event with ID '${event.eventId}' already exists.")
        }
        _activityEvents.value = listOf(event) + _activityEvents.value
        DomainResult.Success(event)
    }

    override fun observeEvidence(): Flow<List<ReworkEvidence>> = _evidenceList.asStateFlow()

    override suspend fun insertEvidence(evidence: ReworkEvidence): DomainResult<ReworkEvidence> = mutex.withLock {
        if (_evidenceList.value.any { it.evidenceId == evidence.evidenceId }) {
            return DomainResult.Error(message = "Evidence with ID '${evidence.evidenceId}' already exists.")
        }
        _evidenceList.value = _evidenceList.value + evidence
        DomainResult.Success(evidence)
    }
}
