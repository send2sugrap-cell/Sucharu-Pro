package com.sucharu.sucharupro.ui.features.delivery.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryItemVerificationRepository
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import com.sucharu.sucharupro.domain.service.DeliveryItemVerificationClassificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Creating and Recording Delivery Item Verifications (Module 08 Step 04).
 */
class DeliveryItemVerificationFormViewModel(
    private val verificationRepository: DeliveryItemVerificationRepository,
    private val dispatchRepository: DispatchExecutionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryItemVerificationFormUiState())
    val uiState: StateFlow<DeliveryItemVerificationFormUiState> = _uiState.asStateFlow()

    fun initialize(projectId: String, preselectedDispatchId: String? = null) {
        val defaultVerificationNo = "VERIF-${System.currentTimeMillis() % 100000}"
        _uiState.update { it.copy(isLoading = true, projectId = projectId, verificationNo = defaultVerificationNo) }

        viewModelScope.launch {
            val allDispatches = dispatchRepository.observeDispatches(projectId).first()
            val eligibleDispatches = allDispatches.filter { it.status == DispatchExecutionStatus.DISPATCHED }

            _uiState.update { it.copy(isLoading = false, availableDispatches = eligibleDispatches) }

            val targetDispatchId = preselectedDispatchId ?: eligibleDispatches.firstOrNull()?.dispatchExecutionId
            if (targetDispatchId != null) {
                selectDispatch(targetDispatchId)
            }
        }
    }

    fun selectDispatch(dispatchId: String) {
        viewModelScope.launch {
            val linesResult = dispatchRepository.getDispatchLines(dispatchId)
            val dispatchLines = if (linesResult is DomainResult.Success) linesResult.data else emptyList()

            val formLines = dispatchLines.map { line ->
                DeliveryItemVerificationLineFormItem(
                    lineId = UUID.randomUUID().toString(),
                    dispatchExecutionLineId = line.dispatchExecutionLineId,
                    challanLineId = line.deliveryChallanLineId,
                    deliveryOrderLineId = line.deliveryOrderLineId,
                    productId = line.productId,
                    expectedQuantity = line.dispatchQuantity,
                    verifiedQuantity = line.dispatchQuantity,
                    batchId = line.batchId ?: "",
                    lotId = line.lotId ?: ""
                )
            }

            _uiState.update {
                it.copy(
                    selectedDispatchId = dispatchId,
                    lines = formLines
                )
            }
        }
    }

    fun onVerificationNoChanged(value: String) {
        _uiState.update { it.copy(verificationNo = value) }
    }

    fun onRemarksChanged(value: String) {
        _uiState.update { it.copy(remarks = value) }
    }

    fun updateVerifiedQuantity(index: Int, qty: Double) {
        _uiState.update { current ->
            val updated = current.lines.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(verifiedQuantity = qty)
            }
            current.copy(lines = updated)
        }
    }

    fun updateDamage(index: Int, isDamaged: Boolean, damagedQty: Double) {
        _uiState.update { current ->
            val updated = current.lines.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(isDamaged = isDamaged, damagedQuantity = damagedQty)
            }
            current.copy(lines = updated)
        }
    }

    fun updateMissing(index: Int, isMissing: Boolean) {
        _uiState.update { current ->
            val updated = current.lines.toMutableList()
            if (index in updated.indices) {
                val qty = if (isMissing) 0.0 else updated[index].expectedQuantity
                updated[index] = updated[index].copy(isMissing = isMissing, verifiedQuantity = qty)
            }
            current.copy(lines = updated)
        }
    }

    fun saveVerification(actorId: String, callerRole: UserRole) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val verificationId = UUID.randomUUID().toString()

        val selectedDispatch = state.availableDispatches.find { it.dispatchExecutionId == state.selectedDispatchId }

        val verification = DeliveryItemVerification(
            verificationId = verificationId,
            projectId = state.projectId,
            verificationNo = state.verificationNo.trim(),
            deliveryOrderId = selectedDispatch?.deliveryOrderId ?: "",
            deliveryChallanId = selectedDispatch?.deliveryChallanId ?: "",
            dispatchExecutionId = state.selectedDispatchId,
            status = DeliveryItemVerificationStatus.DRAFT,
            remarks = state.remarks.trim().ifBlank { null },
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val domainLines = state.lines.map { item ->
            val classification = DeliveryItemVerificationClassificationService.classifyLine(
                expectedQuantity = item.expectedQuantity,
                verifiedQuantity = item.verifiedQuantity,
                isDamaged = item.isDamaged,
                damagedQuantity = item.damagedQuantity,
                isMissing = item.isMissing,
                isProductMismatch = item.isProductMismatch,
                isBatchMismatch = item.isBatchMismatch,
                isLotMismatch = item.isLotMismatch
            )

            DeliveryItemVerificationLine(
                verificationLineId = item.lineId,
                verificationId = verificationId,
                projectId = state.projectId,
                dispatchExecutionLineId = item.dispatchExecutionLineId,
                challanLineId = item.challanLineId,
                deliveryOrderLineId = item.deliveryOrderLineId,
                productId = item.productId,
                batchId = item.batchId.trim().ifBlank { null },
                lotId = item.lotId.trim().ifBlank { null },
                expectedQuantity = item.expectedQuantity,
                verifiedQuantity = item.verifiedQuantity,
                issueQuantity = classification.issueQuantity,
                resultType = classification.resultType,
                issueType = classification.issueType,
                remarks = item.remarks.trim().ifBlank { null },
                createdAt = now
            )
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = verificationRepository.createVerification(verification, domainLines, callerRole)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, isSavedSuccessfully = true) }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isSaving = true) }
                }
            }
        }
    }
}
