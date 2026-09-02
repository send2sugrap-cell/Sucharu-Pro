package com.sucharu.sucharupro.ui.features.customer.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.repository.FakeCustomerRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerAddress
import com.sucharu.sucharupro.domain.model.customer.CustomerAddressType
import com.sucharu.sucharupro.domain.model.customer.CustomerCreditProfile
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * ViewModel coordinating form state, validation, and repository mutations for Customer Create and Edit.
 */
class CustomerFormViewModel(
    private val customerId: String? = null,
    private val repository: CustomerRepository = FakeCustomerRepository(),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val formMode = if (customerId.isNullOrBlank()) CustomerFormMode.CREATE else CustomerFormMode.EDIT

    private val _formState = MutableStateFlow(
        CustomerFormState(
            mode = formMode,
            customerId = customerId,
            isLoading = formMode == CustomerFormMode.EDIT
        )
    )
    val formState: StateFlow<CustomerFormState> = _formState.asStateFlow()

    private var existingCustomer: Customer? = null

    init {
        if (formMode == CustomerFormMode.EDIT && !customerId.isNullOrBlank()) {
            loadExistingCustomer(customerId)
        }
    }

    private fun loadExistingCustomer(id: String) {
        scope.launch {
            repository.getCustomerById(id)
                .onStart {
                    _formState.value = _formState.value.copy(isLoading = true)
                }
                .catch { exception ->
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        errorMessage = exception.localizedMessage ?: "Failed to load customer record."
                    )
                }
                .collect { customer ->
                    if (customer != null) {
                        existingCustomer = customer
                        val primaryAddr = customer.primaryAddress
                        _formState.value = _formState.value.copy(
                            isLoading = false,
                            customerId = customer.customerId,
                            customerCode = customer.customerCode,
                            displayName = customer.displayName,
                            customerType = customer.customerType,
                            status = customer.status,
                            primaryPhone = customer.primaryPhone,
                            alternatePhone = customer.alternatePhone.orEmpty(),
                            email = customer.email.orEmpty(),
                            contactPersonName = customer.contactPersonName.orEmpty(),
                            addressLine = primaryAddr?.addressLine.orEmpty(),
                            area = primaryAddr?.area.orEmpty(),
                            city = primaryAddr?.city.orEmpty(),
                            district = primaryAddr?.district.orEmpty(),
                            postalCode = primaryAddr?.postalCode.orEmpty(),
                            country = primaryAddr?.country ?: "Bangladesh",
                            notes = customer.notes.orEmpty()
                        )
                    } else {
                        _formState.value = _formState.value.copy(
                            isLoading = false,
                            errorMessage = "Customer not found."
                        )
                    }
                }
        }
    }

    fun onDisplayNameChange(value: String) {
        _formState.value = _formState.value.copy(
            displayName = value,
            displayNameError = com.sucharu.sucharupro.core.validation.CustomerValidation.validateDisplayName(value)
        )
    }

    fun onCustomerTypeChange(type: CustomerType) {
        _formState.value = _formState.value.copy(customerType = type)
    }

    fun onStatusChange(status: CustomerStatusType) {
        _formState.value = _formState.value.copy(status = status)
    }

    fun onPrimaryPhoneChange(value: String) {
        _formState.value = _formState.value.copy(
            primaryPhone = value,
            primaryPhoneError = com.sucharu.sucharupro.core.validation.CustomerValidation.validatePrimaryPhone(value),
            duplicateWarning = null,
            duplicateAcknowledged = false
        )
    }

    fun onAlternatePhoneChange(value: String) {
        _formState.value = _formState.value.copy(
            alternatePhone = value,
            alternatePhoneError = com.sucharu.sucharupro.core.validation.CustomerValidation.validateAlternatePhone(value)
        )
    }

    fun onEmailChange(value: String) {
        _formState.value = _formState.value.copy(
            email = value,
            emailError = com.sucharu.sucharupro.core.validation.CustomerValidation.validateEmail(value),
            duplicateWarning = null,
            duplicateAcknowledged = false
        )
    }

    fun onContactPersonChange(value: String) {
        _formState.value = _formState.value.copy(contactPersonName = value)
    }

    fun onAddressLineChange(value: String) {
        _formState.value = _formState.value.copy(addressLine = value)
    }

    fun onAreaChange(value: String) {
        _formState.value = _formState.value.copy(area = value)
    }

    fun onCityChange(value: String) {
        _formState.value = _formState.value.copy(city = value)
    }

    fun onDistrictChange(value: String) {
        _formState.value = _formState.value.copy(district = value)
    }

    fun onPostalCodeChange(value: String) {
        _formState.value = _formState.value.copy(postalCode = value)
    }

    fun onCountryChange(value: String) {
        _formState.value = _formState.value.copy(country = value)
    }

    fun onNotesChange(value: String) {
        _formState.value = _formState.value.copy(notes = value)
    }

    fun dismissDuplicateWarning() {
        _formState.value = _formState.value.copy(
            duplicateWarning = null,
            duplicateCustomerId = null,
            duplicateAcknowledged = false
        )
    }

    fun acknowledgeDuplicateAndSave(onSuccess: (String) -> Unit) {
        _formState.value = _formState.value.copy(duplicateAcknowledged = true)
        saveCustomer(onSuccess)
    }

    /**
     * Validates form inputs and submits create/update mutation to [CustomerRepository].
     */
    fun saveCustomer(onSuccess: (String) -> Unit) {
        val state = _formState.value

        // Prevent duplicate submissions
        if (state.isSaving) return

        // Validate fields using central validation
        val nameError = com.sucharu.sucharupro.core.validation.CustomerValidation.validateDisplayName(state.displayName)
        val phoneError = com.sucharu.sucharupro.core.validation.CustomerValidation.validatePrimaryPhone(state.primaryPhone)
        val altPhoneError = com.sucharu.sucharupro.core.validation.CustomerValidation.validateAlternatePhone(state.alternatePhone)
        val emailError = com.sucharu.sucharupro.core.validation.CustomerValidation.validateEmail(state.email)

        if (nameError != null || phoneError != null || altPhoneError != null || emailError != null) {
            _formState.value = state.copy(
                displayNameError = nameError,
                primaryPhoneError = phoneError,
                alternatePhoneError = altPhoneError,
                emailError = emailError
            )
            return
        }

        scope.launch {
            // Check for duplicate if not previously acknowledged by user
            if (!state.duplicateAcknowledged) {
                val potentialDuplicate = repository.findDuplicateCustomer(
                    phone = state.primaryPhone,
                    email = state.email.ifBlank { null },
                    excludeCustomerId = state.customerId
                )

                if (potentialDuplicate != null) {
                    _formState.value = state.copy(
                        duplicateWarning = "A customer with matching contact info already exists: ${potentialDuplicate.displayName} (${potentialDuplicate.customerCode}). Save anyway to proceed.",
                        duplicateCustomerId = potentialDuplicate.customerId,
                        duplicateAcknowledged = false,
                        isSaving = false
                    )
                    return@launch
                }
            }

            _formState.value = state.copy(isSaving = true, errorMessage = null)
            val timestamp = Instant.now().toString()
            val addresses = if (state.addressLine.isNotBlank()) {
                listOf(
                    CustomerAddress(
                        addressLine = state.addressLine.trim(),
                        area = state.area.trim(),
                        city = state.city.trim(),
                        district = state.district.trim(),
                        postalCode = state.postalCode.trim(),
                        country = state.country.trim(),
                        addressType = CustomerAddressType.PRIMARY,
                        isDefault = true
                    )
                )
            } else {
                existingCustomer?.addresses ?: emptyList()
            }

            val result = if (formMode == CustomerFormMode.CREATE) {
                val newId = "cus-${System.currentTimeMillis() % 1000000}"
                val newCode = "CUS-${(100000..999999).random()}"
                val customerToCreate = Customer(
                    customerId = newId,
                    customerCode = newCode,
                    displayName = state.displayName.trim(),
                    customerType = state.customerType,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = state.primaryPhone.trim(),
                    alternatePhone = state.alternatePhone.trim().ifBlank { null },
                    email = state.email.trim().ifBlank { null },
                    contactPersonName = state.contactPersonName.trim().ifBlank { null },
                    addresses = addresses,
                    creditProfile = CustomerCreditProfile.DEFAULT_CASH_ONLY,
                    notes = state.notes.trim().ifBlank { null },
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
                repository.addCustomer(customerToCreate)
            } else {
                val customerToUpdate = Customer(
                    customerId = checkNotNull(state.customerId) { "Customer ID cannot be null in edit mode" },
                    customerCode = state.customerCode ?: existingCustomer?.customerCode ?: "CUS-000000",
                    displayName = state.displayName.trim(),
                    customerType = state.customerType,
                    status = state.status,
                    primaryPhone = state.primaryPhone.trim(),
                    alternatePhone = state.alternatePhone.trim().ifBlank { null },
                    email = state.email.trim().ifBlank { null },
                    contactPersonName = state.contactPersonName.trim().ifBlank { null },
                    addresses = addresses,
                    creditProfile = existingCustomer?.creditProfile ?: CustomerCreditProfile.DEFAULT_CASH_ONLY,
                    notes = state.notes.trim().ifBlank { null },
                    createdAt = existingCustomer?.createdAt ?: timestamp,
                    updatedAt = timestamp
                )
                repository.updateCustomer(customerToUpdate)
            }

            when (result) {
                is DomainResult.Success -> {
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                    onSuccess(result.data.customerId)
                }
                is DomainResult.Error -> {
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        errorMessage = result.message
                    )
                }
                DomainResult.Loading -> {
                    // Handled by isSaving
                }
            }
        }
    }
}
