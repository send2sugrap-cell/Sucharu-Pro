package com.sucharu.sucharupro.ui.features.finance.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.Expense
import com.sucharu.sucharupro.domain.model.finance.ExpenseActivityEvent
import com.sucharu.sucharupro.domain.model.finance.ExpenseCategory
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ExpenseCategoryRepository
import com.sucharu.sucharupro.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseDetailsUiState(
    val isLoading: Boolean = true,
    val expense: Expense? = null,
    val category: ExpenseCategory? = null,
    val activityEvents: List<ExpenseActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

class ExpenseDetailsViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: ExpenseCategoryRepository,
    private val expenseId: String,
    private val callerRole: UserRole,
    private val currentActorId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseDetailsUiState())
    val uiState: StateFlow<ExpenseDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val expRes = expenseRepository.getExpenseById(expenseId, callerRole)
            if (expRes is DomainResult.Success) {
                val expense = expRes.data
                val catRes = categoryRepository.getCategoryById(expense.categoryId, callerRole)
                val category = if (catRes is DomainResult.Success) catRes.data else null

                val eventsRes = expenseRepository.getActivityEvents(expenseId, callerRole)
                val events = if (eventsRes is DomainResult.Success) eventsRes.data else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        expense = expense,
                        category = category,
                        activityEvents = events
                    )
                }
            } else if (expRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = expRes.message) }
            }
        }
    }

    fun submitExpense() {
        viewModelScope.launch {
            val res = expenseRepository.submitExpense(expenseId, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Expense submitted for approval.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun approveExpense() {
        viewModelScope.launch {
            val res = expenseRepository.approveExpense(expenseId, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Expense approved.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun postExpense(overrideAccountHead: String? = null) {
        viewModelScope.launch {
            val res = expenseRepository.postExpense(expenseId, overrideAccountHead, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Expense posted to financial ledger.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun rejectExpense(reason: String) {
        viewModelScope.launch {
            val res = expenseRepository.rejectExpense(expenseId, reason, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Expense rejected.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun cancelExpense(reason: String) {
        viewModelScope.launch {
            val res = expenseRepository.cancelExpense(expenseId, reason, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Expense cancelled.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }
}
