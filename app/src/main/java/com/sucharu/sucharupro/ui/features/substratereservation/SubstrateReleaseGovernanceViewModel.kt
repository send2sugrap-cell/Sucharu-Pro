package com.sucharu.sucharupro.ui.features.substratereservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateReleaseGovernanceResponseDto
import com.sucharu.sucharupro.data.api.model.substratereservation.toDto
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReleaseGovernanceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Substrate Release & Revision Governance Command Center.
 * Module 19 Step 05.
 */
class SubstrateReleaseGovernanceViewModel(
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SubstrateReleaseGovernanceUiState())
    val uiState: StateFlow<SubstrateReleaseGovernanceUiState> = _uiState.asStateFlow()

    init {
        loadDefaultSampleCase()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun selectRecord(record: SubstrateReleaseGovernanceResponseDto) {
        _uiState.update {
            it.copy(
                currentRecord = record,
                jsonHandoffPreview = buildHandoffJsonPreview(record)
            )
        }
    }

    fun loadDefaultSampleCase() {
        evaluateCancellation(
            reservationId = "RES-ART300-01",
            orderId = "ORD-2026-9041",
            orderItemId = "ITEM-01",
            executionJobId = "JOB-2026-1122",
            sku = "ART-300-25X36",
            materialName = "Art Card 300 GSM (25x36)",
            warehouseId = "WH-CENTRAL-01",
            allocatedSheets = 10000L,
            consumedSheets = 2000L,
            committedSheets = 1000L,
            productionStatus = "READY"
        )
    }

    fun evaluateCancellation(
        reservationId: String,
        orderId: String,
        orderItemId: String,
        executionJobId: String?,
        sku: String,
        materialName: String,
        warehouseId: String,
        allocatedSheets: Long,
        consumedSheets: Long,
        committedSheets: Long,
        productionStatus: String?
    ) {
        scope.launch {
            _uiState.update { it.copy(isEvaluating = true, errorMessage = null) }
            try {
                val input = SubstrateReleaseGovernanceEngine.EvaluationInput(
                    tenantId = "TENANT-001",
                    reservationId = reservationId,
                    orderId = orderId,
                    orderItemId = orderItemId,
                    executionJobId = executionJobId,
                    triggerType = GovernanceTriggerType.JOB_CANCELLATION,
                    sku = sku,
                    materialName = materialName,
                    warehouseId = warehouseId,
                    previousRequiredSheets = allocatedSheets,
                    newRequiredSheets = 0L,
                    allocatedSheets = allocatedSheets,
                    consumedSheets = consumedSheets,
                    committedSheets = committedSheets,
                    productionStatus = productionStatus,
                    evaluator = "planner_supervisor"
                )

                val record = SubstrateReleaseGovernanceEngine.evaluate(input)
                val dto = record.toDto()

                val updatedList = listOf(dto) + _uiState.value.records.filter { it.governanceId != dto.governanceId }
                _uiState.update {
                    it.copy(
                        isEvaluating = false,
                        currentRecord = dto,
                        records = updatedList,
                        jsonHandoffPreview = buildHandoffJsonPreview(dto),
                        successMessage = "Cancellation evaluated: decision=${dto.decision}, releasable=${dto.releasableSheets} sheets"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEvaluating = false, errorMessage = "Evaluation failed: ${e.message}") }
            }
        }
    }

    fun evaluateRevision(
        reservationId: String,
        orderId: String,
        orderItemId: String,
        executionJobId: String?,
        sku: String,
        materialName: String,
        warehouseId: String,
        previousRequiredSheets: Long,
        newRequiredSheets: Long,
        allocatedSheets: Long,
        consumedSheets: Long,
        committedSheets: Long,
        productionStatus: String?,
        isSkuChanged: Boolean = false
    ) {
        scope.launch {
            _uiState.update { it.copy(isEvaluating = true, errorMessage = null) }
            try {
                val triggerType = if (isSkuChanged) {
                    GovernanceTriggerType.SPECIFICATION_REVISION
                } else if (newRequiredSheets < previousRequiredSheets) {
                    GovernanceTriggerType.QUANTITY_REDUCTION
                } else {
                    GovernanceTriggerType.QUANTITY_INCREASE
                }

                val input = SubstrateReleaseGovernanceEngine.EvaluationInput(
                    tenantId = "TENANT-001",
                    reservationId = reservationId,
                    orderId = orderId,
                    orderItemId = orderItemId,
                    executionJobId = executionJobId,
                    triggerType = triggerType,
                    sku = sku,
                    materialName = materialName,
                    warehouseId = warehouseId,
                    previousRequiredSheets = previousRequiredSheets,
                    newRequiredSheets = newRequiredSheets,
                    allocatedSheets = allocatedSheets,
                    consumedSheets = consumedSheets,
                    committedSheets = committedSheets,
                    productionStatus = productionStatus,
                    isSkuChanged = isSkuChanged,
                    evaluator = "planner_supervisor"
                )

                val record = SubstrateReleaseGovernanceEngine.evaluate(input)
                val dto = record.toDto()

                val updatedList = listOf(dto) + _uiState.value.records.filter { it.governanceId != dto.governanceId }
                _uiState.update {
                    it.copy(
                        isEvaluating = false,
                        currentRecord = dto,
                        records = updatedList,
                        jsonHandoffPreview = buildHandoffJsonPreview(dto),
                        successMessage = "Revision evaluated: decision=${dto.decision}, releasable=${dto.releasableSheets}, additional=${dto.additionalRequiredSheets} sheets"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEvaluating = false, errorMessage = "Revision evaluation failed: ${e.message}") }
            }
        }
    }

    fun approveRelease(governanceId: String, notes: String? = null) {
        scope.launch {
            _uiState.update { it.copy(isApproving = true, errorMessage = null) }
            try {
                val current = _uiState.value.currentRecord
                    ?: throw IllegalStateException("No active governance case selected")

                val updated = current.copy(
                    executionStatus = GovernanceExecutionStatus.APPROVED.name,
                    approvedBy = "supervisor_user",
                    approvedAt = System.currentTimeMillis(),
                    notes = notes ?: current.notes
                )

                val updatedList = _uiState.value.records.map {
                    if (it.governanceId == governanceId) updated else it
                }

                _uiState.update {
                    it.copy(
                        isApproving = false,
                        currentRecord = updated,
                        records = updatedList,
                        jsonHandoffPreview = buildHandoffJsonPreview(updated),
                        successMessage = "Release approved for case #${updated.governanceId} (${updated.releasableSheets} sheets)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isApproving = false, errorMessage = "Approval failed: ${e.message}") }
            }
        }
    }

    fun executeRelease(governanceId: String) {
        scope.launch {
            _uiState.update { it.copy(isExecuting = true, errorMessage = null) }
            try {
                val current = _uiState.value.currentRecord
                    ?: throw IllegalStateException("No active governance case selected")

                val updated = current.copy(
                    executionStatus = GovernanceExecutionStatus.RELEASE_EXECUTED.name,
                    executedBy = "supervisor_user",
                    executedAt = System.currentTimeMillis()
                )

                val updatedList = _uiState.value.records.map {
                    if (it.governanceId == governanceId) updated else it
                }

                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        currentRecord = updated,
                        records = updatedList,
                        jsonHandoffPreview = buildHandoffJsonPreview(updated),
                        successMessage = "Substrate release executed! ${updated.releasableSheets} sheets restored to inventory."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExecuting = false, errorMessage = "Release execution failed: ${e.message}") }
            }
        }
    }

    private fun buildHandoffJsonPreview(dto: SubstrateReleaseGovernanceResponseDto): String {
        return """
        {
          "contractVersion": "5.0.0",
          "governanceId": "${dto.governanceId}",
          "tenantId": "${dto.tenantId}",
          "reservationId": "${dto.reservationId}",
          "orderId": "${dto.orderId}",
          "orderItemId": "${dto.orderItemId}",
          "executionJobId": "${dto.executionJobId ?: "NONE"}",
          "triggerType": "${dto.triggerType}",
          "sku": "${dto.sku}",
          "materialName": "${dto.materialName}",
          "warehouseId": "${dto.warehouseId}",
          "allocatedSheets": ${dto.allocatedSheets},
          "consumedSheets": ${dto.consumedSheets},
          "committedSheets": ${dto.committedSheets},
          "releasableSheets": ${dto.releasableSheets},
          "retainedSheets": ${dto.retainedSheets},
          "additionalRequiredSheets": ${dto.additionalRequiredSheets},
          "decision": "${dto.decision}",
          "executionStatus": "${dto.executionStatus}",
          "blockingReason": "${dto.blockingReason}",
          "explanation": "${dto.explanation}",
          "deduplicationFingerprint": "${dto.deduplicationFingerprint}",
          "masterIntegrityHash": "${dto.masterIntegrityHash}",
          "evaluatedBy": "${dto.evaluatedBy}",
          "evaluatedAt": ${dto.evaluatedAt},
          "approvedBy": "${dto.approvedBy ?: "PENDING"}",
          "executedBy": "${dto.executedBy ?: "PENDING"}"
        }
        """.trimIndent()
    }
}
