package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.returns.ReturnAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.returns.ReturnOperation
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC unit tests for [ReturnAuthorizationValidator] (Module 11 Step 01).
 *
 * Covers: ADMIN/MANAGER full authority, per-role operation boundaries,
 * external actor denial, and cross-project enforcement.
 */
class ReturnRbacTest {

    private val projectId = "PRJ-A"

    // =========================================================================
    // ADMIN and MANAGER — full authority
    // =========================================================================

    @Test
    fun `ADMIN has full authority over all Return operations`() {
        for (op in ReturnOperation.entries) {
            val res = ReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.ADMIN,
                operation = op,
                targetProjectId = projectId,
                callerProjectId = projectId
            )
            assertTrue("ADMIN should be authorized for $op", res is DomainResult.Success)
        }
    }

    @Test
    fun `MANAGER has full authority over all Return operations`() {
        for (op in ReturnOperation.entries) {
            val res = ReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.MANAGER,
                operation = op,
                targetProjectId = projectId,
                callerProjectId = projectId
            )
            assertTrue("MANAGER should be authorized for $op", res is DomainResult.Success)
        }
    }

    // =========================================================================
    // WAREHOUSE — operational actions only
    // =========================================================================

    @Test
    fun `WAREHOUSE can view, receive, inspect, process and cancel returns`() {
        val allowed = listOf(
            ReturnOperation.VIEW_RETURN,
            ReturnOperation.RECEIVE_RETURN,
            ReturnOperation.INSPECT_RETURN,
            ReturnOperation.PROCESS_RETURN,
            ReturnOperation.CANCEL_RETURN
        )
        for (op in allowed) {
            val res = ReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = op,
                targetProjectId = projectId,
                callerProjectId = projectId
            )
            assertTrue("WAREHOUSE should be authorized for $op", res is DomainResult.Success)
        }
    }

    @Test
    fun `WAREHOUSE cannot approve or reject returns`() {
        val denied = listOf(ReturnOperation.APPROVE_RETURN, ReturnOperation.REJECT_RETURN)
        for (op in denied) {
            val res = ReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.WAREHOUSE,
                operation = op,
                targetProjectId = projectId,
                callerProjectId = projectId
            )
            assertTrue("WAREHOUSE must be denied for $op", res is DomainResult.Error)
        }
    }

    // =========================================================================
    // QC_INSPECTOR — view and inspect only
    // =========================================================================

    @Test
    fun `QC_INSPECTOR can view and inspect returns`() {
        val allowed = listOf(ReturnOperation.VIEW_RETURN, ReturnOperation.INSPECT_RETURN)
        for (op in allowed) {
            val res = ReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.QC_INSPECTOR,
                operation = op,
                targetProjectId = projectId,
                callerProjectId = projectId
            )
            assertTrue("QC_INSPECTOR should be authorized for $op", res is DomainResult.Success)
        }
    }

    @Test
    fun `QC_INSPECTOR cannot approve, reject, receive, process or cancel returns`() {
        val denied = listOf(
            ReturnOperation.APPROVE_RETURN,
            ReturnOperation.REJECT_RETURN,
            ReturnOperation.RECEIVE_RETURN,
            ReturnOperation.PROCESS_RETURN,
            ReturnOperation.CANCEL_RETURN,
            ReturnOperation.CREATE_RETURN
        )
        for (op in denied) {
            val res = ReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.QC_INSPECTOR,
                operation = op,
                targetProjectId = projectId,
                callerProjectId = projectId
            )
            assertTrue("QC_INSPECTOR must be denied for $op", res is DomainResult.Error)
        }
    }

    // =========================================================================
    // STAFF — create and view only
    // =========================================================================

    @Test
    fun `STAFF can create and view returns`() {
        val allowed = listOf(ReturnOperation.CREATE_RETURN, ReturnOperation.VIEW_RETURN)
        for (op in allowed) {
            val res = ReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.STAFF,
                operation = op,
                targetProjectId = projectId,
                callerProjectId = projectId
            )
            assertTrue("STAFF should be authorized for $op", res is DomainResult.Success)
        }
    }

    @Test
    fun `STAFF cannot perform any approval, inspection, or processing operations`() {
        val denied = listOf(
            ReturnOperation.APPROVE_RETURN,
            ReturnOperation.REJECT_RETURN,
            ReturnOperation.INSPECT_RETURN,
            ReturnOperation.RECEIVE_RETURN,
            ReturnOperation.PROCESS_RETURN,
            ReturnOperation.CANCEL_RETURN
        )
        for (op in denied) {
            val res = ReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.STAFF,
                operation = op,
                targetProjectId = projectId,
                callerProjectId = projectId
            )
            assertTrue("STAFF must be denied for $op", res is DomainResult.Error)
        }
    }

    // =========================================================================
    // ACCOUNTS — view only
    // =========================================================================

    @Test
    fun `ACCOUNTS can view and settle returns but is denied all other operations`() {
        val viewRes = ReturnAuthorizationValidator.validateOperation(
            callerRole = UserRole.ACCOUNTS,
            operation = ReturnOperation.VIEW_RETURN,
            targetProjectId = projectId,
            callerProjectId = projectId
        )
        assertTrue(viewRes is DomainResult.Success)

        val settleRes = ReturnAuthorizationValidator.validateOperation(
            callerRole = UserRole.ACCOUNTS,
            operation = ReturnOperation.SETTLE_RETURN,
            targetProjectId = projectId,
            callerProjectId = projectId
        )
        assertTrue(settleRes is DomainResult.Success)

        val denied = ReturnOperation.entries.filter {
            it != ReturnOperation.VIEW_RETURN && it != ReturnOperation.SETTLE_RETURN
        }
        for (op in denied) {
            val dRes = ReturnAuthorizationValidator.validateOperation(
                callerRole = UserRole.ACCOUNTS,
                operation = op,
                targetProjectId = projectId,
                callerProjectId = projectId
            )
            assertTrue("ACCOUNTS must be denied for $op", dRes is DomainResult.Error)
        }
    }

    // =========================================================================
    // External actors — all denied
    // =========================================================================

    @Test
    fun `CUSTOMER, VENDOR and AFFILIATE are denied all Return operations`() {
        val external = listOf(UserRole.CUSTOMER, UserRole.VENDOR, UserRole.AFFILIATE)
        for (role in external) {
            for (op in ReturnOperation.entries) {
                val res = ReturnAuthorizationValidator.validateOperation(
                    callerRole = role,
                    operation = op,
                    targetProjectId = projectId,
                    callerProjectId = projectId
                )
                assertTrue("External role $role must be denied for $op", res is DomainResult.Error)
            }
        }
    }

    // =========================================================================
    // Cross-project enforcement
    // =========================================================================

    @Test
    fun `cross project operation is rejected even for ADMIN`() {
        val res = ReturnAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = ReturnOperation.VIEW_RETURN,
            targetProjectId = "PRJ-A",
            callerProjectId = "PRJ-B"
        )
        assertTrue("Cross-project access must be rejected", res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("Access denied"))
    }

    @Test
    fun `same project passes project isolation check`() {
        val res = ReturnAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = ReturnOperation.VIEW_RETURN,
            targetProjectId = "PRJ-A",
            callerProjectId = "PRJ-A"
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `null callerProjectId skips project isolation check`() {
        val res = ReturnAuthorizationValidator.validateOperation(
            callerRole = UserRole.ADMIN,
            operation = ReturnOperation.VIEW_RETURN,
            targetProjectId = "PRJ-A",
            callerProjectId = null
        )
        assertTrue(res is DomainResult.Success)
    }
}
