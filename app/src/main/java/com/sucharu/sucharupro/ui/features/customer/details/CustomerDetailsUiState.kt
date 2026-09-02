package com.sucharu.sucharupro.ui.features.customer.details

import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.customer.CustomerNote

/**
 * UI State definition for the Customer Details / Profile screen.
 */
sealed interface CustomerDetailsUiState {

    /**
     * Initial loading state while fetching customer profile.
     */
    data object Loading : CustomerDetailsUiState

    /**
     * Successfully loaded customer profile with notes, activities, and follow-up data.
     */
    data class Success(
        val customer: Customer,
        val notes: List<CustomerNote> = emptyList(),
        val activities: List<CustomerActivity> = emptyList(),
        val isNoteDialogVisible: Boolean = false,
        val noteInputText: String = "",
        val isNoteImportantInput: Boolean = false,
        val editingNoteId: String? = null,
        val noteErrorMessage: String? = null,
        val isNoteSaving: Boolean = false,
        val isFollowUpDialogVisible: Boolean = false,
        val followUpInput: String = "",
        val isStatusConfirmDialogVisible: Boolean = false,
        val pendingStatus: com.sucharu.sucharupro.domain.model.customer.CustomerStatusType? = null
    ) : CustomerDetailsUiState

    /**
     * Customer record not found for the requested identifier.
     */
    data class NotFound(
        val customerId: String
    ) : CustomerDetailsUiState

    /**
     * Error state when data retrieval fails.
     */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : CustomerDetailsUiState
}
