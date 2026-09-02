package com.sucharu.sucharupro.ui.features.finance.adjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.CustomerCreditNote
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustment
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.VendorDebitNote
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialAdjustmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FinancialAdjustmentDetailsUiState(
    val isLoading: Boolean = true,
    val adjustment: FinancialAdjustment? = null,
    val creditNote: CustomerCreditNote? = null,
    val debitNote: VendorDebitNote? = null,
    val activityEvents: List<FinancialAdjustmentActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

class FinancialAdjustmentDetailsViewModel(
    private val adjustmentRepository: FinancialAdjustmentRepository,
    private val adjustmentId: String,
    private val callerRole: UserRole,
    private val currentActorId: String,
    private val authenticatedCustomerId: String? = null,
    private val authenticatedVendorId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialAdjustmentDetailsUiState())
    val uiState: StateFlow<FinancialAdjustmentDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val adjRes = adjustmentRepository.getAdjustmentById(
                adjustmentId = adjustmentId,
                callerRole = callerRole,
                authenticatedCustomerId = authenticatedCustomerId,
                authenticatedVendorId = authenticatedVendorId
            )
            if (adjRes is DomainResult.Success) {
                val adjustment = adjRes.data
                val cnId = adjustment.creditNoteId
                val cnRes = if (cnId != null) {
                    adjustmentRepository.getCreditNoteById(cnId, callerRole, authenticatedCustomerId)
                } else null
                val creditNote = if (cnRes is DomainResult.Success) cnRes.data else null

                val dnId = adjustment.debitNoteId
                val dnRes = if (dnId != null) {
                    adjustmentRepository.getDebitNoteById(dnId, callerRole, authenticatedVendorId)
                } else null
                val debitNote = if (dnRes is DomainResult.Success) dnRes.data else null

                val eventsRes = adjustmentRepository.getActivityEvents(adjustmentId, callerRole)
                val events = if (eventsRes is DomainResult.Success) eventsRes.data else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        adjustment = adjustment,
                        creditNote = creditNote,
                        debitNote = debitNote,
                        activityEvents = events
                    )
                }
            } else if (adjRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = adjRes.message) }
            }
        }
    }

    fun submitAdjustment() {
        viewModelScope.launch {
            val res = adjustmentRepository.submitAdjustment(adjustmentId, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Adjustment submitted for approval.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun approveAdjustment() {
        viewModelScope.launch {
            val res = adjustmentRepository.approveAdjustment(adjustmentId, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Adjustment approved.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun postAdjustment(overrideAccountHead: String? = null) {
        viewModelScope.launch {
            val res = adjustmentRepository.postAdjustment(adjustmentId, overrideAccountHead, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Adjustment posted to ledger.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun rejectAdjustment(reason: String) {
        viewModelScope.launch {
            val res = adjustmentRepository.rejectAdjustment(adjustmentId, reason, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Adjustment rejected.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun cancelAdjustment(reason: String) {
        viewModelScope.launch {
            val res = adjustmentRepository.cancelAdjustment(adjustmentId, reason, currentActorId, callerRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(actionSuccessMessage = "Adjustment cancelled.") }
                loadDetails()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }
}
