package com.sucharu.sucharupro.ui.features.production.scheduling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.productionscheduling.*
import com.sucharu.sucharupro.domain.service.productionscheduling.ProductionSchedulingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductionSchedulingViewModel(
    private val schedulingService: ProductionSchedulingService,
    private val defaultTenantId: String = "TENANT-001",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ProductionSchedulingUiState())
    val uiState: StateFlow<ProductionSchedulingUiState> = _uiState.asStateFlow()

    fun loadScheduleForJob(jobId: String, tenantId: String = defaultTenantId) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        scope.launch {
            try {
                val schedules = schedulingService.listSchedulesForJob(tenantId, jobId)
                val current = schedules.firstOrNull { it.isCurrent } ?: schedules.firstOrNull()
                if (current != null) {
                    val capacity = schedulingService.listCapacityWindows(tenantId, null, null)
                    val queue = schedulingService.listDispatchQueue(tenantId, current.scheduleId)
                    val conflicts = schedulingService.getScheduleConflicts(tenantId, current.scheduleId)
                    val recon = schedulingService.reconcileSchedule(tenantId, current.scheduleId)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentSchedule = current.toDto(),
                        scheduleVersions = schedules.map { it.toDto() },
                        capacityWindows = capacity.map { it.toDto() },
                        dispatchQueue = queue.map { it.toDto() },
                        conflicts = conflicts.map { it.toDto() },
                        reconciliationResult = recon.toDto(),
                        selectedVersion = current.version
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentSchedule = null,
                        scheduleVersions = emptyList<ProductionScheduleResponseDto>()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load schedule for job $jobId"
                )
            }
        }
    }

    fun createSchedule(
        jobId: String,
        baseStartTime: Long? = null,
        requestedDueDate: Long? = null,
        actor: String = "planner",
        tenantId: String = defaultTenantId
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        scope.launch {
            try {
                val created = schedulingService.createScheduleForJob(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    baseStartTime = baseStartTime,
                    requestedDueDate = requestedDueDate,
                    actor = actor
                )
                loadScheduleForJob(jobId, tenantId)
                _uiState.value = _uiState.value.copy(successMessage = "Schedule V${created.version} created successfully.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to create schedule"
                )
            }
        }
    }

    fun approveSchedule(scheduleId: String, actor: String = "manager", tenantId: String = defaultTenantId) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        scope.launch {
            try {
                val approved = schedulingService.approveSchedule(tenantId, scheduleId, actor)
                loadScheduleForJob(approved.executionJobId, tenantId)
                _uiState.value = _uiState.value.copy(successMessage = "Schedule approved & dispatch queue populated.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to approve schedule"
                )
            }
        }
    }

    fun supersedeSchedule(
        scheduleId: String,
        reason: String,
        newStartTime: Long? = null,
        requestedDueDate: Long? = null,
        actor: String = "manager",
        tenantId: String = defaultTenantId
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        scope.launch {
            try {
                val newSched = schedulingService.supersedeSchedule(
                    tenantId = tenantId,
                    scheduleId = scheduleId,
                    reason = reason,
                    newStartTime = newStartTime,
                    requestedDueDate = requestedDueDate,
                    actor = actor
                )
                loadScheduleForJob(newSched.executionJobId, tenantId)
                _uiState.value = _uiState.value.copy(successMessage = "Schedule superseded. V${newSched.version} is now active.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to supersede schedule"
                )
            }
        }
    }

    fun dispatchQueueItem(queueItemId: String, actor: String = "dispatcher", tenantId: String = defaultTenantId) {
        scope.launch {
            try {
                schedulingService.dispatchQueueItem(tenantId, queueItemId, actor)
                val currentSchedId = _uiState.value.currentSchedule?.scheduleId
                if (currentSchedId != null) {
                    val queue = schedulingService.listDispatchQueue(tenantId, currentSchedId)
                    _uiState.value = _uiState.value.copy(
                        dispatchQueue = queue.map { it.toDto() },
                        successMessage = "Work order dispatched to machine floor."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to dispatch queue item")
            }
        }
    }

    fun acknowledgeQueueItem(queueItemId: String, actor: String = "operator", tenantId: String = defaultTenantId) {
        scope.launch {
            try {
                schedulingService.acknowledgeQueueItem(tenantId, queueItemId, actor)
                val currentSchedId = _uiState.value.currentSchedule?.scheduleId
                if (currentSchedId != null) {
                    val queue = schedulingService.listDispatchQueue(tenantId, currentSchedId)
                    _uiState.value = _uiState.value.copy(
                        dispatchQueue = queue.map { it.toDto() },
                        successMessage = "Work order acknowledged by operator on floor."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to acknowledge queue item")
            }
        }
    }

    fun reconcileSchedule(scheduleId: String, tenantId: String = defaultTenantId) {
        scope.launch {
            try {
                val recon = schedulingService.reconcileSchedule(tenantId, scheduleId)
                _uiState.value = _uiState.value.copy(
                    reconciliationResult = recon.toDto(),
                    successMessage = "8-Way scheduling reconciliation completed."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Reconciliation failed")
            }
        }
    }

    fun fetchHandoffContract(scheduleId: String, tenantId: String = defaultTenantId) {
        scope.launch {
            try {
                val contract = schedulingService.getAiHandoffContract(tenantId, scheduleId)
                _uiState.value = _uiState.value.copy(
                    handoffContract = contract.toDto(),
                    isHandoffDialogOpen = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to export AI handoff contract")
            }
        }
    }
}
