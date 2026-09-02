package com.sucharu.sucharupro.ui.features.finance.periodclose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.AccountingPeriodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountingPeriodViewModel(
    private val periodRepository: AccountingPeriodRepository,
    private val projectId: String = "DEFAULT_PROJECT",
    private val actorId: String = "ADMIN_USER",
    private val callerRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _listState = MutableStateFlow(AccountingPeriodListUiState(isLoading = true))
    val listState: StateFlow<AccountingPeriodListUiState> = _listState.asStateFlow()

    private val _detailsState = MutableStateFlow(AccountingPeriodDetailsUiState())
    val detailsState: StateFlow<AccountingPeriodDetailsUiState> = _detailsState.asStateFlow()

    init {
        loadPeriods()
    }

    fun loadPeriods() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, errorMessage = null) }

            periodRepository.observeAccountingPeriods(projectId, callerRole).collect { periods ->
                val openPeriod = periods.firstOrNull { it.status.isOpenForPosting } ?: periods.firstOrNull()
                val readiness = if (openPeriod != null) {
                    val r = periodRepository.evaluateClosingReadiness(openPeriod.periodId, callerRole)
                    if (r is DomainResult.Success) r.data else null
                } else null

                val reopenRequests = if (openPeriod != null) {
                    val r = periodRepository.getReopenRequestsByPeriod(openPeriod.periodId, callerRole)
                    if (r is DomainResult.Success) r.data else emptyList()
                } else emptyList()

                _listState.update {
                    it.copy(
                        isLoading = false,
                        periods = periods,
                        activePeriod = openPeriod,
                        readiness = readiness,
                        reopenRequests = reopenRequests
                    )
                }
            }
        }
    }

    fun loadPeriodDetails(periodId: String) {
        viewModelScope.launch {
            _detailsState.update { it.copy(isLoading = true, errorMessage = null) }

            val periodResult = periodRepository.getAccountingPeriod(periodId, callerRole)
            if (periodResult is DomainResult.Success) {
                val readinessResult = periodRepository.evaluateClosingReadiness(periodId, callerRole)
                val readiness = if (readinessResult is DomainResult.Success) readinessResult.data else null

                val snapshotResult = periodRepository.getFinancialClosingSnapshot(periodId, callerRole)
                val snapshot = if (snapshotResult is DomainResult.Success) snapshotResult.data else null

                val reopenResult = periodRepository.getReopenRequestsByPeriod(periodId, callerRole)
                val reopenRequests = if (reopenResult is DomainResult.Success) reopenResult.data else emptyList()

                _detailsState.update {
                    it.copy(
                        isLoading = false,
                        period = periodResult.data,
                        readiness = readiness,
                        snapshot = snapshot,
                        reopenRequests = reopenRequests
                    )
                }
            } else if (periodResult is DomainResult.Error) {
                _detailsState.update {
                    it.copy(isLoading = false, errorMessage = periodResult.message)
                }
            }
        }
    }

    fun submitForClosing(periodId: String) {
        viewModelScope.launch {
            _detailsState.update { it.copy(isEvaluating = true, errorMessage = null) }
            val result = periodRepository.submitPeriodForClosing(periodId, actorId, callerRole)
            when (result) {
                is DomainResult.Success -> {
                    _detailsState.update {
                        it.copy(
                            isEvaluating = false,
                            period = result.data,
                            successMessage = "Period submitted for closing review."
                        )
                    }
                    loadPeriodDetails(periodId)
                }
                is DomainResult.Error -> {
                    _detailsState.update {
                        it.copy(isEvaluating = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun closePeriod(periodId: String) {
        viewModelScope.launch {
            _detailsState.update { it.copy(isClosing = true, errorMessage = null) }
            val result = periodRepository.closeAccountingPeriod(periodId, actorId, callerRole)
            when (result) {
                is DomainResult.Success -> {
                    _detailsState.update {
                        it.copy(
                            isClosing = false,
                            snapshot = result.data,
                            successMessage = "Accounting period closed and locked successfully."
                        )
                    }
                    loadPeriodDetails(periodId)
                    loadPeriods()
                }
                is DomainResult.Error -> {
                    _detailsState.update {
                        it.copy(isClosing = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun createReopenRequest(periodId: String, reason: String) {
        viewModelScope.launch {
            _detailsState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = periodRepository.createReopenRequest(
                projectId = projectId,
                periodId = periodId,
                reason = reason,
                actorId = actorId,
                callerRole = callerRole
            )
            when (result) {
                is DomainResult.Success -> {
                    _detailsState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Reopen request submitted for Admin review."
                        )
                    }
                    loadPeriodDetails(periodId)
                }
                is DomainResult.Error -> {
                    _detailsState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun approveReopenRequest(requestId: String, periodId: String) {
        viewModelScope.launch {
            _detailsState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = periodRepository.approveReopenRequest(requestId, actorId, callerRole)
            if (result is DomainResult.Success) {
                val reopenExec = periodRepository.reopenAccountingPeriod(periodId, requestId, actorId, callerRole)
                when (reopenExec) {
                    is DomainResult.Success -> {
                        _detailsState.update {
                            it.copy(
                                isLoading = false,
                                period = reopenExec.data,
                                successMessage = "Period reopened for audit."
                            )
                        }
                        loadPeriodDetails(periodId)
                        loadPeriods()
                    }
                    is DomainResult.Error -> {
                        _detailsState.update {
                            it.copy(isLoading = false, errorMessage = reopenExec.message)
                        }
                    }
                    else -> {}
                }
            } else if (result is DomainResult.Error) {
                _detailsState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun rejectReopenRequest(requestId: String, periodId: String, rejectionReason: String) {
        viewModelScope.launch {
            _detailsState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = periodRepository.rejectReopenRequest(requestId, rejectionReason, actorId, callerRole)
            when (result) {
                is DomainResult.Success -> {
                    _detailsState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Reopen request rejected."
                        )
                    }
                    loadPeriodDetails(periodId)
                }
                is DomainResult.Error -> {
                    _detailsState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }
}
