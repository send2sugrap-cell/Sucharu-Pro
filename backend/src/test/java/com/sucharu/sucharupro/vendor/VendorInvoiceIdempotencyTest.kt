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

class VendorInvoiceIdempotencyTest {

    private lateinit var repository: VendorInvoiceRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorInvoiceRepositoryImpl(FakeVendorInvoiceDataSource())
    }

    @Test
    fun testReplayInvoiceCreationIsRejected() = runBlocking {
        val item = VendorInvoiceItem(
            itemId = "vii_01",
            invoiceId = "inv_idem",
            purchaseOrderItemId = "poi_01",
            description = "Test Item",
            quantity = BigDecimal("10"),
            unitPrice = Money(100.0),
            lineTotal = Money(1000.0)
        )
        val invoice = VendorInvoice(
            invoiceId = "inv_idem",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            invoiceNumber = "INV-IDEM-01",
            vendorInvoiceNumber = "V-IDEM-01",
            subtotal = Money(1000.0),
            totalAmount = Money(1000.0),
            items = listOf(item)
        )

        val first = repository.createInvoice(invoice)
        assertTrue(first is DomainResult.Success)

        val duplicate = repository.createInvoice(invoice)
        assertTrue(duplicate is DomainResult.Error)
        assertTrue((duplicate as DomainResult.Error).message.contains("already exists"))
    }
}
