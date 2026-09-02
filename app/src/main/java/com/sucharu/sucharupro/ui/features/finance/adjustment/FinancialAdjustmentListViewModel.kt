package com.sucharu.sucharupro.ui.features.finance.adjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustment
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentSummary
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialAdjustmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FinancialAdjustmentListUiState(
    val isLoading: Boolean = true,
    val adjustments: List<FinancialAdjustment> = emptyList(),
    val filteredAdjustments: List<FinancialAdjustment> = emptyList(),
    val summary: FinancialAdjustmentSummary? = null,
    val searchQuery: String = "",
    val selectedType: FinancialAdjustmentType? = null,
    val selectedStatus: FinancialAdjustmentStatus? = null,
    val selectedDirection: FinancialAdjustmentDirection? = null,
    val errorMessage: String? = null
)

class FinancialAdjustmentListViewModel(
    private val adjustmentRepository: FinancialAdjustmentRepository,
    private val projectId: String,
    private val callerRole: UserRole,
    private val authenticatedCustomerId: String? = null,
    private val authenticatedVendorId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialAdjustmentListUiState())
    val uiState: StateFlow<FinancialAdjustmentListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val flow = if (callerRole == UserRole.CUSTOMER && authenticatedCustomerId != null) {
                adjustmentRepository.observeCustomerAdjustments(projectId, authenticatedCustomerId, callerRole, authenticatedCustomerId)
            } else if (callerRole == UserRole.VENDOR && authenticatedVendorId != null) {
                adjustmentRepository.observeVendorAdjustments(projectId, authenticatedVendorId, callerRole, authenticatedVendorId)
            } else {
                adjustmentRepository.observeAdjustments(projectId, callerRole)
            }

            flow.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load adjustments") }
            }.collect { adjustments ->
                val summaryRes = adjustmentRepository.getAdjustmentSummary(
                    projectId = projectId,
                    callerRole = callerRole,
                    authenticatedCustomerId = authenticatedCustomerId,
                    authenticatedVendorId = authenticatedVendorId
                )
                val summary = if (summaryRes is DomainResult.Success) summaryRes.data else null

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        adjustments = adjustments,
                        summary = summary,
                        filteredAdjustments = applyFilters(adjustments, state.searchQuery, state.selectedType, state.selectedStatus, state.selectedDirection)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredAdjustments = applyFilters(state.adjustments, query, state.selectedType, state.selectedStatus, state.selectedDirection)
            )
        }
    }

    fun onTypeSelected(type: FinancialAdjustmentType?) {
        _uiState.update { state ->
            val newType = if (state.selectedType == type) null else type
            state.copy(
                selectedType = newType,
                filteredAdjustments = applyFilters(state.adjustments, state.searchQuery, newType, state.selectedStatus, state.selectedDirection)
            )
        }
    }

    fun onStatusSelected(status: FinancialAdjustmentStatus?) {
        _uiState.update { state ->
            val newStatus = if (state.selectedStatus == status) null else status
            state.copy(
                selectedStatus = newStatus,
                filteredAdjustments = applyFilters(state.adjustments, state.searchQuery, state.selectedType, newStatus, state.selectedDirection)
            )
        }
    }

    fun onDirectionSelected(direction: FinancialAdjustmentDirection?) {
        _uiState.update { state ->
            val newDir = if (state.selectedDirection == direction) null else direction
            state.copy(
                selectedDirection = newDir,
                filteredAdjustments = applyFilters(state.adjustments, state.searchQuery, state.selectedType, state.selectedStatus, newDir)
            )
        }
    }

    private fun applyFilters(
        list: List<FinancialAdjustment>,
        query: String,
        type: FinancialAdjustmentType?,
        status: FinancialAdjustmentStatus?,
        direction: FinancialAdjustmentDirection?
    ): List<FinancialAdjustment> {
        return list.filter { adj ->
            val matchesQuery = query.isBlank() ||
                    adj.adjustmentNo.contains(query, ignoreCase = true) ||
                    adj.description.contains(query, ignoreCase = true) ||
                    adj.reason.contains(query, ignoreCase = true) ||
                    adj.referenceId.contains(query, ignoreCase = true) ||
                    (adj.customerId?.contains(query, ignoreCase = true) == true) ||
                    (adj.vendorId?.contains(query, ignoreCase = true) == true)

            val matchesType = type == null || adj.adjustmentType == type
            val matchesStatus = status == null || adj.status == status
            val matchesDirection = direction == null || adj.direction == direction

            matchesQuery && matchesType && matchesStatus && matchesDirection
        }
    }
}
