package com.sucharu.sucharupro.ui.features.orders.inquiry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.FakeInquiryDataSource
import com.sucharu.sucharupro.data.repository.InquiryRepositoryImpl
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.repository.InquiryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * ViewModel managing presentation state, search, and filtering for Customer Inquiries.
 */
class InquiryListViewModel(
    private val repository: InquiryRepository = InquiryRepositoryImpl(FakeInquiryDataSource()),
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope: kotlinx.coroutines.CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<InquiryListUiState>(InquiryListUiState.Loading)
    val uiState: StateFlow<InquiryListUiState> = _uiState.asStateFlow()

    private var rawInquiries: List<Inquiry> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentStatusFilter: InquiryStatusType? = null

    init {
        loadInquiries()
    }

    /** Observes the reactive inquiries stream from the repository. */
    fun loadInquiries() {
        scope.launch {
            repository.getInquiries()
                .onStart {
                    _uiState.value = InquiryListUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = InquiryListUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load customer inquiries."
                    )
                }
                .collect { inquiries ->
                    rawInquiries = inquiries
                    if (inquiries.isEmpty()) {
                        _uiState.value = InquiryListUiState.Empty()
                    } else {
                        updateFilteredState()
                    }
                }
        }
    }

    /** Handles search text changes. */
    fun onSearchQueryChange(query: String) {
        currentSearchQuery = query
        updateFilteredState()
    }

    /** Handles inquiry status filter changes. */
    fun onStatusFilterChange(status: InquiryStatusType?) {
        currentStatusFilter = status
        updateFilteredState()
    }

    /** Clears all active filters and search query. */
    fun clearFilters() {
        currentSearchQuery = ""
        currentStatusFilter = null
        updateFilteredState()
    }

    /** Retries loading data from repository. */
    fun retry() {
        loadInquiries()
    }

    private fun updateFilteredState() {
        if (rawInquiries.isEmpty()) {
            _uiState.value = InquiryListUiState.Empty()
            return
        }

        val trimmedQuery = currentSearchQuery.trim()

        val filtered = rawInquiries.filter { inquiry ->
            val matchesSearch = trimmedQuery.isBlank() ||
                inquiry.inquiryNumber.contains(trimmedQuery, ignoreCase = true) ||
                inquiry.inquiryId.contains(trimmedQuery, ignoreCase = true) ||
                inquiry.customerId.contains(trimmedQuery, ignoreCase = true) ||
                (inquiry.contactPerson?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (inquiry.contactPhone?.contains(trimmedQuery, ignoreCase = true) == true) ||
                (inquiry.notes?.contains(trimmedQuery, ignoreCase = true) == true) ||
                inquiry.items.any { item ->
                    item.productName.contains(trimmedQuery, ignoreCase = true) ||
                        item.description.contains(trimmedQuery, ignoreCase = true) ||
                        (item.notes?.contains(trimmedQuery, ignoreCase = true) == true)
                }

            val matchesStatus = currentStatusFilter == null || inquiry.status == currentStatusFilter

            matchesSearch && matchesStatus
        }

        _uiState.value = InquiryListUiState.Success(
            allInquiries = rawInquiries,
            visibleInquiries = filtered,
            searchQuery = currentSearchQuery,
            selectedStatus = currentStatusFilter
        )
    }
}
