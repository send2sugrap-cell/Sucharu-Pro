package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorProfitabilityAttributionAndProvenanceTest {

    private val collector = VendorProfitabilitySourceCollectorImpl()

    @Test
    fun testAttributionHierarchyAndDeduplication() = runBlocking {
        val cost1 = VendorCostAttribution(
            costAttributionId = "COST-1",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            workOrderId = "WO-101",
            jobId = "JOB-201",
            productId = "PROD-301",
            customerId = "CUST-401",
            componentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
            attributedAmount = BigDecimal("25000.0000"),
            isPaid = true,
            sourceEntityId = "WO-101"
        )

        // Duplicate cost with identical fields
        val costDuplicate = cost1.copy(costAttributionId = "COST-1-DUP")

        val revContext = VendorRevenueContextAttribution(
            revenueContextId = "REV-1",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            jobId = "JOB-201",
            recognizedRevenueContext = BigDecimal("60000.0000"),
            sourceEntityId = "INV-501"
        )

        val res = collector.collectVendorData(
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            customCosts = listOf(cost1, costDuplicate),
            customRevenueContext = listOf(revContext)
        )

        assertTrue(res is DomainResult.Success)
        val data = (res as DomainResult.Success).data

        // Total should be 25000 (deduplicated)
        assertEquals(BigDecimal("25000.0000"), data.totalVendorCost)
        assertEquals(BigDecimal("25000.0000"), data.paidVendorCost)
        assertEquals(BigDecimal.ZERO.setScale(VendorProfitabilityMathUtils.SCALE), data.outstandingExposure)
        assertEquals(1, data.costAttributions.size)
        assertTrue(data.warnings.isNotEmpty())
        assertEquals(1, data.workOrderSummaries.size)
        assertEquals(1, data.jobSummaries.size)
        assertEquals(1, data.productSummaries.size)
        assertEquals(1, data.customerSummaries.size)
    }

    @Test
    fun testUnattributedCostDetection() = runBlocking {
        val costUnattached = VendorCostAttribution(
            costAttributionId = "COST-UNATTRIBUTED",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            workOrderId = null,
            jobId = null,
            productId = null,
            customerId = null,
            componentType = JobCostComponentType.OTHER_DIRECT_COST,
            attributedAmount = BigDecimal("5000.0000"),
            sourceEntityId = "EXP-999"
        )

        val res = collector.collectVendorData(
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            customCosts = listOf(costUnattached)
        )

        assertTrue(res is DomainResult.Success)
        val data = (res as DomainResult.Success).data

        assertEquals(BigDecimal("5000.0000"), data.totalVendorCost)
        assertEquals(1, data.unattributedItems.size)
        assertEquals("COST-UNATTRIBUTED", data.costAttributions.first().costAttributionId)
    }
}
