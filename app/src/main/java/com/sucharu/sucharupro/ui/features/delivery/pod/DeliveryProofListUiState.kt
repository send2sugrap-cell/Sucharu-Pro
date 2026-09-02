package com.sucharu.sucharupro.ui.features.delivery.pod

import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofSummary
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType

data class DeliveryProofListUiState(
    val proofs: List<DeliveryProof> = emptyList(),
    val filteredProofs: List<DeliveryProof> = emptyList(),
    val summary: DeliveryProofSummary = DeliveryProofSummary(),
    val searchQuery: String = "",
    val selectedStatus: DeliveryProofStatus? = null,
    val selectedProofType: DeliveryProofType? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
