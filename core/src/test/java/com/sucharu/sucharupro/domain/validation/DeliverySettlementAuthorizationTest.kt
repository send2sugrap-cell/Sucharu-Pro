package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliverySettlementAuthorizationTest {

    @Test
    fun `ADMIN and MANAGER have full settlement authority`() {
        for (role in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
            for (op in DeliverySettlementOperation.values()) {
                val res = DeliverySettlementAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = op,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                )
                assertTrue("Role $role should be authorized for $op", res is DomainResult.Success)
            }
        }
    }

    @Test
    fun `WAREHOUSE can VIEW, CREATE_SPLIT, RECORD_PARTIAL, and RECALCULATE`() {
        val allowed = listOf(
            DeliverySettlementOperation.VIEW,
            DeliverySettlementOperation.CREATE_SPLIT,
            DeliverySettlementOperation.RECORD_PARTIAL,
            DeliverySettlementOperation.RECALCULATE
        )
        for (op in allowed) {
            val res = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = op,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            )
            assertTrue(res is DomainResult.Success)
        }

        val denied = DeliverySettlementAuthorizationValidator.validateOperation(
            callerRole = UserRole.WAREHOUSE,
            operation = DeliverySettlementOperation.FINALIZE_SETTLEMENT,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-01"
        )
        assertTrue(denied is DomainResult.Error)
    }

    @Test
    fun `cross project access is rejected`() {
        val res = DeliverySettlementAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = DeliverySettlementOperation.VIEW,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-02"
        )
        assertTrue(res is DomainResult.Error)
    }
}
