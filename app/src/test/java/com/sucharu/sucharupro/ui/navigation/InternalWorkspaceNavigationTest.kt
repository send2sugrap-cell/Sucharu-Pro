package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import org.junit.Assert.*
import org.junit.Test

class InternalWorkspaceNavigationTest {

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "USR-STAFF-1",
        username = "staff1",
        role = UserRole.STAFF,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USR-MGR-1",
        username = "mgr1",
        role = UserRole.MANAGER,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USR-ADM-1",
        username = "admin1",
        role = UserRole.ADMIN,
        permissions = emptySet(),
        projectId = "PRJ-01",
        accountStatus = AccountStatus.ACTIVE
    )

    @Test
    fun testStaffCanAccessStaffDestinations() {
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(staffPrincipal, AppDestination.Staff.AssignedWork))
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(staffPrincipal, AppDestination.Staff.Production))
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(staffPrincipal, AppDestination.Staff.Qc))
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(staffPrincipal, AppDestination.Staff.Inventory))
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(staffPrincipal, AppDestination.Staff.Delivery))
    }

    @Test
    fun testManagerCanAccessManagerDestinations() {
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(managerPrincipal, AppDestination.Manager.Operations))
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(managerPrincipal, AppDestination.Manager.Approvals))
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(managerPrincipal, AppDestination.Manager.FinanceVisibility))
    }

    @Test
    fun testAdminCanAccessAdminDestinations() {
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(adminPrincipal, AppDestination.Admin.FullAdministration))
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(adminPrincipal, AppDestination.Admin.Users))
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(adminPrincipal, AppDestination.Admin.Security))
        assertTrue(CapabilityAwareNavigation.isRouteAuthorized(adminPrincipal, AppDestination.Admin.Finance))
    }
}
