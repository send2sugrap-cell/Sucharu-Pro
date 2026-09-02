package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductProfitabilityReconciliationTest {

    private val reconciliationService = ProductProfitabilityReconciliationServiceImpl()

    @Test
    fun testReconciliationSuccess() = runBlocking {
        val snapshot = ProductProfitabilitySnapshot(
            snapshotId = "SNAP-1",
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-1",
            totalQuantity = 100,
            recognizedRevenue = BigDecimal("10000.0000"),
            totalActualCost = BigDecimal("6000.0000"),
            grossProfit = BigDecimal("4000.0000"),
            grossMarginPercentage = BigDecimal("40.0000"),
            unitEconomics = ProductUnitEconomics(
                quantity = 100,
                unitRevenue = BigDecimal("100.0000"),
                unitActualCost = BigDecimal("60.0000"),
                unitGrossProfit = BigDecimal("40.0000")
            ),
            costBreakdown = listOf(
                ProductCostBreakdownItem(componentType = JobCostComponentType.MATERIAL_COST, amount = BigDecimal("4000.0000")),
                ProductCostBreakdownItem(componentType = JobCostComponentType.LABOUR_COST, amount = BigDecimal("2000.0000"))
            )
        )

        val revSources = listOf(
            ProductRevenueAttribution(
                revenueAttributionId = "R1",
                tenantId = "T1",
                projectId = "P1",
                productId = "PROD-1",
                recognizedRevenue = BigDecimal("10000.0000"),
                sourceEntityId = "INV-1"
            )
        )

        val costSources = listOf(
            ProductCostAttribution(
                costAttributionId = "C1",
                tenantId = "T1",
                projectId = "P1",
                productId = "PROD-1",
                componentType = JobCostComponentType.MATERIAL_COST,
                attributedAmount = BigDecimal("4000.0000"),
                sourceEntityId = "JOB-1"
            ),
            ProductCostAttribution(
                costAttributionId = "C2",
                tenantId = "T1",
                projectId = "P1",
                productId = "PROD-1",
                componentType = JobCostComponentType.LABOUR_COST,
                attributedAmount = BigDecimal("2000.0000"),
                sourceEntityId = "JOB-1"
            )
        )

        val res = reconciliationService.reconcileSnapshot(snapshot, revSources, costSources)
        assertTrue(res is DomainResult.Success)
        val event = (res as DomainResult.Success).data
        assertTrue(event.isReconciled)
        assertTrue(event.revenueReconciled)
        assertTrue(event.costReconciled)
        assertTrue(event.unitEconomicsReconciled)
        assertTrue(event.discrepancies.isEmpty())
    }

    @Test
    fun testReconciliationDiscrepancyDetection() = runBlocking {
        val snapshot = ProductProfitabilitySnapshot(
            snapshotId = "SNAP-1",
            tenantId = "T1",
            projectId = "P1",
            productId = "PROD-1",
            totalQuantity = 100,
            recognizedRevenue = BigDecimal("10000.0000"), // Mismatch with revenueSources (8000)
            totalActualCost = BigDecimal("6000.0000"),
            grossProfit = BigDecimal("4000.0000"),
            costBreakdown = listOf(
                ProductCostBreakdownItem(componentType = JobCostComponentType.MATERIAL_COST, amount = BigDecimal("6000.0000"))
            )
        )

        val revSources = listOf(
            ProductRevenueAttribution(
                revenueAttributionId = "R1",
                tenantId = "T1",
                projectId = "P1",
                productId = "PROD-1",
                recognizedRevenue = BigDecimal("8000.0000"),
                sourceEntityId = "INV-1"
            )
        )

        val res = reconciliationService.reconcileSnapshot(snapshot, revSources, emptyList())
        assertTrue(res is DomainResult.Success)
        val event = (res as DomainResult.Success).data
        assertFalse(event.isReconciled)
        assertFalse(event.revenueReconciled)
        assertTrue(event.discrepancies.isNotEmpty())
    }
}
