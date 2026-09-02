package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.ProfitabilityMathUtils
import com.sucharu.sucharupro.domain.model.profitability.ProfitabilityReconciliationEvent
import com.sucharu.sucharupro.domain.model.profitability.ProfitabilitySnapshot
import java.math.BigDecimal
import java.util.UUID

/**
 * Production implementation of ProfitabilityReconciliationService.
 */
class ProfitabilityReconciliationServiceImpl(
    private val financialHandoffAdapter: Module16FinancialHandoffAdapter,
    private val sourceRegistry: ProfitabilitySourceRegistry
) : ProfitabilityReconciliationService {

    override suspend fun reconcileSnapshot(
        snapshot: ProfitabilitySnapshot,
        actor: String
    ): DomainResult<ProfitabilityReconciliationEvent> {
        val discrepancies = mutableListOf<String>()

        // 1. Check duplicate sources
        val duplicates = sourceRegistry.detectDuplicateSources(
            snapshot.revenueProvenances,
            snapshot.costAttributions
        )
        discrepancies.addAll(duplicates)

        // 2. Sum revenue provenances
        val canonicalRevenueSum = snapshot.revenueProvenances.fold(BigDecimal.ZERO) { acc, r ->
            acc.add(r.recognizedAmount)
        }.let { ProfitabilityMathUtils.scaleMoney(it) }

        val snapshotRevenue = snapshot.metrics.revenue
        val revenueDiff = snapshotRevenue.subtract(canonicalRevenueSum).abs()

        if (revenueDiff.compareTo(BigDecimal("0.0001")) > 0) {
            discrepancies.add("Snapshot revenue ($snapshotRevenue) differs from provenance sum ($canonicalRevenueSum) by $revenueDiff")
        }

        // 3. Sum cost attributions
        val canonicalCostSum = snapshot.costAttributions.fold(BigDecimal.ZERO) { acc, c ->
            acc.add(c.attributableAmount)
        }.let { ProfitabilityMathUtils.scaleMoney(it) }

        val snapshotCost = snapshot.metrics.totalCost
        val costDiff = snapshotCost.subtract(canonicalCostSum).abs()

        if (costDiff.compareTo(BigDecimal("0.0001")) > 0) {
            discrepancies.add("Snapshot total cost ($snapshotCost) differs from attribution sum ($canonicalCostSum) by $costDiff")
        }

        // 4. Check breakdown sum consistency
        val breakdownSum = snapshot.costBreakdowns.fold(BigDecimal.ZERO) { acc, b ->
            acc.add(b.totalAmount)
        }.let { ProfitabilityMathUtils.scaleMoney(it) }

        if (breakdownSum.subtract(snapshotCost).abs().compareTo(BigDecimal("0.0001")) > 0 && snapshot.costBreakdowns.isNotEmpty()) {
            discrepancies.add("Cost breakdown sum ($breakdownSum) does not match total cost ($snapshotCost)")
        }

        // 5. Cross-check with period handoff if period is specified
        if (!snapshot.periodId.isNullOrBlank()) {
            when (val handoffRes = financialHandoffAdapter.getVerifiedFinancialHandoff(
                snapshot.tenantId,
                snapshot.projectId,
                snapshot.periodId
            )) {
                is DomainResult.Success -> {
                    if (!handoffRes.data.isLedgerBalanced) {
                        discrepancies.add("Module 15 General Ledger is unbalanced for period ${snapshot.periodId}")
                    }
                }
                is DomainResult.Error -> {
                    discrepancies.add("Module 15 Financial Handoff unavailable for reconciliation: ${handoffRes.message}")
                }
                DomainResult.Loading -> {
                    discrepancies.add("Module 15 Financial Handoff is currently loading for reconciliation")
                }
            }
        }

        val isReconciled = discrepancies.isEmpty()

        val event = ProfitabilityReconciliationEvent(
            id = "REC-${UUID.randomUUID()}",
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            snapshotId = snapshot.id,
            scope = snapshot.scope,
            targetEntityId = snapshot.targetEntityId,
            periodId = snapshot.periodId,
            isReconciled = isReconciled,
            canonicalRevenueTotal = canonicalRevenueSum,
            snapshotRevenueTotal = snapshotRevenue,
            revenueDifference = revenueDiff,
            canonicalCostTotal = canonicalCostSum,
            snapshotCostTotal = snapshotCost,
            costDifference = costDiff,
            discrepancies = discrepancies,
            checkedBy = actor
        )

        return DomainResult.Success(event)
    }

    override suspend fun verifyCanonicalAlignment(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<ProfitabilityReconciliationEvent> {
        val handoffRes = financialHandoffAdapter.getVerifiedFinancialHandoff(tenantId, projectId, periodId)
        return when (handoffRes) {
            is DomainResult.Success -> {
                val data = handoffRes.data
                val discrepancies = mutableListOf<String>()
                if (!data.isLedgerBalanced) {
                    discrepancies.add("General ledger debit/credit imbalance detected in period $periodId")
                }
                discrepancies.addAll(data.validationNotes)

                val event = ProfitabilityReconciliationEvent(
                    id = "REC-CANONICAL-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    snapshotId = "PERIOD-$periodId",
                    scope = com.sucharu.sucharupro.domain.model.profitability.ProfitabilityScope.PERIOD,
                    targetEntityId = periodId,
                    periodId = periodId,
                    isReconciled = discrepancies.isEmpty(),
                    canonicalRevenueTotal = data.contract.totalRecognizedRevenue,
                    snapshotRevenueTotal = data.contract.totalRecognizedRevenue,
                    revenueDifference = BigDecimal.ZERO.setScale(4, ProfitabilityMathUtils.ROUNDING_MODE),
                    canonicalCostTotal = data.contract.totalDirectExpenses.add(data.contract.totalRecognizedCostAllocations),
                    snapshotCostTotal = data.contract.totalDirectExpenses.add(data.contract.totalRecognizedCostAllocations),
                    costDifference = BigDecimal.ZERO.setScale(4, ProfitabilityMathUtils.ROUNDING_MODE),
                    discrepancies = discrepancies,
                    checkedBy = "SYSTEM"
                )
                DomainResult.Success(event)
            }
            is DomainResult.Error -> DomainResult.Error(message = handoffRes.message)
            DomainResult.Loading -> DomainResult.Error(message = "Canonical alignment check is loading")
        }
    }
}
