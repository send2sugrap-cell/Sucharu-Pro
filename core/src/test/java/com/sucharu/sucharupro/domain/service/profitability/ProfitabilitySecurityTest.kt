package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeProfitabilityDataSource
import com.sucharu.sucharupro.data.repository.profitability.ProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProfitabilitySecurityTest {

    private lateinit var dataSource: FakeProfitabilityDataSource
    private lateinit var repository: ProfitabilityRepositoryImpl
    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val projectA = "PROJ-A"
    private val projectB = "PROJ-B"

    @Before
    fun setUp() {
        runBlocking {
            dataSource = FakeProfitabilityDataSource()
            repository = ProfitabilityRepositoryImpl(dataSource)

            // Seed snapshot for Tenant A
            repository.saveSnapshot(
                ProfitabilitySnapshot(
                    id = "SNAP-TENANT-A",
                    tenantId = tenantA,
                    projectId = projectA,
                    scope = ProfitabilityScope.JOB,
                    targetEntityId = "JOB-A1",
                    metrics = ProfitabilityMetric(revenue = BigDecimal("10000.0000")),
                    generatedBy = "USER-A"
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
        val listA = repository.listSnapshots(tenantA, projectA)
        assertTrue(listA is DomainResult.Success)
        assertEquals(1, (listA as DomainResult.Success).data.size)

        val listB = repository.listSnapshots(tenantB, projectB)
        assertTrue(listB is DomainResult.Success)
        assertEquals(0, (listB as DomainResult.Success).data.size)
    }

    @Test
    fun testProjectIsolationOnList() = runBlocking {
        val listAnotherProj = repository.listSnapshots(tenantA, "PROJ-OTHER")
        assertTrue(listAnotherProj is DomainResult.Success)
        assertEquals(0, (listAnotherProj as DomainResult.Success).data.size)
    }
}
