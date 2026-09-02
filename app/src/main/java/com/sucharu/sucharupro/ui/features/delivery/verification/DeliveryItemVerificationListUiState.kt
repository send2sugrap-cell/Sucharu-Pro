package com.sucharu.sucharupro.ui.features.delivery.verification

import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus

/**
 * UI State for Delivery Item Verification List (Module 08 Step 04).
 */
data class DeliveryItemVerificationListUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val verifications: List<DeliveryItemVerification> = emptyList(),
    val filteredVerifications: List<DeliveryItemVerification> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: DeliveryItemVerificationStatus? = null,
    val errorMessage: String? = null
)
