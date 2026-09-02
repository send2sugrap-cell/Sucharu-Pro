package com.sucharu.sucharupro.data.api.model.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

data class ProductProfitabilitySnapshotDto(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val productId: String,
    val sku: String? = null,
    val productName: String? = null,
    val editionId: String? = null,
    val versionId: String? = null,
    val periodId: String? = null,
    val customerId: String? = null,
    val totalQuantity: Int,
    val recognizedRevenue: BigDecimal,
    val totalActualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val unitEconomics: ProductUnitEconomicsDto,
    val costBreakdown: List<ProductCostBreakdownItemDto> = emptyList(),
    val profitabilityClassification: String,
    val varianceClassification: String,
    val baselineCost: BigDecimal? = null,
    val costVariance: BigDecimal? = null,
    val costVariancePercentage: BigDecimal? = null,
    val sourceIntegrityStatus: String,
    val isReconciled: Boolean,
    val reconciliationDiscrepancy: BigDecimal,
    val calculationVersion: String,
    val generatedAt: Long,
    val generatedBy: String,
    val integrityHash: String
)

data class ProductCostBreakdownItemDto(
    val componentType: String,
    val displayName: String,
    val amount: BigDecimal,
    val unitAmount: BigDecimal?,
    val percentageOfTotalCost: BigDecimal,
    val sourceCount: Int,
    val allocationBasis: String,
    val provenanceFingerprints: List<String> = emptyList()
)

data class ProductUnitEconomicsDto(
    val quantity: Int,
    val unitRevenue: BigDecimal?,
    val unitActualCost: BigDecimal?,
    val unitGrossProfit: BigDecimal?,
    val unitMaterialCost: BigDecimal?,
    val unitLabourCost: BigDecimal?,
    val unitMachineCost: BigDecimal?,
    val unitVendorCost: BigDecimal?,
    val unitReworkCost: BigDecimal?,
    val unitWastageCost: BigDecimal?,
    val unitFinishingCost: BigDecimal?,
    val unitPackagingCost: BigDecimal?,
    val unitTransportCost: BigDecimal?,
    val unitOtherDirectCost: BigDecimal?,
    val unitAllocatedIndirectCost: BigDecimal?,
    val unitMetricStatus: String
)

data class ProductRevenueAttributionDto(
    val revenueAttributionId: String,
    val productId: String,
    val sku: String? = null,
    val editionId: String? = null,
    val versionId: String? = null,
    val invoiceId: String? = null,
    val orderId: String? = null,
    val customerId: String? = null,
    val quantity: Int,
    val recognizedRevenue: BigDecimal,
    val attributionRatio: BigDecimal,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val attributionMethod: String,
    val provenanceFingerprint: String
)

data class ProductCostAttributionDto(
    val costAttributionId: String,
    val productId: String,
    val sku: String? = null,
    val editionId: String? = null,
    val versionId: String? = null,
    val jobId: String? = null,
    val componentType: String,
    val directness: String,
    val attributedAmount: BigDecimal,
    val allocationBasis: String,
    val numerator: BigDecimal?,
    val denominator: BigDecimal?,
    val allocationRatio: BigDecimal?,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val provenanceFingerprint: String
)

data class ProductProfitabilityReconciliationEventDto(
    val reconciliationId: String,
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val productId: String,
    val isReconciled: Boolean,
    val revenueReconciled: Boolean,
    val costReconciled: Boolean,
    val unitEconomicsReconciled: Boolean,
    val expectedRevenue: BigDecimal,
    val actualRevenue: BigDecimal,
    val expectedCost: BigDecimal,
    val actualCost: BigDecimal,
    val grossProfitDiscrepancy: BigDecimal,
    val discrepancies: List<String>,
    val checkedAt: Long,
    val checkedBy: String
)

data class ProductProfitabilityAuditEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val productId: String,
    val snapshotId: String? = null,
    val action: String,
    val actor: String,
    val actorRole: String,
    val outcome: String,
    val details: String,
    val correlationId: String? = null,
    val timestamp: Long
)

data class ProductProfitabilityComparisonItemDto(
    val productId: String,
    val sku: String?,
    val productName: String?,
    val quantity: Int,
    val recognizedRevenue: BigDecimal,
    val totalActualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val unitRevenue: BigDecimal?,
    val unitActualCost: BigDecimal?,
    val unitGrossProfit: BigDecimal?,
    val vendorOutsourceCost: BigDecimal,
    val reworkCost: BigDecimal,
    val wastageCost: BigDecimal,
    val classification: String,
    val varianceClassification: String
)

data class CalculateProductProfitabilityRequestDto(
    val sku: String? = null,
    val productName: String? = null,
    val editionId: String? = null,
    val versionId: String? = null,
    val periodId: String? = null,
    val customerId: String? = null,
    val customRevenue: List<ProductRevenueAttributionDto>? = null,
    val customCosts: List<ProductCostAttributionDto>? = null,
    val customBaselineCost: BigDecimal? = null,
    val idempotencyKey: String? = null
)

