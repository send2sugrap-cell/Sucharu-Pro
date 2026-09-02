package com.sucharu.sucharupro.ui.features.production.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.shopfloortracking.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.DowntimeCategory
import com.sucharu.sucharupro.domain.service.shopfloortracking.ShopFloorTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ShopFloorTrackingViewModel(
    private val trackingService: ShopFloorTrackingService,
    private val defaultTenantId: String = "TENANT-001",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ShopFloorTrackingUiState())
    val uiState: StateFlow<ShopFloorTrackingUiState> = _uiState.asStateFlow()

    fun loadTrackingDataForJob(jobId: String, tenantId: String = defaultTenantId) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, currentJobId = jobId)
        scope.launch {
            try {
                val times = trackingService.listOperatorTimeRecordsByJob(tenantId, jobId)
                val materials = trackingService.listMaterialConsumptionsByJob(tenantId, jobId)
                val telemetry = trackingService.listMachineTelemetryByJob(tenantId, jobId)
                val handovers = trackingService.listStageHandoversByJob(tenantId, jobId)
                val variance = trackingService.getExecutionVarianceSummary(tenantId, jobId)
                val recon = trackingService.reconcileShopFloorExecution(tenantId, jobId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operatorTimeRecords = times.map { it.toDto() },
                    materialConsumptions = materials.map { it.toDto() },
                    telemetryLogs = telemetry.map { it.toDto() },
                    stageHandovers = handovers.map { it.toDto() },
                    varianceSummary = variance.toDto(),
                    reconciliationResult = recon.toDto()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load shop floor tracking data for job $jobId"
                )
            }
        }
    }

    fun startWorkOrder(
        workOrderId: String,
        jobId: String,
        orderId: String,
        sequenceNumber: Int,
        stageType: ProductionStageType,
        machineId: String,
        machineName: String,
        operatorId: String,
        operatorName: String,
        isSetup: Boolean = true,
        actor: String = "operator",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                trackingService.startWorkOrderExecution(
                    tenantId = tenantId,
                    workOrderId = workOrderId,
                    executionJobId = jobId,
                    orderId = orderId,
                    sequenceNumber = sequenceNumber,
                    stageType = stageType,
                    machineId = machineId,
                    machineName = machineName,
                    operatorId = operatorId,
                    operatorName = operatorName,
                    isSetup = isSetup,
                    actor = actor
                )
                loadTrackingDataForJob(jobId, tenantId)
                _uiState.value = _uiState.value.copy(successMessage = "Work order $workOrderId started.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to start work order")
            }
        }
    }

    fun pauseWorkOrder(
        workOrderId: String,
        pauseReason: String,
        downtimeCategory: DowntimeCategory?,
        actor: String = "operator",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                trackingService.pauseWorkOrderExecution(tenantId, workOrderId, pauseReason, downtimeCategory, actor)
                val currentJob = _uiState.value.currentJobId
                if (currentJob != null) loadTrackingDataForJob(currentJob, tenantId)
                _uiState.value = _uiState.value.copy(
                    isPauseDialogOpen = false,
                    successMessage = "Work order $workOrderId paused."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to pause work order")
            }
        }
    }

    fun resumeWorkOrder(
        workOrderId: String,
        actor: String = "operator",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                trackingService.resumeWorkOrderExecution(tenantId, workOrderId, actor)
                val currentJob = _uiState.value.currentJobId
                if (currentJob != null) loadTrackingDataForJob(currentJob, tenantId)
                _uiState.value = _uiState.value.copy(successMessage = "Work order $workOrderId resumed.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to resume work order")
            }
        }
    }

    fun recordWorkOrderOutput(
        workOrderId: String,
        goodQty: BigDecimal,
        scrapQty: BigDecimal,
        setupMins: Int,
        runMins: Int,
        downtimeMins: Int,
        isCompleted: Boolean,
        actor: String = "operator",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                trackingService.recordWorkOrderOutput(
                    tenantId = tenantId,
                    workOrderId = workOrderId,
                    additionalGoodQuantity = goodQty,
                    additionalScrapQuantity = scrapQty,
                    additionalSetupMinutes = setupMins,
                    additionalRunMinutes = runMins,
                    additionalDowntimeMinutes = downtimeMins,
                    isCompleted = isCompleted,
                    actor = actor
                )
                val currentJob = _uiState.value.currentJobId
                if (currentJob != null) loadTrackingDataForJob(currentJob, tenantId)
                _uiState.value = _uiState.value.copy(
                    isOutputDialogOpen = false,
                    successMessage = "Work order $workOrderId output recorded."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to record output")
            }
        }
    }

    fun recordMaterialConsumption(
        workOrderId: String,
        jobId: String,
        stageType: ProductionStageType,
        materialCode: String,
        materialName: String,
        unitOfMeasure: String,
        plannedQty: BigDecimal,
        actualQty: BigDecimal,
        scrapQty: BigDecimal = BigDecimal.ZERO,
        batchLot: String? = null,
        notes: String? = null,
        actor: String = "operator",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                trackingService.recordMaterialConsumption(
                    tenantId = tenantId,
                    workOrderId = workOrderId,
                    executionJobId = jobId,
                    stageType = stageType,
                    materialCode = materialCode,
                    materialName = materialName,
                    unitOfMeasure = unitOfMeasure,
                    plannedQuantity = plannedQty,
                    actualQuantity = actualQty,
                    scrapQuantity = scrapQty,
                    batchLotNumber = batchLot,
                    notes = notes,
                    actor = actor
                )
                loadTrackingDataForJob(jobId, tenantId)
                _uiState.value = _uiState.value.copy(
                    isMaterialDialogOpen = false,
                    successMessage = "Material consumption recorded."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to record material consumption")
            }
        }
    }

    fun createStageHandover(
        jobId: String,
        fromWorkOrderId: String,
        fromStage: ProductionStageType,
        toWorkOrderId: String?,
        toStage: ProductionStageType?,
        plannedOutputQty: BigDecimal,
        actualGoodQty: BigDecimal,
        scrapQty: BigDecimal,
        discrepancyNotes: String? = null,
        actor: String = "operator",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                trackingService.createStageHandover(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    fromWorkOrderId = fromWorkOrderId,
                    fromStage = fromStage,
                    toWorkOrderId = toWorkOrderId,
                    toStage = toStage,
                    plannedOutputQuantity = plannedOutputQty,
                    actualGoodQuantity = actualGoodQty,
                    scrapQuantity = scrapQty,
                    discrepancyNotes = discrepancyNotes,
                    actor = actor
                )
                loadTrackingDataForJob(jobId, tenantId)
                _uiState.value = _uiState.value.copy(
                    isHandoverDialogOpen = false,
                    successMessage = "Stage handover initiated for verification."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to initiate handover")
            }
        }
    }

    fun acceptStageHandover(
        handoverId: String,
        actor: String = "operator",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                trackingService.acceptStageHandover(tenantId, handoverId, actor)
                val currentJob = _uiState.value.currentJobId
                if (currentJob != null) loadTrackingDataForJob(currentJob, tenantId)
                _uiState.value = _uiState.value.copy(successMessage = "Stage handover accepted.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to accept handover")
            }
        }
    }

    fun fetchHandoffContract(jobId: String, tenantId: String = defaultTenantId) {
        scope.launch {
            try {
                val contract = trackingService.getAiHandoffContract(tenantId, jobId)
                _uiState.value = _uiState.value.copy(
                    handoffContract = contract.toDto(),
                    isHandoffContractDialogOpen = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to fetch AI handoff contract")
            }
        }
    }

    fun openPauseDialog(workOrderId: String) {
        _uiState.value = _uiState.value.copy(isPauseDialogOpen = true, activeWorkOrderId = workOrderId)
    }

    fun closePauseDialog() {
        _uiState.value = _uiState.value.copy(isPauseDialogOpen = false)
    }

    fun openOutputDialog(workOrderId: String) {
        _uiState.value = _uiState.value.copy(isOutputDialogOpen = true, activeWorkOrderId = workOrderId)
    }

    fun closeOutputDialog() {
        _uiState.value = _uiState.value.copy(isOutputDialogOpen = false)
    }

    fun openMaterialDialog(workOrderId: String) {
        _uiState.value = _uiState.value.copy(isMaterialDialogOpen = true, activeWorkOrderId = workOrderId)
    }

    fun closeMaterialDialog() {
        _uiState.value = _uiState.value.copy(isMaterialDialogOpen = false)
    }

    fun openHandoverDialog(workOrderId: String) {
        _uiState.value = _uiState.value.copy(isHandoverDialogOpen = true, activeWorkOrderId = workOrderId)
    }

    fun closeHandoverDialog() {
        _uiState.value = _uiState.value.copy(isHandoverDialogOpen = false)
    }

    fun closeHandoffContractDialog() {
        _uiState.value = _uiState.value.copy(isHandoffContractDialogOpen = false)
    }
}
