package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Separation of Duties tests for Final QC & Production Release (Module 06 Step 07).
 *
 * Verifies that QC Inspectors CANNOT independently authorize production release,
 * requiring Management (ADMIN / MANAGER) approval.
 */
class FinalQcSeparationOfDutiesTest {

    @Test
    fun releaseAuthorization_deniedToInspector() {
        val result = FinalQcAssignmentValidator.validateReleaseAuthorizationPermission(UserRole.QC_INSPECTOR)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Separation of duties violation"))
    }

    @Test
    fun releaseAuthorization_grantedToAdminAndManager() {
        val adminResult = FinalQcAssignmentValidator.validateReleaseAuthorizationPermission(UserRole.ADMIN)
        assertTrue(adminResult is DomainResult.Success)

        val managerResult = FinalQcAssignmentValidator.validateReleaseAuthorizationPermission(UserRole.MANAGER)
        assertTrue(managerResult is DomainResult.Success)
    }

    @Test
    fun releaseAuthorization_deniedToNonManagementRoles() {
        val nonMgmt = listOf(
            UserRole.STAFF,
            UserRole.DESIGNER,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        )
        for (role in nonMgmt) {
            val result = FinalQcAssignmentValidator.validateReleaseAuthorizationPermission(role)
            assertTrue("Role $role should be denied release authorization", result is DomainResult.Error)
        }
    }
}
