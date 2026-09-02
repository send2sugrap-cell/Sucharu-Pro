package com.sucharu.sucharupro.ui.features.qc.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.QcChecklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for listing, observing, and creating QC Checklist Templates (Module 06 Step 03).
 */
class QcChecklistTemplateListViewModel(
    private val repository: QcChecklistRepository,
    private val currentUserRole: UserRole = UserRole.MANAGER
) : ViewModel() {

    private val _uiState = MutableStateFlow<QcChecklistTemplateListUiState>(QcChecklistTemplateListUiState.Loading)
    val uiState: StateFlow<QcChecklistTemplateListUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            repository.observeTemplates()
                .catch { ex -> _uiState.value = QcChecklistTemplateListUiState.Error(ex.localizedMessage ?: "Failed to load templates.") }
                .collect { list ->
                    _uiState.value = QcChecklistTemplateListUiState.Success(templates = list)
                }
        }
    }

    fun createTemplate(name: String, description: String?, qcType: QcType, stageType: String?, createdBy: String?, timestamp: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is QcChecklistTemplateListUiState.Success) {
                _uiState.value = currentState.copy(isCreating = true, errorMessage = null)
            }

            val result = repository.createTemplate(
                name = name,
                description = description,
                qcType = qcType,
                applicableStageType = stageType,
                createdBy = createdBy,
                timestamp = timestamp,
                callerRole = currentUserRole
            )

            if (currentState is QcChecklistTemplateListUiState.Success) {
                when (result) {
                    is DomainResult.Success -> {
                        _uiState.value = currentState.copy(isCreating = false, message = "Template created successfully!")
                    }
                    is DomainResult.Error -> {
                        _uiState.value = currentState.copy(isCreating = false, errorMessage = result.message)
                    }
                    is DomainResult.Loading -> {}
                }
            }
        }
    }
}
