package com.sucharu.sucharupro.ui.features.finance.reporting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialReportingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Financial Reporting & Analytics (Module 09 Step 09).
 */
class FinancialReportingViewModel(
    private val financialReportingRepository: FinancialReportingRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "ACTOR-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FinancialReportingUiState(
            filter = FinancialReportFilter(projectId = projectId)
        )
    )
    val uiState: StateFlow<FinancialReportingUiState> = _uiState.asStateFlow()

    init {
        loadCurrentReport()
        observeSnapshots()
        observeActivityEvents()
    }

    fun selectReportType(reportType: FinancialReportType) {
        _uiState.update { it.copy(selectedReportType = reportType) }
        loadCurrentReport()
    }

    fun selectPeriod(period: FinancialReportPeriod) {
        _uiState.update {
            val updatedFilter = it.filter.copy(reportPeriod = period)
            it.copy(selectedPeriod = period, filter = updatedFilter)
        }
        loadCurrentReport()
    }

    fun toggleFilterSheet(visible: Boolean) {
        _uiState.update { it.copy(isFilterSheetVisible = visible) }
    }

    fun applyFilter(filter: FinancialReportFilter) {
        _uiState.update { it.copy(filter = filter, isFilterSheetVisible = false) }
        loadCurrentReport()
    }

    fun loadCurrentReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = _uiState.value
            when (state.selectedReportType) {
                FinancialReportType.DASHBOARD,
                FinancialReportType.KPI_SUMMARY -> {
                    when (val res = financialReportingRepository.getFinancialKpiSummary(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, kpiSummary = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.PROFIT_AND_LOSS -> {
                    when (val res = financialReportingRepository.getProfitLossReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, profitLossReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.BALANCE_SHEET -> {
                    when (val res = financialReportingRepository.getBalanceSheetReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, balanceSheetReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.CASH_FLOW -> {
                    when (val res = financialReportingRepository.getCashFlowReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, cashFlowReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.TRIAL_BALANCE -> {
                    when (val res = financialReportingRepository.getTrialBalanceReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, trialBalanceReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.GENERAL_LEDGER -> {
                    when (val res = financialReportingRepository.getGeneralLedgerReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, generalLedgerReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.ACCOUNTS_RECEIVABLE -> {
                    when (val res = financialReportingRepository.getAccountsReceivableReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, accountsReceivableReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.ACCOUNTS_PAYABLE -> {
                    when (val res = financialReportingRepository.getAccountsPayableReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, accountsPayableReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.EXPENSE_ANALYSIS -> {
                    when (val res = financialReportingRepository.getExpenseAnalysisReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, expenseAnalysisReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.CUSTOMER_PAYMENT -> {
                    when (val res = financialReportingRepository.getCustomerPaymentReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, customerPaymentReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.SUPPLIER_PAYMENT -> {
                    when (val res = financialReportingRepository.getSupplierPaymentReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, supplierPaymentReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.ADJUSTMENT -> {
                    when (val res = financialReportingRepository.getFinancialAdjustmentReport(projectId, state.filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, financialAdjustmentReport = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinancialReportType.PERIOD_COMPARISON -> {
                    when (val res = financialReportingRepository.getPeriodComparisonReport(
                        projectId, FinancialReportPeriod.CurrentMonth, FinancialReportPeriod.PreviousMonth, currentActorId, currentUserRole
                    )) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, periodComparisonResult = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun createSnapshot(reportType: FinancialReportType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            val requestId = UUID.randomUUID().toString()
            when (val res = financialReportingRepository.createReportSnapshot(
                projectId, reportType, state.filter, requestId, currentActorId, currentUserRole
            )) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Audit Snapshot saved successfully (#${res.data.snapshotId.take(8)})") }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun requestExport(reportType: FinancialReportType, format: FinancialReportExportFormat) {
        viewModelScope.launch {
            val req = FinancialReportExportRequest(
                exportId = UUID.randomUUID().toString(),
                projectId = projectId,
                reportType = reportType,
                format = format,
                filter = _uiState.value.filter,
                requestedBy = currentActorId
            )
            when (val res = financialReportingRepository.requestExport(req, currentUserRole)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(successMessage = "Report exported as ${res.data}") }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
                else -> Unit
            }
        }
    }

    private fun observeSnapshots() {
        viewModelScope.launch {
            financialReportingRepository.observeReportSnapshots(projectId, currentUserRole).collect { list ->
                _uiState.update { it.copy(snapshots = list) }
            }
        }
    }

    private fun observeActivityEvents() {
        viewModelScope.launch {
            financialReportingRepository.observeActivityEvents(projectId, currentUserRole).collect { list ->
                _uiState.update { it.copy(activityEvents = list) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
