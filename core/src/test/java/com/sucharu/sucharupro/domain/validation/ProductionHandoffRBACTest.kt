package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Category D: RBAC Tests for Production Handoff Authorization (Module 05 Step 05).
 */
class ProductionHandoffRBACTest {

    @Test
    fun handoffAuthorization_adminAndManager_pass() {
        listOf(UserRole.ADMIN, UserRole.MANAGER).forEach { role ->
            val result = DesignProductionHandoffValidator.validateHandoffPermission(role)
            assertTrue("Role ${role.name} should be authorized to hand off to production", result is DomainResult.Success)
        }
    }

    @Test
    fun handoffAuthorization_designerCannotSelfAuthorize() {
        val result = DesignProductionHandoffValidator.validateHandoffPermission(UserRole.DESIGNER)
        assertTrue("Designer should not be allowed to self-authorize production handoff", result is DomainResult.Error)
    }

    @Test
    fun handoffAuthorization_restrictedRoles_fail() {
        val restricted = listOf(
            UserRole.CUSTOMER,
            UserRole.AFFILIATE,
            UserRole.VENDOR,
            UserRole.WAREHOUSE,
            UserRole.QC_INSPECTOR,
            UserRole.ACCOUNTS,
            UserRole.STAFF
        )
        restricted.forEach { role ->
            val result = DesignProductionHandoffValidator.validateHandoffPermission(role)
            assertTrue("Role ${role.name} should be restricted from production handoff", result is DomainResult.Error)
        }
    }
}
