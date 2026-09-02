package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Reconciliation Test Suite for Customer Profitability (Module 16 Step 04).
 */
class CustomerProfitabilityReconciliationTest {

    private val reconciliationService = CustomerProfitabilityReconciliationServiceImpl()

    @Test
    fun testPerfectReconciliation() = runBlocking {
        val tenantId = "tenant-001"
        val projectId = "proj-001"
        val customerId = "cust-100"

        val revSources = listOf(
            CustomerRevenueAttribution(
                revenueAttributionId = "REV-1",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                recognizedRevenue = BigDecimal("10000.0000"),
                sourceEntityId = "INV-1"
            )
        )

        val costSources = listOf(
            CustomerCostAttribution(
                costAttributionId = "COST-1",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                componentType = JobCostComponentType.MATERIAL_COST,
                attributedAmount = BigDecimal("6000.0000"),
                sourceEntityId = "JOB-1"
            )
        )

        val components = listOf(
            CustomerCostBreakdownItem(
                componentType = JobCostComponentType.MATERIAL_COST,
                amount = BigDecimal("6000.0000"),
                percentageOfTotalCost = BigDecimal("100.0000")
            )
        )

        val snapshot = CustomerProfitabilitySnapshot(
            snapshotId = "SNAP-1",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            recognizedRevenue = BigDecimal("10000.0000"),
            totalActualCost = BigDecimal("6000.0000"),
            grossProfit = BigDecimal("4000.0000"),
            grossMarginPercentage = BigDecimal("40.0000"),
            contributionMetrics = CustomerContributionMetrics(
                attributableVariableCost = BigDecimal("6000.0000"),
                contributionAmount = BigDecimal("4000.0000")
            ),
            costBreakdown = components,
            isReconciled = true,
            integrityHash = "dummy-hash"
        )

        val res = reconciliationService.reconcileCustomerSnapshot(snapshot, revSources, costSources)
        assertTrue(res is DomainResult.Success)
        val event = (res as DomainResult.Success).data
        assertTrue(event.isReconciled)
        assertTrue(event.revenueReconciled)
        assertTrue(event.costReconciled)
        assertTrue(event.profitReconciled)
        assertTrue(event.contributionReconciled)
        assertTrue(event.discrepancies.isEmpty())
    }

    @Test
    fun testReconciliationDiscrepancyDetection() = runBlocking {
        val tenantId = "tenant-001"
        val projectId = "proj-001"
        val customerId = "cust-100"

        val revSources = listOf(
            CustomerRevenueAttribution(
                revenueAttributionId = "REV-1",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                recognizedRevenue = BigDecimal("10000.0000"),
                sourceEntityId = "INV-1"
            )
        )

        // Snapshot claims revenue of 12000 while sources only equal 10000
        val snapshot = CustomerProfitabilitySnapshot(
            snapshotId = "SNAP-1",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            recognizedRevenue = BigDecimal("12000.0000"),
            totalActualCost = BigDecimal("6000.0000"),
            grossProfit = BigDecimal("6000.0000"),
            grossMarginPercentage = BigDecimal("50.0000"),
            contributionMetrics = CustomerContributionMetrics(
                attributableVariableCost = BigDecimal("6000.0000"),
                contributionAmount = BigDecimal("6000.0000")
            ),
            costBreakdown = emptyList(),
            isReconciled = true,
            integrityHash = "dummy-hash"
        )

        val res = reconciliationService.reconcileCustomerSnapshot(snapshot, revSources, emptyList())
        assertTrue(res is DomainResult.Success)
        val event = (res as DomainResult.Success).data
        assertFalse(event.isReconciled)
        assertFalse(event.revenueReconciled)
        assertTrue(event.discrepancies.isNotEmpty())
    }
}
