package com.sucharu.sucharupro.ui.features.customer.form

import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType

/**
 * Mode of the customer form.
 */
enum class CustomerFormMode {
    CREATE,
    EDIT
}

/**
 * Form state for customer create and edit flows.
 */
data class CustomerFormState(
    val mode: CustomerFormMode = CustomerFormMode.CREATE,
    val customerId: String? = null,
    val customerCode: String? = null,

    // Core fields
    val displayName: String = "",
    val displayNameError: String? = null,

    val customerType: CustomerType = CustomerType.INDIVIDUAL,

    val status: CustomerStatusType = CustomerStatusType.ACTIVE,

    val primaryPhone: String = "",
    val primaryPhoneError: String? = null,

    val alternatePhone: String = "",
    val alternatePhoneError: String? = null,

    val email: String = "",
    val emailError: String? = null,

    val contactPersonName: String = "",

    // Primary address fields
    val addressLine: String = "",
    val area: String = "",
    val city: String = "",
    val district: String = "",
    val postalCode: String = "",
    val country: String = "Bangladesh",

    // Notes
    val notes: String = "",

    // Duplicate detection
    val duplicateWarning: String? = null,
    val duplicateCustomerId: String? = null,
    val duplicateAcknowledged: Boolean = false,

    // Operational states
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditMode: Boolean get() = mode == CustomerFormMode.EDIT
    val isValid: Boolean
        get() = displayNameError == null &&
            primaryPhoneError == null &&
            alternatePhoneError == null &&
            emailError == null &&
            displayName.isNotBlank() &&
            primaryPhone.isNotBlank()
}
