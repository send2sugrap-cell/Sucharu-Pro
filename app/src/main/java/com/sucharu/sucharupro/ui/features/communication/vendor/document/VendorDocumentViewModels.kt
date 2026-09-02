package com.sucharu.sucharupro.ui.features.communication.vendor.document

import androidx.lifecycle.ViewModel
import com.sucharu.sucharupro.domain.model.communication.vendor.document.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VendorDocumentDashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentDashboardUiState())
    val uiState: StateFlow<VendorDocumentDashboardUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        // Simulate loaded state with empty data
        _uiState.value = VendorDocumentDashboardUiState(isLoading = false)
    }
}

class VendorDocumentRequestListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentRequestListUiState())
    val uiState: StateFlow<VendorDocumentRequestListUiState> = _uiState.asStateFlow()

    fun applyFilter(status: VendorDocumentRequestStatus?) {
        _uiState.value = _uiState.value.copy(filterStatus = status)
    }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = false, requests = emptyList())
    }
}

class VendorDocumentRequestDetailsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentRequestDetailsUiState())
    val uiState: StateFlow<VendorDocumentRequestDetailsUiState> = _uiState.asStateFlow()

    fun load(requestId: String) {
        _uiState.value = _uiState.value.copy(isLoading = false)
    }
}

class VendorDocumentSubmitViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentSubmitUiState())
    val uiState: StateFlow<VendorDocumentSubmitUiState> = _uiState.asStateFlow()

    fun updateVendorId(value: String) { _uiState.value = _uiState.value.copy(vendorId = value) }
    fun updateTitle(value: String) { _uiState.value = _uiState.value.copy(title = value) }
    fun updateDescription(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun updateFileReferenceId(value: String) { _uiState.value = _uiState.value.copy(fileReferenceId = value) }
    fun updateFileName(value: String) { _uiState.value = _uiState.value.copy(fileName = value) }
    fun updateDocumentType(value: VendorDocumentType) { _uiState.value = _uiState.value.copy(documentType = value) }
    fun updateIssueDate(value: Long?) { _uiState.value = _uiState.value.copy(issueDate = value) }
    fun updateExpiryDate(value: Long?) { _uiState.value = _uiState.value.copy(expiryDate = value) }
    fun updateNotes(value: String) { _uiState.value = _uiState.value.copy(notes = value) }

    fun submit() {
        if (_uiState.value.fileReferenceId.isBlank() || _uiState.value.title.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Title and file reference are required.")
            return
        }
        _uiState.value = _uiState.value.copy(isSuccess = true, error = null)
    }
}

class VendorDocumentListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentListUiState())
    val uiState: StateFlow<VendorDocumentListUiState> = _uiState.asStateFlow()

    fun load() { _uiState.value = _uiState.value.copy(isLoading = false, documents = emptyList()) }
    fun applyFilter(status: VendorDocumentStatus?) { _uiState.value = _uiState.value.copy(filterStatus = status) }
}

class VendorDocumentDetailsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentDetailsUiState())
    val uiState: StateFlow<VendorDocumentDetailsUiState> = _uiState.asStateFlow()

    fun load(documentId: String) { _uiState.value = _uiState.value.copy(isLoading = false) }
}

class VendorDocumentReviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentReviewUiState())
    val uiState: StateFlow<VendorDocumentReviewUiState> = _uiState.asStateFlow()

    fun updateRemarks(value: String) { _uiState.value = _uiState.value.copy(remarks = value) }
    fun updateRejectionReason(value: String) { _uiState.value = _uiState.value.copy(rejectionReason = value) }

    fun approve() {
        _uiState.value = _uiState.value.copy(action = ReviewAction.APPROVE, isSuccess = true, error = null)
    }

    fun reject() {
        if (_uiState.value.rejectionReason.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Rejection reason is required.")
            return
        }
        _uiState.value = _uiState.value.copy(action = ReviewAction.REJECT, isSuccess = true, error = null)
    }
}

class VendorDocumentVersionHistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentVersionHistoryUiState())
    val uiState: StateFlow<VendorDocumentVersionHistoryUiState> = _uiState.asStateFlow()

    fun load(documentId: String) { _uiState.value = _uiState.value.copy(isLoading = false) }
}

class VendorComplianceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorComplianceUiState())
    val uiState: StateFlow<VendorComplianceUiState> = _uiState.asStateFlow()

    fun load() { _uiState.value = _uiState.value.copy(isLoading = false, summaries = emptyList()) }
    fun selectVendor(vendorId: String?) { _uiState.value = _uiState.value.copy(selectedVendorId = vendorId) }
}

class VendorDocumentExpiryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentExpiryUiState())
    val uiState: StateFlow<VendorDocumentExpiryUiState> = _uiState.asStateFlow()

    fun load() { _uiState.value = _uiState.value.copy(isLoading = false, expiryInfoList = emptyList()) }
    fun applyFilter(status: VendorDocumentExpiryStatus?) { _uiState.value = _uiState.value.copy(filter = status) }
}

class VendorDocumentActivityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VendorDocumentActivityUiState())
    val uiState: StateFlow<VendorDocumentActivityUiState> = _uiState.asStateFlow()

    fun load() { _uiState.value = _uiState.value.copy(isLoading = false, events = emptyList()) }
}
