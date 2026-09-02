package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import org.junit.Assert.*
import org.junit.Test

class CapabilityAwareNavigationTest {

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "USR-CUST-1",
        username = "customer1",
        role = UserRole.CUSTOMER,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    @Test
    fun testMenuFilterHidesAdminDestinationsFromCustomer() {
        val allDestinations = listOf(
            AppDestination.Public.Home,
            AppDestination.Customer.Orders,
            AppDestination.Admin.Users,
            AppDestination.Admin.Security
        )

        val filtered = CapabilityAwareNavigation.filterDestinationsForRole(allDestinations, UserRole.CUSTOMER)
        assertEquals(2, filtered.size)
        assertTrue(filtered.contains(AppDestination.Public.Home))
        assertTrue(filtered.contains(AppDestination.Customer.Orders))
        assertFalse(filtered.contains(AppDestination.Admin.Users))
        assertFalse(filtered.contains(AppDestination.Admin.Security))
    }

    @Test
    fun testHiddenRouteAccessIsDeniedByServerAuthorization() {
        // Customer manually navigating to admin route
        val isAuthorized = CapabilityAwareNavigation.isRouteAuthorized(customerPrincipal, AppDestination.Admin.Users)
        assertFalse("Customer must be denied server route access to admin users", isAuthorized)
    }
}
