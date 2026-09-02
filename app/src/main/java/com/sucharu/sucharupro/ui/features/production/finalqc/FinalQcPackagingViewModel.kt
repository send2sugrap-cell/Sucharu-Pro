package com.sucharu.sucharupro.ui.features.production.finalqc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.finalqc.*
import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.service.finalqc.FinalQcPackagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class FinalQcPackagingViewModel(
    private val finalQcService: FinalQcPackagingService,
    private val defaultTenantId: String = "TENANT-001",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(FinalQcPackagingUiState())
    val uiState: StateFlow<FinalQcPackagingUiState> = _uiState.asStateFlow()

    fun loadQualityDataForJob(jobId: String, tenantId: String = defaultTenantId) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, currentJobId = jobId)
        scope.launch {
            try {
                val inspections = finalQcService.listInspectionsByJob(tenantId, jobId)
                val defects = finalQcService.listDefectsByJob(tenantId, jobId)
                val packagings = finalQcService.listPackagingRecordsByJob(tenantId, jobId)
                val releases = finalQcService.listReleaseRecordsByJob(tenantId, jobId)
                val variance = finalQcService.getQualityVarianceSummary(tenantId, jobId)
                val recon = finalQcService.reconcileFinalQcPackaging(tenantId, jobId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    inspections = inspections.map { it.toDto() },
                    defects = defects.map { it.toDto() },
                    packagingRecords = packagings.map { it.toDto() },
                    releaseRecords = releases.map { it.toDto() },
                    varianceSummary = variance.toDto(),
                    reconciliationResult = recon.toDto()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load final QC data for job $jobId"
                )
            }
        }
    }

    fun createInspection(
        jobId: String,
        orderId: String,
        samplePlanType: InspectionSamplePlanType,
        totalLotQuantity: BigDecimal,
        sampleSize: BigDecimal,
        checklist: List<QcChecklistItem>,
        inspectorId: String,
        inspectorName: String,
        notes: String? = null,
        actor: String = inspectorName,
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                finalQcService.createFinalQcInspection(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    orderId = orderId,
                    samplePlanType = samplePlanType,
                    totalLotQuantity = totalLotQuantity,
                    sampleSize = sampleSize,
                    checklist = checklist,
                    inspectorId = inspectorId,
                    inspectorName = inspectorName,
                    notes = notes,
                    actor = actor
                )
                loadQualityDataForJob(jobId, tenantId)
                _uiState.value = _uiState.value.copy(
                    isInspectionDialogOpen = false,
                    successMessage = "Final QC Inspection started."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to create inspection")
            }
        }
    }

    fun completeInspection(
        inspectionId: String,
        acceptedQty: BigDecimal,
        rejectedQty: BigDecimal,
        reworkQty: BigDecimal = BigDecimal.ZERO,
        notes: String? = null,
        actor: String = "inspector",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                finalQcService.completeFinalQcInspection(
                    tenantId = tenantId,
                    inspectionId = inspectionId,
                    acceptedQuantity = acceptedQty,
                    rejectedQuantity = rejectedQty,
                    reworkQuantity = reworkQty,
                    notes = notes,
                    actor = actor
                )
                val currentJob = _uiState.value.currentJobId
                if (currentJob != null) loadQualityDataForJob(currentJob, tenantId)
                _uiState.value = _uiState.value.copy(successMessage = "Inspection sign-off completed.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to complete inspection")
            }
        }
    }

    fun recordDefectContainment(
        jobId: String,
        inspectionId: String,
        rootCauseStage: ProductionStageType,
        defectType: DefectClassificationType,
        severity: DefectSeverity,
        defectQty: BigDecimal,
        disposition: ContainmentDisposition,
        quarantineLocation: String,
        reworkWorkOrderId: String? = null,
        rootCauseDetails: String,
        actor: String = "inspector",
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                finalQcService.recordDefectContainment(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    inspectionId = inspectionId,
                    rootCauseStage = rootCauseStage,
                    defectType = defectType,
                    severity = severity,
                    defectQuantity = defectQty,
                    disposition = disposition,
                    quarantineLocation = quarantineLocation,
                    reworkWorkOrderId = reworkWorkOrderId,
                    rootCauseDetails = rootCauseDetails,
                    actor = actor
                )
                loadQualityDataForJob(jobId, tenantId)
                _uiState.value = _uiState.value.copy(
                    isDefectDialogOpen = false,
                    successMessage = "Defect contained and quarantined."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to contain defect")
            }
        }
    }

    fun createPackagingRecord(
        jobId: String,
        inspectionId: String,
        packagingType: PackagingType,
        unitsPerPackage: BigDecimal,
        totalPackageCount: Int,
        palletId: String? = null,
        cartonRange: String? = null,
        grossWeightKg: BigDecimal? = null,
        packagedBy: String,
        notes: String? = null,
        actor: String = packagedBy,
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                finalQcService.createPackagingRecord(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    inspectionId = inspectionId,
                    packagingType = packagingType,
                    unitsPerPackage = unitsPerPackage,
                    totalPackageCount = totalPackageCount,
                    palletIdentifier = palletId,
                    cartonNumbersRange = cartonRange,
                    grossWeightKg = grossWeightKg,
                    packagedBy = packagedBy,
                    notes = notes,
                    actor = actor
                )
                loadQualityDataForJob(jobId, tenantId)
                _uiState.value = _uiState.value.copy(
                    isPackagingDialogOpen = false,
                    successMessage = "Packaging record created with barcode slip."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to create packaging record")
            }
        }
    }

    fun authorizeRelease(
        jobId: String,
        orderId: String,
        inspectionId: String,
        packagingId: String,
        releasedQty: BigDecimal,
        destination: String,
        authorizedBy: String,
        notes: String? = null,
        actor: String = authorizedBy,
        tenantId: String = defaultTenantId
    ) {
        scope.launch {
            try {
                finalQcService.authorizeFinishedGoodsRelease(
                    tenantId = tenantId,
                    executionJobId = jobId,
                    orderId = orderId,
                    inspectionId = inspectionId,
                    packagingId = packagingId,
                    releasedQuantity = releasedQty,
                    destination = destination,
                    authorizedBy = authorizedBy,
                    notes = notes,
                    actor = actor
                )
                loadQualityDataForJob(jobId, tenantId)
                _uiState.value = _uiState.value.copy(
                    isReleaseDialogOpen = false,
                    successMessage = "Finished goods release certified with SHA-256 integrity hash."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to authorize release")
            }
        }
    }

    fun fetchHandoffContract(jobId: String, tenantId: String = defaultTenantId) {
        scope.launch {
            try {
                val contract = finalQcService.getAiHandoffContract(tenantId, jobId)
                _uiState.value = _uiState.value.copy(
                    handoffContract = contract.toDto(),
                    isHandoffContractDialogOpen = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to export AI handoff contract")
            }
        }
    }

    fun openInspectionDialog() { _uiState.value = _uiState.value.copy(isInspectionDialogOpen = true) }
    fun closeInspectionDialog() { _uiState.value = _uiState.value.copy(isInspectionDialogOpen = false) }

    fun openDefectDialog(inspectionId: String) { _uiState.value = _uiState.value.copy(isDefectDialogOpen = true, activeInspectionId = inspectionId) }
    fun closeDefectDialog() { _uiState.value = _uiState.value.copy(isDefectDialogOpen = false) }

    fun openPackagingDialog(inspectionId: String) { _uiState.value = _uiState.value.copy(isPackagingDialogOpen = true, activeInspectionId = inspectionId) }
    fun closePackagingDialog() { _uiState.value = _uiState.value.copy(isPackagingDialogOpen = false) }

    fun openReleaseDialog(inspectionId: String) { _uiState.value = _uiState.value.copy(isReleaseDialogOpen = true, activeInspectionId = inspectionId) }
    fun closeReleaseDialog() { _uiState.value = _uiState.value.copy(isReleaseDialogOpen = false) }

    fun closeHandoffContractDialog() { _uiState.value = _uiState.value.copy(isHandoffContractDialogOpen = false) }
}
