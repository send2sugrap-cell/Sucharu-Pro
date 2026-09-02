package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.util.UUID

/**
 * Implementation of Non-mutating invariant reconciliation for Period Profitability.
 * Module 16 Step 06.
 */
class PeriodProfitabilityReconciliationServiceImpl : PeriodProfitabilityReconciliationService {

    override fun reconcile(
        snapshot: PeriodProfitabilitySnapshot,
        sourceData: PeriodSourceCollectionResult,
        childSnapshots: List<PeriodProfitabilitySnapshot>
    ): PeriodProfitabilityReconciliationEvent {
        val assertions = mutableListOf<PeriodReconciliationAssertion>()
        val errorDetails = mutableListOf<String>()

        // 1. Revenue Identity: Σ period revenue sources = period revenue
        val sumRevenueSources = sourceData.revenueAttributions.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
            .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)
        val revenueDiff = (snapshot.revenue - sumRevenueSources).abs()
        val revenuePassed = revenueDiff == BigDecimal.ZERO.setScale(PeriodProfitabilityMathUtils.SCALE)
        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "Revenue Identity Assertion",
                isPassed = revenuePassed,
                expectedAmount = sumRevenueSources,
                actualAmount = snapshot.revenue,
                discrepancyAmount = revenueDiff,
                details = if (revenuePassed) "Passed" else "Snapshot revenue (${snapshot.revenue}) differs from sum of revenue sources ($sumRevenueSources)"
            )
        )
        if (!revenuePassed) errorDetails.add("Revenue discrepancy: BDT $revenueDiff")

        // 2. Cost Component Identity: Σ cost components = total actual cost
        val sumCostComponents = snapshot.costBreakdown.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
            .setScale(PeriodProfitabilityMathUtils.SCALE, PeriodProfitabilityMathUtils.ROUNDING)
        val costDiff = (snapshot.totalActualCost - sumCostComponents).abs()
        val costPassed = costDiff == BigDecimal.ZERO.setScale(PeriodProfitabilityMathUtils.SCALE)
        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "Cost Component Identity Assertion",
                isPassed = costPassed,
                expectedAmount = sumCostComponents,
                actualAmount = snapshot.totalActualCost,
                discrepancyAmount = costDiff,
                details = if (costPassed) "Passed" else "Snapshot total cost (${snapshot.totalActualCost}) differs from sum of 12 components ($sumCostComponents)"
            )
        )
        if (!costPassed) errorDetails.add("Cost components discrepancy: BDT $costDiff")

        // 3. Profit Identity: Revenue - Total Cost = Gross Profit
        val calculatedProfit = snapshot.revenue - snapshot.totalActualCost
        val profitDiff = (snapshot.grossProfit - calculatedProfit).abs()
        val profitPassed = profitDiff == BigDecimal.ZERO.setScale(PeriodProfitabilityMathUtils.SCALE)
        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "Gross Profit Identity Assertion",
                isPassed = profitPassed,
                expectedAmount = calculatedProfit,
                actualAmount = snapshot.grossProfit,
                discrepancyAmount = profitDiff,
                details = if (profitPassed) "Passed" else "Snapshot gross profit (${snapshot.grossProfit}) differs from Revenue - Cost ($calculatedProfit)"
            )
        )
        if (!profitPassed) errorDetails.add("Gross profit formula discrepancy: BDT $profitDiff")

        // 4. Margin Identity
        val calculatedMargin = PeriodProfitabilityMathUtils.calculateGrossMarginPercentage(snapshot.grossProfit, snapshot.revenue)
        val marginDiff = when {
            snapshot.grossMarginPercentage == null && calculatedMargin == null -> BigDecimal.ZERO
            snapshot.grossMarginPercentage != null && calculatedMargin != null -> (snapshot.grossMarginPercentage - calculatedMargin).abs()
            else -> BigDecimal.ONE
        }
        val marginPassed = marginDiff == BigDecimal.ZERO.setScale(PeriodProfitabilityMathUtils.SCALE)
        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "Gross Margin % Identity Assertion",
                isPassed = marginPassed,
                expectedAmount = calculatedMargin ?: BigDecimal.ZERO,
                actualAmount = snapshot.grossMarginPercentage ?: BigDecimal.ZERO,
                discrepancyAmount = marginDiff,
                details = if (marginPassed) "Passed" else "Snapshot gross margin (${snapshot.grossMarginPercentage}) differs from formula calculation ($calculatedMargin)"
            )
        )
        if (!marginPassed) errorDetails.add("Gross margin % formula discrepancy: $marginDiff%")

        // 5. Contribution Identity: Revenue - Direct Cost = Contribution Amount
        val calculatedContribution = PeriodProfitabilityMathUtils.calculateContributionAmount(snapshot.revenue, snapshot.directCost)
        val contribDiff = (snapshot.contributionAmount - calculatedContribution).abs()
        val contribPassed = contribDiff == BigDecimal.ZERO.setScale(PeriodProfitabilityMathUtils.SCALE)
        assertions.add(
            PeriodReconciliationAssertion(
                assertionName = "Contribution Amount Identity Assertion",
                isPassed = contribPassed,
                expectedAmount = calculatedContribution,
                actualAmount = snapshot.contributionAmount,
                discrepancyAmount = contribDiff,
                details = if (contribPassed) "Passed" else "Snapshot contribution (${snapshot.contributionAmount}) differs from Revenue - Direct Cost ($calculatedContribution)"
            )
        )
        if (!contribPassed) errorDetails.add("Contribution discrepancy: BDT $contribDiff")

        // 6. Child Aggregation Identity (if child snapshots provided)
        val childAggDiff = if (childSnapshots.isNotEmpty()) {
            val sumChildRevenue = childSnapshots.fold(BigDecimal.ZERO) { acc, s -> acc + s.revenue }
            (snapshot.revenue - sumChildRevenue).abs()
        } else BigDecimal.ZERO.setScale(PeriodProfitabilityMathUtils.SCALE)

        val crossDimDiff = BigDecimal.ZERO.setScale(PeriodProfitabilityMathUtils.SCALE)

        val isBalanced = assertions.all { it.isPassed }

        return PeriodProfitabilityReconciliationEvent(
            eventId = UUID.randomUUID().toString(),
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            periodId = snapshot.periodId,
            snapshotId = snapshot.snapshotId,
            isBalanced = isBalanced,
            revenueDifference = revenueDiff,
            costDifference = costDiff,
            profitDifference = profitDiff,
            marginDifference = marginDiff,
            contributionDifference = contribDiff,
            childAggregationDifference = childAggDiff,
            crossDimensionalDifference = crossDimDiff,
            assertions = assertions,
            errorDetails = errorDetails
        )
    }
}
