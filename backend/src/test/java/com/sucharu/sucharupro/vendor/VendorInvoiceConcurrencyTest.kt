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

class VendorInvoiceConcurrencyTest {

    private lateinit var repository: VendorInvoiceRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorInvoiceRepositoryImpl(FakeVendorInvoiceDataSource())
    }

    @Test
    fun testOptimisticConcurrencyConflictOnConcurrentUpdates() = runBlocking {
        val item = VendorInvoiceItem(
            itemId = "vii_01",
            invoiceId = "inv_conc",
            purchaseOrderItemId = "poi_01",
            description = "Test Item",
            quantity = BigDecimal("10"),
            unitPrice = Money(100.0),
            lineTotal = Money(1000.0)
        )
        val initial = VendorInvoice(
            invoiceId = "inv_conc",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            invoiceNumber = "INV-CONC-01",
            vendorInvoiceNumber = "V-CONC-01",
            subtotal = Money(1000.0),
            totalAmount = Money(1000.0),
            items = listOf(item),
            version = 1L
        )
        repository.createInvoice(initial)

        // Read thread 1 & thread 2
        val t1Invoice = (repository.findById("PRJ-01", "inv_conc") as DomainResult.Success).data
        val t2Invoice = (repository.findById("PRJ-01", "inv_conc") as DomainResult.Success).data

        // Thread 1 succeeds and increments version to 2
        val t1Update = repository.updateInvoice(t1Invoice.copy(notes = "Thread 1 update"))
        assertTrue(t1Update is DomainResult.Success)
        assertEquals(2L, (t1Update as DomainResult.Success).data.version)

        // Thread 2 attempts update with stale version 1 -> Must fail
        val t2Update = repository.updateInvoice(t2Invoice.copy(notes = "Thread 2 update"))
        assertTrue(t2Update is DomainResult.Error)
        assertTrue((t2Update as DomainResult.Error).message.contains("Optimistic concurrency conflict"))
    }
}
