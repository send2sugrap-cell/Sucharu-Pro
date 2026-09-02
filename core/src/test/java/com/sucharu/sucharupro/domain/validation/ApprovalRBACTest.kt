package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC tests for Approval Workflow (Module 05 Step 04).
 */
class ApprovalRBACTest {

    @Test
    fun approvalRequest_authorizedRoles_pass() {
        val authorized = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.DESIGNER)
        authorized.forEach { role ->
            val result = DesignApprovalValidator.validateApprovalRequestPermission(role)
            assertTrue("Role ${role.name} should be allowed to submit approval requests", result is DomainResult.Success)
        }
    }

    @Test
    fun approvalDecision_approverRoles_pass() {
        val authorized = listOf(UserRole.ADMIN, UserRole.MANAGER)
        authorized.forEach { role ->
            val result = DesignApprovalValidator.validateApprovalDecisionPermission(role)
            assertTrue("Role ${role.name} should be allowed to approve/reject", result is DomainResult.Success)
        }
    }

    @Test
    fun approvalDecision_designerCannotApprove() {
        val result = DesignApprovalValidator.validateApprovalDecisionPermission(UserRole.DESIGNER)
        assertTrue("Designer should not be allowed to approve", result is DomainResult.Error)
    }

    @Test
    fun approvalDecision_restrictedRoles_fail() {
        val restricted = listOf(
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.WAREHOUSE,
            UserRole.QC_INSPECTOR,
            UserRole.ACCOUNTS,
            UserRole.STAFF
        )
        restricted.forEach { role ->
            val result = DesignApprovalValidator.validateApprovalDecisionPermission(role)
            assertTrue("Role ${role.name} should be restricted from approval decisions", result is DomainResult.Error)
        }
    }
}
