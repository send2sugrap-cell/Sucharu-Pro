package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccessPolicy
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRole
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalDashboardCalculator
import org.junit.Assert.*
import org.junit.Test

class VendorPortalRoleVisibilityTest {

    @Test
    fun testOperatorRoleVisibility() {
        val policy = VendorPortalAccessPolicy(policyId = "p1", tenantId = "T1", projectId = "P1")
        val vis = VendorPortalDashboardCalculator.resolveFeatureVisibility(VendorPortalRole.VENDOR_OPERATOR, policy)

        assertTrue(vis.canViewProfile)
        assertTrue(vis.canViewWorkOrders)
        assertTrue(vis.canViewDeliveries)
        assertFalse(vis.canViewFinancials)
        assertFalse(vis.canViewInvoices)
        assertFalse(vis.canViewSettlements)
        assertFalse(vis.canManagePortalUsers)
    }

    @Test
    fun testLogisticsRoleVisibility() {
        val policy = VendorPortalAccessPolicy(policyId = "p2", tenantId = "T1", projectId = "P1")
        val vis = VendorPortalDashboardCalculator.resolveFeatureVisibility(VendorPortalRole.VENDOR_LOGISTICS, policy)

        assertTrue(vis.canViewProfile)
        assertTrue(vis.canViewPurchaseOrders)
        assertTrue(vis.canViewDeliveries)
        assertFalse(vis.canViewWorkOrders)
        assertFalse(vis.canViewFinancials)
        assertFalse(vis.canViewQuality)
    }

    @Test
    fun testQcRoleVisibility() {
        val policy = VendorPortalAccessPolicy(policyId = "p3", tenantId = "T1", projectId = "P1")
        val vis = VendorPortalDashboardCalculator.resolveFeatureVisibility(VendorPortalRole.VENDOR_QC, policy)

        assertTrue(vis.canViewProfile)
        assertTrue(vis.canViewQuality)
        assertTrue(vis.canViewDisputes)
        assertFalse(vis.canViewFinancials)
        assertFalse(vis.canViewRates)
    }
}
