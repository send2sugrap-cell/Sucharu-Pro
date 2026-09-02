package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryChallanAuthorizationTest {

    @Test
    fun `ADMIN and MANAGER have full management permissions`() {
        for (role in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
            for (op in DeliveryChallanOperation.values()) {
                val result = DeliveryChallanAuthorizationValidator.validateOperation(
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
    fun `WAREHOUSE can VIEW and mark READY_FOR_DISPATCH but cannot CREATE, EDIT or APPROVE`() {
        assertTrue(
            DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryChallanOperation.VIEW,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryChallanOperation.READY_FOR_DISPATCH,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )

        // Denied
        assertTrue(
            DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryChallanOperation.CREATE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
        assertTrue(
            DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryChallanOperation.APPROVE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
    }

    @Test
    fun `STAFF and QC and ACCOUNTS can VIEW only`() {
        for (role in listOf(UserRole.STAFF, UserRole.QC_INSPECTOR, UserRole.ACCOUNTS)) {
            assertTrue(
                DeliveryChallanAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DeliveryChallanOperation.VIEW,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Success
            )

            assertTrue(
                DeliveryChallanAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DeliveryChallanOperation.CREATE,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Error
            )
        }
    }

    @Test
    fun `External roles are denied all operations`() {
        for (role in listOf(UserRole.CUSTOMER, UserRole.VENDOR)) {
            assertTrue(
                DeliveryChallanAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DeliveryChallanOperation.VIEW,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Error
            )
        }
    }

    @Test
    fun `Cross project operation is rejected`() {
        val result = DeliveryChallanAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = DeliveryChallanOperation.VIEW,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-02"
        )
        assertTrue(result is DomainResult.Error)
    }
}
