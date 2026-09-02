package com.sucharu.sucharupro.ui.features.imposition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.imposition.SignatureImpositionSpecificationResponseDto
import com.sucharu.sucharupro.data.api.model.imposition.toDto
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.service.imposition.SignatureImpositionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for Multi-Page Signature Imposition Command Center.
 * Module 18 Step 04.
 */
class SignatureViewModel(
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SignatureUiState())
    val uiState: StateFlow<SignatureUiState> = _uiState.asStateFlow()

    init {
        optimizeSignature()
    }

    fun onTabSelected(tab: Int) = _uiState.update { it.copy(selectedTab = tab) }
    fun onNameChanged(v: String) = _uiState.update { it.copy(name = v) }
    fun onJobIdChanged(v: String) = _uiState.update { it.copy(jobId = v) }
    fun onOrderIdChanged(v: String) = _uiState.update { it.copy(orderId = v) }
    fun onTotalPagesChanged(v: String) = _uiState.update { it.copy(totalPages = v) }
    fun onSignaturePageCountChanged(v: String) = _uiState.update { it.copy(signaturePageCount = v) }
    fun onBindingMethodChanged(v: String) = _uiState.update { it.copy(bindingMethod = v) }
    fun onSheetTurningMethodChanged(v: String) = _uiState.update { it.copy(sheetTurningMethod = v) }
    fun onFoldingSchemeChanged(v: String) = _uiState.update { it.copy(foldingScheme = v) }
    fun onPageWidthChanged(v: String) = _uiState.update { it.copy(pageWidthMm = v) }
    fun onPageHeightChanged(v: String) = _uiState.update { it.copy(pageHeightMm = v) }
    fun onParentSheetWidthChanged(v: String) = _uiState.update { it.copy(parentSheetWidthMm = v) }
    fun onParentSheetHeightChanged(v: String) = _uiState.update { it.copy(parentSheetHeightMm = v) }
    fun onRequiredQuantityChanged(v: String) = _uiState.update { it.copy(requiredQuantity = v) }
    fun onPaperStockTypeChanged(v: String) = _uiState.update { it.copy(paperStockType = v) }
    fun onGsmChanged(v: String) = _uiState.update { it.copy(gsm = v) }
    fun onSpineGutterChanged(v: String) = _uiState.update { it.copy(spineGutterMm = v) }
    fun onHeadGutterChanged(v: String) = _uiState.update { it.copy(headGutterMm = v) }
    fun onFootGutterChanged(v: String) = _uiState.update { it.copy(footGutterMm = v) }
    fun onFaceTrimChanged(v: String) = _uiState.update { it.copy(faceTrimMm = v) }
    fun onCreepToggleChanged(v: Boolean) = _uiState.update { it.copy(enableCreepCompensation = v) }
    fun onSignatureIndexSelected(index: Int) = _uiState.update { it.copy(selectedSignatureIndex = index) }
    fun onFormSideIndexSelected(index: Int) = _uiState.update { it.copy(selectedFormSideIndex = index) }

    fun optimizeSignature() {
        scope.launch {
            val s = _uiState.value
            _uiState.update { it.copy(isOptimizing = true, errorMessage = null, successMessage = null) }
            try {
                val totalP = s.totalPages.toIntOrNull() ?: 16
                val sigP = s.signaturePageCount.toIntOrNull() ?: 16
                val qty = s.requiredQuantity.toLongOrNull() ?: 1000L
                val gsmVal = s.gsm.toBigDecimalOrNull() ?: BigDecimal("150.0000")
                val customCal = if (s.customCaliperMm.isNotBlank()) s.customCaliperMm.toBigDecimalOrNull() else null

                val pageDim = PrintingDimension(
                    width = s.pageWidthMm.toBigDecimalOrNull() ?: BigDecimal("210.0000"),
                    height = s.pageHeightMm.toBigDecimalOrNull() ?: BigDecimal("297.0000"),
                    unit = MeasurementUnit.MILLIMETERS
                )
                val parentDim = PrintingDimension(
                    width = s.parentSheetWidthMm.toBigDecimalOrNull() ?: BigDecimal("635.0000"),
                    height = s.parentSheetHeightMm.toBigDecimalOrNull() ?: BigDecimal("914.4000"),
                    unit = MeasurementUnit.MILLIMETERS
                )
                val margins = ImpositionMarginSpec(
                    topMm = s.marginTopMm.toBigDecimalOrNull() ?: BigDecimal("10.0000"),
                    bottomMm = s.marginBottomMm.toBigDecimalOrNull() ?: BigDecimal("10.0000"),
                    leftMm = s.marginLeftMm.toBigDecimalOrNull() ?: BigDecimal("10.0000"),
                    rightMm = s.marginRightMm.toBigDecimalOrNull() ?: BigDecimal("10.0000")
                )
                val gutters = SignatureGutterSpec(
                    spineGutterMm = s.spineGutterMm.toBigDecimalOrNull() ?: BigDecimal("4.0000"),
                    headGutterMm = s.headGutterMm.toBigDecimalOrNull() ?: BigDecimal("6.0000"),
                    footGutterMm = s.footGutterMm.toBigDecimalOrNull() ?: BigDecimal("6.0000"),
                    faceTrimMm = s.faceTrimMm.toBigDecimalOrNull() ?: BigDecimal("4.0000"),
                    bleedMm = s.bleedMm.toBigDecimalOrNull() ?: BigDecimal("3.0000")
                )

                val spec = SignatureImpositionEngine.optimizeSignatureImposition(
                    tenantId = "TENANT-001",
                    name = s.name,
                    jobId = s.jobId,
                    orderId = s.orderId,
                    orderItemId = s.orderItemId,
                    productName = s.productName,
                    totalPages = totalP,
                    signaturePageCount = sigP,
                    bindingMethod = BindingMethod.valueOf(s.bindingMethod),
                    sheetTurningMethod = SheetTurningMethod.valueOf(s.sheetTurningMethod),
                    foldingScheme = FoldingScheme.valueOf(s.foldingScheme),
                    pageDimension = pageDim,
                    parentSheetDimension = parentDim,
                    requiredQuantity = qty,
                    paperStockType = PaperStockType.valueOf(s.paperStockType),
                    gsm = gsmVal,
                    customCaliperMm = customCal,
                    marginSpec = margins,
                    gutterSpec = gutters,
                    enableCreepCompensation = s.enableCreepCompensation,
                    actor = "ui_operator"
                )

                val dto = spec.toDto()
                _uiState.update {
                    it.copy(
                        currentSpecification = dto,
                        historySpecifications = listOf(dto) + it.historySpecifications.filterNot { old -> old.signatureImpositionId == dto.signatureImpositionId },
                        isOptimizing = false,
                        successMessage = "Signature Imposition optimized successfully: ${dto.totalSignaturesCount} signature(s), ${dto.commonRequiredSheets} sheets/sig (${dto.sheetUtilizationPercentage}% sheet utilization)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isOptimizing = false, errorMessage = e.message ?: "Failed to optimize signature layout.") }
            }
        }
    }

    fun exportHandoffContract() {
        val spec = _uiState.value.currentSpecification ?: return
        val json = """
            {
              "contractVersion": "1.0.0",
              "signatureImpositionId": "${spec.signatureImpositionId}",
              "tenantId": "${spec.tenantId}",
              "jobId": "${spec.jobId}",
              "orderId": "${spec.orderId}",
              "productName": "${spec.productName}",
              "totalSignatures": ${spec.totalSignaturesCount},
              "signaturePageCount": ${spec.signaturePageCount},
              "sheetsPerSignature": ${spec.commonRequiredSheets},
              "totalParentSheetsRequired": ${spec.totalParentSheetsRequired},
              "bindingMethod": "${spec.bindingMethod}",
              "sheetTurningMethod": "${spec.sheetTurningMethod}",
              "integrityHash": "${spec.integrityHash}"
            }
        """.trimIndent()
        _uiState.update { it.copy(handoffExportedJson = json, successMessage = "Module 19 Substrate Handoff contract ready.") }
    }
}
