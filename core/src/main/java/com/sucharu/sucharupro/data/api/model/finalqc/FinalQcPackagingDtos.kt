package com.sucharu.sucharupro.data.api.model.finalqc

import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal

data class QcChecklistItemDto(
    val checkCode: String,
    val checkTitle: String,
    val isPassed: Boolean,
    val measuredValue: String? = null,
    val toleranceLimit: String? = null,
    val remarks: String? = null
)

data class CreateFinalQcInspectionRequestDto(
    val orderId: String,
    val samplePlanType: String = "FULL_100_PERCENT",
    val totalLotQuantity: BigDecimal,
    val sampleSize: BigDecimal,
    val checklist: List<QcChecklistItemDto> = emptyList(),
    val inspectorId: String,
    val inspectorName: String,
    val notes: String? = null
)

data class CompleteFinalQcInspectionRequestDto(
    val acceptedQuantity: BigDecimal,
    val rejectedQuantity: BigDecimal,
    val reworkQuantity: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null
)

data class RecordDefectContainmentRequestDto(
    val inspectionId: String,
    val rootCauseStage: String,
    val defectType: String,
    val severity: String,
    val defectQuantity: BigDecimal,
    val disposition: String,
    val quarantineLocation: String,
    val reworkWorkOrderId: String? = null,
    val rootCauseDetails: String
)

data class CreatePackagingRecordRequestDto(
    val inspectionId: String,
    val packagingType: String,
    val unitsPerPackage: BigDecimal,
    val totalPackageCount: Int,
    val palletIdentifier: String? = null,
    val cartonNumbersRange: String? = null,
    val grossWeightKg: BigDecimal? = null,
    val packagedBy: String,
    val notes: String? = null
)

data class AuthorizeFinishedGoodsReleaseRequestDto(
    val orderId: String,
    val inspectionId: String,
    val packagingId: String,
    val releasedQuantity: BigDecimal,
    val destination: String = "WAREHOUSE_FINISHED_GOODS",
    val authorizedBy: String,
    val notes: String? = null
)

data class FinalQcInspectionResponseDto(
    val inspectionId: String,
    val executionJobId: String,
    val orderId: String,
    val samplePlanType: String,
    val totalLotQuantity: BigDecimal,
    val sampleSize: BigDecimal,
    val acceptedQuantity: BigDecimal,
    val rejectedQuantity: BigDecimal,
    val reworkQuantity: BigDecimal,
    val status: String,
    val checklist: List<QcChecklistItemDto>,
    val inspectorId: String,
    val inspectorName: String,
    val inspectionNotes: String?,
    val inspectedAt: Long,
    val completedAt: Long?
)

data class DefectContainmentResponseDto(
    val containmentId: String,
    val executionJobId: String,
    val inspectionId: String,
    val rootCauseStage: String,
    val defectType: String,
    val severity: String,
    val defectQuantity: BigDecimal,
    val disposition: String,
    val quarantineLocation: String,
    val reworkWorkOrderId: String?,
    val rootCauseDetails: String,
    val loggedBy: String,
    val loggedAt: Long
)

data class PackagingResponseDto(
    val packagingId: String,
    val executionJobId: String,
    val inspectionId: String,
    val packagingType: String,
    val unitsPerPackage: BigDecimal,
    val totalPackageCount: Int,
    val totalPackagedQuantity: BigDecimal,
    val palletIdentifier: String?,
    val cartonNumbersRange: String?,
    val grossWeightKg: BigDecimal?,
    val packagingSlipBarcode: String,
    val packagedBy: String,
    val packagedAt: Long,
    val notes: String?
)

data class FinishedGoodsReleaseResponseDto(
    val releaseId: String,
    val executionJobId: String,
    val orderId: String,
    val inspectionId: String,
    val packagingId: String,
    val releasedQuantity: BigDecimal,
    val destination: String,
    val status: String,
    val authorizedBy: String,
    val authorizedAt: Long,
    val integrityHash: String,
    val notes: String?
)

data class FinalQcPackagingVarianceResponseDto(
    val executionJobId: String,
    val totalManufacturedOutput: BigDecimal,
    val sampleInspectedQuantity: BigDecimal,
    val totalAcceptedGoodQuantity: BigDecimal,
    val totalRejectedQuantity: BigDecimal,
    val totalReworkQuantity: BigDecimal,
    val overallQualityYieldPercentage: BigDecimal,
    val defectRatePercentage: BigDecimal,
    val totalPackagedQuantity: BigDecimal,
    val packagingBalanceVariance: BigDecimal,
    val isReadyForFullRelease: Boolean,
    val generatedAt: Long
)

data class FinalQcPackagingReconciliationResponseDto(
    val executionJobId: String,
    val outputMatchedInspectionLot: Boolean,
    val samplePlanConsistent: Boolean,
    val defectAccountingBalanced: Boolean,
    val zeroUncontainedCriticalDefects: Boolean,
    val packagingQuantityMatchesAccepted: Boolean,
    val releaseCertificateHashValid: Boolean,
    val multiTenantIsolationVerified: Boolean,
    val isFullyReconciled: Boolean,
    val discrepancies: List<String>,
    val reconciledAt: Long
)

