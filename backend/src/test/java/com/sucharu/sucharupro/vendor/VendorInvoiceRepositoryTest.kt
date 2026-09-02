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

class VendorInvoiceRepositoryTest {

    private lateinit var repository: VendorInvoiceRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorInvoiceRepositoryImpl(FakeVendorInvoiceDataSource())
    }

    private fun createInvoice(id: String, num: String, vInvNum: String): VendorInvoice {
        val item = VendorInvoiceItem(
            itemId = "vii_$id",
            invoiceId = id,
            purchaseOrderItemId = "poi_01",
            description = "Test Item",
            quantity = BigDecimal("10"),
            unitPrice = Money(100.0),
            lineTotal = Money(1000.0)
        )
        return VendorInvoice(
            invoiceId = id,
            projectId = "PRJ-01",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            invoiceNumber = num,
            vendorInvoiceNumber = vInvNum,
            subtotal = Money(1000.0),
            totalAmount = Money(1000.0),
            items = listOf(item)
        )
    }

    @Test
    fun testCreateAndFindById() = runBlocking {
        val inv = createInvoice("inv_001", "INV-2026-001", "V-001")
        val createRes = repository.createInvoice(inv)
        assertTrue(createRes is DomainResult.Success)

        val findRes = repository.findById("PRJ-01", "inv_001")
        assertTrue(findRes is DomainResult.Success)
        val loaded = (findRes as DomainResult.Success).data
        assertEquals("inv_001", loaded.invoiceId)
        assertEquals("INV-2026-001", loaded.invoiceNumber)
        assertEquals(1, loaded.items.size)
    }

    @Test
    fun testFindByInvoiceNumber() = runBlocking {
        val inv = createInvoice("inv_002", "INV-2026-002", "V-002")
        repository.createInvoice(inv)

        val findRes = repository.findByInvoiceNumber("PRJ-01", "INV-2026-002")
        assertTrue(findRes is DomainResult.Success)
        assertEquals("inv_002", (findRes as DomainResult.Success).data.invoiceId)
    }

    @Test
    fun testFindByVendorInvoiceNumber() = runBlocking {
        val inv = createInvoice("inv_003", "INV-2026-003", "V-003")
        repository.createInvoice(inv)

        val findRes = repository.findByVendorInvoiceNumber("PRJ-01", "VND-01", "V-003")
        assertTrue(findRes is DomainResult.Success)
        assertEquals("inv_003", (findRes as DomainResult.Success).data.invoiceId)
    }

    @Test
    fun testUpdateStatus() = runBlocking {
        val inv = createInvoice("inv_004", "INV-2026-004", "V-004")
        repository.createInvoice(inv)

        val updateRes = repository.updateStatus("PRJ-01", "inv_004", VendorInvoiceStatus.SUBMITTED, updatedBy = "user1")
        assertTrue(updateRes is DomainResult.Success)
        assertEquals(VendorInvoiceStatus.SUBMITTED, (updateRes as DomainResult.Success).data.status)
        assertEquals(2L, updateRes.data.version)
    }

    @Test
    fun testListFiltering() = runBlocking {
        repository.createInvoice(createInvoice("inv_005", "INV-2026-005", "V-005"))
        repository.createInvoice(createInvoice("inv_006", "INV-2026-006", "V-006"))

        val list = repository.list("PRJ-01", vendorId = "VND-01")
        assertTrue(list is DomainResult.Success)
        assertEquals(2, (list as DomainResult.Success).data.size)
    }
}
