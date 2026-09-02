package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryItemVerificationAuthorizationTest {

    @Test
    fun `ADMIN and MANAGER have full management authority`() {
        for (role in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
            for (op in DeliveryItemVerificationOperation.values()) {
                val result = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = op,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                )
                assertTrue("Role $role should be authorized for $op", result is DomainResult.Success)
            }
        }
    }

    @Test
    fun `WAREHOUSE can CREATE, START, VERIFY_LINE, COMPLETE, and VIEW, but not CLOSE or CANCEL`() {
        assertTrue(
            DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryItemVerificationOperation.CREATE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryItemVerificationOperation.START_VERIFICATION,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryItemVerificationOperation.VERIFY_LINE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryItemVerificationOperation.COMPLETE_VERIFICATION,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )

        // Denied
        assertTrue(
            DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryItemVerificationOperation.CLOSE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
        assertTrue(
            DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryItemVerificationOperation.CANCEL,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
    }

    @Test
    fun `QC_INSPECTOR can VIEW and VERIFY_LINE only`() {
        assertTrue(
            DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = UserRole.QC_INSPECTOR,
                operation = DeliveryItemVerificationOperation.VIEW,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = UserRole.QC_INSPECTOR,
                operation = DeliveryItemVerificationOperation.VERIFY_LINE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = UserRole.QC_INSPECTOR,
                operation = DeliveryItemVerificationOperation.CLOSE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
    }

    @Test
    fun `Cross project operation is rejected`() {
        val result = DeliveryItemVerificationAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = DeliveryItemVerificationOperation.VIEW,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-02"
        )
        assertTrue(result is DomainResult.Error)
    }
}
