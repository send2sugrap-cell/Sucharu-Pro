package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class JobCostReconciliationTest {

    private lateinit var reconciliationService: JobCostReconciliationServiceImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"
    private val jobId = "JOB-101"

    @Before
    fun setUp() {
        reconciliationService = JobCostReconciliationServiceImpl()
    }

    @Test
    fun testPerfectJobCostReconciliation() = runBlocking {
        val p1 = JobCostProvenance("P1", tenantId, projectId, jobId, "MODULE_08", "STOCK_OUT", "SO-1", costComponentType = JobCostComponentType.MATERIAL_COST, attributedAmount = BigDecimal("6000.0000"), fingerprintHash = "FP1")
        val p2 = JobCostProvenance("P2", tenantId, projectId, jobId, "MODULE_04", "MACHINE_USAGE", "MACH-1", costComponentType = JobCostComponentType.MACHINE_COST, attributedAmount = BigDecimal("4000.0000"), fingerprintHash = "FP2")

        val c1 = JobCostComponent("C1", tenantId, projectId, jobId, JobCostComponentType.MATERIAL_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("6000.0000"))
        val c2 = JobCostComponent("C2", tenantId, projectId, jobId, JobCostComponentType.MACHINE_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("4000.0000"))

        val snapshot = JobCostSnapshot(
            snapshotId = "SNAP-1",
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            totalActualCost = BigDecimal("10000.0000"),
            totalDirectCost = BigDecimal("10000.0000"),
            totalIndirectCost = BigDecimal.ZERO,
            costComponents = listOf(c1, c2),
            provenances = listOf(p1, p2)
        )

        val res = reconciliationService.reconcileJobCostSnapshot(snapshot)
        assertTrue(res is DomainResult.Success)
        val event = (res as DomainResult.Success).data

        assertTrue(event.isReconciled)
        assertEquals(BigDecimal("0.0000"), event.componentDifference)
        assertEquals(BigDecimal("0.0000"), event.provenanceDifference)
        assertEquals(0, event.discrepancies.size)
    }

    @Test
    fun testDiscrepancyDetectionOnComponentMismatch() = runBlocking {
        val c1 = JobCostComponent("C1", tenantId, projectId, jobId, JobCostComponentType.MATERIAL_COST, CostDirectness.DIRECT, attributedAmount = BigDecimal("5000.0000")) // Sum is 5000

        val snapshot = JobCostSnapshot(
            snapshotId = "SNAP-2",
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            totalActualCost = BigDecimal("7000.0000"), // Snapshot says 7000 -> 2000 diff!
            totalDirectCost = BigDecimal("7000.0000"),
            totalIndirectCost = BigDecimal.ZERO,
            costComponents = listOf(c1),
            provenances = emptyList()
        )

        val res = reconciliationService.reconcileJobCostSnapshot(snapshot)
        assertTrue(res is DomainResult.Success)
        val event = (res as DomainResult.Success).data

        assertFalse(event.isReconciled)
        assertEquals(BigDecimal("2000.0000"), event.componentDifference)
        assertTrue(event.discrepancies.any { it.contains("differs from component sum") })
    }
}
