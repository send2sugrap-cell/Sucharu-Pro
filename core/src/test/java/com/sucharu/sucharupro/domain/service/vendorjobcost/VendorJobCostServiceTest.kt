package com.sucharu.sucharupro.domain.service.vendorjobcost

import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendorjobcost.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorJobCostServiceTest {

    private lateinit var service: VendorJobCostService

    @Before
    fun setUp() {
        service = VendorJobCostService()
    }

    @Test
    fun `getEligibleVendorsForCostContext_filtersOnlyActiveVendorsMatchingRequiredCategory`() {
        // Paper Purchase context requires PAPER_SUPPLIER capability
        val paperVendors = service.getEligibleVendorsForCostContext(WorkCostContext.PAPER_PURCHASE)

        assertEquals(1, paperVendors.size)
        assertEquals("VND-2026-PAPER", paperVendors.first().vendorId)
        assertEquals(VendorCategory.PAPER_SUPPLIER, paperVendors.first().vendorCategory)

        // CTP Prepress context requires CTP_PREPRESS capability
        val ctpVendors = service.getEligibleVendorsForCostContext(WorkCostContext.CTP_PREPRESS)
        assertEquals(1, ctpVendors.size)
        assertEquals("VND-2026-CTP", ctpVendors.first().vendorId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addJobVendorCostEntry_rejectsIncompatibleVendorCategoryForContext`() {
        // Attempting to attach CTP Vendor (VND-2026-CTP) to PAPER_PURCHASE work context MUST fail!
        val invalidEntry = JobVendorCostEntry(
            costEntryId = "COST-INVALID-01",
            jobId = "JOB-2026-001",
            orderId = "ORD-1001",
            workContext = WorkCostContext.PAPER_PURCHASE,
            vendorAttributionType = VendorCostAttributionType.VENDOR,
            vendorId = "VND-2026-CTP", // Incompatible! (CTP Prepress vendor for Paper Purchase)
            amount = BigDecimal("50000.00"),
            createdAt = "2026-09-27T13:30:00Z",
            createdBy = "STAFF-001"
        )

        service.addJobVendorCostEntry(invalidEntry)
    }

    @Test
    fun `addJobVendorCostEntry_supportsMultiVendorEntriesAndNoVendorAttribution`() {
        // Entry 1: Paper Purchase from Paper Vendor
        val paperEntry = JobVendorCostEntry(
            costEntryId = "COST-001",
            jobId = "JOB-2026-001",
            orderId = "ORD-1001",
            workContext = WorkCostContext.PAPER_PURCHASE,
            vendorAttributionType = VendorCostAttributionType.VENDOR,
            vendorId = "VND-2026-PAPER",
            vendorName = "Bengal Paper & Substrate Suppliers",
            amount = BigDecimal("50000.00"),
            invoiceBillRef = "BILL-1024",
            createdAt = "2026-09-27T13:30:00Z",
            createdBy = "STAFF-001"
        )

        // Entry 2: CTP Prepress from CTP Vendor
        val ctpEntry = JobVendorCostEntry(
            costEntryId = "COST-002",
            jobId = "JOB-2026-001",
            orderId = "ORD-1001",
            workContext = WorkCostContext.CTP_PREPRESS,
            vendorAttributionType = VendorCostAttributionType.VENDOR,
            vendorId = "VND-2026-CTP",
            vendorName = "Creative CTP & Plate House",
            amount = BigDecimal("4500.00"),
            invoiceBillRef = "BILL-2048",
            createdAt = "2026-09-27T13:30:00Z",
            createdBy = "STAFF-001"
        )

        // Entry 3: Local Transport with NO_VENDOR (vendorId = null)
        val transportEntry = JobVendorCostEntry(
            costEntryId = "COST-003",
            jobId = "JOB-2026-001",
            orderId = "ORD-1001",
            workContext = WorkCostContext.LOGISTICS_TRANSPORT,
            vendorAttributionType = VendorCostAttributionType.NO_VENDOR,
            vendorId = null, // vendorId = null for NO_VENDOR
            amount = BigDecimal("1000.00"),
            notes = "Local rickshaw/van transport",
            createdAt = "2026-09-27T13:30:00Z",
            createdBy = "STAFF-001"
        )

        service.addJobVendorCostEntry(paperEntry)
        service.addJobVendorCostEntry(ctpEntry)
        service.addJobVendorCostEntry(transportEntry)

        // Verify Multi-Vendor Accounting Summary
        val summary = service.getJobVendorCostSummary("JOB-2026-001")
        assertNotNull(summary)
        assertEquals("JOB-2026-001", summary.jobId)
        assertEquals(3, summary.costEntries.size)
        assertEquals(BigDecimal("54500.00"), summary.totalVendorCost) // 50000 + 4500
        assertEquals(BigDecimal("1000.00"), summary.totalNonVendorCost) // 1000
        assertEquals(BigDecimal("55500.00"), summary.grandTotalJobCost) // 55500
    }
}
