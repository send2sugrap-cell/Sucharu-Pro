package com.sucharu.sucharupro.ui.features.communication.vendor.document

import com.sucharu.sucharupro.domain.model.communication.vendor.document.*

// =========================================================================
// Dashboard State
// =========================================================================
data class VendorDocumentDashboardUiState(
    val isLoading: Boolean = true,
    val totalDocuments: Int = 0,
    val pendingReview: Int = 0,
    val approved: Int = 0,
    val rejected: Int = 0,
    val expired: Int = 0,
    val expiringSoon: Int = 0,
    val nonCompliantVendors: Int = 0,
    val compliancePercentage: Double = 0.0,
    val recentActivity: List<VendorDocumentActivityEvent> = emptyList(),
    val error: String? = null
)

// =========================================================================
// Document Request List State
// =========================================================================
data class VendorDocumentRequestListUiState(
    val isLoading: Boolean = true,
    val requests: List<VendorDocumentRequest> = emptyList(),
    val filterStatus: VendorDocumentRequestStatus? = null,
    val filterDocumentType: VendorDocumentType? = null,
    val error: String? = null
)

// =========================================================================
// Document Request Details State
// =========================================================================
data class VendorDocumentRequestDetailsUiState(
    val isLoading: Boolean = true,
    val request: VendorDocumentRequest? = null,
    val error: String? = null
)

// =========================================================================
// Document Submit State
// =========================================================================
data class VendorDocumentSubmitUiState(
    val isLoading: Boolean = false,
    val requestId: String = "",
    val vendorId: String = "",
    val documentType: VendorDocumentType = VendorDocumentType.OTHER,
    val title: String = "",
    val description: String = "",
    val fileReferenceId: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val issueDate: Long? = null,
    val expiryDate: Long? = null,
    val notes: String = "",
    val isSuccess: Boolean = false,
    val error: String? = null
)

// =========================================================================
// Document List/Details State
// =========================================================================
data class VendorDocumentListUiState(
    val isLoading: Boolean = true,
    val documents: List<VendorDocument> = emptyList(),
    val filterStatus: VendorDocumentStatus? = null,
    val filterType: VendorDocumentType? = null,
    val error: String? = null
)

data class VendorDocumentDetailsUiState(
    val isLoading: Boolean = true,
    val document: VendorDocument? = null,
    val versions: List<VendorDocumentVersion> = emptyList(),
    val reviews: List<VendorDocumentReview> = emptyList(),
    val error: String? = null
)

// =========================================================================
// Review State
// =========================================================================
data class VendorDocumentReviewUiState(
    val isLoading: Boolean = false,
    val document: VendorDocument? = null,
    val remarks: String = "",
    val rejectionReason: String = "",
    val isSuccess: Boolean = false,
    val action: ReviewAction = ReviewAction.NONE,
    val error: String? = null
)

enum class ReviewAction { NONE, APPROVE, REJECT }

// =========================================================================
// Version History State
// =========================================================================
data class VendorDocumentVersionHistoryUiState(
    val isLoading: Boolean = true,
    val document: VendorDocument? = null,
    val versions: List<VendorDocumentVersion> = emptyList(),
    val error: String? = null
)

// =========================================================================
// Compliance State
// =========================================================================
data class VendorComplianceUiState(
    val isLoading: Boolean = true,
    val summaries: List<VendorComplianceSummary> = emptyList(),
    val selectedVendorId: String? = null,
    val selectedSummary: VendorComplianceSummary? = null,
    val error: String? = null
)

// =========================================================================
// Expiry State
// =========================================================================
data class VendorDocumentExpiryUiState(
    val isLoading: Boolean = true,
    val expiryInfoList: List<VendorDocumentExpiryInfo> = emptyList(),
    val filter: VendorDocumentExpiryStatus? = null,
    val error: String? = null
)

// =========================================================================
// Activity State
// =========================================================================
data class VendorDocumentActivityUiState(
    val isLoading: Boolean = true,
    val events: List<VendorDocumentActivityEvent> = emptyList(),
    val error: String? = null
)
