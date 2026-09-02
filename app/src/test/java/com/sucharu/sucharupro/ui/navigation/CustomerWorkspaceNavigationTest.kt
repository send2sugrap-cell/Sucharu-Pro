package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import org.junit.Assert.*
import org.junit.Test

class CustomerWorkspaceNavigationTest {

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "USR-CUST-1001",
        username = "customer1",
        role = UserRole.CUSTOMER,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    @Test
    fun testCustomerCanAccessCustomerDestinations() {
        val destinations = listOf(
            AppDestination.Customer.Home,
            AppDestination.Customer.Profile,
            AppDestination.Customer.Orders,
            AppDestination.Customer.Quotations,
            AppDestination.Customer.Invoices,
            AppDestination.Customer.Payments,
            AppDestination.Customer.DeliveryTracking,
            AppDestination.Customer.Returns,
            AppDestination.Customer.Notifications,
            AppDestination.Customer.Offers,
            AppDestination.Customer.Support,
            AppDestination.Customer.AiAssistant,
            AppDestination.Customer.Settings,
            AppDestination.Customer.SessionsSecurity
        )

        for (dest in destinations) {
            assertTrue("Customer must be authorized for ${dest.route}", CapabilityAwareNavigation.isRouteAuthorized(customerPrincipal, dest))
        }
    }
}
