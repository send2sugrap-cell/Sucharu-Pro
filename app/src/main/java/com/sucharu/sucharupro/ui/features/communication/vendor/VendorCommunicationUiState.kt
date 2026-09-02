package com.sucharu.sucharupro.ui.features.communication.vendor

import com.sucharu.sucharupro.domain.model.communication.vendor.*

/**
 * UI state sealed hierarchy for Vendor Communication screens (Module 10 Step 05).
 */
sealed class VendorCommunicationUiState {
    data object Loading : VendorCommunicationUiState()
    data class Error(val message: String) : VendorCommunicationUiState()

    data class DashboardState(
        val summary: VendorCommunicationSummary? = null,
        val recentCommunications: List<VendorCommunication> = emptyList(),
        val engagementSummary: VendorEngagementSummary? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : VendorCommunicationUiState()

    data class ListState(
        val communications: List<VendorCommunication> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val filterType: VendorCommunicationType? = null,
        val filterStatus: VendorCommunicationStatus? = null,
        val searchQuery: String = ""
    ) : VendorCommunicationUiState()

    data class DetailsState(
        val communication: VendorCommunication? = null,
        val history: List<VendorCommunicationHistory> = emptyList(),
        val acknowledgement: VendorCommunicationAcknowledgement? = null,
        val readReceipt: VendorCommunicationReadReceipt? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : VendorCommunicationUiState()

    data class ComposeState(
        val vendorId: String = "",
        val subject: String = "",
        val message: String = "",
        val communicationType: VendorCommunicationType = VendorCommunicationType.GENERAL_MESSAGE,
        val isSubmitting: Boolean = false,
        val isSuccess: Boolean = false,
        val error: String? = null
    ) : VendorCommunicationUiState()

    data class EngagementState(
        val summary: VendorEngagementSummary? = null,
        val events: List<VendorEngagementEvent> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    ) : VendorCommunicationUiState()

    data class AcknowledgementState(
        val communication: VendorCommunication? = null,
        val acknowledgement: VendorCommunicationAcknowledgement? = null,
        val isSubmitting: Boolean = false,
        val isSuccess: Boolean = false,
        val error: String? = null
    ) : VendorCommunicationUiState()
}
