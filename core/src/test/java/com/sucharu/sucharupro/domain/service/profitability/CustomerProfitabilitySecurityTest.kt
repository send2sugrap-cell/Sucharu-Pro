package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeCustomerProfitabilityDataSource
import com.sucharu.sucharupro.data.repository.profitability.CustomerProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.CustomerProfitabilityFilter
import com.sucharu.sucharupro.domain.model.profitability.CustomerProfitabilitySnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Multi-tenant Security and Isolation Test Suite for Customer Profitability (Module 16 Step 04).
 */
class CustomerProfitabilitySecurityTest {

    private val fakeDataSource = FakeCustomerProfitabilityDataSource()
    private val repository = CustomerProfitabilityRepositoryImpl(fakeDataSource)

    @Test
    fun testTenantIsolationOnSnapshots() = runBlocking {
        val tenantA = "tenant-A"
        val tenantB = "tenant-B"
        val projectIdA = "proj-A"
        val projectIdB = "proj-B"

        val snapA = CustomerProfitabilitySnapshot(
            snapshotId = "SNAP-A",
            tenantId = tenantA,
            projectId = projectIdA,
            customerId = "cust-1",
            recognizedRevenue = BigDecimal("1000.0000"),
            totalActualCost = BigDecimal("600.0000"),
            grossProfit = BigDecimal("400.0000"),
            integrityHash = "hash-A"
        )
        val snapB = CustomerProfitabilitySnapshot(
            snapshotId = "SNAP-B",
            tenantId = tenantB,
            projectId = projectIdB,
            customerId = "cust-1",
            recognizedRevenue = BigDecimal("5000.0000"),
            totalActualCost = BigDecimal("3000.0000"),
            grossProfit = BigDecimal("2000.0000"),
            integrityHash = "hash-B"
        )

        repository.saveSnapshot(snapA)
        repository.saveSnapshot(snapB)

        // Query Tenant A - must not see Tenant B
        val resA = repository.listSnapshots(tenantA, projectIdA, CustomerProfitabilityFilter())
        assertTrue(resA is DomainResult.Success)
        val listA = (resA as DomainResult.Success).data
        assertEquals(1, listA.size)
        assertEquals("SNAP-A", listA.first().snapshotId)
        assertEquals(BigDecimal("1000.0000"), listA.first().recognizedRevenue)

        // Query Tenant B - must not see Tenant A
        val resB = repository.listSnapshots(tenantB, projectIdB, CustomerProfitabilityFilter())
        assertTrue(resB is DomainResult.Success)
        val listB = (resB as DomainResult.Success).data
        assertEquals(1, listB.size)
        assertEquals("SNAP-B", listB.first().snapshotId)
        assertEquals(BigDecimal("5000.0000"), listB.first().recognizedRevenue)
    }
}