// Mapping Extension Functions
fun ProductProfitabilitySnapshot.toDto(): ProductProfitabilitySnapshotDto = ProductProfitabilitySnapshotDto(
    snapshotId = snapshotId,
    tenantId = tenantId,
    projectId = projectId,
    productId = productId,
    sku = sku,
    productName = productName,
    editionId = editionId,
    versionId = versionId,
    periodId = periodId,
    customerId = customerId,
    totalQuantity = totalQuantity,
    recognizedRevenue = recognizedRevenue,
    totalActualCost = totalActualCost,
    grossProfit = grossProfit,
    grossMarginPercentage = grossMarginPercentage,
    unitEconomics = unitEconomics.toDto(),
    costBreakdown = costBreakdown.map { it.toDto() },
    profitabilityClassification = profitabilityClassification.name,
    varianceClassification = varianceClassification.name,
    baselineCost = baselineCost,
    costVariance = costVariance,
    costVariancePercentage = costVariancePercentage,
    sourceIntegrityStatus = sourceIntegrityStatus.name,
    isReconciled = isReconciled,
    reconciliationDiscrepancy = reconciliationDiscrepancy,
    calculationVersion = calculationVersion,
    generatedAt = generatedAt,
    generatedBy = generatedBy,
    integrityHash = integrityHash
)

fun ProductCostBreakdownItem.toDto(): ProductCostBreakdownItemDto = ProductCostBreakdownItemDto(
    componentType = componentType.name,
    displayName = componentType.displayName,
    amount = amount,
    unitAmount = unitAmount,
    percentageOfTotalCost = percentageOfTotalCost,
    sourceCount = sourceCount,
    allocationBasis = allocationBasis.name,
    provenanceFingerprints = provenanceFingerprints
)

fun ProductUnitEconomics.toDto(): ProductUnitEconomicsDto = ProductUnitEconomicsDto(
    quantity = quantity,
    unitRevenue = unitRevenue,
    unitActualCost = unitActualCost,
    unitGrossProfit = unitGrossProfit,
    unitMaterialCost = unitMaterialCost,
    unitLabourCost = unitLabourCost,
    unitMachineCost = unitMachineCost,
    unitVendorCost = unitVendorCost,
    unitReworkCost = unitReworkCost,
    unitWastageCost = unitWastageCost,
    unitFinishingCost = unitFinishingCost,
    unitPackagingCost = unitPackagingCost,
    unitTransportCost = unitTransportCost,
    unitOtherDirectCost = unitOtherDirectCost,
    unitAllocatedIndirectCost = unitAllocatedIndirectCost,
    unitMetricStatus = unitMetricStatus
)

fun ProductRevenueAttribution.toDto(): ProductRevenueAttributionDto = ProductRevenueAttributionDto(
    revenueAttributionId = revenueAttributionId,
    productId = productId,
    sku = sku,
    editionId = editionId,
    versionId = versionId,
    invoiceId = invoiceId,
    orderId = orderId,
    customerId = customerId,
    quantity = quantity,
    recognizedRevenue = recognizedRevenue,
    attributionRatio = attributionRatio,
    sourceModule = sourceModule,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    sourceTransactionId = sourceTransactionId,
    attributionMethod = attributionMethod,
    provenanceFingerprint = provenanceFingerprint
)

fun ProductCostAttribution.toDto(): ProductCostAttributionDto = ProductCostAttributionDto(
    costAttributionId = costAttributionId,
    productId = productId,
    sku = sku,
    editionId = editionId,
    versionId = versionId,
    jobId = jobId,
    componentType = componentType.name,
    directness = directness.name,
    attributedAmount = attributedAmount,
    allocationBasis = allocationBasis.name,
    numerator = numerator,
    denominator = denominator,
    allocationRatio = allocationRatio,
    sourceModule = sourceModule,
    sourceEntityType = sourceEntityType,
    sourceEntityId = sourceEntityId,
    sourceTransactionId = sourceTransactionId,
    provenanceFingerprint = provenanceFingerprint
)

fun ProductProfitabilityReconciliationEvent.toDto(): ProductProfitabilityReconciliationEventDto = ProductProfitabilityReconciliationEventDto(
    reconciliationId = reconciliationId,
    snapshotId = snapshotId,
    tenantId = tenantId,
    projectId = projectId,
    productId = productId,
    isReconciled = isReconciled,
    revenueReconciled = revenueReconciled,
    costReconciled = costReconciled,
    unitEconomicsReconciled = unitEconomicsReconciled,
    expectedRevenue = expectedRevenue,
    actualRevenue = actualRevenue,
    expectedCost = expectedCost,
    actualCost = actualCost,
    grossProfitDiscrepancy = grossProfitDiscrepancy,
    discrepancies = discrepancies,
    checkedAt = checkedAt,
    checkedBy = checkedBy
)

fun ProductProfitabilityAuditEvent.toDto(): ProductProfitabilityAuditEventDto = ProductProfitabilityAuditEventDto(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    productId = productId,
    snapshotId = snapshotId,
    action = action,
    actor = actor,
    actorRole = actorRole,
    outcome = outcome,
    details = details,
    correlationId = correlationId,
    timestamp = timestamp
)

fun ProductProfitabilityComparisonItem.toDto(): ProductProfitabilityComparisonItemDto = ProductProfitabilityComparisonItemDto(
    productId = productId,
    sku = sku,
    productName = productName,
    quantity = quantity,
    recognizedRevenue = recognizedRevenue,
    totalActualCost = totalActualCost,
    grossProfit = grossProfit,
    grossMarginPercentage = grossMarginPercentage,
    unitRevenue = unitRevenue,
    unitActualCost = unitActualCost,
    unitGrossProfit = unitGrossProfit,
    vendorOutsourceCost = vendorOutsourceCost,
    reworkCost = reworkCost,
    wastageCost = wastageCost,
    classification = classification.name,
    varianceClassification = varianceClassification.name
)
