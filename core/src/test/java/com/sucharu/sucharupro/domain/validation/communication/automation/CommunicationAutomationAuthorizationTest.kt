package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationAutomationAuthorizationTest {

    @Test
    fun ruleManagement_adminAndManager_succeeds() {
        assertTrue(CommunicationAutomationAuthorizationValidator.validateRuleManagement(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(CommunicationAutomationAuthorizationValidator.validateRuleManagement(UserRole.MANAGER) is DomainResult.Success)
    }

    @Test
    fun ruleManagement_externalAndStaff_fails() {
        listOf(UserRole.CUSTOMER, UserRole.VENDOR, UserRole.STAFF, UserRole.AFFILIATE).forEach { role ->
            val result = CommunicationAutomationAuthorizationValidator.validateRuleManagement(role)
            assertTrue("$role must not manage automation rules", result is DomainResult.Error)
        }
    }

    @Test
    fun ruleApproval_separationOfDuties_creatorCannotApproveUnlessAdmin() {
        // Manager cannot approve own rule
        val managerSelf = CommunicationAutomationAuthorizationValidator.validateRuleApproval(
            callerRole = UserRole.MANAGER,
            creatorUserId = "user-manager-01",
            approverUserId = "user-manager-01"
        )
        assertTrue(managerSelf is DomainResult.Error)

        // Manager can approve someone else's rule
        val managerOther = CommunicationAutomationAuthorizationValidator.validateRuleApproval(
            callerRole = UserRole.MANAGER,
            creatorUserId = "user-staff-01",
            approverUserId = "user-manager-01"
        )
        assertTrue(managerOther is DomainResult.Success)

        // Admin can approve own rule (executive override)
        val adminSelf = CommunicationAutomationAuthorizationValidator.validateRuleApproval(
            callerRole = UserRole.ADMIN,
            creatorUserId = "user-admin-01",
            approverUserId = "user-admin-01"
        )
        assertTrue(adminSelf is DomainResult.Success)
    }
}
