package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.util.UUID

/**
 * Non-mutating Reconciliation Engine Interface for Product Profitability (Module 16 Step 03).
 */
interface ProductProfitabilityReconciliationService {

    suspend fun reconcileSnapshot(
        snapshot: ProductProfitabilitySnapshot,
        revenueSources: List<ProductRevenueAttribution>,
        costSources: List<ProductCostAttribution>,
        actor: String = "SYSTEM"
    ): DomainResult<ProductProfitabilityReconciliationEvent>
}

/**
 * Production implementation of ProductProfitabilityReconciliationService.
 */
class ProductProfitabilityReconciliationServiceImpl : ProductProfitabilityReconciliationService {

    override suspend fun reconcileSnapshot(
        snapshot: ProductProfitabilitySnapshot,
        revenueSources: List<ProductRevenueAttribution>,
        costSources: List<ProductCostAttribution>,
        actor: String
    ): DomainResult<ProductProfitabilityReconciliationEvent> {
        val discrepancies = mutableListOf<String>()

        // 1. Revenue Reconciliation
        val totalRevenueAttributed = revenueSources.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.recognizedRevenue) }
            .let { ProductProfitabilityMathUtils.scaleMoney(it) }
        val revenueDiff = snapshot.recognizedRevenue.subtract(totalRevenueAttributed).abs()
        val isRevenueReconciled = revenueDiff.compareTo(BigDecimal("0.0001")) <= 0
        if (!isRevenueReconciled) {
            discrepancies.add("Revenue discrepancy detected: Snapshot=${snapshot.recognizedRevenue}, Attributed=${totalRevenueAttributed}, Diff=${revenueDiff}")
        }

        // 2. Cost Components Reconciliation
        val totalComponentCost = snapshot.costBreakdown.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amount) }
            .let { ProductProfitabilityMathUtils.scaleMoney(it) }
        val costDiff = snapshot.totalActualCost.subtract(totalComponentCost).abs()
        val isCostReconciled = costDiff.compareTo(BigDecimal("0.0001")) <= 0
        if (!isCostReconciled) {
            discrepancies.add("Cost component breakdown mismatch: Snapshot Total=${snapshot.totalActualCost}, Component Sum=${totalComponentCost}, Diff=${costDiff}")
        }

        // 3. Profit Mathematical Reconciliation
        val expectedGrossProfit = ProductProfitabilityMathUtils.calculateGrossProfit(snapshot.recognizedRevenue, snapshot.totalActualCost)
        val profitDiff = snapshot.grossProfit.subtract(expectedGrossProfit).abs()
        if (profitDiff.compareTo(BigDecimal("0.0001")) > 0) {
            discrepancies.add("Gross profit formula mismatch: Snapshot Profit=${snapshot.grossProfit}, Expected=${expectedGrossProfit}")
        }

        // 4. Unit Economics Consistency
        var isUnitEconomicsReconciled = true
        if (snapshot.totalQuantity > 0 && snapshot.unitEconomics.unitActualCost != null) {
            val impliedTotalCost = snapshot.unitEconomics.unitActualCost
                .multiply(BigDecimal(snapshot.totalQuantity))
                .let { ProductProfitabilityMathUtils.scaleMoney(it) }
            val unitCostDiff = snapshot.totalActualCost.subtract(impliedTotalCost).abs()
            // Tolerance up to 0.05 for rounding multiplication across high quantities
            if (unitCostDiff.compareTo(BigDecimal("0.0500")) > 0) {
                isUnitEconomicsReconciled = false
                discrepancies.add("Unit economics implied cost discrepancy: Implied=${impliedTotalCost}, Actual Total=${snapshot.totalActualCost}, Diff=${unitCostDiff}")
            }
        }

        val overallReconciled = isRevenueReconciled && isCostReconciled && isUnitEconomicsReconciled && discrepancies.isEmpty()

        val event = ProductProfitabilityReconciliationEvent(
            reconciliationId = "RECON-${UUID.randomUUID()}",
            snapshotId = snapshot.snapshotId,
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            productId = snapshot.productId,
            isReconciled = overallReconciled,
            revenueReconciled = isRevenueReconciled,
            costReconciled = isCostReconciled,
            unitEconomicsReconciled = isUnitEconomicsReconciled,
            expectedRevenue = totalRevenueAttributed,
            actualRevenue = snapshot.recognizedRevenue,
            expectedCost = totalComponentCost,
            actualCost = snapshot.totalActualCost,
            grossProfitDiscrepancy = profitDiff,
            discrepancies = discrepancies,
            checkedAt = System.currentTimeMillis(),
            checkedBy = actor
        )

        return DomainResult.Success(event)
    }
}
