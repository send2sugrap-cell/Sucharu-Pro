package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReturnAuthorizationTest {

    @Test
    fun `ADMIN and MANAGER have full return authority`() {
        for (role in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
            for (op in DeliveryReturnOperation.values()) {
                val res = DeliveryReturnAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = op,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                )
                assertTrue("Role $role should have permission for $op", res is DomainResult.Success)
            }
        }
    }

    @Test
    fun `WAREHOUSE can execute operational actions but cannot approve or reject`() {
        val allowedOps = listOf(
            DeliveryReturnOperation.VIEW,
            DeliveryReturnOperation.CREATE,
            DeliveryReturnOperation.RECEIVE,
            DeliveryReturnOperation.INSPECT,
            DeliveryReturnOperation.SET_DISPOSITION,
            DeliveryReturnOperation.PROCESS_RESTOCK,
            DeliveryReturnOperation.COMPLETE
        )
        for (op in allowedOps) {
            val res = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = op,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            )
            assertTrue(res is DomainResult.Success)
        }

        val deniedOps = listOf(DeliveryReturnOperation.APPROVE, DeliveryReturnOperation.REJECT)
        for (op in deniedOps) {
            val res = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = op,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            )
            assertTrue(res is DomainResult.Error)
        }
    }

    @Test
    fun `cross project operation is strictly rejected`() {
        val res = DeliveryReturnAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = DeliveryReturnOperation.VIEW,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-02"
        )
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("Access denied"))
    }
}
