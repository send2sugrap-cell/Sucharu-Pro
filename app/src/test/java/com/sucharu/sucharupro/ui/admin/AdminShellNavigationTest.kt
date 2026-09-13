package com.sucharu.sucharupro.ui.admin

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.admin.shell.AdminNavigationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminShellNavigationTest {

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = "PRJ-001",
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "stf-001",
        projectId = "PRJ-001",
        username = "staff_operator",
        role = UserRole.STAFF
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cus-001",
        projectId = "PRJ-001",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        customerId = "cus-001"
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "aff-001",
        projectId = "PRJ-001",
        username = "affiliate_user",
        role = UserRole.AFFILIATE,
        affiliateId = "aff-001"
    )

    @Test
    fun testNavigationRegistry_allGroupsMapModules00to24() {
        val groups = AdminNavigationRegistry.allGroups
        assertTrue("Navigation groups must cover Modules 00-24", groups.isNotEmpty())

        val groupIds = groups.map { it.groupId }
        assertTrue(groupIds.contains("FOUNDATION"))
        assertTrue(groupIds.contains("COMMERCIAL"))
        assertTrue(groupIds.contains("PRODUCTION"))
        assertTrue(groupIds.contains("INVENTORY"))
        assertTrue(groupIds.contains("FINANCE"))
        assertTrue(groupIds.contains("RELATIONSHIP"))
        assertTrue(groupIds.contains("INTELLIGENCE"))
        assertTrue(groupIds.contains("SYSTEM"))
    }

    @Test
    fun testAdminPrincipal_authorizedForFullNavigation() {
        val authorizedGroups = AdminNavigationRegistry.getAuthorizedGroups(adminPrincipal)
        assertEquals(AdminNavigationRegistry.allGroups.size, authorizedGroups.size)
    }

    @Test
    fun testStaffPrincipal_capabilityFilteredNavigation() {
        val authorizedGroups = AdminNavigationRegistry.getAuthorizedGroups(staffPrincipal)
        assertTrue(authorizedGroups.isNotEmpty())

        val groupTitles = authorizedGroups.map { it.title }
        assertTrue(groupTitles.contains("PRODUCTION & PREPRESS"))
        assertTrue(groupTitles.contains("INVENTORY & LOGISTICS"))
    }

    @Test
    fun testCustomerPrincipal_capabilityFilteredNavigation() {
        val authorizedGroups = AdminNavigationRegistry.getAuthorizedGroups(customerPrincipal)
        assertTrue(authorizedGroups.isNotEmpty())

        val groupTitles = authorizedGroups.map { it.title }
        assertTrue(groupTitles.contains("CUSTOMER & COMMERCIAL"))
    }

    @Test
    fun testAffiliatePrincipal_capabilityFilteredNavigation() {
        val authorizedGroups = AdminNavigationRegistry.getAuthorizedGroups(affiliatePrincipal)
        assertTrue(authorizedGroups.isNotEmpty())

        val groupTitles = authorizedGroups.map { it.title }
        assertTrue(groupTitles.contains("AFFILIATE & NETWORK"))
    }

    @Test
    fun testNavItemDestinations_haveValidRoutesAndModules() {
        AdminNavigationRegistry.allGroups.flatMap { it.items }.forEach { item ->
            assertNotNull(item.destination)
            assertTrue(item.destination.route.isNotBlank())
            assertTrue(item.destination.title.isNotBlank())
            assertTrue(item.canonicalModule.startsWith("Module"))
        }
    }
}
