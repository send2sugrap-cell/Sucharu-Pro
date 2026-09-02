package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.util.UUID

/**
 * Non-mutating Reconciliation Engine Interface for Customer Profitability (Module 16 Step 04).
 */
interface CustomerProfitabilityReconciliationService {

    suspend fun reconcileCustomerSnapshot(
        snapshot: CustomerProfitabilitySnapshot,
        revenueSources: List<CustomerRevenueAttribution>,
        costSources: List<CustomerCostAttribution>,
        actor: String = "SYSTEM"
    ): DomainResult<CustomerProfitabilityReconciliationEvent>
}

/**
 * Production implementation of CustomerProfitabilityReconciliationService.
 */
class CustomerProfitabilityReconciliationServiceImpl : CustomerProfitabilityReconciliationService {

    override suspend fun reconcileCustomerSnapshot(
        snapshot: CustomerProfitabilitySnapshot,
        revenueSources: List<CustomerRevenueAttribution>,
        costSources: List<CustomerCostAttribution>,
        actor: String
    ): DomainResult<CustomerProfitabilityReconciliationEvent> {
        val discrepancies = mutableListOf<String>()

        // 1. Revenue Reconciliation
        val totalRevenueAttributed = revenueSources.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.recognizedRevenue) }
            .let { CustomerProfitabilityMathUtils.scaleMoney(it) }
        val revenueDiff = snapshot.recognizedRevenue.subtract(totalRevenueAttributed).abs()
        val isRevenueReconciled = revenueDiff.compareTo(BigDecimal("0.0001")) <= 0
        if (!isRevenueReconciled) {
            discrepancies.add("Revenue discrepancy: Snapshot=${snapshot.recognizedRevenue}, Attributed=${totalRevenueAttributed}, Diff=${revenueDiff}")
        }

        // 2. Cost Components Reconciliation
        val totalComponentCost = snapshot.costBreakdown.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amount) }
            .let { CustomerProfitabilityMathUtils.scaleMoney(it) }
        val costDiff = snapshot.totalActualCost.subtract(totalComponentCost).abs()
        val isCostReconciled = costDiff.compareTo(BigDecimal("0.0001")) <= 0
        if (!isCostReconciled) {
            discrepancies.add("Cost component mismatch: Snapshot=${snapshot.totalActualCost}, Component Sum=${totalComponentCost}, Diff=${costDiff}")
        }

        // 3. Profit Formula Reconciliation
        val expectedGp = CustomerProfitabilityMathUtils.calculateGrossProfit(snapshot.recognizedRevenue, snapshot.totalActualCost)
        val profitDiff = snapshot.grossProfit.subtract(expectedGp).abs()
        val isProfitReconciled = profitDiff.compareTo(BigDecimal("0.0001")) <= 0
        if (!isProfitReconciled) {
            discrepancies.add("Gross profit formula mismatch: Snapshot=${snapshot.grossProfit}, Expected=${expectedGp}")
        }

        // 4. Contribution Formula Reconciliation
        val expectedContrib = CustomerProfitabilityMathUtils.calculateContributionAmount(
            snapshot.recognizedRevenue,
            snapshot.contributionMetrics.attributableVariableCost
        )
        val contribDiff = snapshot.contributionMetrics.contributionAmount.subtract(expectedContrib).abs()
        val isContribReconciled = contribDiff.compareTo(BigDecimal("0.0001")) <= 0
        if (!isContribReconciled) {
            discrepancies.add("Contribution formula mismatch: Snapshot=${snapshot.contributionMetrics.contributionAmount}, Expected=${expectedContrib}")
        }

        val overallReconciled = isRevenueReconciled && isCostReconciled && isProfitReconciled && isContribReconciled && discrepancies.isEmpty()

        val event = CustomerProfitabilityReconciliationEvent(
            reconciliationId = "RECON-CUST-${UUID.randomUUID()}",
            snapshotId = snapshot.snapshotId,
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            customerId = snapshot.customerId,
            isReconciled = overallReconciled,
            revenueReconciled = isRevenueReconciled,
            costReconciled = isCostReconciled,
            profitReconciled = isProfitReconciled,
            contributionReconciled = isContribReconciled,
            expectedRevenue = totalRevenueAttributed,
            actualRevenue = snapshot.recognizedRevenue,
            expectedCost = totalComponentCost,
            actualCost = snapshot.totalActualCost,
            expectedGrossProfit = expectedGp,
            actualGrossProfit = snapshot.grossProfit,
            discrepancies = discrepancies,
            checkedAt = System.currentTimeMillis(),
            checkedBy = actor
        )

        return DomainResult.Success(event)
    }
}