data class Module17Step08FinalQcPackagingHandoffContractDto(
    val contractVersion: String,
    val executionJobId: String,
    val orderId: String,
    val finalInspectionStatus: String,
    val totalGoodQuantityAccepted: BigDecimal,
    val totalDefectQuantity: BigDecimal,
    val qualityYieldPercentage: BigDecimal,
    val defectRatePercentage: BigDecimal,
    val totalPackagedCartons: Int,
    val totalPackagedQuantity: BigDecimal,
    val packagingSlipBarcode: String,
    val releaseStatus: String,
    val releaseCertificateHash: String,
    val isFullyReconciled: Boolean,
    val inspectionSummary: List<String>,
    val defectContainmentSummary: List<String>,
    val exportedAt: Long
)

fun ProductionFinalQcInspection.toDto() = FinalQcInspectionResponseDto(
    inspectionId = inspectionId,
    executionJobId = executionJobId,
    orderId = orderId,
    samplePlanType = samplePlanType.name,
    totalLotQuantity = totalLotQuantity,
    sampleSize = sampleSize,
    acceptedQuantity = acceptedQuantity,
    rejectedQuantity = rejectedQuantity,
    reworkQuantity = reworkQuantity,
    status = status.name,
    checklist = checklist.map { QcChecklistItemDto(it.checkCode, it.checkTitle, it.isPassed, it.measuredValue, it.toleranceLimit, it.remarks) },
    inspectorId = inspectorId,
    inspectorName = inspectorName,
    inspectionNotes = inspectionNotes,
    inspectedAt = inspectedAt,
    completedAt = completedAt
)

fun ProductionDefectContainmentRecord.toDto() = DefectContainmentResponseDto(
    containmentId = containmentId,
    executionJobId = executionJobId,
    inspectionId = inspectionId,
    rootCauseStage = rootCauseStage.name,
    defectType = defectType.name,
    severity = severity.name,
    defectQuantity = defectQuantity,
    disposition = disposition.name,
    quarantineLocation = quarantineLocation,
    reworkWorkOrderId = reworkWorkOrderId,
    rootCauseDetails = rootCauseDetails,
    loggedBy = loggedBy,
    loggedAt = loggedAt
)

fun ProductionPackagingRecord.toDto() = PackagingResponseDto(
    packagingId = packagingId,
    executionJobId = executionJobId,
    inspectionId = inspectionId,
    packagingType = packagingType.name,
    unitsPerPackage = unitsPerPackage,
    totalPackageCount = totalPackageCount,
    totalPackagedQuantity = totalPackagedQuantity,
    palletIdentifier = palletIdentifier,
    cartonNumbersRange = cartonNumbersRange,
    grossWeightKg = grossWeightKg,
    packagingSlipBarcode = packagingSlipBarcode,
    packagedBy = packagedBy,
    packagedAt = packagedAt,
    notes = notes
)

fun FinishedGoodsReleaseRecord.toDto() = FinishedGoodsReleaseResponseDto(
    releaseId = releaseId,
    executionJobId = executionJobId,
    orderId = orderId,
    inspectionId = inspectionId,
    packagingId = packagingId,
    releasedQuantity = releasedQuantity,
    destination = destination,
    status = status.name,
    authorizedBy = authorizedBy,
    authorizedAt = authorizedAt,
    integrityHash = integrityHash,
    notes = notes
)

fun FinalQcPackagingVarianceSummary.toDto() = FinalQcPackagingVarianceResponseDto(
    executionJobId = executionJobId,
    totalManufacturedOutput = totalManufacturedOutput,
    sampleInspectedQuantity = sampleInspectedQuantity,
    totalAcceptedGoodQuantity = totalAcceptedGoodQuantity,
    totalRejectedQuantity = totalRejectedQuantity,
    totalReworkQuantity = totalReworkQuantity,
    overallQualityYieldPercentage = overallQualityYieldPercentage,
    defectRatePercentage = defectRatePercentage,
    totalPackagedQuantity = totalPackagedQuantity,
    packagingBalanceVariance = packagingBalanceVariance,
    isReadyForFullRelease = isReadyForFullRelease,
    generatedAt = generatedAt
)

fun FinalQcPackagingReconciliationResult.toDto() = FinalQcPackagingReconciliationResponseDto(
    executionJobId = executionJobId,
    outputMatchedInspectionLot = outputMatchedInspectionLot,
    samplePlanConsistent = samplePlanConsistent,
    defectAccountingBalanced = defectAccountingBalanced,
    zeroUncontainedCriticalDefects = zeroUncontainedCriticalDefects,
    packagingQuantityMatchesAccepted = packagingQuantityMatchesAccepted,
    releaseCertificateHashValid = releaseCertificateHashValid,
    multiTenantIsolationVerified = multiTenantIsolationVerified,
    isFullyReconciled = isFullyReconciled,
    discrepancies = discrepancies,
    reconciledAt = reconciledAt
)

fun Module17Step08FinalQcPackagingHandoffContract.toDto() = Module17Step08FinalQcPackagingHandoffContractDto(
    contractVersion = contractVersion,
    executionJobId = executionJobId,
    orderId = orderId,
    finalInspectionStatus = finalInspectionStatus.name,
    totalGoodQuantityAccepted = totalGoodQuantityAccepted,
    totalDefectQuantity = totalDefectQuantity,
    qualityYieldPercentage = qualityYieldPercentage,
    defectRatePercentage = defectRatePercentage,
    totalPackagedCartons = totalPackagedCartons,
    totalPackagedQuantity = totalPackagedQuantity,
    packagingSlipBarcode = packagingSlipBarcode,
    releaseStatus = releaseStatus.name,
    releaseCertificateHash = releaseCertificateHash,
    isFullyReconciled = isFullyReconciled,
    inspectionSummary = inspectionSummary,
    defectContainmentSummary = defectContainmentSummary,
    exportedAt = exportedAt
)
