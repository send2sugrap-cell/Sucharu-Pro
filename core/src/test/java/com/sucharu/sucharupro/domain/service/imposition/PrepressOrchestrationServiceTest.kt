package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.data.datasource.imposition.FakePrepressOrchestrationDataSource
import com.sucharu.sucharupro.data.repository.imposition.PrepressOrchestrationRepositoryImpl
import com.sucharu.sucharupro.domain.model.imposition.PrepressPlanStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for Prepress Orchestration Service Layer.
 * Module 18 Step 06.
 */
class PrepressOrchestrationServiceTest {

    private val tenantId = "tenant_service_test_18"
    private lateinit var dataSource: FakePrepressOrchestrationDataSource
    private lateinit var repository: PrepressOrchestrationRepositoryImpl
    private lateinit var service: PrepressOrchestrationServiceImpl

    @Before
    fun setUp() {
        dataSource = FakePrepressOrchestrationDataSource()
        repository = PrepressOrchestrationRepositoryImpl(dataSource)
        service = PrepressOrchestrationServiceImpl(
            orchestrationRepository = repository
        )
    }

    @Test
    fun testOrchestrateAndSavePlan_Success() = runBlocking {
        val plan = service.orchestrateAndSavePlan(
            tenantId = tenantId,
            planName = "Commercial Brochure Plan",
            jobId = "JOB-BROCHURE-01",
            orderId = "ORD-BROCHURE-99",
            orderItemId = "ITEM-01",
            productName = "Commercial Brochure",
            requiredQuantity = 2000L,
            actor = "service_tester"
        )

        assertNotNull(plan)
        assertEquals(tenantId, plan.tenantId)
        assertEquals(2000L, plan.requiredQuantity)
        assertEquals("JOB-BROCHURE-01", plan.jobId)

        // Retrieve by ID
        val retrieved = service.getPlan(tenantId, plan.planId)
        assertNotNull(retrieved)
        assertEquals(plan.planId, retrieved?.planId)
    }

    @Test
    fun testUpdatePlanStatus_LifecycleTransition() = runBlocking {
        val plan = service.orchestrateAndSavePlan(
            tenantId = tenantId,
            planName = "Status Test Plan",
            jobId = "JOB-STATUS-01",
            orderId = "ORD-STATUS-99",
            orderItemId = "ITEM-01",
            productName = "Status Test Product",
            requiredQuantity = 1500L,
            actor = "service_tester"
        )

        // Update to APPROVED
        val approved = service.updatePlanStatus(
            tenantId = tenantId,
            planId = plan.planId,
            newStatus = PrepressPlanStatus.APPROVED,
            actor = "manager_alice",
            reason = "Ready for plate production"
        )

        assertEquals(PrepressPlanStatus.APPROVED, approved.status)
        assertEquals("APPROVED", approved.approvalStatus)
        assertEquals("manager_alice", approved.approvedBy)

        // Update to FINALIZED
        val finalized = service.updatePlanStatus(
            tenantId = tenantId,
            planId = plan.planId,
            newStatus = PrepressPlanStatus.FINALIZED,
            actor = "manager_alice",
            reason = "Locked for shop-floor"
        )

        assertEquals(PrepressPlanStatus.FINALIZED, finalized.status)
    }

    @Test
    fun testGetHandoffContract_EmitsValidContract() = runBlocking {
        val plan = service.orchestrateAndSavePlan(
            tenantId = tenantId,
            planName = "Handoff Contract Plan",
            jobId = "JOB-HANDOFF-01",
            orderId = "ORD-HANDOFF-99",
            orderItemId = "ITEM-01",
            productName = "Handoff Product",
            requiredQuantity = 1000L,
            actor = "service_tester"
        )

        val contract = service.getHandoffContract(tenantId, plan.planId)

        assertNotNull(contract)
        assertEquals("1.0.0", contract.contractVersion)
        assertEquals(plan.planId, contract.planId)
        assertEquals(tenantId, contract.tenantId)
        assertEquals(plan.masterIntegrityHash, contract.masterIntegrityHash)
    }

    @Test
    fun testListPlansByJobAndOrder() = runBlocking {
        service.orchestrateAndSavePlan(
            tenantId = tenantId,
            planName = "Job Order Plan 1",
            jobId = "JOB-MULTI-01",
            orderId = "ORD-MULTI-88",
            orderItemId = "ITEM-01",
            productName = "Multi Product 1",
            requiredQuantity = 500L,
            actor = "tester"
        )

        service.orchestrateAndSavePlan(
            tenantId = tenantId,
            planName = "Job Order Plan 2",
            jobId = "JOB-MULTI-01",
            orderId = "ORD-MULTI-99",
            orderItemId = "ITEM-01",
            productName = "Multi Product 2",
            requiredQuantity = 800L,
            actor = "tester"
        )

        val byJob = service.listPlansByJob(tenantId, "JOB-MULTI-01")
        assertEquals(2, byJob.size)

        val byOrder = service.listPlansByOrder(tenantId, "ORD-MULTI-88")
        assertEquals(1, byOrder.size)
    }
}
