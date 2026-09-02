package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignActivityEvent
import com.sucharu.sucharupro.domain.model.design.DesignAssignment
import com.sucharu.sucharupro.domain.model.design.DesignProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [DesignProjectDataSource] with [Mutex] atomicity.
 */
class FakeDesignProjectDataSource(
    initialProjects: List<DesignProject> = emptyList(),
    initialAssignments: List<DesignAssignment> = emptyList(),
    initialActivities: List<DesignActivityEvent> = emptyList()
) : DesignProjectDataSource {

    private val mutex = Mutex()
    private val _projects = MutableStateFlow<List<DesignProject>>(initialProjects)
    private val _assignments = MutableStateFlow<List<DesignAssignment>>(initialAssignments)
    private val _activityEvents = MutableStateFlow<List<DesignActivityEvent>>(initialActivities)

    override fun observeProjects(): Flow<List<DesignProject>> = _projects.asStateFlow()

    override suspend fun fetchProjectById(projectId: String): DomainResult<DesignProject> = mutex.withLock {
        val project = _projects.value.find { it.projectId == projectId }
        return if (project != null) {
            DomainResult.Success(project)
        } else {
            DomainResult.Error(message = "Design Project not found with ID: $projectId")
        }
    }

    override suspend fun insertProject(project: DesignProject): DomainResult<DesignProject> = mutex.withLock {
        if (_projects.value.any { it.projectId == project.projectId }) {
            return DomainResult.Error(message = "Design Project with ID '${project.projectId}' already exists.")
        }
        if (_projects.value.any { it.projectNumber.equals(project.projectNumber, ignoreCase = true) }) {
            return DomainResult.Error(message = "Design Project with Number '${project.projectNumber}' already exists.")
        }
        if (_projects.value.any { it.productionJobId == project.productionJobId && !it.status.isTerminal }) {
            return DomainResult.Error(message = "An active Design Project for Production Job '${project.productionJobId}' already exists.")
        }

        _projects.value = _projects.value + project
        DomainResult.Success(project)
    }

    override suspend fun updateProject(project: DesignProject): DomainResult<DesignProject> = mutex.withLock {
        val index = _projects.value.indexOfFirst { it.projectId == project.projectId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent Design Project: ${project.projectId}")
        }

        val currentList = _projects.value.toMutableList()
        currentList[index] = project
        _projects.value = currentList.toList()
        DomainResult.Success(project)
    }

    override fun observeAssignments(): Flow<List<DesignAssignment>> = _assignments.asStateFlow()

    override suspend fun insertAssignment(assignment: DesignAssignment): DomainResult<DesignAssignment> = mutex.withLock {
        if (_assignments.value.any { it.assignmentId == assignment.assignmentId }) {
            return DomainResult.Error(message = "Assignment with ID '${assignment.assignmentId}' already exists.")
        }
        _assignments.value = _assignments.value + assignment
        DomainResult.Success(assignment)
    }

    override suspend fun updateAssignment(assignment: DesignAssignment): DomainResult<DesignAssignment> = mutex.withLock {
        val index = _assignments.value.indexOfFirst { it.assignmentId == assignment.assignmentId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent assignment: ${assignment.assignmentId}")
        }

        val currentList = _assignments.value.toMutableList()
        currentList[index] = assignment
        _assignments.value = currentList.toList()
        DomainResult.Success(assignment)
    }

    override fun observeActivityEvents(): Flow<List<DesignActivityEvent>> = _activityEvents.asStateFlow()

    override suspend fun insertActivityEvent(event: DesignActivityEvent): DomainResult<DesignActivityEvent> = mutex.withLock {
        if (_activityEvents.value.any { it.eventId == event.eventId }) {
            return DomainResult.Error(message = "Activity event with ID '${event.eventId}' already exists.")
        }
        _activityEvents.value = listOf(event) + _activityEvents.value
        DomainResult.Success(event)
    }
}
