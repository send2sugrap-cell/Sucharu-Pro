package com.sucharu.sucharupro.ui.features.orders.inquiry.form

import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType

/**
 * Operating mode of the Inquiry Form.
 */
enum class InquiryFormMode {
    CREATE,
    EDIT
}

/**
 * UI State representation for Inquiry Create and Edit forms.
 */
data class InquiryFormUiState(
    val mode: InquiryFormMode = InquiryFormMode.CREATE,
    val inquiryId: String? = null,
    val inquiryNumber: String = "",
    val customerId: String = "",
    val customerName: String? = null,
    val availableCustomers: List<Customer> = emptyList(),
    val source: InquirySource = InquirySource.DIRECT_VISIT,
    val contactPerson: String = "",
    val contactPhone: String = "",
    val items: List<InquiryRequirement> = emptyList(),
    val notes: String = "",
    val status: InquiryStatusType = InquiryStatusType.NEW,
    val createdAt: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val savedInquiryId: String? = null,
    val errorMessage: String? = null,
    // Validation Errors
    val customerIdError: String? = null,
    val itemsError: String? = null,
    // Item Dialog State
    val isItemDialogOpen: Boolean = false,
    val editingItemIndex: Int? = null,
    val editingItem: InquiryRequirement? = null
) {
    val isEditMode: Boolean get() = mode == InquiryFormMode.EDIT
    val screenTitle: String get() = if (isEditMode) "Edit Inquiry" else "New Inquiry"
    val saveButtonText: String get() = if (isEditMode) "Save Changes" else "Create Inquiry"
}
