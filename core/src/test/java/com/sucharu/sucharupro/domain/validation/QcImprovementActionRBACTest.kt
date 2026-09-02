package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class QcImprovementActionRBACTest {

    @Test
    fun `QC_INSPECTOR can propose actions but cannot approve or verify them`() {
        assertTrue(QcGovernanceValidator.validateActionProposePermission(UserRole.QC_INSPECTOR) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateActionApprovePermission(UserRole.QC_INSPECTOR) is DomainResult.Error)
        assertTrue(QcGovernanceValidator.validateActionVerifyPermission(UserRole.QC_INSPECTOR) is DomainResult.Error)
    }

    @Test
    fun `MANAGER and ADMIN can approve and verify actions`() {
        assertTrue(QcGovernanceValidator.validateActionApprovePermission(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateActionVerifyPermission(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateActionApprovePermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateActionVerifyPermission(UserRole.ADMIN) is DomainResult.Success)
    }
}
