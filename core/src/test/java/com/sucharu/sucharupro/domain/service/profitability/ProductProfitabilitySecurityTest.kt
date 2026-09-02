package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeProductProfitabilityDataSource
import com.sucharu.sucharupro.data.repository.profitability.ProductProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.ProductProfitabilityFilter
import com.sucharu.sucharupro.domain.model.profitability.ProductProfitabilitySnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductProfitabilitySecurityTest {

    private lateinit var dataSource: FakeProductProfitabilityDataSource
    private lateinit var repository: ProductProfitabilityRepositoryImpl
    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val projectA = "PROJ-A"
    private val projectB = "PROJ-B"
    private val productA = "PROD-A1"

    @Before
    fun setUp() {
        runBlocking {
            dataSource = FakeProductProfitabilityDataSource()
            repository = ProductProfitabilityRepositoryImpl(dataSource)

            // Seed snapshot for Tenant A
            repository.saveSnapshot(
                ProductProfitabilitySnapshot(
                    snapshotId = "SNAP-TENANT-A",
                    tenantId = tenantA,
                    projectId = projectA,
                    productId = productA,
                    totalActualCost = BigDecimal("12500.0000"),
                    recognizedRevenue = BigDecimal("20000.0000")
                )
            )
        }
    }

    @Test
    fun testTenantIsolationOnRead() = runBlocking {
        // Tenant A can read its own snapshot
        val resA = repository.getSnapshotById(tenantA, projectA, "SNAP-TENANT-A")
        assertTrue(resA is DomainResult.Success)

        // Tenant B cannot read Tenant A's snapshot
        val resB = repository.getSnapshotById(tenantB, projectB, "SNAP-TENANT-A")
        assertTrue(resB is DomainResult.Error)
        assertTrue((resB as DomainResult.Error).message.contains("not found"))
    }

    @Test
    fun testTenantIsolationOnList() = runBlocking {
        val listA = repository.listSnapshots(tenantA, projectA, ProductProfitabilityFilter())
        assertTrue(listA is DomainResult.Success)
        assertEquals(1, (listA as DomainResult.Success).data.size)

        val listB = repository.listSnapshots(tenantB, projectB, ProductProfitabilityFilter())
        assertTrue(listB is DomainResult.Success)
        assertEquals(0, (listB as DomainResult.Success).data.size)
    }
}
