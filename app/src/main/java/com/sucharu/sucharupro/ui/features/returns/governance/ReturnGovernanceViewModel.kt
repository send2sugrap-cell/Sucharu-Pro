package com.sucharu.sucharupro.ui.features.returns.governance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnException
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ReturnAnalyticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Return Governance Exception Center (Module 11 Step 06).
 */
class ReturnGovernanceViewModel(
    private val repository: ReturnAnalyticsRepository,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ReturnGovernanceUiState())
    val uiState: StateFlow<ReturnGovernanceUiState> = _uiState.asStateFlow()

    fun loadExceptions(
        projectId: String,
        statusFilter: ReturnExceptionStatus? = _uiState.value.statusFilter,
        callerRole: UserRole? = null,
        callerProjectId: String? = projectId
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, projectId = projectId, statusFilter = statusFilter) }

        scope.launch {
            when (val res = repository.getExceptions(projectId, statusFilter, callerRole, callerProjectId)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, exceptions = res.data, errorMessage = null) }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                DomainResult.Loading -> {}
            }
        }
    }

    fun runInspection(
        actorId: String,
        callerRole: UserRole? = null
    ) {
        val proj = _uiState.value.projectId
        if (proj.isBlank()) return

        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
        scope.launch {
            when (val res = repository.runGovernanceInspection(proj, actorId, callerRole = callerRole, callerProjectId = proj)) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            exceptions = res.data,
                            successMessage = "Governance scan completed. Found ${res.data.size} exception(s)."
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isActionInProgress = false, errorMessage = res.message) }
                }
                DomainResult.Loading -> {}
            }
        }
    }

    fun acknowledgeException(
        exceptionId: String,
        actorId: String,
        callerRole: UserRole? = null
    ) {
        val ex = _uiState.value.exceptions.find { it.exceptionId == exceptionId } ?: return
        val proj = _uiState.value.projectId

        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
        scope.launch {
            when (val res = repository.acknowledgeException(exceptionId, actorId, ex.version, callerRole, proj)) {
                is DomainResult.Success -> {
                    _uiState.update { current ->
                        val updatedList = current.exceptions.map { if (it.exceptionId == exceptionId) res.data else it }
                        current.copy(
                            isActionInProgress = false,
                            exceptions = updatedList,
                            successMessage = "Exception acknowledged."
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isActionInProgress = false, errorMessage = res.message) }
                }
                DomainResult.Loading -> {}
            }
        }
    }

    fun resolveException(
        exceptionId: String,
        actorId: String,
        resolutionNotes: String,
        callerRole: UserRole? = null
    ) {
        val ex = _uiState.value.exceptions.find { it.exceptionId == exceptionId } ?: return
        val proj = _uiState.value.projectId

        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
        scope.launch {
            when (val res = repository.resolveException(exceptionId, actorId, resolutionNotes, ex.version, callerRole, proj)) {
                is DomainResult.Success -> {
                    _uiState.update { current ->
                        val updatedList = current.exceptions.map { if (it.exceptionId == exceptionId) res.data else it }
                        current.copy(
                            isActionInProgress = false,
                            exceptions = updatedList,
                            successMessage = "Exception successfully resolved."
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isActionInProgress = false, errorMessage = res.message) }
                }
                DomainResult.Loading -> {}
            }
        }
    }

    fun selectStatusFilter(filter: ReturnExceptionStatus?) {
        _uiState.update { it.copy(statusFilter = filter) }
        val proj = _uiState.value.projectId
        if (proj.isNotBlank()) {
            loadExceptions(proj, filter)
        }
    }
}
