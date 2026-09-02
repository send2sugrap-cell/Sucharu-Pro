package com.sucharu.sucharupro.data.api.model.productionplanning

import com.sucharu.sucharupro.domain.model.productionplanning.*
import java.math.BigDecimal

data class ProductionJobSpecificationDto(
    val specId: String,
    val jobTitle: String,
    val productType: String,
    val orderedQuantity: Long,
    val plannedQuantity: Long,
    val finishedWidthMm: String,
    val finishedHeightMm: String,
    val substrateType: String,
    val substrateGsm: Int,
    val substrateBrand: String? = null,
    val parentSheetWidthMm: String,
    val parentSheetHeightMm: String,
    val pressSheetWidthMm: String,
    val pressSheetHeightMm: String,
    val printingMethod: String,
    val colorsFront: Int,
    val colorsBack: Int,
    val coatingFront: String = "NONE",
    val coatingBack: String = "NONE",
    val impositionUps: Int,
    val lamination: String = "NONE",
    val bindingMethod: String = "NONE",
    val foldingType: String = "NONE",
    val cuttingRequired: Boolean = true,
    val dieCuttingRequired: Boolean = false,
    val packagingMethod: String = "CARTON_BOX",
    val artworkUrl: String? = null,
    val specialInstructions: String? = null,
    val specFingerprint: String
)

data class ProductionPlanningRequirementDto(
    val requirementId: String,
    val planningId: String,
    val category: String,
    val itemCode: String,
    val description: String,
    val requiredQuantity: String,
    val makeReadyQuantity: String,
    val wasteQuantity: String,
    val totalPlannedQuantity: String,
    val unitOfMeasure: String,
    val estimatedAvailable: Boolean = true,
    val notes: String? = null
)

data class ProductionPlanningOperationDto(
    val operationId: String,
    val planningId: String,
    val sequenceNumber: Int,
    val stageType: String,
    val operationCode: String,
    val operationName: String,
    val targetWorkCenter: String,
    val estimatedSetupMinutes: Int,
    val estimatedRunMinutes: Int,
    val isMandatory: Boolean,
    val isQcCheckpoint: Boolean,
    val dependencies: List<String>,
    val notes: String? = null
)

data class MachineCompatibilityResultDto(
    val machineId: String,
    val machineName: String,
    val status: String,
    val formatMatch: Boolean,
    val substrateMatch: Boolean,
    val colorMatch: Boolean,
    val notes: String? = null
)

data class PlanningDiagnosticDto(
    val diagnosticId: String,
    val planningId: String,
    val code: String,
    val severity: String,
    val category: String,
    val message: String,
    val isBlocking: Boolean,
    val recommendedAction: String? = null
)

data class ManufacturingReadinessEvaluationDto(
    val overallScore: String,
    val isManufacturingReady: Boolean,
    val feasibilityStatus: String,
    val commercialReadinessScore: String,
    val specificationReadinessScore: String,
    val materialReadinessScore: String,
    val machineReadinessScore: String,
    val scheduleReadinessScore: String,
    val blockingIssuesCount: Int,
    val warningsCount: Int,
    val diagnostics: List<PlanningDiagnosticDto>
)

data class ProductionPlanningSnapshotDto(
    val planningId: String,
    val tenantId: String,
    val projectId: String,
    val orderId: String,
    val orderNumber: String,
    val orderItemId: String,
    val commercialCommitmentId: String?,
    val quotationId: String?,
    val quotationVersionNumber: Int?,
    val customerId: String,
    val status: String,
    val version: Int,
    val isCurrent: Boolean,
    val readinessScore: String,
    val feasibilityStatus: String,
    val specification: ProductionJobSpecificationDto,
    val requirements: List<ProductionPlanningRequirementDto>,
    val operations: List<ProductionPlanningOperationDto>,
    val diagnostics: List<PlanningDiagnosticDto>,
    val machineCompatibility: List<MachineCompatibilityResultDto>,
    val orderRequestedDate: Long?,
    val estimatedCompletionDate: Long?,
    val planningFingerprint: String,
    val integrityHash: String,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String
)

data class ProductionPlanningEventDto(
    val eventId: String,
    val planningId: String,
    val tenantId: String,
    val eventType: String,
    val fromStatus: String?,
    val toStatus: String?,
    val eventPayload: String?,
    val performedBy: String,
    val performedAt: Long
)

