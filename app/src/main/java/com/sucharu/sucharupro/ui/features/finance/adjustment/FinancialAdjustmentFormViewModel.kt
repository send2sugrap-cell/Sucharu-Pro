package com.sucharu.sucharupro.ui.features.finance.adjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustment
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialAdjustmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class FinancialAdjustmentFormUiState(
    val adjustmentType: FinancialAdjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
    val direction: FinancialAdjustmentDirection = FinancialAdjustmentDirection.CREDIT,
    val amountText: String = "",
    val currency: String = "BDT",
    val customerId: String = "",
    val vendorId: String = "",
    val referenceType: FinancialReferenceType = FinancialReferenceType.INVOICE,
    val referenceId: String = "",
    val reasonCode: String = "BILLING_ERROR",
    val reason: String = "",
    val description: String = "",
    val notes: String = "",
    val relatedReceivableId: String = "",
    val relatedPayableId: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdAdjustment: FinancialAdjustment? = null
)

class FinancialAdjustmentFormViewModel(
    private val adjustmentRepository: FinancialAdjustmentRepository,
    private val projectId: String,
    private val callerRole: UserRole,
    private val currentActorId: String,
    initialType: FinancialAdjustmentType? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FinancialAdjustmentFormUiState(
            adjustmentType = initialType ?: FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            direction = (initialType ?: FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE).defaultDirection
        )
    )
    val uiState: StateFlow<FinancialAdjustmentFormUiState> = _uiState.asStateFlow()

    fun onTypeChanged(type: FinancialAdjustmentType) = _uiState.update {
        it.copy(
            adjustmentType = type,
            direction = type.defaultDirection,
            referenceType = if (type.isVendorFacing) FinancialReferenceType.VENDOR_BILL else FinancialReferenceType.INVOICE
        )
    }

    fun onDirectionChanged(dir: FinancialAdjustmentDirection) = _uiState.update { it.copy(direction = dir) }
    fun onAmountChanged(amount: String) = _uiState.update { it.copy(amountText = amount) }
    fun onCustomerIdChanged(id: String) = _uiState.update { it.copy(customerId = id) }
    fun onVendorIdChanged(id: String) = _uiState.update { it.copy(vendorId = id) }
    fun onReferenceTypeChanged(refType: FinancialReferenceType) = _uiState.update { it.copy(referenceType = refType) }
    fun onReferenceIdChanged(refId: String) = _uiState.update { it.copy(referenceId = refId) }
    fun onReasonCodeChanged(code: String) = _uiState.update { it.copy(reasonCode = code) }
    fun onReasonChanged(reason: String) = _uiState.update { it.copy(reason = reason) }
    fun onDescriptionChanged(desc: String) = _uiState.update { it.copy(description = desc) }
    fun onNotesChanged(notes: String) = _uiState.update { it.copy(notes = notes) }
    fun onRelatedReceivableChanged(id: String) = _uiState.update { it.copy(relatedReceivableId = id) }
    fun onRelatedPayableChanged(id: String) = _uiState.update { it.copy(relatedPayableId = id) }

    fun submit() {
        val state = _uiState.value
        val amountBigDecimal = state.amountText.toBigDecimalOrNull()
        if (amountBigDecimal == null || amountBigDecimal <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid positive adjustment amount.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val res = adjustmentRepository.createAdjustment(
                projectId = projectId,
                adjustmentType = state.adjustmentType,
                direction = state.direction,
                amount = Money(amountBigDecimal),
                currency = state.currency,
                customerId = state.customerId.trim().ifEmpty { null },
                vendorId = state.vendorId.trim().ifEmpty { null },
                referenceType = state.referenceType,
                referenceId = state.referenceId.trim(),
                reasonCode = state.reasonCode.trim(),
                reason = state.reason.trim(),
                description = state.description.trim(),
                notes = state.notes.trim().ifEmpty { null },
                relatedReceivableId = state.relatedReceivableId.trim().ifEmpty { null },
                relatedPayableId = state.relatedPayableId.trim().ifEmpty { null },
                actorId = currentActorId,
                callerRole = callerRole
            )

            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isSubmitting = false, createdAdjustment = res.data) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
            }
        }
    }
}
