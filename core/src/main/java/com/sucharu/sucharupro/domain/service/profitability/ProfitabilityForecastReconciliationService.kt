package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Non-Mutating Reconciliation & Forecast-vs-Actual Comparison Engine.
 * Module 16 Step 08.
 */
interface ProfitabilityForecastReconciliationService {
    suspend fun reconcileForecast(
        snapshot: ProfitabilityForecastSnapshot
    ): DomainResult<ProfitabilityForecastReconciliationEvent>

    suspend fun compareWithActual(
        snapshot: ProfitabilityForecastSnapshot,
        actualRevenue: BigDecimal,
        actualCost: BigDecimal,
        actualUnits: Long,
        actualPeriodId: String
    ): DomainResult<ForecastActualComparison>
}

class ProfitabilityForecastReconciliationServiceImpl : ProfitabilityForecastReconciliationService {

    override suspend fun reconcileForecast(
        snapshot: ProfitabilityForecastSnapshot
    ): DomainResult<ProfitabilityForecastReconciliationEvent> {
        val assertions = mutableListOf<ForecastReconciliationAssertion>()
        val errorDetails = mutableListOf<String>()

        val rev = ProfitabilityForecastMathUtils.scaleMoney(snapshot.projectedRevenue)
        val cost = ProfitabilityForecastMathUtils.scaleMoney(snapshot.projectedTotalCost)
        val profit = ProfitabilityForecastMathUtils.scaleMoney(snapshot.projectedGrossProfit)

        // 1. Core Identity: Revenue - Cost == Gross Profit
        val expectedProfit = rev.subtract(cost)
        val profitDiff = profit.subtract(expectedProfit).abs()
        val isProfitBalanced = profitDiff.compareTo(BigDecimal("0.0010")) < 0
        assertions.add(
            ForecastReconciliationAssertion(
                assertionName = "CORE_PROFIT_MATHEMATICAL_IDENTITY",
                isPassed = isProfitBalanced,
                expectedAmount = expectedProfit,
                actualAmount = profit,
                discrepancyAmount = profitDiff,
                details = "Projected Gross Profit must equal Projected Revenue minus Projected Total Cost."
            )
        )
        if (!isProfitBalanced) errorDetails.add("Profit identity mismatch: expected $expectedProfit, actual $profit.")

        // 2. Margin Percentage Check
        val expectedMargin = ProfitabilityForecastMathUtils.calculateGrossMarginPercentage(rev, cost)
        val marginDiff = if (snapshot.projectedGrossMarginPercentage != null && expectedMargin != null) {
            snapshot.projectedGrossMarginPercentage.subtract(expectedMargin).abs()
        } else BigDecimal.ZERO
        val isMarginBalanced = marginDiff.compareTo(BigDecimal("0.0100")) < 0
        assertions.add(
            ForecastReconciliationAssertion(
                assertionName = "MARGIN_PERCENTAGE_INTEGRITY",
                isPassed = isMarginBalanced,
                expectedAmount = expectedMargin ?: BigDecimal.ZERO,
                actualAmount = snapshot.projectedGrossMarginPercentage ?: BigDecimal.ZERO,
                discrepancyAmount = marginDiff,
                details = "Projected Gross Margin % must align with revenue and cost."
            )
        )
        if (!isMarginBalanced) errorDetails.add("Margin percentage mismatch: expected $expectedMargin, actual ${snapshot.projectedGrossMarginPercentage}.")

        // 3. Components Summation Check
        val compSum = snapshot.components.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.projectedAmount) }
        val compDiff = if (snapshot.components.isNotEmpty()) cost.subtract(compSum).abs() else BigDecimal.ZERO
        val isCompBalanced = snapshot.components.isEmpty() || compDiff.compareTo(BigDecimal("0.0100")) < 0
        assertions.add(
            ForecastReconciliationAssertion(
                assertionName = "COST_COMPONENTS_SUMMATION",
                isPassed = isCompBalanced,
                expectedAmount = cost,
                actualAmount = compSum,
                discrepancyAmount = compDiff,
                details = "Sum of 12 cost components must equal projected total cost."
            )
        )
        if (!isCompBalanced) errorDetails.add("Component sum mismatch: total cost $cost vs component sum $compSum.")

        // 4. SHA-256 Integrity Hash Check
        val recomputedHash = ProfitabilityForecastMathUtils.generateSnapshotIntegrityHash(
            forecastId = snapshot.forecastId,
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            targetScope = snapshot.targetScope,
            targetEntityId = snapshot.targetEntityId,
            forecastMethod = snapshot.forecastMethod,
            scenarioType = snapshot.scenarioType,
            projectedRevenue = snapshot.projectedRevenue,
            projectedTotalCost = snapshot.projectedTotalCost,
            projectedGrossProfit = snapshot.projectedGrossProfit,
            components = snapshot.components,
            assumptions = snapshot.assumptions,
            provenanceRecords = snapshot.provenanceRecords
        )
        val isHashValid = snapshot.integrityHash.isBlank() || snapshot.integrityHash == recomputedHash
        assertions.add(
            ForecastReconciliationAssertion(
                assertionName = "INTEGRITY_HASH_VERIFICATION",
                isPassed = isHashValid,
                expectedAmount = BigDecimal.ONE,
                actualAmount = if (isHashValid) BigDecimal.ONE else BigDecimal.ZERO,
                discrepancyAmount = if (isHashValid) BigDecimal.ZERO else BigDecimal.ONE,
                details = "SHA-256 integrity hash must match freshly computed hash over snapshot payload."
            )
        )
        if (!isHashValid) errorDetails.add("Integrity hash mismatch: stored ${snapshot.integrityHash} vs recomputed $recomputedHash.")

        val allPassed = assertions.all { it.isPassed }

        val event = ProfitabilityForecastReconciliationEvent(
            eventId = "recon-fc-${System.currentTimeMillis()}-${snapshot.forecastId}",
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            forecastId = snapshot.forecastId,
            isBalanced = allPassed,
            revenueDifference = BigDecimal.ZERO,
            costDifference = compDiff,
            profitDifference = profitDiff,
            marginDifference = marginDiff,
            componentDifference = compDiff,
            scenarioDifference = BigDecimal.ZERO,
            assertions = assertions,
            errorDetails = errorDetails
        )

        return DomainResult.Success(event)
    }

    override suspend fun compareWithActual(
        snapshot: ProfitabilityForecastSnapshot,
        actualRevenue: BigDecimal,
        actualCost: BigDecimal,
        actualUnits: Long,
        actualPeriodId: String
    ): DomainResult<ForecastActualComparison> {
        val fcRev = snapshot.projectedRevenue
        val fcCost = snapshot.projectedTotalCost
        val fcProfit = snapshot.projectedGrossProfit
        val fcMargin = snapshot.projectedGrossMarginPercentage

        val actRev = ProfitabilityForecastMathUtils.scaleMoney(actualRevenue)
        val actCost = ProfitabilityForecastMathUtils.scaleMoney(actualCost)
        val actProfit = ProfitabilityForecastMathUtils.calculateGrossProfit(actRev, actCost)
        val actMargin = ProfitabilityForecastMathUtils.calculateGrossMarginPercentage(actRev, actCost)

        val revVar = actRev.subtract(fcRev)
        val revVarPct = ProfitabilityForecastMathUtils.calculatePercentageChange(fcRev, actRev)

        val costVar = actCost.subtract(fcCost)
        val costVarPct = ProfitabilityForecastMathUtils.calculatePercentageChange(fcCost, actCost)

        val profitVar = actProfit.subtract(fcProfit)
        val profitVarPct = ProfitabilityForecastMathUtils.calculatePercentageChange(fcProfit, actProfit)

        val marginVarPct = if (actMargin != null && fcMargin != null) actMargin.subtract(fcMargin) else null
        val unitsVar = actualUnits - snapshot.projectedUnits

        // Directional Accuracy: True if both grew or both shrank vs baseline
        val baseRev = snapshot.baselineRevenue ?: fcRev
        val isDirectionallyAccurate = (fcRev.compareTo(baseRev) >= 0 && actRev.compareTo(baseRev) >= 0) ||
                (fcRev.compareTo(baseRev) < 0 && actRev.compareTo(baseRev) < 0)

        // Mean Absolute Percentage Error across revenue and cost
        val revError = revVarPct?.abs() ?: BigDecimal.ZERO
        val costError = costVarPct?.abs() ?: BigDecimal.ZERO
        val mape = revError.add(costError).divide(BigDecimal("2.0000"), 4, RoundingMode.HALF_UP)

        val comparison = ForecastActualComparison(
            comparisonId = "act-comp-${System.currentTimeMillis()}-${snapshot.forecastId}",
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            forecastId = snapshot.forecastId,
            actualPeriodId = actualPeriodId,
            targetScope = snapshot.targetScope,
            targetEntityId = snapshot.targetEntityId,
            targetEntityLabel = snapshot.targetEntityLabel,
            forecastRevenue = fcRev,
            actualRevenue = actRev,
            revenueVariance = revVar,
            revenueVariancePercentage = revVarPct,
            forecastCost = fcCost,
            actualCost = actCost,
            costVariance = costVar,
            costVariancePercentage = costVarPct,
            forecastGrossProfit = fcProfit,
            actualGrossProfit = actProfit,
            profitVariance = profitVar,
            profitVariancePercentage = profitVarPct,
            forecastMarginPercentage = fcMargin,
            actualMarginPercentage = actMargin,
            marginVariancePercentage = marginVarPct,
            forecastUnits = snapshot.projectedUnits,
            actualUnits = actualUnits,
            unitsVariance = unitsVar,
            isDirectionallyAccurate = isDirectionallyAccurate,
            meanAbsolutePercentageError = mape,
            evaluationNotes = if (isDirectionallyAccurate) "Forecast tracked actual direction with MAPE of $mape%." else "Directional divergence observed."
        )

        return DomainResult.Success(comparison)
    }
}
