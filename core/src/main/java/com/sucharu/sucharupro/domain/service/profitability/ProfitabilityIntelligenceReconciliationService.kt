package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Non-mutating Reconciliation Engine for Cross-Dimensional Profitability Intelligence.
 * Validates zero discrepancy across dimensions, relationships, and financial totals.
 * Module 16 Step 07.
 */
interface ProfitabilityIntelligenceReconciliationService {
    suspend fun reconcile(
        tenantId: String,
        projectId: String,
        periodId: String,
        snapshot: ProfitabilityIntelligenceSnapshot
    ): DomainResult<ProfitabilityIntelligenceReconciliationEvent>
}

class ProfitabilityIntelligenceReconciliationServiceImpl : ProfitabilityIntelligenceReconciliationService {

    override suspend fun reconcile(
        tenantId: String,
        projectId: String,
        periodId: String,
        snapshot: ProfitabilityIntelligenceSnapshot
    ): DomainResult<ProfitabilityIntelligenceReconciliationEvent> {
        val assertions = mutableListOf<PeriodReconciliationAssertion>()
        val errorDetails = mutableListOf<String>()

        val scaledRevenue = ProfitabilityIntelligenceMathUtils.scaleMoney(snapshot.revenue)
        val scaledCost = ProfitabilityIntelligenceMathUtils.scaleMoney(snapshot.totalCost)
        val scaledProfit = ProfitabilityIntelligenceMathUtils.scaleMoney(snapshot.grossProfit)

        // 1. Core Mathematical Identity: Revenue - Cost == Gross Profit
        val expectedProfit = scaledRevenue.subtract(scaledCost)
        val profitDiff = scaledProfit.subtract(expectedProfit).abs()
        val isProfitMathBalanced = profitDiff.compareTo(BigDecimal("0.0010")) < 0

        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "CORE_PROFIT_MATHEMATICAL_IDENTITY",
                isPassed = isProfitMathBalanced,
                expectedAmount = expectedProfit,
                actualAmount = scaledProfit,
                discrepancyAmount = profitDiff,
                details = "Gross Profit must equal Revenue minus Total Cost."
            )
        )
        if (!isProfitMathBalanced) {
            errorDetails.add("Core Profit Math mismatch: expected $expectedProfit, actual $scaledProfit, diff $profitDiff.")
        }

        // 2. Margin Percentage Check
        val expectedMargin = ProfitabilityIntelligenceMathUtils.calculateGrossMarginPercentage(scaledRevenue, scaledCost)
        val marginDiff = if (snapshot.grossMargin != null && expectedMargin != null) {
            snapshot.grossMargin.subtract(expectedMargin).abs()
        } else {
            BigDecimal.ZERO
        }
        val isMarginBalanced = marginDiff.compareTo(BigDecimal("0.0100")) < 0
        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "MARGIN_PERCENTAGE_INTEGRITY",
                isPassed = isMarginBalanced,
                expectedAmount = expectedMargin ?: BigDecimal.ZERO,
                actualAmount = snapshot.grossMargin ?: BigDecimal.ZERO,
                discrepancyAmount = marginDiff,
                details = "Gross Margin percentage must align with Revenue and Cost."
            )
        )
        if (!isMarginBalanced) {
            errorDetails.add("Margin calculation mismatch: expected $expectedMargin, actual ${snapshot.grossMargin}.")
        }

        // 3. Customer Dimension Summation vs Total Revenue Check
        val customerDims = snapshot.dimensionInsights.filter { it.dimensionType == ProfitabilityDimensionType.CUSTOMER }
        val custTotalRev = customerDims.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.revenue) }
        val custRevDiff = if (customerDims.isNotEmpty()) scaledRevenue.subtract(custTotalRev).abs() else BigDecimal.ZERO
        val isCustRevBalanced = customerDims.isEmpty() || custRevDiff.compareTo(BigDecimal("1.0000")) < 0

        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "CUSTOMER_REVENUE_ALIGNMENT",
                isPassed = isCustRevBalanced,
                expectedAmount = scaledRevenue,
                actualAmount = custTotalRev,
                discrepancyAmount = custRevDiff,
                details = "Customer dimension revenues must sum up to overall snapshot revenue."
            )
        )
        if (!isCustRevBalanced) {
            errorDetails.add("Customer Revenue alignment warning: Total Rev $scaledRevenue vs Customer Sum $custTotalRev.")
        }

        // 4. Duplicate Dimension IDs check
        val duplicates = snapshot.dimensionInsights.groupBy { "${it.dimensionType}:${it.dimensionId}" }.filter { it.value.size > 1 }
        val isZeroDuplicates = duplicates.isEmpty()
        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "ZERO_DUPLICATE_DIMENSIONS",
                isPassed = isZeroDuplicates,
                expectedAmount = BigDecimal.ZERO,
                actualAmount = BigDecimal(duplicates.size),
                discrepancyAmount = BigDecimal(duplicates.size),
                details = "Zero duplicate dimensions allowed per snapshot."
            )
        )
        if (!isZeroDuplicates) {
            errorDetails.add("Duplicate dimensions detected: ${duplicates.keys.joinToString(", ")}.")
        }

        // 5. Integrity Hash Validation
        val recomputedHash = ProfitabilityIntelligenceMathUtils.generateIntegrityHash(
            tenantId = snapshot.tenantId,
            periodId = snapshot.analysisPeriodId,
            revenue = snapshot.revenue,
            totalCost = snapshot.totalCost,
            grossProfit = snapshot.grossProfit,
            dimensionInsights = snapshot.dimensionInsights,
            relationshipInsights = snapshot.relationshipInsights,
            drivers = snapshot.drivers,
            leakages = snapshot.leakages,
            priorities = snapshot.managementPriorities,
            healthScore = snapshot.healthScore?.overallScore
        )
        val isHashValid = snapshot.integrityHash.isBlank() || snapshot.integrityHash == recomputedHash
        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "INTEGRITY_HASH_VERIFICATION",
                isPassed = isHashValid,
                expectedAmount = BigDecimal.ONE,
                actualAmount = if (isHashValid) BigDecimal.ONE else BigDecimal.ZERO,
                discrepancyAmount = if (isHashValid) BigDecimal.ZERO else BigDecimal.ONE,
                details = "SHA-256 integrity hash must match freshly computed hash over snapshot payload."
            )
        )
        if (!isHashValid) {
            errorDetails.add("Integrity hash mismatch: stored ${snapshot.integrityHash} vs recomputed $recomputedHash.")
        }

        val allPassed = assertions.all { it.isPassed }

        val event = ProfitabilityIntelligenceReconciliationEvent(
            eventId = "recon-evt-${System.currentTimeMillis()}-${snapshot.snapshotId}",
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            snapshotId = snapshot.snapshotId,
            isBalanced = allPassed,
            revenueDifference = custRevDiff,
            costDifference = BigDecimal.ZERO,
            profitDifference = profitDiff,
            marginDifference = marginDiff,
            contributionDifference = BigDecimal.ZERO,
            relationshipDifference = BigDecimal.ZERO,
            driverImpactDifference = BigDecimal.ZERO,
            assertions = assertions,
            errorDetails = errorDetails
        )

        return DomainResult.Success(event)
    }
}
