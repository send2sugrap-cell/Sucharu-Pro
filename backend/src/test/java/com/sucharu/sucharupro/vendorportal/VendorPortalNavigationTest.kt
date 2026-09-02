package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFeatureVisibility
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalDashboardCalculator
import org.junit.Assert.*
import org.junit.Test

class VendorPortalNavigationTest {

    @Test
    fun testNavigationItemBuildingAndBadgeCounts() {
        val visibility = VendorPortalFeatureVisibility(
            canViewProfile = true,
            canViewServices = true,
            canViewCapabilities = true,
            canViewPurchaseOrders = true,
            canViewWorkOrders = true,
            canViewDeliveries = true,
            canViewInvoices = true,
            canViewFinancials = true,
            canViewQuality = true,
            canViewDisputes = true,
            canViewSettlements = true,
            canViewPerformance = true,
            canManagePortalUsers = true
        )

        val navItems = VendorPortalDashboardCalculator.buildNavigationItems(
            visibility = visibility,
            openPoCount = 3,
            openWoCount = 2,
            pendingDeliveryCount = 1,
            pendingInvoiceCount = 4,
            openDisputeCount = 0
        )

        assertTrue(navItems.isNotEmpty())

        val poNav = navItems.firstOrNull { it.id == "nav_po" }
        assertNotNull(poNav)
        assertEquals(3, poNav!!.badgeCount)

        val woNav = navItems.firstOrNull { it.id == "nav_wo" }
        assertNotNull(woNav)
        assertEquals(2, woNav!!.badgeCount)

        val invNav = navItems.firstOrNull { it.id == "nav_invoices" }
        assertNotNull(invNav)
        assertEquals(4, invNav!!.badgeCount)

        val settingsNav = navItems.firstOrNull { it.id == "nav_settings" }
        assertNotNull(settingsNav)
    }
}
