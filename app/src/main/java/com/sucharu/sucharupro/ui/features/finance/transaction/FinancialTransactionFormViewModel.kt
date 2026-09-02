package com.sucharu.sucharupro.ui.features.finance.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinancialTransactionFormViewModel(
    private val repository: FinancialTransactionRepository,
    private val projectId: String,
    private val currentUserId: String,
    private val currentUserRole: UserRole
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialTransactionFormUiState())
    val uiState: StateFlow<FinancialTransactionFormUiState> = _uiState.asStateFlow()

    fun onTransactionTypeChanged(type: FinancialTransactionType) {
        _uiState.update { it.copy(transactionType = type) }
    }

    fun onEntryTypeChanged(entryType: FinancialEntryType) {
        _uiState.update { it.copy(entryType = entryType) }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { it.copy(amountInput = amount) }
    }

    fun onReferenceTypeChanged(type: FinancialReferenceType) {
        _uiState.update { it.copy(referenceType = type) }
    }

    fun onReferenceIdChanged(refId: String) {
        _uiState.update { it.copy(referenceIdInput = refId) }
    }

    fun onCustomerIdChanged(custId: String) {
        _uiState.update { it.copy(customerIdInput = custId) }
    }

    fun onVendorIdChanged(vendorId: String) {
        _uiState.update { it.copy(vendorIdInput = vendorId) }
    }

    fun onDescriptionChanged(desc: String) {
        _uiState.update { it.copy(descriptionInput = desc) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    fun submitForm() {
        val state = _uiState.value
        val amountDouble = state.amountInput.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid positive amount.") }
            return
        }
        if (state.referenceIdInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Reference ID cannot be blank.") }
            return
        }
        if (state.descriptionInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Description cannot be blank.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = repository.createTransaction(
                projectId = projectId,
                transactionType = state.transactionType,
                entryType = state.entryType,
                amount = amountDouble.toMoney(),
                currency = "BDT",
                referenceType = state.referenceType,
                referenceId = state.referenceIdInput.trim(),
                customerId = state.customerIdInput.trim().ifEmpty { null },
                vendorId = state.vendorIdInput.trim().ifEmpty { null },
                description = state.descriptionInput.trim(),
                notes = state.notesInput.trim().ifEmpty { null },
                actorId = currentUserId,
                callerRole = currentUserRole
            )

            when (result) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            successTransactionId = result.data.transactionId,
                            errorMessage = null
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isSubmitting = true) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
