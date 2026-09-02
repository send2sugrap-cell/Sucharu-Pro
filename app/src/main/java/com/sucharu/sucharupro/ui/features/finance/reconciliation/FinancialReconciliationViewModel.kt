package com.sucharu.sucharupro.ui.features.finance.reconciliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.AccountingPeriodRepository
import com.sucharu.sucharupro.domain.repository.FinancialReconciliationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinancialReconciliationViewModel(
    private val reconciliationRepository: FinancialReconciliationRepository,
    private val periodRepository: AccountingPeriodRepository,
    private val projectId: String = "DEFAULT_PROJECT",
    private val actorId: String = "ADMIN_USER",
    private val callerRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(FinancialReconciliationDashboardUiState(isLoading = true))
    val dashboardState: StateFlow<FinancialReconciliationDashboardUiState> = _dashboardState.asStateFlow()

    private val _executionState = MutableStateFlow(FinancialReconciliationExecutionUiState())
    val executionState: StateFlow<FinancialReconciliationExecutionUiState> = _executionState.asStateFlow()

    private val _discrepanciesState = MutableStateFlow(FinancialDiscrepanciesUiState(isLoading = true))
    val discrepanciesState: StateFlow<FinancialDiscrepanciesUiState> = _discrepanciesState.asStateFlow()

    init {
        loadDashboardData()
        loadDiscrepancies()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = true, errorMessage = null) }

            val summaryResult = reconciliationRepository.getFinancialControlSummary(
                projectId = projectId,
                callerRole = callerRole
            )

            val summary = if (summaryResult is DomainResult.Success) summaryResult.data else null

            reconciliationRepository.observeReconciliations(projectId, callerRole).collect { list ->
                val discrepanciesResult = reconciliationRepository.getDiscrepancies(projectId, callerRole = callerRole)
                val discrepancies = if (discrepanciesResult is DomainResult.Success) discrepanciesResult.data else emptyList()

                _dashboardState.update {
                    it.copy(
                        isLoading = false,
                        summary = summary,
                        reconciliations = list,
                        discrepancies = discrepancies
                    )
                }
            }
        }
    }

    fun setTypeFilter(type: FinancialReconciliationType?) {
        _dashboardState.update { it.copy(selectedTypeFilter = type) }
    }

    fun setStatusFilter(status: FinancialReconciliationStatus?) {
        _dashboardState.update { it.copy(selectedStatusFilter = status) }
    }

    fun setSearchQuery(query: String) {
        _dashboardState.update { it.copy(searchQuery = query) }
    }

    fun setExecutionTab(tabIndex: Int) {
        _executionState.update { it.copy(activeTab = tabIndex) }
    }

    fun executeCashReconciliation(
        periodId: String,
        openingCash: Money,
        cashReceipts: Money,
        cashPayments: Money,
        cashAdjustments: Money = Money.ZERO,
        actualClosingCash: Money,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _executionState.update { it.copy(isExecuting = true, errorMessage = null) }
            val result = reconciliationRepository.executeCashReconciliation(
                projectId = projectId,
                periodId = periodId,
                openingCash = openingCash,
                cashReceipts = cashReceipts,
                cashPayments = cashPayments,
                cashAdjustments = cashAdjustments,
                actualClosingCash = actualClosingCash,
                notes = notes,
                actorId = actorId,
                callerRole = callerRole
            )

            when (result) {
                is DomainResult.Success -> {
                    _executionState.update {
                        it.copy(
                            isExecuting = false,
                            cashReconciliation = result.data,
                            actionSuccessMessage = "Cash reconciliation completed successfully."
                        )
                    }
                    loadDashboardData()
                }
                is DomainResult.Error -> {
                    _executionState.update {
                        it.copy(isExecuting = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun executeBankReconciliation(
        periodId: String,
        openingBankBalance: Money,
        ledgerDeposits: Money,
        ledgerWithdrawals: Money,
        bankStatementBalance: Money,
        outstandingDeposits: Money = Money.ZERO,
        outstandingWithdrawals: Money = Money.ZERO,
        adjustments: Money = Money.ZERO,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _executionState.update { it.copy(isExecuting = true, errorMessage = null) }
            val result = reconciliationRepository.executeBankReconciliation(
                projectId = projectId,
                periodId = periodId,
                openingBankBalance = openingBankBalance,
                ledgerDeposits = ledgerDeposits,
                ledgerWithdrawals = ledgerWithdrawals,
                bankStatementBalance = bankStatementBalance,
                outstandingDeposits = outstandingDeposits,
                outstandingWithdrawals = outstandingWithdrawals,
                adjustments = adjustments,
                notes = notes,
                actorId = actorId,
                callerRole = callerRole
            )

            when (result) {
                is DomainResult.Success -> {
                    _executionState.update {
                        it.copy(
                            isExecuting = false,
                            bankReconciliation = result.data,
                            actionSuccessMessage = "Bank reconciliation completed successfully."
                        )
                    }
                    loadDashboardData()
                }
                is DomainResult.Error -> {
                    _executionState.update {
                        it.copy(isExecuting = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun loadDiscrepancies(periodId: String? = null) {
        viewModelScope.launch {
            _discrepanciesState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = reconciliationRepository.getDiscrepancies(
                projectId = projectId,
                periodId = periodId,
                callerRole = callerRole
            )
            when (result) {
                is DomainResult.Success -> {
                    _discrepanciesState.update {
                        it.copy(isLoading = false, discrepancies = result.data)
                    }
                }
                is DomainResult.Error -> {
                    _discrepanciesState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun resolveDiscrepancy(discrepancyId: String, resolutionNote: String) {
        viewModelScope.launch {
            _discrepanciesState.update { it.copy(isResolving = true, errorMessage = null) }
            val result = reconciliationRepository.resolveDiscrepancy(
                discrepancyId = discrepancyId,
                resolutionNote = resolutionNote,
                actorId = actorId,
                callerRole = callerRole
            )
            when (result) {
                is DomainResult.Success -> {
                    _discrepanciesState.update {
                        it.copy(
                            isResolving = false,
                            successMessage = "Discrepancy resolved."
                        )
                    }
                    loadDiscrepancies()
                    loadDashboardData()
                }
                is DomainResult.Error -> {
                    _discrepanciesState.update {
                        it.copy(isResolving = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun waiveDiscrepancy(discrepancyId: String, waiverReason: String) {
        viewModelScope.launch {
            _discrepanciesState.update { it.copy(isWaiving = true, errorMessage = null) }
            val result = reconciliationRepository.waiveDiscrepancy(
                discrepancyId = discrepancyId,
                waiverReason = waiverReason,
                actorId = actorId,
                callerRole = callerRole
            )
            when (result) {
                is DomainResult.Success -> {
                    _discrepanciesState.update {
                        it.copy(
                            isWaiving = false,
                            successMessage = "Discrepancy waived by Administrator."
                        )
                    }
                    loadDiscrepancies()
                    loadDashboardData()
                }
                is DomainResult.Error -> {
                    _discrepanciesState.update {
                        it.copy(isWaiving = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }
}
