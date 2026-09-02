package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeJobCostDataSource
import com.sucharu.sucharupro.data.repository.profitability.JobCostRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.JobCostSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class JobCostSecurityTest {

    private lateinit var dataSource: FakeJobCostDataSource
    private lateinit var repository: JobCostRepositoryImpl
    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val projectA = "PROJ-A"
    private val projectB = "PROJ-B"
    private val jobA = "JOB-A1"

    @Before
    fun setUp() {
        runBlocking {
            dataSource = FakeJobCostDataSource()
            repository = JobCostRepositoryImpl(dataSource)

            // Seed snapshot for Tenant A
            repository.saveSnapshot(
                JobCostSnapshot(
                    snapshotId = "SNAP-TENANT-A",
                    tenantId = tenantA,
                    projectId = projectA,
                    jobId = jobA,
                    totalActualCost = BigDecimal("8500.0000")
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
    fun testCrossJobIsolation() = runBlocking {
        val resOtherJob = repository.getLatestSnapshotByJobId(tenantA, projectA, "JOB-OTHER")
        assertTrue(resOtherJob is DomainResult.Error)
    }
}
