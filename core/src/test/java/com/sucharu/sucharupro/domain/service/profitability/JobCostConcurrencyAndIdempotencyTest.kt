package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeJobCostDataSource
import com.sucharu.sucharupro.data.repository.profitability.JobCostRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class JobCostConcurrencyAndIdempotencyTest {

    private lateinit var service: JobCostCalculationServiceImpl
    private lateinit var repository: JobCostRepositoryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"
    private val jobId = "JOB-777"

    @Before
    fun setUp() {
        val dataSource = FakeJobCostDataSource()
        repository = JobCostRepositoryImpl(dataSource)
        val collector = JobCostSourceCollectorImpl()
        val reconService = JobCostReconciliationServiceImpl()

        service = JobCostCalculationServiceImpl(
            repository = repository,
            sourceCollector = collector,
            reconciliationService = reconService
        )
    }

    @Test
    fun testIdempotencyKeyReturnsExistingSnapshot() = runBlocking {
        val key = "IDEM-JOB-001"

        val c1 = JobCostComponent("C1", tenantId, projectId, jobId, JobCostComponentType.MATERIAL_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("5000.0000"))

        val res1 = service.calculateJobActualCost(
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            customDirectCosts = listOf(c1),
            idempotencyKey = key,
            actor = "USER-1"
        )
        assertTrue(res1 is DomainResult.Success)
        val snap1 = (res1 as DomainResult.Success).data

        // Execute again with same idempotency key but different payload
        val c2 = JobCostComponent("C2", tenantId, projectId, jobId, JobCostComponentType.MATERIAL_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("99999.0000"))

        val res2 = service.calculateJobActualCost(
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            customDirectCosts = listOf(c2),
            idempotencyKey = key,
            actor = "USER-1"
        )
        assertTrue(res2 is DomainResult.Success)
        val snap2 = (res2 as DomainResult.Success).data

        assertEquals(snap1.snapshotId, snap2.snapshotId)
        assertEquals(BigDecimal("5000.0000"), snap2.totalActualCost) // Remains original
    }

    @Test
    fun testConcurrentJobCostCalculationsSafety() = runBlocking {
        val jobs = mutableListOf<Deferred<DomainResult<JobCostSnapshot>>>()

        coroutineScope {
            for (i in 1..20) {
                val targetJob = "JOB-CONCURRENT-$i"
                val comp = JobCostComponent("C-$i", tenantId, projectId, targetJob, JobCostComponentType.MATERIAL_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("${100 * i}.0000"))
                jobs.add(
                    async(Dispatchers.Default) {
                        service.calculateJobActualCost(
                            tenantId = tenantId,
                            projectId = projectId,
                            jobId = targetJob,
                            customDirectCosts = listOf(comp),
                            actor = "CONCURRENT-WORKER-$i"
                        )
                    }
                )
            }
        }

        val results = jobs.awaitAll()
        assertEquals(20, results.size)
        assertTrue(results.all { it is DomainResult.Success })

        val listRes = repository.listSnapshots(tenantId, projectId, limit = 50)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(20, (listRes as DomainResult.Success).data.size)
    }
}
