package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.DeliveryGovernanceAuthorizationValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryAnalyticsAuthorizationTest {

    @Test
    fun `view analytics is permitted for internal roles`() {
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateViewAnalytics(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateViewAnalytics(UserRole.WAREHOUSE) is DomainResult.Success)
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateViewAnalytics(UserRole.STAFF) is DomainResult.Success)
    }

    @Test
    fun `governance visibility is restricted from external roles`() {
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateViewGovernance(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateViewGovernance(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateViewGovernance(UserRole.CUSTOMER) is DomainResult.Error)
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateViewGovernance(UserRole.VENDOR) is DomainResult.Error)
    }

    @Test
    fun `only management roles can resolve or dismiss alerts`() {
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateResolveAlert(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateResolveAlert(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateResolveAlert(UserRole.WAREHOUSE) is DomainResult.Error)
        assertTrue(DeliveryGovernanceAuthorizationValidator.validateResolveAlert(UserRole.CUSTOMER) is DomainResult.Error)
    }
}
