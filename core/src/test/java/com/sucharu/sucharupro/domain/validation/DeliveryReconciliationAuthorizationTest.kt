package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReconciliationAuthorizationTest {

    @Test
    fun `admin has full access to all operations`() {
        val res = DeliveryReconciliationAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = DeliveryReconciliationOperation.CLOSE,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-01",
            actorId = "admin-1",
            creatorId = "admin-1"
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `manager can resolve discrepancies and reconcile`() {
        val res = DeliveryReconciliationAuthorizationValidator.validateOperation(
            callerRole = UserRole.MANAGER,
            operation = DeliveryReconciliationOperation.RESOLVE_DISCREPANCY,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-01"
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `separation of duties prevents creator manager from closing own reconciliation`() {
        val res = DeliveryReconciliationAuthorizationValidator.validateOperation(
            callerRole = UserRole.MANAGER,
            operation = DeliveryReconciliationOperation.CLOSE,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-01",
            actorId = "mgr-1",
            creatorId = "mgr-1"
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun `warehouse cannot close reconciliation`() {
        val res = DeliveryReconciliationAuthorizationValidator.validateOperation(
            callerRole = UserRole.WAREHOUSE,
            operation = DeliveryReconciliationOperation.CLOSE,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-01"
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun `customer has read only access`() {
        val viewRes = DeliveryReconciliationAuthorizationValidator.validateOperation(
            callerRole = UserRole.CUSTOMER,
            operation = DeliveryReconciliationOperation.VIEW,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-01"
        )
        assertTrue(viewRes is DomainResult.Success)

        val mutateRes = DeliveryReconciliationAuthorizationValidator.validateOperation(
            callerRole = UserRole.CUSTOMER,
            operation = DeliveryReconciliationOperation.START_RECONCILIATION,
            targetProjectId = "PRJ-01",
            callerProjectId = "PRJ-01"
        )
        assertTrue(mutateRes is DomainResult.Error)
    }
}
