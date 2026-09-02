package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchExecutionAuthorizationTest {

    @Test
    fun `ADMIN and MANAGER have full management authority`() {
        for (role in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
            for (op in DispatchExecutionOperation.values()) {
                val result = DispatchExecutionAuthorizationValidator.validateOperation(
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
    fun `WAREHOUSE can VIEW, PREPARE, and EXECUTE_DISPATCH, but cannot CREATE, EDIT or APPROVE`() {
        assertTrue(
            DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DispatchExecutionOperation.VIEW,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DispatchExecutionOperation.PREPARE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )
        assertTrue(
            DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DispatchExecutionOperation.EXECUTE_DISPATCH,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Success
        )

        // Denied
        assertTrue(
            DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DispatchExecutionOperation.CREATE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
        assertTrue(
            DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = DispatchExecutionOperation.APPROVE,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            ) is DomainResult.Error
        )
    }

    @Test
    fun `Internal roles STAFF, QC, ACCOUNTS can VIEW only`() {
        for (role in listOf(UserRole.STAFF, UserRole.QC_INSPECTOR, UserRole.ACCOUNTS)) {
            assertTrue(
                DispatchExecutionAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DispatchExecutionOperation.VIEW,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Success
            )
            assertTrue(
                DispatchExecutionAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DispatchExecutionOperation.EXECUTE_DISPATCH,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Error
            )
        }
    }

    @Test
    fun `Cross project operation is rejected`() {
        val result = DispatchExecutionAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = DispatchExecutionOperation.VIEW,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-02"
        )
        assertTrue(result is DomainResult.Error)
    }
}
