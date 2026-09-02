package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalDashboardDomainTest {

    @Test
    fun testDashboardModelsInstantiationAndIntegrity() {
        val kpi = VendorPortalKpi(
            key = "ACTIVE_PO",
            label = "Active Purchase Orders",
            value = "5",
            numericValue = 5.0,
            status = "GOOD",
            category = "OPERATIONAL"
        )
        assertEquals("ACTIVE_PO", kpi.key)
        assertEquals("5", kpi.value)
        assertEquals(5.0, kpi.numericValue!!, 0.001)

        val profile = VendorPortalProfileSummary(
            vendorId = "vnd_001",
            vendorCode = "VND-001",
            vendorName = "Apex Print Corp",
            vendorType = "MANUFACTURER",
            category = "RAW_MATERIALS",
            status = "ACTIVE",
            portalAccountStatus = "ACTIVE",
            portalRole = "VENDOR_ADMIN",
            projectScope = "PROJ-ALPHA",
            serviceCount = 3,
            capabilityCount = 5,
            activeRatesCount = 2
        )
        assertEquals("vnd_001", profile.vendorId)
        assertEquals(3, profile.serviceCount)

        val operations = VendorPortalOperationalSummary(
            totalPurchaseOrders = 10,
            activePurchaseOrders = 2,
            completedPurchaseOrders = 8,
            totalWorkOrders = 5,
            openWorkOrders = 1,
            completedWorkOrders = 4,
            totalDeliveries = 8,
            pendingDeliveries = 1,
            acceptedDeliveries = 7,
            onTimeDeliveryRatePercent = 87.5,
            poFulfillmentRatePercent = 80.0
        )
        assertEquals(10, operations.totalPurchaseOrders)
        assertEquals(87.5, operations.onTimeDeliveryRatePercent, 0.001)

        val financials = VendorPortalFinancialSummary(
            totalInvoices = 6,
            pendingInvoices = 2,
            approvedInvoices = 1,
            paidInvoices = 3,
            disputedInvoices = 0,
            totalInvoicedAmount = Money(6000.0),
            totalPaidAmount = Money(3000.0),
            totalOutstandingPayables = Money(3000.0),
            totalSettlements = 2,
            pendingSettlementsCount = 1
        )
        assertEquals(6, financials.totalInvoices)
        assertEquals(Money(3000.0), financials.totalOutstandingPayables)
    }

    @Test
    fun testFeatureVisibilityDefaults() {
        val visibility = VendorPortalFeatureVisibility()
        assertTrue(visibility.canViewProfile)
        assertTrue(visibility.canViewServices)
        assertTrue(visibility.canViewCapabilities)
        assertFalse(visibility.canViewRates)
        assertFalse(visibility.canViewFinancials)
        assertFalse(visibility.canManagePortalUsers)
    }
}
