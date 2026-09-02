package com.sucharu.sucharupro.ui.features.production.jobclosure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.jobclosure.*
import com.sucharu.sucharupro.domain.model.jobclosure.*
import com.sucharu.sucharupro.domain.service.jobclosure.ProductionJobClosureService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ProductionJobClosureViewModel(
    private val closureService: ProductionJobClosureService,
    private val defaultTenantId: String = "TENANT-001",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ProductionJobClosureUiState())
    val uiState: StateFlow<ProductionJobClosureUiState> = _uiState.asStateFlow()

    fun loadClosureDataForJob(jobId: String, tenantId: String = defaultTenantId) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, currentJobId = jobId)
        scope.launch {
            try {
                val record = closureService.getClosureRecordByJob(tenantId, jobId)
                val scorecard = closureService.getScorecardByJob(tenantId, jobId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    closureRecord = record?.toDto(),
                    scorecard = scorecard?.toDto(),
                    readinessAudit = record?.readinessAudit?.toDto()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load job closure data for $jobId"
                )
            }
        }
    }

    fun auditReadiness(
        jobId: String,
        orderId: String,
        actor: String = "closure-auditor",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                val audit = closureService.auditJobClosureReadiness(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    orderId = orderId,
                    jobExecution = null,
                    actor = actor
                )
                _uiState.value = _uiState.value.copy(
                    readinessAudit = audit.toDto(),
                    successMessage = "Pre-closure audit complete: Ready for closure = ${audit.isReadyForClosure}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to perform pre-closure audit")
            }
        }
    }

    fun closeAndSealJob(
        jobId: String,
        orderId: String,
        orderQuantity: BigDecimal,
        goodUnitsReleased: BigDecimal,
        estimatedTotalCost: BigDecimal,
        actualTotalCost: BigDecimal,
        totalCostVariance: BigDecimal,
        reworkOrScrapUnits: BigDecimal = BigDecimal.ZERO,
        machineEfficiency: BigDecimal = BigDecimal("85.0000"),
        onTime: Boolean = true,
        primaryDowntimeDrivers: List<String> = emptyList(),
        scrapAndDefectTakeaways: List<String> = emptyList(),
        costVarianceTakeaways: List<String> = emptyList(),
        operationalRecommendations: List<String> = emptyList(),
        actor: String = "plant-manager",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                val record = closureService.closeAndSealJob(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    orderId = orderId,
                    orderQuantity = orderQuantity,
                    goodUnitsReleased = goodUnitsReleased,
                    estimatedTotalCost = estimatedTotalCost,
                    actualTotalCost = actualTotalCost,
                    totalCostVariance = totalCostVariance,
                    reworkOrScrapUnits = reworkOrScrapUnits,
                    machineEfficiency = machineEfficiency,
                    onTime = onTime,
                    primaryDowntimeDrivers = primaryDowntimeDrivers,
                    scrapAndDefectTakeaways = scrapAndDefectTakeaways,
                    costVarianceTakeaways = costVarianceTakeaways,
                    operationalRecommendations = operationalRecommendations,
                    actor = actor
                )
                _uiState.value = _uiState.value.copy(
                    closureRecord = record.toDto(),
                    scorecard = record.scorecard.toDto(),
                    readinessAudit = record.readinessAudit.toDto(),
                    isCloseJobDialogOpen = false,
                    successMessage = "Job sealed & closed successfully. Master Seal Hash: ${record.masterCertificate.masterSealHash.take(16)}..."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to close and seal production job")
            }
        }
    }

    fun fetchHandoffContract(jobId: String, tenantId: String = defaultTenantId) {
        scope.launch {
            try {
                val contract = closureService.getAiHandoffContract(tenantId, jobId)
                _uiState.value = _uiState.value.copy(
                    handoffContract = contract.toDto(),
                    isHandoffContractDialogOpen = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to export AI handoff contract")
            }
        }
    }

    fun openCloseJobDialog() { _uiState.value = _uiState.value.copy(isCloseJobDialogOpen = true) }
    fun closeCloseJobDialog() { _uiState.value = _uiState.value.copy(isCloseJobDialogOpen = false) }

    fun openAuditDetailsDialog() { _uiState.value = _uiState.value.copy(isAuditDetailsDialogOpen = true) }
    fun closeAuditDetailsDialog() { _uiState.value = _uiState.value.copy(isAuditDetailsDialogOpen = false) }

    fun closeHandoffContractDialog() { _uiState.value = _uiState.value.copy(isHandoffContractDialogOpen = false) }
}
