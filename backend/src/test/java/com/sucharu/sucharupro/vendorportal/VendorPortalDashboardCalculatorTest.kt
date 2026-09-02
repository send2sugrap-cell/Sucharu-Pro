package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccessPolicy
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRole
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalDashboardCalculator
import org.junit.Assert.*
import org.junit.Test

class VendorPortalDashboardCalculatorTest {

    @Test
    fun testZeroSafePercentageCalculation() {
        assertEquals(0.0, VendorPortalDashboardCalculator.calculatePercentage(0, 0), 0.001)
        assertEquals(0.0, VendorPortalDashboardCalculator.calculatePercentage(5, 0), 0.001)
        assertEquals(0.0, VendorPortalDashboardCalculator.calculatePercentage(0, 10), 0.001)
        assertEquals(0.0, VendorPortalDashboardCalculator.calculatePercentage(-5, 10), 0.001)
        assertEquals(50.0, VendorPortalDashboardCalculator.calculatePercentage(5, 10), 0.001)
        assertEquals(33.33, VendorPortalDashboardCalculator.calculatePercentage(1, 3), 0.001)
        assertEquals(66.67, VendorPortalDashboardCalculator.calculatePercentage(2, 3), 0.001)
    }

    @Test
    fun testOutstandingPayablesZeroSafe() {
        val invoiced = Money(5000.0)
        val paid = Money(3000.0)
        val outstanding = VendorPortalDashboardCalculator.calculateOutstandingPayables(invoiced, paid)
        assertEquals(Money(2000.0), outstanding)

        // When paid > invoiced (e.g. advance), outstanding never goes negative
        val overpaid = Money(7000.0)
        val zeroOutstanding = VendorPortalDashboardCalculator.calculateOutstandingPayables(invoiced, overpaid)
        assertEquals(Money.ZERO, zeroOutstanding)
    }

    @Test
    fun testQualityRatingResolution() {
        assertEquals("EXCELLENT", VendorPortalDashboardCalculator.resolveQualityRating(0.5))
        assertEquals("GOOD", VendorPortalDashboardCalculator.resolveQualityRating(2.0))
        assertEquals("FAIR", VendorPortalDashboardCalculator.resolveQualityRating(4.5))
        assertEquals("POOR", VendorPortalDashboardCalculator.resolveQualityRating(8.0))
    }

    @Test
    fun testFeatureVisibilityResolutionByRole() {
        val policy = VendorPortalAccessPolicy(
            policyId = "pol_01",
            tenantId = "TENANT-01",
            projectId = "PROJ-01",
            allowRfqSubmission = true,
            allowPoAcknowledgement = true,
            allowInvoiceSubmission = true,
            allowQualityDispute = true
        )

        // VENDOR_ADMIN
        val adminVis = VendorPortalDashboardCalculator.resolveFeatureVisibility(VendorPortalRole.VENDOR_ADMIN, policy)
        assertTrue(adminVis.canViewProfile)
        assertTrue(adminVis.canViewRates)
        assertTrue(adminVis.canViewPurchaseOrders)
        assertTrue(adminVis.canViewInvoices)
        assertTrue(adminVis.canViewFinancials)
        assertTrue(adminVis.canViewQuality)
        assertTrue(adminVis.canManagePortalUsers)

        // VENDOR_OPERATOR
        val opVis = VendorPortalDashboardCalculator.resolveFeatureVisibility(VendorPortalRole.VENDOR_OPERATOR, policy)
        assertTrue(opVis.canViewProfile)
        assertFalse(opVis.canViewRates)
        assertTrue(opVis.canViewPurchaseOrders)
        assertTrue(opVis.canViewWorkOrders)
        assertFalse(opVis.canViewInvoices)
        assertFalse(opVis.canViewFinancials)
        assertFalse(opVis.canManagePortalUsers)

        // VENDOR_FINANCE
        val finVis = VendorPortalDashboardCalculator.resolveFeatureVisibility(VendorPortalRole.VENDOR_FINANCE, policy)
        assertTrue(finVis.canViewProfile)
        assertTrue(finVis.canViewRates)
        assertTrue(finVis.canViewInvoices)
        assertTrue(finVis.canViewFinancials)
        assertFalse(finVis.canViewQuality)
        assertFalse(finVis.canManagePortalUsers)

        // VENDOR_QC
        val qcVis = VendorPortalDashboardCalculator.resolveFeatureVisibility(VendorPortalRole.VENDOR_QC, policy)
        assertTrue(qcVis.canViewProfile)
        assertFalse(qcVis.canViewRates)
        assertTrue(qcVis.canViewQuality)
        assertTrue(qcVis.canViewDisputes)
        assertFalse(qcVis.canViewFinancials)

        // VENDOR_VIEWER
        val viewerVis = VendorPortalDashboardCalculator.resolveFeatureVisibility(VendorPortalRole.VENDOR_VIEWER, policy)
        assertTrue(viewerVis.canViewProfile)
        assertFalse(viewerVis.canViewRates)
        assertFalse(viewerVis.canViewFinancials)
        assertFalse(viewerVis.canManagePortalUsers)
    }
}
