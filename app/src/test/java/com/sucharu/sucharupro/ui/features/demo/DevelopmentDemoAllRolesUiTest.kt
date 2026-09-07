package com.sucharu.sucharupro.ui.features.demo

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.composition.DemoRole
import com.sucharu.sucharupro.ui.features.auth.PostLoginRouter
import org.junit.Assert.assertEquals
import org.junit.Test

class DevelopmentDemoAllRolesUiTest {

    @Test
    fun testCustomerRole_postLoginRouting() {
        val principal = AuthenticatedPrincipal(userId = "CUST-1", projectId = "TENANT-1", username = "cust", role = UserRole.CUSTOMER)
        val destination = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("customer/home", destination.route)
    }

    @Test
    fun testAffiliateRole_postLoginRouting() {
        val principal = AuthenticatedPrincipal(userId = "AFF-1", projectId = "TENANT-1", username = "aff", role = UserRole.AFFILIATE)
        val destination = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("affiliate/home", destination.route)
    }

    @Test
    fun testStaffRole_postLoginRouting() {
        val principal = AuthenticatedPrincipal(userId = "STAFF-1", projectId = "TENANT-1", username = "staff", role = UserRole.STAFF)
        val destination = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("staff/assigned-work", destination.route)
    }

    @Test
    fun testManagerRole_postLoginRouting() {
        val principal = AuthenticatedPrincipal(userId = "MGR-1", projectId = "TENANT-1", username = "mgr", role = UserRole.MANAGER)
        val destination = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("manager/operations", destination.route)
    }

    @Test
    fun testAdminRole_postLoginRouting() {
        val principal = AuthenticatedPrincipal(userId = "ADM-1", projectId = "TENANT-1", username = "adm", role = UserRole.ADMIN)
        val destination = PostLoginRouter.resolveAppDestination(principal)
        assertEquals("admin/dashboard", destination.route)
    }

    @Test
    fun testDemoRoleSelector_allRolesMappedToCanonicalUserRoles() {
        assertEquals(UserRole.CUSTOMER, DemoRole.CUSTOMER.userRole)
        assertEquals(UserRole.AFFILIATE, DemoRole.AFFILIATE.userRole)
        assertEquals(UserRole.STAFF, DemoRole.STAFF.userRole)
        assertEquals(UserRole.MANAGER, DemoRole.MANAGER.userRole)
        assertEquals(UserRole.ADMIN, DemoRole.ADMIN.userRole)
    }
}
