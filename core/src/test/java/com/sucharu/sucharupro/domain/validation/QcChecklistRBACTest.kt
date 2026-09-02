package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC tests for QC Checklist template management and inspection execution (Module 06 Step 03).
 */
class QcChecklistRBACTest {

    @Test
    fun templateManagement_authorizedRoles_succeed() {
        listOf(UserRole.ADMIN, UserRole.MANAGER).forEach { role ->
            assertTrue(QcChecklistTemplateValidator.validateTemplateManagementPermission(role) is DomainResult.Success)
        }
    }

    @Test
    fun inspectionExecution_authorizedRoles_succeed() {
        listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR).forEach { role ->
            assertTrue(QcInspectionChecklistValidator.validateInspectionPermission(role) is DomainResult.Success)
        }
    }

    @Test
    fun restrictedRoles_deniedChecklistOperations() {
        listOf(
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        ).forEach { role ->
            assertTrue(QcChecklistTemplateValidator.validateTemplateManagementPermission(role) is DomainResult.Error)
            assertTrue(QcInspectionChecklistValidator.validateInspectionPermission(role) is DomainResult.Error)
        }
    }
}
