package com.sucharu.sucharupro.ui.features.substratereservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.substratereservation.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReplenishmentEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for Substrate Auto-Replenishment Triggers & Supplier Reorder Alerts.
 * Module 19 Step 04.
 */
class SubstrateReplenishmentViewModel(
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SubstrateReplenishmentUiState())
    val uiState: StateFlow<SubstrateReplenishmentUiState> = _uiState.asStateFlow()

    init {
        loadDefaultSampleReplenishment()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setShowEvaluateDialog(show: Boolean) {
        _uiState.update { it.copy(showEvaluateDialog = show) }
    }

    fun setShowAlertConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showAlertConfirmDialog = show) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun selectEvaluation(evaluation: SubstrateReplenishmentResponseDto) {
        _uiState.update {
            it.copy(
                currentEvaluation = evaluation,
                jsonHandoffPreview = buildHandoffJsonPreview(evaluation)
            )
        }
    }

    fun loadDefaultSampleReplenishment() {
        evaluateReplenishment(
            sku = "ART-300-25X36",
            materialName = "Art Card 300 GSM (25x36)",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetWidthMm = BigDecimal("635.0000"),
            sheetHeightMm = BigDecimal("914.4000"),
            warehouseId = "WH-CENTRAL-01",
            warehouseName = "Central Press Substrate Depot",
            onHandSheets = 8500L,
            reservedSheets = 5000L,
            pendingInboundSheets = 1000L,
            plannedDemandSheets = 3000L,
            minStockSheets = 3000L,
            safetyStockSheets = 6000L,
            reorderPointSheets = 12000L,
            targetStockSheets = 35000L,
            moqSheets = 5000L
        )
    }

    fun evaluateReplenishment(
        sku: String,
        materialName: String,
        stockType: PaperStockType,
        gsm: BigDecimal,
        sheetWidthMm: BigDecimal,
        sheetHeightMm: BigDecimal,
        warehouseId: String,
        warehouseName: String,
        onHandSheets: Long,
        reservedSheets: Long,
        pendingInboundSheets: Long,
        plannedDemandSheets: Long,
        minStockSheets: Long,
        safetyStockSheets: Long,
        reorderPointSheets: Long,
        targetStockSheets: Long,
        moqSheets: Long
    ) {
        scope.launch {
            _uiState.update { it.copy(isEvaluating = true, errorMessage = null) }
            try {
                val tenantId = "TENANT-001"
                val policy = SubstrateReplenishmentPolicy(
                    policyId = "POL-$sku-$tenantId",
                    tenantId = tenantId,
                    sku = sku,
                    policyType = ReplenishmentPolicyType.DEMAND_AWARE,
                    minimumStockSheets = minStockSheets,
                    safetyStockSheets = safetyStockSheets,
                    reorderPointSheets = reorderPointSheets,
                    targetStockSheets = targetStockSheets,
                    minimumOrderQuantitySheets = moqSheets,
                    standardPackReamSize = 500,
                    leadTimeDays = 4,
                    policyVersion = "1.0.0"
                )

                val sampleVendors = listOf(
                    Vendor(
                        vendorId = "VND-PAPER-01",
                        projectId = tenantId,
                        vendorCode = "VND-PAP-01",
                        vendorName = "Century Paper & Board Mills Ltd",
                        status = VendorStatus.ACTIVE,
                        primaryEmail = "sales@centurypaper.com",
                        primaryPhone = "+8801711000101"
                    ),
                    Vendor(
                        vendorId = "VND-PAPER-02",
                        projectId = tenantId,
                        vendorCode = "VND-PAP-02",
                        vendorName = "Bashundhara Paper Mills Corp",
                        status = VendorStatus.ACTIVE,
                        primaryEmail = "orders@bashundharapaper.com",
                        primaryPhone = "+8801711000102"
                    ),
                    Vendor(
                        vendorId = "VND-PAPER-03",
                        projectId = tenantId,
                        vendorCode = "VND-PAP-03",
                        vendorName = "Magna Paper & Board Supply",
                        status = VendorStatus.ACTIVE,
                        primaryEmail = "contact@magnapaper.com",
                        primaryPhone = "+8801711000103"
                    )
                )

                val input = SubstrateReplenishmentEngine.EvaluationInput(
                    tenantId = tenantId,
                    productId = "PROD-$sku",
                    sku = sku,
                    materialName = materialName,
                    stockType = stockType,
                    gsm = gsm,
                    sheetDimension = PrintingDimension(sheetWidthMm, sheetHeightMm, MeasurementUnit.MILLIMETERS),
                    warehouseId = warehouseId,
                    warehouseName = warehouseName,
                    onHandPhysicalSheets = onHandSheets,
                    activeReservedSheets = reservedSheets,
                    pendingInboundSheets = pendingInboundSheets,
                    plannedDemandSheets = plannedDemandSheets,
                    policy = policy,
                    candidateVendors = sampleVendors,
                    evaluator = "planner_admin"
                )

                val evalResult = SubstrateReplenishmentEngine.evaluate(input)
                val dto = evalResult.toDto()

                val updatedList = listOf(dto) + _uiState.value.evaluations.filter { it.evaluationId != dto.evaluationId }
                _uiState.update {
                    it.copy(
                        isEvaluating = false,
                        currentEvaluation = dto,
                        evaluations = updatedList,
                        jsonHandoffPreview = buildHandoffJsonPreview(dto),
                        showEvaluateDialog = false,
                        successMessage = "Replenishment evaluated: state=${dto.triggerState}, priority=${dto.priority}, recommended=${dto.recommendedReorderSheets} sheets"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEvaluating = false, errorMessage = "Replenishment evaluation failed: ${e.message}") }
            }
        }
    }

    fun triggerSupplierAlert(evaluationId: String, vendorId: String? = null) {
        scope.launch {
            _uiState.update { it.copy(isTriggeringAlert = true, errorMessage = null) }
            try {
                val current = _uiState.value.currentEvaluation
                    ?: throw IllegalStateException("No active evaluation selected")

                val vendor = if (!vendorId.isNullOrBlank()) {
                    current.recommendedSuppliers.firstOrNull { it.vendorId == vendorId }
                } else {
                    current.recommendedSuppliers.firstOrNull()
                } ?: throw IllegalStateException("No eligible supplier candidate")

                val alert = SupplierReorderAlert(
                    alertId = "ALRT-${java.util.UUID.randomUUID().toString().take(8).uppercase()}",
                    evaluationId = current.evaluationId,
                    tenantId = current.tenantId,
                    vendorId = vendor.vendorId,
                    vendorCode = vendor.vendorCode,
                    vendorName = vendor.vendorName,
                    sku = current.sku,
                    materialName = current.materialName,
                    requestedSheets = current.recommendedReorderSheets,
                    requestedReams = current.recommendedReorderReams,
                    targetDeliveryTimestamp = System.currentTimeMillis() + (vendor.estimatedLeadTimeDays * 86400000L),
                    priority = ReplenishmentPriority.valueOf(current.priority),
                    status = ReplenishmentTriggerState.SUPPLIER_ALERT_SENT,
                    alertPayloadJson = "{\"sku\":\"${current.sku}\",\"sheets\":${current.recommendedReorderSheets}}",
                    dispatchedBy = "planner_admin",
                    dispatchedAt = System.currentTimeMillis(),
                    acknowledgedAt = null,
                    purchaseRequisitionId = "REQ-PO-${java.util.UUID.randomUUID().toString().take(6).uppercase()}"
                )
                val alertDto = alert.toDto()

                val updatedEval = current.copy(triggerState = ReplenishmentTriggerState.SUPPLIER_ALERT_SENT.name)
                val updatedEvaluations = _uiState.value.evaluations.map {
                    if (it.evaluationId == current.evaluationId) updatedEval else it
                }

                _uiState.update {
                    it.copy(
                        isTriggeringAlert = false,
                        currentEvaluation = updatedEval,
                        evaluations = updatedEvaluations,
                        alerts = listOf(alertDto) + it.alerts,
                        showAlertConfirmDialog = false,
                        successMessage = "Reorder alert dispatched to ${vendor.vendorName} for ${current.recommendedReorderSheets} sheets!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTriggeringAlert = false, errorMessage = "Failed to dispatch alert: ${e.message}") }
            }
        }
    }

    private fun buildHandoffJsonPreview(dto: SubstrateReplenishmentResponseDto): String {
        return """
        {
          "contractVersion": "4.0.0",
          "evaluationId": "${dto.evaluationId}",
          "tenantId": "${dto.tenantId}",
          "sku": "${dto.sku}",
          "materialName": "${dto.materialName}",
          "warehouseId": "${dto.warehouseId}",
          "onHandPhysicalSheets": ${dto.onHandPhysicalSheets},
          "activeReservedSheets": ${dto.activeReservedSheets},
          "availableSheets": ${dto.availableSheets},
          "netProjectedAvailabilitySheets": ${dto.netProjectedAvailabilitySheets},
          "safetyStockSheets": ${dto.safetyStockSheets},
          "reorderPointSheets": ${dto.reorderPointSheets},
          "isReorderRequired": ${dto.isReorderRequired},
          "projectedShortfallSheets": ${dto.projectedShortfallSheets},
          "recommendedReorderSheets": ${dto.recommendedReorderSheets},
          "recommendedReorderReams": "${dto.recommendedReorderReams}",
          "triggerState": "${dto.triggerState}",
          "priority": "${dto.priority}",
          "primaryReason": "${dto.primaryReason}",
          "preferredVendorId": "${dto.primaryVendorId ?: "NONE"}",
          "preferredVendorName": "${dto.primaryVendorName ?: "NONE"}",
          "deduplicationFingerprint": "${dto.deduplicationFingerprint}",
          "masterIntegrityHash": "${dto.masterIntegrityHash}",
          "evaluatedBy": "${dto.evaluatedBy}",
          "evaluatedAt": ${dto.evaluatedAt}
        }
        """.trimIndent()
    }
}