data class ProductionPlanningReconciliationDto(
    val planningId: String,
    val orderId: String,
    val quotationId: String?,
    val commercialCommitmentId: String?,
    val isFullyReconciled: Boolean,
    val customerMatch: Boolean,
    val quantityMatch: Boolean,
    val specFingerprintMatch: Boolean,
    val pricingBoundaryPreserved: Boolean,
    val tenantIsolationVerified: Boolean,
    val discrepancies: List<String>,
    val verifiedAt: Long
)

data class CreateProductionPlanningRequestDto(
    val orderItemId: String? = null,
    val idempotencyKey: String? = null
)

data class SupersedeProductionPlanningRequestDto(
    val reason: String
)

data class Module17Step04ProductionPlanningHandoffDto(
    val contractVersion: String,
    val planningId: String,
    val tenantId: String,
    val projectId: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val planningStatus: String,
    val readinessScore: String,
    val isManufacturingReady: Boolean,
    val feasibilityStatus: String,
    val jobTitle: String,
    val orderedQuantity: Long,
    val plannedQuantity: Long,
    val primaryWorkCenter: String,
    val totalEstimatedRunMinutes: Int,
    val operationsCount: Int,
    val blockingIssues: List<String>,
    val warnings: List<String>,
    val reconciliationStatus: String,
    val integrityHash: String,
    val generatedAt: Long
)

// ============================================================
// DTO EXTENSION MAPPINGS
// ============================================================

fun ProductionJobSpecification.toDto() = ProductionJobSpecificationDto(
    specId = specId,
    jobTitle = jobTitle,
    productType = productType,
    orderedQuantity = orderedQuantity,
    plannedQuantity = plannedQuantity,
    finishedWidthMm = finishedWidthMm.toPlainString(),
    finishedHeightMm = finishedHeightMm.toPlainString(),
    substrateType = substrateType,
    substrateGsm = substrateGsm,
    substrateBrand = substrateBrand,
    parentSheetWidthMm = parentSheetWidthMm.toPlainString(),
    parentSheetHeightMm = parentSheetHeightMm.toPlainString(),
    pressSheetWidthMm = pressSheetWidthMm.toPlainString(),
    pressSheetHeightMm = pressSheetHeightMm.toPlainString(),
    printingMethod = printingMethod,
    colorsFront = colorsFront,
    colorsBack = colorsBack,
    coatingFront = coatingFront,
    coatingBack = coatingBack,
    impositionUps = impositionUps,
    lamination = lamination,
    bindingMethod = bindingMethod,
    foldingType = foldingType,
    cuttingRequired = cuttingRequired,
    dieCuttingRequired = dieCuttingRequired,
    packagingMethod = packagingMethod,
    artworkUrl = artworkUrl,
    specialInstructions = specialInstructions,
    specFingerprint = specFingerprint
)

fun ProductionPlanningRequirement.toDto() = ProductionPlanningRequirementDto(
    requirementId = requirementId,
    planningId = planningId,
    category = category,
    itemCode = itemCode,
    description = description,
    requiredQuantity = requiredQuantity.toPlainString(),
    makeReadyQuantity = makeReadyQuantity.toPlainString(),
    wasteQuantity = wasteQuantity.toPlainString(),
    totalPlannedQuantity = totalPlannedQuantity.toPlainString(),
    unitOfMeasure = unitOfMeasure,
    estimatedAvailable = estimatedAvailable,
    notes = notes
)

fun ProductionPlanningOperation.toDto() = ProductionPlanningOperationDto(
    operationId = operationId,
    planningId = planningId,
    sequenceNumber = sequenceNumber,
    stageType = stageType.name,
    operationCode = operationCode,
    operationName = operationName,
    targetWorkCenter = targetWorkCenter,
    estimatedSetupMinutes = estimatedSetupMinutes,
    estimatedRunMinutes = estimatedRunMinutes,
    isMandatory = isMandatory,
    isQcCheckpoint = isQcCheckpoint,
    dependencies = dependencies,
    notes = notes
)

fun MachineCompatibilityResult.toDto() = MachineCompatibilityResultDto(
    machineId = machineId,
    machineName = machineName,
    status = status.name,
    formatMatch = formatMatch,
    substrateMatch = substrateMatch,
    colorMatch = colorMatch,
    notes = notes
)

