package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

interface ExecutiveRankingEngine {
    fun computeRankings(payload: ProfitabilityEvaluationPayload): ExecutiveRankingsPayload
    fun computeConcentration(payload: ProfitabilityEvaluationPayload): ExecutiveConcentrationSummary
}

class ExecutiveRankingEngineImpl : ExecutiveRankingEngine {

    private val ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
    private val ONE_HUNDRED = BigDecimal("100.0000").setScale(4, RoundingMode.HALF_UP)

    override fun computeRankings(payload: ProfitabilityEvaluationPayload): ExecutiveRankingsPayload {
        // 1. Top Profitable Jobs
        val topJobs = payload.jobs
            .filter { it.grossProfit > ZERO }
            .sortedWith(compareByDescending<JobProfitabilityEvaluationItem> { it.grossProfit }.thenBy { it.jobCode })
            .take(5)
            .mapIndexed { idx, job ->
                ExecutiveRankingItem(
                    rank = idx + 1,
                    dimension = ProfitabilityAlertDimension.JOB,
                    entityId = job.jobId,
                    entityCode = job.jobCode,
                    entityName = "Job ${job.jobCode}",
                    revenue = job.revenue,
                    cost = job.actualCost,
                    grossProfit = job.grossProfit,
                    marginPercentage = job.grossMarginPercentage,
                    score = job.grossProfit,
                    highlightReason = "High contribution margin of ${job.grossMarginPercentage}%"
                )
            }

        // 2. Loss-Making Jobs
        val lossJobs = payload.jobs
            .filter { it.grossProfit < ZERO }
            .sortedWith(compareBy<JobProfitabilityEvaluationItem> { it.grossProfit }.thenBy { it.jobCode })
            .take(5)
            .mapIndexed { idx, job ->
                ExecutiveRankingItem(
                    rank = idx + 1,
                    dimension = ProfitabilityAlertDimension.JOB,
                    entityId = job.jobId,
                    entityCode = job.jobCode,
                    entityName = "Job ${job.jobCode}",
                    revenue = job.revenue,
                    cost = job.actualCost,
                    grossProfit = job.grossProfit,
                    marginPercentage = job.grossMarginPercentage,
                    score = job.grossProfit.abs(),
                    highlightReason = "Cost overrun: actual cost BDT ${job.actualCost} exceeded revenue BDT ${job.revenue}"
                )
            }

        // 3. Top Profitable Products
        val topProducts = payload.products
            .filter { it.grossProfit > ZERO }
            .sortedWith(compareByDescending<ProductProfitabilityEvaluationItem> { it.grossProfit }.thenBy { it.productCode })
            .take(5)
            .mapIndexed { idx, prod ->
                ExecutiveRankingItem(
                    rank = idx + 1,
                    dimension = ProfitabilityAlertDimension.PRODUCT,
                    entityId = prod.productId,
                    entityCode = prod.productCode,
                    entityName = prod.productName,
                    revenue = prod.totalRevenue,
                    cost = prod.totalCost,
                    grossProfit = prod.grossProfit,
                    marginPercentage = prod.grossMarginPercentage,
                    score = prod.grossProfit,
                    highlightReason = "Generating BDT ${prod.grossProfit} gross profit (${prod.grossMarginPercentage}% margin)"
                )
            }

        // 4. Least Profitable Products
        val leastProducts = payload.products
            .sortedWith(compareBy<ProductProfitabilityEvaluationItem> { it.grossMarginPercentage }.thenBy { it.productCode })
            .take(5)
            .mapIndexed { idx, prod ->
                ExecutiveRankingItem(
                    rank = idx + 1,
                    dimension = ProfitabilityAlertDimension.PRODUCT,
                    entityId = prod.productId,
                    entityCode = prod.productCode,
                    entityName = prod.productName,
                    revenue = prod.totalRevenue,
                    cost = prod.totalCost,
                    grossProfit = prod.grossProfit,
                    marginPercentage = prod.grossMarginPercentage,
                    score = prod.grossMarginPercentage,
                    highlightReason = "Low margin: ${prod.grossMarginPercentage}% with unit cost BDT ${prod.unitCost}"
                )
            }

        // 5. Top Contributing Customers
        val topCustomers = payload.customers
            .sortedWith(compareByDescending<CustomerProfitabilityEvaluationItem> { it.totalRevenue }.thenBy { it.customerCode })
            .take(5)
            .mapIndexed { idx, cust ->
                ExecutiveRankingItem(
                    rank = idx + 1,
                    dimension = ProfitabilityAlertDimension.CUSTOMER,
                    entityId = cust.customerId,
                    entityCode = cust.customerCode,
                    entityName = cust.customerName,
                    revenue = cust.totalRevenue,
                    cost = cust.totalCost,
                    grossProfit = cust.grossProfit,
                    marginPercentage = cust.grossMarginPercentage,
                    contributionMarginPercentage = cust.contributionMarginPercentage,
                    score = cust.totalRevenue,
                    highlightReason = "Generates ${cust.revenueSharePercentage}% of total enterprise revenue"
                )
            }

        // 6. Lowest Margin Customers
        val lowMarginCustomers = payload.customers
            .sortedWith(compareBy<CustomerProfitabilityEvaluationItem> { it.grossMarginPercentage }.thenBy { it.customerCode })
            .take(5)
            .mapIndexed { idx, cust ->
                ExecutiveRankingItem(
                    rank = idx + 1,
                    dimension = ProfitabilityAlertDimension.CUSTOMER,
                    entityId = cust.customerId,
                    entityCode = cust.customerCode,
                    entityName = cust.customerName,
                    revenue = cust.totalRevenue,
                    cost = cust.totalCost,
                    grossProfit = cust.grossProfit,
                    marginPercentage = cust.grossMarginPercentage,
                    contributionMarginPercentage = cust.contributionMarginPercentage,
                    score = cust.grossMarginPercentage,
                    highlightReason = "Eroded margin: ${cust.grossMarginPercentage}% on BDT ${cust.totalRevenue} revenue"
                )
            }

        // 7. Highest Spend Vendors
        val topVendors = payload.vendors
            .sortedWith(compareByDescending<VendorProfitabilityEvaluationItem> { it.totalSpend }.thenBy { it.vendorCode })
            .take(5)
            .mapIndexed { idx, vend ->
                ExecutiveRankingItem(
                    rank = idx + 1,
                    dimension = ProfitabilityAlertDimension.VENDOR,
                    entityId = vend.vendorId,
                    entityCode = vend.vendorCode,
                    entityName = vend.vendorName,
                    revenue = vend.totalSpend,
                    cost = vend.totalSpend,
                    grossProfit = ZERO,
                    marginPercentage = ZERO,
                    score = vend.totalSpend,
                    highlightReason = "Accounts for ${vend.spendSharePercentage}% of total procurement spend"
                )
            }

        // 8. Highest Risk Vendors
        val riskVendors = payload.vendors
            .sortedWith(compareByDescending<VendorProfitabilityEvaluationItem> { it.dependencyRiskScore }.thenBy { it.vendorCode })
            .take(5)
            .mapIndexed { idx, vend ->
                ExecutiveRankingItem(
                    rank = idx + 1,
                    dimension = ProfitabilityAlertDimension.VENDOR,
                    entityId = vend.vendorId,
                    entityCode = vend.vendorCode,
                    entityName = vend.vendorName,
                    revenue = vend.totalSpend,
                    cost = vend.totalSpend,
                    grossProfit = ZERO,
                    marginPercentage = ZERO,
                    score = vend.dependencyRiskScore,
                    highlightReason = "High dependency index (${vend.dependencyRiskScore}) & cost pressure (${vend.costPressureScore})"
                )
            }

        return ExecutiveRankingsPayload(
            topProfitableJobs = topJobs,
            lossMakingJobs = lossJobs,
            topProfitableProducts = topProducts,
            leastProfitableProducts = leastProducts,
            topContributingCustomers = topCustomers,
            lowestMarginCustomers = lowMarginCustomers,
            highestSpendVendors = topVendors,
            highestRiskVendors = riskVendors
        )
    }

