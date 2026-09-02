package com.sucharu.sucharupro.ui.features.orders.inquiry.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInquiryDataSource
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.InquiryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.InquiryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * ViewModel managing UI state, validation, and repository mutations for Inquiry Create and Edit.
 */
class InquiryFormViewModel(
    private val inquiryId: String? = null,
    private val inquiryRepository: InquiryRepository = InquiryRepositoryImpl(FakeInquiryDataSource()),
    private val customerRepository: CustomerRepository = CustomerRepositoryImpl(FakeCustomerDataSource()),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val formMode = if (inquiryId.isNullOrBlank()) InquiryFormMode.CREATE else InquiryFormMode.EDIT

    private val _uiState = MutableStateFlow(
        InquiryFormUiState(
            mode = formMode,
            inquiryId = inquiryId,
            isLoading = formMode == InquiryFormMode.EDIT
        )
    )
    val uiState: StateFlow<InquiryFormUiState> = _uiState.asStateFlow()

    private var existingInquiry: Inquiry? = null

    init {
        loadAvailableCustomers()
        if (formMode == InquiryFormMode.EDIT && !inquiryId.isNullOrBlank()) {
            loadExistingInquiry(inquiryId)
        }
    }

    private fun loadAvailableCustomers() {
        customerRepository.getCustomers()
            .onEach { customers ->
                _uiState.value = _uiState.value.copy(
                    availableCustomers = customers,
                    customerName = customers.find { it.customerId == _uiState.value.customerId }?.displayName
                )
            }
            .catch {
                // Silently ignore customer load error; customer selector remains functional with empty list
            }
            .launchIn(scope)
    }

    private fun loadExistingInquiry(id: String) {
        scope.launch {
            inquiryRepository.getInquiryById(id)
                .catch { ex ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = ex.localizedMessage ?: "Failed to load inquiry."
                    )
                }
                .collect { inquiry ->
                    if (inquiry != null) {
                        existingInquiry = inquiry
                        val matchedCustomer = _uiState.value.availableCustomers.find { it.customerId == inquiry.customerId }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            inquiryId = inquiry.inquiryId,
                            inquiryNumber = inquiry.inquiryNumber,
                            customerId = inquiry.customerId,
                            customerName = matchedCustomer?.displayName,
                            source = inquiry.source,
                            contactPerson = inquiry.contactPerson.orEmpty(),
                            contactPhone = inquiry.contactPhone.orEmpty(),
                            items = inquiry.items,
                            notes = inquiry.notes.orEmpty(),
                            status = inquiry.status,
                            createdAt = inquiry.createdAt
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Inquiry not found."
                        )
                    }
                }
        }
    }

    fun onCustomerSelected(customer: Customer) {
        _uiState.value = _uiState.value.copy(
            customerId = customer.customerId,
            customerName = customer.displayName,
            contactPerson = if (_uiState.value.contactPerson.isBlank()) customer.contactPersonName.orEmpty() else _uiState.value.contactPerson,
            contactPhone = if (_uiState.value.contactPhone.isBlank()) customer.primaryPhone else _uiState.value.contactPhone,
            customerIdError = null
        )
    }

    fun onSourceChange(source: InquirySource) {
        _uiState.value = _uiState.value.copy(source = source)
    }

    fun onContactPersonChange(value: String) {
        _uiState.value = _uiState.value.copy(contactPerson = value)
    }

    fun onContactPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(contactPhone = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    // --- Requirement Items Management ---

    fun openAddItemDialog() {
        _uiState.value = _uiState.value.copy(
            isItemDialogOpen = true,
            editingItemIndex = null,
            editingItem = null
        )
    }

    fun openEditItemDialog(index: Int) {
        val currentItems = _uiState.value.items
        if (index in currentItems.indices) {
            _uiState.value = _uiState.value.copy(
                isItemDialogOpen = true,
                editingItemIndex = index,
                editingItem = currentItems[index]
            )
        }
    }

    fun closeItemDialog() {
        _uiState.value = _uiState.value.copy(
            isItemDialogOpen = false,
            editingItemIndex = null,
            editingItem = null
        )
    }

    fun saveItem(item: InquiryRequirement) {
        val currentList = _uiState.value.items.toMutableList()
        val index = _uiState.value.editingItemIndex

        if (index != null && index in currentList.indices) {
            currentList[index] = item
        } else {
            currentList.add(item)
        }

        _uiState.value = _uiState.value.copy(
            items = currentList,
            itemsError = null,
            isItemDialogOpen = false,
            editingItemIndex = null,
            editingItem = null
        )
    }

    fun removeItem(index: Int) {
        val currentList = _uiState.value.items.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _uiState.value = _uiState.value.copy(items = currentList)
        }
    }

    // --- Save Inquiry ---

    fun saveInquiry(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return

        // Field Validations
        var hasError = false
        val customerError = if (state.customerId.isBlank()) {
            hasError = true
            "Customer selection is required."
        } else null

        val itemsError = if (state.items.isEmpty()) {
            hasError = true
            "At least one specification requirement is required."
        } else null

        if (hasError) {
            _uiState.value = state.copy(
                customerIdError = customerError,
                itemsError = itemsError
            )
            return
        }

        scope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val timestamp = Instant.now().toString()

            val result = if (formMode == InquiryFormMode.CREATE) {
                val newId = "inq-${UUID.randomUUID().toString().take(8)}"
                val newNumber = "INQ-${(1000..9999).random()}"
                val inquiryToCreate = Inquiry(
                    inquiryId = newId,
                    inquiryNumber = newNumber,
                    customerId = state.customerId.trim(),
                    source = state.source,
                    status = InquiryStatusType.NEW,
                    contactPerson = state.contactPerson.trim().ifBlank { null },
                    contactPhone = state.contactPhone.trim().ifBlank { null },
                    items = state.items,
                    notes = state.notes.trim().ifBlank { null },
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
                inquiryRepository.createInquiry(inquiryToCreate)
            } else {
                val current = existingInquiry
                val inquiryToUpdate = Inquiry(
                    inquiryId = checkNotNull(state.inquiryId) { "Inquiry ID cannot be null in edit mode." },
                    inquiryNumber = current?.inquiryNumber ?: state.inquiryNumber.ifBlank { "INQ-0000" },
                    customerId = state.customerId.trim(),
                    source = state.source,
                    status = current?.status ?: state.status,
                    contactPerson = state.contactPerson.trim().ifBlank { null },
                    contactPhone = state.contactPhone.trim().ifBlank { null },
                    items = state.items,
                    notes = state.notes.trim().ifBlank { null },
                    createdAt = current?.createdAt ?: state.createdAt ?: timestamp,
                    updatedAt = timestamp
                )
                inquiryRepository.updateInquiry(inquiryToUpdate)
            }

            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true,
                        savedInquiryId = result.data.inquiryId
                    )
                    onSuccess(result.data.inquiryId)
                }
                is DomainResult.Error -> {
                    _uiState.value = _uiState.value.copy(
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
