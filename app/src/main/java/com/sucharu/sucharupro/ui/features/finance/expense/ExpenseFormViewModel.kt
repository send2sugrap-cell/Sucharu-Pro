package com.sucharu.sucharupro.ui.features.finance.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.Expense
import com.sucharu.sucharupro.domain.model.finance.ExpenseCategory
import com.sucharu.sucharupro.domain.model.finance.ExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ExpenseCategoryRepository
import com.sucharu.sucharupro.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class ExpenseFormUiState(
    val categoryId: String = "",
    val categories: List<ExpenseCategory> = emptyList(),
    val amountText: String = "",
    val currency: String = "BDT",
    val description: String = "",
    val notes: String = "",
    val paymentMethod: ExpensePaymentMethod = ExpensePaymentMethod.CASH,
    val paymentReference: String = "",
    val vendorId: String = "",
    val referenceType: FinancialReferenceType? = null,
    val referenceId: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdExpense: Expense? = null
)

class ExpenseFormViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: ExpenseCategoryRepository,
    private val projectId: String,
    private val callerRole: UserRole,
    private val currentActorId: String,
    initialCategoryId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ExpenseFormUiState(categoryId = initialCategoryId ?: "")
    )
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val cats = categoryRepository.observeCategories(projectId, callerRole).first()
            _uiState.update { state ->
                val chosenCat = if (state.categoryId.isNotBlank()) state.categoryId else cats.firstOrNull()?.categoryId ?: ""
                state.copy(categories = cats, categoryId = chosenCat)
            }
        }
    }

    fun onCategoryChanged(catId: String) = _uiState.update { it.copy(categoryId = catId) }
    fun onAmountChanged(amount: String) = _uiState.update { it.copy(amountText = amount) }
    fun onDescriptionChanged(desc: String) = _uiState.update { it.copy(description = desc) }
    fun onNotesChanged(notes: String) = _uiState.update { it.copy(notes = notes) }
    fun onPaymentMethodChanged(method: ExpensePaymentMethod) = _uiState.update { it.copy(paymentMethod = method) }
    fun onPaymentReferenceChanged(ref: String) = _uiState.update { it.copy(paymentReference = ref) }
    fun onVendorIdChanged(vend: String) = _uiState.update { it.copy(vendorId = vend) }
    fun onReferenceTypeChanged(type: FinancialReferenceType?) = _uiState.update { it.copy(referenceType = type) }
    fun onReferenceIdChanged(refId: String) = _uiState.update { it.copy(referenceId = refId) }

    fun submit() {
        val state = _uiState.value
        val amountBigDecimal = state.amountText.toBigDecimalOrNull()
        if (amountBigDecimal == null || amountBigDecimal <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid positive expense amount.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val res = expenseRepository.createExpense(
                projectId = projectId,
                categoryId = state.categoryId.trim(),
                amount = Money(amountBigDecimal),
                currency = state.currency,
                description = state.description.trim(),
                paymentMethod = state.paymentMethod,
                paymentReference = state.paymentReference.trim().ifEmpty { null },
                vendorId = state.vendorId.trim().ifEmpty { null },
                referenceType = state.referenceType,
                referenceId = state.referenceId.trim().ifEmpty { null },
                expenseDate = System.currentTimeMillis(),
                notes = state.notes.trim().ifEmpty { null },
                actorId = currentActorId,
                callerRole = callerRole
            )

            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isSubmitting = false, createdExpense = res.data) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = res.message) }
            }
        }
    }
}
