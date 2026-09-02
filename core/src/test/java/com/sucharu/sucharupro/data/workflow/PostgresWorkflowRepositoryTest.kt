package com.sucharu.sucharupro.data.workflow

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowApprovalRepository
import com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowDefinitionRepository
import com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowInstanceRepository
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecision
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecisionType
import com.sucharu.sucharupro.domain.workflow.model.ApprovalPolicy
import com.sucharu.sucharupro.domain.workflow.model.ApprovalRequest
import com.sucharu.sucharupro.domain.workflow.model.ApprovalStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PostgresWorkflowRepositoryTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var defRepo: PostgresWorkflowDefinitionRepository
    private lateinit var instanceRepo: PostgresWorkflowInstanceRepository
    private lateinit var approvalRepo: PostgresWorkflowApprovalRepository

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        defRepo = PostgresWorkflowDefinitionRepository(mockDb)
        instanceRepo = PostgresWorkflowInstanceRepository(mockDb)
        approvalRepo = PostgresWorkflowApprovalRepository(mockDb)
    }

    @Test
    fun testTenantIsolationOnWorkflowInstances() {
        runBlocking {
            val tenantAlpha = TenantContext("tenant_alpha")
            val tenantBeta = TenantContext("tenant_beta")

            val instanceA = WorkflowInstance(
                workflowId = "wf-alpha-1",
                projectId = "tenant_alpha",
                definitionId = "def-order",
                versionId = "v1",
                actorId = "user-a"
            )

            instanceRepo.createInstance(instanceA, tenantAlpha)

            // Tenant Alpha can view
            val fetchedAlpha = instanceRepo.getInstanceById("wf-alpha-1", tenantAlpha)
            assertNotNull(fetchedAlpha)
            assertEquals("wf-alpha-1", fetchedAlpha?.workflowId)

            // Tenant Beta cannot view Tenant Alpha's workflow
            val fetchedBeta = instanceRepo.getInstanceById("wf-alpha-1", tenantBeta)
            assertNull(fetchedBeta)
        }
    }

    @Test
    fun testIdempotentInstanceCreationSuppressesDuplicate() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val instance = WorkflowInstance(
                workflowId = "wf-idempotent-1",
                projectId = "tenant_alpha",
                definitionId = "def-order",
                versionId = "v1",
                actorId = "user-1",
                idempotencyKey = "key-order-100"
            )

            val firstInsert = instanceRepo.createInstance(instance, tenant)
            assertTrue(firstInsert)

            val secondInsert = instanceRepo.createInstance(instance, tenant)
            assertFalse(secondInsert)
        }
    }

    @Test
    fun testApprovalPolicyAndDecisionPersistence() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")

            val policy = ApprovalPolicy(
                policyId = "POL-QC-1",
                projectId = "tenant_alpha",
                policyName = "QC Approval Policy",
                requiredRole = UserRole.MANAGER
            )
            approvalRepo.savePolicy(policy, tenant)

            val fetchedPolicy = approvalRepo.getPolicyById("POL-QC-1", tenant)
            assertNotNull(fetchedPolicy)
            assertEquals("QC Approval Policy", fetchedPolicy?.policyName)

            val request = ApprovalRequest(
                approvalId = "appr-qc-1",
                projectId = "tenant_alpha",
                workflowId = "wf-qc-1",
                stepId = "step-qc",
                policyId = "POL-QC-1",
                requesterId = "staff-1",
                requesterRole = UserRole.STAFF,
                title = "QC Deviation Approval"
            )
            approvalRepo.createApprovalRequest(request, tenant)

            val pendingList = approvalRepo.listPendingApprovals(10, tenant)
            assertEquals(1, pendingList.size)
            assertEquals("appr-qc-1", pendingList[0].approvalId)

            val decision = ApprovalDecision(
                decisionId = "dec-1",
                projectId = "tenant_alpha",
                approvalId = "appr-qc-1",
                approverId = "manager-1",
                approverRole = UserRole.MANAGER,
                decisionType = ApprovalDecisionType.APPROVE,
                notes = "Approved QC deviation"
            )
            approvalRepo.recordDecision(decision, tenant)

            val decisions = approvalRepo.getDecisionsForApproval("appr-qc-1", tenant)
            assertEquals(1, decisions.size)
            assertEquals(ApprovalDecisionType.APPROVE, decisions[0].decisionType)
            assertEquals("manager-1", decisions[0].approverId)
        }
    }
}
