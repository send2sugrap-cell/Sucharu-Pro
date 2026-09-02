package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import com.sucharu.sucharupro.ui.features.auth.PostLoginRouter
import org.junit.Assert.*
import org.junit.Test

class AiAgentInteractiveRoutingSecurityTest {

    private val aiAgentPrincipal = AuthenticatedPrincipal(
        userId = "USR-AI-AGENT-01",
        username = "ai_agent_bot",
        role = UserRole.AI_AGENT,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    @Test
    fun testAiAgentIsDeniedInteractivePostLoginRouting() {
        val dest = PostLoginRouter.resolveAppDestination(aiAgentPrincipal)
        assertTrue("AI_AGENT machine principal must resolve to Forbidden security state", dest is AppDestination.Security.Forbidden)
    }

    @Test
    fun testAiAgentIsDeniedDeepLinkHumanDashboardAccess() {
        val destAdmin = DeepLinkAuthorizer.authorizeDeepLink("admin/dashboard", aiAgentPrincipal)
        assertTrue(destAdmin is AppDestination.Security.Forbidden)

        val destCustomer = DeepLinkAuthorizer.authorizeDeepLink("customer/orders", aiAgentPrincipal)
        assertTrue(destCustomer is AppDestination.Security.Forbidden)
    }

    @Test
    fun testAiAgentReceivesEmptyHumanMenuFilter() {
        val filtered = CapabilityAwareNavigation.filterDestinationsForRole(
            listOf(AppDestination.Customer.Orders, AppDestination.Admin.Users),
            UserRole.AI_AGENT
        )
        assertTrue("AI_AGENT must receive zero human UI menu items", filtered.isEmpty())
    }
}
