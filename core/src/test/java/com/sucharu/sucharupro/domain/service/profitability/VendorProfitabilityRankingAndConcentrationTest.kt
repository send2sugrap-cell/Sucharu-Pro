package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorProfitabilityRankingAndConcentrationTest {

    private val rankingService = VendorProfitabilityRankingServiceImpl()

    private val snap1 = VendorProfitabilitySnapshot(
        snapshotId = "S1",
        tenantId = "T1",
        projectId = "P1",
        vendorId = "V1",
        vendorName = "Vendor Alpha",
        totalVendorCost = BigDecimal("50000.0000"),
        directVendorCost = BigDecimal("50000.0000"),
        paidVendorCost = BigDecimal("50000.0000"),
        outstandingExposure = BigDecimal.ZERO,
        attributedRevenueContext = BigDecimal("150000.0000"),
        attributedTotalJobCost = BigDecimal("50000.0000"),
        fulfillmentProfitabilityImpact = BigDecimal("100000.0000"),
        efficiencyScore = BigDecimal("92.0000"),
        riskClassification = VendorRiskClassification.LOW_RISK,
        integrityHash = "H1"
    )

    private val snap2 = VendorProfitabilitySnapshot(
        snapshotId = "S2",
        tenantId = "T1",
        projectId = "P1",
        vendorId = "V2",
        vendorName = "Vendor Beta",
        totalVendorCost = BigDecimal("80000.0000"),
        directVendorCost = BigDecimal("80000.0000"),
        paidVendorCost = BigDecimal("40000.0000"),
        outstandingExposure = BigDecimal("40000.0000"),
        attributedRevenueContext = BigDecimal("120000.0000"),
        attributedTotalJobCost = BigDecimal("80000.0000"),
        fulfillmentProfitabilityImpact = BigDecimal("40000.0000"),
        efficiencyScore = BigDecimal("65.0000"),
        riskClassification = VendorRiskClassification.HIGH_RISK,
        integrityHash = "H2"
    )

    @Test
    fun testDeterministicRankingByCostAndEfficiency() {
        val list = listOf(snap1, snap2)

        // Rank by total cost descending (V2 first)
        val rankedByCost = rankingService.rankVendors(list, VendorRankingCriteria.TOTAL_COST, ascending = false)
        assertEquals(2, rankedByCost.size)
        assertEquals("V2", rankedByCost[0].vendorId)
        assertEquals(1, rankedByCost[0].rank)
        assertEquals("V1", rankedByCost[1].vendorId)
        assertEquals(2, rankedByCost[1].rank)

        // Rank by efficiency descending (V1 first)
        val rankedByEff = rankingService.rankVendors(list, VendorRankingCriteria.EFFICIENCY_SCORE, ascending = false)
        assertEquals("V1", rankedByEff[0].vendorId)
        assertEquals("V2", rankedByEff[1].vendorId)
    }

    @Test
    fun testConcentrationAnalysis() {
        val list = listOf(snap1, snap2)
        val conc = rankingService.analyzeConcentration("T1", "P1", list)

        assertEquals(BigDecimal("130000.0000"), conc.totalVendorSpend)
        assertEquals(2, conc.totalVendorCount)
        assertEquals("V2", conc.top1VendorId)
        assertEquals(BigDecimal("80000.0000"), conc.top1Spend)
        assertTrue(conc.top1SharePercentage > BigDecimal("60.0000"))
        assertEquals(VendorDependencyClassification.CRITICAL_DEPENDENCY, conc.concentrationRisk)
    }

    @Test
    fun testMultiVendorComparison() {
        val list = listOf(snap1, snap2)
        val comps = rankingService.compareVendors(list, listOf("V1", "V2"))

        assertEquals(2, comps.size)
        assertEquals("Vendor Alpha", comps[0].vendorName)
        assertEquals("Vendor Beta", comps[1].vendorName)
    }
}
