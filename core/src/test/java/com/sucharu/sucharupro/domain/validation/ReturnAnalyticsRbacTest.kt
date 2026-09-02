package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.returns.ReturnAnalyticsAuthorizationValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC Authorization tests for Return Analytics & Governance (Module 11 Step 06 Chunk 03).
 */
class ReturnAnalyticsRbacTest {

    @Test
    fun `test authorized roles ADMIN, MANAGER, ACCOUNTS succeed`() {
        assertTrue(ReturnAnalyticsAuthorizationValidator.validateRole(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(ReturnAnalyticsAuthorizationValidator.validateRole(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(ReturnAnalyticsAuthorizationValidator.validateRole(UserRole.ACCOUNTS) is DomainResult.Success)
    }

    @Test
    fun `test unauthorized roles CUSTOMER, STAFF, QC_INSPECTOR, WAREHOUSE, VENDOR fail`() {
        assertTrue(ReturnAnalyticsAuthorizationValidator.validateRole(UserRole.CUSTOMER) is DomainResult.Error)
        assertTrue(ReturnAnalyticsAuthorizationValidator.validateRole(UserRole.STAFF) is DomainResult.Error)
        assertTrue(ReturnAnalyticsAuthorizationValidator.validateRole(UserRole.QC_INSPECTOR) is DomainResult.Error)
        assertTrue(ReturnAnalyticsAuthorizationValidator.validateRole(UserRole.WAREHOUSE) is DomainResult.Error)
        assertTrue(ReturnAnalyticsAuthorizationValidator.validateRole(UserRole.VENDOR) is DomainResult.Error)
    }
}
