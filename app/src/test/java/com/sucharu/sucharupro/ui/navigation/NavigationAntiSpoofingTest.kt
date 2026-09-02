package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import org.junit.Assert.*
import org.junit.Test

class NavigationAntiSpoofingTest {

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "USR-CUST-777",
        username = "customer777",
        role = UserRole.CUSTOMER,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    @Test
    fun testClientSuppliedRoleInDeepLinkIsIgnored() {
        // Customer attempts deep link passing clientSuppliedRole = "ADMIN"
        val dest = DeepLinkAuthorizer.authorizeDeepLink(
            route = "admin/users",
            principal = customerPrincipal,
            clientSuppliedRole = "ADMIN"
        )
        assertTrue("Client-supplied role hint must be ignored and route denied", dest is AppDestination.Security.Forbidden)
    }

    @Test
    fun testClientSuppliedUserIdInDeepLinkIsIgnored() {
        // Customer attempts deep link passing clientSuppliedUserId = "USR-ADMIN-1"
        val dest = DeepLinkAuthorizer.authorizeDeepLink(
            route = "admin/dashboard",
            principal = customerPrincipal,
            clientSuppliedUserId = "USR-ADMIN-1"
        )
        assertTrue("Client-supplied userId hint must be ignored and route denied", dest is AppDestination.Security.Forbidden)
    }

    @Test
    fun testClientSuppliedProjectIdInDeepLinkIsIgnored() {
        val dest = DeepLinkAuthorizer.authorizeDeepLink(
            route = "admin/finance",
            principal = customerPrincipal,
            clientSuppliedProjectId = "PRJ-OTHER-99"
        )
        assertTrue("Client-supplied projectId hint must be ignored and route denied", dest is AppDestination.Security.Forbidden)
    }
}
