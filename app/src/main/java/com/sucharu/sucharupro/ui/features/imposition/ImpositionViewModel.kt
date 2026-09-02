package com.sucharu.sucharupro.ui.features.imposition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.imposition.ImpositionCandidateDto
import com.sucharu.sucharupro.data.api.model.imposition.ImpositionSpecificationResponseDto
import com.sucharu.sucharupro.data.api.model.imposition.toDto
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.service.imposition.SingleJobImpositionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ImpositionViewModel : ViewModel() {

    private val engine = SingleJobImpositionEngine()

    private val _uiState = MutableStateFlow(ImpositionUiState())
    val uiState: StateFlow<ImpositionUiState> = _uiState.asStateFlow()

    init {
        // Initial calculation with default parameters
        calculateAndSaveImposition()
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onJobIdChanged(v: String) = _uiState.update { it.copy(jobId = v) }
    fun onOrderIdChanged(v: String) = _uiState.update { it.copy(orderId = v) }
    fun onOrderItemIdChanged(v: String) = _uiState.update { it.copy(orderItemId = v) }
    fun onProductNameChanged(v: String) = _uiState.update { it.copy(productName = v) }
    fun onItemWidthChanged(v: String) = _uiState.update { it.copy(itemWidthMm = v) }
    fun onItemHeightChanged(v: String) = _uiState.update { it.copy(itemHeightMm = v) }
    fun onSheetWidthChanged(v: String) = _uiState.update { it.copy(sheetWidthMm = v) }
    fun onSheetHeightChanged(v: String) = _uiState.update { it.copy(sheetHeightMm = v) }
    fun onQuantityChanged(v: String) = _uiState.update { it.copy(requiredQuantity = v) }
    fun onOrientationPolicyChanged(v: String) = _uiState.update { it.copy(orientationPolicy = v) }

    fun loadSpecifications() {
        // Refresh calculation
        calculateAndSaveImposition()
    }

    fun calculateAndSaveImposition() {
        viewModelScope.launch {
            val s = _uiState.value
            _uiState.update { it.copy(isCalculating = true, errorMessage = null, successMessage = null) }
            try {
                val itemDim = PrintingDimension(
                    width = s.itemWidthMm.toBigDecimalOrNull() ?: BigDecimal("210.0000"),
                    height = s.itemHeightMm.toBigDecimalOrNull() ?: BigDecimal("297.0000"),
                    unit = MeasurementUnit.MILLIMETERS
                )
                val sheetDim = PrintingDimension(
                    width = s.sheetWidthMm.toBigDecimalOrNull() ?: BigDecimal("635.0000"),
                    height = s.sheetHeightMm.toBigDecimalOrNull() ?: BigDecimal("914.4000"),
                    unit = MeasurementUnit.MILLIMETERS
                )
                val margins = ImpositionMarginSpec(
                    topMm = s.marginTopMm.toBigDecimalOrNull() ?: BigDecimal("10.0000"),
                    bottomMm = s.marginBottomMm.toBigDecimalOrNull() ?: BigDecimal("10.0000"),
                    leftMm = s.marginLeftMm.toBigDecimalOrNull() ?: BigDecimal("10.0000"),
                    rightMm = s.marginRightMm.toBigDecimalOrNull() ?: BigDecimal("10.0000")
                )
                val spacing = ImpositionSpacingSpec(
                    bleedMm = s.bleedMm.toBigDecimalOrNull() ?: BigDecimal("3.0000"),
                    horizontalGutterMm = s.horizontalGutterMm.toBigDecimalOrNull() ?: BigDecimal("4.0000"),
                    verticalGutterMm = s.verticalGutterMm.toBigDecimalOrNull() ?: BigDecimal("4.0000")
                )
                val policy = try {
                    ImpositionOrientationPolicy.valueOf(s.orientationPolicy)
                } catch (e: Exception) {
                    ImpositionOrientationPolicy.AUTO_OPTIMAL
                }
                val qty = s.requiredQuantity.toLongOrNull() ?: 1000L

                val domainSpec = engine.calculateOptimalLayout(
                    tenantId = "TENANT-CLIENT",
                    jobId = s.jobId.takeIf { it.isNotBlank() },
                    orderId = s.orderId,
                    orderItemId = s.orderItemId,
                    calculationId = s.calculationId.takeIf { it.isNotBlank() },
                    productName = s.productName,
                    finishedItemDimension = itemDim,
                    parentSheetDimension = sheetDim,
                    margins = margins,
                    spacing = spacing,
                    orientationPolicy = policy,
                    requiredQuantity = qty,
                    notes = s.notes,
                    actor = "client_user"
                )

                val dto: ImpositionSpecificationResponseDto = domainSpec.toDto()
                _uiState.update {
                    it.copy(
                        isCalculating = false,
                        currentSpecification = dto,
                        specificationsList = listOf(dto) + it.specificationsList.filterNot { item -> item.impositionId == dto.impositionId },
                        candidateBreakdown = dto.candidates,
                        successMessage = "Optimal layout calculated: ${dto.copiesPerSheet} UP (${dto.columns}x${dto.rows}) with ${dto.yieldPercentage}% yield."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCalculating = false,
                        errorMessage = e.message ?: "Failed to calculate imposition layout"
                    )
                }
            }
        }
    }

    fun selectSpecification(spec: ImpositionSpecificationResponseDto) {
        _uiState.update {
            it.copy(
                currentSpecification = spec,
                selectedTab = 0
            )
        }
    }
}
