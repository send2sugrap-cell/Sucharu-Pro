package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReleaseGovernanceDataSource
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReleaseGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.substratereservation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Service lifecycle and workflow test suite for SubstrateReleaseGovernanceService.
 * Module 19 Step 05.
 */
class SubstrateReleaseGovernanceServiceTest {

    private lateinit var dataSource: FakeSubstrateReleaseGovernanceDataSource
    private lateinit var repository: SubstrateReleaseGovernanceRepositoryImpl
    private lateinit var service: SubstrateReleaseGovernanceServiceImpl

    @Before
    fun setup() {
        dataSource = FakeSubstrateReleaseGovernanceDataSource()
        repository = SubstrateReleaseGovernanceRepositoryImpl(dataSource)
        service = SubstrateReleaseGovernanceServiceImpl(repository)
    }

    private fun sampleInput(
        triggerType: GovernanceTriggerType = GovernanceTriggerType.JOB_CANCELLATION,
        allocatedSheets: Long = 8000L,
        consumedSheets: Long = 0L,
        committedSheets: Long = 0L
    ): SubstrateReleaseGovernanceEngine.EvaluationInput {
        return SubstrateReleaseGovernanceEngine.EvaluationInput(
            tenantId = "TENANT-001",
            reservationId = "RES-001",
            orderId = "ORD-001",
            orderItemId = "ITEM-01",
            executionJobId = "JOB-001",
            triggerType = triggerType,
            sku = "COATED-150",
            materialName = "Coated Paper 150 GSM",
            warehouseId = "WH-01",
            previousRequiredSheets = allocatedSheets,
            newRequiredSheets = 0L,
            allocatedSheets = allocatedSheets,
            consumedSheets = consumedSheets,
            committedSheets = committedSheets,
            productionStatus = "READY",
            evaluator = "supervisor_tester"
        )
    }

    @Test
    fun testFullGovernanceLifecycle_evaluate_approve_execute() = runBlocking {
        val input = sampleInput()

        // 1. Evaluate
        val evaluated = service.evaluateCancellation("TENANT-001", input)
        assertEquals(GovernanceExecutionStatus.EVALUATED, evaluated.executionStatus)
        assertEquals(ReleaseGovernanceDecision.RELEASE_ELIGIBLE, evaluated.decision)
        assertEquals(8000L, evaluated.releasableSheets)

        // 2. Approve
        val approved = service.approveRelease("TENANT-001", evaluated.governanceId, "supervisor_bob", "Approved for cancellation")
        assertEquals(GovernanceExecutionStatus.APPROVED, approved.executionStatus)
        assertEquals("supervisor_bob", approved.approvedBy)
        assertNotNull(approved.approvedAt)

        // 3. Execute
        val executed = service.executeRelease("TENANT-001", approved.governanceId, "warehouse_operator")
        assertEquals(GovernanceExecutionStatus.RELEASE_EXECUTED, executed.executionStatus)
        assertEquals("warehouse_operator", executed.executedBy)
        assertNotNull(executed.executedAt)

        // Verify audit trail
        val audits = repository.listAuditEvents("TENANT-001", evaluated.governanceId)
        assertEquals(3, audits.size)
        assertTrue(audits.any { it.action == "EVALUATE_CANCELLATION" })
        assertTrue(audits.any { it.action == "APPROVE_RELEASE" })
        assertTrue(audits.any { it.action == "EXECUTE_RELEASE" })
    }

    @Test
    fun testRejectionLifecycle() = runBlocking {
        val input = sampleInput()
        val evaluated = service.evaluateCancellation("TENANT-001", input)

        val rejected = service.rejectRelease("TENANT-001", evaluated.governanceId, "manager_alice", "Production resumption requested by client")
        assertEquals(GovernanceExecutionStatus.REJECTED, rejected.executionStatus)

        // Attempting to approve rejected case must fail
        try {
            service.approveRelease("TENANT-001", rejected.governanceId, "manager_alice")
            fail("Expected IllegalStateException when approving rejected record")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Only cases in EVALUATED status can be approved"))
        }
    }

    @Test
    fun testIdempotencyOfEvaluation() = runBlocking {
        val input = sampleInput()

        val eval1 = service.evaluateCancellation("TENANT-001", input)
        val eval2 = service.evaluateCancellation("TENANT-001", input)

        // Must return identical record without creating duplicate
        assertEquals(eval1.governanceId, eval2.governanceId)
        assertEquals(eval1.deduplicationFingerprint, eval2.deduplicationFingerprint)

        val list = service.listGovernanceRecords("TENANT-001")
        assertEquals(1, list.size)
    }

    @Test
    fun testExportHandoffContract() = runBlocking {
        val input = sampleInput()
        val evaluated = service.evaluateCancellation("TENANT-001", input)

        val handoff = service.exportHandoffContract("TENANT-001", evaluated.governanceId)
        assertEquals("5.0.0", handoff.contractVersion)
        assertEquals(evaluated.governanceId, handoff.governanceId)
        assertEquals(evaluated.tenantId, handoff.tenantId)
        assertEquals(evaluated.decision.name, handoff.decision)
        assertEquals(evaluated.releasableSheets, handoff.releasableSheets)
    }
}
