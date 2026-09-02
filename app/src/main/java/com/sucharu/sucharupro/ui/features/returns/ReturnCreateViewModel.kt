package com.sucharu.sucharupro.ui.features.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ReturnRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Return Request Creation (Module 11 Step 02).
 */
class ReturnCreateViewModel(
    private val repository: ReturnRepository,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ReturnCreateUiState())
    val uiState: StateFlow<ReturnCreateUiState> = _uiState.asStateFlow()

    fun initialize(projectId: String, customerId: String = "") {
        _uiState.update {
            it.copy(
                projectId = projectId,
                customerId = if (customerId.isNotBlank()) customerId else it.customerId
            )
        }
    }

    fun onCustomerIdChanged(customerId: String) {
        _uiState.update { it.copy(customerId = customerId, errorMessage = null) }
    }

    fun onOriginalChallanIdChanged(challanId: String) {
        _uiState.update { it.copy(originalChallanId = challanId, errorMessage = null) }
    }

    fun onOriginalChallanItemIdChanged(challanItemId: String) {
        _uiState.update { it.copy(originalChallanItemId = challanItemId, errorMessage = null) }
    }

    fun onProductIdChanged(productId: String) {
        _uiState.update { it.copy(productId = productId, errorMessage = null) }
    }

    fun onQuantityChanged(quantityText: String) {
        _uiState.update { it.copy(requestedQuantityText = quantityText, errorMessage = null) }
    }

    fun onUnitChanged(unit: String) {
        _uiState.update { it.copy(unit = unit, errorMessage = null) }
    }

    fun onReasonChanged(reason: ReturnReason) {
        _uiState.update { it.copy(reason = reason, errorMessage = null) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes, errorMessage = null) }
    }

    fun submitReturnRequest(
        actorId: String,
        callerRole: UserRole? = null
    ) {
        val state = _uiState.value
        val qty = state.requestedQuantityText.toIntOrNull()

        if (state.projectId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Project ID is required.") }
            return
        }
        if (state.customerId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Customer ID is required.") }
            return
        }
        if (state.productId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Product ID is required.") }
            return
        }
        if (qty == null || qty <= 0) {
            _uiState.update { it.copy(errorMessage = "Requested quantity must be greater than zero.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        scope.launch {
            val returnId = "RET-${UUID.randomUUID().toString().take(8).uppercase()}"
            val returnNo = "RN-${System.currentTimeMillis() % 1000000}"
            val returnItemId = "RI-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()

            val request = ReturnRequest(
                returnId = returnId,
                projectId = state.projectId,
                returnNo = returnNo,
                customerId = state.customerId,
                originalChallanId = state.originalChallanId.ifBlank { null },
                status = ReturnStatus.REQUESTED,
                reason = state.reason,
                description = state.notes.ifBlank { null },
                requestedAt = now,
                requestedBy = actorId,
                createdAt = now,
                updatedAt = now,
                version = 1L
            )

            val item = ReturnItem(
                returnItemId = returnItemId,
                returnId = returnId,
                productId = state.productId,
                originalChallanItemId = state.originalChallanItemId.ifBlank { null },
                requestedQuantity = qty,
                acceptedQuantity = 0,
                rejectedQuantity = 0,
                unit = state.unit.ifBlank { "pcs" },
                condition = null,
                notes = state.notes.ifBlank { null }
            )

            val result = repository.createReturn(
                request = request,
                items = listOf(item),
                actorId = actorId,
                callerRole = callerRole,
                callerProjectId = state.projectId
            )

            when (result) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            createdReturnId = returnId
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}
