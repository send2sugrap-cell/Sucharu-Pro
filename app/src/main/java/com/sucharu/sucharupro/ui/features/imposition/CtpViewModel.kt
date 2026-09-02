package com.sucharu.sucharupro.ui.features.imposition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.imposition.*
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.service.imposition.CtpOutputGenerationEngine
import com.sucharu.sucharupro.domain.service.imposition.CtpOutputService
import com.sucharu.sucharupro.domain.service.imposition.SignatureImpositionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for CTP Prepress Output & Plate Package Command Center.
 * Module 18 Step 05.
 */
class CtpViewModel(
    private val ctpService: CtpOutputService? = null,
    private val tenantId: String = "tenant_default",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(CtpUiState())
    val uiState: StateFlow<CtpUiState> = _uiState.asStateFlow()

    init {
        generateDefaultCtpPackage()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectPlateIndex(index: Int) {
        _uiState.update { it.copy(activePlateIndex = index) }
    }

    fun selectColorChannel(channel: PlateColorSeparation) {
        _uiState.update { it.copy(activeColorChannel = channel) }
    }

    fun togglePlateGrid(visible: Boolean) {
        _uiState.update { it.copy(showPlateGrid = visible) }
    }

    fun toggleMarks(visible: Boolean) {
        _uiState.update { it.copy(showMarks = visible) }
    }

    fun toggleBleedLines(visible: Boolean) {
        _uiState.update { it.copy(showBleedLines = visible) }
    }

    fun toggleGripperZone(visible: Boolean) {
        _uiState.update { it.copy(showGripperZone = visible) }
    }

    fun toggleColorBars(visible: Boolean) {
        _uiState.update { it.copy(showColorBars = visible) }
    }

    fun updateScreeningMethod(method: ScreeningMethod) {
        _uiState.update { it.copy(inputScreeningMethod = method) }
        recalculateLive()
    }

    fun updateResolutionDpi(dpi: OutputResolutionDpi) {
        _uiState.update { it.copy(inputResolutionDpi = dpi) }
        recalculateLive()
    }

    fun updateScreenRuling(lpi: String) {
        _uiState.update { it.copy(inputScreenRulingLpi = lpi) }
        recalculateLive()
    }

    fun updatePlateDimensions(width: String, height: String) {
        _uiState.update { it.copy(inputPlateWidthMm = width, inputPlateHeightMm = height) }
        recalculateLive()
    }

    fun updateGripperMargins(gripper: String, tail: String, sideLeft: String, sideRight: String) {
        _uiState.update {
            it.copy(
                inputGripperMarginMm = gripper,
                inputTailMarginMm = tail,
                inputSideGuideLeftMm = sideLeft,
                inputSideGuideRightMm = sideRight
            )
        }
        recalculateLive()
    }

    fun generateDefaultCtpPackage() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Build baseline 16pp signature imposition
                val pageDim = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
                val sheetDim = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

                val signatureSpec = SignatureImpositionEngine.optimizeSignatureImposition(
                    tenantId = tenantId,
                    name = "Catalog 16pp Signature Baseline",
                    jobId = _uiState.value.inputJobId,
                    orderId = _uiState.value.inputOrderId,
                    orderItemId = _uiState.value.inputOrderItemId,
                    productName = _uiState.value.inputProductName,
                    totalPages = 16,
                    signaturePageCount = 16,
                    bindingMethod = BindingMethod.SADDLE_STITCH,
                    sheetTurningMethod = SheetTurningMethod.SHEETWISE,
                    foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
                    pageDimension = pageDim,
                    parentSheetDimension = sheetDim,
                    requiredQuantity = 1000L,
                    paperStockType = PaperStockType.ART_PAPER,
                    gsm = BigDecimal("150.0000"),
                    gutterSpec = SignatureGutterSpec(
                        spineGutterMm = BigDecimal("4.0000"),
                        headGutterMm = BigDecimal("6.0000"),
                        footGutterMm = BigDecimal("6.0000"),
                        faceTrimMm = BigDecimal("4.0000"),
                        bleedMm = BigDecimal("3.0000")
                    ),
                    enableCreepCompensation = true
                )

                val plateDim = PlateDimensionSpec(
                    plateWidthMm = _uiState.value.inputPlateWidthMm.toBigDecimalOrNull() ?: BigDecimal("695.0000"),
                    plateHeightMm = _uiState.value.inputPlateHeightMm.toBigDecimalOrNull() ?: BigDecimal("994.4000"),
                    plateThicknessMm = BigDecimal("0.3000"),
                    gripperMarginMm = _uiState.value.inputGripperMarginMm.toBigDecimalOrNull() ?: BigDecimal("45.0000"),
                    tailMarginMm = _uiState.value.inputTailMarginMm.toBigDecimalOrNull() ?: BigDecimal("25.0000"),
                    sideGuideMarginLeftMm = _uiState.value.inputSideGuideLeftMm.toBigDecimalOrNull() ?: BigDecimal("30.0000"),
                    sideGuideMarginRightMm = _uiState.value.inputSideGuideRightMm.toBigDecimalOrNull() ?: BigDecimal("30.0000")
                )

                val markPolicy = PrepressMarkPolicy(
                    includeRegistrationMarks = _uiState.value.inputIncludeRegistrationMarks,
                    includeCropMarks = _uiState.value.inputIncludeCropMarks,
                    includeBleedMarks = _uiState.value.inputIncludeBleedMarks,
                    includeColorBars = _uiState.value.inputIncludeColorBars,
                    includePlateSlugs = _uiState.value.inputIncludePlateSlugs
                )

                val ctpSpec = CtpOutputGenerationEngine.generateFromSignatureImposition(
                    signatureSpec = signatureSpec,
                    plateDimensionSpec = plateDim,
                    resolutionDpi = _uiState.value.inputResolutionDpi,
                    screeningMethod = _uiState.value.inputScreeningMethod,
                    screenRulingLpi = _uiState.value.inputScreenRulingLpi.toBigDecimalOrNull() ?: BigDecimal("175.0000"),
                    markPolicy = markPolicy,
                    colorSeparations = listOf(
                        PlateColorSeparation.CYAN,
                        PlateColorSeparation.MAGENTA,
                        PlateColorSeparation.YELLOW,
                        PlateColorSeparation.BLACK
                    ),
                    spotColorNames = if (_uiState.value.inputIncludeSpotVarnish) listOf(_uiState.value.inputSpotColorName) else emptyList()
                )

                val saved = if (ctpService != null) {
                    ctpService.generateFromSignature(
                        tenantId = tenantId,
                        signatureImpositionId = signatureSpec.signatureImpositionId,
                        plateDimensionSpec = plateDim,
                        resolutionDpi = _uiState.value.inputResolutionDpi,
                        screeningMethod = _uiState.value.inputScreeningMethod,
                        screenRulingLpi = _uiState.value.inputScreenRulingLpi.toBigDecimalOrNull() ?: BigDecimal("175.0000"),
                        markPolicy = markPolicy
                    )
                } else ctpSpec

                val dto = saved.toDto()
                val handoffContract = if (ctpService != null) {
                    ctpService.getHandoffContract(tenantId, saved.ctpOutputId)
                } else {
                    Module18Step05CtpHandoffContract(
                        contractVersion = "1.0.0",
                        tenantId = saved.tenantId,
                        ctpOutputId = saved.ctpOutputId,
                        jobId = saved.jobId,
                        orderId = saved.orderId,
                        orderItemId = saved.orderItemId,
                        sourceImpositionType = saved.sourceImpositionType,
                        sourceImpositionId = saved.sourceImpositionId,
                        sourceImpositionHash = saved.sourceImpositionHash,
                        status = saved.status.name,
                        packageVersion = saved.packageVersion,
                        totalPlatesCount = saved.outputPackage.totalPlatesCount,
                        frontPlatesCount = saved.outputPackage.frontPlatesCount,
                        backPlatesCount = saved.outputPackage.backPlatesCount,
                        resolutionDpi = saved.resolutionDpi.dpi,
                        screeningMethod = saved.screeningMethod.name,
                        defaultScreenRulingLpi = saved.defaultScreenRulingLpi,
                        plateWidthMm = saved.plateDimensionSpec.plateWidthMm,
                        plateHeightMm = saved.plateDimensionSpec.plateHeightMm,
                        pressSheetWidthMm = saved.outputPackage.pressSheetWidthMm,
                        pressSheetHeightMm = saved.outputPackage.pressSheetHeightMm,
                        gripperMarginMm = saved.plateDimensionSpec.gripperMarginMm,
                        tailMarginMm = saved.plateDimensionSpec.tailMarginMm,
                        ctpOutputIntegrityHash = saved.integrityHash
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentSpecification = dto,
                        specificationsList = listOf(dto),
                        handoffContractJson = serializeHandoff(handoffContract),
                        successMessage = "Generated ${saved.outputPackage.totalPlatesCount} CTP plates successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to generate CTP package") }
            }
        }
    }

    private fun recalculateLive() {
        generateDefaultCtpPackage()
    }

    fun approveCtpPackage() {
        val current = _uiState.value.currentSpecification ?: return
        scope.launch {
            try {
                if (ctpService != null) {
                    val updated = ctpService.updateStatus(
                        tenantId = tenantId,
                        ctpOutputId = current.ctpOutputId,
                        newStatus = CtpOutputStatus.APPROVED,
                        actor = "prepress_manager",
                        reason = "Approved for CTP RIP imaging"
                    )
                    _uiState.update { it.copy(currentSpecification = updated.toDto(), successMessage = "Package APPROVED for RIP") }
                } else {
                    _uiState.update {
                        it.copy(
                            currentSpecification = current.copy(status = CtpOutputStatus.APPROVED.name),
                            successMessage = "Package APPROVED for RIP"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to approve CTP package") }
            }
        }
    }

    private fun serializeHandoff(contract: Module18Step05CtpHandoffContract): String {
        return """
        {
          "contractVersion": "${contract.contractVersion}",
          "tenantId": "${contract.tenantId}",
          "ctpOutputId": "${contract.ctpOutputId}",
          "jobId": "${contract.jobId}",
          "orderId": "${contract.orderId}",
          "orderItemId": "${contract.orderItemId}",
          "sourceImpositionType": "${contract.sourceImpositionType}",
          "sourceImpositionId": "${contract.sourceImpositionId}",
          "sourceImpositionHash": "${contract.sourceImpositionHash}",
          "status": "${contract.status}",
          "packageVersion": ${contract.packageVersion},
          "totalPlatesCount": ${contract.totalPlatesCount},
          "frontPlatesCount": ${contract.frontPlatesCount},
          "backPlatesCount": ${contract.backPlatesCount},
          "resolutionDpi": ${contract.resolutionDpi},
          "screeningMethod": "${contract.screeningMethod}",
          "defaultScreenRulingLpi": "${contract.defaultScreenRulingLpi}",
          "plateDimensionsMm": "${contract.plateWidthMm} x ${contract.plateHeightMm}",
          "pressSheetDimensionsMm": "${contract.pressSheetWidthMm} x ${contract.pressSheetHeightMm}",
          "gripperMarginMm": "${contract.gripperMarginMm}",
          "tailMarginMm": "${contract.tailMarginMm}",
          "ctpOutputIntegrityHash": "${contract.ctpOutputIntegrityHash}",
          "generatedTimestamp": "${contract.generatedTimestamp}"
        }
        """.trimIndent()
    }
}
