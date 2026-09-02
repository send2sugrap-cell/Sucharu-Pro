package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import com.sucharu.sucharupro.ui.features.auth.PostLoginRouter
import org.junit.Assert.*
import org.junit.Test

class AccountStatusRoutingTest {

    @Test
    fun testPendingStatusRoutesToVerificationRequired() {
        val principal = AuthenticatedPrincipal(
            userId = "U1",
            projectId = "P1",
            username = "user",
            role = UserRole.CUSTOMER,
            accountStatus = AccountStatus.PENDING
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertTrue(dest is AppDestination.Security.VerificationRequired)
    }

    @Test
    fun testLockedStatusRoutesToAccountUnavailable() {
        val principal = AuthenticatedPrincipal(
            userId = "U1",
            projectId = "P1",
            username = "user",
            role = UserRole.CUSTOMER,
            accountStatus = AccountStatus.LOCKED
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertTrue(dest is AppDestination.Security.AccountUnavailable)
    }

    @Test
    fun testSuspendedStatusRoutesToAccountUnavailable() {
        val principal = AuthenticatedPrincipal(
            userId = "U1",
            projectId = "P1",
            username = "user",
            role = UserRole.CUSTOMER,
            accountStatus = AccountStatus.SUSPENDED
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertTrue(dest is AppDestination.Security.AccountUnavailable)
    }

    @Test
    fun testDeactivatedStatusRoutesToAccountUnavailable() {
        val principal = AuthenticatedPrincipal(
            userId = "U1",
            projectId = "P1",
            username = "user",
            role = UserRole.CUSTOMER,
            accountStatus = AccountStatus.DEACTIVATED
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertTrue(dest is AppDestination.Security.AccountUnavailable)
    }

    @Test
    fun testSecurityReviewStatusRoutesToSecurityReview() {
        val principal = AuthenticatedPrincipal(
            userId = "U1",
            projectId = "P1",
            username = "user",
            role = UserRole.CUSTOMER,
            accountStatus = AccountStatus.SECURITY_REVIEW
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertTrue(dest is AppDestination.Security.SecurityReview)
    }
}
