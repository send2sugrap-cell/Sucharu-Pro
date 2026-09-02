package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class JobCostProvenanceAndDeduplicationTest {

    private lateinit var collector: JobCostSourceCollectorImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"
    private val jobId = "JOB-500"

    @Before
    fun setUp() {
        collector = JobCostSourceCollectorImpl()
    }

    @Test
    fun testCollectCostsWithValidComponents() = runBlocking {
        val p1 = JobCostProvenance(
            provenanceId = "P1",
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            sourceModule = "MODULE_08",
            sourceEntityType = "STOCK_OUT",
            sourceEntityId = "SO-001",
            costComponentType = JobCostComponentType.MATERIAL_COST,
            attributedAmount = BigDecimal("5000.0000")
        )

        val c1 = JobCostComponent(
            componentId = "C1",
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            componentType = JobCostComponentType.MATERIAL_COST,
            directness = CostDirectness.DIRECT,
            attributedAmount = BigDecimal("5000.0000"),
            provenances = listOf(p1)
        )

        val alloc1 = JobCostAllocationDetail(
            allocationId = "AL-1",
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            costPoolName = "Production Facility Overhead",
            allocationBasis = AllocationBasisType.MACHINE_HOURS,
            allocatedAmount = BigDecimal("1200.0000")
        )

        val res = collector.collectJobCosts(
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            customDirectCosts = listOf(c1),
            customIndirectCosts = listOf(alloc1)
        )

        assertTrue(res is DomainResult.Success)
        val data = (res as DomainResult.Success).data
        assertEquals(2, data.components.size)
        assertEquals(2, data.provenances.size)
        assertEquals(0, data.duplicateCount)
        assertEquals(JobCostReadinessStatus.COMPLETE, data.readinessStatus)
    }

    @Test
    fun testDuplicateFingerprintDetection() = runBlocking {
        val p1 = JobCostProvenance(
            provenanceId = "P1",
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            sourceModule = "MODULE_12",
            sourceEntityType = "WORK_ORDER",
            sourceEntityId = "WO-001",
            costComponentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
            attributedAmount = BigDecimal("3000.0000"),
            fingerprintHash = "MODULE_12:WORK_ORDER:WO-001::VENDOR_OUTSOURCE_COST"
        )

        val p2 = JobCostProvenance(
            provenanceId = "P2",
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            sourceModule = "MODULE_12",
            sourceEntityType = "WORK_ORDER",
            sourceEntityId = "WO-001", // Duplicate!
            costComponentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
            attributedAmount = BigDecimal("3000.0000"),
            fingerprintHash = "MODULE_12:WORK_ORDER:WO-001::VENDOR_OUTSOURCE_COST"
        )

        val c1 = JobCostComponent("C1", tenantId, projectId, jobId, JobCostComponentType.VENDOR_OUTSOURCE_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("3000.0000"), provenances = listOf(p1))
        val c2 = JobCostComponent("C2", tenantId, projectId, jobId, JobCostComponentType.VENDOR_OUTSOURCE_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("3000.0000"), provenances = listOf(p2))

        val res = collector.collectJobCosts(
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            customDirectCosts = listOf(c1, c2)
        )

        assertTrue(res is DomainResult.Success)
        val data = (res as DomainResult.Success).data
        assertTrue(data.duplicateCount > 0)
        assertEquals(JobCostReadinessStatus.CONFLICTED, data.readinessStatus)
        assertTrue(data.warnings.any { it.contains("Duplicate cost source fingerprint detected") })
    }

    @Test
    fun testUnallocatedOverheadStatusWhenIndirectCostsMissing() = runBlocking {
        val p1 = JobCostProvenance(
            provenanceId = "P1",
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            sourceModule = "MODULE_04",
            sourceEntityType = "STAGE_EXECUTION",
            sourceEntityId = "EXEC-01",
            costComponentType = JobCostComponentType.LABOUR_COST,
            attributedAmount = BigDecimal("2000.0000")
        )

        val c1 = JobCostComponent(
            componentId = "C1",
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            componentType = JobCostComponentType.LABOUR_COST,
            directness = CostDirectness.DIRECT,
            attributedAmount = BigDecimal("2000.0000"),
            provenances = listOf(p1)
        )

        val res = collector.collectJobCosts(
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            customDirectCosts = listOf(c1),
            customIndirectCosts = null // missing indirect allocations
        )

        assertTrue(res is DomainResult.Success)
        val data = (res as DomainResult.Success).data
        assertEquals(JobCostReadinessStatus.UNALLOCATED, data.readinessStatus)
    }
}
