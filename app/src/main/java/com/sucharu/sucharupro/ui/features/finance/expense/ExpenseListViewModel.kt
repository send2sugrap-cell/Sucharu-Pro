package com.sucharu.sucharupro.ui.features.finance.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.Expense
import com.sucharu.sucharupro.domain.model.finance.ExpenseCategory
import com.sucharu.sucharupro.domain.model.finance.ExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.finance.ExpenseStatus
import com.sucharu.sucharupro.domain.model.finance.ExpenseSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ExpenseCategoryRepository
import com.sucharu.sucharupro.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseListUiState(
    val isLoading: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val categories: List<ExpenseCategory> = emptyList(),
    val summary: ExpenseSummary? = null,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val selectedStatus: ExpenseStatus? = null,
    val selectedMethod: ExpensePaymentMethod? = null,
    val errorMessage: String? = null
)

class ExpenseListViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: ExpenseCategoryRepository,
    private val projectId: String,
    private val callerRole: UserRole,
    private val currentActorId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()

    init {
        seedDefaultsAndLoad()
    }

    private fun seedDefaultsAndLoad() {
        viewModelScope.launch {
            categoryRepository.seedDefaultCategoriesIfEmpty(projectId, currentActorId, callerRole)
            loadData()
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            combine(
                expenseRepository.observeExpenses(projectId, callerRole),
                categoryRepository.observeCategories(projectId, callerRole)
            ) { expenses, categories ->
                expenses to categories
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load expenses") }
            }.collect { (expenses, categories) ->
                val summaryRes = expenseRepository.getExpenseSummary(projectId = projectId, callerRole = callerRole)
                val summary = if (summaryRes is DomainResult.Success) summaryRes.data else null

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        expenses = expenses,
                        categories = categories,
                        summary = summary,
                        filteredExpenses = applyFilters(expenses, state.searchQuery, state.selectedCategoryId, state.selectedStatus, state.selectedMethod)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredExpenses = applyFilters(state.expenses, query, state.selectedCategoryId, state.selectedStatus, state.selectedMethod)
            )
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { state ->
            val newCat = if (state.selectedCategoryId == categoryId) null else categoryId
            state.copy(
                selectedCategoryId = newCat,
                filteredExpenses = applyFilters(state.expenses, state.searchQuery, newCat, state.selectedStatus, state.selectedMethod)
            )
        }
    }

    fun onStatusSelected(status: ExpenseStatus?) {
        _uiState.update { state ->
            val newStatus = if (state.selectedStatus == status) null else status
            state.copy(
                selectedStatus = newStatus,
                filteredExpenses = applyFilters(state.expenses, state.searchQuery, state.selectedCategoryId, newStatus, state.selectedMethod)
            )
        }
    }

    fun onMethodSelected(method: ExpensePaymentMethod?) {
        _uiState.update { state ->
            val newMethod = if (state.selectedMethod == method) null else method
            state.copy(
                selectedMethod = newMethod,
                filteredExpenses = applyFilters(state.expenses, state.searchQuery, state.selectedCategoryId, state.selectedStatus, newMethod)
            )
        }
    }

    private fun applyFilters(
        list: List<Expense>,
        query: String,
        categoryId: String?,
        status: ExpenseStatus?,
        method: ExpensePaymentMethod?
    ): List<Expense> {
        return list.filter { expense ->
            val matchesQuery = query.isBlank() ||
                    expense.expenseNo.contains(query, ignoreCase = true) ||
                    expense.description.contains(query, ignoreCase = true) ||
                    (expense.vendorId?.contains(query, ignoreCase = true) == true) ||
                    (expense.paymentReference?.contains(query, ignoreCase = true) == true)

            val matchesCategory = categoryId == null || expense.categoryId == categoryId
            val matchesStatus = status == null || expense.status == status
            val matchesMethod = method == null || expense.paymentMethod == method

            matchesQuery && matchesCategory && matchesStatus && matchesMethod
        }
    }
}
