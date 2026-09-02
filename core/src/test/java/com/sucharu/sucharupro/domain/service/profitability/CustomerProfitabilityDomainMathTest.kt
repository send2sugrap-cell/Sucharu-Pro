package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Domain Math and High-Precision Test Suite for Customer Profitability & Contribution Analysis (Module 16 Step 04).
 */
class CustomerProfitabilityDomainMathTest {

    @Test
    fun testGrossProfitAndMarginCalculations() {
        val rev = BigDecimal("10000.0000")
        val cost = BigDecimal("6500.0000")

        val gp = CustomerProfitabilityMathUtils.calculateGrossProfit(rev, cost)
        assertEquals(BigDecimal("3500.0000"), gp)

        val margin = CustomerProfitabilityMathUtils.calculateGrossMarginPercentage(rev, cost)
        assertNotNull(margin)
        assertEquals(BigDecimal("35.0000"), margin)

        val c2r = CustomerProfitabilityMathUtils.calculateCostToRevenuePercentage(cost, rev)
        assertNotNull(c2r)
        assertEquals(BigDecimal("65.0000"), c2r)
    }

    @Test
    fun testContributionAmountAndMargin() {
        val rev = BigDecimal("20000.0000")
        val varCost = BigDecimal("12000.0000")

        val contrib = CustomerProfitabilityMathUtils.calculateContributionAmount(rev, varCost)
        assertEquals(BigDecimal("8000.0000"), contrib)

        val contribMargin = CustomerProfitabilityMathUtils.calculateContributionMarginPercentage(rev, varCost)
        assertNotNull(contribMargin)
        assertEquals(BigDecimal("40.0000"), contribMargin)
    }

    @Test
    fun testOperationalMetricsAndZeroSafety() {
        val rev = BigDecimal("50000.0000")
        val cost = BigDecimal("30000.0000")
        val gp = BigDecimal("20000.0000")

        val aov = CustomerProfitabilityMathUtils.calculateAverageOrderValue(rev, 10)
        assertEquals(BigDecimal("5000.0000"), aov)

        val ajv = CustomerProfitabilityMathUtils.calculateAverageJobValue(rev, 25)
        assertEquals(BigDecimal("2000.0000"), ajv)

        val arpu = CustomerProfitabilityMathUtils.calculateAverageRevenuePerUnit(rev, 1000)
        assertEquals(BigDecimal("50.0000"), arpu)

        val acpu = CustomerProfitabilityMathUtils.calculateAverageCostPerUnit(cost, 1000)
        assertEquals(BigDecimal("30.0000"), acpu)

        val apu = CustomerProfitabilityMathUtils.calculateAverageProfitPerUnit(gp, 1000)
        assertEquals(BigDecimal("20.0000"), apu)

        // Zero safety checks
        assertNull(CustomerProfitabilityMathUtils.calculateAverageOrderValue(rev, 0))
        assertNull(CustomerProfitabilityMathUtils.calculateAverageJobValue(rev, 0))
        assertNull(CustomerProfitabilityMathUtils.calculateAverageRevenuePerUnit(rev, 0))
        assertNull(CustomerProfitabilityMathUtils.calculateAverageCostPerUnit(cost, 0))
        assertNull(CustomerProfitabilityMathUtils.calculateAverageProfitPerUnit(gp, 0))
        assertNull(CustomerProfitabilityMathUtils.calculateGrossMarginPercentage(BigDecimal.ZERO, cost))
        assertNull(CustomerProfitabilityMathUtils.calculateContributionMarginPercentage(BigDecimal.ZERO, variableCost = BigDecimal("100")))
    }

    @Test
    fun testProfitabilityClassification() {
        val rev = BigDecimal("10000.0000")

        // Highly profitable (>= 30%)
        val c1 = CustomerProfitabilityMathUtils.classifyCustomerProfitability(rev, BigDecimal("6000.0000"), BigDecimal("40.0000"))
        assertEquals(CustomerProfitabilityClassification.HIGHLY_PROFITABLE, c1)

        // Profitable (15% - 30%)
        val c2 = CustomerProfitabilityMathUtils.classifyCustomerProfitability(rev, BigDecimal("8000.0000"), BigDecimal("20.0000"))
        assertEquals(CustomerProfitabilityClassification.PROFITABLE, c2)

        // Low margin (0% - 15%)
        val c3 = CustomerProfitabilityMathUtils.classifyCustomerProfitability(rev, BigDecimal("9500.0000"), BigDecimal("5.0000"))
        assertEquals(CustomerProfitabilityClassification.LOW_MARGIN, c3)

        // Break even (0%)
        val c4 = CustomerProfitabilityMathUtils.classifyCustomerProfitability(rev, BigDecimal("10000.0000"), BigDecimal("0.0000"))
        assertEquals(CustomerProfitabilityClassification.BREAK_EVEN, c4)

        // Loss making (< 0%)
        val c5 = CustomerProfitabilityMathUtils.classifyCustomerProfitability(rev, BigDecimal("12000.0000"), BigDecimal("-20.0000"))
        assertEquals(CustomerProfitabilityClassification.LOSS_MAKING, c5)

        // No revenue
        val c6 = CustomerProfitabilityMathUtils.classifyCustomerProfitability(BigDecimal.ZERO, BigDecimal.ZERO, null)
        assertEquals(CustomerProfitabilityClassification.NO_REVENUE, c6)
    }

