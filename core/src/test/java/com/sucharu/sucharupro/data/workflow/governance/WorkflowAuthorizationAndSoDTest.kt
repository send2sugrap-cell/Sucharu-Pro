package com.sucharu.sucharupro.data.workflow.governance

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.workflow.control.WorkflowControlPlaneService
import com.sucharu.sucharupro.data.workflow.postgres.*
import com.sucharu.sucharupro.domain.workflow.governance.CreateWorkflowDefinitionRequest
import com.sucharu.sucharupro.domain.workflow.governance.WorkflowApprovalDecisionDto
import com.sucharu.sucharupro.domain.workflow.governance.WorkflowFilterCriteria
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecisionType
import com.sucharu.sucharupro.domain.workflow.model.ApprovalPolicy
import com.sucharu.sucharupro.domain.workflow.model.ApprovalRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WorkflowAuthorizationAndSoDTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var controlPlaneService: WorkflowControlPlaneService

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "usr_cust_1",
        projectId = "PRJ-TEST",
        username = "cust_user",
        role = UserRole.CUSTOMER,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "usr_staff_1",
        projectId = "PRJ-TEST",
        username = "staff_user",
        role = UserRole.STAFF,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "usr_mgr_1",
        projectId = "PRJ-TEST",
        username = "mgr_user",
        role = UserRole.MANAGER,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "usr_admin_1",
        projectId = "PRJ-TEST",
        username = "admin_user",
        role = UserRole.ADMIN,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    private val aiAgentPrincipal = AuthenticatedPrincipal(
        userId = "agent_print_bot",
        projectId = "PRJ-TEST",
        username = "ai_agent",
        role = UserRole.AI_AGENT,
        principalType = PrincipalType.AI_AGENT,
        permissions = emptySet()
    )

    @Before
    fun setup() {
        mockDb = MockPostgresEventDatabase()
        controlPlaneService = WorkflowControlPlaneService(
            definitionRepository = PostgresWorkflowDefinitionRepository(mockDb),
            instanceRepository = PostgresWorkflowInstanceRepository(mockDb),
            stepExecutionRepository = PostgresWorkflowStepExecutionRepository(mockDb),
            compensationRepository = PostgresWorkflowCompensationRepository(mockDb),
            approvalRepository = PostgresWorkflowApprovalRepository(mockDb),
            idempotencyStore = PostgresWorkflowIdempotencyStore(mockDb)
        )
    }

    @Test
    fun testCustomerRoleIsStrictlyDeniedFromWorkflowControl() = runBlocking {
        try {
            controlPlaneService.getDefinitions(customerPrincipal)
            fail("Expected SecurityException for Customer accessing workflow definitions")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("lacks required capability"))
        }

        try {
            controlPlaneService.getInstances(WorkflowFilterCriteria(), customerPrincipal)
            fail("Expected SecurityException for Customer accessing workflow instances")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("lacks required capability"))
        }
    }

    @Test
    fun testStaffCanViewInstancesButCannotCreateDefinitions() = runBlocking {
        // Can view
        val instances = controlPlaneService.getInstances(WorkflowFilterCriteria(), staffPrincipal)
        assertNotNull(instances)

        // Cannot create definition
        try {
            controlPlaneService.createDefinition(
                CreateWorkflowDefinitionRequest(name = "Illegal", description = null, category = "TEST"),
                staffPrincipal
            )
            fail("Expected SecurityException for Staff creating workflow definition")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("lacks required capability"))
        }
    }

    @Test
    fun testManagerCannotPublishWorkflowVersion() = runBlocking {
        val def = controlPlaneService.createDefinition(
            CreateWorkflowDefinitionRequest(name = "Brochure Print", description = null, category = "PRINT"),
            managerPrincipal
        )

        try {
            controlPlaneService.publishVersion(def.definitionId, "v1", managerPrincipal)
            fail("Expected SecurityException for Manager publishing workflow version")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("lacks required capability"))
        }
    }

    @Test
    fun testAdminCanPublishWorkflowVersion() = runBlocking {
        val def = controlPlaneService.createDefinition(
            CreateWorkflowDefinitionRequest(name = "Brochure Print", description = null, category = "PRINT"),
            adminPrincipal
        )

        val published = controlPlaneService.publishVersion(def.definitionId, "v1", adminPrincipal)
        assertTrue(published.isActive)
        assertEquals("usr_admin_1", published.publishedBy)
    }

    @Test
    fun testAiAgentIsStrictlyProhibitedFromAdministrativeOperations() = runBlocking {
        try {
            controlPlaneService.createDefinition(
                CreateWorkflowDefinitionRequest(name = "AI Illegal", description = null, category = "AI"),
                aiAgentPrincipal
            )
            fail("Expected SecurityException for AI Agent creating definition")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("strictly prohibited") || e.message!!.contains("lacks required capability"))
        }
    }

    @Test
    fun testSeparationOfDutiesProhibitsSelfApproval() = runBlocking {
        val approvalRepo = PostgresWorkflowApprovalRepository(mockDb)
        val tenant = TenantContext("PRJ-TEST")

        val policy = ApprovalPolicy(
            policyId = "pol-strict-sod",
            projectId = "PRJ-TEST",
            policyName = "Strict SoD",
            requiredRole = UserRole.MANAGER,
            allowSelfApproval = false
        )
        approvalRepo.savePolicy(policy, tenant)

        val request = ApprovalRequest(
            approvalId = "appr-sod-01",
            workflowId = "wf-sod-01",
            stepId = "approval-step",
            projectId = "PRJ-TEST",
            policyId = "pol-strict-sod",
            requesterId = managerPrincipal.userId, // manager is the requester!
            requesterRole = UserRole.MANAGER,
            title = "Strict SoD Approval Request"
        )
        approvalRepo.createApprovalRequest(request, tenant)

        try {
            controlPlaneService.submitApprovalDecision(
                "appr-sod-01",
                WorkflowApprovalDecisionDto(decision = ApprovalDecisionType.APPROVE, notes = "Self approval"),
                managerPrincipal
            )
            fail("Expected SecurityException for Self-Approval attempt")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("Separation of Duties violation") || e.message!!.contains("denied"))
        }
    }
}
