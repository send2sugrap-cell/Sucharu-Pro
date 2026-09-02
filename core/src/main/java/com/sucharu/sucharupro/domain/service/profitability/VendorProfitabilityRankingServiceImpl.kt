package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Production implementation of Vendor Ranking, Concentration and Comparison Service.
 * Module 16 Step 05.
 */
class VendorProfitabilityRankingServiceImpl : VendorProfitabilityRankingService {

    override fun rankVendors(
        snapshots: List<VendorProfitabilitySnapshot>,
        criteria: VendorRankingCriteria,
        ascending: Boolean,
        limit: Int
    ): List<VendorRankingItem> {
        val comparator = when (criteria) {
            VendorRankingCriteria.TOTAL_COST -> compareBy<VendorProfitabilitySnapshot> { it.totalVendorCost }
            VendorRankingCriteria.COST_PER_JOB -> compareBy { it.costPerJob ?: BigDecimal.ZERO }
            VendorRankingCriteria.COST_PER_UNIT -> compareBy { it.costPerUnit ?: BigDecimal.ZERO }
            VendorRankingCriteria.COST_VARIANCE -> compareBy { it.costVariancePercentage ?: BigDecimal.ZERO }
            VendorRankingCriteria.EFFICIENCY_SCORE -> compareBy { it.efficiencyScore }
            VendorRankingCriteria.REWORK_COST -> compareBy { it.reworkCost }
            VendorRankingCriteria.REWORK_RATE -> compareBy { it.reworkRate ?: BigDecimal.ZERO }
            VendorRankingCriteria.RISK_SCORE -> compareBy { it.riskClassification.ordinal }
            VendorRankingCriteria.DEPENDENCY_SHARE -> compareBy { it.dependencySharePercentage ?: BigDecimal.ZERO }
        }.thenBy { it.vendorId }

        val sorted = if (ascending) {
            snapshots.sortedWith(comparator)
        } else {
            snapshots.sortedWith(comparator.reversed())
        }

        return sorted.take(limit).mapIndexed { index, snap ->
            val (metricVal, metricLbl) = when (criteria) {
                VendorRankingCriteria.TOTAL_COST -> Pair(snap.totalVendorCost, "Total Cost (${snap.currency})")
                VendorRankingCriteria.COST_PER_JOB -> Pair(snap.costPerJob ?: BigDecimal.ZERO, "Cost Per Job (${snap.currency})")
                VendorRankingCriteria.COST_PER_UNIT -> Pair(snap.costPerUnit ?: BigDecimal.ZERO, "Cost Per Unit (${snap.currency})")
                VendorRankingCriteria.COST_VARIANCE -> Pair(snap.costVariancePercentage ?: BigDecimal.ZERO, "Cost Variance %")
                VendorRankingCriteria.EFFICIENCY_SCORE -> Pair(snap.efficiencyScore, "Efficiency Score (0-100)")
                VendorRankingCriteria.REWORK_COST -> Pair(snap.reworkCost, "Rework Cost (${snap.currency})")
                VendorRankingCriteria.REWORK_RATE -> Pair(snap.reworkRate ?: BigDecimal.ZERO, "Rework Rate %")
                VendorRankingCriteria.RISK_SCORE -> Pair(BigDecimal.valueOf(snap.riskClassification.ordinal.toLong()), "Risk Tier")
                VendorRankingCriteria.DEPENDENCY_SHARE -> Pair(snap.dependencySharePercentage ?: BigDecimal.ZERO, "Dependency Share %")
            }

            VendorRankingItem(
                rank = index + 1,
                vendorId = snap.vendorId,
                vendorName = snap.vendorName,
                serviceCategory = snap.serviceCategory,
                metricValue = metricVal.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
                metricLabel = metricLbl,
                efficiencyScore = snap.efficiencyScore,
                riskClassification = snap.riskClassification
            )
        }
    }

    override fun analyzeConcentration(
        tenantId: String,
        projectId: String,
        snapshots: List<VendorProfitabilitySnapshot>,
        periodId: String?
    ): VendorConcentrationAnalysis {
        val totalSpend = snapshots.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.totalVendorCost) }
        val sortedBySpend = snapshots.sortedByDescending { it.totalVendorCost }

        val top1 = sortedBySpend.firstOrNull()
        val top1Spend = top1?.totalVendorCost ?: BigDecimal.ZERO
        val top1Share = if (totalSpend > BigDecimal.ZERO) {
            top1Spend.multiply(BigDecimal("100.0000")).divide(totalSpend, VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING)
        } else {
            BigDecimal.ZERO
        }

        val top5Spend = sortedBySpend.take(5).fold(BigDecimal.ZERO) { acc, s -> acc.add(s.totalVendorCost) }
        val top5Share = if (totalSpend > BigDecimal.ZERO) {
            top5Spend.multiply(BigDecimal("100.0000")).divide(totalSpend, VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING)
        } else {
            BigDecimal.ZERO
        }

        val top10Spend = sortedBySpend.take(10).fold(BigDecimal.ZERO) { acc, s -> acc.add(s.totalVendorCost) }
        val top10Share = if (totalSpend > BigDecimal.ZERO) {
            top10Spend.multiply(BigDecimal("100.0000")).divide(totalSpend, VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING)
        } else {
            BigDecimal.ZERO
        }

        val risk = when {
            top1Share >= BigDecimal("30.0000") || top5Share >= BigDecimal("70.0000") -> VendorDependencyClassification.CRITICAL_DEPENDENCY
            top1Share >= BigDecimal("20.0000") || top5Share >= BigDecimal("50.0000") -> VendorDependencyClassification.HIGH_DEPENDENCY
            top1Share >= BigDecimal("10.0000") || top5Share >= BigDecimal("30.0000") -> VendorDependencyClassification.MODERATE_DEPENDENCY
            else -> VendorDependencyClassification.LOW_DEPENDENCY
        }

        return VendorConcentrationAnalysis(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            totalVendorSpend = totalSpend.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            totalVendorCount = snapshots.size,
            top1Spend = top1Spend.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            top1SharePercentage = top1Share,
            top1VendorId = top1?.vendorId,
            top1VendorName = top1?.vendorName,
            top5Spend = top5Spend.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            top5SharePercentage = top5Share,
            top10Spend = top10Spend.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            top10SharePercentage = top10Share,
            concentrationRisk = risk
        )
    }

    override fun compareVendors(
        snapshots: List<VendorProfitabilitySnapshot>,
        vendorIds: List<String>
    ): List<VendorComparisonItem> {
        val targetMap = snapshots.associateBy { it.vendorId }
        return vendorIds.mapNotNull { vId ->
            targetMap[vId]?.let { snap ->
                VendorComparisonItem(
                    vendorId = snap.vendorId,
                    vendorName = snap.vendorName,
                    serviceCategory = snap.serviceCategory,
                    totalVendorCost = snap.totalVendorCost,
                    paidVendorCost = snap.paidVendorCost,
                    outstandingExposure = snap.outstandingExposure,
                    costPerJob = snap.costPerJob,
                    costPerUnit = snap.costPerUnit,
                    costVariancePercentage = snap.costVariancePercentage,
                    reworkCost = snap.reworkCost,
                    qualityFailureCount = snap.qualityFailureCount,
                    efficiencyScore = snap.efficiencyScore,
                    riskClassification = snap.riskClassification,
                    dependencyClassification = snap.dependencyClassification,
                    trendDirection = snap.trendDirection
                )
            }
        }
    }
}
