package com.sucharu.sucharupro.ui.features.imposition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.imposition.DynamicNestingSpecificationResponseDto
import com.sucharu.sucharupro.data.api.model.imposition.NestingCandidateItemDto
import com.sucharu.sucharupro.data.api.model.imposition.toDto
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import com.sucharu.sucharupro.domain.service.imposition.DynamicNestingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for Dynamic 2D Nesting & Wastage Optimization Command Center.
 * Module 18 Step 03.
 */
class NestingViewModel(
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(NestingUiState())
    val uiState: StateFlow<NestingUiState> = _uiState.asStateFlow()

    init {
        optimizeNesting()
    }

    fun onTabSelected(tab: Int) = _uiState.update { it.copy(selectedTab = tab) }
    fun onNameChanged(v: String) = _uiState.update { it.copy(name = v) }
    fun onSheetWidthChanged(v: String) = _uiState.update { it.copy(parentSheetWidthMm = v) }
    fun onSheetHeightChanged(v: String) = _uiState.update { it.copy(parentSheetHeightMm = v) }
    fun onOrientationPolicyChanged(v: String) = _uiState.update { it.copy(orientationPolicy = v) }
    fun onPlacementStrategyChanged(v: String) = _uiState.update { it.copy(placementStrategy = v) }
    fun onMinOffcutChanged(v: String) = _uiState.update { it.copy(minOffcutDimensionMm = v) }

    fun addCandidate(item: NestingCandidateItemDto) {
        _uiState.update { it.copy(candidatePool = it.candidatePool + item) }
        optimizeNesting()
    }

    fun removeCandidate(jobId: String) {
        _uiState.update { it.copy(candidatePool = it.candidatePool.filterNot { item -> item.jobId == jobId }) }
        optimizeNesting()
    }

    fun optimizeNesting() {
        scope.launch {
            val s = _uiState.value
            if (s.candidatePool.isEmpty()) {
                _uiState.update { it.copy(currentSpecification = null, errorMessage = "Candidate pool is empty.") }
                return@launch
            }

            _uiState.update { it.copy(isOptimizing = true, errorMessage = null, successMessage = null) }
            try {
                val candidateItems = s.candidatePool.map {
                    NestingCandidateItem(
                        jobId = it.jobId,
                        orderId = it.orderId,
                        orderItemId = it.orderItemId,
                        productName = it.productName,
                        finishedDimension = PrintingDimension(
                            width = it.finishedWidthMm,
                            height = it.finishedHeightMm,
                            unit = MeasurementUnit.MILLIMETERS
                        ),
                        requiredQuantity = it.requiredQuantity,
                        paperStockType = PaperStockType.valueOf(it.paperStockType),
                        gsm = it.gsm,
                        colorMode = ColorMode.valueOf(it.colorMode),
                        printingSideOption = PrintingSideOption.valueOf(it.printingSideOption),
                        allowRotation = it.allowRotation,
                        priorityScore = it.priorityScore
                    )
                }

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
                val spacing = ImpositionSpacingSpec(
                    bleedMm = s.bleedMm.toBigDecimalOrNull() ?: BigDecimal("3.0000"),
                    horizontalGutterMm = s.horizontalGutterMm.toBigDecimalOrNull() ?: BigDecimal("4.0000"),
                    verticalGutterMm = s.verticalGutterMm.toBigDecimalOrNull() ?: BigDecimal("4.0000")
                )
                val orientationPolicy = try {
                    NestingOrientationPolicy.valueOf(s.orientationPolicy)
                } catch (e: Exception) {
                    NestingOrientationPolicy.ALLOW_ROTATION
                }
                val placementStrategy = try {
                    NestingPlacementStrategy.valueOf(s.placementStrategy)
                } catch (e: Exception) {
                    NestingPlacementStrategy.BOTTOM_LEFT_FILL
                }
                val minOffcut = s.minOffcutDimensionMm.toBigDecimalOrNull() ?: BigDecimal("100.0000")

                val domainSpec = DynamicNestingEngine.optimizeNesting(
                    tenantId = "TENANT-CLIENT",
                    name = s.name,
                    candidateItems = candidateItems,
                    parentSheetDimension = parentDim,
                    marginSpec = margins,
                    spacingSpec = spacing,
                    orientationPolicy = orientationPolicy,
                    placementStrategy = placementStrategy,
                    minOffcutDimensionMm = minOffcut,
                    actor = "client_user"
                )

                val dto = domainSpec.toDto()
                _uiState.update {
                    it.copy(
                        isOptimizing = false,
                        currentSpecification = dto,
                        specificationsList = listOf(dto) + it.specificationsList.filterNot { old -> old.nestingId == dto.nestingId },
                        successMessage = "Nesting optimized: ${dto.totalItemsPlaced} items placed (${dto.jobSummaries.size} jobs), ${dto.commonRequiredSheets} sheets required with ${dto.usableYieldPercentage}% usable yield."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isOptimizing = false,
                        errorMessage = e.message ?: "Failed to optimize dynamic nesting"
                    )
                }
            }
        }
    }

    fun selectSpecification(spec: DynamicNestingSpecificationResponseDto) {
        _uiState.update {
            it.copy(
                currentSpecification = spec,
                selectedTab = 0
            )
        }
    }
}
