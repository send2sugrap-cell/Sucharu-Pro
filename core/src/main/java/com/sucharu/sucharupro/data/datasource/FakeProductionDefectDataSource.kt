package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectAssignment
import com.sucharu.sucharupro.domain.model.qc.DefectEvidence
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.QcDefectActivityEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory implementation of [ProductionDefectDataSource] protected with [Mutex] (Module 06 Step 04).
 */
class FakeProductionDefectDataSource(
    initialDefects: List<ProductionDefect> = emptyList(),
    initialAssignments: List<DefectAssignment> = emptyList(),
    initialActivities: List<QcDefectActivityEvent> = emptyList(),
    initialEvidence: List<DefectEvidence> = emptyList()
) : ProductionDefectDataSource {

    private val mutex = Mutex()
    private val _defects = MutableStateFlow<List<ProductionDefect>>(initialDefects)
    private val _assignments = MutableStateFlow<List<DefectAssignment>>(initialAssignments)
    private val _activityEvents = MutableStateFlow<List<QcDefectActivityEvent>>(initialActivities)
    private val _evidenceList = MutableStateFlow<List<DefectEvidence>>(initialEvidence)

    override fun observeDefects(): Flow<List<ProductionDefect>> = _defects.asStateFlow()

    override suspend fun fetchDefectById(defectId: String): DomainResult<ProductionDefect> = mutex.withLock {
        val defect = _defects.value.find { it.defectId == defectId }
        return if (defect != null) {
            DomainResult.Success(defect)
        } else {
            DomainResult.Error(message = "Defect record not found with ID: $defectId")
        }
    }

    override suspend fun insertDefect(defect: ProductionDefect): DomainResult<ProductionDefect> = mutex.withLock {
        if (_defects.value.any { it.defectId == defect.defectId }) {
            return DomainResult.Error(message = "Defect with ID '${defect.defectId}' already exists.")
        }
        _defects.value = _defects.value + defect
        DomainResult.Success(defect)
    }

    override suspend fun updateDefect(defect: ProductionDefect): DomainResult<ProductionDefect> = mutex.withLock {
        val index = _defects.value.indexOfFirst { it.defectId == defect.defectId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent defect: ${defect.defectId}")
        }
        val currentList = _defects.value.toMutableList()
        currentList[index] = defect
        _defects.value = currentList.toList()
        DomainResult.Success(defect)
    }

    override fun observeAssignments(): Flow<List<DefectAssignment>> = _assignments.asStateFlow()

    override suspend fun insertAssignment(assignment: DefectAssignment): DomainResult<DefectAssignment> = mutex.withLock {
        if (_assignments.value.any { it.assignmentId == assignment.assignmentId }) {
            return DomainResult.Error(message = "Assignment with ID '${assignment.assignmentId}' already exists.")
        }
        _assignments.value = _assignments.value + assignment
        DomainResult.Success(assignment)
    }

    override suspend fun updateAssignment(assignment: DefectAssignment): DomainResult<DefectAssignment> = mutex.withLock {
        val index = _assignments.value.indexOfFirst { it.assignmentId == assignment.assignmentId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent assignment: ${assignment.assignmentId}")
        }
        val currentList = _assignments.value.toMutableList()
        currentList[index] = assignment
        _assignments.value = currentList.toList()
        DomainResult.Success(assignment)
    }

    override fun observeActivityEvents(): Flow<List<QcDefectActivityEvent>> = _activityEvents.asStateFlow()

    override suspend fun insertActivityEvent(event: QcDefectActivityEvent): DomainResult<QcDefectActivityEvent> = mutex.withLock {
        if (_activityEvents.value.any { it.eventId == event.eventId }) {
            return DomainResult.Error(message = "Activity event with ID '${event.eventId}' already exists.")
        }
        _activityEvents.value = listOf(event) + _activityEvents.value
        DomainResult.Success(event)
    }

    override fun observeEvidence(): Flow<List<DefectEvidence>> = _evidenceList.asStateFlow()

    override suspend fun insertEvidence(evidence: DefectEvidence): DomainResult<DefectEvidence> = mutex.withLock {
        if (_evidenceList.value.any { it.evidenceId == evidence.evidenceId }) {
            return DomainResult.Error(message = "Evidence with ID '${evidence.evidenceId}' already exists.")
        }
        _evidenceList.value = _evidenceList.value + evidence
        DomainResult.Success(evidence)
    }
}
