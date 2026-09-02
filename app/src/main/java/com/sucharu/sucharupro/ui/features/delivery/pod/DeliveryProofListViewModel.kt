package com.sucharu.sucharupro.ui.features.delivery.pod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType
import com.sucharu.sucharupro.domain.repository.DeliveryProofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeliveryProofListViewModel(
    private val repository: DeliveryProofRepository,
    private val projectId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryProofListUiState(isLoading = true))
    val uiState: StateFlow<DeliveryProofListUiState> = _uiState.asStateFlow()

    init {
        loadProofs()
    }

    private fun loadProofs() {
        viewModelScope.launch {
            combine(
                repository.observeProofs(projectId),
                repository.observeProofSummary(projectId)
            ) { proofs, summary ->
                proofs to summary
            }.collect { (proofs, summary) ->
                _uiState.update { current ->
                    current.copy(
                        proofs = proofs,
                        summary = summary,
                        filteredProofs = filterProofs(
                            proofs = proofs,
                            query = current.searchQuery,
                            status = current.selectedStatus,
                            type = current.selectedProofType
                        ),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredProofs = filterProofs(current.proofs, query, current.selectedStatus, current.selectedProofType)
            )
        }
    }

    fun onStatusFilterChange(status: DeliveryProofStatus?) {
        _uiState.update { current ->
            current.copy(
                selectedStatus = status,
                filteredProofs = filterProofs(current.proofs, current.searchQuery, status, current.selectedProofType)
            )
        }
    }

    fun onProofTypeFilterChange(type: DeliveryProofType?) {
        _uiState.update { current ->
            current.copy(
                selectedProofType = type,
                filteredProofs = filterProofs(current.proofs, current.searchQuery, current.selectedStatus, type)
            )
        }
    }

    private fun filterProofs(
        proofs: List<DeliveryProof>,
        query: String,
        status: DeliveryProofStatus?,
        type: DeliveryProofType?
    ): List<DeliveryProof> {
        return proofs.filter { proof ->
            val matchesQuery = query.isBlank() ||
                    proof.proofNo.contains(query, ignoreCase = true) ||
                    proof.deliveryShipmentId.contains(query, ignoreCase = true) ||
                    proof.deliveryOrderId.contains(query, ignoreCase = true) ||
                    (proof.recipientName?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || proof.proofStatus == status
            val matchesType = type == null || proof.proofType == type

            matchesQuery && matchesStatus && matchesType
        }
    }
}
