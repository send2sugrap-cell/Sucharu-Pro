package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import org.junit.Assert.*
import org.junit.Test

class AffiliateWorkspaceNavigationTest {

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "USR-AFF-2002",
        username = "affiliate1",
        role = UserRole.AFFILIATE,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    @Test
    fun testAffiliateCanAccessAffiliateDestinations() {
        val destinations = listOf(
            AppDestination.Affiliate.Home,
            AppDestination.Affiliate.Profile,
            AppDestination.Affiliate.ReferralLinks,
            AppDestination.Affiliate.Referrals,
            AppDestination.Affiliate.Commission,
            AppDestination.Affiliate.CommissionHistory,
            AppDestination.Affiliate.Payouts,
            AppDestination.Affiliate.Performance,
            AppDestination.Affiliate.Offers,
            AppDestination.Affiliate.Notifications,
            AppDestination.Affiliate.AiAssistant,
            AppDestination.Affiliate.Settings,
            AppDestination.Affiliate.SessionsSecurity
        )

        for (dest in destinations) {
            assertTrue("Affiliate must be authorized for ${dest.route}", CapabilityAwareNavigation.isRouteAuthorized(affiliatePrincipal, dest))
        }
    }
}
