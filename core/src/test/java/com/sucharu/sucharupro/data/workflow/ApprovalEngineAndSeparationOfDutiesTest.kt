package com.sucharu.sucharupro.data.workflow

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.workflow.approval.ApprovalEngine
import com.sucharu.sucharupro.domain.workflow.approval.ApprovalEvaluationResult
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecisionType
import com.sucharu.sucharupro.domain.workflow.model.ApprovalPolicy
import com.sucharu.sucharupro.domain.workflow.model.ApprovalRequest
import com.sucharu.sucharupro.domain.workflow.model.ApprovalStatus
import com.sucharu.sucharupro.domain.workflow.model.HumanConfirmationMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApprovalEngineAndSeparationOfDutiesTest {

    private lateinit var approvalEngine: ApprovalEngine

    @Before
    fun setUp() {
        approvalEngine = ApprovalEngine()
    }

    @Test
    fun testSeparationOfDutiesProhibitsSelfApproval() {
        val policy = ApprovalPolicy(
            policyId = "POL-FIN-1",
            projectId = "tenant_alpha",
            policyName = "Financial Payout Policy",
            requiredRole = UserRole.MANAGER,
            allowSelfApproval = false
        )

        val request = ApprovalRequest(
            approvalId = "appr-req-1",
            projectId = "tenant_alpha",
            workflowId = "wf-payout-1",
            stepId = "step-payout",
            policyId = "POL-FIN-1",
            requesterId = "user-alice",
            requesterRole = UserRole.MANAGER,
            title = "Payout $10,000"
        )

        val alicePrincipal = AuthenticatedPrincipal(
            userId = "user-alice",
            username = "alice_manager",
            role = UserRole.MANAGER,
            projectId = "tenant_alpha",
            principalType = PrincipalType.HUMAN
        )

        val result = approvalEngine.processDecision(
            request = request,
            policy = policy,
            principal = alicePrincipal,
            decisionType = ApprovalDecisionType.APPROVE
        )

        assertTrue(result is ApprovalEvaluationResult.Denied)
        val denied = result as ApprovalEvaluationResult.Denied
        assertTrue(denied.isSecurityViolation)
        assertTrue(denied.reason.contains("Separation of Duties violation"))
    }

    @Test
    fun testAiAgentIsStrictlyProhibitedFromApproving() {
        val policy = ApprovalPolicy(
            policyId = "POL-ORDER-1",
            projectId = "tenant_alpha",
            policyName = "Order Policy",
            requiredRole = UserRole.STAFF,
            allowSelfApproval = true
        )

        val request = ApprovalRequest(
            approvalId = "appr-req-2",
            projectId = "tenant_alpha",
            workflowId = "wf-order-2",
            stepId = "step-check",
            policyId = "POL-ORDER-1",
            requesterId = "user-bob",
            requesterRole = UserRole.CUSTOMER,
            title = "Approve Paper Quality"
        )

        val aiAgentPrincipal = AuthenticatedPrincipal(
            userId = "agent-sucharu",
            username = "sucharu_ai",
            role = UserRole.AI_AGENT,
            projectId = "tenant_alpha",
            principalType = PrincipalType.AI_AGENT
        )

        val result = approvalEngine.processDecision(
            request = request,
            policy = policy,
            principal = aiAgentPrincipal,
            decisionType = ApprovalDecisionType.APPROVE
        )

        assertTrue(result is ApprovalEvaluationResult.Denied)
        val denied = result as ApprovalEvaluationResult.Denied
        assertTrue(denied.isSecurityViolation)
        assertTrue(denied.reason.contains("AI_AGENT machine principals are strictly prohibited"))
    }

    @Test
    fun testIndependentManagerCanApproveAndFinalizeRequest() {
        val policy = ApprovalPolicy(
            policyId = "POL-FIN-1",
            projectId = "tenant_alpha",
            policyName = "Financial Payout Policy",
            requiredRole = UserRole.MANAGER,
            allowSelfApproval = false,
            minimumApprovals = 1
        )

        val request = ApprovalRequest(
            approvalId = "appr-req-3",
            projectId = "tenant_alpha",
            workflowId = "wf-payout-2",
            stepId = "step-payout",
            policyId = "POL-FIN-1",
            requesterId = "user-alice",
            requesterRole = UserRole.STAFF,
            title = "Payout $5,000"
        )

        val managerBobPrincipal = AuthenticatedPrincipal(
            userId = "user-bob",
            username = "bob_manager",
            role = UserRole.MANAGER,
            projectId = "tenant_alpha",
            principalType = PrincipalType.HUMAN
        )

        val result = approvalEngine.processDecision(
            request = request,
            policy = policy,
            principal = managerBobPrincipal,
            decisionType = ApprovalDecisionType.APPROVE,
            notes = "Verified invoice matches printing spec"
        )

        assertTrue(result is ApprovalEvaluationResult.Decided)
        val decided = result as ApprovalEvaluationResult.Decided
        assertEquals(ApprovalStatus.APPROVED, decided.updatedRequest.status)
        assertEquals(ApprovalDecisionType.APPROVE, decided.decision.decisionType)
        assertEquals("user-bob", decided.decision.approverId)
    }

    @Test
    fun testRejectionMarksApprovalRequestRejected() {
        val policy = ApprovalPolicy(
            policyId = "POL-DISCOUNT-1",
            projectId = "tenant_alpha",
            policyName = "Discount Policy",
            requiredRole = UserRole.MANAGER
        )

        val request = ApprovalRequest(
            approvalId = "appr-req-4",
            projectId = "tenant_alpha",
            workflowId = "wf-discount-1",
            stepId = "step-discount",
            policyId = "POL-DISCOUNT-1",
            requesterId = "user-charlie",
            requesterRole = UserRole.CUSTOMER,
            title = "50% Discount Request"
        )

        val managerPrincipal = AuthenticatedPrincipal(
            userId = "user-manager",
            username = "manager_dave",
            role = UserRole.MANAGER,
            projectId = "tenant_alpha",
            principalType = PrincipalType.HUMAN
        )

        val result = approvalEngine.processDecision(
            request = request,
            policy = policy,
            principal = managerPrincipal,
            decisionType = ApprovalDecisionType.REJECT,
            notes = "Margin is below minimum policy"
        )

        assertTrue(result is ApprovalEvaluationResult.Rejected)
        val rejected = result as ApprovalEvaluationResult.Rejected
        assertEquals(ApprovalStatus.REJECTED, rejected.updatedRequest.status)
    }

    @Test
    fun testEscalationRoutesToHigherRole() {
        val policy = ApprovalPolicy(
            policyId = "POL-REFUND-1",
            projectId = "tenant_alpha",
            policyName = "Refund Policy",
            requiredRole = UserRole.STAFF,
            escalationRole = UserRole.ADMIN
        )

        val request = ApprovalRequest(
            approvalId = "appr-req-5",
            projectId = "tenant_alpha",
            workflowId = "wf-ref-1",
            stepId = "step-refund",
            policyId = "POL-REFUND-1",
            requesterId = "user-staff",
            requesterRole = UserRole.STAFF,
            title = "Refund > $20,000"
        )

        val staffPrincipal = AuthenticatedPrincipal(
            userId = "user-staff-2",
            username = "staff_eva",
            role = UserRole.STAFF,
            projectId = "tenant_alpha",
            principalType = PrincipalType.HUMAN
        )

        val result = approvalEngine.processDecision(
            request = request,
            policy = policy,
            principal = staffPrincipal,
            decisionType = ApprovalDecisionType.ESCALATE,
            notes = "Exceeds staff refund threshold"
        )

        assertTrue(result is ApprovalEvaluationResult.Escalated)
        val escalated = result as ApprovalEvaluationResult.Escalated
        assertEquals(UserRole.ADMIN, escalated.escalation.toRole)
        assertEquals(UserRole.STAFF, escalated.escalation.fromRole)
    }
}
