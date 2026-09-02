package com.sucharu.sucharupro.ui.features.finance.payable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.finance.VendorPayableAgingBucket
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.model.finance.VendorPayableSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.VendorPayableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VendorPayableListUiState(
    val isLoading: Boolean = true,
    val payables: List<VendorPayable> = emptyList(),
    val filteredPayables: List<VendorPayable> = emptyList(),
    val summary: VendorPayableSummary? = null,
    val searchQuery: String = "",
    val selectedStatus: VendorPayableStatus? = null,
    val selectedAgingBucket: VendorPayableAgingBucket? = null,
    val errorMessage: String? = null
)

class VendorPayableListViewModel(
    private val repository: VendorPayableRepository,
    private val projectId: String,
    private val callerRole: UserRole,
    private val authenticatedVendorId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VendorPayableListUiState())
    val uiState: StateFlow<VendorPayableListUiState> = _uiState.asStateFlow()

    init {
        loadPayables()
        loadSummary()
    }

    fun loadPayables() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val flow = if (callerRole == UserRole.VENDOR && authenticatedVendorId != null) {
                repository.observeVendorPayables(projectId, authenticatedVendorId, callerRole, authenticatedVendorId)
            } else {
                repository.observePayables(projectId, callerRole)
            }

            flow.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load payables") }
            }.collect { list ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        payables = list,
                        filteredPayables = applyFilters(list, state.searchQuery, state.selectedStatus, state.selectedAgingBucket)
                    )
                }
            }
        }
    }

    fun loadSummary() {
        viewModelScope.launch {
            val vendorFilter = if (callerRole == UserRole.VENDOR) authenticatedVendorId else null
            val res = repository.getVendorPayableSummary(
                projectId = projectId,
                vendorId = vendorFilter,
                callerRole = callerRole,
                authenticatedVendorId = authenticatedVendorId
            )
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(summary = res.data) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredPayables = applyFilters(state.payables, query, state.selectedStatus, state.selectedAgingBucket)
            )
        }
    }

    fun onStatusSelected(status: VendorPayableStatus?) {
        _uiState.update { state ->
            val newStatus = if (state.selectedStatus == status) null else status
            state.copy(
                selectedStatus = newStatus,
                filteredPayables = applyFilters(state.payables, state.searchQuery, newStatus, state.selectedAgingBucket)
            )
        }
    }

    fun onAgingBucketSelected(bucket: VendorPayableAgingBucket?) {
        _uiState.update { state ->
            val newBucket = if (state.selectedAgingBucket == bucket) null else bucket
            state.copy(
                selectedAgingBucket = newBucket,
                filteredPayables = applyFilters(state.payables, state.searchQuery, state.selectedStatus, newBucket)
            )
        }
    }

    private fun applyFilters(
        list: List<VendorPayable>,
        query: String,
        status: VendorPayableStatus?,
        bucket: VendorPayableAgingBucket?
    ): List<VendorPayable> {
        return list.filter { payable ->
            val matchesQuery = query.isBlank() ||
                    payable.payableNo.contains(query, ignoreCase = true) ||
                    payable.vendorId.contains(query, ignoreCase = true) ||
                    payable.referenceId.contains(query, ignoreCase = true) ||
                    (payable.supplierInvoiceNo?.contains(query, ignoreCase = true) == true) ||
                    payable.description.contains(query, ignoreCase = true)

            val matchesStatus = status == null || payable.status == status
            val matchesBucket = bucket == null || payable.agingBucket == bucket

            matchesQuery && matchesStatus && matchesBucket
        }
    }
}
