package com.sucharu.sucharupro.ui.features.imposition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.imposition.*
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.service.imposition.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for Prepress Orchestration Master Command Center.
 * Module 18 Step 06.
 */
class PrepressOrchestrationViewModel(
    private val orchestrationService: PrepressOrchestrationService? = null,
    private val tenantId: String = "tenant_default",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(PrepressOrchestrationUiState())
    val uiState: StateFlow<PrepressOrchestrationUiState> = _uiState.asStateFlow()

    init {
        generateDefaultOrchestrationPlan()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun generateDefaultOrchestrationPlan() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 1. Generate Sample Upstream Signature & CTP Spec
                val pageDim = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
                val sheetDim = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

                val sigSpec = SignatureImpositionEngine.optimizeSignatureImposition(
                    tenantId = tenantId,
                    name = "Catalog 16pp Master Imposition",
                    jobId = "JOB-CAT-2026",
                    orderId = "ORD-7788",
                    orderItemId = "ITEM-01",
                    productName = "Product Catalog 16pp (A4)",
                    totalPages = 16,
                    signaturePageCount = 16,
                    bindingMethod = BindingMethod.SADDLE_STITCH,
                    sheetTurningMethod = SheetTurningMethod.SHEETWISE,
                    foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
                    pageDimension = pageDim,
                    parentSheetDimension = sheetDim,
                    requiredQuantity = 1000L,
                    paperStockType = PaperStockType.ART_PAPER,
                    gsm = BigDecimal("150.0000")
                )

                val ctpSpec = CtpOutputGenerationEngine.generateFromSignatureImposition(
                    signatureSpec = sigSpec,
                    colorSeparations = listOf(
                        PlateColorSeparation.CYAN,
                        PlateColorSeparation.MAGENTA,
                        PlateColorSeparation.YELLOW,
                        PlateColorSeparation.BLACK
                    )
                )

                // 2. Orchestrate Master Plan
                val domainPlan = PrepressOrchestrationEngine.orchestratePlan(
                    tenantId = tenantId,
                    planName = "Prepress Master Orchestration: Product Catalog 16pp",
                    jobId = "JOB-CAT-2026",
                    orderId = "ORD-7788",
                    orderItemId = "ITEM-01",
                    productName = "Product Catalog 16pp (A4)",
                    requiredQuantity = 1000L,
                    step04Signature = sigSpec,
                    step05CtpOutput = ctpSpec,
                    planVersion = 1,
                    actor = "lead_prepress"
                )

                val handoffContract = Module18Step06PrepressOrchestrationHandoffContract(
                    contractVersion = "1.0.0",
                    planId = domainPlan.planId,
                    tenantId = domainPlan.tenantId,
                    jobId = domainPlan.jobId,
                    orderId = domainPlan.orderId,
                    orderItemId = domainPlan.orderItemId,
                    productName = domainPlan.productName,
                    planVersion = domainPlan.version,
                    planStatus = domainPlan.status.name,
                    requiredSheets = domainPlan.requiredSheets,
                    totalProducedQuantity = domainPlan.totalProducedQuantity,
                    totalPlatesCount = domainPlan.totalPlatesCount,
                    totalSignaturesCount = domainPlan.totalSignaturesCount,
                    sheetUtilizationPercentage = domainPlan.sheetUtilizationPercentage,
                    wastePercentage = domainPlan.wastePercentage,
                    pressSheetWidthMm = domainPlan.pressSheetWidthMm,
                    pressSheetHeightMm = domainPlan.pressSheetHeightMm,
                    plateWidthMm = domainPlan.plateWidthMm,
                    plateHeightMm = domainPlan.plateHeightMm,
                    readinessScore = domainPlan.readinessScore.overallScore,
                    isFullyReconciled = domainPlan.reconciliationResult.isReconciled,
                    blockingErrorsCount = domainPlan.reconciliationResult.blockingErrorsCount,
                    warningsCount = domainPlan.reconciliationResult.warningsCount,
                    masterIntegrityHash = domainPlan.masterIntegrityHash,
                    step05CtpOutputId = domainPlan.step05CtpOutputId,
                    step04SignatureId = domainPlan.step04SignatureId,
                    step03NestingId = null,
                    step02GangRunBatchId = null,
                    step01ImpositionId = null
                )

                val json = """
                    {
                      "contractVersion": "${handoffContract.contractVersion}",
                      "planId": "${handoffContract.planId}",
                      "tenantId": "${handoffContract.tenantId}",
                      "jobId": "${handoffContract.jobId}",
                      "orderId": "${handoffContract.orderId}",
                      "productName": "${handoffContract.productName}",
                      "planVersion": ${handoffContract.planVersion},
                      "planStatus": "${handoffContract.planStatus}",
                      "requiredSheets": ${handoffContract.requiredSheets},
                      "totalProducedQuantity": ${handoffContract.totalProducedQuantity},
                      "totalPlatesCount": ${handoffContract.totalPlatesCount},
                      "totalSignaturesCount": ${handoffContract.totalSignaturesCount},
                      "sheetUtilizationPercentage": "${handoffContract.sheetUtilizationPercentage}%",
                      "wastePercentage": "${handoffContract.wastePercentage}%",
                      "readinessScore": "${handoffContract.readinessScore}/100",
                      "isFullyReconciled": ${handoffContract.isFullyReconciled},
                      "masterIntegrityHash": "${handoffContract.masterIntegrityHash}",
                      "step05CtpOutputId": "${handoffContract.step05CtpOutputId}",
                      "step04SignatureId": "${handoffContract.step04SignatureId}"
                    }
                """.trimIndent()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPlan = domainPlan.toDto(),
                        plansList = listOf(domainPlan.toDto()),
                        handoffContractJson = json,
                        successMessage = "Master prepress orchestration plan successfully reconciled and sealed."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Error orchestrating plan: ${e.message}")
                }
            }
        }
    }

    fun approvePlan() {
        val current = _uiState.value.currentPlan ?: return
        val updated = current.copy(
            status = PrepressPlanStatus.APPROVED.name,
            approvalStatus = "APPROVED",
            approvedBy = "prepress_manager",
            approvedAt = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(
                currentPlan = updated,
                successMessage = "Prepress Orchestration Plan APPROVED for Production Dispatch & Substrate Reservation."
            )
        }
    }

    fun finalizePlan() {
        val current = _uiState.value.currentPlan ?: return
        val updated = current.copy(
            status = PrepressPlanStatus.FINALIZED.name,
            approvalStatus = "FINALIZED",
            notes = "Master plan permanently locked and dispatched to Module 17/19."
        )
        _uiState.update {
            it.copy(
                currentPlan = updated,
                successMessage = "Prepress Plan FINALIZED: Downstream contracts locked."
            )
        }
    }
}
