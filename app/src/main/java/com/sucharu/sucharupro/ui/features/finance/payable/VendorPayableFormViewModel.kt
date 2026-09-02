package com.sucharu.sucharupro.ui.features.finance.payable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.VendorPayableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class VendorPayableFormUiState(
    val vendorId: String = "",
    val referenceType: FinancialReferenceType = FinancialReferenceType.PURCHASE,
    val referenceId: String = "",
    val supplierInvoiceNo: String = "",
    val amountText: String = "",
    val currency: String = "BDT",
    val dueDateOffsetDays: Int = 30,
    val description: String = "",
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdPayable: VendorPayable? = null
)

class VendorPayableFormViewModel(
    private val repository: VendorPayableRepository,
    private val projectId: String,
    private val callerRole: UserRole,
    private val currentActorId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(VendorPayableFormUiState())
    val uiState: StateFlow<VendorPayableFormUiState> = _uiState.asStateFlow()

    fun onVendorIdChanged(vendorId: String) = _uiState.update { it.copy(vendorId = vendorId) }
    fun onReferenceTypeChanged(type: FinancialReferenceType) = _uiState.update { it.copy(referenceType = type) }
    fun onReferenceIdChanged(refId: String) = _uiState.update { it.copy(referenceId = refId) }
    fun onSupplierInvoiceNoChanged(invNo: String) = _uiState.update { it.copy(supplierInvoiceNo = invNo) }
    fun onAmountChanged(amount: String) = _uiState.update { it.copy(amountText = amount) }
    fun onDueDateOffsetChanged(days: Int) = _uiState.update { it.copy(dueDateOffsetDays = days) }
    fun onDescriptionChanged(desc: String) = _uiState.update { it.copy(description = desc) }
    fun onNotesChanged(notes: String) = _uiState.update { it.copy(notes = notes) }

    fun submit() {
        val state = _uiState.value
        val amountBigDecimal = state.amountText.toBigDecimalOrNull()
        if (amountBigDecimal == null || amountBigDecimal <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid positive payable amount.") }
            return
        }

        val dueDate = System.currentTimeMillis() + (state.dueDateOffsetDays * 86400000L)

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val res = repository.createPayable(
                projectId = projectId,
                vendorId = state.vendorId.trim(),
                referenceType = state.referenceType,
                referenceId = state.referenceId.trim(),
                supplierInvoiceNo = state.supplierInvoiceNo.trim().ifEmpty { null },
                originalAmount = Money(amountBigDecimal),
                currency = state.currency,
                dueDate = dueDate,
                description = state.description.trim(),
                notes = state.notes.trim().ifEmpty { null },
                actorId = currentActorId,
                callerRole = callerRole
            )

            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isSubmitting = false, createdPayable = res.data) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
            }
        }
    }
}
