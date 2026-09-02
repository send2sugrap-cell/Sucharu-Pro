package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import org.junit.Assert.*
import org.junit.Test

class DeepLinkAuthorizationTest {

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "USR-CUST-100",
        username = "customer100",
        role = UserRole.CUSTOMER,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "USR-AFF-200",
        username = "affiliate200",
        role = UserRole.AFFILIATE,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    @Test
    fun testCustomerDeepLinkAccessToOwnOrderSucceeds() {
        val dest = DeepLinkAuthorizer.authorizeDeepLink("customer/orders/ORD-1001", customerPrincipal)
        assertTrue(dest is AppDestination.Customer.OrderDetails)
        assertEquals("ORD-1001", (dest as AppDestination.Customer.OrderDetails).orderId)
    }

    @Test
    fun testCustomerDeepLinkAccessToAnotherCustomerOrderIsForbidden() {
        val dest = DeepLinkAuthorizer.authorizeDeepLink("customer/orders/CUST-OTHER-ORD-99", customerPrincipal)
        assertTrue(dest is AppDestination.Security.Forbidden)
    }

    @Test
    fun testAffiliateDeepLinkAccessToAnotherAffiliateReferralIsForbidden() {
        val dest = DeepLinkAuthorizer.authorizeDeepLink("affiliate/referrals/REF-OTHER-999", affiliatePrincipal)
        assertTrue(dest is AppDestination.Security.Forbidden)
    }

    @Test
    fun testCustomerDeepLinkAccessToAdminRouteIsForbidden() {
        val dest = DeepLinkAuthorizer.authorizeDeepLink("admin/users/USR-ADMIN-1", customerPrincipal)
        assertTrue(dest is AppDestination.Security.Forbidden)
    }
}
