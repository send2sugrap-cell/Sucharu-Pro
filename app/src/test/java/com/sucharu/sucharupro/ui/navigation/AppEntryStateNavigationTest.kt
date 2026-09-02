package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import com.sucharu.sucharupro.ui.features.auth.PostLoginRouter
import org.junit.Assert.*
import org.junit.Test

class AppEntryStateNavigationTest {

    @Test
    fun testGuestStateRoutesToPublicHome() {
        val principal = AuthenticatedPrincipal(
            userId = "USR-GUEST-1",
            username = "guest",
            role = UserRole.GUEST,
            permissions = emptySet(),
            projectId = "PRJ-01",
            accountStatus = AccountStatus.ACTIVE
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertTrue(dest is AppDestination.Public.Home)
        assertTrue(dest.isPublic)
    }

    @Test
    fun testAuthenticatedStateResolvesRoleWorkspace() {
        val customer = AuthenticatedPrincipal(
            userId = "USR-CUST-1",
            username = "customer1",
            role = UserRole.CUSTOMER,
            permissions = emptySet(),
            projectId = "PRJ-01",
            accountStatus = AccountStatus.ACTIVE
        )
        val dest = PostLoginRouter.resolveAppDestination(customer)
        assertTrue(dest is AppDestination.Customer.Home)
        assertFalse(dest.isPublic)
    }
}
