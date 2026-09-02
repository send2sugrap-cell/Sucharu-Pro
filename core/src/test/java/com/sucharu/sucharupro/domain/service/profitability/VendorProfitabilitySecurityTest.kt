package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeVendorProfitabilityDataSource
import com.sucharu.sucharupro.data.repository.profitability.VendorProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.VendorCostAttribution
import com.sucharu.sucharupro.domain.model.profitability.VendorProfitabilityFilter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorProfitabilitySecurityTest {

    private val fakeDs = FakeVendorProfitabilityDataSource()
    private val repo = VendorProfitabilityRepositoryImpl(fakeDs)
    private val service = VendorProfitabilityServiceImpl(repo)

    @Test
    fun testTenantIsolationBetweenTenants() = runBlocking {
        // Tenant 1 calculation
        val costT1 = VendorCostAttribution(
            costAttributionId = "COST-T1",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            attributedAmount = BigDecimal("50000.0000"),
            sourceEntityId = "WO-1"
        )
        val resT1 = service.calculateVendorProfitability(
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            customCosts = listOf(costT1)
        )
        assertTrue(resT1 is DomainResult.Success)

        // Tenant 2 calculation
        val costT2 = VendorCostAttribution(
            costAttributionId = "COST-T2",
            tenantId = "TENANT-2",
            projectId = "PROJ-2",
            vendorId = "VEND-2",
            attributedAmount = BigDecimal("80000.0000"),
            sourceEntityId = "WO-2"
        )
        val resT2 = service.calculateVendorProfitability(
            tenantId = "TENANT-2",
            projectId = "PROJ-2",
            vendorId = "VEND-2",
            customCosts = listOf(costT2)
        )
        assertTrue(resT2 is DomainResult.Success)

        // Tenant 1 cannot see Tenant 2 snapshots
        val t1Snaps = service.listSnapshots("TENANT-1", VendorProfitabilityFilter())
        assertTrue(t1Snaps is DomainResult.Success)
        val t1List = (t1Snaps as DomainResult.Success).data
        assertEquals(1, t1List.size)
        assertEquals("VEND-1", t1List.first().vendorId)
        assertFalse(t1List.any { it.vendorId == "VEND-2" })

        // Tenant 2 cannot see Tenant 1 latest snapshot
        val t2Latest = service.getLatestSnapshot("TENANT-2", "VEND-1")
        assertTrue(t2Latest is DomainResult.Success)
        assertNull((t2Latest as DomainResult.Success).data)
    }
}
