package com.sucharu.sucharupro.ui.features.qc.finalqc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinalQcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * ViewModel managing Final QC & Production Release details and user actions (Module 06 Step 07).
 */
class FinalQcDetailsViewModel(
    private val finalQcId: String,
    private val finalQcRepository: FinalQcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinalQcDetailsUiState(isLoading = true))
    val uiState: StateFlow<FinalQcDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Observe inspection
            launch {
                finalQcRepository.observeFinalQcById(finalQcId)
                    .catch { ex ->
                        _uiState.update { it.copy(errorMessage = ex.message ?: "Failed to load inspection.") }
                    }
                    .collect { inspection ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                inspection = inspection
                            )
                        }
                        if (inspection != null) {
                            checkEligibility()
                        }
                    }
            }

            // Observe activities
            launch {
                finalQcRepository.observeFinalQcActivity(finalQcId)
                    .catch { /* ignore */ }
                    .collect { events ->
                        _uiState.update { it.copy(activityEvents = events) }
                    }
            }
        }
    }

    fun checkEligibility() {
        viewModelScope.launch {
            val result = finalQcRepository.evaluateReleaseEligibility(finalQcId)
            if (result is DomainResult.Success) {
                _uiState.update { it.copy(eligibilityResult = result.data) }
            }
        }
    }

    fun startInspection(inspectorId: String, inspectorName: String?, callerRole: UserRole? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val now = Instant.now().toString()
            val result = finalQcRepository.startInspection(
                finalQcId = finalQcId,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                timestamp = now,
                callerRole = callerRole
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            successMessage = "Final QC Inspection started successfully."
                        )
                    }
                    checkEligibility()
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            errorMessage = result.message
                        )
                    }
                }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun passInspection(
        acceptedQuantity: Int?,
        notes: String?,
        inspectorId: String,
        inspectorName: String?,
        callerRole: UserRole? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, showPassDialog = false) }
            val now = Instant.now().toString()
            val result = finalQcRepository.submitPass(
                finalQcId = finalQcId,
                acceptedQuantity = acceptedQuantity,
                notes = notes,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                timestamp = now,
                callerRole = callerRole
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            successMessage = "Final QC marked as PASSED."
                        )
                    }
                    checkEligibility()
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            errorMessage = result.message
                        )
                    }
                }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun failInspection(
        rejectedQuantity: Int,
        failureReason: String,
        notes: String?,
        inspectorId: String,
        inspectorName: String?,
        callerRole: UserRole? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, showFailDialog = false) }
            val now = Instant.now().toString()
            val result = finalQcRepository.submitFail(
                finalQcId = finalQcId,
                rejectedQuantity = rejectedQuantity,
                failureReason = failureReason,
                notes = notes,
                inspectorId = inspectorId,
                inspectorName = inspectorName,
                timestamp = now,
                callerRole = callerRole
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            successMessage = "Final QC recorded as FAILED."
                        )
                    }
                    checkEligibility()
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            errorMessage = result.message
                        )
                    }
                }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun authorizeRelease(
        releaseNotes: String?,
        authorizedBy: String,
        authorizedByName: String?,
        callerRole: UserRole? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, showReleaseDialog = false) }
            val now = Instant.now().toString()
            val result = finalQcRepository.authorizeProductionRelease(
                finalQcId = finalQcId,
                releaseNotes = releaseNotes,
                authorizedBy = authorizedBy,
                authorizedByName = authorizedByName,
                timestamp = now,
                callerRole = callerRole
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            releaseAuthorization = result.data,
                            successMessage = "Production Release formally AUTHORIZED."
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            errorMessage = result.message
                        )
                    }
                }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun setShowPassDialog(show: Boolean) {
        _uiState.update { it.copy(showPassDialog = show) }
    }

    fun setShowFailDialog(show: Boolean) {
        _uiState.update { it.copy(showFailDialog = show) }
    }

    fun setShowReleaseDialog(show: Boolean) {
        _uiState.update { it.copy(showReleaseDialog = show) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
