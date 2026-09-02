package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import org.junit.Assert.*
import org.junit.Test

class VendorPortalPermissionTest {

    @Test
    fun testVendorPrincipalPermissions() {
        val principal = AuthenticatedPrincipal(
            userId = "usr_001",
            projectId = "PROJ-ALPHA",
            username = "vendor.user",
            role = UserRole.VENDOR,
            principalType = PrincipalType.HUMAN,
            vendorId = "vnd_001",
            permissions = setOf(
                UserPermission.READ_VENDOR_PORTAL,
                UserPermission.READ_OWN_PROFILE,
                UserPermission.UPDATE_OWN_PROFILE
            )
        )

        assertTrue(principal.hasPermission(UserPermission.READ_VENDOR_PORTAL))
        assertTrue(principal.hasPermission(UserPermission.READ_OWN_PROFILE))
        assertFalse(principal.hasPermission(UserPermission.MANAGE_VENDOR_PORTAL_ACCESS))
        assertFalse(principal.hasPermission(UserPermission.ADMIN_ALL))
        assertEquals("vnd_001", principal.effectiveVendorId)
    }
}
