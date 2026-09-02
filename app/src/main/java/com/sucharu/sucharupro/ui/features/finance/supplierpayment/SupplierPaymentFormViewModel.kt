package com.sucharu.sucharupro.ui.features.finance.supplierpayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.SupplierPayment
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.SupplierPaymentRepository
import com.sucharu.sucharupro.domain.repository.VendorPayableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class SupplierPaymentFormUiState(
    val vendorId: String = "",
    val payableId: String = "",
    val selectedPayable: VendorPayable? = null,
    val amountText: String = "",
    val currency: String = "BDT",
    val paymentMethod: SupplierPaymentMethod = SupplierPaymentMethod.BANK_TRANSFER,
    val paymentReference: String = "",
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdPayment: SupplierPayment? = null
)

class SupplierPaymentFormViewModel(
    private val paymentRepository: SupplierPaymentRepository,
    private val payableRepository: VendorPayableRepository,
    private val projectId: String,
    private val callerRole: UserRole,
    private val currentActorId: String,
    initialPayableId: String? = null,
    initialVendorId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SupplierPaymentFormUiState(
            vendorId = initialVendorId ?: "",
            payableId = initialPayableId ?: ""
        )
    )
    val uiState: StateFlow<SupplierPaymentFormUiState> = _uiState.asStateFlow()

    init {
        if (!initialPayableId.isNullOrBlank()) {
            loadPayableDetails(initialPayableId)
        }
    }

    fun onVendorIdChanged(vendorId: String) = _uiState.update { it.copy(vendorId = vendorId) }

    fun onPayableIdChanged(payableId: String) {
        _uiState.update { it.copy(payableId = payableId) }
        loadPayableDetails(payableId)
    }

    fun onAmountChanged(amount: String) = _uiState.update { it.copy(amountText = amount) }
    fun onPaymentMethodChanged(method: SupplierPaymentMethod) = _uiState.update { it.copy(paymentMethod = method) }
    fun onPaymentReferenceChanged(ref: String) = _uiState.update { it.copy(paymentReference = ref) }
    fun onNotesChanged(notes: String) = _uiState.update { it.copy(notes = notes) }

    private fun loadPayableDetails(payableId: String) {
        viewModelScope.launch {
            val res = payableRepository.getPayableById(payableId.trim(), callerRole)
            if (res is DomainResult.Success) {
                val payable = res.data
                _uiState.update {
                    it.copy(
                        selectedPayable = payable,
                        vendorId = payable.vendorId,
                        amountText = payable.outstandingAmount.formatted()
                    )
                }
            }
        }
    }

    fun submit() {
        val state = _uiState.value
        val amountBigDecimal = state.amountText.toBigDecimalOrNull()
        if (amountBigDecimal == null || amountBigDecimal <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid positive payment amount.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val res = paymentRepository.createPayment(
                projectId = projectId,
                vendorId = state.vendorId.trim(),
                payableId = state.payableId.trim(),
                amount = Money(amountBigDecimal),
                currency = state.currency,
                paymentMethod = state.paymentMethod,
                paymentReference = state.paymentReference.trim().ifEmpty { null },
                paymentDate = System.currentTimeMillis(),
                notes = state.notes.trim().ifEmpty { null },
                actorId = currentActorId,
                callerRole = callerRole
            )

            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isSubmitting = false, createdPayment = res.data) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
            }
        }
    }
}
