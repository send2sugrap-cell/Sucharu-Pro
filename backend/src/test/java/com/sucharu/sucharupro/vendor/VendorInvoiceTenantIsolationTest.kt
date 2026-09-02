package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorInvoiceDataSource
import com.sucharu.sucharupro.data.repository.VendorInvoiceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorInvoiceTenantIsolationTest {

    private lateinit var repository: VendorInvoiceRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorInvoiceRepositoryImpl(FakeVendorInvoiceDataSource())
    }

    @Test
    fun testTenantAlphaCannotAccessTenantBetaInvoice() = runBlocking {
        val itemAlpha = VendorInvoiceItem(
            itemId = "vii_alpha",
            invoiceId = "inv_alpha",
            purchaseOrderItemId = "poi_alpha",
            description = "Alpha Paper",
            quantity = BigDecimal("100"),
            unitPrice = Money(50.0),
            lineTotal = Money(5000.0)
        )
        val invoiceAlpha = VendorInvoice(
            invoiceId = "inv_alpha",
            projectId = "PRJ-TENANT-ALPHA",
            vendorId = "VND-ALPHA",
            purchaseOrderId = "PO-ALPHA",
            invoiceNumber = "INV-ALPHA-001",
            vendorInvoiceNumber = "V-INV-ALPHA",
            subtotal = Money(5000.0),
            totalAmount = Money(5000.0),
            items = listOf(itemAlpha)
        )
        repository.createInvoice(invoiceAlpha)

        // Tenant Beta queries for invoice Alpha -> Must return error / not found
        val betaLookup = repository.findById("PRJ-TENANT-BETA", "inv_alpha")
        assertTrue(betaLookup is DomainResult.Error)

        // Tenant Beta lists invoices -> Must be empty
        val betaList = repository.list("PRJ-TENANT-BETA")
        assertTrue(betaList is DomainResult.Success)
        assertTrue((betaList as DomainResult.Success).data.isEmpty())
    }
}
