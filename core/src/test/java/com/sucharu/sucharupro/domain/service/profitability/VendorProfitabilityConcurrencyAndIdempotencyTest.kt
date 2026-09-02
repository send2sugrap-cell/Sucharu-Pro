package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeVendorProfitabilityDataSource
import com.sucharu.sucharupro.data.repository.profitability.VendorProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.VendorCostAttribution
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorProfitabilityConcurrencyAndIdempotencyTest {

    private val fakeDs = FakeVendorProfitabilityDataSource()
    private val repo = VendorProfitabilityRepositoryImpl(fakeDs)
    private val service = VendorProfitabilityServiceImpl(repo)

    @Test
    fun testIdempotencyReplayReturnsIdenticalSnapshot() = runBlocking {
        val cost = VendorCostAttribution(
            costAttributionId = "COST-IDEM",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            attributedAmount = BigDecimal("50000.0000"),
            sourceEntityId = "WO-1"
        )

        val res1 = service.calculateVendorProfitability(
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            customCosts = listOf(cost),
            idempotencyKey = "IDEM-KEY-100"
        )
        assertTrue(res1 is DomainResult.Success)
        val snap1 = (res1 as DomainResult.Success).data

        val res2 = service.calculateVendorProfitability(
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            customCosts = listOf(cost),
            idempotencyKey = "IDEM-KEY-100"
        )
        assertTrue(res2 is DomainResult.Success)
        val snap2 = (res2 as DomainResult.Success).data

        assertEquals(snap1.snapshotId, snap2.snapshotId)
        assertEquals(snap1.integrityHash, snap2.integrityHash)
    }

    @Test
    fun testConcurrentCalculations() = runBlocking {
        val cost = VendorCostAttribution(
            costAttributionId = "COST-CONC",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            vendorId = "VEND-1",
            attributedAmount = BigDecimal("45000.0000"),
            sourceEntityId = "WO-CONC"
        )

        val deferreds = (1..5).map { idx ->
            async {
                service.calculateVendorProfitability(
                    tenantId = "TENANT-1",
                    projectId = "PROJ-1",
                    vendorId = "VEND-$idx",
                    customCosts = listOf(cost.copy(vendorId = "VEND-$idx"))
                )
            }
        }

        val results = deferreds.awaitAll()
        results.forEach { res ->
            assertTrue(res is DomainResult.Success)
        }
    }
}
