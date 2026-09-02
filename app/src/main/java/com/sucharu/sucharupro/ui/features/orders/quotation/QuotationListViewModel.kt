package com.sucharu.sucharupro.ui.features.orders.quotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.data.repository.QuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.domain.repository.QuotationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * ViewModel managing presentation state, search, and filtering for Commercial Quotations.
 */
class QuotationListViewModel(
    private val repository: QuotationRepository = QuotationRepositoryImpl(FakeQuotationDataSource()),
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope: kotlinx.coroutines.CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<QuotationListUiState>(QuotationListUiState.Loading)
    val uiState: StateFlow<QuotationListUiState> = _uiState.asStateFlow()

    private var rawQuotations: List<Quotation> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentStatusFilter: QuotationStatusType? = null

    init {
        loadQuotations()
    }

    /** Observes the reactive quotations stream from the repository. */
    fun loadQuotations() {
        scope.launch {
            repository.getQuotations()
                .onStart {
                    _uiState.value = QuotationListUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = QuotationListUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load commercial quotations."
                    )
                }
                .collect { quotations ->
                    rawQuotations = quotations
                    if (quotations.isEmpty()) {
                        _uiState.value = QuotationListUiState.Empty()
                    } else {
                        updateFilteredState()
                    }
                }
        }
    }

    /** Handles search query text changes. */
    fun onSearchQueryChange(query: String) {
        currentSearchQuery = query
        updateFilteredState()
    }

    /** Handles quotation status filter changes. */
    fun onStatusFilterChange(status: QuotationStatusType?) {
        currentStatusFilter = status
        updateFilteredState()
    }

    /** Clears all active search and filter constraints. */
    fun clearFilters() {
        currentSearchQuery = ""
        currentStatusFilter = null
        updateFilteredState()
    }

    /** Retries fetching quotations. */
    fun retry() {
        loadQuotations()
    }

    private fun updateFilteredState() {
        if (rawQuotations.isEmpty()) {
            _uiState.value = QuotationListUiState.Empty()
            return
        }

        val trimmedQuery = currentSearchQuery.trim()

        val filtered = rawQuotations.filter { quotation ->
            val matchesSearch = trimmedQuery.isBlank() ||
                quotation.quotationNumber.contains(trimmedQuery, ignoreCase = true) ||
                quotation.quotationId.contains(trimmedQuery, ignoreCase = true) ||
                quotation.customerId.contains(trimmedQuery, ignoreCase = true) ||
                (quotation.inquiryId?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (quotation.termsAndConditions?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (quotation.approvedBy?.contains(trimmedQuery, ignoreCase = true) == true) ||
                quotation.items.any { item ->
                    item.description.contains(trimmedQuery, ignoreCase = true) ||
                        (item.specification?.contains(trimmedQuery, ignoreCase = true) == true)
                } ||
                quotation.revisions.any { rev ->
                    (rev.revisionReason?.contains(trimmedQuery, ignoreCase = true) == true) ||
                        (rev.notes?.contains(trimmedQuery, ignoreCase = true) == true)
                }

            val matchesStatus = currentStatusFilter == null || quotation.status == currentStatusFilter

            matchesSearch && matchesStatus
        }

        _uiState.value = QuotationListUiState.Success(
            allQuotations = rawQuotations,
            visibleQuotations = filtered,
            searchQuery = currentSearchQuery,
            selectedStatus = currentStatusFilter
        )
    }
}
