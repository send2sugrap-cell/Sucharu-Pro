package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC authorization tests for Proof & Revision management (Module 05 Step 03).
 */
class ProofRBACTest {

    @Test
    fun proofManagement_authorizedRoles_pass() {
        val authorized = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.DESIGNER)
        authorized.forEach { role ->
            val result = DesignProofValidator.validateProofManagementPermission(role)
            assertTrue("Role ${role.name} should have proof management permission", result is DomainResult.Success)
        }
    }

    @Test
    fun proofManagement_restrictedRoles_fail() {
        val restricted = listOf(
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.WAREHOUSE,
            UserRole.QC_INSPECTOR,
            UserRole.ACCOUNTS,
            UserRole.STAFF
        )
        restricted.forEach { role ->
            val result = DesignProofValidator.validateProofManagementPermission(role)
            assertTrue("Role ${role.name} should be restricted from proof management", result is DomainResult.Error)
        }
    }

    @Test
    fun revisionRequest_authorizedRoles_pass() {
        val authorized = listOf(UserRole.ADMIN, UserRole.MANAGER)
        authorized.forEach { role ->
            val result = DesignProofValidator.validateRevisionRequestPermission(role)
            assertTrue("Role ${role.name} should have revision request permission", result is DomainResult.Success)
        }
    }

    @Test
    fun revisionRequest_designerAndRestricted_fail() {
        val restricted = listOf(
            UserRole.DESIGNER,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.WAREHOUSE,
            UserRole.QC_INSPECTOR,
            UserRole.ACCOUNTS,
            UserRole.STAFF
        )
        restricted.forEach { role ->
            val result = DesignProofValidator.validateRevisionRequestPermission(role)
            assertTrue("Role ${role.name} should not be allowed to request revisions independently", result is DomainResult.Error)
        }
    }
}
