package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [ProductionJobDataSource] with [Mutex] atomicity.
 */
class FakeProductionJobDataSource(
    initialJobs: List<ProductionJob> = emptyList(),
    initialAssignments: List<ProductionStageAssignment> = emptyList(),
    initialExecutions: List<ProductionStageExecution> = emptyList(),
    initialActivities: List<ProductionActivityEvent> = emptyList(),
    initialOutputs: List<ProductionStageOutput> = emptyList()
) : ProductionJobDataSource {

    private val mutex = Mutex()
    private val _jobs = MutableStateFlow<List<ProductionJob>>(initialJobs)
    private val _assignments = MutableStateFlow<List<ProductionStageAssignment>>(initialAssignments)
    private val _executions = MutableStateFlow<List<ProductionStageExecution>>(initialExecutions)
    private val _activityEvents = MutableStateFlow<List<ProductionActivityEvent>>(initialActivities)
    private val _outputs = MutableStateFlow<List<ProductionStageOutput>>(initialOutputs)

    override fun observeJobs(): Flow<List<ProductionJob>> = _jobs.asStateFlow()

    override suspend fun fetchJobById(jobId: String): DomainResult<ProductionJob> = mutex.withLock {
        val job = _jobs.value.find { it.jobId == jobId }
        return if (job != null) {
            DomainResult.Success(job)
        } else {
            DomainResult.Error(message = "Production Job not found with ID: $jobId")
        }
    }

    override suspend fun insertJob(job: ProductionJob): DomainResult<ProductionJob> = mutex.withLock {
        if (_jobs.value.any { it.jobId == job.jobId }) {
            return DomainResult.Error(message = "Production Job with ID '${job.jobId}' already exists.")
        }
        if (_jobs.value.any { it.jobNumber.equals(job.jobNumber, ignoreCase = true) }) {
            return DomainResult.Error(message = "Production Job with Number '${job.jobNumber}' already exists.")
        }
        if (_jobs.value.any { it.handoffId == job.handoffId && !it.status.isTerminal }) {
            return DomainResult.Error(message = "An active Production Job for Handoff '${job.handoffId}' already exists.")
        }

        _jobs.value = _jobs.value + job
        DomainResult.Success(job)
    }

    override suspend fun updateJob(job: ProductionJob): DomainResult<ProductionJob> = mutex.withLock {
        val index = _jobs.value.indexOfFirst { it.jobId == job.jobId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent Production Job: ${job.jobId}")
        }

        val currentList = _jobs.value.toMutableList()
        currentList[index] = job
        _jobs.value = currentList.toList()
        DomainResult.Success(job)
    }

    override fun observeAssignments(): Flow<List<ProductionStageAssignment>> = _assignments.asStateFlow()

    override suspend fun insertAssignment(assignment: ProductionStageAssignment): DomainResult<ProductionStageAssignment> = mutex.withLock {
        if (_assignments.value.any { it.assignmentId == assignment.assignmentId }) {
            return DomainResult.Error(message = "Assignment with ID '${assignment.assignmentId}' already exists.")
        }
        _assignments.value = _assignments.value + assignment
        DomainResult.Success(assignment)
    }

    override suspend fun updateAssignment(assignment: ProductionStageAssignment): DomainResult<ProductionStageAssignment> = mutex.withLock {
        val index = _assignments.value.indexOfFirst { it.assignmentId == assignment.assignmentId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent assignment: ${assignment.assignmentId}")
        }

        val currentList = _assignments.value.toMutableList()
        currentList[index] = assignment
        _assignments.value = currentList.toList()
        DomainResult.Success(assignment)
    }

    override fun observeExecutions(): Flow<List<ProductionStageExecution>> = _executions.asStateFlow()

    override suspend fun insertExecution(execution: ProductionStageExecution): DomainResult<ProductionStageExecution> = mutex.withLock {
        if (_executions.value.any { it.executionId == execution.executionId }) {
            return DomainResult.Error(message = "Execution with ID '${execution.executionId}' already exists.")
        }
        _executions.value = _executions.value + execution
        DomainResult.Success(execution)
    }

    override suspend fun updateExecution(execution: ProductionStageExecution): DomainResult<ProductionStageExecution> = mutex.withLock {
        val index = _executions.value.indexOfFirst { it.executionId == execution.executionId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent execution: ${execution.executionId}")
        }

        val currentList = _executions.value.toMutableList()
        currentList[index] = execution
        _executions.value = currentList.toList()
        DomainResult.Success(execution)
    }

    override fun observeActivityEvents(): Flow<List<ProductionActivityEvent>> = _activityEvents.asStateFlow()

    override suspend fun insertActivityEvent(event: ProductionActivityEvent): DomainResult<ProductionActivityEvent> = mutex.withLock {
        _activityEvents.value = _activityEvents.value + event
        DomainResult.Success(event)
    }

    override fun observeOutputs(): Flow<List<ProductionStageOutput>> = _outputs.asStateFlow()

    override suspend fun insertOutput(output: ProductionStageOutput): DomainResult<ProductionStageOutput> = mutex.withLock {
        if (_outputs.value.any { it.outputId == output.outputId }) {
            return DomainResult.Error(message = "Output record with ID '${output.outputId}' already exists.")
        }
        _outputs.value = _outputs.value + output
        DomainResult.Success(output)
    }
}
