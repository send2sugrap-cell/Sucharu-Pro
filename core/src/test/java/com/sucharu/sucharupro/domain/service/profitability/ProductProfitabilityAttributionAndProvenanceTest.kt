package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductProfitabilityAttributionAndProvenanceTest {

    private val collector = ProductProfitabilitySourceCollectorImpl()

    @Test
    fun testAttributionAndDeduplication() = runBlocking {
        val rev1 = ProductRevenueAttribution(
            revenueAttributionId = "REV-1",
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-100",
            quantity = 50,
            recognizedRevenue = BigDecimal("25000.0000"),
            sourceEntityId = "INV-001"
        )
        val revDup = ProductRevenueAttribution(
            revenueAttributionId = "REV-2",
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-100",
            quantity = 50,
            recognizedRevenue = BigDecimal("25000.0000"),
            sourceEntityId = "INV-001" // Duplicate sourceEntityId
        )

        val cost1 = ProductCostAttribution(
            costAttributionId = "COST-1",
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-100",
            componentType = JobCostComponentType.MATERIAL_COST,
            attributedAmount = BigDecimal("12000.0000"),
            sourceEntityId = "JOB-001"
        )

        val res = collector.collectProductData(
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-100",
            customRevenue = listOf(rev1, revDup),
            customCosts = listOf(cost1)
        )

        assertTrue(res is DomainResult.Success)
        val data = (res as DomainResult.Success).data
        assertEquals(1, data.revenueAttributions.size) // Deduplicated
        assertEquals(1, data.costAttributions.size)
        assertEquals(BigDecimal("25000.0000"), data.totalRecognizedRevenue)
        assertEquals(BigDecimal("12000.0000"), data.totalActualCost)
        assertEquals(50, data.totalQuantity)
        assertEquals(ProductSourceIntegrityStatus.DUPLICATE_DETECTED, data.sourceIntegrity)
    }

    @Test
    fun testDeterministicIntegrityHashGeneration() {
        val comps = listOf(
            ProductCostBreakdownItem(componentType = JobCostComponentType.MATERIAL_COST, amount = BigDecimal("5000.0000")),
            ProductCostBreakdownItem(componentType = JobCostComponentType.LABOUR_COST, amount = BigDecimal("3000.0000"))
        )
        val fps = listOf("FINGERPRINT_A", "FINGERPRINT_B")

        val hash1 = ProductProfitabilityMathUtils.generateIntegrityHash(
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-1",
            calculationVersion = "V1",
            quantity = 100,
            recognizedRevenue = BigDecimal("15000.0000"),
            totalActualCost = BigDecimal("8000.0000"),
            grossProfit = BigDecimal("7000.0000"),
            components = comps,
            provenanceFingerprints = fps
        )

        val hash2 = ProductProfitabilityMathUtils.generateIntegrityHash(
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-1",
            calculationVersion = "V1",
            quantity = 100,
            recognizedRevenue = BigDecimal("15000.0000"),
            totalActualCost = BigDecimal("8000.0000"),
            grossProfit = BigDecimal("7000.0000"),
            components = comps,
            provenanceFingerprints = fps.reversed() // Order should not affect sorted hash
        )

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 hex string length
    }
}
