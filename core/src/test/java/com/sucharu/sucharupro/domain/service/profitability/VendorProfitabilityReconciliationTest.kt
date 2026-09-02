package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorProfitabilityReconciliationTest {

    private val collector = VendorProfitabilitySourceCollectorImpl()
    private val reconService = VendorProfitabilityReconciliationServiceImpl()

    @Test
    fun testBalancedReconciliation() = runBlocking {
        val cost = VendorCostAttribution(
            costAttributionId = "COST-1",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            workOrderId = "WO-1",
            jobId = "JOB-1",
            productId = "PROD-1",
            customerId = "CUST-1",
            componentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
            attributedAmount = BigDecimal("30000.0000"),
            isPaid = true,
            sourceEntityId = "WO-1"
        )

        val sourceRes = collector.collectVendorData(
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            customCosts = listOf(cost)
        )
        val sourceData = (sourceRes as DomainResult.Success).data

        val snapshot = VendorProfitabilitySnapshot(
            snapshotId = "SNAP-1",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            vendorName = "Precision Prints",
            totalVendorCost = BigDecimal("30000.0000"),
            directVendorCost = BigDecimal("30000.0000"),
            paidVendorCost = BigDecimal("30000.0000"),
            outstandingExposure = BigDecimal.ZERO,
            attributedRevenueContext = BigDecimal.ZERO,
            attributedTotalJobCost = BigDecimal("30000.0000"),
            fulfillmentProfitabilityImpact = BigDecimal("-30000.0000"),
            costBreakdown = listOf(
                VendorCostBreakdownItem(
                    componentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
                    amount = BigDecimal("30000.0000"),
                    percentageOfTotalCost = BigDecimal("100.0000")
                )
            ),
            integrityHash = "HASH-1"
        )

        val reconEvent = reconService.reconcile(snapshot, sourceData)

        assertTrue(reconEvent.isBalanced)
        assertEquals(BigDecimal.ZERO.setScale(VendorProfitabilityMathUtils.SCALE), reconEvent.totalCostDifference)
        assertEquals(BigDecimal.ZERO.setScale(VendorProfitabilityMathUtils.SCALE), reconEvent.componentDifference)
        assertEquals(BigDecimal.ZERO.setScale(VendorProfitabilityMathUtils.SCALE), reconEvent.provenanceDifference)
        assertTrue(reconEvent.paidVsLiabilityValid)
        assertTrue(reconEvent.errorDetails.isEmpty())
    }

    @Test
    fun testReconciliationDiscrepancyDetection() = runBlocking {
        val cost = VendorCostAttribution(
            costAttributionId = "COST-1",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            workOrderId = "WO-1",
            jobId = "JOB-1",
            productId = "PROD-1",
            customerId = "CUST-1",
            componentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
            attributedAmount = BigDecimal("30000.0000"),
            isPaid = false,
            sourceEntityId = "WO-1"
        )

        val sourceRes = collector.collectVendorData(
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            customCosts = listOf(cost)
        )
        val sourceData = (sourceRes as DomainResult.Success).data

        // Snapshot has mismatched total and invalid paid > total
        val snapshot = VendorProfitabilitySnapshot(
            snapshotId = "SNAP-MISMATCH",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            vendorName = "Precision Prints",
            totalVendorCost = BigDecimal("25000.0000"), // Mismatch vs source 30000
            directVendorCost = BigDecimal("25000.0000"),
            paidVendorCost = BigDecimal("35000.0000"), // Invalid paid > total
            outstandingExposure = BigDecimal("-10000.0000"),
            attributedRevenueContext = BigDecimal.ZERO,
            attributedTotalJobCost = BigDecimal("25000.0000"),
            fulfillmentProfitabilityImpact = BigDecimal("-25000.0000"),
            costBreakdown = listOf(
                VendorCostBreakdownItem(
                    componentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
                    amount = BigDecimal("20000.0000"), // Comp mismatch vs 25000
                    percentageOfTotalCost = BigDecimal("80.0000")
                )
            ),
            integrityHash = "HASH-MISMATCH"
        )

        val reconEvent = reconService.reconcile(snapshot, sourceData)

        assertFalse(reconEvent.isBalanced)
        assertTrue(reconEvent.totalCostDifference > BigDecimal.ZERO)
        assertTrue(reconEvent.componentDifference > BigDecimal.ZERO)
        assertFalse(reconEvent.paidVsLiabilityValid)
        assertTrue(reconEvent.errorDetails.isNotEmpty())
    }
}
