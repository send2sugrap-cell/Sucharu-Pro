package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class JobCostDomainMathTest {

    @Test
    fun testMoneyScaling() {
        val scaled = JobCostMathUtils.scaleMoney(BigDecimal("543.210987"))
        assertEquals(BigDecimal("543.2110"), scaled)
        assertEquals(4, scaled.scale())
    }

    @Test
    fun testTotalDirectCostCalculation() {
        val c1 = JobCostComponent("C1", "T1", "P1", "J1", JobCostComponentType.MATERIAL_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("1000.5000"))
        val c2 = JobCostComponent("C2", "T1", "P1", "J1", JobCostComponentType.LABOUR_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("500.2500"))
        val c3 = JobCostComponent("C3", "T1", "P1", "J1", JobCostComponentType.ALLOCATED_INDIRECT_COST, CostDirectness.INDIRECT, attributedAmount = BigDecimal("200.0000"))

        val totalDirect = JobCostMathUtils.calculateTotalDirectCost(listOf(c1, c2, c3))
        assertEquals(BigDecimal("1500.7500"), totalDirect)
    }

    @Test
    fun testTotalIndirectCostCalculation() {
        val c1 = JobCostComponent("C1", "T1", "P1", "J1", JobCostComponentType.MATERIAL_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("1000.5000"))
        val c2 = JobCostComponent("C2", "T1", "P1", "J1", JobCostComponentType.ALLOCATED_INDIRECT_COST, CostDirectness.INDIRECT, attributedAmount = BigDecimal("350.5000"))

        val totalIndirect = JobCostMathUtils.calculateTotalIndirectCost(listOf(c1, c2))
        assertEquals(BigDecimal("350.5000"), totalIndirect)
    }

    @Test
    fun testTotalActualCostFormula() {
        val direct = BigDecimal("1500.7500")
        val indirect = BigDecimal("350.5000")
        val total = JobCostMathUtils.calculateTotalActualCost(direct, indirect)
        assertEquals(BigDecimal("1851.2500"), total)
    }

    @Test
    fun testVarianceUnderBudgetCalculation() {
        val actual = BigDecimal("9500.0000")
        val estimated = BigDecimal("10000.0000")
        val variance = JobCostMathUtils.calculateVariance(actual, estimated)

        assertEquals(BigDecimal("-500.0000"), variance.costVariance)
        assertEquals(BigDecimal("-5.0000"), variance.costVariancePercentage)
        assertEquals(CostVarianceClassification.UNDER_BUDGET, variance.classification)
    }

    @Test
    fun testVarianceOnTargetCalculation() {
        val actual = BigDecimal("10100.0000")
        val estimated = BigDecimal("10000.0000")
        val variance = JobCostMathUtils.calculateVariance(actual, estimated)

        assertEquals(BigDecimal("100.0000"), variance.costVariance)
        assertEquals(BigDecimal("1.0000"), variance.costVariancePercentage) // +1% is within ±2%
        assertEquals(CostVarianceClassification.ON_TARGET, variance.classification)
    }

    @Test
    fun testVarianceOverBudgetCalculation() {
        val actual = BigDecimal("12000.0000")
        val estimated = BigDecimal("10000.0000")
        val variance = JobCostMathUtils.calculateVariance(actual, estimated)

        assertEquals(BigDecimal("2000.0000"), variance.costVariance)
        assertEquals(BigDecimal("20.0000"), variance.costVariancePercentage)
        assertEquals(CostVarianceClassification.OVER_BUDGET, variance.classification)
    }

    @Test
    fun testZeroEstimatedBaselineSafeHandling() {
        val actual = BigDecimal("5000.0000")
        val zeroBaseline = BigDecimal.ZERO
        val variance = JobCostMathUtils.calculateVariance(actual, zeroBaseline)

        assertNull(variance.estimatedCost)
        assertNull(variance.costVariance)
        assertNull(variance.costVariancePercentage)
        assertEquals(CostVarianceClassification.BASELINE_UNAVAILABLE, variance.classification)

        val nullBaseline = JobCostMathUtils.calculateVariance(actual, null)
        assertEquals(CostVarianceClassification.BASELINE_UNAVAILABLE, nullBaseline.classification)
    }

    @Test
    fun testIntegrityHashGeneration() {
        val hash1 = JobCostMathUtils.generateIntegrityHash(
            tenantId = "T1",
            projectId = "P1",
            jobId = "JOB-100",
            calculationVersion = "JOB_COST_ENGINE_V1",
            totalActualCost = BigDecimal("15000.0000"),
            totalDirectCost = BigDecimal("12000.0000"),
            totalIndirectCost = BigDecimal("3000.0000"),
            componentHashes = listOf("MATERIAL_COST:8000.0000", "LABOUR_COST:4000.0000", "ALLOCATED_INDIRECT_COST:3000.0000")
        )

        val hash2 = JobCostMathUtils.generateIntegrityHash(
            tenantId = "T1",
            projectId = "P1",
            jobId = "JOB-100",
            calculationVersion = "JOB_COST_ENGINE_V1",
            totalActualCost = BigDecimal("15000.0000"),
            totalDirectCost = BigDecimal("12000.0000"),
            totalIndirectCost = BigDecimal("3000.0000"),
            componentHashes = listOf("LABOUR_COST:4000.0000", "MATERIAL_COST:8000.0000", "ALLOCATED_INDIRECT_COST:3000.0000") // reordered
        )

        assertNotNull(hash1)
        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2) // Stable ordering ensures matching hash
    }
}