fun PlanningDiagnostic.toDto() = PlanningDiagnosticDto(
    diagnosticId = diagnosticId,
    planningId = planningId,
    code = code,
    severity = severity.name,
    category = category,
    message = message,
    isBlocking = isBlocking,
    recommendedAction = recommendedAction
)

fun ManufacturingReadinessEvaluation.toDto() = ManufacturingReadinessEvaluationDto(
    overallScore = overallScore.toPlainString(),
    isManufacturingReady = isManufacturingReady,
    feasibilityStatus = feasibilityStatus.name,
    commercialReadinessScore = commercialReadinessScore.toPlainString(),
    specificationReadinessScore = specificationReadinessScore.toPlainString(),
    materialReadinessScore = materialReadinessScore.toPlainString(),
    machineReadinessScore = machineReadinessScore.toPlainString(),
    scheduleReadinessScore = scheduleReadinessScore.toPlainString(),
    blockingIssuesCount = blockingIssuesCount,
    warningsCount = warningsCount,
    diagnostics = diagnostics.map { it.toDto() }
)

fun ProductionPlanningSnapshot.toDto() = ProductionPlanningSnapshotDto(
    planningId = planningId,
    tenantId = tenantId,
    projectId = projectId,
    orderId = orderId,
    orderNumber = orderNumber,
    orderItemId = orderItemId,
    commercialCommitmentId = commercialCommitmentId,
    quotationId = quotationId,
    quotationVersionNumber = quotationVersionNumber,
    customerId = customerId,
    status = status.name,
    version = version,
    isCurrent = isCurrent,
    readinessScore = readinessScore.toPlainString(),
    feasibilityStatus = feasibilityStatus.name,
    specification = specification.toDto(),
    requirements = requirements.map { it.toDto() },
    operations = operations.map { it.toDto() },
    diagnostics = diagnostics.map { it.toDto() },
    machineCompatibility = machineCompatibility.map { it.toDto() },
    orderRequestedDate = orderRequestedDate,
    estimatedCompletionDate = estimatedCompletionDate,
    planningFingerprint = planningFingerprint,
    integrityHash = integrityHash,
    createdAt = createdAt,
    updatedAt = updatedAt,
    createdBy = createdBy
)

fun ProductionPlanningEvent.toDto() = ProductionPlanningEventDto(
    eventId = eventId,
    planningId = planningId,
    tenantId = tenantId,
    eventType = eventType.name,
    fromStatus = fromStatus?.name,
    toStatus = toStatus?.name,
    eventPayload = eventPayload,
    performedBy = performedBy,
    performedAt = performedAt
)

fun ProductionPlanningReconciliationResult.toDto() = ProductionPlanningReconciliationDto(
    planningId = planningId,
    orderId = orderId,
    quotationId = quotationId,
    commercialCommitmentId = commercialCommitmentId,
    isFullyReconciled = isFullyReconciled,
    customerMatch = customerMatch,
    quantityMatch = quantityMatch,
    specFingerprintMatch = specFingerprintMatch,
    pricingBoundaryPreserved = pricingBoundaryPreserved,
    tenantIsolationVerified = tenantIsolationVerified,
    discrepancies = discrepancies,
    verifiedAt = verifiedAt
)

fun Module17Step04ProductionPlanningHandoffContract.toDto() = Module17Step04ProductionPlanningHandoffDto(
    contractVersion = contractVersion,
    planningId = planningId,
    tenantId = tenantId,
    projectId = projectId,
    orderId = orderId,
    orderNumber = orderNumber,
    customerId = customerId,
    planningStatus = planningStatus,
    readinessScore = readinessScore.toPlainString(),
    isManufacturingReady = isManufacturingReady,
    feasibilityStatus = feasibilityStatus,
    jobTitle = jobTitle,
    orderedQuantity = orderedQuantity,
    plannedQuantity = plannedQuantity,
    primaryWorkCenter = primaryWorkCenter,
    totalEstimatedRunMinutes = totalEstimatedRunMinutes,
    operationsCount = operationsCount,
    blockingIssues = blockingIssues,
    warnings = warnings,
    reconciliationStatus = reconciliationStatus,
    integrityHash = integrityHash,
    generatedAt = generatedAt
)
