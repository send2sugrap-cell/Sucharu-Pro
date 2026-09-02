package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Ranking, Concentration, and Comparison Test Suite for Customer Profitability (Module 16 Step 04).
 */
class CustomerProfitabilityRankingAndConcentrationTest {

    private val rankingService = CustomerProfitabilityRankingServiceImpl()

    @Test
    fun testCustomerRanking() {
        val snapA = createMockSnapshot("cust-A", "Customer A", BigDecimal("50000.0000"), BigDecimal("30000.0000"), BigDecimal("20000.0000"))
        val snapB = createMockSnapshot("cust-B", "Customer B", BigDecimal("80000.0000"), BigDecimal("40000.0000"), BigDecimal("40000.0000"))
        val snapC = createMockSnapshot("cust-C", "Customer C", BigDecimal("20000.0000"), BigDecimal("15000.0000"), BigDecimal("5000.0000"))

        val rankedByProfit = rankingService.rankCustomers(listOf(snapA, snapB, snapC), CustomerRankingCriteria.GROSS_PROFIT)
        assertEquals(3, rankedByProfit.size)
        assertEquals("cust-B", rankedByProfit[0].customerId) // 40k profit
        assertEquals("cust-A", rankedByProfit[1].customerId) // 20k profit
        assertEquals("cust-C", rankedByProfit[2].customerId) // 5k profit
        assertEquals(1, rankedByProfit[0].rank)
        assertEquals(2, rankedByProfit[1].rank)
        assertEquals(3, rankedByProfit[2].rank)

        val rankedByRev = rankingService.rankCustomers(listOf(snapA, snapB, snapC), CustomerRankingCriteria.REVENUE)
        assertEquals("cust-B", rankedByRev[0].customerId) // 80k rev
        assertEquals("cust-A", rankedByRev[1].customerId) // 50k rev
        assertEquals("cust-C", rankedByRev[2].customerId) // 20k rev
    }

    @Test
    fun testConcentrationAnalysis() {
        val snapA = createMockSnapshot("cust-A", "Customer A", BigDecimal("60000.0000"), BigDecimal("30000.0000"), BigDecimal("30000.0000"))
        val snapB = createMockSnapshot("cust-B", "Customer B", BigDecimal("30000.0000"), BigDecimal("15000.0000"), BigDecimal("15000.0000"))
        val snapC = createMockSnapshot("cust-C", "Customer C", BigDecimal("10000.0000"), BigDecimal("5000.0000"), BigDecimal("5000.0000"))

        // Total business revenue = 100k
        val analysis = rankingService.analyzeConcentration(listOf(snapA, snapB, snapC))
        assertEquals(BigDecimal("100000.0000"), analysis.totalBusinessRevenue)
        assertEquals(BigDecimal("50000.0000"), analysis.totalBusinessProfit)

        // Top 1 (Cust A) is 60k / 100k = 60% -> CONCENTRATION_HIGH (>25%)
        assertEquals(BigDecimal("60.0000"), analysis.top1RevenueSharePercentage)
        assertEquals(CustomerConcentrationRisk.CONCENTRATION_HIGH, analysis.concentrationRisk)
    }

    @Test
    fun testCustomerComparison() {
        val snapA = createMockSnapshot("cust-A", "Customer A", BigDecimal("50000.0000"), BigDecimal("30000.0000"), BigDecimal("20000.0000"))
        val snapB = createMockSnapshot("cust-B", "Customer B", BigDecimal("80000.0000"), BigDecimal("40000.0000"), BigDecimal("40000.0000"))

        val comparison = rankingService.compareCustomers(listOf(snapA, snapB), listOf("cust-A", "cust-B"))
        assertEquals(2, comparison.size)
        assertEquals("cust-A", comparison[0].customerId)
        assertEquals("cust-B", comparison[1].customerId)
    }

    private fun createMockSnapshot(
        customerId: String,
        customerName: String,
        revenue: BigDecimal,
        cost: BigDecimal,
        grossProfit: BigDecimal
    ): CustomerProfitabilitySnapshot {
        val margin = CustomerProfitabilityMathUtils.calculateGrossMarginPercentage(revenue, cost)
        return CustomerProfitabilitySnapshot(
            snapshotId = "SNAP-$customerId",
            tenantId = "tenant-001",
            projectId = "proj-001",
            customerId = customerId,
            customerName = customerName,
            recognizedRevenue = revenue,
            totalActualCost = cost,
            grossProfit = grossProfit,
            grossMarginPercentage = margin,
            contributionMetrics = CustomerContributionMetrics(
                attributableVariableCost = cost,
                contributionAmount = grossProfit
            ),
            operationalMetrics = CustomerOperationalMetrics(orderCount = 5, totalQuantitySold = 100),
            costBreakdown = emptyList(),
            profitabilityClassification = CustomerProfitabilityClassification.PROFITABLE,
            integrityHash = "hash-$customerId"
        )
    }
}