    @Test
    fun testTrendCalculation() {
        val t1 = CustomerProfitabilityMathUtils.calculateTrend(BigDecimal("30.0000"), BigDecimal("22.0000"))
        assertEquals(CustomerProfitabilityTrend.STRONGLY_IMPROVING, t1)

        val t2 = CustomerProfitabilityMathUtils.calculateTrend(BigDecimal("25.0000"), BigDecimal("22.0000"))
        assertEquals(CustomerProfitabilityTrend.IMPROVING, t2)

        val t3 = CustomerProfitabilityMathUtils.calculateTrend(BigDecimal("22.5000"), BigDecimal("22.0000"))
        assertEquals(CustomerProfitabilityTrend.STABLE, t3)

        val t4 = CustomerProfitabilityMathUtils.calculateTrend(BigDecimal("20.0000"), BigDecimal("23.0000"))
        assertEquals(CustomerProfitabilityTrend.DECLINING, t4)

        val t5 = CustomerProfitabilityMathUtils.calculateTrend(BigDecimal("15.0000"), BigDecimal("23.0000"))
        assertEquals(CustomerProfitabilityTrend.STRONGLY_DECLINING, t5)

        val t6 = CustomerProfitabilityMathUtils.calculateTrend(BigDecimal("20.0000"), null)
        assertEquals(CustomerProfitabilityTrend.INSUFFICIENT_DATA, t6)
    }

    @Test
    fun testConcentrationRiskAssessment() {
        val r1 = CustomerProfitabilityMathUtils.assessConcentrationRisk(BigDecimal("30.0000"), BigDecimal("70.0000"))
        assertEquals(CustomerConcentrationRisk.CONCENTRATION_HIGH, r1)

        val r2 = CustomerProfitabilityMathUtils.assessConcentrationRisk(BigDecimal("15.0000"), BigDecimal("40.0000"))
        assertEquals(CustomerConcentrationRisk.CONCENTRATION_MODERATE, r2)

        val r3 = CustomerProfitabilityMathUtils.assessConcentrationRisk(BigDecimal("5.0000"), BigDecimal("20.0000"))
        assertEquals(CustomerConcentrationRisk.CONCENTRATION_LOW, r3)
    }

    @Test
    fun testSha256FingerprintAndIntegrityHash() {
        val fp1 = CustomerProfitabilityMathUtils.generateFingerprint(
            tenantId = "tenant-001",
            customerId = "cust-123",
            sourceModule = "MODULE_14",
            sourceEntityType = "INVOICE",
            sourceEntityId = "INV-001",
            sourceTransactionId = "TX-001",
            componentType = "REVENUE"
        )
        val fp2 = CustomerProfitabilityMathUtils.generateFingerprint(
            tenantId = "tenant-001",
            customerId = "cust-123",
            sourceModule = "MODULE_14",
            sourceEntityType = "INVOICE",
            sourceEntityId = "INV-001",
            sourceTransactionId = "TX-001",
            componentType = "REVENUE"
        )
        assertEquals(fp1, fp2)
        assertEquals(64, fp1.length)

        val hash = CustomerProfitabilityMathUtils.generateIntegrityHash(
            tenantId = "tenant-001",
            projectId = "proj-001",
            customerId = "cust-123",
            periodType = "ALL_TIME",
            calculationVersion = "CUSTOMER_PROFITABILITY_V1",
            revenue = BigDecimal("1000.0000"),
            cost = BigDecimal("800.0000"),
            grossProfit = BigDecimal("200.0000"),
            contribution = BigDecimal("300.0000"),
            components = emptyList(),
            provenanceFingerprints = listOf(fp1)
        )
        assertEquals(64, hash.length)
    }
}
