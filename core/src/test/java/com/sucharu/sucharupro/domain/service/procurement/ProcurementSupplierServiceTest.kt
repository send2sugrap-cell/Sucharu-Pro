package com.sucharu.sucharupro.domain.service.procurement

import com.sucharu.sucharupro.domain.model.procurement.ProcurementSupplierCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProcurementSupplierServiceTest {

    private lateinit var service: ProcurementSupplierService

    @Before
    fun setUp() {
        service = ProcurementSupplierService()
    }

    @Test
    fun `buildProcurementSupplierManagementSummary_computesTotalsAndPreservesFinishedGoodsInventoryBoundary`() {
        val summary = service.buildProcurementSupplierManagementSummary()

        assertNotNull(summary)
        assertEquals(2, summary.totalSuppliersCount)
        assertEquals(2, summary.activeSuppliersCount)
        assertEquals(20, summary.totalActivePurchaseOrdersCount)

        assertEquals(BigDecimal("630000.00"), summary.totalProcurementCommitmentValue)
        assertEquals(BigDecimal("30000.00"), summary.totalSupplierPayablesOutstanding)
        assertEquals(BigDecimal("600000.00"), summary.totalSupplierPaymentsMadeYtd)

        // CRITICAL INVARIANT PROOF:
        // Finished Goods Only Inventory Boundary MUST be preserved!
        assertTrue("Finished Goods Only Inventory Boundary MUST be preserved!", summary.isFinishedGoodsInventoryBoundaryPreserved)

        val ctpSuppliers = service.filterSuppliersByCategory(summary, ProcurementSupplierCategory.CTP_PLATE_SUPPLIER)
        assertEquals(1, ctpSuppliers.size)
        assertEquals("VND-2026-001", ctpSuppliers.first().vendorId)
    }
}
