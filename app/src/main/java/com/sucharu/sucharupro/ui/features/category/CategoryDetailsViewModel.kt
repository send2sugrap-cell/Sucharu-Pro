package com.sucharu.sucharupro.ui.features.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.repository.category.CategoryDetailsRepository
import com.sucharu.sucharupro.data.repository.category.CategoryDetailsRepositoryImpl
import com.sucharu.sucharupro.data.repository.category.CategoryItemDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

sealed interface CategoryDetailsUiState {
    object Loading : CategoryDetailsUiState
    data class Success(val items: List<CategoryItemDetail>) : CategoryDetailsUiState
    data class Error(val message: String) : CategoryDetailsUiState
    object Empty : CategoryDetailsUiState
}

class CategoryDetailsViewModel(
    private val repository: CategoryDetailsRepository = CategoryDetailsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryDetailsUiState>(CategoryDetailsUiState.Loading)
    val uiState: StateFlow<CategoryDetailsUiState> = _uiState.asStateFlow()

    fun loadServicesCategory(categoryKey: String) {
        viewModelScope.launch {
            repository.getCategoryServices(categoryKey)
                .onStart { _uiState.value = CategoryDetailsUiState.Loading }
                .catch { _uiState.value = CategoryDetailsUiState.Error(it.localizedMessage ?: "Failed to load services") }
                .collect { list ->
                    _uiState.value = if (list.isEmpty()) CategoryDetailsUiState.Empty else CategoryDetailsUiState.Success(list)
                }
        }
    }

    fun loadProductsCategory(categoryKey: String) {
        viewModelScope.launch {
            repository.getCategoryProducts(categoryKey)
                .onStart { _uiState.value = CategoryDetailsUiState.Loading }
                .catch { _uiState.value = CategoryDetailsUiState.Error(it.localizedMessage ?: "Failed to load products") }
                .collect { list ->
                    _uiState.value = if (list.isEmpty()) CategoryDetailsUiState.Empty else CategoryDetailsUiState.Success(list)
                }
        }
    }
}
