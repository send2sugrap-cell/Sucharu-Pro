package com.sucharu.sucharupro.ui.features.qc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.repository.ProductionQcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for listing, observing, and creating Quality Control records (Module 06 Step 01).
 */
class ProductionQcListViewModel(
    private val productionJobId: String? = null,
    private val inspectorId: String? = null,
    private val qcRepository: ProductionQcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductionQcListUiState>(ProductionQcListUiState.Loading)
    val uiState: StateFlow<ProductionQcListUiState> = _uiState.asStateFlow()

    init {
        loadQcList()
    }

    private fun loadQcList() {
        val stream = when {
            productionJobId != null -> qcRepository.getQcForJob(productionJobId)
            inspectorId != null -> qcRepository.getQcForInspector(inspectorId)
            else -> qcRepository.observeQcList()
        }

        viewModelScope.launch {
            stream
                .catch { ex -> _uiState.value = ProductionQcListUiState.Error(ex.localizedMessage ?: "Failed to load QC list.") }
                .collect { list ->
                    _uiState.value = ProductionQcListUiState.Success(qcList = list)
                }
        }
    }

    fun createQc(
        targetJobId: String,
        stageId: String?,
        qcType: QcType,
        notes: String?,
        createdBy: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ProductionQcListUiState.Success) {
                _uiState.value = currentState.copy(isCreating = true, errorMessage = null)
            }

            val result = qcRepository.createQc(
                productionJobId = targetJobId,
                productionStageId = stageId,
                qcType = qcType,
                notes = notes,
                createdBy = createdBy,
                timestamp = timestamp
            )

            if (currentState is ProductionQcListUiState.Success) {
                when (result) {
                    is DomainResult.Success -> {
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            message = "QC record created successfully!"
                        )
                    }
                    is DomainResult.Error -> {
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            errorMessage = result.message
                        )
                    }
                    is DomainResult.Loading -> {}
                }
            }
        }
    }
}
