package com.sucharu.sucharupro.ui.features.production.jobcosting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.jobcosting.*
import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.service.jobcosting.ProductionJobCostingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ProductionJobCostingViewModel(
    private val costingService: ProductionJobCostingService,
    private val defaultTenantId: String = "TENANT-001",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ProductionJobCostingUiState())
    val uiState: StateFlow<ProductionJobCostingUiState> = _uiState.asStateFlow()

    fun loadCostingDataForJob(jobId: String, tenantId: String = defaultTenantId) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, currentJobId = jobId)
        scope.launch {
            try {
                val actualCost = costingService.getActualJobCostByJob(tenantId, jobId)
                val variance = costingService.getVarianceSummaryByJob(tenantId, jobId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    actualCostRecord = actualCost?.toDto(),
                    varianceSummary = variance?.toDto()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load job costing data for $jobId"
                )
            }
        }
    }

    fun calculateActualJobCost(
        jobId: String,
        orderId: String,
        manufacturedGoodQuantity: BigDecimal,
        packagingUnitRate: BigDecimal = BigDecimal("25.0000"),
        overheadAllocationRate: BigDecimal = BigDecimal("0.1000"),
        actor: String = "cost-accountant",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                val record = costingService.calculateActualJobCost(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    orderId = orderId,
                    manufacturedGoodQuantity = manufacturedGoodQuantity,
                    packagingUnitRate = packagingUnitRate,
                    overheadAllocationRate = overheadAllocationRate,
                    actor = actor
                )
                _uiState.value = _uiState.value.copy(
                    actualCostRecord = record.toDto(),
                    isCalculateCostDialogOpen = false,
                    successMessage = "Actual manufacturing cost calculated: ${record.grandTotalActualCost}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to calculate actual job cost")
            }
        }
    }

    fun calculateJobCostVariance(
        jobId: String,
        quotedSellingPrice: BigDecimal,
        estimatedTotalCost: BigDecimal,
        estimatedMaterialCost: BigDecimal,
        estimatedLaborCost: BigDecimal,
        estimatedMachineCost: BigDecimal,
        orderQuantity: BigDecimal,
        actor: String = "cost-accountant",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                val variance = costingService.calculateJobCostVariance(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    quotedSellingPrice = quotedSellingPrice,
                    estimatedTotalCost = estimatedTotalCost,
                    estimatedMaterialCost = estimatedMaterialCost,
                    estimatedLaborCost = estimatedLaborCost,
                    estimatedMachineCost = estimatedMachineCost,
                    orderQuantity = orderQuantity,
                    actor = actor
                )
                _uiState.value = _uiState.value.copy(
                    varianceSummary = variance.toDto(),
                    isVarianceDialogOpen = false,
                    successMessage = "Manufacturing cost variance analyzed: Total Variance = ${variance.totalCostVariance}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to analyze variance")
            }
        }
    }

    fun reconcileJobCosting(
        jobId: String,
        actor: String = "cost-auditor",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                val recon = costingService.reconcileJobCosting(tenantId, jobId, actor)
                _uiState.value = _uiState.value.copy(
                    reconciliationResult = recon.toDto(),
                    successMessage = "8-Way Manufacturing Cost Reconciliation Certified."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to reconcile job costing")
            }
        }
    }

    fun fetchHandoffContract(jobId: String, tenantId: String = defaultTenantId) {
        scope.launch {
            try {
                val contract = costingService.getAiHandoffContract(tenantId, jobId)
                _uiState.value = _uiState.value.copy(
                    handoffContract = contract.toDto(),
                    isHandoffContractDialogOpen = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to export AI handoff contract")
            }
        }
    }

    fun openCalculateCostDialog() { _uiState.value = _uiState.value.copy(isCalculateCostDialogOpen = true) }
    fun closeCalculateCostDialog() { _uiState.value = _uiState.value.copy(isCalculateCostDialogOpen = false) }

    fun openVarianceDialog() { _uiState.value = _uiState.value.copy(isVarianceDialogOpen = true) }
    fun closeVarianceDialog() { _uiState.value = _uiState.value.copy(isVarianceDialogOpen = false) }

    fun closeHandoffContractDialog() { _uiState.value = _uiState.value.copy(isHandoffContractDialogOpen = false) }
}
