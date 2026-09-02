package com.sucharu.sucharupro.ui.features.inventory.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryStockIdentity
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating details for an individual Inventory Product (Module 07 Step 01).
 */
class InventoryProductDetailsViewModel(
    private val repository: InventoryProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryProductDetailsUiState())
    val uiState: StateFlow<InventoryProductDetailsUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: String, callerRole: UserRole = UserRole.MANAGER) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val prodResult = repository.getProductById(productId, callerRole)
            if (prodResult is DomainResult.Success) {
                val product = prodResult.data
                val stockIdentity = InventoryStockIdentity.fromProduct(product)
                val catId = product.categoryId
                val category = if (catId != null) {
                    val catRes = repository.getCategoryById(catId, callerRole)
                    if (catRes is DomainResult.Success) catRes.data else null
                } else null

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        product = product,
                        category = category,
                        stockIdentity = stockIdentity
                    )
                }
            } else if (prodResult is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = prodResult.message) }
            }
        }
    }

    fun activateProduct(actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        val currentProduct = _uiState.value.product ?: return
        viewModelScope.launch {
            val res = repository.activateProduct(currentProduct.id, actorId, timestamp, role)
            if (res is DomainResult.Success) {
                loadProduct(currentProduct.id, role)
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun deactivateProduct(actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        val currentProduct = _uiState.value.product ?: return
        viewModelScope.launch {
            val res = repository.deactivateProduct(currentProduct.id, actorId, timestamp, role)
            if (res is DomainResult.Success) {
                loadProduct(currentProduct.id, role)
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }
}
