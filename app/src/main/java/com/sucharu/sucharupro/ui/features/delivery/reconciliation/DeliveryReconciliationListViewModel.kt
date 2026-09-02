package com.sucharu.sucharupro.ui.features.delivery.reconciliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.repository.DeliveryReconciliationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeliveryReconciliationListViewModel(
    private val repository: DeliveryReconciliationRepository,
    private val projectId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryReconciliationListUiState(isLoading = true))
    val uiState: StateFlow<DeliveryReconciliationListUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val selectedStatus = MutableStateFlow<DeliveryReconciliationStatus?>(null)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.observeReconciliations(projectId),
                repository.observeSummary(projectId),
                searchQuery,
                selectedStatus
            ) { list, summary, query, status ->
                val filtered = list.filter { item ->
                    val matchesQuery = query.isBlank() ||
                            item.deliveryOrderId.contains(query, ignoreCase = true) ||
                            item.reconciliationId.contains(query, ignoreCase = true)
                    val matchesStatus = status == null || item.reconciliationStatus == status
                    matchesQuery && matchesStatus
                }
                DeliveryReconciliationListUiState(
                    reconciliations = filtered,
                    summary = summary,
                    searchQuery = query,
                    selectedStatusFilter = status,
                    isLoading = false,
                    errorMessage = null
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onStatusFilterSelected(status: DeliveryReconciliationStatus?) {
        selectedStatus.value = status
    }
}
