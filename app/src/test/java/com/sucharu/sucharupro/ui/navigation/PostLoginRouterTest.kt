package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import com.sucharu.sucharupro.ui.features.auth.PostLoginRouter
import org.junit.Assert.*
import org.junit.Test

class PostLoginRouterTest {

    @Test
    fun testCustomerRoleRoutesToCustomerWorkspace() {
        val principal = AuthenticatedPrincipal(
            userId = "USR-CUST-1",
            username = "cust1",
            role = UserRole.CUSTOMER,
            permissions = emptySet(),
            projectId = "PRJ-01",
            accountStatus = AccountStatus.ACTIVE
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("customer/home", dest.route)
    }

    @Test
    fun testAffiliateRoleRoutesToAffiliateWorkspace() {
        val principal = AuthenticatedPrincipal(
            userId = "USR-AFF-1",
            username = "aff1",
            role = UserRole.AFFILIATE,
            permissions = emptySet(),
            projectId = "PRJ-01",
            accountStatus = AccountStatus.ACTIVE
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("affiliate/home", dest.route)
    }

    @Test
    fun testStaffRoleRoutesToStaffWorkspace() {
        val principal = AuthenticatedPrincipal(
            userId = "USR-STAFF-1",
            username = "staff1",
            role = UserRole.STAFF,
            permissions = emptySet(),
            projectId = "PRJ-01",
            accountStatus = AccountStatus.ACTIVE
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("staff/assigned-work", dest.route)
    }

    @Test
    fun testManagerRoleRoutesToManagerWorkspace() {
        val principal = AuthenticatedPrincipal(
            userId = "USR-MGR-1",
            username = "mgr1",
            role = UserRole.MANAGER,
            permissions = emptySet(),
            projectId = "PRJ-01",
            accountStatus = AccountStatus.ACTIVE
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("manager/operations", dest.route)
    }

    @Test
    fun testAdminRoleRoutesToAdminWorkspace() {
        val principal = AuthenticatedPrincipal(
            userId = "USR-ADM-1",
            username = "admin1",
            role = UserRole.ADMIN,
            permissions = emptySet(),
            projectId = "PRJ-01",
            accountStatus = AccountStatus.ACTIVE
        )
        val dest = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("admin/dashboard", dest.route)
    }
}
