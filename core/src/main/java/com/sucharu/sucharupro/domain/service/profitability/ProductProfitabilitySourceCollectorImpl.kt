package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.ProductProfitabilityValidator
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Production implementation of ProductProfitabilitySourceCollector.
 *
 * Implements deterministic attribution, deduplication, fingerprint hashing, and source integrity tracking.
 */
class ProductProfitabilitySourceCollectorImpl : ProductProfitabilitySourceCollector {

    override suspend fun collectProductData(
        tenantId: String,
        projectId: String,
        productId: String,
        customRevenue: List<ProductRevenueAttribution>?,
        customCosts: List<ProductCostAttribution>?
    ): DomainResult<ProductSourceCollectionResult> {
        val validation = ProductProfitabilityValidator.validateCalculationRequest(tenantId, projectId, productId)
        if (validation is DomainResult.Error) {
            return DomainResult.Error(message = validation.message)
        }

        val allRevenue = mutableListOf<ProductRevenueAttribution>()
        val allCosts = mutableListOf<ProductCostAttribution>()
        val allFingerprints = mutableListOf<String>()
        val seenFingerprints = mutableSetOf<String>()
        val warnings = mutableListOf<String>()
        var duplicateCount = 0
        var conflictCount = 0

        // 1. Process Revenue Attributions
        if (!customRevenue.isNullOrEmpty()) {
            customRevenue.forEach { rev ->
                val errors = ProductProfitabilityValidator.validateRevenueAttribution(rev)
                if (errors.isNotEmpty()) {
                    warnings.addAll(errors)
                    conflictCount++
                } else {
                    val fp = if (rev.provenanceFingerprint.isNotBlank()) {
                        rev.provenanceFingerprint
                    } else {
                        ProductProfitabilityMathUtils.generateFingerprint(
                            sourceModule = rev.sourceModule,
                            sourceEntityType = rev.sourceEntityType,
                            sourceEntityId = rev.sourceEntityId,
                            sourceTransactionId = rev.sourceTransactionId,
                            productId = productId,
                            componentType = "REVENUE"
                        )
                    }

                    if (seenFingerprints.contains(fp)) {
                        duplicateCount++
                        warnings.add("Duplicate revenue attribution detected for source ${rev.sourceEntityId}")
                    } else {
                        seenFingerprints.add(fp)
                        allFingerprints.add(fp)
                        allRevenue.add(rev.copy(provenanceFingerprint = fp))
                    }
                }
            }
        }

        // 2. Process Cost Attributions
        if (!customCosts.isNullOrEmpty()) {
            customCosts.forEach { cost ->
                val errors = ProductProfitabilityValidator.validateCostAttribution(cost)
                if (errors.isNotEmpty()) {
                    warnings.addAll(errors)
                    conflictCount++
                } else {
                    val fp = if (cost.provenanceFingerprint.isNotBlank()) {
                        cost.provenanceFingerprint
                    } else {
                        ProductProfitabilityMathUtils.generateFingerprint(
                            sourceModule = cost.sourceModule,
                            sourceEntityType = cost.sourceEntityType,
                            sourceEntityId = cost.sourceEntityId,
                            sourceTransactionId = cost.sourceTransactionId,
                            productId = productId,
                            componentType = cost.componentType.name
                        )
                    }

                    if (seenFingerprints.contains(fp)) {
                        duplicateCount++
                        warnings.add("Duplicate cost attribution detected for source ${cost.sourceEntityId} and component ${cost.componentType.name}")
                    } else {
                        seenFingerprints.add(fp)
                        allFingerprints.add(fp)
                        allCosts.add(cost.copy(provenanceFingerprint = fp))
                    }
                }
            }
        }

        // 3. Compute Aggregates
        val totalRevenue = allRevenue.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.recognizedRevenue) }
            .let { ProductProfitabilityMathUtils.scaleMoney(it) }

        val totalCost = allCosts.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.attributedAmount) }
            .let { ProductProfitabilityMathUtils.scaleMoney(it) }

        val totalQuantity = allRevenue.fold(0) { acc, r -> acc + r.quantity }

        // 4. Build 12 Cost Components Breakdown
        val componentsByType = allCosts.groupBy { it.componentType }
        val componentItems = JobCostComponentType.values().map { type ->
            val matching = componentsByType[type] ?: emptyList()
            val compAmount = matching.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.attributedAmount) }
                .let { ProductProfitabilityMathUtils.scaleMoney(it) }
            val pct = if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
                compAmount.multiply(BigDecimal("100")).divide(totalCost, ProductProfitabilityMathUtils.SCALE, ProductProfitabilityMathUtils.ROUNDING_MODE)
            } else {
                BigDecimal.ZERO.setScale(ProductProfitabilityMathUtils.SCALE, ProductProfitabilityMathUtils.ROUNDING_MODE)
            }
            val unitAmount = if (totalQuantity > 0) {
                compAmount.divide(BigDecimal(totalQuantity).setScale(4, RoundingMode.HALF_UP), ProductProfitabilityMathUtils.SCALE, ProductProfitabilityMathUtils.ROUNDING_MODE)
            } else null

            ProductCostBreakdownItem(
                componentType = type,
                amount = compAmount,
                unitAmount = unitAmount,
                percentageOfTotalCost = pct,
                sourceCount = matching.size,
                allocationBasis = matching.firstOrNull()?.allocationBasis ?: ProductCostAllocationBasis.DIRECT,
                provenanceFingerprints = matching.map { it.provenanceFingerprint }
            )
        }

        // 5. Determine Integrity Status
        val integrityStatus = when {
            conflictCount > 0 -> ProductSourceIntegrityStatus.SOURCE_CONFLICT
            duplicateCount > 0 -> ProductSourceIntegrityStatus.DUPLICATE_DETECTED
            allRevenue.isEmpty() && allCosts.isEmpty() -> ProductSourceIntegrityStatus.SOURCE_INCOMPLETE
            else -> ProductSourceIntegrityStatus.VERIFIED
        }

        return DomainResult.Success(
            ProductSourceCollectionResult(
                revenueAttributions = allRevenue,
                costAttributions = allCosts,
                totalQuantity = totalQuantity,
                totalRecognizedRevenue = totalRevenue,
                totalActualCost = totalCost,
                components = componentItems,
                provenanceFingerprints = allFingerprints,
                sourceIntegrity = integrityStatus,
                warnings = warnings
            )
        )
    }
}
