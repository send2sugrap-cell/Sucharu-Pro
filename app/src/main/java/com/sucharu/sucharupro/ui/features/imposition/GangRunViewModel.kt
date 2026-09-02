package com.sucharu.sucharupro.ui.features.imposition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.imposition.GangRunCandidateItemDto
import com.sucharu.sucharupro.data.api.model.imposition.GangRunSpecificationResponseDto
import com.sucharu.sucharupro.data.api.model.imposition.toDto
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import com.sucharu.sucharupro.domain.service.imposition.GangRunClusteringEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for Multi-Job Gang-Run Batching Command Center.
 * Module 18 Step 02.
 */
class GangRunViewModel : ViewModel() {

    private val engine = GangRunClusteringEngine()

    private val _uiState = MutableStateFlow(GangRunUiState())
    val uiState: StateFlow<GangRunUiState> = _uiState.asStateFlow()

    init {
        optimizeGangRun()
    }

    fun onTabSelected(tab: Int) = _uiState.update { it.copy(selectedTab = tab) }
    fun onBatchNameChanged(v: String) = _uiState.update { it.copy(batchName = v) }
    fun onSheetWidthChanged(v: String) = _uiState.update { it.copy(parentSheetWidthMm = v) }
    fun onSheetHeightChanged(v: String) = _uiState.update { it.copy(parentSheetHeightMm = v) }

    fun addCandidate(item: GangRunCandidateItemDto) {
        _uiState.update { it.copy(candidatePool = it.candidatePool + item) }
        optimizeGangRun()
    }

    fun removeCandidate(jobId: String) {
        _uiState.update { it.copy(candidatePool = it.candidatePool.filterNot { item -> item.jobId == jobId }) }
        optimizeGangRun()
    }

    fun optimizeGangRun() {
        viewModelScope.launch {
            val s = _uiState.value
            if (s.candidatePool.isEmpty()) {
                _uiState.update { it.copy(currentSpecification = null, errorMessage = "Candidate pool is empty.") }
                return@launch
            }

            _uiState.update { it.copy(isOptimizing = true, errorMessage = null, successMessage = null) }
            try {
                val candidateItems = s.candidatePool.map {
                    GangRunCandidateItem(
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
                        printingSideOption = PrintingSideOption.valueOf(it.printingSideOption)
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
                val policy = try {
                    GangRunClusteringPolicy.valueOf(s.clusteringPolicy)
                } catch (e: Exception) {
                    GangRunClusteringPolicy.STRICT_IDENTICAL_SUBSTRATE
                }

                val clusters = engine.formClusters(candidateItems, policy)
                if (clusters.isEmpty()) {
                    _uiState.update { it.copy(isOptimizing = false, errorMessage = "No compatible clusters could be formed.") }
                    return@launch
                }

                // Optimize the primary cluster
                val primaryCluster = clusters.first()
                val domainSpec = engine.optimizeGangRun(
                    tenantId = "TENANT-CLIENT",
                    batchName = s.batchName,
                    cluster = primaryCluster,
                    parentSheetDimension = parentDim,
                    margins = margins,
                    spacing = spacing,
                    actor = "client_user"
                )

                val dto = domainSpec.toDto()
                _uiState.update {
                    it.copy(
                        isOptimizing = false,
                        currentSpecification = dto,
                        specificationsList = listOf(dto) + it.specificationsList.filterNot { old -> old.gangRunId == dto.gangRunId },
                        successMessage = "Gang-run optimized: ${dto.allocations.size} jobs co-located, ${dto.commonRequiredSheets} parent sheets required with ${dto.sheetYieldPercentage}% yield."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isOptimizing = false,
                        errorMessage = e.message ?: "Failed to optimize gang-run batch"
                    )
                }
            }
        }
    }

    fun selectSpecification(spec: GangRunSpecificationResponseDto) {
        _uiState.update {
            it.copy(
                currentSpecification = spec,
                selectedTab = 0
            )
        }
    }
}
