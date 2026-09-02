package com.sucharu.sucharupro.ui.features.delivery.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryItemVerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Delivery Item Verification details and line actions (Module 08 Step 04).
 */
class DeliveryItemVerificationDetailsViewModel(
    private val repository: DeliveryItemVerificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryItemVerificationDetailsUiState(isLoading = true))
    val uiState: StateFlow<DeliveryItemVerificationDetailsUiState> = _uiState.asStateFlow()

    fun loadVerificationDetails(verificationId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeVerification(verificationId).collect { verification ->
                if (verification == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Delivery verification not found.") }
                    return@collect
                }

                val linesResult = repository.getVerificationLines(verificationId)
                val lines = if (linesResult is DomainResult.Success) linesResult.data else emptyList()

                val summaryResult = repository.getVerificationSummary(verificationId)
                val summary = if (summaryResult is DomainResult.Success) summaryResult.data else null

                val activitiesResult = repository.getActivityEvents(verificationId)
                val activities = if (activitiesResult is DomainResult.Success) activitiesResult.data else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        verification = verification,
                        lines = lines,
                        summary = summary,
                        activityEvents = activities
                    )
                }
            }
        }
    }

    fun submitVerification(verificationId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.submitVerification(verificationId, actorId, callerRole) }
    }

    fun startVerification(verificationId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.startVerification(verificationId, actorId, callerRole) }
    }

    fun verifyLine(
        verificationId: String,
        lineId: String,
        verifiedQty: Double,
        isDamaged: Boolean,
        damagedQty: Double,
        isMissing: Boolean,
        isProductMismatch: Boolean,
        isBatchMismatch: Boolean,
        isLotMismatch: Boolean,
        remarks: String?,
        actorId: String,
        callerRole: UserRole
    ) {
        performAction {
            repository.verifyLine(
                verificationId = verificationId,
                verificationLineId = lineId,
                verifiedQuantity = verifiedQty,
                isDamaged = isDamaged,
                damagedQuantity = damagedQty,
                isMissing = isMissing,
                isProductMismatch = isProductMismatch,
                isBatchMismatch = isBatchMismatch,
                isLotMismatch = isLotMismatch,
                remarks = remarks,
                actorId = actorId,
                callerRole = callerRole
            )
        }
    }

    fun completeVerification(verificationId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.completeVerification(verificationId, actorId, callerRole) }
    }

    fun closeVerification(verificationId: String, actorId: String, callerRole: UserRole) {
        performAction { repository.closeVerification(verificationId, actorId, callerRole) }
    }

    fun cancelVerification(verificationId: String, actorId: String, reason: String?, callerRole: UserRole) {
        performAction { repository.cancelVerification(verificationId, actorId, reason, callerRole) }
    }

    private fun performAction(action: suspend () -> DomainResult<*>) {
        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            when (val result = action()) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, successMessage = "Action completed successfully.") }
                    _uiState.value.verification?.verificationId?.let { loadVerificationDetails(it) }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isActionInProgress = false, errorMessage = result.message) }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isActionInProgress = true) }
                }
            }
        }
    }
}
