package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryAuthorizationTest {

    @Test
    fun `ADMIN and MANAGER have full permissions`() {
        for (role in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
            for (op in DeliveryOperation.values()) {
                val result = DeliveryAuthorizationValidator.validateOperation(
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
    fun `WAREHOUSE can VIEW, READY_FOR_DISPATCH, CREATE_DISPATCH_REQUEST but not CREATE or APPROVE`() {
        // Allowed
        assertTrue(
            DeliveryAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryOperation.VIEW,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryOperation.READY_FOR_DISPATCH,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryOperation.CREATE_DISPATCH_REQUEST,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )

        // Denied
        assertTrue(
            DeliveryAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryOperation.CREATE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
        assertTrue(
            DeliveryAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryOperation.APPROVE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
        assertTrue(
            DeliveryAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DeliveryOperation.CANCEL,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
    }

    @Test
    fun `STAFF and QC and ACCOUNTS can VIEW only`() {
        val readOnlyRoles = listOf(UserRole.STAFF, UserRole.QC_INSPECTOR, UserRole.ACCOUNTS)
        for (role in readOnlyRoles) {
            assertTrue(
                DeliveryAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DeliveryOperation.VIEW,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Success
            )

            assertTrue(
                DeliveryAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DeliveryOperation.CREATE,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Error
            )
        }
    }

    @Test
    fun `External roles are denied all operations`() {
        val externalRoles = listOf(UserRole.CUSTOMER, UserRole.VENDOR)
        for (role in externalRoles) {
            assertTrue(
                DeliveryAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DeliveryOperation.VIEW,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Error
            )
        }
    }

    @Test
    fun `Cross project access is denied regardless of role`() {
        val result = DeliveryAuthorizationValidator.validateOperation(
            callerRole = UserRole.MANAGER,
            operation = DeliveryOperation.VIEW,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-02"
        )
        assertTrue(result is DomainResult.Error)
    }
}
