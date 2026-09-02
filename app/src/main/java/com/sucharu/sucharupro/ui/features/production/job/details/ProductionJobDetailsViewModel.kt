package com.sucharu.sucharupro.ui.features.production.job.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ViewModel for Production Job Card details, stage progression, hold/resume, and cancellation.
 */
class ProductionJobDetailsViewModel(
    private val repository: ProductionJobRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<ProductionJobDetailsUiState>(ProductionJobDetailsUiState.Loading)
    val uiState: StateFlow<ProductionJobDetailsUiState> = _uiState.asStateFlow()

    private var currentJobId: String? = null

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun loadJob(jobId: String) {
        currentJobId = jobId
        _uiState.value = ProductionJobDetailsUiState.Loading

        scope.launch {
            kotlinx.coroutines.flow.combine(
                repository.getJobById(jobId),
                repository.getProductionActivityEvents(jobId),
                repository.getStageExecutionsForJob(jobId),
                repository.getStageOutputsForJob(jobId)
            ) { job, activities, executions, outputs ->
                JobDetailsCombinedData(job, activities, executions, outputs)
            }.collect { (job, activities, executions, outputs) ->
                if (job != null) {
                    val currentSuccess = _uiState.value as? ProductionJobDetailsUiState.Success
                    val reconciliation = com.sucharu.sucharupro.domain.validation.ProductionOutputReconciliationCalculator.computeJobReconciliation(
                        job = job,
                        outputs = outputs
                    )
                    val checklist = com.sucharu.sucharupro.domain.validation.ProductionCompletionValidator.computeCompletionChecklist(
                        job = job,
                        executions = executions,
                        outputs = outputs
                    )
                    _uiState.value = ProductionJobDetailsUiState.Success(
                        job = job,
                        activities = activities,
                        stageExecutions = executions,
                        stageOutputs = outputs,
                        reconciliation = reconciliation,
                        completionChecklist = checklist,
                        isActionInProgress = currentSuccess?.isActionInProgress ?: false,
                        actionMessage = currentSuccess?.actionMessage,
                        actionError = currentSuccess?.actionError
                    )
                } else {
                    _uiState.value = ProductionJobDetailsUiState.NotFound(jobId = jobId)
                }
            }
        }
    }

    fun startStage(stageId: String, actorId: String? = null, actorName: String? = null, notes: String? = null) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.startStage(
                jobId = current.job.jobId,
                stageId = stageId,
                actorId = actorId,
                actorName = actorName,
                notes = notes,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Stage started successfully.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun completeStage(stageId: String, actorId: String? = null, notes: String? = null) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.completeStage(
                jobId = current.job.jobId,
                stageId = stageId,
                actorId = actorId,
                notes = notes,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Stage completed successfully.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun skipStage(stageId: String, actorId: String? = null, notes: String? = null) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.skipStage(
                jobId = current.job.jobId,
                stageId = stageId,
                actorId = actorId,
                notes = notes,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Stage skipped.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun holdJob(reason: String? = null) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.holdJob(
                jobId = current.job.jobId,
                reason = reason,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Job placed on hold.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun resumeJob() {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.resumeJob(
                jobId = current.job.jobId,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Job resumed.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun cancelJob(reason: String) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.cancelJob(
                jobId = current.job.jobId,
                reason = reason,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Job cancelled.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun markJobReady() {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.markJobReady(
                jobId = current.job.jobId,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Job marked as Ready.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun confirmProductionCompletion(
        actorId: String = "supervisor",
        actorName: String = "Production Supervisor",
        remarks: String? = null
    ) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.confirmProductionCompletion(
                jobId = current.job.jobId,
                actorId = actorId,
                actorName = actorName,
                remarks = remarks,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "উৎপাদন সফলভাবে সম্পন্ন হয়েছে। (Production completed)",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun deliverJob() {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.deliverJob(
                jobId = current.job.jobId,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Job delivered.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun getAvailableOperators(): List<com.sucharu.sucharupro.domain.model.job.ProductionOperator> {
        return repository.getAvailableOperators()
    }

    fun assignStageOperator(
        stageId: String,
        operatorId: String,
        operatorName: String,
        assignedBy: String? = null,
        notes: String? = null
    ) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.assignStageOperator(
                jobId = current.job.jobId,
                stageId = stageId,
                operatorId = operatorId,
                operatorName = operatorName,
                assignedBy = assignedBy,
                notes = notes,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Operator assigned successfully.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun reassignStageOperator(
        stageId: String,
        newOperatorId: String,
        newOperatorName: String,
        reassignedBy: String? = null,
        notes: String? = null
    ) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.reassignStageOperator(
                jobId = current.job.jobId,
                stageId = stageId,
                newOperatorId = newOperatorId,
                newOperatorName = newOperatorName,
                reassignedBy = reassignedBy,
                notes = notes,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Operator reassigned successfully.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun unassignStageOperator(
        stageId: String,
        unassignedBy: String? = null,
        reason: String? = null
    ) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.unassignStageOperator(
                jobId = current.job.jobId,
                stageId = stageId,
                unassignedBy = unassignedBy,
                reason = reason,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Operator unassigned.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun addStageExecutionNote(
        stageId: String,
        note: String,
        actorId: String? = null,
        actorName: String? = null
    ) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.addStageExecutionNote(
                jobId = current.job.jobId,
                stageId = stageId,
                note = note,
                actorId = actorId,
                actorName = actorName,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Execution note added.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun recordStageOutput(
        stageId: String,
        quantity: Int,
        unit: String,
        remarks: String? = null,
        operatorId: String? = null,
        operatorName: String? = null
    ) {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(isActionInProgress = true, actionError = null)

        scope.launch {
            val result = repository.recordStageOutput(
                jobId = current.job.jobId,
                stageId = stageId,
                quantity = quantity,
                unit = unit,
                operatorId = operatorId,
                operatorName = operatorName,
                remarks = remarks,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionMessage = "Output of $quantity $unit recorded successfully.",
                        actionError = null
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = current.copy(
                        isActionInProgress = false,
                        actionError = result.message
                    )
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun dismissActionFeedback() {
        val current = _uiState.value as? ProductionJobDetailsUiState.Success ?: return
        _uiState.value = current.copy(actionMessage = null, actionError = null)
    }
}

private data class JobDetailsCombinedData(
    val job: ProductionJob?,
    val activities: List<com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent>,
    val executions: List<com.sucharu.sucharupro.domain.model.job.ProductionStageExecution>,
    val outputs: List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>
)
