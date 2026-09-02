package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryShipmentAuthorizationTest {

    @Test
    fun `ADMIN and MANAGER have full management authorization`() {
        for (role in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
            for (op in DeliveryShipmentOperation.values()) {
                val res = DeliveryShipmentAuthorizationValidator.validateOperation(
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
    fun `WAREHOUSE can CREATE, MARK_READY, MARK_DISPATCHED, UPDATE_STATUS, RECORD_ATTEMPT, ADD_EVENT, and VIEW`() {
        val allowedOps = listOf(
            DeliveryShipmentOperation.VIEW,
            DeliveryShipmentOperation.CREATE,
            DeliveryShipmentOperation.MARK_READY,
            DeliveryShipmentOperation.MARK_DISPATCHED,
            DeliveryShipmentOperation.UPDATE_STATUS,
            DeliveryShipmentOperation.RECORD_ATTEMPT,
            DeliveryShipmentOperation.ADD_EVENT
        )
        for (op in allowedOps) {
            val res = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = op,
                targetProjectId = "PRJ-01",
                callerProjectId = "PRJ-01"
            )
            assertTrue("WAREHOUSE should be authorized for $op", res is DomainResult.Success)
        }

        val denied = DeliveryShipmentAuthorizationValidator.validateOperation(
            callerRole = UserRole.WAREHOUSE,
            operation = DeliveryShipmentOperation.CANCEL,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-01"
        )
        assertTrue(denied is DomainResult.Error)
    }

    @Test
    fun `STAFF and ACCOUNTS have VIEW access only`() {
        for (role in listOf(UserRole.STAFF, UserRole.ACCOUNTS, UserRole.QC_INSPECTOR)) {
            assertTrue(
                DeliveryShipmentAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DeliveryShipmentOperation.VIEW,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Success
            )
            assertTrue(
                DeliveryShipmentAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = DeliveryShipmentOperation.MARK_DISPATCHED,
                    targetProjectId = "PRJ-01",
                    callerProjectId = "PRJ-01"
                ) is DomainResult.Error
            )
        }
    }

    @Test
    fun `cross project operation is rejected`() {
        val res = DeliveryShipmentAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = DeliveryShipmentOperation.VIEW,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-02"
        )
        assertTrue(res is DomainResult.Error)
    }
}
