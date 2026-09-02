package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC authorization tests for Artwork and File Management (Module 05 Step 02).
 */
class ArtworkRBACTest {

    @Test
    fun authorizedRoles_passArtworkPermission() {
        val authorized = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.DESIGNER)
        authorized.forEach { role ->
            val result = DesignArtworkValidator.validateArtworkPermission(role)
            assertTrue("Role ${role.name} should be authorized for artwork management", result is DomainResult.Success)
        }
    }

    @Test
    fun restrictedRoles_failArtworkPermission() {
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
            val result = DesignArtworkValidator.validateArtworkPermission(role)
            assertTrue("Role ${role.name} should be restricted from managing artwork", result is DomainResult.Error)
            val error = result as DomainResult.Error
            assertTrue(error.message.contains("is not authorized to manage artwork files"))
        }
    }
}