    override fun computeConcentration(payload: ProfitabilityEvaluationPayload): ExecutiveConcentrationSummary {
        // Customer Revenue Concentration
        val totalCustRev = payload.customers.sumOf { it.totalRevenue }.setScale(4, RoundingMode.HALF_UP)
        val sortedCustByRev = payload.customers.sortedByDescending { it.totalRevenue }
        val custTop1Rev = if (totalCustRev > ZERO && sortedCustByRev.isNotEmpty()) sortedCustByRev.take(1).sumOf { it.totalRevenue }.multiply(ONE_HUNDRED).divide(totalCustRev, 4, RoundingMode.HALF_UP) else ZERO
        val custTop5Rev = if (totalCustRev > ZERO && sortedCustByRev.isNotEmpty()) sortedCustByRev.take(5).sumOf { it.totalRevenue }.multiply(ONE_HUNDRED).divide(totalCustRev, 4, RoundingMode.HALF_UP) else ZERO
        val custTop10Rev = if (totalCustRev > ZERO && sortedCustByRev.isNotEmpty()) sortedCustByRev.take(10).sumOf { it.totalRevenue }.multiply(ONE_HUNDRED).divide(totalCustRev, 4, RoundingMode.HALF_UP) else ZERO
        val custRevRisk = ExecutiveProfitabilityMathUtils.calculateConcentrationRisk(custTop1Rev, custTop5Rev)

        val custRevMetric = ConcentrationMetric(
            dimension = ProfitabilityAlertDimension.CUSTOMER,
            top1SharePercentage = custTop1Rev,
            top5SharePercentage = custTop5Rev,
            top10SharePercentage = custTop10Rev,
            totalEntitiesCount = payload.customers.size,
            riskLevel = custRevRisk,
            explanation = "Top customer represents $custTop1Rev% of revenue; top 5 represent $custTop5Rev%."
        )

        // Customer Profit Concentration
        val totalCustProfit = payload.customers.map { it.grossProfit.max(ZERO) }.fold(ZERO, BigDecimal::add)
        val sortedCustByProfit = payload.customers.sortedByDescending { it.grossProfit }
        val custTop1Profit = if (totalCustProfit > ZERO && sortedCustByProfit.isNotEmpty()) sortedCustByProfit.take(1).sumOf { it.grossProfit.max(ZERO) }.multiply(ONE_HUNDRED).divide(totalCustProfit, 4, RoundingMode.HALF_UP) else ZERO
        val custTop5Profit = if (totalCustProfit > ZERO && sortedCustByProfit.isNotEmpty()) sortedCustByProfit.take(5).sumOf { it.grossProfit.max(ZERO) }.multiply(ONE_HUNDRED).divide(totalCustProfit, 4, RoundingMode.HALF_UP) else ZERO
        val custTop10Profit = if (totalCustProfit > ZERO && sortedCustByProfit.isNotEmpty()) sortedCustByProfit.take(10).sumOf { it.grossProfit.max(ZERO) }.multiply(ONE_HUNDRED).divide(totalCustProfit, 4, RoundingMode.HALF_UP) else ZERO
        val custProfitRisk = ExecutiveProfitabilityMathUtils.calculateConcentrationRisk(custTop1Profit, custTop5Profit)

        val custProfitMetric = ConcentrationMetric(
            dimension = ProfitabilityAlertDimension.CUSTOMER,
            top1SharePercentage = custTop1Profit,
            top5SharePercentage = custTop5Profit,
            top10SharePercentage = custTop10Profit,
            totalEntitiesCount = payload.customers.size,
            riskLevel = custProfitRisk,
            explanation = "Top customer contributes $custTop1Profit% of profit; top 5 contribute $custTop5Profit%."
        )

        // Product Revenue Concentration
        val totalProdRev = payload.products.sumOf { it.totalRevenue }.setScale(4, RoundingMode.HALF_UP)
        val sortedProdByRev = payload.products.sortedByDescending { it.totalRevenue }
        val prodTop1Rev = if (totalProdRev > ZERO && sortedProdByRev.isNotEmpty()) sortedProdByRev.take(1).sumOf { it.totalRevenue }.multiply(ONE_HUNDRED).divide(totalProdRev, 4, RoundingMode.HALF_UP) else ZERO
        val prodTop5Rev = if (totalProdRev > ZERO && sortedProdByRev.isNotEmpty()) sortedProdByRev.take(5).sumOf { it.totalRevenue }.multiply(ONE_HUNDRED).divide(totalProdRev, 4, RoundingMode.HALF_UP) else ZERO
        val prodTop10Rev = if (totalProdRev > ZERO && sortedProdByRev.isNotEmpty()) sortedProdByRev.take(10).sumOf { it.totalRevenue }.multiply(ONE_HUNDRED).divide(totalProdRev, 4, RoundingMode.HALF_UP) else ZERO
        val prodRevRisk = ExecutiveProfitabilityMathUtils.calculateConcentrationRisk(prodTop1Rev, prodTop5Rev)

        val prodRevMetric = ConcentrationMetric(
            dimension = ProfitabilityAlertDimension.PRODUCT,
            top1SharePercentage = prodTop1Rev,
            top5SharePercentage = prodTop5Rev,
            top10SharePercentage = prodTop10Rev,
            totalEntitiesCount = payload.products.size,
            riskLevel = prodRevRisk,
            explanation = "Top product represents $prodTop1Rev% of revenue; top 5 represent $prodTop5Rev%."
        )

        // Vendor Spend Concentration
        val totalVendSpend = payload.vendors.sumOf { it.totalSpend }.setScale(4, RoundingMode.HALF_UP)
        val sortedVendBySpend = payload.vendors.sortedByDescending { it.totalSpend }
        val vendTop1Spend = if (totalVendSpend > ZERO && sortedVendBySpend.isNotEmpty()) sortedVendBySpend.take(1).sumOf { it.totalSpend }.multiply(ONE_HUNDRED).divide(totalVendSpend, 4, RoundingMode.HALF_UP) else ZERO
        val vendTop5Spend = if (totalVendSpend > ZERO && sortedVendBySpend.isNotEmpty()) sortedVendBySpend.take(5).sumOf { it.totalSpend }.multiply(ONE_HUNDRED).divide(totalVendSpend, 4, RoundingMode.HALF_UP) else ZERO
        val vendTop10Spend = if (totalVendSpend > ZERO && sortedVendBySpend.isNotEmpty()) sortedVendBySpend.take(10).sumOf { it.totalSpend }.multiply(ONE_HUNDRED).divide(totalVendSpend, 4, RoundingMode.HALF_UP) else ZERO
        val vendSpendRisk = ExecutiveProfitabilityMathUtils.calculateConcentrationRisk(vendTop1Spend, vendTop5Spend)

        val vendSpendMetric = ConcentrationMetric(
            dimension = ProfitabilityAlertDimension.VENDOR,
            top1SharePercentage = vendTop1Spend,
            top5SharePercentage = vendTop5Spend,
            top10SharePercentage = vendTop10Spend,
            totalEntitiesCount = payload.vendors.size,
            riskLevel = vendSpendRisk,
            explanation = "Top vendor receives $vendTop1Spend% of spend; top 5 receive $vendTop5Spend%."
        )

        val overallRisk = maxOf(custRevRisk, custProfitRisk, prodRevRisk, vendSpendRisk)

        return ExecutiveConcentrationSummary(
            customerRevenueConcentration = custRevMetric,
            customerProfitConcentration = custProfitMetric,
            productRevenueConcentration = prodRevMetric,
            vendorSpendConcentration = vendSpendMetric,
            overallConcentrationRisk = overallRisk
        )
    }
}
