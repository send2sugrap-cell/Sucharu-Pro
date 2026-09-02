package com.sucharu.sucharupro.ui.features.inventory.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating reactive flows for the Inventory Product catalog list (Module 07 Step 01).
 */
class InventoryProductListViewModel(
    private val repository: InventoryProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryProductListUiState(isLoading = true))
    val uiState: StateFlow<InventoryProductListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeProducts(),
                repository.observeCategories()
            ) { products, categories ->
                Pair(products, categories)
            }.collect { (products, categories) ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        products = products,
                        query = current.searchQuery,
                        categoryFilter = current.selectedCategoryFilter,
                        typeFilter = current.selectedTypeFilter,
                        showInactive = current.showInactiveOnly
                    )
                    current.copy(
                        isLoading = false,
                        products = products,
                        categories = categories,
                        filteredProducts = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                products = current.products,
                query = query,
                categoryFilter = current.selectedCategoryFilter,
                typeFilter = current.selectedTypeFilter,
                showInactive = current.showInactiveOnly
            )
            current.copy(searchQuery = query, filteredProducts = filtered)
        }
    }

    fun onCategoryFilterChanged(categoryId: String?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                products = current.products,
                query = current.searchQuery,
                categoryFilter = categoryId,
                typeFilter = current.selectedTypeFilter,
                showInactive = current.showInactiveOnly
            )
            current.copy(selectedCategoryFilter = categoryId, filteredProducts = filtered)
        }
    }

    fun onTypeFilterChanged(type: InventoryProductType?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                products = current.products,
                query = current.searchQuery,
                categoryFilter = current.selectedCategoryFilter,
                typeFilter = type,
                showInactive = current.showInactiveOnly
            )
            current.copy(selectedTypeFilter = type, filteredProducts = filtered)
        }
    }

    fun onShowInactiveToggled(showInactive: Boolean) {
        _uiState.update { current ->
            val filtered = applyFilters(
                products = current.products,
                query = current.searchQuery,
                categoryFilter = current.selectedCategoryFilter,
                typeFilter = current.selectedTypeFilter,
                showInactive = showInactive
            )
            current.copy(showInactiveOnly = showInactive, filteredProducts = filtered)
        }
    }

    fun activateProduct(productId: String, actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        viewModelScope.launch {
            val res = repository.activateProduct(productId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun deactivateProduct(productId: String, actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        viewModelScope.launch {
            val res = repository.deactivateProduct(productId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applyFilters(
        products: List<InventoryProduct>,
        query: String,
        categoryFilter: String?,
        typeFilter: InventoryProductType?,
        showInactive: Boolean
    ): List<InventoryProduct> {
        val q = query.trim().lowercase()
        return products.filter { p ->
            val matchesActive = if (showInactive) !p.isActive else p.isActive
            val matchesQuery = q.isBlank() || p.name.lowercase().contains(q) || p.sku.lowercase().contains(q)
            val matchesCategory = categoryFilter == null || p.categoryId == categoryFilter
            val matchesType = typeFilter == null || p.productType == typeFilter
            matchesActive && matchesQuery && matchesCategory && matchesType
        }
    }
}
