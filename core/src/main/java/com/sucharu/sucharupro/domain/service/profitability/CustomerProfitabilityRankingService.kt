package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Ranking criteria for Customer Profitability.
 */
enum class CustomerRankingCriteria {
    REVENUE,
    GROSS_PROFIT,
    GROSS_MARGIN,
    CONTRIBUTION,
    ORDER_COUNT,
    QUANTITY,
    AVERAGE_ORDER_VALUE
}

/**
 * Customer Ranking, Concentration, and Comparison Service Interface (Module 16 Step 04).
 */
interface CustomerProfitabilityRankingService {

    fun rankCustomers(
        snapshots: List<CustomerProfitabilitySnapshot>,
        criteria: CustomerRankingCriteria = CustomerRankingCriteria.GROSS_PROFIT
    ): List<CustomerProfitabilityRankingItem>

    fun analyzeConcentration(
        snapshots: List<CustomerProfitabilitySnapshot>
    ): CustomerConcentrationAnalysis

    fun compareCustomers(
        snapshots: List<CustomerProfitabilitySnapshot>,
        customerIds: List<String>
    ): List<CustomerProfitabilityComparisonItem>
}

/**
 * Production implementation of CustomerProfitabilityRankingService.
 */
class CustomerProfitabilityRankingServiceImpl : CustomerProfitabilityRankingService {

    override fun rankCustomers(
        snapshots: List<CustomerProfitabilitySnapshot>,
        criteria: CustomerRankingCriteria
    ): List<CustomerProfitabilityRankingItem> {
        val sorted = snapshots.sortedWith { a, b ->
            val comp = when (criteria) {
                CustomerRankingCriteria.REVENUE -> b.recognizedRevenue.compareTo(a.recognizedRevenue)
                CustomerRankingCriteria.GROSS_PROFIT -> b.grossProfit.compareTo(a.grossProfit)
                CustomerRankingCriteria.GROSS_MARGIN -> (b.grossMarginPercentage ?: BigDecimal("-9999")).compareTo(a.grossMarginPercentage ?: BigDecimal("-9999"))
                CustomerRankingCriteria.CONTRIBUTION -> b.contributionMetrics.contributionAmount.compareTo(a.contributionMetrics.contributionAmount)
                CustomerRankingCriteria.ORDER_COUNT -> b.operationalMetrics.orderCount.compareTo(a.operationalMetrics.orderCount)
                CustomerRankingCriteria.QUANTITY -> b.operationalMetrics.totalQuantitySold.compareTo(a.operationalMetrics.totalQuantitySold)
                CustomerRankingCriteria.AVERAGE_ORDER_VALUE -> (b.operationalMetrics.averageOrderValue ?: BigDecimal.ZERO).compareTo(a.operationalMetrics.averageOrderValue ?: BigDecimal.ZERO)
            }
            if (comp != 0) comp else a.customerId.compareTo(b.customerId) // Deterministic tie break
        }

        return sorted.mapIndexed { index, snap ->
            CustomerProfitabilityRankingItem(
                rank = index + 1,
                customerId = snap.customerId,
                customerName = snap.customerName,
                revenue = snap.recognizedRevenue,
                totalCost = snap.totalActualCost,
                grossProfit = snap.grossProfit,
                grossMarginPercentage = snap.grossMarginPercentage,
                contributionAmount = snap.contributionMetrics.contributionAmount,
                orderCount = snap.operationalMetrics.orderCount,
                quantity = snap.operationalMetrics.totalQuantitySold,
                averageOrderValue = snap.operationalMetrics.averageOrderValue,
                classification = snap.profitabilityClassification
            )
        }
    }

    override fun analyzeConcentration(
        snapshots: List<CustomerProfitabilitySnapshot>
    ): CustomerConcentrationAnalysis {
        val totalRev = snapshots.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.recognizedRevenue) }
            .let { CustomerProfitabilityMathUtils.scaleMoney(it) }
        val totalGp = snapshots.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.grossProfit) }
            .let { CustomerProfitabilityMathUtils.scaleMoney(it) }

        val rankedByRev = rankCustomers(snapshots, CustomerRankingCriteria.REVENUE)

        fun calcShare(items: List<CustomerProfitabilityRankingItem>, count: Int, isProfit: Boolean): BigDecimal {
            val topN = items.take(count)
            val sum = if (!isProfit) {
                topN.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.revenue) }
            } else {
                topN.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.grossProfit) }
            }
            val base = if (!isProfit) totalRev else totalGp
            return CustomerProfitabilityMathUtils.calculateSharePercentage(sum, base) ?: BigDecimal.ZERO.setScale(CustomerProfitabilityMathUtils.SCALE, CustomerProfitabilityMathUtils.ROUNDING_MODE)
        }

        val top1Rev = calcShare(rankedByRev, 1, false)
        val top5Rev = calcShare(rankedByRev, 5, false)
        val top10Rev = calcShare(rankedByRev, 10, false)

        val top1Profit = calcShare(rankedByRev, 1, true)
        val top5Profit = calcShare(rankedByRev, 5, true)
        val top10Profit = calcShare(rankedByRev, 10, true)

        val risk = CustomerProfitabilityMathUtils.assessConcentrationRisk(top1Rev, top5Rev)

        return CustomerConcentrationAnalysis(
            totalBusinessRevenue = totalRev,
            totalBusinessProfit = totalGp,
            top1RevenueSharePercentage = top1Rev,
            top5RevenueSharePercentage = top5Rev,
            top10RevenueSharePercentage = top10Rev,
            top1ProfitSharePercentage = top1Profit,
            top5ProfitSharePercentage = top5Profit,
            top10ProfitSharePercentage = top10Profit,
            concentrationRisk = risk,
            topCustomers = rankedByRev.take(10)
        )
    }

    override fun compareCustomers(
        snapshots: List<CustomerProfitabilitySnapshot>,
        customerIds: List<String>
    ): List<CustomerProfitabilityComparisonItem> {
        val matching = snapshots.filter { customerIds.contains(it.customerId) }
        return matching.map { s ->
            CustomerProfitabilityComparisonItem(
                customerId = s.customerId,
                customerName = s.customerName,
                revenue = s.recognizedRevenue,
                totalCost = s.totalActualCost,
                grossProfit = s.grossProfit,
                grossMarginPercentage = s.grossMarginPercentage,
                contributionAmount = s.contributionMetrics.contributionAmount,
                contributionMarginPercentage = s.contributionMetrics.contributionMarginPercentage,
                orderCount = s.operationalMetrics.orderCount,
                jobCount = s.operationalMetrics.jobCount,
                totalQuantity = s.operationalMetrics.totalQuantitySold,
                averageOrderValue = s.operationalMetrics.averageOrderValue,
                classification = s.profitabilityClassification,
                trend = s.trend
            )
        }
    }
}
