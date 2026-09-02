package com.sucharu.sucharupro.ui.features.delivery.verification

import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationSummary

/**
 * UI State for Delivery Item Verification Details (Module 08 Step 04).
 */
data class DeliveryItemVerificationDetailsUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val verification: DeliveryItemVerification? = null,
    val lines: List<DeliveryItemVerificationLine> = emptyList(),
    val summary: DeliveryItemVerificationSummary? = null,
    val activityEvents: List<DeliveryItemVerificationActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)
