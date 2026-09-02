package com.sucharu.sucharupro.ui.features.qc.costtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.QcCostTimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for QC Cost & Time Reconciliation Details Screen (Module 06 Step 08).
 */
class QcCostTimeDetailsViewModel(
    private val productionJobId: String,
    private val repository: QcCostTimeRepository,
    private val currentUserRole: UserRole? = UserRole.QC_INSPECTOR,
    private val currentUserId: String = "user-current",
    private val currentUserName: String = "Current User"
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        QcCostTimeDetailsUiState(
            isLoading = true,
            productionJobId = productionJobId,
            currentUserRole = currentUserRole
        )
    )
    val uiState: StateFlow<QcCostTimeDetailsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            repository.observeReconciliationForJob(productionJobId),
            repository.observeCostEntriesForJob(productionJobId),
            repository.observeTimeEntriesForJob(productionJobId),
            repository.observeSnapshotForJob(productionJobId),
            repository.observeActivityEvents(productionJobId)
        ) { recon, costs, times, snap, events ->
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    reconciliation = recon,
                    costEntries = costs,
                    timeEntries = times,
                    snapshot = snap,
                    activityEvents = events
                )
            }
        }.catch { error ->
            _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
        }.launchIn(viewModelScope)
    }

    fun onRecordCostClicked() {
        _uiState.update { it.copy(showRecordCostDialog = true) }
    }

    fun onRecordTimeClicked() {
        _uiState.update { it.copy(showRecordTimeDialog = true) }
    }

    fun onReconcileClicked() {
        _uiState.update { it.copy(showReconcileDialog = true) }
    }

    fun onAdjustClicked() {
        _uiState.update { it.copy(showAdjustDialog = true) }
    }

    fun onLockClicked() {
        _uiState.update { it.copy(showLockDialog = true) }
    }

    fun dismissDialogs() {
        _uiState.update {
            it.copy(
                showRecordCostDialog = false,
                showRecordTimeDialog = false,
                showReconcileDialog = false,
                showAdjustDialog = false,
                showLockDialog = false
            )
        }
    }

    fun recordCost(
        projectId: String,
        costType: QcCostType,
        description: String,
        quantity: Double,
        unitCost: Double,
        currency: String = "BDT",
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, showRecordCostDialog = false) }
            val res = repository.createCostEntry(
                projectId = projectId,
                productionJobId = productionJobId,
                costType = costType,
                description = description,
                quantity = quantity,
                unitCost = unitCost,
                currency = currency,
                recordedBy = currentUserId,
                recordedByName = currentUserName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (res) {
                is DomainResult.Success -> _uiState.update { it.copy(isActionInProgress = false, successMessage = "Cost entry recorded.") }
                is DomainResult.Error -> _uiState.update { it.copy(isActionInProgress = false, errorMessage = res.message) }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun recordTime(
        projectId: String,
        entryType: QcTimeEntryType,
        startedAt: String,
        endedAt: String?,
        durationMinutes: Long,
        notes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, showRecordTimeDialog = false) }
            val res = repository.createTimeEntry(
                projectId = projectId,
                productionJobId = productionJobId,
                entryType = entryType,
                actorId = currentUserId,
                actorName = currentUserName,
                startedAt = startedAt,
                endedAt = endedAt,
                durationMinutes = durationMinutes,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (res) {
                is DomainResult.Success -> _uiState.update { it.copy(isActionInProgress = false, successMessage = "Time entry recorded.") }
                is DomainResult.Error -> _uiState.update { it.copy(isActionInProgress = false, errorMessage = res.message) }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun executeReconciliation(
        plannedCost: Double,
        plannedMinutes: Long,
        notes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, showReconcileDialog = false) }
            val res = repository.calculateReconciliation(
                productionJobId = productionJobId,
                plannedCost = plannedCost,
                plannedMinutes = plannedMinutes,
                reconciledBy = currentUserId,
                reconciledByName = currentUserName,
                notes = notes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (res) {
                is DomainResult.Success -> _uiState.update { it.copy(isActionInProgress = false, successMessage = "Reconciliation completed.") }
                is DomainResult.Error -> _uiState.update { it.copy(isActionInProgress = false, errorMessage = res.message) }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun executeAdjustment(
        reconciliationId: String,
        adjustedPlannedCost: Double?,
        adjustedPlannedMinutes: Long?,
        adjustmentReason: String,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, showAdjustDialog = false) }
            val res = repository.adjustReconciliation(
                reconciliationId = reconciliationId,
                adjustedPlannedCost = adjustedPlannedCost,
                adjustedPlannedMinutes = adjustedPlannedMinutes,
                adjustmentReason = adjustmentReason,
                adjustedBy = currentUserId,
                adjustedByName = currentUserName,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (res) {
                is DomainResult.Success -> _uiState.update { it.copy(isActionInProgress = false, successMessage = "Reconciliation adjusted.") }
                is DomainResult.Error -> _uiState.update { it.copy(isActionInProgress = false, errorMessage = res.message) }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun executeLock(
        reconciliationId: String,
        lockNotes: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, showLockDialog = false) }
            val res = repository.lockReconciliation(
                reconciliationId = reconciliationId,
                lockedBy = currentUserId,
                lockedByName = currentUserName,
                lockNotes = lockNotes,
                timestamp = timestamp,
                callerRole = currentUserRole
            )
            when (res) {
                is DomainResult.Success -> _uiState.update { it.copy(isActionInProgress = false, successMessage = "Reconciliation permanently LOCKED and snapshot created.") }
                is DomainResult.Error -> _uiState.update { it.copy(isActionInProgress = false, errorMessage = res.message) }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
