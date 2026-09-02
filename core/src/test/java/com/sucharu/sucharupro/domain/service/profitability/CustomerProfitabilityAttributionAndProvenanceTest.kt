package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Attribution, Deduplication, and Provenance Test Suite for Customer Profitability (Module 16 Step 04).
 */
class CustomerProfitabilityAttributionAndProvenanceTest {

    private val collector = CustomerProfitabilitySourceCollectorImpl()

    @Test
    fun testAttributionCollectionAndDeduplication() = runBlocking {
        val tenantId = "tenant-001"
        val projectId = "proj-001"
        val customerId = "cust-100"

        val rev1 = CustomerRevenueAttribution(
            revenueAttributionId = "REV-1",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            orderId = "ORD-1",
            recognizedRevenue = BigDecimal("5000.0000"),
            sourceEntityId = "INV-1"
        )
        // Duplicate rev
        val rev2 = rev1.copy(revenueAttributionId = "REV-2")

        val cost1 = CustomerCostAttribution(
            costAttributionId = "COST-1",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            orderId = "ORD-1",
            jobId = "JOB-1",
            componentType = JobCostComponentType.MATERIAL_COST,
            attributedAmount = BigDecimal("2000.0000"),
            sourceEntityId = "JOB-1"
        )
        val cost2 = CustomerCostAttribution(
            costAttributionId = "COST-2",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            orderId = "ORD-1",
            jobId = "JOB-1",
            componentType = JobCostComponentType.LABOUR_COST,
            attributedAmount = BigDecimal("1000.0000"),
            sourceEntityId = "JOB-1"
        )

        val res = collector.collectCustomerData(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customRevenue = listOf(rev1, rev2),
            customCosts = listOf(cost1, cost2)
        )

        assertTrue(res is DomainResult.Success)
        val data = (res as DomainResult.Success).data

        // Dedup should keep 1 revenue of 5000
        assertEquals(BigDecimal("5000.0000"), data.totalRevenue)
        assertEquals(1, data.revenueAttributions.size)

        // Costs should be 2000 + 1000 = 3000
        assertEquals(BigDecimal("3000.0000"), data.totalCost)
        assertEquals(2, data.costAttributions.size)

        // Source integrity status should detect duplicate
        assertEquals(ProductSourceIntegrityStatus.DUPLICATE_DETECTED, data.sourceIntegrity)
    }

    @Test
    fun testUnattributedDetection() = runBlocking {
        val tenantId = "tenant-001"
        val projectId = "proj-001"
        val customerId = "cust-100"

        // Revenue for different customer
        val unattrRev = CustomerRevenueAttribution(
            revenueAttributionId = "REV-OTHER",
            tenantId = tenantId,
            projectId = projectId,
            customerId = "cust-999",
            recognizedRevenue = BigDecimal("7000.0000"),
            sourceEntityId = "INV-999"
        )

        val res = collector.collectCustomerData(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customRevenue = listOf(unattrRev),
            customCosts = emptyList()
        )

        assertTrue(res is DomainResult.Success)
        val data = (res as DomainResult.Success).data
        assertEquals(1, data.unattributedItems.size)
        assertEquals("UNATTRIBUTED_REVENUE", data.unattributedItems.first().itemType)
        assertEquals(BigDecimal("7000.0000"), data.unattributedItems.first().amount)
    }
}
